package controllers

import common.messages.SlickIO.{DatabaseError, SuccessWithStatusCode}
import io.swagger.annotations.{ApiResponse, ApiResponses, _}
import javax.inject.{Inject, Singleton}
import models.api.DeviceDTO.{deviceReads, deviceWrites}
import models.api.PagedGen._
import models.api.{DeviceDTO, _}
import models.db.DeviceDAO
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.ExecutionContext

@Singleton @Api(value = "Devices")
class DeviceController @Inject()(cc: ControllerComponents, deviceDAO:  DeviceDAO)
                                (implicit ec: ExecutionContext)
  extends AbstractController(cc) {

  implicit val nilReader = Json.reads[scala.collection.immutable.Nil.type]
  implicit val nilWriter = Json.writes[scala.collection.immutable.Nil.type]
  implicit val dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"

  @ApiOperation(
    nickname = "getDevicesPaged",
    value = "Get Page of Devices Contained in PageData Object",
    notes = "Returns a PagedDataObject including an array of Devices, a prev and next link for paging and the complete Devices count",
    response = classOf[PagedDeviceData],
    httpMethod = "GET",
    produces = "application/json",
    code=200
  )
  @ApiResponses(Array(
    new ApiResponse(code = 200, message= "Ok", reference = "PagedDeviceData"),
    new ApiResponse(code = 500, message = "Internal Server Error")))
  def devices(@ApiParam(name = "owner",value="query for company", required = false )owner: Option[Long], @ApiParam(name = "startIndex",value="Index of first device of page", required = false )startIndex: Option[Int], @ApiParam(name = "endIndex",value="Index of last device of page", required = false )endIndex: Option[Int], @ApiParam(name = "showDeleted",value="Show deleted devices too", required = false ) showDeleted: Option[Boolean]) = Action.async { implicit request =>
    deviceDAO.getCount(showDeleted.getOrElse(false), owner).map { i =>
      val from = startIndex.getOrElse(0)
      var to = endIndex.getOrElse(250)
      if(to - from > 250) {
        to = from + 250
      }
      if(to > i) {
        to = i - 1
      }
      deviceDAO.all(from, to, showDeleted.getOrElse(false), owner).map {
        case Right(succ: SuccessWithStatusCode[Seq[DeviceDTO]]) => {
          val body = succ.body.map {
            owner => {
              Json.toJson(owner)
            }
          }
          val pagedData = new PagedData[JsValue](
            data = Json.toJson(body),
            prev = PagedGen.prevGen(to, from, i, "/v1/devices"),
            next = PagedGen.nextGen(to, from, i, "/v1/devices"),
            count = i
          )
          Status(succ.status)(Json.toJson(pagedData))
        }
        case Left(err: DatabaseError) => Status(err.statusCode)(err.message)
        case _ => Status(500)("Internal Server Error")
      }
    }.flatten
  }

  @ApiOperation(
    nickname = "getDeviceById",
    value = "Returns device based on given id if found",
    notes = "Id has to be provided",
    response = classOf[DeviceDTO],
    httpMethod = "GET",
    produces = "application/json",
    code=200
  )
  @ApiResponses(Array(
    new ApiResponse(code = 200, message= "OK", reference = "DeviceDTO", responseContainer = "JSON"),
    new ApiResponse(code = 404, message= "Id not found"),
    new ApiResponse(code = 500, message = "Internal Server Error")))
  def getDevice(@ApiParam(name = "id",value="Long", required = true )id: Long, @ApiParam(name = "showDeleted",value="Show device if it is deleted", required = false )showDeleted: Option[Boolean]) = Action.async { implicit request =>
    deviceDAO.lookup(id, showDeleted.getOrElse(false)).map {
      case Left(err: DatabaseError) => Status(err.statusCode)(err.message)
      case Right(succ: SuccessWithStatusCode[DeviceDTO]) => Status(succ.status)(Json.toJson(succ.body))
      case _ => Status(500)("Internal Server Error")
    }
  }

  @ApiOperation(
    nickname = "createDevice",
    value = "Creates new Device from Json Body",
    notes = "Body must be provided and ID is auto generated. Returns created Object as Json",
    response = classOf[DeviceDTO],
    httpMethod = "POST",
    consumes = "application/json",
    produces = "application/json",
    code=200
  )
  @ApiResponses(Array(
    new ApiResponse(code = 200, message= "Created", reference = "DeviceDTO", responseContainer = "JSON"),
    new ApiResponse(code = 500, message = "Internal Server Error")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "Object", value = "models.api.DeviceDTO", required = true, dataType = "models.api.DeviceDTO", paramType = "body")
  ))
  def postDevice = Action.async { implicit request =>
      val json = request.body.asJson
      val stock = json.get.as[DeviceDTO]
      deviceDAO.create(stock).map {
        case Left(err: DatabaseError) => Status(err.statusCode)(err.message)
        case Right(succ: SuccessWithStatusCode[DeviceDTO]) => Status(succ.status)(Json.toJson(succ.body))
        case _ => Status(500)("Internal Server Error")
      }
  }

  @ApiOperation(
    nickname = "deleteDevice",
    value = "Soft deletes Device",
    notes = "Id has to be provided",
    response = classOf[DeviceDTO],
    httpMethod = "DELETE",
    code=201
  )
  @ApiResponses(Array(
    new ApiResponse(code = 404, message= "Id not found"),
    new ApiResponse(code = 500, message = "Internal Server Error")))
  def deleteDevice(@ApiParam(name = "id",value="Long", required = true )id: Long) = Action.async { implicit request =>
    deviceDAO.delete(id.toLong).map {
      case Left(err: DatabaseError) => Status(err.statusCode)(err.message)
      case Right(succ: SuccessWithStatusCode[Boolean]) => Status(succ.status)
      case _ => Status(500)("Internal Server Error")
    }
  }

  @ApiOperation(
    nickname = "patchDevice",
    value = "Updates Device",
    notes = "Only Id is required. Only set fields will be updated",
    response = classOf[DeviceDTO],
    httpMethod = "PATCH",
    consumes = "application/json",
    produces = "application/json",
    code=201
  )
  @ApiResponses(Array(
    new ApiResponse(code = 500, message = "Internal Server Error")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "Object", value = "Object to be updated", required = true, dataType = "models.api.DeviceDTO", paramType = "body")
  ))
  def patchDevice = Action.async { implicit request =>
    val json = request.body.asJson
    val stock = json.get.as[DeviceDTO]
    deviceDAO.update(stock).map {
      case Left(err: DatabaseError) => Status(err.statusCode)(err.message)
      case Right(succ: SuccessWithStatusCode[Boolean]) => Status(succ.status)
      case _ => Status(500)("Internal Server Error")

    }
  }
  @ApiOperation(
    nickname = "replaceDevice",
    value = "Replaces Device",
    notes = "Only Id is required. New Object contains only the fields in request body ",
    response = classOf[DeviceDTO],
    httpMethod = "PUT",
    consumes = "application/json",
    produces = "application/json",
    code=201
  )
  @ApiResponses(Array(
    new ApiResponse(code = 500, message = "Internal Server Error")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "Object", value = "Object to be replaced", required = true, dataType = "models.api.DeviceDTO", paramType = "body")
  ))
  def putDevice = Action.async { implicit request =>
    val json = request.body.asJson
    val stock = json.get.as[DeviceDTO]
    deviceDAO.replace(stock).map {
        case Left(err: DatabaseError) => Status(err.statusCode)(err.message)
        case Right(succ: SuccessWithStatusCode[Boolean]) => Status(succ.status)
        case _ => Status(500)("Internal Server Error")
    }
  }

}
