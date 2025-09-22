package pj.domain.parsers

import pj.domain.Result
import pj.domain.models.Physical.*
import pj.domain.models.Human.*
import pj.domain.models.Task.*
import pj.domain.models.Product.*
import pj.domain.models.Order.*
import pj.domain.models.Domain.*
import pj.io.FileIO
import pj.xml.XML.*

import scala.xml.Elem

object ProductionParser:

  // Parse the entire XML to a Production
  // To be used whenever receiving the input

  def parseProduction(xml: Elem, milestone: String): Result[Production] =
    for {
      physicalResourcesNode <- fromNode(xml, "PhysicalResources")
      physicalResources <- PhysicalParser.parsePhysicalResources(physicalResourcesNode)
      physicalTypes: Set[PhysicalInType] = physicalResources.map(_.typ).toSet
      tasksNode <- fromNode(xml, "Tasks")
      humanResourcesNode <- fromNode(xml, "HumanResources")
      humanResources <- HumanParser.parseHumanResources(humanResourcesNode, physicalTypes)
      tasks <- TaskParser.parseTasks(tasksNode, physicalResources, humanResourcesNode, milestone)

      validTasks = tasks.map(task => task.id -> task).toMap
      
      productsNode <- fromNode(xml, "Products")
      products <- ProductParser.parseProducts(productsNode, validTasks)
      validProductIds = products.map(_.id).toSet

      ordersNode <- fromNode(xml, "Orders")
      orders <- OrderParser.parseOrders(ordersNode, validProductIds)

    } yield Production(
      physicalResources = physicalResources,
      taskResources = tasks,
      humanResources = humanResources,
      products = products,
      orders = orders
    )
