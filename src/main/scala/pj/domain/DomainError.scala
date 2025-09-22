package pj.domain

type Result[A] = Either[DomainError,A]

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