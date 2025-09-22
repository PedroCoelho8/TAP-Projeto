package pj.domain.schedule

import pj.domain.DomainError.ResourceUnavailable
import scala.xml.Elem
import pj.domain.{DomainError, Result}
import pj.domain.models.Domain.{EndValue, ProductNumber, Production, StartValue, TaskSchedule}
import pj.domain.parsers.*
import pj.xml.XMLWriter
import pj.domain.models.Physical.*
import pj.domain.models.Human.*
import pj.domain.models.Product.*
import pj.domain.models.Order.*
import pj.domain.models.Task.*

object ScheduleMS01 extends Schedule:

  def create(xml: Elem): Result[Elem] =
    try
      ProductionParser.parseProduction(xml, "MS01") match
        case Left(error) => Left(error)
        case Right(production) =>

          val initial: Result[(Int, List[TaskSchedule])] = Right((0, List.empty[TaskSchedule]))

          val resultSchedules: Result[(Int, List[TaskSchedule])] = production.orders.foldLeft(initial):
            case (accResult, order) =>
              accResult match
                case Left(err) => Left(err)
                case Right((globalTime, accumulatedSchedules)) =>
                  generateSchedulesForOrder(order, production, globalTime).map:
                    case (latestEndTime, orderSchedules) =>
                      (latestEndTime, accumulatedSchedules ++ orderSchedules)

          resultSchedules.flatMap { case (_, allTaskSchedules) =>
            XMLWriter.toXmlFile(allTaskSchedules) match
              case Right(xmlString) => Right(scala.xml.XML.loadString(xmlString))
              case Left(err)        => Left(err)
          }

    catch case e: Exception =>
      Left(DomainError.XMLError(s"Erro ao criar Schedule: ${e.getMessage}"))



  private def generateSchedulesForOrder(order: Order, production: Production, startTime: Int): Result[(Int, List[TaskSchedule])] =
   
    (1 to order.quantity.to).foldLeft[Result[(Int, List[TaskSchedule])]](Right((startTime, List.empty))):
      case (accResult, productNumber) =>
        accResult match
          case Left(err) => Left(err)
          case Right((currentTime, schedules)) =>
            generateSchedulesForProduct(order, productNumber, production, currentTime).map:
              case (endTime, productSchedules) =>
                (endTime, schedules ++ productSchedules)


  private def generateSchedulesForProduct(order: Order, productNumber: Int, production: Production, startTime: Int): Result[(Int, List[TaskSchedule])] =
  
    production.products.find(_.id == order.prdref) match
      case None => Right((startTime, List.empty))
      case Some(product) =>
        product.taskRefs.foldLeft[Result[(Int, List[TaskSchedule])]](Right((startTime, List.empty))) {
          case (accResult, taskRef) =>
            accResult match
              case Left(err) => Left(err)
              case Right((start, schedules)) =>
                TaskId.from(taskRef.tskref.to) match
                  case Right(taskId) =>
                    production.taskResources.find(_.id == taskId) match
                      case Some(task) =>
                        createTaskSchedule(order, productNumber, task, production, start).map { schedule =>
                          (schedule.end.end, schedule :: schedules)
                        }
                      case None => Right((start, schedules))
                  case Left(_) => Right((start, schedules))
        }.map { case (endTime, schedules) => (endTime, schedules.reverse) }


  private def createTaskSchedule(order: Order, productNumber: Int, task: Task, production: Production, start: Int): Result[TaskSchedule] =
    val allAvailablePhysicals = production.physicalResources
    val initialFoldState: (List[Physical], List[Physical]) = (allAvailablePhysicals, Nil)

    val (_, assignedPhysicalsList) = task.physicalResources.foldLeft(initialFoldState):
      case ((available, assigned), requiredType) =>
        available.find(_.typ == requiredType) match
          case Some(foundPhysical) =>
            (available.filterNot(_ == foundPhysical), foundPhysical :: assigned)
          case None =>
            (available, assigned)

    val physicals = assignedPhysicalsList.reverse

    assignHumansToPhysicals(task.id, physicals, production).map { assignedHumans =>
      TaskSchedule(
        order = order.id,
        productNumber = ProductNumber(productNumber),
        task = task.id,
        start = StartValue(start),
        end = EndValue(start + task.time.to),
        physicalResources = physicals,
        humanResources = assignedHumans
      )
    }


  private def assignHumansToPhysicals(taskId: TaskId, physicals: List[Physical], production: Production): Result[List[Human]] =
    val physicalsWithTypes = physicals.flatMap(p => production.physicalResources.find(_.id.to == p.id.to).map(r => (p, r.typ)))

    val initial: Result[(List[Human], List[Human])] = Right((production.humanResources, List.empty))

    val result: Result[(List[Human], List[Human])] = physicalsWithTypes.foldLeft(initial):
      case (Left(err), _) => Left(err)
      case (Right((availableHumans, humansList)), (physical, requiredType)) =>
        availableHumans.find(h => h.handles.contains(requiredType)) match
          case Some(human) =>
            Right((availableHumans.filterNot(_ == human), human :: humansList))
          case None =>
            Left(ResourceUnavailable(taskId.to, requiredType.to))

    result.map { case (_, assignedHumans) => assignedHumans.reverse}


  // This function creates schedules for all orders in the production, its used to generate the final schedule based on a production object. The main function create uses a XML input to create a production object, This function skips that step and uses the production created on the generators (Milestone MS02).

  def createSchedules(production: Production): Result[List[TaskSchedule]] =
    production.orders.foldLeft[Result[(Int, List[TaskSchedule])]](Right((0, Nil))) {
      case (Left(err), _) => Left(err)
      case (Right((startTime, schedules)), order) =>
        generateSchedulesForOrder(order, production, startTime).map:
          case (newEnd, newSchedules) => (newEnd, schedules ++ newSchedules)
    }.map(_._2)
