package glow.parsing

import com.github.michaelbull.result.Result

typealias ParseResult<T> = Result<T, ParseError>

data class ParseError(
    val message: String,
    val location: Location,
) {
    override fun toString(): String {
        return "$message at line ${location.line}\n\n$location\n\n"
    }
}