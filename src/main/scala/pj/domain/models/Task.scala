package pj.domain.models

import pj.domain.DomainError.{IOFileProblem, InvalidTaskId, InvalidTime, TaskUsesNonExistentPRT}
import pj.domain.Result
import pj.domain.models.Physical.PhysicalInType

object Task:

  opaque type TaskId = String

  object TaskId:
    def from(id: String): Result[TaskId] =
      if(!id.isBlank && id.startsWith("TSK_")) Right(id)
      else Left(InvalidTaskId(id))

    extension (id: TaskId)
      def to: String = id

  opaque type TaskTime = Int

  object TaskTime:
    def from(time: Int): Result[TaskTime] =
      if (time > 0) Right(time)
      else Left(InvalidTime(time))

    extension (time: TaskTime)
      def to: Int = time  

  opaque type PhysicalResource = PhysicalInType

  object PhysicalResource:
    def from(resource: PhysicalInType): Result[PhysicalResource] = Right(resource)

    extension (physical: PhysicalResource)
      def to: PhysicalInType = physical

  final case class Task(id: TaskId, time: TaskTime, physicalResources: List[PhysicalResource])
