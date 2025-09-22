package pj.domain.models

import pj.domain.Result
import pj.domain.DomainError.*
import pj.domain.models.Human.*
import pj.domain.models.Product.*
import pj.domain.models.Order.*
import pj.domain.models.Physical.*
import pj.domain.models.Task.*

object Domain:

    case class Production(
                           physicalResources: List[Physical], 
                           taskResources: List[Task], 
                           humanResources: List[Human], 
                           products: List[Product], 
                           orders: List[Order])

    final case class TaskSchedule(
                                   order: OrderId ,
                                   productNumber: ProductNumber,
                                   task: TaskId,
                                   start: StartValue,
                                   end: EndValue,
                                   physicalResources: List[Physical],
                                   humanResources: List[Human]
                                 )


    final case class ProductNumber(productNumber: Int)

    final case class StartValue(start: Int)

    final case class EndValue(end: Int)


