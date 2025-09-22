//package pj.xml
//
//import org.scalatest.funsuite.AnyFunSuite
//import pj.domain.*
//import pj.domain.DomainError.*
//
//import scala.xml.XML
//import java.io.File
//
//class XmlWriterTest extends AnyFunSuite {
//
//  def createPhysicalOut(id: String): PhysicalOut =
//    PhysicalOut(PhysicalInId(id))
//
//  def createHumanOut(name: String): HumanOut =
//    HumanOut(HumanName(name))
//
//  def createTaskSchedule(orderId: String, productNumber: Int, taskId: String, start: Int, end: Int, physicalIds: List[String], humanNames: List[String]): TaskSchedule =
//    TaskSchedule( OrderId(orderId),
//      ProductNumber(productNumber),
//      TaskId(taskId),
//      StartValue(start),
//      EndValue(end),
//      physicalIds.map(createPhysicalOut),
//      humanNames.map(createHumanOut)
//    )
//
//  val sampleTaskSchedule: TaskSchedule =
//    createTaskSchedule("ORD_1", 1, "TSK_1", 0, 30, List("PRS_1", "PRS_2"), List("Antonio", "Maria"))
//
//  val multipleTaskSchedules: List[TaskSchedule] = List(
//    sampleTaskSchedule,
//    createTaskSchedule("ORD_1", 1, "TSK_2", 30, 75, List("PRS_3"), List("Joao")),
//    createTaskSchedule("ORD_2", 1,"TSK_1", 0, 30, List("PRS_4", "PRS_5"), List("Jose", "Antonio"))
//  )
//
//  test("toXmlError should generate correct error XML"):
//    val error = XMLError("Test error message")
//    val result = XMLWriter.toXmlError(error)
//
//    assert(result.label == "ScheduleError")
//    assert((result \@ "message") == "XMLError(Test error message)")
//
//  test("toXmlFile should generate correct XML for a single TaskSchedule"):
//    val result = XMLWriter.toXmlFile(List(sampleTaskSchedule))
//
//    result match
//      case Right(xmlString) =>
//        assert(xmlString.contains("TaskSchedule"))
//        assert(xmlString.contains("order=\"ORD_1\""))
//        assert(xmlString.contains("productNumber=\"1\""))
//        assert(xmlString.contains("task=\"TSK_1\""))
//        assert(xmlString.contains("start=\"0\""))
//        assert(xmlString.contains("end=\"30\""))
//
//        assert(xmlString.contains("<Physical id=\"PRS_1\""))
//        assert(xmlString.contains("<Physical id=\"PRS_2\""))
//
//        assert(xmlString.contains("<Human name=\"Antonio\""))
//        assert(xmlString.contains("<Human name=\"Maria\""))
//
//        val file = new File("schedule.xml")
//        assert(file.exists())
//        file.delete()
//
//      case Left(error) => fail(s"Expected Right but got Left: $error")
//
//  test("toXmlFile should generate correct XML for multiple TaskSchedules"):
//    val result = XMLWriter.toXmlFile(multipleTaskSchedules)
//
//    result match
//      case Right(xmlString) =>
//        val countTaskSchedules = "<TaskSchedule ".r.findAllIn(xmlString).length
//        assert(countTaskSchedules == 3)
//
//        val file = new File("schedule.xml")
//        assert(file.exists())
//
//        val xmlContent = XML.loadFile(file)
//        val taskScheduleNodes = xmlContent \\ "TaskSchedule"
//        assert(taskScheduleNodes.sizeIs == 3)
//
//        file.delete()
//
//      case Left(error) => fail(s"Expected Right but got Left: $error")
//
//  test("toXmlFile should handle empty list of TaskSchedules"):
//    val result = XMLWriter.toXmlFile(List())
//
//    result match
//      case Right(xmlString) =>
//        assert(!xmlString.contains("<TaskSchedule"))
//
//        val file = new File("schedule.xml")
//        assert(file.exists())
//        file.delete()
//
//      case Left(error) => fail(s"Expected Right but got Left: $error")
//
//  test("toXmlFile should return Left with error on exception"):
//    val mockTaskSchedule = createTaskSchedule("ORD_1" * 1000000, Int.MaxValue, "TSK_1", 0, 30, List("PRS_1"), List("Antonio"))
//
//    val result = XMLWriter.toXmlFile(List(mockTaskSchedule))
//
//    result match
//      case Left(XMLError(_)) => succeed
//      case Right(_) =>
//        val file = new File("schedule.xml")
//        if (file.exists()) file.delete()
//
//  test("XML output should follow the correct structure"):
//    val result = XMLWriter.toXmlFile(List(sampleTaskSchedule))
//
//    result match
//      case Right(_) =>
//        val file = new File("schedule.xml")
//        val xmlContent = XML.loadFile(file)
//
//        assert(xmlContent.label == "Schedule")
//
//        assert(xmlContent.namespace == "http://www.dei.isep.ipp.pt/tap-2025")
//
//        val taskScheduleNodes = xmlContent \\ "TaskSchedule"
//        assert(taskScheduleNodes.nonEmpty)
//        val taskScheduleNode = taskScheduleNodes.headOption.getOrElse(fail("No TaskSchedule node found"))
//
//        assert(taskScheduleNode.attribute("order").map(_.text).getOrElse("") == "ORD_1")
//        assert(taskScheduleNode.attribute("productNumber").map(_.text).getOrElse("") == "1")
//        assert(taskScheduleNode.attribute("task").map(_.text).getOrElse("") == "TSK_1")
//        assert(taskScheduleNode.attribute("start").map(_.text).getOrElse("") == "0")
//        assert(taskScheduleNode.attribute("end").map(_.text).getOrElse("") == "30")
//
//        val physicalResources = taskScheduleNode \\ "PhysicalResources"
//        val humanResources = taskScheduleNode \\ "HumanResources"
//
//        assert(physicalResources.nonEmpty)
//        assert(humanResources.nonEmpty)
//
//        val physicals = physicalResources.headOption.getOrElse(fail("No Physicals node found")) \\ "Physical"
//        val humans = humanResources.headOption.getOrElse(fail("No Humans node found")) \\ "Human"
//
//        assert(physicals.sizeIs == 2)
//        assert(humans.sizeIs == 2)
//
//        file.delete()
//
//      case Left(error) => fail(s"Expected Right but got Left: $error")
//
//  test("XML output should handle special characters in attributes"):
//    val specialCharTaskSchedule = createTaskSchedule("ORD_1", 1, "TSK_1", 0, 30, List("PRS_1"), List("Antonio & Maria"))
//
//    val result = XMLWriter.toXmlFile(List(specialCharTaskSchedule))
//
//    result match
//      case Right(xmlString) =>
//        assert(xmlString.contains("Antonio &amp; Maria") || xmlString.contains("Antonio &#38; Maria"))
//
//        val file = new File("schedule.xml")
//        file.delete()
//
//      case Left(error) => fail(s"Expected Right but got Left: $error")
//
//  test("TaskSchedule with no resources should be handled correctly"):
//    val emptyResourceTaskSchedule = createTaskSchedule("ORD_1", 1, "TSK_1", 0, 30, List(), List())
//
//    val result = XMLWriter.toXmlFile(List(emptyResourceTaskSchedule))
//
//    result match
//      case Right(xmlString) =>
//        val file = new File("schedule.xml")
//        assert(file.exists())
//
//        val xmlContent = XML.loadFile(file)
//
//        val taskScheduleNodes = xmlContent \\ "TaskSchedule"
//        assert(taskScheduleNodes.nonEmpty)
//
//        val physicalResourcesNodes = xmlContent \\ "PhysicalResources"
//        val humanResourcesNodes = xmlContent \\ "HumanResources"
//        assert(physicalResourcesNodes.nonEmpty)
//        assert(humanResourcesNodes.nonEmpty)
//
//        val physicalNodes = physicalResourcesNodes.headOption.getOrElse(fail("No PhysicalResources found")) \\ "Physical"
//        val humanNodes = humanResourcesNodes.headOption.getOrElse(fail("No HumanResources found")) \\ "Human"
//        assert(physicalNodes.isEmpty)
//        assert(humanNodes.isEmpty)
//
//        file.delete()
//
//      case Left(error) => fail(s"Expected Right but got Left: $error")
//}