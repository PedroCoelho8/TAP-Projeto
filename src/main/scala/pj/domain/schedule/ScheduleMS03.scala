package pj.domain.schedule

import scala.xml.Elem
import pj.domain.*
import pj.domain.models.Human.*
import pj.domain.models.Physical.*
import pj.domain.models.Task.*
import pj.domain.models.Order.*
import pj.domain.models.Domain.*
import pj.domain.models.Product.*
import pj.domain.parsers.*
import pj.xml.XMLWriter
import scala.annotation.tailrec

object ScheduleMS03 extends Schedule:

  // Add safety limits
  private val MAX_SCHEDULING_TIME = 10000
  private val MAX_ITERATIONS = 10000

  // Resource availability tracking
  case class ResourceState(
                            physicalResources: Map[PhysicalInId, Int], // resource -> available_at_time
                            humanResources: Map[HumanName, Int], // human -> available_at_time
                            orderProgress: Map[OrderId, Map[Int, Int]], // order -> product_number -> next_task_index
                            completedTasks: Map[(OrderId, Int, Int), Int] // (order, product_number, task_index) -> completion_time
                          )

  // Helper case class for pending tasks
  case class PendingTask(
                                  orderId: OrderId,
                                  productNumber: Int,
                                  taskId: String,
                                  taskIndex: Int
                                )

  case class TaskSchedulingInfo(
                                         startTime: Int,
                                         duration: Int,
                                         physicalResources: List[Physical],
                                         humanResources: List[Human]
                                       )

  def create(xml: Elem): Result[Elem] =
    try
      ProductionParser.parseProduction(xml, "MS03") match
        case Left(error) => Left(error)
        case Right(production) =>
          generateOptimalSchedules(production).flatMap(writeXmlResult)
    catch
      case e: Exception =>
        Left(DomainError.XMLError(s"Erro ao criar Schedule: ${e.getMessage}"))

  def fromXmlToSchedules(xml: Elem): Result[List[TaskSchedule]] =
    try
      ProductionParser.parseProduction(xml, "MS03") match
        case Left(error) => Left(error)
        case Right(production) => generateOptimalSchedules(production)
    catch
      case e: Exception =>
        Left(DomainError.XMLError(s"Erro ao gerar schedules: ${e.getMessage}"))

  def generateOptimalSchedules(production: Production): Result[List[TaskSchedule]] =
    val pendingTasks = createPendingTasks(production)
    val initialState = createInitialState(production)

    scheduleTasksIteratively(pendingTasks, initialState, production).map { schedules =>
      schedules.sortBy(s => (s.start.start, s.end.end, extractOrderNumber(s.order)))
    }

  private def createPendingTasks(production: Production): List[PendingTask] =
    for {
      order <- production.orders
      productNumber <- 1 to order.quantity.to
      product <- production.products.find(_.id == order.prdref).toList
      (taskRef, taskIndex) <- product.taskRefs.zipWithIndex
    } yield PendingTask(order.id, productNumber, taskRef.tskref.to, taskIndex)

  private def createInitialState(production: Production): ResourceState =
    ResourceState(
      physicalResources = production.physicalResources.map(p => p.id -> 0).toMap,
      humanResources = production.humanResources.map(h => h.name -> 0).toMap,
      orderProgress = production.orders.map(order =>
        order.id -> (1 to order.quantity.to).map(_ -> 0).toMap
      ).toMap,
      completedTasks = Map.empty
    )

  private def scheduleTasksIteratively(
                                        initialPendingTasks: List[PendingTask],
                                        initialState: ResourceState,
                                        production: Production
                                      ): Result[List[TaskSchedule]] =

    @tailrec
    def loop(
              pendingTasks: List[PendingTask],
              state: ResourceState,
              completedSchedules: List[TaskSchedule],
              currentTime: Int,
              iterations: Int,
              lastProgressTime: Int
            ): Result[List[TaskSchedule]] =
      if (pendingTasks.isEmpty)
        Right(completedSchedules.reverse)
      else if (iterations >= MAX_ITERATIONS)
        Left(DomainError.XMLError(s"Maximum iterations ($MAX_ITERATIONS) exceeded during scheduling"))
      else if (currentTime >= MAX_SCHEDULING_TIME)
        Left(DomainError.XMLError(s"Maximum scheduling time ($MAX_SCHEDULING_TIME) exceeded"))
      else
        val readyTasks = pendingTasks.filter(isTaskReady(_, state))

        if (readyTasks.nonEmpty)
          findBestTaskToSchedule(readyTasks, state, production, pendingTasks) match
            case Some((task, schedulingInfo)) =>
              TaskId.from(task.taskId) match
                case Right(taskId) =>
                  val schedule = createTaskSchedule(task, taskId, schedulingInfo)
                  val updatedState = updateStateAfterScheduling(
                    state,
                    task,
                    schedulingInfo.physicalResources,
                    schedulingInfo.humanResources,
                    schedulingInfo.startTime + schedulingInfo.duration
                  )
                  loop(
                    pendingTasks.filterNot(_ == task),
                    updatedState,
                    schedule :: completedSchedules,
                    currentTime,
                    iterations + 1,
                    currentTime
                  )

                case Left(error) => Left(error)

            case None =>
              val nextTime =
                val t = findNextAvailableTime(state)
                if (t == lastProgressTime) t + 1 else t
              val updatedState = advanceTimeToNext(state, nextTime)
              loop(pendingTasks, updatedState, completedSchedules, nextTime, iterations + 1, lastProgressTime)
        else
          val nextTime =
            val t = findNextAvailableTime(state)
            if (t <= currentTime) currentTime + 1 else t

          if (nextTime - lastProgressTime > 100)
            Left(DomainError.XMLError(s"Scheduling appears stuck - no progress after time $lastProgressTime"))
          else
            val updatedState = advanceTimeToNext(state, nextTime)
            loop(pendingTasks, updatedState, completedSchedules, nextTime, iterations + 1, lastProgressTime)

    loop(initialPendingTasks, initialState, Nil, 0, 0, 0)
  

  private def findBestTaskToSchedule(
                                      readyTasks: List[PendingTask],
                                      state: ResourceState,
                                      production: Production,
                                      allPendingTasks: List[PendingTask]
                                    ): Option[(PendingTask, TaskSchedulingInfo)] =

    val taskOptions = readyTasks.flatMap { task =>
      calculateTaskSchedulingOptions(task, state, production).map(info => (task, info))
    }

    if (taskOptions.isEmpty)
      None
    else
      selectBestTask(taskOptions, state, production, allPendingTasks)

  private def selectBestTask(
                              taskOptions: List[(PendingTask, TaskSchedulingInfo)],
                              state: ResourceState,
                              production: Production,
                              allPendingTasks: List[PendingTask]
                            ): Option[(PendingTask, TaskSchedulingInfo)] =

    val scoredOptions = taskOptions.map { case (task, info) =>
      val score = calculateTaskPriority(task, info, state, production, allPendingTasks)
      (task, info, score)
    }

    scoredOptions match
      case Nil => None
      case firstOption :: rest =>
        val bestOption = rest.foldLeft(firstOption) { (best, current) =>
          if (current._3 < best._3) current else best
        }
        Some((bestOption._1, bestOption._2))

  private def calculateTaskPriority(
                                     task: PendingTask,
                                     info: TaskSchedulingInfo,
                                     state: ResourceState,
                                     production: Production,
                                     allPendingTasks: List[PendingTask]
                                   ): Double =

    val startTimeFactor = info.startTime.toDouble
    val remainingWork = calculateRemainingWorkForOrder(task, allPendingTasks, production)
    val criticalPathFactor = -remainingWork.toDouble
    val resourceUtilizationFactor = if (info.startTime == 0) -10.0 else 0.0
    val orderCompletionFactor = extractOrderNumber(task.orderId) * 0.1
    val durationFactor = -info.duration * 0.01

    startTimeFactor + criticalPathFactor + resourceUtilizationFactor + orderCompletionFactor + durationFactor

  private def calculateRemainingWorkForOrder(
                                              task: PendingTask,
                                              allPendingTasks: List[PendingTask],
                                              production: Production
                                            ): Int =
    val remainingTasks = allPendingTasks.filter { t =>
      t.orderId == task.orderId &&
        t.productNumber == task.productNumber &&
        t.taskIndex >= task.taskIndex
    }
    remainingTasks.map(getTaskDuration(_, production)).sum

  private def calculateTaskSchedulingOptions(
                                              task: PendingTask,
                                              state: ResourceState,
                                              production: Production
                                            ): Option[TaskSchedulingInfo] =

    production.taskResources.find(_.id.to == task.taskId).flatMap { taskDef =>
      val predecessorTime = findPredecessorCompletionTime(task, state, production).getOrElse(0)
      findBestResourceAssignmentSimplified(taskDef, production, state, predecessorTime).map:
        case (physicalOut, humanOut, startTime) =>
          TaskSchedulingInfo(startTime, taskDef.time.to, physicalOut, humanOut)
    }

  // Simplified resource assignment to prevent exponential explosion
  private def findBestResourceAssignmentSimplified(
                                                    taskDef: Task,
                                                    production: Production,
                                                    state: ResourceState,
                                                    minStartTime: Int
                                                  ): Option[(List[Physical], List[Human], Int)] =

    val requiredResourceTypes = taskDef.physicalResources.map(_.to.to)

    if (requiredResourceTypes.isEmpty)
      None
    else
      // Use greedy approach instead of backtracking to prevent stack overflow
      findResourceAssignmentGreedy(requiredResourceTypes, production, state, minStartTime)

  private def findResourceAssignmentGreedy(
                                            requiredTypes: List[String],
                                            production: Production,
                                            state: ResourceState,
                                            minStartTime: Int
                                          ): Option[(List[Physical], List[Human], Int)] =

    case class AssignmentState(
                                usedHumans: Set[HumanName],
                                assignedPhysical: List[Physical],
                                assignedHuman: List[Human],
                                maxStartTime: Int
                              )

    val initialState = AssignmentState(Set.empty, List.empty, List.empty, minStartTime)

    val result = requiredTypes.foldLeft(Option(initialState)):
      case (Some(current), resourceType) =>
        val availablePhysical = getAvailablePhysicalResources(production, state, resourceType)
        val availableHuman = getAvailableHumanResources(production, state, resourceType)
          .filterNot { case (human, _) => current.usedHumans.contains(human.name) }

        if (availablePhysical.isEmpty || availableHuman.isEmpty) None
        else
          val combinations = for {
            (physical, physicalAvailableAt) <- availablePhysical
            (human, humanAvailableAt) <- availableHuman
          } yield
            val earliestStart = math.max(math.max(physicalAvailableAt, humanAvailableAt), minStartTime)
            (physical, human, earliestStart)

          combinations.minByOption(_._3).map:
            case (physical, human, startTime) =>
              AssignmentState(
                usedHumans = current.usedHumans + human.name,
                assignedPhysical = physical :: current.assignedPhysical,
                assignedHuman = human :: current.assignedHuman,
                maxStartTime = math.max(current.maxStartTime, startTime)
              )

      case (None, _) => None

    result.map(s =>
      (s.assignedPhysical.reverse, s.assignedHuman.reverse, s.maxStartTime)
    )

  private def getAvailablePhysicalResources(
                                             production: Production,
                                             state: ResourceState,
                                             requiredType: String
                                           ): List[(Physical, Int)] =
    production.physicalResources
      .filter(_.typ.to == requiredType)
      .map(p => (Physical(p.id, p.typ), state.physicalResources.getOrElse(p.id, 0)))

  private def getAvailableHumanResources(
                                          production: Production,
                                          state: ResourceState,
                                          requiredType: String
                                        ): List[(Human, Int)] =
    production.humanResources
      .filter(_.handles.exists(_.to.to == requiredType))
      .map(h => (Human(h.id, h.name, h.handles), state.humanResources.getOrElse(h.name, 0)))

  private def createTaskSchedule(
                                  task: PendingTask,
                                  taskId: TaskId,
                                  schedulingInfo: TaskSchedulingInfo
                                ): TaskSchedule =
    TaskSchedule(
      order = task.orderId,
      productNumber = ProductNumber(task.productNumber),
      task = taskId,
      start = StartValue(schedulingInfo.startTime),
      end = EndValue(schedulingInfo.startTime + schedulingInfo.duration),
      physicalResources = schedulingInfo.physicalResources,
      humanResources = schedulingInfo.humanResources
    )

  private def isTaskReady(pendingTask: PendingTask, state: ResourceState): Boolean =
    state.orderProgress.get(pendingTask.orderId).fold(false) { productProgress =>
      productProgress.get(pendingTask.productNumber).fold(false) { nextTaskIndex =>
        nextTaskIndex == pendingTask.taskIndex
      }
    }

  private def getTaskDuration(task: PendingTask, production: Production): Int =
    production.taskResources.find(_.id.to == task.taskId).map(_.time.to).getOrElse(0)

  private def updateStateAfterScheduling(
                                          state: ResourceState,
                                          task: PendingTask,
                                          physicalResources: List[Physical],
                                          humanResources: List[Human],
                                          endTime: Int
                                        ): ResourceState =

    // Update all physical resources
    val newPhysicalState = physicalResources.foldLeft(state.physicalResources) { (acc, physical) =>
      acc.updated(physical.id, endTime)
    }

    // Update all human resources
    val newHumanState = humanResources.foldLeft(state.humanResources) { (acc, human) =>
      acc.updated(human.name, endTime)
    }

    val currentOrderProgress = state.orderProgress.getOrElse(task.orderId, Map.empty)
    val newProductProgress = currentOrderProgress.updated(task.productNumber, task.taskIndex + 1)
    val newOrderProgress = state.orderProgress.updated(task.orderId, newProductProgress)
    val taskKey = (task.orderId, task.productNumber, task.taskIndex)
    val newCompletedTasks = state.completedTasks.updated(taskKey, endTime)

    ResourceState(newPhysicalState, newHumanState, newOrderProgress, newCompletedTasks)

  private def findPredecessorCompletionTime(
                                             task: PendingTask,
                                             state: ResourceState,
                                             production: Production
                                           ): Option[Int] =
    if (task.taskIndex == 0)
      Some(0)
    else
      val previousTaskKey = (task.orderId, task.productNumber, task.taskIndex - 1)
      state.completedTasks.get(previousTaskKey)

  private def findNextAvailableTime(state: ResourceState): Int =
    val physicalTimes = state.physicalResources.values
    val humanTimes = state.humanResources.values
    val allTimes = physicalTimes ++ humanTimes
    allTimes.minOption.getOrElse(0)

  private def advanceTimeToNext(state: ResourceState, time: Int): ResourceState =
    state // Time advancement is implicit in our model

  private def extractOrderNumber(orderId: OrderId): Int =
    val numberString = orderId.to.replaceAll("\\D", "")
    if (numberString.nonEmpty)
      numberString.toIntOption.getOrElse(0)
    else
      0

  private def writeXmlResult(schedules: List[TaskSchedule]): Result[Elem] = {
    XMLWriter.toXmlFile(schedules) match
      case Right(xmlString) => Right(scala.xml.XML.loadString(xmlString))
      case Left(err) => Left(err)
  }