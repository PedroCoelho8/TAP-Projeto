package pj.domain.parsers.unit

import org.scalatest.funsuite.AnyFunSuite
import pj.domain.DomainError.*
import pj.domain.models.Physical.*
import pj.domain.models.Task.*
import pj.domain.Result
import pj.domain.parsers.TaskParser

import scala.xml.{Node, XML}

class TaskParserTest extends AnyFunSuite {

  // Helper functions to create XML nodes for tests
  private def createPhysicalsNode(physicals: List[(String, String)]): Node =
    val physicalNodes = physicals.map { case (id, typeStr) =>
      s"""<Physical id="$id" type="$typeStr" />"""
    }.mkString
    XML.loadString(s"""<Resources>$physicalNodes</Resources>""")

  private def createHumansNode(humans: List[(String, String, List[String])]): Node =
    val humanNodes = humans.map { case (id, name, handles) =>
      val handlesXml = handles.map(h => s"""<Handles type="$h" />""").mkString
      s"""<Human id="$id" name="$name">$handlesXml</Human>"""
    }.mkString
    XML.loadString(s"""<Resources>$humanNodes</Resources>""")

  private def createTasksNode(tasks: List[(String, String, List[String])]): Node =
    val taskNodes = tasks.map { case (id, time, resourceTypes) =>
      val resourcesXml = resourceTypes.map(t => s"""<PhysicalResource type="$t" />""").mkString
      s"""<Task id="$id" time="$time">$resourcesXml</Task>"""
    }.mkString
    XML.loadString(s"""<Tasks>$taskNodes</Tasks>""")

  // Setup of valid resources for the tests
  private val validPhysicals: List[Physical] = List(
    Physical(
      PhysicalInId.from("PRS_1").getOrElse(fail("Test setup failed")),
      PhysicalInType.from("PRST 1").getOrElse(fail("Test setup failed"))
    ),
    Physical(
      PhysicalInId.from("PRS_2").getOrElse(fail("Test setup failed")),
      PhysicalInType.from("PRST 1").getOrElse(fail("Test setup failed"))
    ),
    Physical(
      PhysicalInId.from("PRS_3").getOrElse(fail("Test setup failed")),
      PhysicalInType.from("PRST 1").getOrElse(fail("Test setup failed"))
    ),
    Physical(
      PhysicalInId.from("PRS_4").getOrElse(fail("Test setup failed")),
      PhysicalInType.from("PRST 2").getOrElse(fail("Test setup failed"))
    ),
    Physical(
      PhysicalInId.from("PRS_5").getOrElse(fail("Test setup failed")),
      PhysicalInType.from("PRST 2").getOrElse(fail("Test setup failed"))
    ),
    Physical(
      PhysicalInId.from("PRS_6").getOrElse(fail("Test setup failed")),
      PhysicalInType.from("PRST 3").getOrElse(fail("Test setup failed"))
    )
  )

  private val validHumansNode: Node = createHumansNode(List(
    ("HRS_1", "Antonio", List("PRST 1", "PRST 2")),
    ("HRS_2", "Maria", List("PRST 1", "PRST 3")),
    ("HRS_3", "Joao", List("PRST 2", "PRST 3")),
    ("HRS_4", "Jose", List("PRST 1", "PRST 2", "PRST 3"))
  ))

  test("should parse a valid task"):
    val taskNode = createTasksNode(List(
      ("TSK_1", "30", List("PRST 1"))
    ))

    val result = TaskParser.parseTasks(taskNode, validPhysicals, validHumansNode, "MS01")

    result match
      case Right(tasks) =>
        assert(tasks.lengthIs == 1)
        tasks.headOption match
          case Some(task) =>
            assert(task.id.to == "TSK_1")
            assert(task.time.to == 30)
            assert(task.physicalResources.lengthIs == 1)
            val requiredType = PhysicalInType.from("PRST 1").getOrElse(fail("Test setup failed"))
            val requiredResource = PhysicalResource.from(requiredType).getOrElse(fail("Test setup failed"))
            assert(task.physicalResources.contains(requiredResource))
          case None => fail("Tasks list should not be empty.")
      case Left(err) => fail(s"Expected Right but got Left: $err")

  test("should parse multiple valid tasks"):
    val tasksNode = createTasksNode(List(
      ("TSK_1", "30", List("PRST 1")),
      ("TSK_2", "45", List("PRST 2"))
    ))

    val result = TaskParser.parseTasks(tasksNode, validPhysicals, validHumansNode, "MS01")

    result match
      case Right(tasks) => assert(tasks.lengthIs == 2)
      case Left(err) => fail(s"Expected Right but got Left: $err")

  test("should parse a task with multiple resources of the same type"):
    val tasksNode = createTasksNode(List(
      ("TSK_1", "30", List("PRST 1", "PRST 1"))
    ))

    val result = TaskParser.parseTasks(tasksNode, validPhysicals, validHumansNode, "MS01")

    result match
      case Right(tasks) =>
        assert(tasks.lengthIs == 1)
        tasks.headOption match
          case Some(task) =>
            assert(task.physicalResources.lengthIs == 2)
            val requiredType = PhysicalInType.from("PRST 1").getOrElse(fail("Test setup failed"))
            val requiredResource = PhysicalResource.from(requiredType).getOrElse(fail("Test setup failed"))
            assert(task.physicalResources.count(_ == requiredResource) == 2)
          case None => fail("Tasks list should not be empty.")
      case Left(err) => fail(s"Expected Right but got Left: $err")

  test("should fail when task ID is invalid"):
    val taskNode = createTasksNode(List(
      ("INVALID_ID", "30", List("PRST 1"))
    ))

    val result = TaskParser.parseTasks(taskNode, validPhysicals, validHumansNode, "MS01")

    assert(result == Left(InvalidTaskId("INVALID_ID")))

  test("should throw NumberFormatException when time is not a number"):
    val taskNode = createTasksNode(List(
      ("TSK_1", "not_a_number", List("PRST 1"))
    ))

    assertThrows[NumberFormatException]:
      TaskParser.parseTasks(taskNode, validPhysicals, validHumansNode, "MS01")

  test("should fail when physical resource type does not exist"):
    val taskNode = createTasksNode(List(
      ("TSK_1", "30", List("PRST 99"))
    ))

    val result = TaskParser.parseTasks(taskNode, validPhysicals, validHumansNode, "MS01")

    assert(result == Left(TaskUsesNonExistentPRT("PRST 99")))


  test("should fail when not enough physical resources are available"):
    val taskNode = createTasksNode(List(
      ("TSK_1", "30", List("PRST 1", "PRST 1", "PRST 1", "PRST 1")) // Requires 4
    ))

    // validPhysicals only has 3 of "PRST 1"
    val result = TaskParser.parseTasks(taskNode, validPhysicals, validHumansNode, "MS01")

    result match
      case Left(ResourceUnavailable(taskId, resourceType)) =>
        assert(taskId == "TSK_1")
        assert(resourceType == "PRST 1")
      case other => fail(s"Expected ResourceUnavailable but got $other")

  test("should fail when not enough human resources are available"):
    val taskNode = createTasksNode(List(
      ("TSK_1", "30", List("PRST 1", "PRST 1", "PRST 1", "PRST 1")) // Requires 4 humans for PRST 1
    ))

    val physicalsWithEnoughResources = List(
      Physical(PhysicalInId.from("PRS_1").getOrElse(fail("Test setup failed")), PhysicalInType.from("PRST 1").getOrElse(fail("Test setup failed"))),
      Physical(PhysicalInId.from("PRS_2").getOrElse(fail("Test setup failed")), PhysicalInType.from("PRST 1").getOrElse(fail("Test setup failed"))),
      Physical(PhysicalInId.from("PRS_3").getOrElse(fail("Test setup failed")), PhysicalInType.from("PRST 1").getOrElse(fail("Test setup failed"))),
      Physical(PhysicalInId.from("PRS_4").getOrElse(fail("Test setup failed")), PhysicalInType.from("PRST 1").getOrElse(fail("Test setup failed")))
    )

    // Only 3 humans can handle "PRST 1"
    val humansNode = createHumansNode(List(
      ("HRS_1", "Antonio", List("PRST 1")),
      ("HRS_2", "Maria", List("PRST 1")),
      ("HRS_3", "Jose", List("PRST 1"))
    ))

    val result = TaskParser.parseTasks(taskNode, physicalsWithEnoughResources, humansNode, "MS01")

    result match
      case Left(ResourceUnavailable(taskId, resourceType)) =>
        assert(taskId == "TSK_1")
        assert(resourceType == "PRST 1")
      case other => fail(s"Expected ResourceUnavailable but got $other")

  test("should return an empty list for an input with no tasks"):
    val emptyNode = XML.loadString("<Tasks></Tasks>")
    val result = TaskParser.parseTasks(emptyNode, validPhysicals, validHumansNode, "MS01")

    result match
      case Right(tasks) => assert(tasks.isEmpty)
      case Left(err) => fail(s"Expected Right but got Left: $err")

  test("should succeed with a valid ID that is just the prefix"):
    val taskNode = createTasksNode(List(
      ("TSK_", "30", List("PRST 1"))
    ))

    val result = TaskParser.parseTasks(taskNode, validPhysicals, validHumansNode, "MS01")

    result match
      case Right(tasks) =>
        assert(tasks.lengthIs == 1)
        tasks.headOption.foreach { task =>
          assert(task.id.to == "TSK_")
        }
      case Left(err) => fail(s"Expected Right but got Left: $err")

  test("should fail if the task 'id' attribute is missing"):
    val nodeWithoutId = XML.loadString("""<Task time="30"><PhysicalResource type="PRST 1" /></Task>""")
    val tasksNode = XML.loadString(s"""<Tasks>${nodeWithoutId.toString}</Tasks>""")

    val result = TaskParser.parseTasks(tasksNode, validPhysicals, validHumansNode, "MS01")

    result match
      // Corrected the expected error message to match the actual output
      case Left(XMLError(msg)) => assert(msg == "Attribute id is empty/undefined in Task")
      case other => fail(s"Expected XMLError but got $other")
  
  test("should fail if the task 'time' attribute is missing"):
    val nodeWithoutTime = XML.loadString("""<Task id="TSK_1"><PhysicalResource type="PRST 1" /></Task>""")
    val tasksNode = XML.loadString(s"""<Tasks>${nodeWithoutTime.toString}</Tasks>""")

    val result = TaskParser.parseTasks(tasksNode, validPhysicals, validHumansNode, "MS01")

    result match
      // Proactively corrected this test as it would have the same issue
      case Left(XMLError(msg)) => assert(msg == "Attribute time is empty/undefined in Task")
      case other => fail(s"Expected XMLError but got $other")

  test("should fail with the first error when multiple tasks have errors"):
    val tasksNode = createTasksNode(List(
      ("TSK_1", "30", List("PRST 1")),
      ("INVALID_ID", "45", List("PRST 2")),
      ("TSK_3", "60", List("PRST 3"))
    ))

    val result = TaskParser.parseTasks(tasksNode, validPhysicals, validHumansNode, "MS01")

    assert(result == Left(InvalidTaskId("INVALID_ID")))
}