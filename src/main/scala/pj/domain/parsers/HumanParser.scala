package pj.domain.parsers

import pj.domain.DomainError.*
import pj.domain.models.Human.{Human, HumanId, HumanName, Handles}
import pj.domain.models.Physical.PhysicalInType
import pj.xml.XML.*
import scala.xml.Node
import pj.domain.Result

object HumanParser:

  def parseHumanResources(node: Node, validTypes: Set[PhysicalInType]): Result[List[Human]] =
    val humanNodes = node \ "Human"
    traverse(humanNodes, parseHumanResource(_, validTypes))

  private def parseHumanResource(node: Node, validTypes: Set[PhysicalInType]): Result[Human] =
    for
      idStr     <- fromAttribute(node, "id")
      id        <- HumanId.from(idStr)
      nameStr   <- fromAttribute(node, "name")
      name      <- HumanName.from(nameStr)
      handlesNs  = node \ "Handles"
      handles   <- traverse(handlesNs, parseHandles(_, validTypes))
    yield
      Human(id, name, handles)

  private def parseHandles(node: Node, validTypes: Set[PhysicalInType]): Result[Handles] =
    fromAttribute(node, "type").flatMap { typ =>
      PhysicalInType.from(typ).flatMap { pt =>
        if validTypes.contains(pt) then
          Handles.from(pt)
        else
          Left(InvalidHandleType(typ))
      }
    }
