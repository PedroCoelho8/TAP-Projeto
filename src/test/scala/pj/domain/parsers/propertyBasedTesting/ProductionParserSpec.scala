//package pj.domain.parsers.propertyBasedTesting
//
//import org.scalatest.matchers.should.Matchers
//import org.scalatest.wordspec.AnyWordSpec
//import org.scalacheck.{Gen, Properties}
//import org.scalacheck.Prop.forAll
//import pj.domain.*
//import pj.domain.parsers.*
//import scala.xml.Elem
//
//class ProductionParserSpec extends AnyWordSpec with Matchers:
//
//  def validProductionXml(productId: String, orderId: String, qty: Int): Elem =
//    <Production>
//      <PhysicalResources>
//        <Physical id="PRS_1" type="PRST 1"/>
//      </PhysicalResources>
//      <Tasks>
//        <Task id="TSK_1" time="1">
//          <PhysicalResource type="PRST 1"/>
//        </Task>
//      </Tasks>
//      <HumanResources>
//        <Human id="HRS_1" name="Bob">
//          <Handles type="PRST 1"/>
//        </Human>
//      </HumanResources>
//      <Products>
//        <Product id={productId} name="ProdName">
//          <Process tskref="TSK_1"/>
//        </Product>
//      </Products>
//      <Orders>
//        <Order id={orderId} prdref={productId} quantity={qty.toString}/>
//      </Orders>
//    </Production>
//
//  val properties = new Properties("ProductionParser"):
//
//    property("valid production should parse successfully") =
//      forAll(Gen.identifier, Gen.identifier, Gen.choose(1, 10)) { (prdIdSuffix, ordIdSuffix, qty) =>
//        val prdId = "PRD_" + prdIdSuffix.filter(_.isLetterOrDigit)
//        val ordId = "ORD_" + ordIdSuffix.filter(_.isLetterOrDigit)
//
//        val xml = validProductionXml(prdId, ordId, qty)
//
//        ProductionParser.parseProduction(xml).isRight
//      }
//
//    property("missing Products node should fail") =
//      forAll(Gen.identifier, Gen.identifier, Gen.choose(1, 10)) { (prdIdSuffix, ordIdSuffix, qty) =>
//        val prdId = "PRD_" + prdIdSuffix.filter(_.isLetterOrDigit)
//        val ordId = "ORD_" + ordIdSuffix.filter(_.isLetterOrDigit)
//
//        val xml =
//          <Production>
//            <PhysicalResources>
//              <Physical id="PRS_1" type="PRST 1"/>
//            </PhysicalResources>
//            <Tasks>
//              <Task id="TSK_1" time="1">
//                <PhysicalResource type="PRST 1"/>
//              </Task>
//            </Tasks>
//            <HumanResources>
//              <Human id="HRS_1" name="Bob">
//                <Handles type="PRST 1"/>
//              </Human>
//            </HumanResources>
//            <!-- Products section missing -->
//            <Orders>
//              <Order id={ordId} prdref={prdId} quantity={qty.toString}/>
//            </Orders>
//          </Production>
//
//        ProductionParser.parseProduction(xml).isLeft
//      }
//
//    property("order referencing unknown product should fail") =
//      forAll(Gen.identifier, Gen.identifier, Gen.choose(1, 10)) { (prdIdSuffix, ordIdSuffix, qty) =>
//        val prdId = "PRD_" + prdIdSuffix.filter(_.isLetterOrDigit)
//        val ordId = "ORD_" + ordIdSuffix.filter(_.isLetterOrDigit)
//
//        val xml =
//          <Production>
//            <PhysicalResources>
//              <Physical id="PRS_1" type="PRST 1"/>
//            </PhysicalResources>
//            <Tasks>
//              <Task id="TSK_1" time="1">
//                <PhysicalResource type="PRST 1"/>
//              </Task>
//            </Tasks>
//            <HumanResources>
//              <Human id="HRS_1" name="Bob">
//                <Handles type="PRST 1"/>
//              </Human>
//            </HumanResources>
//            <Products>
//              <Product id="PRD_OTHER" name="Other">
//                <Process tskref="TSK_1"/>
//              </Product>
//            </Products>
//            <Orders>
//              <Order id={ordId} prdref={prdId} quantity={qty.toString}/>
//            </Orders>
//          </Production>
//
//        ProductionParser.parseProduction(xml) == Left(DomainError.ProductDoesNotExist(prdId))
//      }
//
//    property("idempotency: same xml gives same result") =
//      forAll(Gen.identifier, Gen.identifier, Gen.choose(1, 10)) { (prdIdSuffix, ordIdSuffix, qty) =>
//        val prdId = "PRD_" + prdIdSuffix.filter(_.isLetterOrDigit)
//        val ordId = "ORD_" + ordIdSuffix.filter(_.isLetterOrDigit)
//
//        val xml = validProductionXml(prdId, ordId, qty)
//
//        val result1 = ProductionParser.parseProduction(xml)
//        val result2 = ProductionParser.parseProduction(xml)
//
//        result1 == result2
//      }
//
//  // Integração com ScalaTest
//  "ProductionParser" should :
//    properties.properties.foreach { case (name, prop) =>
//      name in :
//        prop.check()
//    } 
//    
//
