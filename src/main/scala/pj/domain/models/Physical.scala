package pj.domain.models

import pj.domain.DomainError.{InvalidPhysicalId, InvalidPhysicalType}
import pj.domain.Result

object Physical:

  opaque type PhysicalInId = String

  object PhysicalInId:
    def from(id: String): Result[PhysicalInId] =
      if(!id.isBlank && id.startsWith("PRS_")) Right(id)
      else Left(InvalidPhysicalId(id))

    extension(id: PhysicalInId)
      def to: String = id

  opaque type PhysicalInType = String

  object PhysicalInType:
    def from(typ: String): Result[PhysicalInType] =
      if(!typ.isBlank && typ.startsWith("PRST")) Right(typ)
      else Left(InvalidPhysicalType(typ))

    extension(typ: PhysicalInType)
      def to: String = typ
      
  final case class Physical(id: PhysicalInId, typ: PhysicalInType)