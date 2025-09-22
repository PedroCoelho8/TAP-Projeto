package pj.domain.models

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import pj.domain.DomainError

class ProductSpec extends AnyFunSuite with Matchers:

  test("ProductId.from should accept valid id"):
    Product.ProductId.from("PRD_1").isRight shouldBe true

  test("ProductId.from should reject invalid id"):
    Product.ProductId.from("PD_1") shouldBe Left(DomainError.InvalidProductId("PD_1"))

  test("ProductName.from should accept non-empty name"):
    Product.ProductName.from("MyProduct").isRight shouldBe true

  test("ProductName.from should reject empty name"):
    Product.ProductName.from("").toString should include ("Invalid Product name")

  test("TaskReference.from should accept non-empty reference"):
    Product.TaskReference.from("TSK_01").isRight shouldBe true

  test("TaskReference.from should reject empty reference"):
    Product.TaskReference.from("") shouldBe Left(DomainError.TaskDoesNotExist(""))

  test("Process.from should create process from valid TaskReference"):
    val ref = Product.TaskReference.from("TSK_001").getOrElse(fail("Expected valid TaskReference"))
    val process = Product.Process.from("TSK_001").getOrElse(fail("Expected valid Process"))
    process.tskref.to shouldBe ref.to

  test("Product creation with valid fields"):
    val id = Product.ProductId.from("PRD_01").getOrElse(fail("Expected valid ProductId"))
    val name = Product.ProductName.from("ProdName").getOrElse(fail("Expected valid ProductName"))
    val proc1 = Product.Process.from("TSK_001").getOrElse(fail("Expected valid Process"))
    val proc2 = Product.Process.from("TSK_002").getOrElse(fail("Expected valid Process"))

    val product = Product.Product(id, name, List(proc1, proc2))

    product.id.to shouldBe "PRD_01"
    product.name.to shouldBe "ProdName"
    product.taskRefs.map(_.tskref.to) should contain allOf ("TSK_001", "TSK_002")
