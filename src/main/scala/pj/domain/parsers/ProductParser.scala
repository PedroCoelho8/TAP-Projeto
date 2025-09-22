package pj.domain.parsers

import pj.domain.DomainError.*
import pj.domain.{Result}
import pj.xml.XML.*
import pj.domain.models.Product.{Process, Product, ProductId, ProductName, TaskReference}
import pj.domain.models.Task.{Task, TaskId}

import scala.xml.Node


object ProductParser:

  def parseProducts(node: Node, validTasks: Map[TaskId, Task]): Result[List[Product]] =
    val productNodes = node \ "Product"
    traverse(productNodes, parseProduct(validTasks))
  
  private def parseProduct(validTasks: Map[TaskId, Task])(node: Node): Result[Product] =
    for
      idStr <- fromAttribute(node, "id")
      id <- ProductId.from(idStr)
      nameStr <- fromAttribute(node, "name")
      name <- ProductName.from(nameStr)
      processNodes = node \ "Process"
      processes <- traverse(processNodes, parseProcess(validTasks))
    yield
      Product(id, name, processes)

  private def parseProcess(validTasks: Map[TaskId, Task])(node: Node): Result[Process] =
    for
      tskrefStr <- fromAttribute(node, "tskref")
      tskrefId <- TaskId.from(tskrefStr)
      _ <- if validTasks.contains(tskrefId) then Right(()) else Left(TaskDoesNotExist(tskrefStr))
      tskref <- TaskReference.from(tskrefStr)
    yield
      Process(tskref)
