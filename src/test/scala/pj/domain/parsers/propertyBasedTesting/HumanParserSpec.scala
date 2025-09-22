//package pj.domain.parsers.propertyBasedTesting
//
//import org.scalatest.matchers.should.Matchers
//import org.scalatest.wordspec.AnyWordSpec
//import org.scalacheck.Gen
//import org.scalacheck.Prop.forAll
//import org.scalacheck.Properties
//import pj.domain.parsers.HumanParser
//import pj.domain.*
//import scala.xml.Elem
//
//class HumanParserSpec extends AnyWordSpec with Matchers:
//
//  def validIdGen: Gen[String] =
//    Gen.nonEmptyListOf(Gen.alphaNumChar).map(s => "HRS_" + s.mkString)
//
//  def invalidIdGen: Gen[String] =
//    Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString).suchThat(!_.startsWith("HRS_"))
//
//  def validNameGen: Gen[String] =
//    Gen.nonEmptyListOf(Gen.alphaNumChar).map("Name" + _.mkString)
//
//  def validHandleTypeGen: Gen[String] =
//    Gen.oneOf("PRST 1", "PRST 2")
//
//  def invalidHandleTypeGen: Gen[String] =
//    Gen.alphaStr.suchThat(t => t.nonEmpty && !Set("PRST 1", "PRST 2").contains(t))
//
//  val validTypes: Set[PhysicalInType] =
//    Set(PhysicalInType("PRST 1"), PhysicalInType("PRST 2"))
//
//  val properties: Properties = new Properties("HumanParser"):
//
//    property("valid id, name, and handles should parse correctly") =
//      forAll(validIdGen, validNameGen, validHandleTypeGen) { (id, name, typ) =>
//        val xml: Elem =
//          <Resources>
//            <Human id={id} name={name}>
//              <Handles type={typ}/>
//            </Human>
//          </Resources>
//
//        val expected = List(HumanIn(HumanId(id), HumanName(name), List(Handles(PhysicalInType(typ)))))
//        HumanParser.parseHumanResources(xml, validTypes) == Right(expected)
//      }
//
//    property("invalid id should fail parsing") =
//      forAll(invalidIdGen, validNameGen, validHandleTypeGen) { (id, name, typ) =>
//        val xml =
//          <Resources>
//            <Human id={id} name={name}>
//              <Handles type={typ}/>
//            </Human>
//          </Resources>
//
//        HumanParser.parseHumanResources(xml, validTypes) == Left(DomainError.InvalidHumanId(id))
//      }
//
//    property("invalid handle type should fail parsing") =
//      forAll(validIdGen, validNameGen, invalidHandleTypeGen) { (id, name, badType) =>
//        val xml =
//          <Resources>
//            <Human id={id} name={name}>
//              <Handles type={badType}/>
//            </Human>
//          </Resources>
//
//        HumanParser.parseHumanResources(xml, validTypes) == Left(DomainError.InvalidHandleType(badType))
//      }
//
//    property("missing name attribute should fail") =
//      forAll(validIdGen, validHandleTypeGen) { (id, typ) =>
//        val xml =
//          <Resources>
//            <Human id={id}>
//              <Handles type={typ}/>
//            </Human>
//          </Resources>
//
//        HumanParser.parseHumanResources(xml, validTypes).isLeft
//      }
//
//    property("missing id attribute should fail") =
//      forAll(validNameGen, validHandleTypeGen) { (name, typ) =>
//        val xml =
//          <Resources>
//            <Human name={name}>
//              <Handles type={typ}/>
//            </Human>
//          </Resources>
//
//        HumanParser.parseHumanResources(xml, validTypes).isLeft
//      }
//
//    property("Handles with no type attribute should fail") =
//      forAll(validIdGen, validNameGen) { (id, name) =>
//        val xml =
//          <Resources>
//            <Human id={id} name={name}>
//              <Handles/>
//            </Human>
//          </Resources>
//
//        HumanParser.parseHumanResources(xml, validTypes).isLeft
//      }
//
//    property("parse multiple valid handles") =
//      forAll(validIdGen, validNameGen) { (id, name) =>
//        val xml =
//          <Resources>
//            <Human id={id} name={name}>
//              <Handles type="PRST 1"/>
//              <Handles type="PRST 2"/>
//            </Human>
//          </Resources>
//
//        val expectedHandles = List("PRST 1", "PRST 2").map(t => Handles(PhysicalInType(t)))
//        HumanParser.parseHumanResources(xml, validTypes) == Right(List(HumanIn(HumanId(id), HumanName(name), expectedHandles)))
//      }
//
//    property("parse human with no handles") =
//      forAll(validIdGen, validNameGen) { (id, name) =>
//        val xml =
//          <Resources>
//            <Human id={id} name={name}/>
//          </Resources>
//
//        HumanParser.parseHumanResources(xml, validTypes) == Right(List(HumanIn(HumanId(id), HumanName(name), List.empty)))
//      }
//
//    property("parse list of valid humans") =
//      val humanPairGen = Gen.zip(validIdGen, validNameGen)
//      forAll(Gen.nonEmptyListOf(humanPairGen)) { humans =>
//        val xml =
//          <Resources>
//            {humans.map { case (id, name) =>
//            <Human id={id} name={name}>
//              <Handles type="PRST 1"/>
//            </Human>
//          }}
//          </Resources>
//
//        val expected = humans.map { case (id, name) =>
//          HumanIn(HumanId(id), HumanName(name), List(Handles(PhysicalInType("PRST 1"))))
//        }
//
//        HumanParser.parseHumanResources(xml, validTypes) == Right(expected)
//      }
//
//    property("idempotency: same XML gives same result twice") =
//      forAll(validIdGen, validNameGen, Gen.oneOf("PRST 1", "PRST 2")) { (id, name, typ) =>
//        val xml =
//          <Resources>
//            <Human id={id} name={name}>
//              <Handles type={typ}/>
//            </Human>
//          </Resources>
//
//        val r1 = HumanParser.parseHumanResources(xml, validTypes)
//        val r2 = HumanParser.parseHumanResources(xml, validTypes)
//        r1 == r2
//      }
//
//  "HumanParser" should:
//    properties.properties.foreach { case (name, property) =>
//      name in:
//        property.check()
//    }
