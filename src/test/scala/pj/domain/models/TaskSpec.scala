package pj.domain.models

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import pj.domain.DomainError
import pj.domain.models.Physical.PhysicalInType

class TaskSpec extends AnyFunSuite with Matchers:

  test("TaskId.from should accept valid id"):
    Task.TaskId.from("TSK_1").isRight shouldBe true

  test("TaskId.from should reject invalid id"):
    Task.TaskId.from("TS_1") shouldBe Left(DomainError.InvalidTaskId("TS_1"))

  test("TaskTime.from should accept positive time"):
    Task.TaskTime.from(10).isRight shouldBe true

  test("TaskTime.from should reject zero or negative time"):
    Task.TaskTime.from(0) shouldBe Left(DomainError.InvalidTime(0))
    Task.TaskTime.from(-1) shouldBe Left(DomainError.InvalidTime(-1))

  def validPhysicalInType: PhysicalInType =
    Physical.PhysicalInType.from("PRST_01").getOrElse(fail("Expected valid PhysicalInType"))

  test("PhysicalResource.from should always succeed with valid PhysicalInType"):
    val phys = validPhysicalInType
    val resource = Task.PhysicalResource.from(phys)
    resource.isRight shouldBe true
    resource.getOrElse(fail()).to shouldBe phys

  test("Task creation with valid fields"):
    val id = Task.TaskId.from("TSK_01").getOrElse(fail("Expected valid TaskId"))
    val time = Task.TaskTime.from(15).getOrElse(fail("Expected valid TaskTime"))
    val physRes = List(Task.PhysicalResource.from(validPhysicalInType).getOrElse(fail("Expected valid PhysicalResource")))

    val task = Task.Task(id, time, physRes)



    task.id.to shouldBe "TSK_01"
    task.time.to shouldBe 15
    task.physicalResources match
      case head :: _ => head.to.to shouldBe "PRST_01"
      case Nil       => fail("Expected at least one PhysicalResource")

