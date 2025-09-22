package pj.domain.models

import pj.domain.models.Physical.PhysicalInType
import pj.domain.{Result, DomainError}

object Human:
  
  opaque type HumanId = String

  object HumanId:
    def from(id: String): Result[HumanId] =
      if (!id.isBlank && id.startsWith("HRS_"))
        Right(id)
      else
        Left(DomainError.InvalidHumanId(id))
        
    extension (id: HumanId)
      def to: String = id

  opaque type HumanName = String

  object HumanName:
    def from(name: String): Result[HumanName] =
      if (!name.isBlank)
        Right(name)
      else
        Left(DomainError.XMLError("Invalid human name: " + name))
    
    extension (name: HumanName)
      def to: String = name

  opaque type Handles = PhysicalInType

  object Handles:
    def from(physicalInType: PhysicalInType): Result[Handles] =
      Right(physicalInType)

    extension (handles: Handles)
      def to: PhysicalInType = handles

  final case class Human(id: HumanId, name: HumanName, handles: List[Handles])