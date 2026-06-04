import io.eventdriven.strictland.Strictland
import org.scalatest.funsuite.AnyFunSuite

class StrictlandSmokeTest extends AnyFunSuite {

  test("library is consumable from Scala") {
    val result = Strictland().greet()
    assert(result == "Hello from Strictland!")
  }
}
