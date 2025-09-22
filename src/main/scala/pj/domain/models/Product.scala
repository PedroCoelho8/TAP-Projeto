package pj.domain.models

import pj.domain.DomainError.*
import pj.domain.Result

object Product: 
  
  opaque type ProductId = String
  
  object ProductId:
    def from(id: String): Result[ProductId] =
      if (id.startsWith("PRD_")) Right(id)
      else Left(InvalidProductId(id))
  
    extension (id: ProductId)
      def to: String = id
      
      
  opaque type ProductName = String
  
  object ProductName:
    def from(name: String): Result[ProductName] =
      if (name.nonEmpty) Right(name)
      else Left(XMLError("Invalid Product name: " + name))
  
    extension (name: ProductName)
      def to: String = name
  
  opaque type TaskReference = String
  
  object TaskReference:
    def from(tskref: String): Result[TaskReference] =
      if (tskref.nonEmpty && tskref.startsWith("TSK_")) Right(tskref)
      else Left(TaskDoesNotExist(tskref))
  
    extension (tskref: TaskReference)
      def to: String = tskref
  
  
  final case class Process(tskref: TaskReference)
  object Process:
    def from(tskref: String): Result[Process] =
      TaskReference.from(tskref).map(Process(_))
  
  
  
  
  final case class Product  (
                                     id: ProductId,
                                     name: ProductName,
                                     taskRefs: List[Process]
                                   )
