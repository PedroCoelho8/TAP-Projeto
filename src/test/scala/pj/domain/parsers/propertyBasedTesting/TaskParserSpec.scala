//package pj.domain.parsers.propertyBasedTesting
//
//import org.scalacheck.Gen
//import org.scalacheck.Prop.forAll
//import org.scalacheck.Properties
//import pj.domain.parsers.{TaskParser, PhysicalParser, HumanParser}
//import pj.domain.*
//import scala.xml.Elem
//
//object TaskParserSpec extends Properties("Task"):
//
//  def validTaskIdGen: Gen[String] =
//    Gen.nonEmptyListOf(Gen.alphaNumChar).map(s => "TSK_" + s.mkString)
//
//  def invalidTaskIdGen: Gen[String] =
//    Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString).suchThat(!_.startsWith("TSK_"))
//
//  def validTimeGen: Gen[String] =
//    Gen.chooseNum(1, 1000).map(_.toString)
//
//  def invalidTimeGen: Gen[String] =
//    Gen.alphaStr.suchThat(s => s.nonEmpty && !s.forall(_.isDigit))
//
//  def validPhysicalIdGen: Gen[String] =
//    Gen.nonEmptyListOf(Gen.alphaNumChar).map(s => "PRS_" + s.mkString)
//
//  def validPhysicalTypeGen: Gen[String] =
//    Gen.nonEmptyListOf(Gen.alphaNumChar).map(s => "PRST " + s.mkString)
//
//  def validHumanIdGen: Gen[String] =
//    Gen.nonEmptyListOf(Gen.alphaNumChar).map(s => "HRS_" + s.mkString)
//
//  def validHumanNameGen: Gen[String] =
//    Gen.nonEmptyListOf(Gen.alphaNumChar).map(s => "Name" + s.mkString)
//    
//  property("VALID task should parse correctly") = forAll(validTaskIdGen, validTimeGen, validPhysicalTypeGen) { (taskId, time, resourceType) =>
//      val physicalId = validPhysicalIdGen.sample.getOrElse("PRS_default")
//      val humanId = validHumanIdGen.sample.getOrElse("HRS_default")
//      val humanName = validHumanNameGen.sample.getOrElse("NameDefault")
//
//      val physicalXml: Elem =
//        <Resources>
//          <Physical id={physicalId} type={resourceType}/>
//        </Resources>
//
//      val humanXml: Elem =
//        <Resources>
//          <Human id={humanId} name={humanName}>
//            <Handles type={resourceType}/>
//          </Human>
//        </Resources>
//
//      val taskXml: Elem =
//        <Tasks>
//          <Task id={taskId} time={time}>
//            <PhysicalResource type={resourceType}/>
//          </Task>
//        </Tasks>
//
//      val parsedPhysicals = PhysicalParser.parsePhysicalResources(physicalXml).getOrElse(List.empty)
//      val result = TaskParser.parseTasks(taskXml, parsedPhysicals, humanXml)
//
//      val expectedTask = Task(
//        TaskId(taskId),
//        TaskTime(time.toInt),
//        List(PhysicalResource(PhysicalInType(resourceType)))
//      )
//
//      result == Right(List(expectedTask))
//  }
//
//  property("Parse multiple tasks correctly") = forAll(Gen.listOfN(2, Gen.zip(validTaskIdGen, validTimeGen))) { taskPairs =>
//      val taskIds = taskPairs.map(_._1)
//      val uniqueTaskIds = taskIds.distinct
//
//      if (uniqueTaskIds.sizeIs == taskPairs.size)
//        val resourceType = validPhysicalTypeGen.sample.getOrElse("PRST default")
//        val physicalResources = (1 to taskPairs.size).map(i =>
//          (validPhysicalIdGen.sample.getOrElse(s"PRS_default$i"), resourceType)
//        ).toList
//
//        val humanResources = (1 to taskPairs.size).map(i =>
//          (validHumanIdGen.sample.getOrElse(s"HRS_default$i"),
//            validHumanNameGen.sample.getOrElse(s"NameDefault$i"),
//            resourceType)
//        ).toList
//
//        val physicalXml: Elem =
//          <Resources>
//            {physicalResources.map { case (id, typ) =>
//              <Physical id={id} type={typ}/>
//          }}
//          </Resources>
//
//        val humanXml: Elem =
//          <Resources>
//            {humanResources.map { case (id, name, typ) =>
//            <Human id={id} name={name}>
//              <Handles type={typ}/>
//            </Human>
//          }}
//          </Resources>
//
//        val taskXml: Elem =
//          <Tasks>
//            {taskPairs.map { case (id, time) =>
//            <Task id={id} time={time}>
//              <PhysicalResource type={resourceType}/>
//            </Task>
//          }}
//          </Tasks>
//
//        val parsedPhysicals = PhysicalParser.parsePhysicalResources(physicalXml).getOrElse(List.empty)
//        val result = TaskParser.parseTasks(taskXml, parsedPhysicals, humanXml)
//
//        val expectedTasks = taskPairs.map { case (id, time) =>
//          Task(
//            TaskId(id),
//            TaskTime(time.toInt),
//            List(PhysicalResource(PhysicalInType(resourceType)))
//          )
//        }
//
//        result == Right(expectedTasks)
//      else
//        true
//  }
//
//  property("Same XML gives same result twice") = forAll(validTaskIdGen, validTimeGen, validPhysicalTypeGen):
//      (taskId, time, resourceType) =>
//        val physicalId = validPhysicalIdGen.sample.getOrElse("PRS_default")
//        val humanId = validHumanIdGen.sample.getOrElse("HRS_default")
//        val humanName = validHumanNameGen.sample.getOrElse("NameDefault")
//
//        val physicalXml: Elem =
//          <Resources>
//            <Physical id={physicalId} type={resourceType}/>
//          </Resources>
//
//        val humanXml: Elem =
//          <Resources>
//            <Human id={humanId} name={humanName}>
//              <Handles type={resourceType}/>
//            </Human>
//          </Resources>
//
//        val taskXml: Elem =
//          <Tasks>
//            <Task id={taskId} time={time}>
//              <PhysicalResource type={resourceType}/>
//            </Task>
//          </Tasks>
//
//        val parsedPhysicals = PhysicalParser.parsePhysicalResources(physicalXml).getOrElse(List.empty)
//        val result1 = TaskParser.parseTasks(taskXml, parsedPhysicals, humanXml)
//        val result2 = TaskParser.parseTasks(taskXml, parsedPhysicals, humanXml)
//
//        result1 == result2
//
//  property("FAIL invalid task ID should fail parsing") = forAll(invalidTaskIdGen, validTimeGen, validPhysicalTypeGen) { (invalidId, time, resourceType) =>
//      val physicalId = validPhysicalIdGen.sample.getOrElse("PRS_default")
//      val humanId = validHumanIdGen.sample.getOrElse("HRS_default")
//      val humanName = validHumanNameGen.sample.getOrElse("NameDefault")
//
//      val physicalXml: Elem =
//        <Resources>
//          <Physical id={physicalId} type={resourceType}/>
//        </Resources>
//
//      val humanXml: Elem =
//        <Resources>
//          <Human id={humanId} name={humanName}>
//            <Handles type={resourceType}/>
//          </Human>
//        </Resources>
//
//      val taskXml: Elem =
//        <Tasks>
//          <Task id={invalidId} time={time}>
//            <PhysicalResource type={resourceType}/>
//          </Task>
//        </Tasks>
//
//      val parsedPhysicals = PhysicalParser.parsePhysicalResources(physicalXml).getOrElse(List.empty)
//      val result = TaskParser.parseTasks(taskXml, parsedPhysicals, humanXml)
//
//      result == Left(DomainError.InvalidTaskId(invalidId))
//  }
//
//  property("FAIL invalid time value should fail parsing") = forAll(validTaskIdGen, invalidTimeGen, validPhysicalTypeGen) { (taskId, invalidTime, resourceType) =>
//      val physicalId = validPhysicalIdGen.sample.getOrElse("PRS_default")
//      val humanId = validHumanIdGen.sample.getOrElse("HRS_default")
//      val humanName = validHumanNameGen.sample.getOrElse("NameDefault")
//
//      val physicalXml: Elem =
//        <Resources>
//          <Physical id={physicalId} type={resourceType}/>
//        </Resources>
//
//      val humanXml: Elem =
//        <Resources>
//          <Human id={humanId} name={humanName}>
//            <Handles type={resourceType}/>
//          </Human>
//        </Resources>
//
//      val taskXml: Elem =
//        <Tasks>
//          <Task id={taskId} time={invalidTime}>
//            <PhysicalResource type={resourceType}/>
//          </Task>
//        </Tasks>
//
//      val parsedPhysicals = PhysicalParser.parsePhysicalResources(physicalXml).getOrElse(List.empty)
//      val result = TaskParser.parseTasks(taskXml, parsedPhysicals, humanXml)
//
//      result.isLeft && result.left.exists:
//        case DomainError.IOFileProblem(_) => true
//        case _ => false
//  }
//
//  property("FAIL task with non-existent physical resource type should fail") = forAll(validTaskIdGen, validTimeGen, validPhysicalTypeGen,
//      validPhysicalTypeGen.suchThat(t => t.nonEmpty)):
//      (taskId, time, existingType, nonExistingTypeBase) =>
//        val nonExistingType = if (nonExistingTypeBase == existingType) s"${nonExistingTypeBase}X" else nonExistingTypeBase
//
//        val physicalId = validPhysicalIdGen.sample.getOrElse("PRS_default")
//        val humanId = validHumanIdGen.sample.getOrElse("HRS_default")
//        val humanName = validHumanNameGen.sample.getOrElse("NameDefault")
//
//        val physicalXml: Elem =
//          <Resources>
//            <Physical id={physicalId} type={existingType}/>
//          </Resources>
//
//        val humanXml: Elem =
//          <Resources>
//            <Human id={humanId} name={humanName}>
//              <Handles type={existingType}/>
//            </Human>
//          </Resources>
//
//        val taskXml: Elem =
//          <Tasks>
//            <Task id={taskId} time={time}>
//              <PhysicalResource type={nonExistingType}/>
//            </Task>
//          </Tasks>
//
//        val parsedPhysicals = PhysicalParser.parsePhysicalResources(physicalXml).getOrElse(List.empty)
//        val result = TaskParser.parseTasks(taskXml, parsedPhysicals, humanXml)
//
//        result == Left(DomainError.TaskUsesNonExistentPRT(nonExistingType))
//
//  property("FAIL task with insufficient physical resources should fail") = forAll(validTaskIdGen, validTimeGen, validPhysicalTypeGen):
//      (taskId, time, resourceType) =>
//        val physicalId = validPhysicalIdGen.sample.getOrElse("PRS_default")
//        val humanId1 = validHumanIdGen.sample.getOrElse("HRS_default1")
//        val humanId2 = validHumanIdGen.sample.getOrElse("HRS_default2")
//        val humanName1 = validHumanNameGen.sample.getOrElse("NameDefault1")
//        val humanName2 = validHumanNameGen.sample.getOrElse("NameDefault2")
//
//        val physicalXml: Elem =
//          <Resources>
//            <Physical id={physicalId} type={resourceType}/>
//          </Resources>
//
//        val humanXml: Elem =
//          <Resources>
//            <Human id={humanId1} name={humanName1}>
//              <Handles type={resourceType}/>
//            </Human>
//            <Human id={humanId2} name={humanName2}>
//              <Handles type={resourceType}/>
//            </Human>
//          </Resources>
//
//        val taskXml: Elem =
//          <Tasks>
//            <Task id={taskId} time={time}>
//              <PhysicalResource type={resourceType}/>
//              <PhysicalResource type={resourceType}/>
//            </Task>
//          </Tasks>
//
//        val parsedPhysicals = PhysicalParser.parsePhysicalResources(physicalXml).getOrElse(List.empty)
//        val result = TaskParser.parseTasks(taskXml, parsedPhysicals, humanXml)
//
//        result == Left(DomainError.ResourceUnavailable(taskId, resourceType))
//
//  property("FAIL task with insufficient human resources should fail") = forAll(validTaskIdGen, validTimeGen, validPhysicalTypeGen):
//      (taskId, time, resourceType) =>
//        val physicalId1 = validPhysicalIdGen.sample.getOrElse("PRS_default1")
//        val physicalId2 = validPhysicalIdGen.sample.getOrElse("PRS_default2")
//        val humanId = validHumanIdGen.sample.getOrElse("HRS_default")
//        val humanName = validHumanNameGen.sample.getOrElse("NameDefault")
//
//        val physicalXml: Elem =
//          <Resources>
//            <Physical id={physicalId1} type={resourceType}/>
//            <Physical id={physicalId2} type={resourceType}/>
//          </Resources>
//
//        val humanXml: Elem =
//          <Resources>
//            <Human id={humanId} name={humanName}>
//              <Handles type={resourceType}/>
//            </Human>
//          </Resources>
//          
//        val taskXml: Elem =
//          <Tasks>
//            <Task id={taskId} time={time}>
//              <PhysicalResource type={resourceType}/>
//              <PhysicalResource type={resourceType}/>
//            </Task>
//          </Tasks>
//
//        val parsedPhysicals = PhysicalParser.parsePhysicalResources(physicalXml).getOrElse(List.empty)
//        val result = TaskParser.parseTasks(taskXml, parsedPhysicals, humanXml)
//
//        result == Left(DomainError.ResourceUnavailable(taskId, resourceType))
//      
//  property("FAIL duplicate task IDs should fail parsing") = forAll(validTaskIdGen, validTimeGen, validTimeGen, validPhysicalTypeGen) { (duplicateTaskId, time1, time2, resourceType) =>
//      val physicalId = validPhysicalIdGen.sample.getOrElse("PRS_default")
//      val humanId = validHumanIdGen.sample.getOrElse("HRS_default")
//      val humanName = validHumanNameGen.sample.getOrElse("NameDefault")
//
//      val physicalXml: Elem =
//            <Resources>
//              <Physical id={physicalId} type={resourceType}/>
//            </Resources>
//
//      val humanXml: Elem =
//        <Resources>
//           <Human id={humanId} name={humanName}>
//              <Handles type={resourceType}/>
//           </Human>
//        </Resources>
//
//      val taskXml: Elem =
//        <Tasks>
//          <Task id={duplicateTaskId} time={time1}>
//            <PhysicalResource type={resourceType}/>
//          </Task>
//          <Task id={duplicateTaskId} time={time2}>
//            <PhysicalResource type={resourceType}/>
//          </Task>
//        </Tasks>
//
//      val parsedPhysicals = PhysicalParser.parsePhysicalResources(physicalXml).getOrElse(List.empty)
//      val result = TaskParser.parseTasks(taskXml, parsedPhysicals, humanXml)
//
//      result == Left(DomainError.InvalidTaskId(duplicateTaskId))
//  }