package pj.domain.parsers

import pj.domain.DomainError.InvalidPhysicalId
import pj.xml.XML.*
import pj.domain.models.Physical.*
import pj.domain.Result

import scala.xml.Node

object PhysicalParser{

  def parsePhysicalResources(node: Node): Result[List[Physical]] =
    val physicalNodes = node \ "Physical"
    traverse(physicalNodes, parsePhysicalResource).flatMap { physicals =>
      checkForDuplicateIds(physicals)
    }

  private def parsePhysicalResource(node: Node): Result[Physical] =
    for
      idStr <- fromAttribute(node, "id")
      id <- PhysicalInId.from(idStr)
      typeStr <- fromAttribute(node, "type")
      typ <- PhysicalInType.from(typeStr)
    yield
      Physical(id, typ)

  private def checkForDuplicateIds(physicals: List[Physical]): Result[List[Physical]] =
    val ids = physicals.map(_.id)
    val uniqueIds = ids.toSet

    if (ids.lengthIs == uniqueIds.size)
      Right(physicals)
    else
      val duplicateId = ids.groupBy(identity)
        .find(_._2.lengthIs > 1)
        .map(_._1)
        .getOrElse("unknown")
      val idAsString = duplicateId match
        case id: PhysicalInId => id.to
        case id: String => id
      Left(InvalidPhysicalId(idAsString)) 
}