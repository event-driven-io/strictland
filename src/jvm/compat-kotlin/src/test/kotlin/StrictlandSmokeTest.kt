import io.eventdriven.strictland.Json
import io.eventdriven.strictland.MessageContract
import io.eventdriven.strictland.MessageSnapshot
import kotlin.test.Test
import kotlin.test.assertNotNull

class StrictlandSmokeTest {

    @Test
    fun `library is consumable from Kotlin`() {
        val contract = MessageContract.specification(Json.Jackson.defaults())
        val step = contract.given(MessageSnapshot.forMessageType("SomeEvent"))

        assertNotNull(step)
    }
}
