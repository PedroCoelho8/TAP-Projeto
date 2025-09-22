package pj.xml

import pj.domain.DomainError.XMLError
import pj.domain.models.Domain.TaskSchedule
import pj.domain.DomainError
import pj.domain.models.Task.*
import pj.domain.models.Human.*
import pj.domain.models.Order.*
import pj.domain.models.Physical.*
import pj.domain.models.Product.*
import scala.xml.{XML, PrettyPrinter, Elem}
import java.io.{File, FileWriter}

object XMLWriter {

  def toXmlError(error: DomainError): Elem =
      <ScheduleError message={error.toString} />

  def toXmlFile(taskSchedules: List[TaskSchedule]): Either[DomainError, String] =
    try
      val taskScheduleElems = taskSchedules.map { ts =>
        val physicalResourcesElems = ts.physicalResources.map(pr => toXmlPhysical(pr.id.to))
        val physicalResourcesElem = toXmlPhysicalResources(physicalResourcesElems)

        val humanResourcesElems = ts.humanResources.map(hr => toXmlHuman(hr.name.to))
        val humanResourcesElem = toXmlHumanResources(humanResourcesElems)

        toXmlTaskSchedule(
          ts.order.to,
          ts.productNumber.productNumber.toString,
          ts.task.to,
          ts.start.start.toString,
          ts.end.end.toString,
          List(physicalResourcesElem, humanResourcesElem)
        )
      }

      val scheduleElem = toXmlSchedule(taskScheduleElems)

      val prettyPrinter = new PrettyPrinter(120, 2)
      val xmlString = """<?xml version="1.0" encoding="UTF-8"?>""" + "\n" + prettyPrinter.format(scheduleElem)

      val outputFile = new File("schedule.xml")
      val writer = new FileWriter(outputFile)
      writer.write(xmlString)
      writer.close()

      Right(xmlString)
    catch
      case e: Exception => Left(XMLError(s"Error creating XML file: ${e.getMessage}"))

  private def toXmlSchedule(taskSchedules: List[Elem]): Elem =
    <Schedule
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns="http://www.dei.isep.ipp.pt/tap-2025"
    xsi:schemaLocation="http://www.dei.isep.ipp.pt/tap-2025 ../../schedule.xsd ">
      {taskSchedules}
    </Schedule>

  private def toXmlTaskSchedule(orderId: String, productNum: String, taskId: String, st: String, en: String, listResources: List[Elem]): Elem =
    <TaskSchedule order={orderId} productNumber={productNum} task={taskId} start={st} end={en}>
      {listResources}
    </TaskSchedule>
  
  private def toXmlPhysicalResources(listPhysicalResources: List[Elem]): Elem =
    <PhysicalResources>
      {listPhysicalResources}
    </PhysicalResources>
  
  private def toXmlPhysical(physicalId: String): Elem =
    <Physical id={physicalId} />
  
  private def toXmlHumanResources(listHumanResources: List[Elem]): Elem =
    <HumanResources>
      {listHumanResources}
    </HumanResources>
  
  private def toXmlHuman(humanName: String): Elem =
    <Human name={humanName} />
  
  
}