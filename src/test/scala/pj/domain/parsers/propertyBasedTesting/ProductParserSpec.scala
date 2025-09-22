//package pj.domain.parsers.propertyBasedTesting
//
//import org.scalatest.matchers.should.Matchers
//import org.scalatest.wordspec.AnyWordSpec
//import org.scalacheck.{Gen, Properties}
//import org.scalacheck.Prop.forAll
//import pj.domain.parsers.ProductParser
//import pj.domain.*
//import scala.xml.Elem
//
//class ProductParserSpec extends AnyWordSpec with Matchers:
//
//  // === Geradores válidos e inválidos ===
//
//  def validProductIdGen: Gen[String] =
//    Gen.nonEmptyListOf(Gen.alphaNumChar).map(chars => "PRD_" + chars.mkString)
//
//  def invalidProductIdGen: Gen[String] =
//    Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString).suchThat(!_.startsWith("PRD_"))
//
//  def validProductNameGen: Gen[String] =
//    Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString)
//
//  def validTaskIdGen: Gen[String] =
//    Gen.nonEmptyListOf(Gen.alphaNumChar).map(chars => "TSK_" + chars.mkString)
//
//  def invalidTaskIdGen: Gen[String] =
//    Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString).suchThat(!_.startsWith("TSK_"))
//
//  def createProductXml(productId: String, productName: String, taskRefs: List[String]): Elem =
//    <Products>
//      <Product id={productId} name={productName}>
//        {taskRefs.map(tskref => <Process tskref={tskref}/>)}
//      </Product>
//    </Products>
//
//  // Helper para criar o Map[String, Task] esperado pelo parser
//  def createValidTasks(taskRefs: List[String]): Map[String, Task] =
//    taskRefs.map { id =>
//      val resource = PhysicalResource(PhysicalInType("defaultType"))
//      id -> Task(TaskId(id), TaskTime(1), List(resource))
//    }.toMap
//
//  val properties = new Properties("ProductParser"):
//
//    property("valid product with taskRefs parses successfully") =
//      forAll(validProductIdGen, validProductNameGen, Gen.nonEmptyListOf(validTaskIdGen)):
//        (productId, name, taskRefs) =>
//          val xml = createProductXml(productId, name, taskRefs)
//          val validTasks = createValidTasks(taskRefs)
//
//          val expected = List(
//            Product(
//              ProductId(productId),
//              ProductName(name),
//              taskRefs.map(tskref => Process(TaskReference(tskref)))
//            )
//          )
//
//          ProductParser.parseProducts(xml, validTasks) == Right(expected)
//
//    property("product with no taskRefs parses as empty task list") =
//      forAll(validProductIdGen, validProductNameGen) { (productId, name) =>
//        val xml = createProductXml(productId, name, List.empty)
//        val validTasks = Map.empty[String, Task]
//
//        val expected = List(
//          Product(ProductId(productId), ProductName(name), List.empty)
//        )
//
//        ProductParser.parseProducts(xml, validTasks) == Right(expected)
//      }
//
//    property("invalid productId should fail") =
//      forAll(invalidProductIdGen, validProductNameGen, Gen.listOf(validTaskIdGen)):
//        (productId, name, taskRefs) =>
//          val xml = createProductXml(productId, name, taskRefs)
//          val validTasks = createValidTasks(taskRefs)
//
//          ProductParser.parseProducts(xml, validTasks) == Left(DomainError.InvalidProductId(productId))
//
//    property("invalid taskRef id should fail") =
//      forAll(validProductIdGen, validProductNameGen, Gen.nonEmptyListOf(invalidTaskIdGen)):
//        (productId, name, taskRefs) =>
//          val xml = createProductXml(productId, name, taskRefs)
//          val validTasks = Map.empty[String, Task] // Nenhuma task válida
//
//          ProductParser.parseProducts(xml, validTasks) match
//            case Left(DomainError.TaskDoesNotExist(invalid)) =>
//              taskRefs.contains(invalid) // deve estar entre os inválidos gerados
//            case _ => false
//
//    property("missing id attribute should fail") =
//      forAll(validProductNameGen, Gen.listOf(validTaskIdGen)) { (name, taskRefs) =>
//        val xml =
//          <Products>
//            <Product name={name}>
//              {taskRefs.map(tskref => <Process tskref={tskref}/>)}
//            </Product>
//          </Products>
//        val validTasks = createValidTasks(taskRefs)
//
//        ProductParser.parseProducts(xml, validTasks).isLeft
//      }
//
//    property("missing name attribute should fail") =
//      forAll(validProductIdGen, Gen.listOf(validTaskIdGen)) { (productId, taskRefs) =>
//        val xml =
//          <Products>
//            <Product id={productId}>
//              {taskRefs.map(tskref => <Process tskref={tskref}/>)}
//            </Product>
//          </Products>
//        val validTasks = createValidTasks(taskRefs)
//
//        ProductParser.parseProducts(xml, validTasks).isLeft
//      }
//
//    property("idempotency: same XML gives same result") =
//      forAll(validProductIdGen, validProductNameGen, Gen.listOf(validTaskIdGen)):
//        (productId, name, taskRefs) =>
//          val xml = createProductXml(productId, name, taskRefs)
//          val validTasks = createValidTasks(taskRefs)
//
//          val r1 = ProductParser.parseProducts(xml, validTasks)
//          val r2 = ProductParser.parseProducts(xml, validTasks)
//          r1 == r2
//
//  // === Execução dos testes usando ScalaTest ===
//  "ProductParser" should:
//    properties.properties.foreach { case (name, prop) =>
//      name in:
//        prop.check()
//    }
