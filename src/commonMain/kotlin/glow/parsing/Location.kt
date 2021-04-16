package glow.parsing

data class Location(
    val source: String,
    val offset: Int,
    val line: Int,
    val column: Int,
) {
    override fun toString(): String {
        return "$source\n${" ".repeat(column - 1)}ᐱ\n"
    }
}
