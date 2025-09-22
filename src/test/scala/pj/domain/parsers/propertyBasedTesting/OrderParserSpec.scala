//package pj.domain.parsers.propertyBasedTesting
//
//import org.scalatest.matchers.should.Matchers
//import org.scalatest.wordspec.AnyWordSpec
//import org.scalacheck.Gen
//import org.scalacheck.Prop.forAll
//import org.scalacheck.Properties
//import pj.domain.parsers.OrderParser
//import pj.domain.*
//import scala.xml.Elem
//
//class OrderParserSpec extends AnyWordSpec with Matchers:
//
//  def validOrderIdGen: Gen[String] = Gen.alphaStr.map(s => "ORD_" + s.filter(_.isLetterOrDigit))
//  def validQuantityGen: Gen[Int] = Gen.choose(1, 100)
//
//  val validProductIdStrings: List[String] = List("PRD_1", "PRD_2", "PRD_3")
//  val validProductIds: Set[ProductId] = validProductIdStrings.map(ProductId.apply).toSet
//
//  val properties: Properties = new Properties("OrderParser"):
//
//    property("VALID id, product ref, and quantity should parse correctly") =
//      forAll(validOrderIdGen, Gen.oneOf(validProductIdStrings), validQuantityGen) { (id, prdRef, qty) =>
//        val xml: Elem =
//          <Resources>
//            <Order id={id} prdref={prdRef} quantity={qty.toString}/>
//          </Resources>
//
//        val result = OrderParser.parseOrders(xml, validProductIds)
//        result == Right(List(Order(OrderId(id), ProductId(prdRef), OrderQuantity(qty))))
//      }
//
//    property("FAIL id should fail parsing") =
//      forAll(Gen.alphaStr, Gen.oneOf(validProductIdStrings), validQuantityGen) { (id, prdRef, qty) =>
//        val xml =
//          <Resources>
//            <Order id={id} prdref={prdRef} quantity={qty.toString}/>
//          </Resources>
//
//        if (!id.startsWith("ORD_"))
//          val result = OrderParser.parseOrders(xml, validProductIds)
//          result == Left(DomainError.InvalidOrderId(id))
//        else
//          val result = OrderParser.parseOrders(xml, validProductIds)
//          result == Right(List(Order(OrderId(id), ProductId(prdRef), OrderQuantity(qty))))
//      }
//
//    property("FAIL product reference should fail parsing") =
//      forAll(validOrderIdGen, Gen.alphaStr, validQuantityGen) { (id, prdRef, qty) =>
//        val xml =
//          <Resources>
//            <Order id={id} prdref={prdRef} quantity={qty.toString}/>
//          </Resources>
//
//        if (!validProductIds.contains(ProductId(prdRef)))
//          val result = OrderParser.parseOrders(xml, validProductIds)
//          result == Left(DomainError.ProductDoesNotExist(prdRef))
//        else
//          val result = OrderParser.parseOrders(xml, validProductIds)
//          result == Right(List(Order(OrderId(id), ProductId(prdRef), OrderQuantity(qty))))
//      }
//
//    property("FAIL quantity should fail parsing") =
//      forAll(validOrderIdGen, Gen.oneOf(validProductIdStrings), Gen.choose(0, -1)) { (id, prdRef, qty) =>
//        val xml =
//          <Resources>
//            <Order id={id} prdref={prdRef} quantity={qty.toString}/>
//          </Resources>
//
//        if (qty <= 0)
//          val result = OrderParser.parseOrders(xml, validProductIds)
//          result == Left(DomainError.InvalidQuantity(qty))
//        else
//          val result = OrderParser.parseOrders(xml, validProductIds)
//          result == Right(List(Order(OrderId(id), ProductId(prdRef), OrderQuantity(qty))))
//      }
//
//    property("FAIL should fail when quantity is zero or negative") =
//      forAll(validOrderIdGen, Gen.oneOf(validProductIdStrings), Gen.choose(0, -1)) { (id, prdRef, qty) =>
//        val xml =
//          <Resources>
//            <Order id={id} prdref={prdRef} quantity={qty.toString}/>
//          </Resources>
//
//        val result = OrderParser.parseOrders(xml, validProductIds)
//        result == Left(DomainError.InvalidQuantity(qty))
//      }
//
//    property("FAIL should handle missing prdref") =
//      forAll(validOrderIdGen, validQuantityGen) { (id, qty) =>
//        val xml =
//          <Resources>
//            <Order id={id} prdref="" quantity={qty.toString}/>
//          </Resources>
//
//        val result = OrderParser.parseOrders(xml, validProductIds)
//        result == Left(DomainError.ProductDoesNotExist(""))
//      }
//
//    property("FAIL should fail when prdref is missing") =
//      forAll(validOrderIdGen, validQuantityGen) { (id, qty) =>
//        val xml =
//          <Resources>
//            <Order id={id} quantity={qty.toString}/>
//          </Resources>
//
//        val result = OrderParser.parseOrders(xml, validProductIds)
//        result == Left(DomainError.ProductDoesNotExist(""))
//      }
//
//  "OrderParser" should:
//    properties.properties.foreach { case (name, property) =>
//      name in:
//        property.check()
//    }
