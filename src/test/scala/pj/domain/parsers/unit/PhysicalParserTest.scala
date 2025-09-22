package pj.domain.parsers.unit

import org.scalatest.funsuite.AnyFunSuite
import scala.xml.{Node, XML}
import pj.domain.*
import pj.domain.DomainError.{InvalidPhysicalId, InvalidPhysicalType}
import pj.domain.parsers.PhysicalParser
import pj.domain.models.Physical.*

class PhysicalParserTest extends AnyFunSuite:

  def createPhysicalNode(id: String, typeStr: String): Node =
    XML.loadString(s"""<Physical id="$id" type="$typeStr" />""")

  def createPhysicalsNode(physicals: List[(String, String)]): Node =
    val physicalNodes = physicals.map { case (id, typeStr) =>
      s"""<Physical id="$id" type="$typeStr" />"""
    }.mkString
    XML.loadString(s"<Resources>$physicalNodes</Resources>")

  private def mkPhysicalInId(id: String): PhysicalInId =
    PhysicalInId.from(id).fold(
      e => fail(s"Invalid PhysicalInId in test data: $id, error: $e"),
      identity
    )

  private def mkPhysicalInType(typ: String): PhysicalInType =
    PhysicalInType.from(typ).fold(
      e => fail(s"Invalid PhysicalInType in test data: $typ, error: $e"),
      identity
    )

  test("VALID parse valid physical resource"):
    val physicalNode = createPhysicalNode("PRS_123", "PRST 1")
    val xml = XML.loadString(s"<Resources>${physicalNode.toString}</Resources>")
    val result = PhysicalParser.parsePhysicalResources(xml)

    val expected = Right(List(Physical(mkPhysicalInId("PRS_123"), mkPhysicalInType("PRST 1"))))
    assert(result == expected)

  test("VALID parse multiple valid physical resources"):
    val physicalsList = List(
      ("PRS_123", "PRST 1"),
      ("PRS_456", "PRST 2")
    )
    val node = createPhysicalsNode(physicalsList)
    val result = PhysicalParser.parsePhysicalResources(node)

    val expected = Right(List(
      Physical(mkPhysicalInId("PRS_123"), mkPhysicalInType("PRST 1")),
      Physical(mkPhysicalInId("PRS_456"), mkPhysicalInType("PRST 2"))
    ))
    assert(result == expected)

  test("FAIL when physical ID is invalid"):
    val invalidId = "INVALID_ID"
    val physicalNode = createPhysicalNode(invalidId, "PRST 1")
    val xml = XML.loadString(s"<Resources>${physicalNode.toString}</Resources>")
    val result = PhysicalParser.parsePhysicalResources(xml)

    assert(result == Left(InvalidPhysicalId(invalidId)))

  test("FAIL when physical type is invalid"):
    val invalidType = "Invalid Type"
    val physicalNode = createPhysicalNode("PRS_123", invalidType)
    val xml = XML.loadString(s"<Resources>${physicalNode.toString}</Resources>")
    val result = PhysicalParser.parsePhysicalResources(xml)

    assert(result == Left(InvalidPhysicalType(invalidType)))

  test("FAIL first error when there are more errors"):
    val physicalsList = List(
      ("PRS_123", "PRST 1"),
      ("INVALID", "PRST 2"),
      ("PRS_789", "Invalid Type")
    )
    val node = createPhysicalsNode(physicalsList)
    val result = PhysicalParser.parsePhysicalResources(node)

    assert(result == Left(InvalidPhysicalId("INVALID")))

  test("VALID empty resources"):
    val emptyNode = XML.loadString("<Resources></Resources>")
    val result = PhysicalParser.parsePhysicalResources(emptyNode)

    assert(result == Right(List()))

  test("VALID ID with just the prefix"):
    val physicalNode = createPhysicalNode("PRS_", "PRST 1")
    val xml = XML.loadString(s"<Resources>${physicalNode.toString}</Resources>")
    val result = PhysicalParser.parsePhysicalResources(xml)

    val expected = Right(List(Physical(mkPhysicalInId("PRS_"), mkPhysicalInType("PRST 1"))))
    assert(result == expected)

  test("VALID type with just the prefix"):
    val physicalNode = createPhysicalNode("PRS_123", "PRST ")
    val xml = XML.loadString(s"<Resources>${physicalNode.toString}</Resources>")
    val result = PhysicalParser.parsePhysicalResources(xml)

    val expected = Right(List(Physical(mkPhysicalInId("PRS_123"), mkPhysicalInType("PRST "))))
    assert(result == expected)

  test("FAIL missing id attribute"):
    val nodeWithoutId = XML.loadString("""<Physical type="PRST 1" />""")
    val xml = XML.loadString(s"<Resources>${nodeWithoutId.toString}</Resources>")
    val result = PhysicalParser.parsePhysicalResources(xml)

    assert(result.isLeft)

  test("FAIL missing type attribute"):
    val nodeWithoutType = XML.loadString("""<Physical id="PRS_123" />""")
    val xml = XML.loadString(s"<Resources>${nodeWithoutType.toString}</Resources>")
    val result = PhysicalParser.parsePhysicalResources(xml)

    assert(result.isLeft)
