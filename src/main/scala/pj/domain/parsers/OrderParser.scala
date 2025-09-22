package pj.domain.parsers


import pj.domain.DomainError.*
import pj.domain.Result
import pj.domain.models.Order.{Order, OrderId, OrderQuantity}
import pj.xml.XML.*
import pj.domain.models.Product.ProductId

import scala.xml.Node

object OrderParser:

  def parseOrders(node: Node, validProductIds: Set[ProductId]): Result[List[Order]] =
    val orderNodes = node \ "Order"
    traverse(orderNodes, parseOrder(_, validProductIds))

  private def parseOrder(node: Node, validProductIds: Set[ProductId]): Result[Order] =
    for
      idStr <- fromAttribute(node, "id")
      id <- OrderId.from(idStr)
      productStr <- fromAttribute(node, "prdref")
      productId <- ProductId.from(productStr)
      _ <- if validProductIds.contains(productId) then Right(()) else Left(ProductDoesNotExist(productStr))
      quantityStr <- fromAttribute(node, "quantity")
      quantityInt <- parseInt(quantityStr).toRight(XMLError(s"Invalid quantity: $quantityStr"))
      quantity <- OrderQuantity.from(quantityInt)
    yield
      Order(id, productId, quantity)


  private def parseInt(s: String): Option[Int] =
    try Some(s.toInt)
    catch case _: NumberFormatException => None
