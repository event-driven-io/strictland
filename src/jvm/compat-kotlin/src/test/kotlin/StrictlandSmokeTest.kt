import io.eventdriven.strictland.Strictland
import kotlin.test.Test
import kotlin.test.assertEquals

class StrictlandSmokeTest {

    @Test
    fun `library is consumable from Kotlin`() {
        val result = Strictland().greet()
        assertEquals("Hello from Strictland!", result)
    }
}
