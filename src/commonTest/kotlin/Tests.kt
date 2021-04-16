import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import glow.parsing.Parser
import kotlin.test.Test

class Tests {

    @Test
    fun tests() {

        val source = "def major (Num) = 18 >"

        val parser = Parser(source)
        val ast = parser.parse()

        when (ast) {
            is Ok -> println(ast.value)
            is Err -> println(ast.error)
        }
    }
}