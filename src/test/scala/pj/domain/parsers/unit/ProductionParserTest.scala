package pj.domain.parsers.unit

import org.scalatest.funsuite.AnyFunSuiteLike
import pj.domain.*
import pj.domain.models.Physical.*
import pj.domain.models.Human.*
import pj.domain.models.Task.*
import pj.domain.models.Product.*
import pj.domain.models.Order.*
import pj.domain.models.Domain.*
import pj.domain.parsers.ProductionParser
import scala.xml.XML

class ProductionParserTest extends AnyFunSuiteLike:

  type PhysicalIn = Physical

  object StubPhysicalParser:
    def parsePhysicalResources(node: scala.xml.Node): Result[List[PhysicalIn]] =
      for {
        id1 <- PhysicalInId.from("PRS_1")
        id2 <- PhysicalInId.from("PRS_2")
        typ1 <- PhysicalInType.from("PRST 1")
        typ2 <- PhysicalInType.from("PRST 2")
      } yield List(
        Physical(id1, typ1),
        Physical(id2, typ2)
      )

  object StubTaskParser:
    def parseTasks(node: scala.xml.Node, physicalResources: List[PhysicalIn], humanResourcesNode: scala.xml.Node): Result[List[Task]] =
      for {
        id1 <- TaskId.from("TSK_1")
        id2 <- TaskId.from("TSK_2")
        time1 <- TaskTime.from(100)
        time2 <- TaskTime.from(80)
        pr1typ <- PhysicalInType.from("PRST 1")
        pr2typ <- PhysicalInType.from("PRST 2")
        pr1 <- PhysicalResource.from(pr1typ)
        pr2 <- PhysicalResource.from(pr2typ)
      } yield List(
        Task(id1, time1, List(pr1)),
        Task(id2, time2, List(pr2))
      )

  object StubHumanParser:
    def parseHumanResources(node: scala.xml.Node, physicalTypes: Set[PhysicalInType]): Result[List[Human]] =
      for {
        hId1 <- HumanId.from("HRS_1")
        hId2 <- HumanId.from("HRS_2")
        name1 <- HumanName.from("Antonio")
        name2 <- HumanName.from("Maria")

        prst1 <- PhysicalInType.from("PRST 1")
        prst2 <- PhysicalInType.from("PRST 2")
        prst3 <- PhysicalInType.from("PRST 3")
        prst4 <- PhysicalInType.from("PRST 4")

        handle1a <- Handles.from(prst1)
        handle1b <- Handles.from(prst2)
        handle2a <- Handles.from(prst3)
        handle2b <- Handles.from(prst4)
      } yield List(
        Human(hId1, name1, List(handle1a, handle1b)),
        Human(hId2, name2, List(handle2a, handle2b))
      )

  object StubProductParser:
    def parseProducts(node: scala.xml.Node, validTasks: Map[TaskId, Task]): Result[List[Product]] =
      for {
        id1 <- ProductId.from("PRD_1")
        id2 <- ProductId.from("PRD_2")
        name1 <- ProductName.from("Product 1")
        name2 <- ProductName.from("Product 2")
      } yield List(
        Product(id1, name1, List()),
        Product(id2, name2, List())
      )

  object StubOrderParser:
    def parseOrders(node: scala.xml.Node, validProductIds: Set[ProductId]): Result[List[Order]] =
      for {
        o1 <- OrderId.from("ORD_1")
        o2 <- OrderId.from("ORD_2")
        p1 <- ProductId.from("PRD_1")
        p2 <- ProductId.from("PRD_2")
        q1 <- OrderQuantity.from(1)
        q2 <- OrderQuantity.from(2)
      } yield List(
        Order(o1, p1, q1),
        Order(o2, p2, q2)
      )

  test("ProductionParser - Parses using stub parsers"):
    val xmlStr =
      """<Production>
        |  <PhysicalResources/>
        |  <Tasks/>
        |  <HumanResources/>
        |  <Products/>
        |  <Orders/>
        |</Production>""".stripMargin

    val xmlNode = XML.loadString(xmlStr)

    val result = for {
      physicalResources <- StubPhysicalParser.parsePhysicalResources(xmlNode)
      physicalTypes = physicalResources.map(_.typ).toSet
      humanResources <- StubHumanParser.parseHumanResources(xmlNode, physicalTypes)
      tasks <- StubTaskParser.parseTasks(xmlNode, physicalResources, xmlNode)
      validTasks = tasks.map(task => task.id -> task).toMap
      products <- StubProductParser.parseProducts(xmlNode, validTasks)
      validProductIds = products.map(_.id).toSet
      orders <- StubOrderParser.parseOrders(xmlNode, validProductIds)
    } yield Production(physicalResources, tasks, humanResources, products, orders)

    assert(result.isRight, s"Unexpected failure: ${result.left.getOrElse("")}")
