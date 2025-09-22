package pj.domain.parsers.unit

import org.scalatest.funsuite.AnyFunSuite
import pj.domain.*
import pj.domain.DomainError.*
import pj.domain.parsers.HumanParser
import pj.domain.models.Human.{Human, HumanId, HumanName, Handles}
import pj.domain.models.Physical.PhysicalInType

import scala.xml.{Node, XML}

class HumanParserTest extends AnyFunSuite:

  def createHumanNode(id: String, name: String, handleTypes: List[String]): Node =
    val handles = handleTypes.map(t => s"""<Handles type="$t" />""").mkString
    XML.loadString(s"""<Human id="$id" name="$name">$handles</Human>""")

  def wrapInResources(nodes: Node*): Node =
    XML.loadString(s"<Resources>${nodes.map(_.toString).mkString}</Resources>")

  val prst1 = PhysicalInType.from("PRST 1").fold(
    _ => fail("Expected PRST 1 to be a valid PhysicalInType"),
    identity
  )

  val prst2 = PhysicalInType.from("PRST 2").fold(
    _ => fail("Expected PRST 2 to be a valid PhysicalInType"),
    identity
  )

  val validTypes = Set(prst1, prst2)

  test("VALID human with single valid handle"):
    val human = createHumanNode("HRS_001", "Alice", List("PRST 1"))
    val result = HumanParser.parseHumanResources(wrapInResources(human), validTypes)

    val expected = for
      id <- HumanId.from("HRS_001")
      name <- HumanName.from("Alice")
      handle <- Handles.from(prst1)
    yield List(Human(id, name, List(handle)))

    assert(result == expected)

  test("VALID human with multiple valid handles"):
    val human = createHumanNode("HRS_002", "Bob", List("PRST 1", "PRST 2"))
    val result = HumanParser.parseHumanResources(wrapInResources(human), validTypes)

    val expected = for
      id <- HumanId.from("HRS_002")
      name <- HumanName.from("Bob")
      handle1 <- Handles.from(prst1)
      handle2 <- Handles.from(prst2)
    yield List(Human(id, name, List(handle1, handle2)))

    assert(result == expected)

  test("FAIL invalid human ID"):
    val human = createHumanNode("INVALID_ID", "Alice", List("PRST 1"))
    val result = HumanParser.parseHumanResources(wrapInResources(human), validTypes)

    assert(result == Left(InvalidHumanId("INVALID_ID")))

  test("FAIL missing name attribute"):
    val humanWithoutName = XML.loadString("""<Human id="HRS_003"><Handles type="PRST 1" /></Human>""")
    val result = HumanParser.parseHumanResources(wrapInResources(humanWithoutName), validTypes)

    assert(result == Left(XMLError("Attribute name is empty/undefined in Human")))


  test("FAIL handle with invalid type (not in validTypes)"):
    val human = createHumanNode("HRS_004", "Charlie", List("PRST 3"))
    val result = HumanParser.parseHumanResources(wrapInResources(human), validTypes)

    assert(result == Left(InvalidHandleType("PRST 3")))

  test("VALID human with no handles"):
    val human = XML.loadString("""<Human id="HRS_005" name="Diana" />""")
    val result = HumanParser.parseHumanResources(wrapInResources(human), validTypes)

    val expected = for
      id <- HumanId.from("HRS_005")
      name <- HumanName.from("Diana")
    yield List(Human(id, name, List()))

    assert(result == expected)

  test("VALID empty resource list"):
    val result = HumanParser.parseHumanResources(XML.loadString("<Resources></Resources>"), validTypes)
    assert(result == Right(List()))

  test("FAIL multiple humans — first with invalid handle type"):
    val validHuman = createHumanNode("HRS_006", "Eve", List("PRST 1"))
    val invalidHuman = createHumanNode("HRS_007", "Frank", List("INVALID_TYPE"))
    val result = HumanParser.parseHumanResources(wrapInResources(invalidHuman, validHuman), validTypes)

    assert(result == Left(InvalidPhysicalType("INVALID_TYPE")))
