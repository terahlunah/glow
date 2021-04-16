package glow.parsing

import com.github.michaelbull.result.*
import kotlin.reflect.typeOf

class Parser(
    source: String,
) {
    private val tokenizer = Tokenizer(source)

    private var token = tokenizer.nextToken()

    private fun error(message: String) = tokenizer.error(message)

    private fun next(): ParseResult<TokenInfo> {
        val old = token
        token = tokenizer.nextToken()
        return old
    }

    private inline fun <reified T : Token> consume(): ParseResult<T> = token.flatMap {
        if (it.token is T) {
            next()
            Ok(it.token)
        } else {
            error("Expected ${T::class.simpleName} but found ${it.token::class.simpleName}")
        }
    }

    private inline fun <reified T : Token> expect(message: String): ParseResult<T> = consume<T>().mapError {
        error("Expected $message").unwrapError()
    }

    private fun tryConsume(type: Token): Boolean = token.mapOr(false) {
        val ok = it.token == type
        if (ok) next()
        ok
    }

    // Parsing

    fun parse(): ParseResult<Ast> = binding {

        val definitions = mutableListOf<Definition>()

        while (true) {
            definitions.add(
                when (token.bind().token) {
                    Token.Type -> parseTypeDefinition().bind()
                    Token.Def -> parseTermDefinition().bind()
                    Token.Eof -> break
                    else -> error("Expected top level statement").bind<Definition>()
                }
            )
        }

        definitions
    }

    private fun parseTermDefinition(): ParseResult<Definition.TermDef> = binding {

        expect<Token.Def>("keyword 'def'").bind()

        val name = parseTerm().bind()

        val generics = parseGenerics().bind()

        val functionType = parseFunctionType().bind()

        val assignments = parseAssignments().bind()

        consume<Token.Equal>().bind()

        val body = parseTermBody().bind()

        Definition.TermDef(
            name,
            generics,
            functionType,
            assignments,
            body,
        )
    }

    private fun parseTypeDefinition(): ParseResult<Definition.TypeDef> = binding {
        expect<Token.Type>("keyword 'type'").bind()

        val name = parseIdent().bind()

        val generics = parseGenerics().bind()

        val constructors = mutableListOf<Constructor>()
        while (tryConsume(Token.Pipe)) {
            constructors.add(parseConsructor().bind())
        }

        Definition.TypeDef(
            name,
            generics,
            constructors,
        )
    }

    private fun parseTerm(): ParseResult<String> = binding {
        val ident = when (val type = token.bind().token) {
            Token.LeftChevron -> "<"
            Token.RightChevron -> ">"
            is Token.Ident -> type.id
            else -> error("Expected term").bind<String>()
        }
        next().bind()
        ident
    }

    private fun parseTermBody(): ParseResult<List<Expr>> = binding {
        listOf(parseExpr().bind()) + parseExprList().bind()
    }

    private fun parseIdent(): ParseResult<String> = binding {
        val ident = when (val type = token.bind().token) {
            is Token.Ident -> type.id
            else -> error("Expected identifier").bind<String>()
        }
        next().bind()
        ident
    }

    private fun parseGenerics(): ParseResult<List<String>> = binding {
        val generics = mutableListOf<String>()

        if (tryConsume(Token.LeftChevron)) {
            generics.add(parseIdent().bind())

            while (tryConsume(Token.Comma)) {
                generics.add(parseIdent().bind())
            }

            consume<Token.RightChevron>().bind()
        }

        generics
    }

    private fun parseType(): ParseResult<Type> = binding {
        val name = parseIdent().bind()
        val generics = parseGenerics().bind()
        Type(name, generics)
    }

    private fun parseFunctionType(): ParseResult<FunctionType> = binding {
        consume<Token.LeftPar>().bind()

        val inputs = parseFunctionTypeList().bind()

        val outputs = if (tryConsume(Token.Arrow)) {
            parseFunctionTypeList().bind()
        } else {
            emptyList()
        }

        consume<Token.RightPar>().bind()

        FunctionType.Function(inputs, outputs)
    }

    private fun parseFunctionTypeList(): ParseResult<List<FunctionType>> = binding {
        val types = mutableListOf<FunctionType>()

        while (true) {
            val type = when (token.bind().token) {
                Token.LeftPar -> parseFunctionType().bind()
                is Token.Ident -> FunctionType.Cell(parseType().bind())
                else -> break
            }

            types.add(type)
            tryConsume(Token.Comma)
        }

        types
    }

    private fun parseAssignments(): ParseResult<List<String>> = binding {
        val assignments = mutableListOf<String>()

        if (tryConsume(Token.Arrow)) {
            while (token.bind().token.isIdent()) {
                assignments.add(parseIdent().bind())
                tryConsume(Token.Comma)
            }
            tryConsume(Token.Comma)
        }

        assignments
    }

    private fun parseConsructor(): ParseResult<Constructor> = binding {
        val name = parseIdent().bind()
        val types = mutableListOf<Type>()

        if (tryConsume(Token.LeftPar)) {
            types.add(parseType().bind())

            while (tryConsume(Token.Comma)) {
                types.add(parseType().bind())
            }

            consume<Token.RightPar>().bind()
        }

        Constructor(name, types)
    }

    private fun parseExpr(): ParseResult<Expr> = binding {
        when (val type = token.bind().token) {
            is Token.Str, is Token.Num -> parseLiteral().bind()
            Token.Match -> parseMatch().bind()
            Token.LeftBracket -> {
                consume<Token.LeftBracket>().bind()
                val exprs = parseExprList().bind()
                consume<Token.RightBracket>().bind()
                Expr.Closure(exprs)
            }
            else -> {
                if (type.isTerm()) {
                    Expr.Term(parseTerm().bind())
                } else {
                    error("Expected a term").bind<Expr>()
                }
            }
        }

    }

    private fun parseExprList(): ParseResult<List<Expr>> = binding {
        val exprs = mutableListOf<Expr>()

        while (token.bind().token.isExpr()) {
            exprs.add(parseExpr().bind())
        }

        exprs
    }

    private fun parseMatch(): ParseResult<Expr.Match> = binding {
        consume<Token.Match>().bind()

        val branches = mutableListOf<MatchBranch>()

        while (tryConsume(Token.Pipe)) {
            val cons = parseIdent().bind()
            consume<Token.LeftBracket>().bind()
            val exprs = parseExprList().bind()
            consume<Token.RightBracket>().bind()
            branches.add(MatchBranch(cons, exprs))
        }

        Expr.Match(branches)
    }

    private fun parseLiteral(): ParseResult<Expr.Literal> = binding {
        val expr = when (val type = token.bind().token) {
            is Token.Str -> Expr.Literal(Literal.Str(type.s))
            is Token.Num -> Expr.Literal(Literal.Num(type.n))
            else -> error("Expected a literal").bind<Expr.Literal>()
        }
        next().bind()
        expr
    }

    private fun Token.isTerm() = when (this) {
        Token.LeftChevron,
        Token.RightChevron,
        is Token.Ident,
        -> true
        else -> false
    }

    private fun Token.isIdent() = when (this) {
        is Token.Ident -> true
        else -> false
    }

    private fun Token.isExpr() = when (this) {
        Token.LeftBracket,
        Token.LeftBraces,
        Token.Match,
        is Token.Str,
        is Token.Num,
        -> true
        else -> this.isTerm()
    }

}