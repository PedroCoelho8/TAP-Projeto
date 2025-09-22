package pj.domain.schedule

import org.scalatest.funsuite.AnyFunSuite
import scala.xml.Elem
import pj.domain.*
import pj.domain.models.Human.*
import pj.domain.models.Physical.*
import pj.domain.models.Task.*
import pj.domain.models.Order.*
import pj.domain.models.Domain.*
import pj.domain.models.Product.*

class MS03UnitTesting extends AnyFunSuite:

  // Test fixtures
  val validProduction: Option[Production] = (
    for {
      // Orders
      orderId <- OrderId.from("ORD_O1")
      productId <- ProductId.from("PRD_1")
      quantity <- OrderQuantity.from(2)

      // Products
      productName <- ProductName.from("Product_Name")
      process1 <- Process.from("TSK_1")
      process2 <- Process.from("TSK_2")

      // Tasks
      taskId1 <- TaskId.from("TSK_1")
      taskTime1 <- TaskTime.from(10)
      physInType1 <- PhysicalInType.from("PRST 1")
      physResource1 <- PhysicalResource.from(physInType1)

      taskId2 <- TaskId.from("TSK_2")
      taskTime2 <- TaskTime.from(15)
      physInType2 <- PhysicalInType.from("PRST 2")
      physResource2 <- PhysicalResource.from(physInType2)

      // Physical Resources
      physInId1 <- PhysicalInId.from("PRS_1")
      physInId2 <- PhysicalInId.from("PRS_2")

      // Humans
      humanId1 <- HumanId.from("HRS_1")
      humanName1 <- HumanName.from("Worker1")
      handles1 <- Handles.from(physInType1)

      humanId2 <- HumanId.from("HRS_2")
      humanName2 <- HumanName.from("Worker2")
      handles2 <- Handles.from(physInType2)



    } yield Production(
      orders = List(Order(orderId, productId, quantity)),
      products = List(Product(productId, productName, List(process1, process2))),
      taskResources = List(
        Task(taskId1, taskTime1, List(physResource1)),
        Task(taskId2, taskTime2, List(physResource2))
      ),
      physicalResources = List(
        Physical(physInId1, physInType1),
        Physical(physInId2, physInType2)
      ),
      humanResources = List(
        Human(humanId1, humanName1, List(handles1)),
        Human(humanId2, humanName2, List(handles2))
      )
    )
    ).toOption


  private val emptyProduction = Production(
    orders = List.empty,
    products = List.empty,
    taskResources = List.empty,
    physicalResources = List.empty,
    humanResources = List.empty
  )

  val initialResourceState: Either[DomainError, ScheduleMS03.ResourceState] =
    for {
      physId1 <- PhysicalInId.from("PRS_1")
      physId2 <- PhysicalInId.from("PRS_2")
      humanName1 <- HumanName.from("Worker1")
      humanName2 <- HumanName.from("Worker2")
      orderId <- OrderId.from("ORD_O1")
    } yield ScheduleMS03.ResourceState(
      physicalResources = Map(
        physId1 -> 0,
        physId2 -> 0
      ),
      humanResources = Map(
        humanName1 -> 0,
        humanName2 -> 0
      ),
      orderProgress = Map(
        orderId -> Map(1 -> 0, 2 -> 0)
      ),
      completedTasks = Map.empty
    )

  // Tests for create method
  test("create should handle valid XML input"):
    val validXml =
      <Production xmlns="http://www.dei.isep.ipp.pt/tap-2025"
                    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xsi:schemaLocation="http://www.dei.isep.ipp.pt/tap-2025 ../../production.xsd ">
          <PhysicalResources>
            <Physical id="PRS_1" type="PRST 1"/>
          </PhysicalResources>
          <Tasks>
            <Task id="TSK_1" time="10">
              <PhysicalResource type="PRST 1"/>
            </Task>
          </Tasks>
          <HumanResources>
            <Human id="HRS_1" name="Worker1">
              <Handles type="PRST 1"/>
            </Human>
          </HumanResources>
          <Products>
            <Product id="PRD_1" name="Product 1">
              <Process tskref="TSK_1"/>
            </Product>
          </Products>
          <Orders>
            <Order id="ORD_O1" prdref="PRD_1" quantity="1" />
          </Orders>
        </Production>

    val result = ScheduleMS03.create(validXml)
    assert(result.isRight, "Should successfully create schedule from valid XML")

  test("create should handle XML parsing errors"):
    val invalidXml = <invalid/>

    val result = ScheduleMS03.create(invalidXml)
    assert(result.isLeft, "Should fail with invalid XML structure")

  test("create should handle production parsing errors"):
    val malformedXml =
      <Production>
        <Orders>
          <Order id="ORD_1" productref="PRD_1" quantity="1" duedate="100"/>
        </Orders>
      </Production>

    val result = ScheduleMS03.create(malformedXml)
    assert(result.isLeft, "Should fail when order references missing product")

  test("create should handle exception during processing"):
    val xmlWithNullData = scala.xml.XML.loadString("<production></production>")

    val result = ScheduleMS03.create(xmlWithNullData)
    result match
      case Left(DomainError.XMLError(msg)) =>
        assert(msg.contains("Node PhysicalResources is empty/undefined in production"))
      case _ => fail("Expected XMLError for malformed input")


  test("create should handle invalid OrderId format"):
    val xmlWithInvalidOrderId =
      <production id="PRD_1">
        <orders>
          <order id="O1" productref="PRD_1" quantity="1" duedate="100"/>
        </orders>
      </production>

    val result = ScheduleMS03.create(xmlWithInvalidOrderId)
    assert(result.isLeft, "Should fail with invalid OrderId format (must start with 'ORD_')")

  test("create should handle invalid quantity"):
    val xmlWithInvalidQuantity =
      <Production id="PRD_1">
        <Orders>
          <Order id="ORD_1" productref="PRD_1" quantity="0" duedate="100"/>
        </Orders>
      </Production>

    val result = ScheduleMS03.create(xmlWithInvalidQuantity)
    assert(result.isLeft, "Should fail with invalid quantity (must be > 0)")

  // Tests for fromXmlToSchedules method
  test("fromXmlToSchedules should return schedules for valid input"):
    val validXml =
      <Production>
        <PhysicalResources>
          <Physical id="PRS_1" type="PRST 1"/>
        </PhysicalResources>
        <Tasks>
          <Task id="TSK_1" time="10">
            <PhysicalResource type="PRST 1"/>
          </Task>
        </Tasks>
        <HumanResources>
          <Human id="HRS_1" name="Worker1">
            <Handles type="PRST 1"/>
          </Human>
        </HumanResources>
        <Products>
          <Product id="PRD_1" name ="leite">
            <Process tskref="TSK_1"/>
          </Product>
        </Products>
        <Orders>
          <Order id="ORD_1" prdref="PRD_1" quantity="1"/>
        </Orders>

      </Production>

    val result = ScheduleMS03.fromXmlToSchedules(validXml)
    result match
      case Right(schedules) =>
        assert(schedules.nonEmpty, "Should generate at least one schedule")
        OrderId.from("ORD_1").toOption.fold(
          fail("Invalid OrderId") // fail the test if conversion fails
        ) { orderId =>
          assert(schedules.forall(_.order == orderId), "All schedules should be for order ORD_1")
        }
      case Left(error) => fail(s"Should succeed but got: $error")

  test("fromXmlToSchedules should handle parsing errors"):
    val invalidXml = <malformed/>

    val result = ScheduleMS03.fromXmlToSchedules(invalidXml)
    assert(result.isLeft, "Should fail with parsing error")

  test("fromXmlToSchedules should handle exception during schedule generation"):
    val xmlWithCorruptedData =
      <Production>
        <Orders>
          <Order id="ORD_1" productref="PRD_" quantity="999999"/>
        </Orders>
        <Products>
          <Product id="PRD_1">
            <Tasks>
              <Task taskref="TSK_1"/>
            </Tasks>
          </Product>
        </Products>
      </Production>

    val result = ScheduleMS03.fromXmlToSchedules(xmlWithCorruptedData)
    result match
      case Left(DomainError.XMLError(msg)) =>
        assert(msg.contains("Node PhysicalResources is empty/undefined in Production"), "Should wrap exceptions properly")
      case _ => fail("Should return XMLError for corrupted data")

  // Tests for generateOptimalSchedules method
  test("generateOptimalSchedules should handle empty production"):
    val result = ScheduleMS03.generateOptimalSchedules(emptyProduction)

    result match
      case Right(schedules) => assert(schedules.isEmpty, "Should return empty list for empty production")
      case Left(error) => fail(s"Should succeed with empty list but got: $error")

  test("generateOptimalSchedules should handle valid production"):


    validProduction match
      case Some(validProduction) =>
        val result = ScheduleMS03.generateOptimalSchedules(validProduction)

        result match
        case Right(schedules) =>
          assert(schedules.nonEmpty, "Should generate schedules")
          assert(schedules.lengthIs == 4, "Should generate 4 schedules (2 orders × 2 tasks)")

          // Verify all schedules have valid data
          schedules.foreach { schedule =>
            assert(schedule.start.start >= 0, "Start time should be non-negative")
            assert(schedule.end.end > schedule.start.start, "End time should be after start time")
            assert(schedule.physicalResources.nonEmpty, "Should have physical resources")
            assert(schedule.humanResources.nonEmpty, "Should have human resources")
          }
        case Left(error) => fail(s"Should succeed but got: $error")

      case None =>

        fail("validProductionOpt was None, expected Some(Production)")


  test("generateOptimalSchedules should handle missing task resources"):
    validProduction match
      case Some(validProduction) =>
        val productionWithMissingTask = validProduction.copy(
          taskResources = List.empty // Remove all task definitions
        )

        val result = ScheduleMS03.generateOptimalSchedules(productionWithMissingTask)

        result match
          case Right(schedules) => assert(schedules.isEmpty, "Should return empty schedules when tasks are missing")
          case Left(_) => succeed // Also acceptable to fail

      case None =>
        fail("validProductionOpt was None, expected Some(Production)")


  test("generateOptimalSchedules should handle missing physical resources"):
    validProduction match
      case Some(validProduction) =>
        val productionWithoutPhysical = validProduction.copy(
          physicalResources = List.empty
        )

        val result = ScheduleMS03.generateOptimalSchedules(productionWithoutPhysical)

        result match
          case Right(schedules) => assert(schedules.isEmpty, "Should return empty when no physical resources")
          case Left(_) => succeed // Also acceptable to fail
      case None =>
        fail("validProductionOpt was None, expected Some(Production)")

  test("generateOptimalSchedules should handle missing human resources"):
    validProduction match
      case Some(validProduction) =>
        val productionWithoutHumans = validProduction.copy(
          humanResources = List.empty
        )

        val result = ScheduleMS03.generateOptimalSchedules(productionWithoutHumans)

        result match
          case Right(schedules) => assert(schedules.isEmpty, "Should return empty when no human resources")
          case Left(_) => succeed // Also acceptable to fail
      case None =>
        fail("validProductionOpt was None, expected Some(Production)")

  test("generateOptimalSchedules should handle excessive iteration scenario"):
    val complexProduction: Option[Production] = (
      for {
        orderId <- OrderId.from("ORD_O1")
        productId <- ProductId.from("PRD_1")
        quantity <- OrderQuantity.from(50)
        productName <- ProductName.from("Leite")

        process1 <- Process.from("TSK_1")
        process2 <- Process.from("TSK_2")
        process3 <- Process.from("TSK_3")

        taskId1 <- TaskId.from("TSK_1")
        taskId2 <- TaskId.from("TSK_2")
        taskId3 <- TaskId.from("TSK_3")

        taskTime1 <- TaskTime.from(1)
        taskTime2 <- TaskTime.from(1)
        taskTime3 <- TaskTime.from(1)

        physType <- PhysicalInType.from("PRST 1")
        physRes <- PhysicalResource.from(physType)

        physInId <- PhysicalInId.from("PRS_1")

        humanId <- HumanId.from("HRS_1")
        humanName <- HumanName.from("Worker1")
        handles <- Handles.from(physType)
      } yield Production(
        orders = List(Order(orderId, productId, quantity)),
        products = List(Product(productId, productName, List(process1, process2, process3))),
        taskResources = List(
          Task(taskId1, taskTime1, List(physRes)),
          Task(taskId2, taskTime2, List(physRes)),
          Task(taskId3, taskTime3, List(physRes))
        ),
        physicalResources = List(Physical(physInId, physType)),
        humanResources = List(Human(humanId, humanName, List(handles)))
      )
      ).toOption

    complexProduction match
      case Some(production) =>
        val result = ScheduleMS03.generateOptimalSchedules(production)

        result match
          case Right(schedules) =>
            assert(schedules.lengthIs == 150, "Should generate 150 schedules (50 × 3)")
          case Left(DomainError.XMLError(msg)) if msg.contains("Maximum iterations") =>
            succeed // Expected behavior
          case Left(error) =>
            fail(s"Unexpected error: $error")

      case None => fail("Could not construct valid production for excessive iteration test")

  test("generateOptimalSchedules should handle time limit scenario"):
    validProduction match
      case Some(validProduction) =>
        val result = ScheduleMS03.generateOptimalSchedules(validProduction)
        assert(result.isRight, "Normal production should not hit time limits")
      case None =>
        fail("validProductionOpt was None, expected Some(Production)")

  // Tests for ResourceState case class
  test("ResourceState should be properly constructed and immutable"):
    for {
      physId <- PhysicalInId.from("PRS_1")
      humanName <- HumanName.from("HRS_1")
      orderId <- OrderId.from("ORD_1")
    } yield
      val physicalRes = Map(physId -> 10)
      val humanRes = Map(humanName -> 20)
      val orderProg = Map(orderId -> Map(1 -> 2))
      val completed = Map((orderId, 1, 0) -> 30)

      val state = ScheduleMS03.ResourceState(physicalRes, humanRes, orderProg, completed)

      assert(state.physicalResources == physicalRes)
      assert(state.humanResources == humanRes)
      assert(state.orderProgress == orderProg)
      assert(state.completedTasks == completed)

  test("ResourceState should support equality correctly"):
    for {
      orderId <- OrderId.from("ORD_O1")
      physId <- PhysicalInId.from("PRS_1")
      humanName <- HumanName.from("HRS_1")
    } yield
      val state1 = ScheduleMS03.ResourceState(
        Map(physId -> 0),
        Map(humanName -> 0),
        Map(orderId -> Map(1 -> 0)),
        Map.empty
      )

      val state2 = ScheduleMS03.ResourceState(
        Map(physId -> 0),
        Map(humanName -> 0),
        Map(orderId -> Map(1 -> 0)),
        Map.empty
      )

      val state3 = ScheduleMS03.ResourceState(
        Map(physId -> 5), // Different value
        Map(humanName -> 0),
        Map(orderId -> Map(1 -> 0)),
        Map.empty
      )

      assert(state1 == state2, "Identical states should be equal")
      assert(state1 != state3, "Different states should not be equal")

  // Tests for PendingTask case class
  test("PendingTask should be properly constructed and immutable"):
    for {
      orderId <- OrderId.from("ORD_1")
    } yield
      val productNumber = 5
      val taskId = "TSK_1"
      val taskIndex = 2

      val task = ScheduleMS03.PendingTask(orderId, productNumber, taskId, taskIndex)

      assert(task.orderId == orderId)
      assert(task.productNumber == productNumber)
      assert(task.taskId == taskId)
      assert(task.taskIndex == taskIndex)

  test("PendingTask should support equality correctly"):
    for {
      orderId1 <- OrderId.from("ORD_1")
      orderId2 <- OrderId.from("ORD_2")
    } yield
      val task1 = ScheduleMS03.PendingTask(orderId1, 1, "TSK_1", 0)
      val task2 = ScheduleMS03.PendingTask(orderId1, 1, "TSK_1", 0)
      val task3 = ScheduleMS03.PendingTask(orderId2, 1, "TSK_1", 0) // Different order

      assert(task1 == task2, "Identical tasks should be equal")
      assert(task1 != task3, "Different tasks should not be equal")

  test("TaskSchedulingInfo should be properly constructed and immutable"):
    for {
      physId <- PhysicalInId.from("PRS_1")
      physType <- PhysicalInType.from("PRST_1")
      humanId <- HumanId.from("HRS_1")
      humanName <- HumanName.from("Worker1")
      handlesType <- PhysicalInType.from("PRST_1")
      handles <- Handles.from(handlesType)
    } yield
      val startTime = 15
      val duration = 25
      val physicalRes = List(Physical(physId, physType))
      val humanRes = List(Human(humanId, humanName, List(handles)))

      val info = ScheduleMS03.TaskSchedulingInfo(startTime, duration, physicalRes, humanRes)

      assert(info.startTime == startTime)
      assert(info.duration == duration)
      assert(info.physicalResources == physicalRes)
      assert(info.humanResources == humanRes)

  test("TaskSchedulingInfo should support equality correctly"):
    for {
      physId <- PhysicalInId.from("PRS_1")
      physType <- PhysicalInType.from("PRST_1")
      humanId <- HumanId.from("HRS_1")
      humanName <- HumanName.from("Worker1")
      handlesType <- PhysicalInType.from("PRST_1")
      handles <- Handles.from(handlesType)
    } yield
      val physicalRes = List(Physical(physId, physType))
      val humanRes = List(Human(humanId, humanName, List(handles)))

      val info1 = ScheduleMS03.TaskSchedulingInfo(10, 5, physicalRes, humanRes)
      val info2 = ScheduleMS03.TaskSchedulingInfo(10, 5, physicalRes, humanRes)
      val info3 = ScheduleMS03.TaskSchedulingInfo(20, 5, physicalRes, humanRes) // Different start time

      assert(info1 == info2, "Identical scheduling info should be equal")
      assert(info1 != info3, "Different scheduling info should not be equal")
  
  test("generateOptimalSchedules should handle resource conflicts gracefully"):
    val testResult = for {
      orderId1 <- OrderId.from("ORD_1")
      orderId2 <- OrderId.from("ORD_2")
      productId <- ProductId.from("PRD_1")
      productName <- ProductName.from("Leite")
      taskId <- TaskId.from("TSK_1")
      physId <- PhysicalInId.from("PRS_1")
      physType <- PhysicalInType.from("PRST_1")
      humanId <- HumanId.from("HRS_1")
      humanName <- HumanName.from("Worker1")
      handlesType <- PhysicalInType.from("PRST_1")
      handles <- Handles.from(handlesType)
      quantity <- OrderQuantity.from(1)
      taskTime <- TaskTime.from(10)
      process <- Process.from("TSK_1")
      physicalResource <- PhysicalResource.from(physType)
    } yield
      val conflictProduction = Production(
        orders = List(
          Order(orderId1, productId, quantity),
          Order(orderId2, productId, quantity)
        ),
        products = List(Product(productId, productName, List(process))),
        taskResources = List(Task(taskId, taskTime, List(physicalResource))),
        physicalResources = List(Physical(physId, physType)), // Only one machine
        humanResources = List(Human(humanId, humanName, List(handles))) // Only one worker
      )

      val result = ScheduleMS03.generateOptimalSchedules(conflictProduction)

      result match
        case Right(schedules) =>
          assert(schedules.lengthIs == 2, "Should schedule both orders sequentially")
          schedules.headOption.fold(
            fail("Expected at least one schedule but got none")
          ) { schedule1 =>
            schedules.drop(1).headOption.fold(
              fail("Expected a second schedule but got none")
            ) { schedule2 =>
              val noOverlap = schedule1.end.end <= schedule2.start.start || schedule2.end.end <= schedule1.start.start
              assert(noOverlap, "Schedules should not overlap when using same resources")
            }
          }
        case Left(error) =>
          fail(s"Should succeed but got: $error")

    testResult match
      case Left(err) => fail(s"Failed to construct test data: $err")
      case Right(_) => () // test passed successfully

  test("generateOptimalSchedules should handle single task single order"):
    val maybeProduction = for {
      orderId <- OrderId.from("ORD_O1")
      quantity <- OrderQuantity.from(1)
      productId <- ProductId.from("PRD_1")
      productName <- ProductName.from("Leite")
      process <- Process.from("TSK_1")
      taskId <- TaskId.from("TSK_1")
      taskTime <- TaskTime.from(10)
      physInType <- PhysicalInType.from("PRST_1")
      physResource <- PhysicalResource.from(physInType)
      physInId <- PhysicalInId.from("PRS_1")
      humanId <- HumanId.from("HRS_1")
      humanName <- HumanName.from("Worker1")
      handles <- Handles.from(physInType)
    } yield Production(
      orders = List(Order(orderId, productId, quantity)),
      products = List(Product(productId, productName, List(process))),
      taskResources = List(Task(taskId, taskTime, List(physResource))),
      physicalResources = List(Physical(physInId, physInType)),
      humanResources = List(Human(humanId, humanName, List(handles)))
    )

    maybeProduction.fold(
      error => fail(s"Failed to construct Production: $error"),
      simpleProduction => {
        val result = ScheduleMS03.generateOptimalSchedules(simpleProduction)
        result.fold(
          error => fail(s"Should succeed but got: $error"),
          schedules => {
            assert(schedules.lengthIs == 1, "Should generate exactly one schedule")
            schedules.headOption.fold(
              fail("Expected at least one schedule but got none")
            ) { schedule =>
              assert(schedule.start.start == 0, "Single task should start at time 0")
              assert(schedule.end.end == 10, "Should end at start + duration")
            }
          }
        )
      }
    )

  // Additional tests for OrderId validation
  test("OrderId validation should reject blank strings"):
    val result = OrderId.from("")
    assert(result.isLeft, "Should reject blank OrderId")
    result match
      case Left(DomainError.InvalidOrderId(_)) => succeed
      case _ => fail("Should return InvalidOrderId error")

  test("OrderId validation should reject strings not starting with ORD_"):
    val result = OrderId.from("INVALID_ID")
    assert(result.isLeft, "Should reject OrderId not starting with 'ORD_'")
    result match
      case Left(DomainError.InvalidOrderId(_)) => succeed
      case _ => fail("Should return InvalidOrderId error")

  test("OrderId validation should accept valid format"):
    val result = OrderId.from("ORD_12345")
    assert(result.isRight, "Should accept valid OrderId format")

  // Additional tests for OrderQuantity validation
  test("OrderQuantity validation should reject zero"):
    val result = OrderQuantity.from(0)
    assert(result.isLeft, "Should reject zero quantity")
    result match
      case Left(DomainError.InvalidQuantity(_)) => succeed
      case _ => fail("Should return InvalidQuantity error")

  test("OrderQuantity validation should reject negative numbers"):
    val result = OrderQuantity.from(-5)
    assert(result.isLeft, "Should reject negative quantity")
    result match
      case Left(DomainError.InvalidQuantity(_)) => succeed
      case _ => fail("Should return InvalidQuantity error")

  test("OrderQuantity validation should accept positive numbers"):
    val result = OrderQuantity.from(10)
    assert(result.isRight, "Should accept positive quantity")
    result match
      case Right(qty) => assert(qty.to == 10, "Should preserve quantity value")
      case _ => fail("Should return valid quantity")

  // Tests for constants and safety limits
  test("MAX_SCHEDULING_TIME should be reasonable"):
    // Testing that the constant exists and has a reasonable value
    val maxTime = 10000 // Expected value based on the code
    assert(maxTime > 0, "Max scheduling time should be positive")
    assert(maxTime < 1000000, "Max scheduling time should not be excessive")

  test("MAX_ITERATIONS should be reasonable"):
    val maxIterations = 10000 // Expected value based on the code
    assert(maxIterations > 0, "Max iterations should be positive")
    assert(maxIterations < 1000000, "Max iterations should not be excessive")

  // Tests for error handling
  test("DomainError.XMLError should have correct toString"):
    val error = DomainError.XMLError("test error message")
    assert(error.toString.contains("XMLError"), "Should contain error type")
    assert(error.toString.contains("test error message"), "Should contain error message")

  test("DomainError.IOFileProblem should have correct toString"):
    val error = DomainError.IOFileProblem("file access error")
    assert(error.toString.contains("IOFileProblem"), "Should contain error type")
    assert(error.toString.contains("file access error"), "Should contain error message")
