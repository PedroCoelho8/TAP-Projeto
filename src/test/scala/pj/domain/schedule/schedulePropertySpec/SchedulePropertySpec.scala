package pj.domain.schedule.schedulePropertySpec

import org.scalacheck.*
import org.scalacheck.Prop.forAll
import org.scalacheck.Properties
import pj.domain.*
import pj.domain.generators.Generators.*
import pj.domain.models.Domain.TaskSchedule
import pj.domain.models.Order.OrderId
import pj.domain.schedule.ScheduleMS01

object SchedulePropertySpec extends Properties("Schedule"):

  property("no overlapping use of physical resources on generated productions") = forAll(genProduction) { production =>
    ScheduleMS01.createSchedules(production) match
      case Left(_) => false
      case Right(schedules) =>
        val tasksGroupedByResource = schedules.flatMap { sched =>
          sched.physicalResources.map(pr => (pr.id, sched.start.start, sched.end.end))
        }.groupBy(_._1)

        tasksGroupedByResource.forall { case (_, usages) =>
          usages.combinations(2).forall:
            case List((_, start1, end1), (_, start2, end2)) =>
              end1 <= start2 || end2 <= start1
            case _ => true
        }
  }

  property("no overlapping use of human resources") = forAll(genProduction) { production =>
    ScheduleMS01.createSchedules(production) match
      case Left(_) => false
      case Right(schedules) =>
        val tasksGroupedByHuman = schedules.flatMap { sched =>
          sched.humanResources.map(hr => (hr.name, sched.start.start, sched.end.end))
        }.groupBy(_._1)

        tasksGroupedByHuman.forall { case (_, usages) =>
          usages.combinations(2).forall:
            case List((_, start1, end1), (_, start2, end2)) =>
              end1 <= start2 || end2 <= start1
            case _ => true
        }
  }


  property("all tasks referenced in product processes are scheduled") = forAll(genProduction) { production =>
    ScheduleMS01.createSchedules(production) match
      case Left(_) => false
      case Right(taskSchedules) =>
        val scheduledTaskIds: Set[String] = taskSchedules.map(_.task.to).toSet

        val allTaskRefsFromOrders: Set[String] = production.orders.flatMap { order =>
          production.products.find(_.id == order.prdref).toList.flatMap { product =>
            product.taskRefs.map(_.tskref.to)
          }
        }.toSet

        allTaskRefsFromOrders.subsetOf(scheduledTaskIds)
  }


  property("each task schedule duration matches task time") = forAll(genProduction) { production =>
    ScheduleMS01.createSchedules(production) match
      case Left(_) => false
      case Right(taskSchedules) =>
        taskSchedules.forall { sched =>
          val taskOpt = production.taskResources.find(_.id == sched.task)
          taskOpt.exists(task => (sched.end.end - sched.start.start) == task.time.to)
        }
  }


  property("all scheduled physical resources exist in production") = forAll(genProduction) { production =>
    ScheduleMS01.createSchedules(production) match
      case Left(_) => false
      case Right(schedules) =>
        val existingPhysIds = production.physicalResources.map(_.id).toSet
        schedules.flatMap(_.physicalResources).forall(res => existingPhysIds.contains(res.id))
  }

  property("task order in product process is respected") = forAll(genProduction) { production =>
    ScheduleMS01.createSchedules(production) match
      case Left(_) => false
      case Right(schedules) =>
        val scheduleMap: Map[(OrderId, String), TaskSchedule] =
          schedules.map(s => (s.order, s.task.to) -> s).toMap

        production.orders.flatMap { order =>
          production.products.find(_.id == order.prdref).map { product =>
            // taskRefs é uma lista de Process(tskref: TaskReference)
            (order, product.taskRefs.map(_.tskref.to))
          }
        }.forall { case (order, taskRefIds: List[String]) =>
          val taskSchedules = taskRefIds.flatMap(ref => scheduleMap.get((order.id, ref)))

          taskSchedules
            .map(_.start.start)
            .sliding(2)
            .forall:
              case Seq(t1, t2) => t1 <= t2
              case _ => true
        }
  }
  property("no overlapping tasks in the schedule overall") = forAll(genProduction) { production =>
    ScheduleMS01.createSchedules(production) match
      case Left(_) => false
      case Right(schedules) =>
        schedules.combinations(2).forall:
          case List(s1, s2) =>
            s1.end.end <= s2.start.start || s2.end.end <= s1.start.start
          case _ => true
  }
  property("tasks are repeated according to order quantity") = forAll(genProduction) { production =>
    ScheduleMS01.createSchedules(production) match
      case Left(_) => false
      case Right(schedules) =>
        production.orders.forall { order =>
          val relatedTasksCount = schedules.count(_.order == order.id)
          val expectedCount = order.quantity.to *
            production.products.find(_.id == order.prdref).map(_.taskRefs.size).getOrElse(0)

          relatedTasksCount == expectedCount
        }
  }

  property("all human resources assigned to a task can operate the corresponding physical resource") = forAll(genProduction) { production =>
    ScheduleMS01.createSchedules(production) match
      case Left(_) => false
      case Right(schedules) =>
        schedules.forall { sched =>
          sched.humanResources.forall { human =>
            sched.physicalResources.forall { physical =>
              production.humanResources.find(_.id == human.id).exists { hr =>
                hr.handles.exists(handle => handle.to.to == physical.typ.to)
              }
            }
          }
        }
  }

  property("one-to-one mapping between human and physical resources per task") = forAll(genProduction) { production =>
    ScheduleMS01.createSchedules(production) match
      case Left(_) => true
      case Right(schedules) =>
        schedules.forall { sched =>
          val numPhysicals = sched.physicalResources.map(_.id).distinct.size
          val numHumans = sched.humanResources.map(_.id).distinct.size
          numPhysicals == numHumans
        }
  }

  property("product numbers are complete and correct for each order") = forAll(genProduction) { production =>
    ScheduleMS01.createSchedules(production) match
      case Left(_) => true
      case Right(schedules) =>
        production.orders.forall { order =>
          val scheduledProductNumbers = schedules
            .filter(_.order == order.id)
            .map(_.productNumber.productNumber)
            .toSet

          val expectedProductNumbers = (1 to order.quantity.to).toSet

          scheduledProductNumbers == expectedProductNumbers
        }
  }

  property("all scheduled tasks must have a non-negative start time") = forAll(genProduction) { production =>
    ScheduleMS01.createSchedules(production) match
      case Left(_) => true
      case Right(schedules) =>
        schedules.forall(_.start.start >= 0)
  }