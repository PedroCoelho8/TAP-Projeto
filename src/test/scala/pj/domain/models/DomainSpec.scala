package pj.domain.models

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class DomainSpec extends AnyFunSuite with Matchers:

  test("Production should be instantiated correctly with all fields"):
    val physical = Physical.Physical(
      Physical.PhysicalInId.from("PRS_001").getOrElse(fail("Expected valid PhysicalInId")),
      Physical.PhysicalInType.from("PRST_01").getOrElse(fail("Expected valid PhysicalInType"))
    )

    val task = Task.Task(
      Task.TaskId.from("TSK_001").getOrElse(fail("Expected valid TaskId")),
      Task.TaskTime.from(30).getOrElse(fail("Expected valid TaskTime")),
      List(Task.PhysicalResource.from(physical.typ).getOrElse(fail("Expected valid PhysicalResource")))
    )

    val human = Human.Human(
      Human.HumanId.from("HRS_001").getOrElse(fail("Expected valid HumanId")),
      Human.HumanName.from("John").getOrElse(fail("Expected valid HumanName")),
      List(Human.Handles.from(physical.typ).getOrElse(fail("Expected valid Handles")))
    )

    val product = Product.Product(
      Product.ProductId.from("PRD_001").getOrElse(fail("Expected valid ProductId")),
      Product.ProductName.from("Produto 1").getOrElse(fail("Expected valid ProductName")),
      List(Product.Process.from("TSK_REF_01").getOrElse(fail("Expected valid Process")))
    )

    val order = Order.Order(
      Order.OrderId.from("ORD_001").getOrElse(fail("Expected valid OrderId")),
      product.id,
      Order.OrderQuantity.from(10).getOrElse(fail("Expected valid OrderQuantity"))
    )

    val production = Domain.Production(
      physicalResources = List(physical),
      taskResources = List(task),
      humanResources = List(human),
      products = List(product),
      orders = List(order)
    )

    production.physicalResources should have size 1
    production.taskResources should have size 1
    production.humanResources should have size 1
    production.products should have size 1
    production.orders should have size 1


  test("TaskSchedule should store provided values correctly"):
    val orderId = Order.OrderId.from("ORD_001").getOrElse(fail("Expected valid OrderId"))
    val productNumber = Domain.ProductNumber(1)
    val taskId = Task.TaskId.from("TSK_001").getOrElse(fail("Expected valid TaskId"))
    val start = Domain.StartValue(0)
    val end = Domain.EndValue(10)
    val physical = Physical.Physical(
      Physical.PhysicalInId.from("PRS_001").getOrElse(fail("Expected valid PhysicalInId")),
      Physical.PhysicalInType.from("PRST_01").getOrElse(fail("Expected valid PhysicalInType"))
    )
    val human = Human.Human(
      Human.HumanId.from("HRS_001").getOrElse(fail("Expected valid HumanId")),
      Human.HumanName.from("Alice").getOrElse(fail("Expected valid HumanName")),
      List(Human.Handles.from(physical.typ).getOrElse(fail("Expected valid Handles")))
    )

    val schedule = Domain.TaskSchedule(
      order = orderId,
      productNumber = productNumber,
      task = taskId,
      start = start,
      end = end,
      physicalResources = List(physical),
      humanResources = List(human)
    )

    schedule.order.to shouldBe "ORD_001"
    schedule.productNumber.productNumber shouldBe 1
    schedule.task.to shouldBe "TSK_001"
    schedule.start.start shouldBe 0
    schedule.end.end shouldBe 10
    schedule.physicalResources should contain (physical)
    schedule.humanResources should contain (human)
