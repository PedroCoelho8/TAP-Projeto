package pj.domain.models

import pj.domain.*
import pj.domain.models.Product.ProductId

object Order:
  
  opaque type OrderId = String

  object OrderId:
    def from(id: String): Result[OrderId] =
      if (!id.isBlank && id.startsWith("ORD_"))
        Right(id)
      else
        Left(DomainError.InvalidOrderId(id))

    extension (id: OrderId)
      def to: String = id

  opaque type OrderQuantity = Int

  object OrderQuantity:
    def from(qty: Int): Result[OrderQuantity] =
      if qty > 0 then Right(qty)
      else Left(DomainError.InvalidQuantity(qty))

    extension (qty: OrderQuantity)
      def to: Int = qty

  final case class Order(id: OrderId, prdref: ProductId, quantity: OrderQuantity)