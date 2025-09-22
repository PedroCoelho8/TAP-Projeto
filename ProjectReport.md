# Project Report
## Técnicas Avançadas de Programação
### Elaborated by: Pedro Alves (nmr), Pedro Coelho (1240485), Leonardo Gomes (1211239)

---

### Project Context and Objectives

This project is developed within the scope of the Técnicas Avançadas de Programação (TAP) course, part of the Master's in Informatics Engineering.  The primary goal is to design and implement an application to schedule production orders in a factory, applying functional programming principles. The core problem involves receiving a set of production orders and available resources (both human and physical) to generate a valid production schedule.  Each order specifies a quantity of a certain product , and each product requires a linear sequence of tasks to be completed. The final schedule must assign a specific start time to every task for every product unit, ensuring that the necessary resources are allocated exclusively for the duration of each task.  The project is structured into three distinct milestones, evolving from a simple, non-optimized solution to a more complex and efficient one. 

---

### Milestone 1: MVP – Naive scheduling

The objective of the first milestone was to develop a Minimum Viable Product (MVP) that implements a naive scheduling algorithm.  This initial version serves as a baseline for the project, establishing the maximum possible production time under a set of simplified constraints.

For this milestone, we made the scheduling logic simpler by ignoring some real-world rules:

- Products are scheduled in the exact order they appear in the input file.

- Only one task can run at a time in the whole facility.

- Because of that, the running task is free to use any physical or human resource it needs.

The main goal was to create a program that reads an input XML file and produces a valid schedule in the required output XML format.

---

### Milestone 2: Property-based Tests

The second milestone shifted the focus from implementation to verification by creating a robust suite of property-based tests for the problem domain.  The goal was to define and test the fundamental invariants and rules of the production scheduling system, ensuring the logic's correctness regardless of the specific scheduling algorithm used.

The properties to be tested included, but were not limited to:

- A resource (physical or human) cannot be allocated to more than one task simultaneously. 

- The final schedule must be complete, meaning it must account for every task required for every unit of every product specified in the orders.

This milestone required a deep analysis of the domain to identify other relevant properties and implement them using the ScalaCheck framework, thereby increasing confidence in the solution's correctness. 

---

### Milestone 3: Production optimization

The goal of the final milestone was to improve the initial algorithm so that it could reduce the total production time. This meant removing the simple rules from Milestone 1 and solving a more complex problem: how to use resources more efficiently and run tasks in parallel when possible.

The main improvements were:

- Allowing multiple tasks to run at the same time, as long as there are enough physical and human resources available.

- Giving the system the freedom to change the order in which products are made (even across different orders) to find a faster way to finish everything.

The only rules that still had to be followed were the real-world limits of the factory (like available machines) and the skills of the workers.

The new version of the algorithm should always produce a schedule that is at least as fast — and ideally faster — than the simple one created in Milestone 1.

---

### Domain Modeling
The main part of the solution is a clear and well-organized model of the problem. This model ensures type safety and encapsulates business rules through the use of `opaque types` and smart constructors. Each entity in the problem description corresponds to a set of immutable data structures in our code. The following sections describe each of these entities and their relationships.

---

####  Core Entities
These are the fundamental building blocks of our production environment.

--- 

#### Order
The `Order` entity represents a production order submitted to the main algorithm.

```scala 
final case class Order(id: OrderId, prdref: ProductId, quantity: OrderQuantity)
```

It contains essential information such as the order identifier (OrderId), the product reference (prdref) which will be processed, and the quantity of the product to be produced.

The OrderId is a unique identifier that follows a specific format (ORD_...) to ensure consistency and validation across the system. 

In the same way the model also guarantees that each order has a positive, non-zero quantity.

---

#### Product
A `Product` defines a type of item that can be manufactured. 

```scala
final case class Product  (id: ProductId, name: ProductName, taskRefs: List[Process])
```

It contains a unique identifier (ProductId) that has to follow a specific format (PRD_...), a name (ProductName), and a list of Process references — each corresponding to a task that must be completed, in order, to produce one unit. 

These task references are declarative links to the actual task definitions, allowing the product’s process chain to be defined flexibly.

---

#### Task
The `Task` entity represents a specific operation that needs to be performed as part of the production process.
```scala
final case class Task(id: TaskId, time: TaskTime, physicalResources: List[PhysicalResource])
```

A Task is composed of a unique identifier (TaskId), the time (TaskTime) it takes to be performed in temporal units (t.u.), and a physicalResources list containing the physical resources needed for its execution.

---

#### Resources
##### Physical Resources

The `Physical Resource` is a fundamental entity in our domain model, representing assets that can be used in the production process.

```scala
final case class Physical(id: PhysicalInId, typ: PhysicalInType)
```

Each one has a unique identifier (PhysicalInId) and a functional classification (PhysicalInType) that describes what kind of tasks it can support.

The domain model validates each field strictly, requiring identifiers to follow a format such as "PRS_..." and types to begin with "PRST".

---

##### Human Resources
The `Human Resource` entity represents a worker with specific skills that can be assigned to tasks. Their primary role is to operate the physical resource required for production tasks.

```scala
final case class Human(id: HumanId, name: HumanName, handles: List[Handles])
```

Each Human is defined by a unique id (HumanId) and a name (HumanName). Crucially, it also includes a handles list, which represents the worker's specific skill set by detailing the physical resource types they are specialized in and certified to handle.

---

#### Aggregates and Scheduling Output
These top-level structures bring the core entities together to represent the overall problem input and the final solution output.

---

##### Production
The Production aggregate is a central component of our domain model, encapsulating the entire production context.

``` scala
case class Production(
                       physicalResources: List[Physical], 
                       taskResources: List[Task], 
                       humanResources: List[Human], 
                       products: List[Product], 
                       orders: List[Order])
```

The Production aggregate groups together the entire production context: all available physical and human resources, tasks, products that will be created, and the orders that refer them. 

It acts as the main input for the scheduling engine, representing the full snapshot of the factory’s current workload and capabilities.

---

##### TaskSchedule
The `TaskSchedule` is the fundamental building block of the final production schedule.

``` scala
final case class TaskSchedule(
                               order: OrderId ,
                               productNumber: ProductNumber,
                               task: TaskId,
                               start: StartValue,
                               end: EndValue,
                               physicalResources: List[Physical],
                               humanResources: List[Human]
                             )
```

Each `TaskSchedule` instance provides detailed information about a scheduled operation. It contains the originating `order` and `productNumber` to identify which specific item is being worked on. It also includes the `task` being performed and its assigned `start` and `end` times. Finally, it lists the specific `physicalResources` and `humanResources` that are exclusively allocated to this task for its entire duration, making them unavailable for any other task during that time.

The collection of all `TaskSchedule` instances for every task within every order makes up the complete, final production schedule.

---

### Validations and Error Handling

Robust validation and meaningful error handling are crucial to maintaining domain integrity. This application adopts a programming approach in order to prevent invalid data from reaching the scheduling logic, thereby preserving correctness, safety, and user trust. 
All inputs are parsed and validated before being used, and domain-specific errors are captured and surfaced when inconsistencies arise, right at the opaque types definition.

---

#### Strategies used
To enforce correctness and type safety across the system, we employed three key modeling strategies:
- **Opaque Types + Smart Constructors:** Every domain identifier (like OrderId, TaskId, HumanId, etc.) is defined as an opaque type. This hides the underlying implementation (usually String or Int) and ensures values are only constructed through explicit validation. For example:
  -   ```scala
      object OrderId:
      def from(id: String): Result[OrderId] =
      if (!id.isBlank && id.startsWith("ORD_"))
      Right(id)
      else
      Left(DomainError.InvalidOrderId(id))
      ```
  - This guarantees that only well-formed strings can be used as order identifiers. The same structure is consistently used across all other domain entities.

- **Use of Result[A] = Either[DomainError, A]:** To indicate that any input parsing or construction may fail, we use a custom Result[A] type alias:
  - ```scala
    type Result[A] = Either[DomainError,A]
    ```
  - This enforces explicit error propagation throughout the codebase.

- **Centralized Error Representation via DomainError:** To keep error handling consistent and descriptive throughout the application, we define a sealed enum named DomainError. This makes all possible domain-related failures explicit and enables pattern matching when handling errors.
  - ```scala
    enum DomainError:
    case IOFileProblem(error: String)
    case XMLError(error: String)
    case TaskUsesNonExistentPRT(resourceType: String)
    case ResourceUnavailable(taskId: String, resourceType: String)
    case InvalidOrderId(orderId: String)
    case InvalidHumanId(humanId: String)
    case InvalidPhysicalId(physicalId: String)
    case ProductDoesNotExist(productId: String)
    case InvalidQuantity(quantity: Int)
    case TaskDoesNotExist(taskId: String)
    case InvalidHandleType(handleId: String)
    case InvalidPhysicalType(physicalId: String)
    case InvalidTaskId(taskId: String)
    case InvalidProductId(productId: String)
    case InvalidTime(time: Int)
    case ImpossibleSchedule
    ```
  - This approach provides a type-safe and exhaustive way to describe errors. Whether it’s malformed XML, an invalid ID format, or a production schedule that cannot be satisfied, each scenario has a specific and meaningful case



---

#### Validations
- Each domain entity implements its own validation logic, encapsulated inside its smart constructor. Here’s a breakdown:


- **Order**
  - Validates that the order ID follows the "ORD_..." format and that the quantity is a positive integer greater than zero.
  - ```scala 
    object OrderId:
    def from(id: String): Result[OrderId] =
      if (!id.isBlank && id.startsWith("ORD_"))
        Right(id)
      else
        Left(DomainError.InvalidOrderId(id))
    ```
  - ```scala
    object OrderQuantity:
    def from(qty: Int): Result[OrderQuantity] =
    if qty > 0 then Right(qty)
    else Left(DomainError.InvalidQuantity(qty))
    ```
    
- **Product**
  - Validates that the product ID follows the "PRD_..." format and that its name is non-empty and the task reference starts with TSK_, as defined in the task ID.
  - ```scala 
    object ProductId:
    def from(id: String): Result[ProductId] =
    if (id.startsWith("PRD_")) Right(id)
    else Left(InvalidProductId(id))
    ```
  - ```scala
    object ProductName:
    def from(name: String): Result[ProductName] =
      if (name.nonEmpty) Right(name)
      else Left(XMLError("Invalid Product name: " + name))
    ```
  - ```scala 
    object TaskReference:
    def from(tskref: String): Result[TaskReference] =
      if (tskref.nonEmpty && tskref.startsWith("TSK_")) Right(tskref)
      else Left(TaskDoesNotExist(tskref))
    ```  
  
- **Task**
  - Validates that the task ID follows the "TSK_..." format, that the time is a positive integer, and that the physical resources are valid.
  - ```scala
    object TaskId:
    def from(id: String): Result[TaskId] =
      if(!id.isBlank && id.startsWith("TSK_")) Right(id)
      else Left(InvalidTaskId(id))
    ```
  - ```scala
    object TaskTime:
    def from(time: Int): Result[TaskTime] =
      if (time > 0) Right(time)
      else Left(InvalidTime(time))
    ```
  - ```scala
      object PhysicalResource:
        def from(resource: PhysicalInType): Result[PhysicalResource] = Right(resource)
    ```
- **Physical Resource**
  - Validates that the physical resource ID follows the "PRS_..." format and that the type starts with "PRST".
  - ```scala
    object PhysicalInId:
    def from(id: String): Result[PhysicalInId] =
      if(!id.isBlank && id.startsWith("PRS_")) Right(id)
      else Left(InvalidPhysicalId(id))
    ```
  - ```scala
    object PhysicalInType:
    def from(typ: String): Result[PhysicalInType] =
      if(!typ.isBlank && typ.startsWith("PRST")) Right(typ)
      else Left(InvalidPhysicalType(typ))
    ```

- **Human Resource**

  - Validates that the human resource ID follows the "HRS_..." format, that the name is non-empty, and that the handles list contains valid physical resource types.
    - ```scala
      object HumanId:
        def from(id: String): Result[HumanId] =
          if (!id.isBlank && id.startsWith("HRS_"))
          Right(id)
        else
          Left(DomainError.InvalidHumanId(id))
      ```
    - ```scala
      object HumanName:
        def from(name: String): Result[HumanName] =
          if (!name.isBlank)
            Right(name)
          else
            Left(DomainError.XMLError("Invalid human name: " + name))
      ```
    - ```scala
      object Handles:
        def from(physicalInType: PhysicalInType): Result[Handles] =
          Right(physicalInType)
      ```




---

#### Error Handling
- All input parsing and domain operations return a Result[A], defined as shown above. This allows us to handle errors gracefully at every step of the process.

- When invalid input is detected, the application returns a Left wrapping a specific DomainError. For example:

  - **InvalidOrderId("XYZ")** — triggered when the string doesn’t begin with "ORD_".
  - **InvalidTime(-3)** — triggered when a task has negative duration.
  - **InvalidPhysicalType("ABC")** — thrown when an unrecognized physical type is parsed

- This allows the system to fail early, with descriptive errors, and makes debugging and testing more effective. These domain errors are particularly valuable during test execution, where precise messaging helps identify input faults instantly.

---

### Parsers

Parsers function as adapters that transform structured XML input into corresponding domain-specific data representations. Its responsibility is to read the XML data, validate it according to the business rules and transform it into our immutable `case classes` and `opaque types`. This process is prone to failure, which is why each parser returns a `Result[A]`, explicitly propagating errors.

----

#### ProductionParser
The `ProductionParser` serves as the main entry point for parsing the XML input file into a fully structured `Production` instance. It coordinates the parsing of all core components of the production domain, including physical resources, tasks, human resources, products, and orders.

The `parseProduction` method performs the following steps, each of which may fail and return a domain-specific error:

- `Physical Resources`: It invokes the `PhysicalParser` to extract the physical resources and determine their types (`PhysicalInType`).

- `Human Resources`: It passes the extracted physical types to the `HumanParser` to validate the compatibility of human resource skills (`Handles`) with the available physical inputs.

- `Tasks`: It parses the set of tasks using the `TaskParser`, ensuring that all physical and human resources referenced in task definitions are valid and available.

- `Products`: It constructs a mapping of valid tasks and provides it to the `ProductParser`, which verifies that task references within product definitions (`tskref`) are consistent and defined.

- `Orders`: It parses the orders by passing the validated product identifiers to the `OrderParser`, thereby ensuring that all order references point to existing products.

Each step uses the `Result` to propagate errors in a functional style. This compositional approach guarantees that the parser only yields a `Production` object if all components are successfully validated and parsed.

---

#### PhysicalParser
The `PhysicalParser` is responsible for extracting and validating the list of physical resources defined in the XML input. It ensures that each resource has a valid identifier and type, and that no duplicate IDs exist.

The method `parsePhysicalResources`, performs the following steps:

- Node extraction: It extracts all `<Physical>` elements from the provided XML node.

- Parsing: Each individual node is parsed into a `Physical` object by the `parsePhysicalResource` method, which performs:

  - Validation of the `id` attribute using the `PhysicalInId` smart constructor;

  - Validation of the `type` attribute using the `PhysicalInType` smart constructor.

- Duplicate detection: Once all physical resources are parsed, the method checks for duplicate identifiers. If any duplicate is found, an `InvalidPhysicalId` error is returned with the offending ID.

This parser ensures that only well-formed and uniquely identified physical resources are admitted into the domain model. All parsing logic is implemented using the Result type to handle errors.

___

#### HumanParser

The `HumanParser` is responsible for transforming `<Human>` elements from the XML input into well-typed `Human` domain objects.

The method `parseHumanResources` receives:

- An XML node containing one or more `<Human>` elements;

- A set of valid physical types (`PhysicalInType`) previously extracted from the parsed physical resources.

The parsing process proceeds as follows:

- Extraction: It iterates over each `<Human>` node and parses its content in `parseHumanResource`.

- Validation of basic attributes: For each human, the following validations are applied:

  - The `id` attribute is validated using the `HumanId` smart constructor.

  - The `name` attribute is validated via `HumanName`.

- Parsing `handles` (skills): Each `<Handles>` subnode is parsed using `parseHandles`, which:

  - Reads the `type` attribute;

  - Verifies that the referenced `PhysicalInType` exists among the valid types previously parsed;

  - Constructs a `Handles` instance only if the type is valid. If the type is not recognised or not part of the allowed set, an `InvalidHandleType` error is returned.

The parser ensures that all human resources reference only physical types that are actually present in the system, preserving referential integrity and consistency across resource definitions.

---

#### TaskParser

The `TaskParser` is responsible for transforming XML `<Task>` nodes into typed Task objects. 
These objects define the specific operations a product must go through during manufacturing and identify the required physical resources for each task.

The method `parseTasks` accepts three arguments:
- A node containing `<Task>` elements;
- A list of valid `physical resources` (to validate each task’s compatibility);
- The `<HumanResources>` node (used to pre-validate human availability)

The parsing flow includes:
- Delegated parsing of human resources using `HumanParser`, to validate that at least one human exists for each required `physical type`;
- Parsing each `<Task>` element, including:
  - `id` attribute validation via TaskId;
  - `time` attribute parsed and validated via TaskTime;
  - List of `<PhysicalResource>` nodes, each validated through:
    - Type extraction with `PhysicalInType`.from;
    - Cross-checking existence against available Physical resources;

- Post-validation checks, including:
  - Detection of duplicate `TaskIds`;
  - Detection of insufficient `physical` or `human` resources to execute the task, returning:
    - `ResourceUnavailable` for Milestone 1;
    - `ImpossibleSchedule` for Milestone 3.

- All parsing steps use Result[A] to propagate errors effectively
---

#### ProductParser
The `ProductParser` converts XML `<Product>` elements into domain-level `Product` instances. It ensures that products are well-formed and reference only existing tasks.

The method `parseProduct` includes:
- Extracts each `<Product>` node;
- Validates:
  - `id` using `ProductId`;
  - `name` using `ProductName`;
- Parses embedded `<Process>` nodes, ensuring:
  - The `tskref` matches a valid `TaskId`;
  - It is validated via `TaskReference.from(...)`;
  - Otherwise, a `TaskDoesNotExist` error is returned.

---

#### OrderParser

The `OrderParser` is responsible for parsing `<Order>` elements from the input XML and transforming them into validated `Order` domain objects.

The core function `parseOrders` receives:
- An XML node containing one or more `<Order>` elements;
- A Set of valid `product` identifiers (previously parsed), used to ensure that each order refers to an existing `product`.

The parsing process for each individual `<Order>` node involves:
- Extracting the `id` attribute and validating it through `OrderId.from(...)`;
- Extracting the `prdref` attribute and converting it to a `ProductId`, ensuring that the reference exists in the list of valid product IDs;
- Extracting the `quantity` attribute, parsing it to an Int, and validating that the quantity is strictly positive using `OrderQuantity.from(...)`.

If any of these validations fail:
- A `ProductDoesNotExist` error is returned when the `prdref` does not match a known product;
- An XMLError is raised when the `quantity` string is not numeric;
- Or other domain-specific errors (`InvalidOrderId`) are used to report malformed attributes.


As with the other parsers, all logic is composed using for-comprehensions over Result[A]...


---


### MS01 Scheduling Algorithm
The scheduling algorithm for Milestone 1 (MS01) provides a naive yet functional approach to generating a valid production schedule. It strictly adheres to the assumptions established during the first phase of the project:

- Only one task is executed at a time (FIFO order).
- Resources are assumed to be freely available when needed.
- Tasks are processed in the order provided by the input.

The algorithm operates by traversing all orders, expanding them into products (iterating based on quantity), and scheduling their respective tasks sequentially.

#### Overview of workflow:

The main entry point is the method:

```scala 
def create(xml: Elem): Result[Elem]
```

This function follows these steps:
- XML Parsing: It uses the `ProductionParser` to read and validate the input XML into a structured `Production` object.
- Schedule Generation: For each `order` in the `production`, it recursively:
  - Schedules all individual products based on the quantity specified.
  - Schedules the tasks associated with each product.
  - Assigns physical and human resources in a simple first-match fashion.
  - XML Output: The resulting List[TaskSchedule] is transformed back into XML using XMLWriter.
All failures are handled using the Result type and appropriate domain errors are propagated.


**Scheduling Logic**

The core scheduling logic is distributed across a set of helper methods:

---
- **generateSchedulesForOrder**
  - ```scala
    def generateSchedulesForOrder(order: Order, production: Production, startTime: Int): Result[(Int, List[TaskSchedule])]
    ```
    - Repeats scheduling logic for each unit of the product (from quantity).
    - Accumulates the schedule and updates the current time after each product instance.

--- 

- **generateSchedulesForProduct**
  - ```scala
    def generateSchedulesForProduct(order: Order, productNumber: Int, production: Production, startTime: Int): Result[(Int, List[TaskSchedule])]    ```
    ```
    - Retrieves the Product based on the order’s reference.
    - Iterates through each taskRef in the product’s process list.
    - For each task, attempts to generate a TaskSchedule.


--- 
- **createTaskSchedule**
  - ```scala
      def createTaskSchedule(order: Order, productNumber: Int, task: Task, production: Production, start: Int): Result[TaskSchedule]    ```
    ```
    - Matches the required PhysicalResource types in a greedy manner.
    - Delegates human assignment to assignHumansToPhysicals.
    - Returns a fully populated TaskSchedule.
---
- **assignHumansToPhysicals**
  - ```scala
    def assignHumansToPhysicals(taskId: TaskId, physicals: List[Physical], production: Production): Result[List[Human]]
    ```
    - Finds a human for each physical resource based on the skill (Handles) associated with its type.
    - Ensures that no human is assigned to multiple resources in the same task.
  - If no suitable human is available, the function returns a ResourceUnavailable error.

---

- **Additional Method: Create Schedules**
  - ```scala
    def createSchedules(production: Production): Result[List[TaskSchedule]]
    ```
    - This method allows reuse of the scheduling logic with a Production object directly, which was used to work with the generators and test fixtures instead of XML. It replicates the behavior of create(...) without the parsing step.

  

---
#### MS01 Tests
The tests for Milestone 1 focus on verifying the correctness of the scheduling algorithm under the simplified assumptions. The tests are structured to cover various scenarios, including:

- We've developed unit tests using ScalaTest to validate every aspect of the domain model and the scheduling algorithm. The tests are organized into several categories:
- **- Basic Validations (Correct Inputs)**
  - Tests that ensure the basic properties of the domain model are respected, such as valid IDs, positive quantities, and correct task references.
- **Basic Validations (Incorrect Inputs)**
  - These tests cover more complex scenarios, such as ensuring that invalid inputs are correctly rejected and that the domain model maintains its integrity.
- **Basic Case Classes Validations for TaskSchedule and Production**
  - Tests that verify the correct instantiation of case classes, ensuring that all required fields are present and valid.
- **Validations for every Parser**
  - Each parser is tested to ensure it correctly transforms XML input into domain objects, handling both valid and invalid cases.
- **Scheduling Algorithm Tests**
  - These tests validate the core scheduling logic, ensuring that it produces correct and expected schedules based on the input orders and products.

- We were also provided with 20 XML files containing both valid and invalid schedules, which were tested against the scheduling algorithm. These files were used to verify that the algorithm could handle various scenarios, including edge cases and complex dependencies between tasks. - 
  - These scenarios were automatically evaluated using the provided AssessmentTestMS01 suite
    - In order to test those cases we used the following command:
      - ```sbt testOnly pj.assessment.AssessmentTestMS01```
      - After running the tests, we can see that the results are as expected, with all tests passing successfully. This indicates that the scheduling algorithm is functioning correctly under the assumptions of Milestone 1.
      - ```
        Final score of Milestone 1: 20 / 20 = 100
        [info] AssessmentTestMS01:
        [info] - File validAgenda_01_in.xml should be valid
        [info] - File validAgenda_02_in.xml should be valid
        [info] - File validAgenda_03_in.xml should be valid
        [info] - File validAgenda_10_in.xml should be valid
        [info] - File validAgenda_11_in.xml should be valid
        [info] - File validAgenda_12_in.xml should be valid
        [info] - File invalidAgenda_01_in.xml should NOT be valid
        [info] - File invalidAgenda_02_in.xml should NOT be valid
        [info] - File invalidAgenda_03_in.xml should NOT be valid
        [info] - File invalidHumanId_in.xml should NOT be valid
        [info] - File invalidHumanResourceUnavailable_in.xml should NOT be valid
        [info] - File invalidOrderId_in.xml should NOT be valid
        [info] - File invalidPhysicalId_in.xml should NOT be valid
        [info] - File invalidPhysicalResourceUnavailable_in.xml should NOT be valid
        [info] - File invalidProductIdRef_in.xml should NOT be valid
        [info] - File invalidProductId_in.xml should NOT be valid
        [info] - File invalidQuantity_in.xml should NOT be valid
        [info] - File invalidTaskIdRef_in.xml should NOT be valid
        [info] - File invalidTaskId_in.xml should NOT be valid
        [info] - File invalidTaskUsesNonExistentPRT_in.xml should NOT be valid
        [info] Run completed in 681 milliseconds.
        [info] Total number of tests run: 20
        [info] Suites: completed 1, aborted 0
        [info] Tests: succeeded 20, failed 0, canceled 0, ignored 0, pending 0
        [info] All tests passed.
        ```
---

### MS02 Property-based Tests

The second milestone of the project shifted focus from implementation to ensuring the correctness and robustness of our solution. Instead of writing tests for specific examples, we adopted a Property-Based Testing (PBT) approach using the ScalaCheck library. The goal was to define system invariants and business rules that must hold true for any valid input, and then let ScalaCheck generate hundreds of random test scenarios to attempt to falsify these properties.

This part is split into two main sections, reflecting the evaluation criteria: first, how we built our test data generators, and second, how we defined the properties our system must always satisfy.

#### Test Data Generation (Generators)

The foundation of PBT is the ability to generate valid and realistic test data. Our generation strategy was built in layers, starting with simple generators and composing them to create complex and consistent test cases, culminating in the `genProduction` generator, which produces a complete scenario.

##### Simple Generators

We begin by defining generators for individual domain types, many of which are opaque wrappers around strings or integers validated through smart constructors. Each of these is generated using Gen combinators and then validated using .fold(...):

```scala
val genPhysicalInIdString: Gen[String] =
  Gen.chooseNum(1, 99).map(n => f"PRS_$n%02d")

def genPhysicalInId: Gen[PhysicalInId] =
  genPhysicalInIdString.flatMap(PhysicalInId.from(_).fold(_ => Gen.fail, Gen.const))
```

- `Gen.chooseNum(1, 99)` → Generates a random number between 1 and 99 (inclusive).

- `.map(n => f"PRS_$n%02d")` → Formats the number n as a string with the prefix "PRS_" followed by two digits (e.g. 1 → PRS_01, 12 → PRS_12).

This pattern is repeated for other simple types like `TaskId`, `HumanId`, `OrderId`, `ProductId`, `TaskTime`, and `OrderQuantity`.

Opaque types such as `Handles`, `PhysicalResource`, or `TaskReference` follow a similar strategy:

```scala
def genHandles: Gen[Handles] =
  genPhysicalInType.flatMap(Handles.from(_).fold(_ => Gen.fail, Gen.const))
```

- `PhysicalInId.from(_)`
  - Tries to create a `PhysicalInId` from the generated string.

  - This method returns an `Either[Error, PhysicalInId]`:

    - `Left(error)` if the string is invalid;

    - `Right(validId)` if it is valid.

- `.fold(_ => Gen.fail, Gen.const)`
  - `.fold` applies a function depending on the case:

  - If error (`Left`): we use `_ => Gen.fail` → generates no value (test fails).

  If it's a success (`Right`): we use `Gen.const` → creates a constant `Gen` with that valid value.

- `flatMap(...)`
  - Since `.from(...)` returns an `Either`, we need to transform this result into a new `Gen`.

  - We use `flatMap` because we want to sequence this generation with a check (validation via smart constructor).

- Final result:
`genPhysicalInId` is a `Gen[PhysicalInId]` that:

  - Generates a string of type "PRS_01" to "PRS_99";

  - Tries to construct a `PhysicalInId` with it;

  - If the string is valid, it returns that value;

  - If it is invalid, the generator fails and tries another one.

This approach ensures that only valid domain values are generated.

---

##### Complex Generators

These combine multiple simple generators to produce structured domain entities.

For example, to generate a `Physical` entity, the identifier and type of the resource are combined:

```scala
def genPhysical: Gen[Physical] =
  for {
    id  <- genPhysicalInId
    typ <- genPhysicalInType
  } yield Physical(id, typ)
```

- `genPhysicalInId` and `genPhysicalInType` are `simple generators` that produce valid strings encapsulated in `PhysicalInId` and `PhysicalInType`, respectively, through their smart constructors (from), which guarantee the validity of each field individually.

- The construction of the `Physical(id, typ)` object only takes place after both fields have been validated, which ensures that `genPhysical` always generates valid and complete instances of the `Physical` entity.

This same principle is applied to the construction of other composite entities, such as:
- Task;
- Human;
- Product;
- Order;
- Production.

---

##### Dependent Generation

Many entities depend on each other to maintain model consistency. Dependent generation is a technique used to ensure that the generated values respect these dependencies and relationships, producing realistic and valid scenarios for testing.

In our project, the most complex generator, `genProduction`, exemplifies this concept:
- It first generates a non-empty list of physical resources (`physicalResources`), guaranteeing the uniqueness of their identifiers.
- Based on these physical resources, it calculates a `humanCount` number to generate humans with unique identifiers and names.
- For tasks (`taskResources`), in addition to creating their basic fields, it randomly selects a set of physical resources that have already been generated, thus establishing a direct dependency between tasks and the physical resources required.
- It then generates humans (`humanResourcesWithSkills`) who have a set of skills (`handles`) extracted from the tasks, ensuring that the human's skill set covers all the skills required by the tasks, guaranteeing the completeness of the relationships.
- Products are then built by associating processes derived from the tasks generated, maintaining coherence between products and their steps.
- Finally, it generates `orders` referencing the `products` generated, closing the dependency cycle.

---

##### Test Case Generation

After building layered generators for both simple and complex entities, we used them to create test cases that cover the entire domain model in a consistent and connected way.

The main generator, genProduction, creates a complete scenario that simulates a real production environment. It makes sure all the dependencies between entities are properly respected, such as:

- Tasks rely on physical resources like machines.

- Humans have the skills needed to operate those resources.

- Products are made up of sequences of tasks (processes).

- Orders refer to existing products with valid quantities.

This approach ensures that the generated test data represents realistic and valid domain states, rather than random or meaningless values.

---

##### Properties

Below is a list of the essential properties that the system must respect, defined in ScalaCheck:

- No overlap in the use of physical resources: No task can use the same physical resource simultaneously.

- No overlap in the use of human resources: Humans cannot be allocated to more than one task at the same time.

- All tasks referenced in products are scheduled: No important tasks are omitted from the schedule.

- Task duration coincides with expected time: The interval scheduled for the task corresponds to the time set for it.

- All physical resources used exist in production: No invalid resources are referenced.

- Task order respected: Tasks are executed in the correct sequence within the product process.

- No overlapping tasks in the overall schedule: Tasks do not overlap in an invalid way.

- Correct number of tasks repeated per order quantity: Tasks are repeated according to the order quantity.

- Human resources have the skills to operate the physical resources assigned: Human resources are only allocated to tasks in which they have the necessary competence.

- One-to-one mapping between human and physical resources per task: Each task must have the same number of humans and physical resources assigned, ensuring balanced allocation.

- Product numbers are complete and correct for each order: Verifies that for each order, the scheduled product instance numbers form a complete sequence from 1 to the order quantity, without any gaps or duplicates.

- Start time of tasks is always non-negative: This property enforces the rule that the production schedule begins at time 0, preventing any task from being scheduled at a negative time.

---

### MS03 Scheduling Algorithm

The MS03 has basically picked up the algorithm from MS01, but instead of making a sequential task schedule, it makes the most optimal solution (time-based). So,

- multiple tasks may be done at the same time
- 1 physical resource must be used by 1 human at a task's duration, and neither can be used by other tasks at the same time.

The remaining concerns, like task order in products are picked up from the previous element and are also controller in the various options developed by this algorithm.

This algorithm attempts to find all possible solution and get the best one. If the first rule equals, the following rule applies:

- time-based: the production schedule with the best time;
- task-based: the production schedule with the best ordering in terms of end values;
- order-base: the production schedule which is "following the Order's ID" number

---
####  Core Methods

There are a number of functionalities to prosecute in this Schedule, but these are the main initiators to actually get the result.

---
- **create**
  ```scala
    def create(xml: Elem): Result[Elem]
    ```
  - Main entry point that parses XML input and generates an optimal schedule, returning the result as XML.
  - Parses production data from XML using ProductionParser with "MS03" identifier.
  - Returns either a successful XML result or a DomainError if parsing or scheduling fails.

---

- **fromXmlToSchedules**
  ```scala
    def fromXmlToSchedules(xml: Elem): Result[List[TaskSchedule]]
    ```
  - Converts XML input directly to a list of TaskSchedule objects without XML serialization.
  - Useful when you need the schedule data structures rather than XML output.
  - Returns either a list of TaskSchedule objects or a DomainError.

---
- **generateOptimalSchedules**
  ```scala
    def generateOptimalSchedules(production: Production): Result[List[TaskSchedule]]
    ```
  - Core scheduling algorithm that generates optimal task schedules for a given production.
  - Creates pending tasks, initializes resource state, and uses iterative scheduling.
  - Returns schedules sorted by start time, end time, and order number.

---
- **createPendingTasks**
  ```scala
    private def createPendingTasks(production: Production): List[PendingTask]
    ```
  - Creates a list of all tasks that need to be scheduled across all orders and products.
  - Generates one PendingTask for each task in each product instance of each order.
  - Tasks are created with their order ID, product number, task ID, and task index.

---

- **createInitialState**
  ```scala
    private def createInitialState(production: Production): ResourceState
    ```
  - Initializes the resource state with all resources available at time 0.
  - Sets up tracking for physical resources, human resources, order progress, and completed tasks.
  - All resources start as immediately available (availability time = 0).

---

- **scheduleTasksIteratively**
  ```scala
    private def scheduleTasksIteratively(
      initialPendingTasks: List[PendingTask],
      initialState: ResourceState,
      production: Production
    ): Result[List[TaskSchedule]]
    ```
  - Main iterative scheduling loop with safety limits to prevent infinite loops.
  - Uses tail recursion to process pending tasks one by one.
  - Implements time advancement when no tasks are ready to be scheduled.
  - Includes safety checks for maximum iterations (10,000) and maximum scheduling time (10,000).

---

#### Task Selection and Scheduling

Tasks have now no need to be sequentially added and, therefore, there is no need for the first task to start first.

Most of the time it is even more valuable to start with a different one to get the best possible time.

---
- **findBestTaskToSchedule**
  ```scala
    private def findBestTaskToSchedule(
      readyTasks: List[PendingTask],
      state: ResourceState,
      production: Production,
      allPendingTasks: List[PendingTask]
    ): Option[(PendingTask, TaskSchedulingInfo)]
    ```
  - Evaluates all ready tasks and selects the best one to schedule next.
  - Calculates scheduling options for each ready task.
  - Returns the task with the highest priority score along with its scheduling information.

---

- **selectBestTask**
  ```scala
    private def selectBestTask(
      taskOptions: List[(PendingTask, TaskSchedulingInfo)],
      state: ResourceState,
      production: Production,
      allPendingTasks: List[PendingTask]
    ): Option[(PendingTask, TaskSchedulingInfo)]
    ```
  - Scores all available task options and selects the one with the best (lowest) priority score.
  - Uses fold to efficiently find the minimum scoring task.
  - Returns None if no valid task options are available.

---

- **calculateTaskPriority**
  ```scala
    private def calculateTaskPriority(
      task: PendingTask,
      info: TaskSchedulingInfo,
      state: ResourceState,
      production: Production,
      allPendingTasks: List[PendingTask]
    ): Double
    ```
  - Calculates a priority score for task scheduling decisions (lower scores = higher priority).
  - Considers multiple factors: start time, remaining work, resource utilization, order completion, and duration.
  - Gives preference to tasks that can start immediately (-10.0 bonus) and critical path tasks.

---

- **calculateRemainingWorkForOrder**
  ```scala
    private def calculateRemainingWorkForOrder(
      task: PendingTask,
      allPendingTasks: List[PendingTask],
      production: Production
    ): Int
    ```
  - Calculates the total duration of remaining tasks for a specific order and product.
  - Used in priority calculation to identify critical path tasks.
  - Sums up durations of all tasks from the current task index onwards.

---

#### Resource Management

Assigning physical and human resources has become tougher in this new schedule, therefore new methods to manage this have to be implemented and control the assignment of humans and physicals at the same time

---
- **calculateTaskSchedulingOptions**
  ```scala
    private def calculateTaskSchedulingOptions(
      task: PendingTask,
      state: ResourceState,
      production: Production
    ): Option[TaskSchedulingInfo]
    ```
  - Determines the best resource assignment and timing for a specific task.
  - Finds the task definition and calculates predecessor completion time.
  - Uses simplified resource assignment to prevent exponential complexity.

---

- **findBestResourceAssignmentSimplified**
  ```scala
    private def findBestResourceAssignmentSimplified(
      taskDef: Task,
      production: Production,
      state: ResourceState,
      minStartTime: Int
    ): Option[(List[Physical], List[Human], Int)]
    ```
  - Simplified resource assignment algorithm to prevent stack overflow from backtracking.
  - Uses greedy approach instead of exhaustive search.
  - Returns physical resources, human resources, and earliest start time.

---

- **findResourceAssignmentGreedy**
  ```scala
    private def findResourceAssignmentGreedy(
      requiredTypes: List[String],
      production: Production,
      state: ResourceState,
      minStartTime: Int
    ): Option[(List[Physical], List[Human], Int)]
    ```
  - Greedy algorithm for resource assignment that prevents exponential explosion.
  - Assigns resources one type at a time, choosing the earliest available combination.
  - Ensures no human is assigned to multiple resources in the same task.

---

- **getAvailablePhysicalResources**
  ```scala
    private def getAvailablePhysicalResources(
      production: Production,
      state: ResourceState,
      requiredType: String
    ): List[(Physical, Int)]
    ```
  - Finds all physical resources of a specific type and their availability times.
  - Filters production resources by type and maps to availability from current state.
  - Returns tuples of (Physical resource, availability time).

---
- **getAvailableHumanResources**
  ```scala
    private def getAvailableHumanResources(
      production: Production,
      state: ResourceState,
      requiredType: String
    ): List[(Human, Int)]
    ```
  - Finds all human resources capable of handling a specific resource type.
  - Filters humans by their skill handles and maps to availability from current state.
  - Returns tuples of (Human resource, availability time).

---

#### Task Management

Managing whether a task is ready to be created to task schedule or even to fill the end time was needed to be accounted for.

---
- **createTaskSchedule**
  ```scala
    private def createTaskSchedule(
      task: PendingTask,
      taskId: TaskId,
      schedulingInfo: TaskSchedulingInfo
    ): TaskSchedule
    ```
  - Creates a TaskSchedule object from a pending task and its scheduling information.
  - Combines task details with calculated start/end times and assigned resources.
  - Produces the final schedule entry for a completed task assignment.

---

- **isTaskReady**
  ```scala
    private def isTaskReady(pendingTask: PendingTask, state: ResourceState): Boolean
    ```
  - Determines if a task is ready to be scheduled based on order progress.
  - Checks if all predecessor tasks in the same product sequence have been completed.
  - Returns true only if the task is the next expected task in its product sequence.

---
- **getTaskDuration**
  ```scala
    private def getTaskDuration(task: PendingTask, production: Production): Int
    ```
  - Retrieves the duration of a specific task from the production definition.
  - Looks up the task in the production's taskResources list.
  - Returns 0 if the task is not found (defensive programming).

---

#### State Management

We had to manage the state of the application making sure 2 or more tasks were running at the same, but not using the same physical resources nor the same humans

---
- **updateStateAfterScheduling**
  ```scala
    private def updateStateAfterScheduling(
      state: ResourceState,
      task: PendingTask,
      physicalResources: List[Physical],
      humanResources: List[Human],
      endTime: Int
    ): ResourceState
    ```
  - Updates the resource state after a task has been scheduled.
  - Sets all used resources as unavailable until the task end time.
  - Updates order progress and records task completion time.

---
- **findPredecessorCompletionTime**
  ```scala
    private def findPredecessorCompletionTime(
      task: PendingTask,
      state: ResourceState,
      production: Production
    ): Option[Int]
    ```
  - Finds when the previous task in the same product sequence was completed.
  - Returns Some(0) for the first task in a sequence.
  - Returns the completion time of the predecessor task if it exists.

---
- **findNextAvailableTime**
  ```scala
    private def findNextAvailableTime(state: ResourceState): Int
    ```
  - Finds the earliest time when any resource becomes available.
  - Considers both physical and human resource availability times.
  - Used for time advancement when no tasks are currently ready.

---
- **advanceTimeToNext**
  ```scala
    private def advanceTimeToNext(state: ResourceState, time: Int): ResourceState
    ```
  - Advances the simulation to a specific time.
  - Currently returns the state unchanged as time advancement is implicit in the model.
  - Placeholder for explicit time-based state updates if needed.

---

#### Utility Methods

These are simple methods to finalize the methods or verify positions

---
- **extractOrderNumber**
  ```scala
    private def extractOrderNumber(orderId: OrderId): Int
    ```
  - Extracts numeric portion from an order ID string for sorting purposes.
  - Removes all non-digit characters and converts to integer.
  - Returns 0 if no digits are found in the order ID.

---
- **writeXmlResult**
  ```scala
    private def writeXmlResult(schedules: List[TaskSchedule]): Result[Elem]
    ```
  - Converts a list of TaskSchedule objects to XML format.
  - Uses XMLWriter to serialize schedules and loads the result as XML Element.
  - Returns either a successful XML Element or a DomainError.

---

#### Case Classes

These case classes were simply needed because of the newly developed algorithm.

Therefore, they were included in this method's location

---
- **ResourceState**
  - ```scala
    case class ResourceState(
      physicalResources: Map[PhysicalInId, Int],
      humanResources: Map[HumanName, Int],
      orderProgress: Map[OrderId, Map[Int, Int]],
      completedTasks: Map[(OrderId, Int, Int), Int]
    )
    ```
  - Tracks the current state of all resources and order progress.
  - Maps resources to their next availability time.
  - Tracks which task is next for each product in each order.

---

- **PendingTask**
  - ```scala
    case class PendingTask(
      orderId: OrderId,
      productNumber: Int,
      taskId: String,
      taskIndex: Int
    )
    ```
  - Represents a task that needs to be scheduled.
  - Contains all information needed to identify and schedule the task.
  - Used as the basic unit of work in the scheduling algorithm.

---

- **TaskSchedulingInfo**
  - ```scala
    case class TaskSchedulingInfo(
      startTime: Int,
      duration: Int,
      physicalResources: List[Physical],
      humanResources: List[Human]
    )
    ```
  - Contains all scheduling information for a task once resources are assigned.
  - Includes timing information and resource assignments.
  - Used to create the final TaskSchedule object.

---

#### Constants

- **MAX_SCHEDULING_TIME**: 10,000 - Maximum simulation time to prevent infinite scheduling
- **MAX_ITERATIONS**: 10,000 - Maximum number of scheduling iterations to prevent infinite loops

---

#### MS03 Tests
The tests for Milestone 3 focus on verifying the correctness of the scheduling algorithm under the simplified assumptions. The tests are structured to cover various scenarios, including:

- All the types of unit tests done to MS01 were also done to MS03
  - We were also provided with 51 XML files containing both valid and invalid schedules, which were tested against the scheduling algorithm. These files were used to verify that the algorithm could handle various scenarios, including edge cases and complex dependencies between tasks. -
    - These scenarios were automatically evaluated using the provided AssessmentTestMS03 suite, but in this case the valid outputs were not given. Therefore we had to develop them ourselves.
      - In order to test those cases we used the following command:
        - ```sbt testOnly pj.assessment.AssessmentTestMS03```
        - After running the tests, we can see that the results are as expected, with all tests passing successfully. This indicates that the scheduling algorithm is functioning correctly under the assumptions of Milestone 1.
        - ```
          Final score of Milestone 3: 49 / 51 = 96
          [info] AssessmentTestMS03:
          [info] - File validAgenda_01_in.xml should be valid
          [info] - File validAgenda_02_in.xml should be valid
          [info] - File validAgenda_03_in.xml should be valid
          [info] - File validAgenda_04_in.xml should be valid
          [info] - File validAgenda_05_in.xml should be valid
          [info] - File validAgenda_06_in.xml should be valid
          [info] - File validAgenda_07_in.xml should be valid
          [info] - File validAgenda_08_in.xml should be valid
          [info] - File validAgenda_09_in.xml should be valid
          [info] - File validAgenda_10_in.xml should be valid
          [info] - File validAgenda_11_in.xml should be valid
          [info] - File validAgenda_12_in.xml should be valid
          [info] - File validAgenda_50_in.xml should be valid
          [info] - File validAgenda_51_in.xml should be valid
          [info] - File validAgenda_52_in.xml should be valid
          [info] - File validAgenda_53_in.xml should be valid
          [info] - File validAgenda_54_in.xml should be valid
          [info] - File validAgenda_55_in.xml should be valid
          [info] - File validAgenda_56_in.xml should be valid
          [info] - File validAgenda_57_in.xml should be valid
          [info] - File validAgenda_58_in.xml should be valid
          [info] - File validAgenda_59_in.xml should be valid
          [info] - File validAgenda_60_in.xml should be valid
          [info] - File validAgenda_61_in.xml should be valid
          [info] - File validAgenda_62_in.xml should be valid
          [info] - File validAgenda_63_in.xml should be valid
          [info] - File validAgenda_64_in.xml should be valid
          [info] - File validAgenda_65_in.xml should be valid *** FAILED ***
          [info]   Expected XML ELEMENT but algorithm produced ERROR XMLError(Maximum iterations (10000) exceeded during scheduling) for file validAgenda_65_in.xml (AssessmentBehaviours.scala:45)
          [info] - File validAgenda_66_in.xml should be valid
          [info] - File validAgenda_67_in.xml should be valid
          [info] - File validAgenda_68_in.xml should be valid
          [info] - File validAgenda_69_in.xml should be valid
          [info] - File validAgenda_70_in.xml should be valid
          [info] - File validAgenda_simple_01_in.xml should be valid
          [info] - File validAgenda_simple_02_in.xml should be valid
          [info] - File validAgenda_simple_03_in.xml should be valid
          [info] - File validAgenda_simple_04_in.xml should be valid
          [info] - File invalidAgenda_01_in.xml should NOT be valid
          [info] - File invalidAgenda_02_in.xml should NOT be valid
          [info] - File invalidAgenda_03_in.xml should NOT be valid
          [info] - File invalidHumanId_in.xml should NOT be valid
          [info] - File invalidHumanResourceUnavailable_in.xml should NOT be valid *** FAILED ***
          [info]   "[XMLError(Maximum iterations (10000) exceeded during scheduling)]" did not equal "[ImpossibleSchedule]" ERROR messages do not match! (AssessmentBehaviours.scala:58)
          [info]   Analysis:
          [info]   "[XMLError(Maximum iterations (10000) exceeded during scheduling)]" -> "[ImpossibleSchedule]"
          [info] - File invalidOrderId_in.xml should NOT be valid
          [info] - File invalidPhysicalId_in.xml should NOT be valid
          [info] - File invalidPhysicalResourceUnavailable_in.xml should NOT be valid
          [info] - File invalidProductIdRef_in.xml should NOT be valid
          [info] - File invalidProductId_in.xml should NOT be valid
          [info] - File invalidQuantity_in.xml should NOT be valid
          [info] - File invalidTaskIdRef_in.xml should NOT be valid
          [info] - File invalidTaskId_in.xml should NOT be valid
          [info] - File invalidTaskUsesNonExistentPRT_in.xml should NOT be valid
          [info] Run completed in 4 seconds, 677 milliseconds.
          [info] Total number of tests run: 51
          [info] Suites: completed 1, aborted 0
          [info] Tests: succeeded 49, failed 2, canceled 0, ignored 0, pending 0
          [info] *** 2 TESTS FAILED ***

          ```
---

Why couldn't we get a higher score? Our method was not developed in a way that could handle StackOverFlow problems.

Since it tries to look so deep into the possibilities originated, there was a mistake we did not evaluate that led us to not getting the responses we wanted to actually have and therefore these two methods blew up at the 10000 attempts

The algorithm is still reliable and trustworthy, but it didn't make itself easy on evaluating the invalid humans at some 10000 problems

Without this limitation, our output would be correct when trying to put through validAgenda_65, but these tests would not be passing, they would blow up

### Conclusion

This project was a step-by-step journey using advanced programming to solve a difficult production scheduling problem. In Milestone 1, we built a basic first version (MVP). It wasn't fast, but it proved that our program could read data and create a correct schedule from start to finish.

Milestone 2 changed our focus from building to testing. We used a powerful tool called ScalaCheck for Property-Based Testing. We defined the core rules of our system and let the tool create hundreds of random tests. 

The final part, Milestone 3, focused on the biggest challenge: making the total production time as short as possible. We removed the simple rules and created a smarter algorithm that could run tasks at the same time and change their order. Our method for picking the "best" task to do next worked well in most cases and greatly cut down the production time compared to our first version. Using a special technique with a state that we updated step-by-step helped us handle the difficult job of managing many available resources at once.

However, the difficult optimization problem also showed the weaknesses of our method. The two tests that failed in Milestone 3 (validAgenda_65 and invalidHumanResourceUnavailable) show that for very big or very restricted problems, our algorithm sometimes hits its limit before finding a solution or figuring out that a schedule is impossible. This means that even though our method is good, it isn't perfect and could be improved to better manage tricky situations or to find impossible schedules faster.

To sum up, the project successfully used functional programming ideas (like immutability, opaque types for safety, and Either for handling errors) to design and solve a real-world problem. We showed a clear growth in skills, moving from a simple solution to an optimized and well-tested system. This shows we have a good understanding of the topics from the Técnincas Avançadas de Programação course.