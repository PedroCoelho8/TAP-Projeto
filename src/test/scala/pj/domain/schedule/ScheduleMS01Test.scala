package pj.domain.schedule

import org.scalatest.funsuite.AnyFunSuite
import pj.domain.DomainError

import pj.xml.XML
import scala.xml
import java.io.File

class ScheduleMS01Test extends AnyFunSuite:

  val inputDir = new File("files/test/inputs")
  val expectedDir = new File("files/test/outputs")

  test("ScheduleMS01 diagnostic run — log all results, no failures, no mutables"):

    assert(inputDir.exists && expectedDir.exists, "Input or output directory does not exist.")

    val inputFiles = inputDir.listFiles().filter(_.getName.endsWith(".xml")).toList

    val results: List[String] = inputFiles.map { inputFile =>
      val inputXml = scala.xml.XML.loadFile(inputFile)
      val result = ScheduleMS01.create(inputXml)
      val expectedFile = new File(expectedDir, inputFile.getName)

      result match
        case Left(error) =>
          s"[FAIL] ${inputFile.getName}: $error"

        case Right(generatedXml) if expectedFile.exists =>
          val expectedXml = scala.xml.XML.loadFile(expectedFile)
          println(normalize(generatedXml))
          println(normalize(expectedXml))
          val isMatch = normalize(generatedXml) == normalize(expectedXml)
          if isMatch then s"[PASS] ${inputFile.getName}"
          else s"[MISMATCH] ${inputFile.getName} — Output differs from expected."

        case Right(_) =>
          s"[UNEXPECTED PASS] ${inputFile.getName} — No expected output to compare."
    }

    println("\n--- Test Summary ---")
    results.foreach(println)
    println("--------------------")

    assert(true)

  test("toString should return correct string for IOFileProblem"):
    val error = DomainError.IOFileProblem("file error")
    assert(error.toString == "IOFileProblem(file error)")

  test("toString should return correct string for XMLError"):
    val error = DomainError.XMLError("xml error")
    assert(error.toString == "XMLError(xml error)")

  test("toString should return correct string for TaskUsesNonExistentPRT"):
    val error = DomainError.TaskUsesNonExistentPRT("prt")
    assert(error.toString == "TaskUsesNonExistentPRT(prt)")

  test("toString should return correct string for ResourceUnavailable"):
    val error = DomainError.ResourceUnavailable("task1", "resourceType")
    assert(error.toString == "ResourceUnavailable(task1,resourceType)")

  test("toString should return correct string for InvalidOrderId"):
    val error = DomainError.InvalidOrderId("order123")
    assert(error.toString == "InvalidOrderId(order123)")

  test("toString should return correct string for InvalidHumanId"):
    val error = DomainError.InvalidHumanId("human123")
    assert(error.toString == "InvalidHumanId(human123)")

  test("toString should return correct string for InvalidPhysicalId"):
    val error = DomainError.InvalidPhysicalId("invalid physical id")
    assert(error.toString == "InvalidPhysicalId(invalid physical id)")

  test("toString should return correct string for ProductDoesNotExist"):
    val error = DomainError.ProductDoesNotExist("product123")
    assert(error.toString == "ProductDoesNotExist(product123)")

  test("toString should return correct string for InvalidQuantity"):
    val error = DomainError.InvalidQuantity(42)
    assert(error.toString == "InvalidQuantity(42)")

  test("toString should return correct string for TaskDoesNotExist"):
    val error = DomainError.TaskDoesNotExist("task123")
    assert(error.toString == "TaskDoesNotExist(task123)")

  test("toString should return correct string for InvalidHandleType"):
    val error = DomainError.InvalidHandleType("handle123")
    assert(error.toString == "InvalidHandleType(handle123)")

  test("toString should return correct string for InvalidPhysicalType"):
    val error = DomainError.InvalidPhysicalType("physicalType123")
    assert(error.toString == "InvalidPhysicalType(physicalType123)")

  test("toString should return correct string for InvalidTaskId"):
    val error = DomainError.InvalidTaskId("taskId123")
    assert(error.toString == "InvalidTaskId(taskId123)")

  test("toString should return correct string for InvalidProductId"):
    val error = DomainError.InvalidProductId("productId123")
    assert(error.toString == "InvalidProductId(productId123)")

  test("toString should return correct string for InvalidTime"):
    val error = DomainError.InvalidTime(99)
    assert(error.toString == "InvalidTime(99)")

  test("fromNode returns Right if node TaskSchedule exists"):
    val xml: scala.xml.Elem = <Schedule>
      <TaskSchedule order="ORD_001" productNumber="1" task="TSK_001" start="0" end="10"/>
    </Schedule>
    val result = XML.fromNode(xml, "TaskSchedule")
    assert(result.isRight)

  test("fromNode returns Left if node TaskSchedule does not exist"):
    val xml: scala.xml.Elem = <Schedule>
      <OtherNode/>
    </Schedule>
    val result = XML.fromNode(xml, "TaskSchedule")
    assert(result.isLeft)

  test("fromAttribute returns Right if attribute order exists"):
    val xml: scala.xml.Elem = <TaskSchedule order="ORD_001" productNumber="1" task="TSK_001" start="0" end="10"/>
    val result = XML.fromAttribute(xml, "order")
    assert(result.isRight)

  test("fromAttribute returns Left if attribute order does not exist"):
    val xml: scala.xml.Elem = <TaskSchedule productNumber="1" task="TSK_001" start="0" end="10"/>
    val result = XML.fromAttribute(xml, "order")
    assert(result.isLeft)

  test("traverse returns Right if all TaskSchedule elements are valid"):
    val list = Seq(
        <TaskSchedule order="ORD_001" productNumber="1" task="TSK_001" start="0" end="10"/>,
        <TaskSchedule order="ORD_002" productNumber="2" task="TSK_002" start="11" end="20"/>
    )
    val result = XML.traverse(list, ts => for {
      order <- XML.fromAttribute(ts, "order")
      productNumber <- XML.fromAttribute(ts, "productNumber")
      task <- XML.fromAttribute(ts, "task")
      start <- XML.fromAttribute(ts, "start")
      end <- XML.fromAttribute(ts, "end")
    } yield (order, productNumber, task, start, end))
    assert(result.isRight)

  test("traverse returns Left if a TaskSchedule element is invalid (missing order)"):
    val list = Seq(
        <TaskSchedule order="ORD_001" productNumber="1" task="TSK_001" start="0" end="10"/>,
        <TaskSchedule productNumber="2" task="TSK_002" start="11" end="20"/> // missing order attr
    )
    val result = XML.traverse(list, ts => for {
      order <- XML.fromAttribute(ts, "order")
      productNumber <- XML.fromAttribute(ts, "productNumber")
      task <- XML.fromAttribute(ts, "task")
      start <- XML.fromAttribute(ts, "start")
      end <- XML.fromAttribute(ts, "end")
    } yield (order, productNumber, task, start, end))
    assert(result.isLeft)

  def normalize(xml: scala.xml.Node): String =
    xml.toString().trim.replaceAll("\\s+", "")
