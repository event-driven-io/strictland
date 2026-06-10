import io.eventdriven.strictland.{MessageContract, Snapshot}
import org.scalatest.funsuite.AnyFunSuite

class StrictlandSmokeTest extends AnyFunSuite {

  test("library is consumable from Scala") {
    val contract = MessageContract.specification()
    val step = contract.`given`(Snapshot.forMessageType("SomeEvent"))

    assert(step != null)
  }
}
