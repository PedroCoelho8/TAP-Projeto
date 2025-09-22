package pj.domain.models

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import pj.domain.DomainError

class OrderSpec extends AnyFunSuite with Matchers:

  test("OrderId.from should accept valid id"):
    Order.OrderId.from("ORD_123").isRight shouldBe true

  test("OrderId.from should reject invalid id"):
    Order.OrderId.from("123") shouldBe Left(DomainError.InvalidOrderId("123"))

  test("OrderQuantity.from should accept positive quantity"):
    Order.OrderQuantity.from(5).isRight shouldBe true

  test("OrderQuantity.from should reject zero or negative quantity"):
    Order.OrderQuantity.from(0) shouldBe Left(DomainError.InvalidQuantity(0))
    Order.OrderQuantity.from(-1) shouldBe Left(DomainError.InvalidQuantity(-1))

  def validProductId: pj.domain.models.Product.ProductId =
    pj.domain.models.Product.ProductId.from("PRD_01").getOrElse(fail("Expected valid ProductId"))

  test("Order should be created with valid id, product ref and quantity"):
    val id = Order.OrderId.from("ORD_001").getOrElse(fail("Expected valid OrderId"))
    val quantity = Order.OrderQuantity.from(10).getOrElse(fail("Expected valid OrderQuantity"))
    val prdref = validProductId

    val order = Order.Order(id, prdref, quantity)

    order.id.to shouldBe "ORD_001"
    order.quantity.to shouldBe 10
    order.prdref.to shouldBe "PRD_01"
