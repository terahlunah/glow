package glow.parsing

sealed class Token {
    object Type : Token()
    object Def : Token()
    object Match : Token()
    data class Ident(val id: String) : Token()
    data class Str(val s: String) : Token()
    data class Num(val n: Double) : Token()
    object LeftPar : Token()
    object RightPar : Token()
    object LeftChevron : Token()
    object RightChevron : Token()
    object LeftBracket : Token()
    object RightBracket : Token()
    object LeftBraces : Token()
    object RightBraces : Token()
    object Arrow : Token()
    object StoreArrow : Token()
    object Equal : Token()
    object Comma : Token()
    object Semicolon : Token()
    object DoubleQuote : Token()
    object Pipe : Token()
    object Eof : Token()
}

data class TokenInfo(
    val token: Token,
    val location: Location,
)