package pj.domain.models

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import pj.domain.DomainError

class PhysicalSpec extends AnyFunSuite with Matchers:

  test("PhysicalInId.from should accept valid id"):
    Physical.PhysicalInId.from("PRS_01").isRight shouldBe true

  test("PhysicalInId.from should reject invalid id"):
    Physical.PhysicalInId.from("PRST_01") shouldBe Left(DomainError.InvalidPhysicalId("PRST_01"))

  test("PhysicalInType.from should accept valid type"):
    Physical.PhysicalInType.from("PRST 1").isRight shouldBe true

  test("PhysicalInType.from should reject invalid type"):
    Physical.PhysicalInType.from("PRS_1") shouldBe Left(DomainError.InvalidPhysicalType("PRS_1"))

  test("Physical should be created with valid id and type"):
    val id = Physical.PhysicalInId.from("PRS_01")
      .getOrElse(fail("Expected valid PhysicalInId"))
    val typ = Physical.PhysicalInType.from("PRST 1")
      .getOrElse(fail("Expected valid PhysicalInType"))
    val physical = Physical.Physical(id, typ)

    if( physical.typ.to == "PRST 1" && physical.id.to == "PRS_01" )
      succeed
    else
      fail("Physical object was not created correctly")


  test("Physical should be created with valid id/type"):
    val id = Physical.PhysicalInId.from("PRS_01").getOrElse(fail("Expected valid PhysicalInId"))
    val typ = Physical.PhysicalInType.from("PRST 1").getOrElse(fail("Expected valid PhysicalInType"))
    val physical = Physical.Physical(id, typ)
  
    physical.id.to shouldBe "PRS_01"
    physical.typ.to shouldBe "PRST 1"
