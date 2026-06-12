import io.eventdriven.strictland.{MessageContract, Snapshot, Json}
import org.scalatest.funsuite.AnyFunSuite

class StrictlandSmokeTest extends AnyFunSuite {

  test("library is consumable from Scala") {
    val contract = MessageContract.specification(Json.Jackson.defaults())
    val step = contract.`given`(Snapshot.forMessageType("SomeEvent"))

    assert(step != null)
  }
}
