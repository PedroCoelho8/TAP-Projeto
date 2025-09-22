package pj.domain.parsers.unit

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.Assertions.fail
import scala.xml.{Node, XML}
import pj.domain.*
import pj.domain.DomainError.*
import pj.domain.parsers.OrderParser
import pj.domain.models.Order.{Order, OrderId, OrderQuantity}
import pj.domain.models.Product.ProductId

class OrderParserTest extends AnyFunSuite:

  def createOrderNode(id: String, prdRef: String, quantity: Int): Node =
    XML.loadString(s"""<Order id="$id" prdref="$prdRef" quantity="$quantity" />""")

  def createOrdersNode(orders: List[(String, String, Int)]): Node =
    val orderNodes = orders.map { case (id, prdRef, quantity) =>
      s"""<Order id="$id" prdref="$prdRef" quantity="$quantity" />"""
    }.mkString
    XML.loadString(s"<Orders>$orderNodes</Orders>")

  private def mkProductId(id: String): ProductId =
    ProductId.from(id).fold(
      e => fail(s"Invalid ProductId in test data: $id, error: $e"),
      identity
    )

  private def mkOrderId(id: String): OrderId =
    OrderId.from(id).fold(
      e => fail(s"Invalid OrderId in test data: $id, error: $e"),
      identity
    )

  private def mkOrderQuantity(qty: Int): OrderQuantity =
    OrderQuantity.from(qty).fold(
      e => fail(s"Invalid OrderQuantity in test data: $qty, error: $e"),
      identity
    )

  val validProductIds: Set[ProductId] = Set(mkProductId("PRD_1"), mkProductId("PRD_2"))

  test("VALID parse single valid order"):
    val orderNode = createOrderNode("ORD_1", "PRD_1", 10)
    val xml = XML.loadString(s"<Orders>${orderNode.toString}</Orders>")
    val result = OrderParser.parseOrders(xml, validProductIds)

    val expected = Right(List(Order(mkOrderId("ORD_1"), mkProductId("PRD_1"), mkOrderQuantity(10))))
    assert(result == expected)

  test("VALID parse multiple valid orders"):
    val ordersList = List(
      ("ORD_1", "PRD_1", 5),
      ("ORD_2", "PRD_2", 7)
    )
    val node = createOrdersNode(ordersList)
    val result = OrderParser.parseOrders(node, validProductIds)

    val expected = Right(List(
      Order(mkOrderId("ORD_1"), mkProductId("PRD_1"), mkOrderQuantity(5)),
      Order(mkOrderId("ORD_2"), mkProductId("PRD_2"), mkOrderQuantity(7))
    ))
    assert(result == expected)

  test("FAIL invalid order id"):
    val orderNode = createOrderNode("INVALID_ID", "PRD_1", 10)
    val xml = XML.loadString(s"<Orders>${orderNode.toString}</Orders>")
    val result = OrderParser.parseOrders(xml, validProductIds)
    assert(result == Left(InvalidOrderId("INVALID_ID")))

  test("FAIL product does not exist"):
    val orderNode = createOrderNode("ORD_1", "PRD_0", 10)
    val xml = XML.loadString(s"<Orders>${orderNode.toString}</Orders>")
    val result = OrderParser.parseOrders(xml, validProductIds)
    assert(result == Left(ProductDoesNotExist("PRD_0")))

  test("FAIL quantity is zero"):
    val orderNode = createOrderNode("ORD_1", "PRD_1", 0)
    val xml = XML.loadString(s"<Orders>${orderNode.toString}</Orders>")
    val result = OrderParser.parseOrders(xml, validProductIds)
    assert(result == Left(InvalidQuantity(0)))

  test("FAIL quantity is negative"):
    val orderNode = createOrderNode("ORD_1", "PRD_1", -5)
    val xml = XML.loadString(s"<Orders>${orderNode.toString}</Orders>")
    val result = OrderParser.parseOrders(xml, validProductIds)
    assert(result == Left(InvalidQuantity(-5)))

  test("FAIL first error when there are multiple orders with errors"):
    val ordersList = List(
      ("INVALID_ID", "PRD_1", 10),
      ("ORD_2", "UNKNOWN", 5),
      ("ORD_3", "PRD_2", -1)
    )
    val node = createOrdersNode(ordersList)
    val result = OrderParser.parseOrders(node, validProductIds)
    assert(result == Left(InvalidOrderId("INVALID_ID")))

  test("VALID empty orders list"):
    val emptyNode = XML.loadString("<Orders></Orders>")
    val result = OrderParser.parseOrders(emptyNode, validProductIds)
    assert(result == Right(List()))

  test("FAIL missing id attribute"):
    val nodeWithoutId = XML.loadString("""<Order prdref="PRD_1" quantity="5" />""")
    val xml = XML.loadString(s"<Orders>${nodeWithoutId.toString}</Orders>")
    val result = OrderParser.parseOrders(xml, validProductIds)
    assert(result.isLeft)

  test("FAIL missing prdref attribute"):
    val nodeWithoutPrd = XML.loadString("""<Order id="ORD_1" quantity="5" />""")
    val xml = XML.loadString(s"<Orders>${nodeWithoutPrd.toString}</Orders>")
    val result = OrderParser.parseOrders(xml, validProductIds)
    assert(result.isLeft)

  test("FAIL missing quantity attribute"):
    val nodeWithoutQuantity = XML.loadString("""<Order id="ORD_1" prdref="PRD_1" />""")
    val xml = XML.loadString(s"<Orders>${nodeWithoutQuantity.toString}</Orders>")
    val result = OrderParser.parseOrders(xml, validProductIds)
    assert(result.isLeft)

  test("VALID order with attributes in different order"):
    val node = XML.loadString("""<Order quantity="3" prdref="PRD_1" id="ORD_1" />""")
    val xml = XML.loadString(s"<Orders>${node.toString}</Orders>")
    val result = OrderParser.parseOrders(xml, validProductIds)
    val expected = Right(List(Order(mkOrderId("ORD_1"), mkProductId("PRD_1"), mkOrderQuantity(3))))
    assert(result == expected)
