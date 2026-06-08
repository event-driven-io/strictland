import io.eventdriven.strictland.Contract
import io.eventdriven.strictland.Snapshot
import kotlin.test.Test
import kotlin.test.assertNotNull

class StrictlandSmokeTest {

    @Test
    fun `library is consumable from Kotlin`() {
        val contract = Contract.specification()
        val step = contract.given(Snapshot.forMessageType("SomeEvent"))

        assertNotNull(step)
    }
}
