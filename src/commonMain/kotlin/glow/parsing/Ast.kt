package glow.parsing

typealias Ast = List<Definition>

sealed class Definition {

    data class TermDef(
        val name: String,
        val generics: List<String>,
        val functionType: FunctionType,
        val assigments: List<String>,
        val body: List<Expr>,
    ) : Definition()

    data class TypeDef(
        val name: String,
        val generics: List<String>,
        val constructors: List<Constructor>,
    ) : Definition()
}

data class Constructor(
    val name: String,
    val types: List<Type>,
)

data class Type(
    val name: String,
    val generics: List<String>,
)

sealed class FunctionType {

    data class Cell(
        val type: Type,
    ) : FunctionType()

    data class Function(
        val inputs: List<FunctionType>,
        val outputs: List<FunctionType>,
    ) : FunctionType()
}

sealed class Expr {

    data class Assignment(
        val assignments: List<String>,
    ) : Expr()

    data class Term(
        val id: String,
    ) : Expr()

    data class Closure(
        val exprs: List<Expr>,
    ) : Expr()

    data class Match(
        val branches: List<MatchBranch>,
    ) : Expr()

    data class Literal(
        val literal: glow.parsing.Literal,
    ) : Expr()

}

sealed class Literal {

    data class Str(
        val value: String,
    ) : Literal()

    data class Num(
        val value: Double,
    ) : Literal()
}

data class MatchBranch(
    val cons: String,
    val exprs: List<Expr>,
)