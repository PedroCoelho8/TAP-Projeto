package pj.domain.parsers.unit

import org.scalatest.funsuite.AnyFunSuiteLike
import pj.domain.*
import pj.domain.DomainError.*
import pj.domain.parsers.ProductParser
import pj.domain.models.Task.{Task, TaskId, TaskTime}

import scala.xml.XML

class ProductParserTest extends AnyFunSuiteLike:

  private def createTask(id: String, time: Int): (TaskId, Task) =
    val taskId = TaskId.from(id).getOrElse(fail(s"Invalid TaskId: $id"))
    val taskTime = TaskTime.from(time).getOrElse(fail(s"Invalid TaskTime: $time"))
    (taskId, Task(taskId, taskTime, List()))

  test("Valid - Returns product list"):
    val xmlStr =
      """<Products>
        |  <Product id="PRD_01" name="Product1">
        |    <Process tskref="TSK_01"/>
        |  </Product>
        |</Products>""".stripMargin

    val xmlNode = XML.loadString(xmlStr)
    val validTasks = Map(createTask("TSK_01", 5))

    val result = ProductParser.parseProducts(xmlNode, validTasks)

    result.fold(
      err => fail(s"Unexpected failure: $err"),
      products =>
        assert(products.sizeIs == 1)
        products.headOption.fold(fail("Expected at least one product")) { product =>
          assert(product.id.to == "PRD_01")
          assert(product.name.to == "Product1")
          assert(product.taskRefs.headOption.exists(_.tskref.to == "TSK_01"))
        }
    )

  test("Valid - Multiple products parsed correctly"):
    val xmlStr =
      """<Products>
        |  <Product id="PRD_01" name="Product1">
        |    <Process tskref="TSK_01"/>
        |    <Process tskref="TSK_02"/>
        |  </Product>
        |  <Product id="PRD_02" name="Product2">
        |    <Process tskref="TSK_03"/>
        |  </Product>
        |</Products>""".stripMargin

    val xmlNode = XML.loadString(xmlStr)

    val validTasks = Map(
      createTask("TSK_01", 5),
      createTask("TSK_02", 10),
      createTask("TSK_03", 3)
    )

    val result = ProductParser.parseProducts(xmlNode, validTasks)

    result.fold(
      err => fail(s"Unexpected failure: $err"),
      products =>
        assert(products.sizeIs == 2)

        products.find(_.id.to == "PRD_01").fold(fail("Missing PRD_01")) { prd1 =>
          assert(prd1.name.to == "Product1")
          assert(prd1.taskRefs.sizeIs == 2)
          assert(prd1.taskRefs.exists(_.tskref.to == "TSK_01"))
          assert(prd1.taskRefs.exists(_.tskref.to == "TSK_02"))
        }

        products.find(_.id.to == "PRD_02").fold(fail("Missing PRD_02")) { prd2 =>
          assert(prd2.name.to == "Product2")
          assert(prd2.taskRefs.sizeIs == 1)
          assert(prd2.taskRefs.headOption.exists(_.tskref.to == "TSK_03"))
        }
    )

  test("Valid - Task exists, returns Process"):
    val xmlStr =
      """<Products>
        |  <Product id="PRD_01" name="Product1">
        |    <Process tskref="TSK_01"/>
        |  </Product>
        |</Products>""".stripMargin

    val xmlNode = XML.loadString(xmlStr)
    val validTasks = Map(createTask("TSK_01", 5))

    val result = ProductParser.parseProducts(xmlNode, validTasks)

    result.fold(
      err => fail(s"Unexpected failure: $err"),
      products =>
        assert(products.sizeIs == 1)
        products.headOption.fold(fail("Expected at least one product")) { product =>
          assert(product.id.to == "PRD_01")
          assert(product.name.to == "Product1")
          assert(product.taskRefs.headOption.exists(_.tskref.to == "TSK_01"))
        }
    )

  test("Invalid - Invalid Product Id"):
    val xmlStr =
      """<Products>
        |  <Product id="BAD_01" name="InvalidProduct">
        |    <Process tskref="TSK_01"/>
        |  </Product>
        |</Products>""".stripMargin

    val xmlNode = XML.loadString(xmlStr)
    val validTasks = Map(createTask("TSK_01", 5))

    val result = ProductParser.parseProducts(xmlNode, validTasks)

    result.fold(
      {
        case InvalidProductId(id) => assert(id == "BAD_01")
        case other => fail(s"Unexpected error: $other")
      },
      _ => fail("Expected failure due to invalid product ID")
    )

  test("Invalid - Missing Task Reference"):
    val xmlStr =
      """<Products>
        |  <Product id="PRD_01" name="Product1">
        |    <Process tskref="TSK_99"/>
        |  </Product>
        |</Products>""".stripMargin

    val xmlNode = XML.loadString(xmlStr)
    val validTasks = Map(createTask("TSK_01", 5))

    val result = ProductParser.parseProducts(xmlNode, validTasks)

    result.fold(
      {
        case TaskDoesNotExist(tskref) => assert(tskref == "TSK_99")
        case other => fail(s"Unexpected error: $other")
      },
      _ => fail("Expected failure due to invalid task reference")
    )
