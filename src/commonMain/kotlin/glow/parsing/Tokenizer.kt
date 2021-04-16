package glow.parsing

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok

class Tokenizer(
    source: String,
) {
    private var offset: Int = 0
    private var line: Int = 1
    private var column: Int = 1
    private val source: String = source.filter { it != '\r' }

    private fun lineAt(line: Int) = source.split('\n')[line]

    private fun location() = Location(lineAt(line - 1), offset, line, column)

    private fun token(type: Token, location: Location? = null) = Ok(TokenInfo(type, location ?: location()))

    fun error(message: String) = Err(ParseError(message, location()))

    private fun cur() = source.getOrNull(offset)

    private fun peek() = source.getOrNull(offset + 1)

    private fun advance(n: Int = 1) {
        repeat(n) {
            if (cur() == '\n') {
                column++
                line++
            } else {
                column++
            }
            offset++
        }
    }

    fun nextToken(): ParseResult<TokenInfo> {

        skipComment()
        skipBlank()

        return when {
            offset >= source.length -> token(Token.Eof)
            cur() == '"' -> tokenizeString()
            cur() == '-' && peek() == '>' -> {
                advance(2)
                token(Token.Arrow)
            }
            cur().isIdent() -> tokenizeIdent()
            else -> {
                val c = cur()
                advance()
                when (c) {
                    '<' -> token(Token.LeftChevron)
                    '>' -> token(Token.RightChevron)
                    '(' -> token(Token.LeftPar)
                    ')' -> token(Token.RightPar)
                    '[' -> token(Token.LeftBracket)
                    ']' -> token(Token.RightBracket)
                    '{' -> token(Token.LeftBraces)
                    '}' -> token(Token.RightBraces)
                    '|' -> token(Token.Pipe)
                    '=' -> token(Token.Equal)
                    ',' -> token(Token.Comma)
                    ';' -> token(Token.Semicolon)
                    '"' -> tokenizeString()
                    else -> error("Unexpected character")
                }
            }
        }
    }

    private fun tokenizeString(): ParseResult<TokenInfo> {

        val loc = location()

        val str = StringBuilder()

        while (cur() != '"') {
            if (cur() == null) return error("Unexpected end of file")
            str.append(cur())
            advance()
        }
        advance()

        return token(Token.Str(str.toString()), loc)
    }

    private fun tokenizeIdent(): ParseResult<TokenInfo> {

        // handle numbers
        if (cur() == '-' && peek().isNum() || cur().isNum()) {
            return tokenizeNum()
        }

        val ident = StringBuilder()
        val loc = location()

        do {
            ident.append(cur())
            advance()
        } while (cur().isIdent())

        val tokenType = when (ident.toString()) {
            "type" -> Token.Type
            "def" -> Token.Def
            "match" -> Token.Match
            else -> Token.Ident(ident.toString())
        }

        return token(tokenType, loc)
    }

    private fun tokenizeNum(): ParseResult<TokenInfo> {
        val num = StringBuilder()
        val loc = location()

        var sign = 1.0

        if (cur() == '-') {
            sign = -1.0
            advance()
        }

        while (cur().isNum()) {
            num.append(cur())
            advance()
        }

        if (cur() == '.') {
            advance()
            num.append('.')

            if (!cur().isNum()) return error("Expected a number after '.'")

            while (cur().isNum()) {
                num.append(cur())
                advance()
            }
        }

        return token(
            Token.Num(
                sign * num.toString().toDouble()
            ),
            loc,
        )
    }

    private fun skipBlank() {
        while (cur().isBlank()) {
            advance() // eat the #
        }
    }

    private fun skipComment() {
        if (cur() == '#') {
            advance() // eat the #
            do {
                advance()
            } while (cur() == '\n')
            advance()
        }
    }

    private fun Char?.isIdent() = when (this) {
        in 'a'..'z', in 'A'..'Z', in '0'..'9' -> true
        in listOf('_', '+', '-', '*', '/', '%', '?', '!', ':') -> true
        else -> false
    }

    private fun Char?.isNum() = when (this) {
        in '0'..'9' -> true
        else -> false
    }

    private fun Char?.isBlank() = this in listOf(' ', '\t', '\r', '\n')

}