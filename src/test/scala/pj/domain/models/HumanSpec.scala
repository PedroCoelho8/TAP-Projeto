package pj.domain.models

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import pj.domain.DomainError

class HumanSpec extends AnyFunSuite with Matchers:

  test("HumanId.from should accept valid id"):
    Human.HumanId.from("HRS_01").isRight shouldBe true

  test("HumanId.from should reject invalid id"):
    Human.HumanId.from("HR_01") shouldBe Left(DomainError.InvalidHumanId("HR_01"))

  test("HumanName.from should accept valid name"):
    Human.HumanName.from("Maria").isRight shouldBe true

  test("HumanName.from should reject blank name"):
    Human.HumanName.from("") shouldBe Left(DomainError.XMLError("Invalid human name: "))

  test("Handles.from should accept valid PhysicalInType"):
    val validType = pj.domain.models.Physical.PhysicalInType.from("PRST 1").getOrElse(fail("Expected valid PhysicalInType"))
    Human.Handles.from(validType).isRight shouldBe true

  test("Human should be created with valid id, name, and handles"):
    val id = Human.HumanId.from("HRS_01").getOrElse(fail("Expected valid HumanId"))
    val name = Human.HumanName.from("Maria").getOrElse(fail("Expected valid HumanName"))
    val physType = pj.domain.models.Physical.PhysicalInType.from("PRST 1").getOrElse(fail("Expected valid PhysicalInType"))
    val handles = List(Human.Handles.from(physType).getOrElse(fail("Expected valid Handles")))

    val human = Human.Human(id, name, handles)

    human.id.to shouldBe "HRS_01"
    human.name.to shouldBe "Maria"
    human.handles.map(_.to) should contain ("PRST 1")
