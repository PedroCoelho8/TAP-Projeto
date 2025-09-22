package pj.domain.parsers

import pj.domain.Result
import pj.domain.DomainError.*
import pj.xml.XML.*
import pj.domain.models.Task.*
import pj.domain.models.Physical.*
import pj.domain.models.Human.*
import pj.domain.models.Task.TaskId
import pj.domain.models.Task.TaskTime

import scala.util.Try
import scala.xml.Node

object TaskParser {

  def parseTasks(node: Node, validPhysicalResources: List[Physical], humanResourcesNode: Node, milestone: String): Result[List[Task]] =
    val validHumanResources = HumanParser.parseHumanResources(humanResourcesNode, validPhysicalResources.map(_.typ).toSet).getOrElse(List.empty)

    val taskNodes = node \ "Task"
    val parsedTasks = traverse(taskNodes, node => parseTask(node, validPhysicalResources, validHumanResources))

    parsedTasks.flatMap { tasks =>
      checkForDuplicateTaskIds(tasks).flatMap { uniqueTasks =>
        val taskWithMissingResources = uniqueTasks.find(task =>
          task.physicalResources.exists(reqType =>
            validPhysicalResources.count(_.typ == reqType) < task.physicalResources.count(_ == reqType)
          )
        )

        val taskWithMissingHumans = uniqueTasks.find(task =>
          task.physicalResources.exists(reqType =>
            validHumanResources.count(_.handles.exists(_ == reqType)) < task.physicalResources.count(_ == reqType)
          )
        )

        (taskWithMissingResources, taskWithMissingHumans) match
          case (Some(task), _) =>
            val idAsString = task.id match
              case id: PhysicalInId => id
              case id: String => id
            val physicals = task.physicalResources.map(pr => pr.to)
            val finalist = physicals.map(pr => pr.to)
            milestone match
              case "MS01" => Left(ResourceUnavailable(idAsString.to, finalist.distinct.headOption.getOrElse("Unknown")))
              case "MS03" => Left(ImpossibleSchedule) 
              case _ => Left(ResourceUnavailable(idAsString.to, finalist.distinct.headOption.getOrElse("Unknown"))) // Default fallback
          case (_, Some(task)) =>
            val idAsString = task.id match
              case id: PhysicalInId => id
              case id: String => id
            val physicals = task.physicalResources.map(pr => pr.to)
            val finalist = physicals.map(pr => pr.to)
            milestone match
              case "MS01" => Left(ResourceUnavailable(idAsString.to, finalist.distinct.headOption.getOrElse("Unknown")))
              case "MS03" => Left(ImpossibleSchedule) 
              case _ => Left(ResourceUnavailable(idAsString.to, finalist.distinct.headOption.getOrElse("Unknown"))) // Default fallback
          case _ => Right(uniqueTasks)
      }
    }

  private def checkForDuplicateTaskIds(tasks: List[Task]): Result[List[Task]] =
    val ids = tasks.map(_.id)
    val uniqueIds = ids.toSet

    if (ids.lengthIs == uniqueIds.size)
      Right(tasks)
    else
      val duplicateId = ids.groupBy(identity)
        .find(_._2.lengthIs > 1)
        .map(_._1)
        .getOrElse("unknown")
      val idAsString = duplicateId match
        case id: PhysicalInId => id.to
        case id: String => id
      Left(InvalidTaskId(idAsString))

  private def parseTask(node: Node, validPhysicalResources: List[Physical], validHumanResources: List[Human]): Result[Task] =
    for
      idStr <- fromAttribute(node, "id")
      id <- TaskId.from(idStr)
      timeStr <- fromAttribute(node, "time")
      time <- TaskTime.from(timeStr.toInt)
      physicalResourcesNeeded = node \ "PhysicalResource"
      physicalResources <- traverse(physicalResourcesNeeded, validatePhysicalResourceType(_, validPhysicalResources))
    yield
      Task(id, time, physicalResources)

  private def validatePhysicalResourceType(node: Node, validPhysicalResources: List[Physical]): Result[PhysicalResource] =
    fromAttribute(node, "type").flatMap { resourceType =>
      PhysicalInType.from(resourceType).flatMap { resource =>
        if (validPhysicalResources.exists(_.typ == resource))
          val physicalResource = PhysicalResource.from(resource)
          physicalResource
        else
          Left(TaskUsesNonExistentPRT(resourceType))
      }
    }
}