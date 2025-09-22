//package pj.domain.parsers.propertyBasedTesting
//
//import org.scalacheck.{Gen, Properties}
//import org.scalacheck.Prop.forAll
//import pj.domain.parsers.PhysicalParser
//import pj.domain.*
//
//import scala.xml.Elem
//
//object PhysicalParserSpec extends Properties("Physical"):
//
//  def validIdGen: Gen[String] =
//    Gen.nonEmptyListOf(Gen.alphaNumChar).map(s => "PRS_" + s.mkString)
//
//  def validTypeGen: Gen[String] =
//    Gen.nonEmptyListOf(Gen.alphaNumChar).map(s => "PRST " + s.mkString)
//
//  def invalidIdGen: Gen[String] =
//    Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString).suchThat(!_.startsWith("PRS_"))
//
//  def invalidTypeGen: Gen[String] =
//    Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString).suchThat(!_.startsWith("PRST "))
//
//  property("VALID id and type should parse correctly") = forAll(validIdGen, validTypeGen) { (id, typ) =>
//      val xml: Elem =
//        <Resources>
//          <Physical id={id} type={typ}/>
//        </Resources>
//
//      val result = PhysicalParser.parsePhysicalResources(xml)
//      result == Right(List(PhysicalIn(PhysicalInId(id), PhysicalInType(typ))))
//  }
//
//  property("Parse multiple physical resources correctly") = forAll(Gen.listOfN(2, Gen.zip(validIdGen, validTypeGen))) { physicalPairs =>
//      val physicalIds = physicalPairs.map(_._1)
//      val uniquePhysicalIds = physicalIds.distinct
//
//      if (uniquePhysicalIds.sizeIs == physicalPairs.size)
//        val xml: Elem =
//          <Resources>
//            {physicalPairs.map { case (id, typ) =>
//              <Physical id={id} type={typ}/>
//          }}
//          </Resources>
//
//        val result = PhysicalParser.parsePhysicalResources(xml)
//
//        val expectedPhysicals = physicalPairs.map { case (id, typ) =>
//          PhysicalIn(PhysicalInId(id), PhysicalInType(typ))
//        }
//
//        result == Right(expectedPhysicals)
//      else
//        true
//  }
//
//  property("Same XML gives same result twice") = forAll(validIdGen, validTypeGen) { (id, typ) =>
//      val xml: Elem =
//        <Resources>
//          <Physical id={id} type={typ}/>
//        </Resources>
//
//      val result1 = PhysicalParser.parsePhysicalResources(xml)
//      val result2 = PhysicalParser.parsePhysicalResources(xml)
//
//      result1 == result2
//  }
//
//  property("FAIL id should fail parsing") = forAll(invalidIdGen, validTypeGen) { (id, typ) =>
//      val xml: Elem =
//        <Resources>
//          <Physical id={id} type={typ}/>
//        </Resources>
//
//      val result = PhysicalParser.parsePhysicalResources(xml)
//      result == Left(DomainError.InvalidPhysicalId(id))
//  }
//
//  property("FAIL type should fail parsing") = forAll(validIdGen, invalidTypeGen) { (id, typ) =>
//      val xml: Elem =
//        <Resources>
//          <Physical id={id} type={typ}/>
//        </Resources>
//
//      val result = PhysicalParser.parsePhysicalResources(xml)
//      result == Left(DomainError.InvalidPhysicalType(typ))
//  }
//
//  property("FAIL duplicate physical IDs should fail parsing") = forAll(validIdGen, validTypeGen, validTypeGen) { (duplicateId, type1, type2) =>
//      val xml: Elem =
//        <Resources>
//          <Physical id={duplicateId} type={type1}/>
//          <Physical id={duplicateId} type={type2}/>
//        </Resources>
//
//      val result = PhysicalParser.parsePhysicalResources(xml)
//      result == Left(DomainError.InvalidPhysicalId(duplicateId))
//  }