package pj.domain.generators

import org.scalacheck.*
import pj.domain.models.Product.*
import pj.domain.models.Order.*
import pj.domain.models.Physical.*
import pj.domain.models.Task.*
import pj.domain.models.Human.*
import pj.domain.models.Domain.*
import pj.domain.Result
import org.scalacheck.Gen.const

// Gerador de objetos para as case classes de domínio

object Generators:

  // =================== HELPERS ===================
  extension [A](res: Result[A])
    private def asGen: Gen[A] = res.fold(err => Gen.fail, const)

  // =================== PHYSICAL ===================

  val genPhysicalInIdString: Gen[String] =
    Gen.chooseNum(1, 99).map(n => f"PRS_$n%02d")

  def genPhysicalInId: Gen[PhysicalInId] =
    genPhysicalInIdString.flatMap(PhysicalInId.from(_).fold(_ => Gen.fail, Gen.const))

  val genPhysicalInTypeString: Gen[String] =
    Gen.oneOf("PRST 1", "PRST 2", "PRST 3", "PRST 4", "PRST 5")

  def genPhysicalInType: Gen[PhysicalInType] =
    genPhysicalInTypeString.flatMap(PhysicalInType.from(_).fold(_ => Gen.fail, Gen.const))

  def genPhysical: Gen[Physical] =
    for {
      id <- genPhysicalInId
      typ <- genPhysicalInType
    } yield Physical(id, typ)

  def genPhysicalResource: Gen[PhysicalResource] =
    genPhysicalInType.flatMap(PhysicalResource.from(_).fold(_ => Gen.fail, Gen.const))


  // =================== TASK ===================

  val genTaskIdString: Gen[String] =
    Gen.chooseNum(1, 99).map(n => f"TSK_$n%02d")

  def genTaskId: Gen[TaskId] =
    genTaskIdString.flatMap(TaskId.from(_).fold(_ => Gen.fail, Gen.const))

  val genTaskTimeInt: Gen[Int] = Gen.chooseNum(10, 200)

  def genTaskTime: Gen[TaskTime] =
    genTaskTimeInt.flatMap(TaskTime.from(_).fold(_ => Gen.fail, Gen.const))

  def genTask: Gen[Task] = for {
    id <- genTaskId
    time <- genTaskTime
    physResList <- Gen.listOf(genPhysicalResource)
    task <- Task(id, time, physResList)
  } yield task


  // =================== HUMAN ===================

  def genUniqueHumanIds(n: Int): Gen[List[HumanId]] =
    Gen.pick(n, (1 to 999).map(i => f"HRS_$i%03d")).flatMap { ids =>
      Gen.sequence[List[HumanId], HumanId](ids.toList.map(HumanId.from(_).asGen))
    }

  def genHumanName: Gen[HumanName] =
    Gen.oneOf("Antonio", "Maria", "Manuel", "Susana", "Joao", "Laura")
      .flatMap(HumanName.from(_).fold(_ => Gen.fail, Gen.const))

  def genHandles: Gen[Handles] =
    genPhysicalInType.flatMap(Handles.from(_).fold(_ => Gen.fail, Gen.const))

  def genHumanWithId(id: HumanId, typ: PhysicalInType): Gen[Human] = for {
    name <- genHumanName
    handle <- Handles.from(typ).asGen
    human <- Human(id, name, List(handle))
  } yield human

  // =================== PRODUCT ===================

  val genTaskReference: Gen[TaskReference] = genTaskId.flatMap { tskref =>
    TaskReference.from(tskref.to).fold(_ => Gen.fail, Gen.const)
  }


  def genProcess: Gen[Process] =
    genTaskReference.flatMap(tskref => Process.from(tskref.to).fold(_ => Gen.fail, Gen.const))

  def genProductId: Gen[ProductId] =
    Gen.chooseNum(1, 99)
      .map(n => f"PRD_$n%02d")
      .flatMap(ProductId.from(_).fold(_ => Gen.fail, Gen.const))

  def genProductName: Gen[ProductName] =
    Gen.chooseNum(1, 99)
      .map(n => f"Product $n")
      .flatMap(ProductName.from(_).fold(_ => Gen.fail, Gen.const))

  def genProduct: Gen[Product] = for {
    id <- genProductId
    name <- genProductName
    processes <- Gen.nonEmptyListOf(genProcess)
    product <- Product(id, name, processes)
  } yield product


  // =================== ORDER ===================

  def genOrderId: Gen[OrderId] =
    Gen.chooseNum(1, 99)
      .map(n => f"ORD_$n%02d")
      .flatMap(OrderId.from(_).fold(_ => Gen.fail, Gen.const))

  def genOrderQuantity: Gen[OrderQuantity] =
    Gen.chooseNum(1, 3)
      .flatMap(OrderQuantity.from(_).fold(_ => Gen.fail, Gen.const))

  def genOrder: Gen[Order] = for {
    id <- genOrderId
    prdRef <- genProductId
    quantity <- genOrderQuantity
    order <- Order(id, prdRef, quantity)
  } yield order


  // =================== PRODUCTION ===================

  def genProduction: Gen[Production] = for {
    physicalResources <- Gen.nonEmptyListOf(genPhysical)
      .map(_.distinctBy(_.id))
  
    physicalTypeCounts = physicalResources.groupBy(_.typ).view.mapValues(_.size).toMap.toList
  
    taskResources <- Gen.nonEmptyListOf {
      for {
        id <- genTaskId
        time <- genTaskTime
        taskResources <- Gen.someOf(physicalTypeCounts).suchThat(_.nonEmpty).flatMap { chosen =>
          val perType: List[Gen[List[PhysicalResource]]] = chosen.map { case (typ, maxCount) =>
            Gen.choose(1, maxCount).flatMap { n =>
              Gen.sequence[List[PhysicalResource], PhysicalResource](
                List.fill(n)(PhysicalResource.from(typ).asGen)
              )
            }
          }.toList
          Gen.sequence[List[List[PhysicalResource]], List[PhysicalResource]](perType).map(_.flatten)
        }
      } yield Task(id, time, taskResources)
    }.map(_.distinctBy(_.id))
  
    typeDemand: Map[PhysicalInType, Int] =
      taskResources
        .map(_.physicalResources.groupBy(_.to).view.mapValues(_.size).toMap)
        .foldLeft(Map.empty[PhysicalInType, Int]) { (acc, taskMap) =>
          acc ++ taskMap.map { case (typ, count) =>
            typ -> math.max(count, acc.getOrElse(typ, 0))
          }
        }
  
    totalHumans = typeDemand.values.sum
  
    uniqueHumanIds <- genUniqueHumanIds(totalHumans)
  
    humanResources <-
      val expanded = typeDemand.flatMap { case (typ, count) =>
        List.fill(count)(typ)
      }.toList.zip(uniqueHumanIds) // associa tipo físico ao ID único
  
      Gen.sequence[List[Human], Human](expanded.map { case (typ, id) => genHumanWithId(id, typ) })
  
    products <- Gen.nonEmptyListOf {
      for {
        id <- genProductId
        name <- genProductName
        taskSubset <- Gen.nonEmptyListOf(Gen.oneOf(taskResources)).map(_.distinct)
        processes <- Gen.sequence[List[Process], Process](taskSubset.map(t =>
          TaskReference.from(t.id.to).flatMap(ref => Process.from(ref.to)).asGen
        ))
      } yield Product(id, name, processes)
    }.map(_.distinctBy(_.id))
  
    orders <- Gen.nonEmptyListOf {
      for {
        id <- genOrderId
        p <- Gen.oneOf(products)
        quantity <- genOrderQuantity
      } yield Order(id, p.id, quantity)
    }.map(_.distinctBy(_.id))
  
  } yield Production(physicalResources, taskResources, humanResources, products, orders)
