import io.eventdriven.strictland.{Contract, Snapshot}
import org.scalatest.funsuite.AnyFunSuite

class StrictlandSmokeTest extends AnyFunSuite {

  test("library is consumable from Scala") {
    val contract = Contract.specification()
    val step = contract.`given`(Snapshot.forMessageType("SomeEvent"))

    assert(step != null)
  }
}
