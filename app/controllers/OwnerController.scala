package controllers

import akka.http.scaladsl.model.HttpResponse
import common.messages.SlickIO.{DatabaseError, SuccessWithStatusCode}
import io.swagger.annotations.{ApiResponse, ApiResponses}
import javax.inject.{Inject, Singleton}
import play.api.libs.json._
import play.api.libs.json.Json.toJson
import play.api.mvc._
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._
import play.api.libs.json.Reads
import play.api.libs.json._
import io.swagger.annotations._
import io.swagger.core._
import io.swagger.annotations.ApiKeyAuthDefinition.ApiKeyLocation
import models.api._
import models.api.PagedGen._
import models.db.{OwnerDAO}
import models.api.OwnerDTO
import models.api.OwnerDTO.ownerWrites
import models.api.OwnerDTO.ownerReads
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._
import play.api.libs.json.Reads

import scala.concurrent.ExecutionContext
@Singleton @Api(value = "Owners")
class OwnerController @Inject()(cc: ControllerComponents, ownerDAO:  OwnerDAO)
                               (implicit ec: ExecutionContext)
  extends AbstractController(cc) {

  implicit val nilReader = Json.reads[scala.collection.immutable.Nil.type]
  implicit val nilWriter = Json.writes[scala.collection.immutable.Nil.type]
  implicit val dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"

  @ApiOperation(
    nickname = "getOwnersPaged",
    value = "Get Page of Owners Contained in PageData Object",
    notes = "Returns a PagedDataObject including an array of Owners, a prev and next link for paging and the complete Owners count",
    response = classOf[PagedOwnerData],
    httpMethod = "GET",
    produces = "application/json",
    code=200
  )
  @ApiResponses(Array(
    new ApiResponse(code = 200, message= "Ok", reference = "PagedOwnerData"),
    new ApiResponse(code = 500, message = "Internal Server Error")))
  def owners(@ApiParam(name = "company",value="query for company", required = false )company: Option[String], @ApiParam(name = "startIndex",value="Index of first owner of page", required = false )startIndex: Option[Int], @ApiParam(name = "endIndex",value="Index of last owner of page", required = false )endIndex: Option[Int], @ApiParam(name = "showDeleted",value="Show deleted owners too", required = false ) showDeleted: Option[Boolean]) = Action.async { implicit request =>
    ownerDAO.getCount(showDeleted.getOrElse(false), company).map { i =>
      val from = startIndex.getOrElse(0)
      var to = endIndex.getOrElse(250)
      if(to - from > 250) {
        to = from + 250
      }
      if(to > i) {
        to = i - 1
      }
      ownerDAO.all(from, to, showDeleted.getOrElse(false), company).map {
        case Right(succ: SuccessWithStatusCode[Seq[OwnerDTO]]) => {
          val body = succ.body.map {
            owner => {
              Json.toJson(owner)
            }
          }
          val pagedData = new PagedData[JsValue](
            data = Json.toJson(body),
            prev = PagedGen.prevGen(to, from, i, "/v1/owners"),
            next = PagedGen.nextGen(to, from, i, "/v1/owners"),
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
    nickname = "getOwnerById",
    value = "Returns owner based on given id if found",
    notes = "Id has to be provided",
    response = classOf[OwnerDTO],
    httpMethod = "GET",
    produces = "application/json",
    code=200
  )
  @ApiResponses(Array(
    new ApiResponse(code = 200, message= "OK", reference = "OwnerDTO", responseContainer = "JSON"),
    new ApiResponse(code = 404, message= "Id not found"),
    new ApiResponse(code = 500, message = "Internal Server Error")))
  def getOwner(@ApiParam(name = "id",value="Long", required = true )id: Long, @ApiParam(name = "showDeleted",value="Show owner if it is deleted", required = false )showDeleted: Option[Boolean]) = Action.async { implicit request =>
    ownerDAO.lookup(id, showDeleted.getOrElse(false)).map {
      case Left(err: DatabaseError) => Status(err.statusCode)(err.message)
      case Right(succ: SuccessWithStatusCode[OwnerDTO]) => Status(succ.status)(Json.toJson(succ.body))
      case _ => Status(500)("Internal Server Error")
    }
  }

  @ApiOperation(
    nickname = "createOwner",
    value = "Creates new Owner from Json Body",
    notes = "Body must be provided and ID is auto generated. Returns created Object as Json",
    response = classOf[OwnerDTO],
    httpMethod = "POST",
    consumes = "application/json",
    produces = "application/json",
    code=200
  )
  @ApiResponses(Array(
    new ApiResponse(code = 200, message= "Created", reference = "OwnerDTO", responseContainer = "JSON"),
    new ApiResponse(code = 500, message = "Internal Server Error")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "Object", value = "models.api.OwnerDTO", required = true, dataType = "models.api.OwnerDTO", paramType = "body")
  ))
  def postOwner = Action.async { implicit request =>
      val json = request.body.asJson
      val stock = json.get.as[OwnerDTO]
      ownerDAO.create(stock).map {
        case Left(err: DatabaseError) => Status(err.statusCode)(err.message)
        case Right(succ: SuccessWithStatusCode[OwnerDTO]) => Status(succ.status)(Json.toJson(succ.body))
        case _ => Status(500)("Internal Server Error")
      }
  }

  @ApiOperation(
    nickname = "deleteOwner",
    value = "Soft deletes Owner",
    notes = "Id has to be provided",
    response = classOf[OwnerDTO],
    httpMethod = "DELETE",
    code=201
  )
  @ApiResponses(Array(
    new ApiResponse(code = 404, message= "Id not found"),
    new ApiResponse(code = 500, message = "Internal Server Error")))
  def deleteOwner(@ApiParam(name = "id",value="Long", required = true )id: Long) = Action.async { implicit request =>
    ownerDAO.delete(id.toLong).map {
      case Left(err: DatabaseError) => Status(err.statusCode)(err.message)
      case Right(succ: SuccessWithStatusCode[Boolean]) => Status(succ.status)
      case _ => Status(500)("Internal Server Error")
    }
  }

  @ApiOperation(
    nickname = "patchOwner",
    value = "Updates Owner",
    notes = "Only Id is required. Only set fields will be updated",
    response = classOf[OwnerDTO],
    httpMethod = "PATCH",
    consumes = "application/json",
    produces = "application/json",
    code=201
  )
  @ApiResponses(Array(
    new ApiResponse(code = 500, message = "Internal Server Error")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "Object", value = "Object to be updated", required = true, dataType = "models.api.OwnerDTO", paramType = "body")
  ))
  def patchOwner = Action.async { implicit request =>
    val json = request.body.asJson
    val stock = json.get.as[OwnerDTO]
    ownerDAO.update(stock).map {
      case Left(err: DatabaseError) => Status(err.statusCode)(err.message)
      case Right(succ: SuccessWithStatusCode[Boolean]) => Status(succ.status)
      case _ => Status(500)("Internal Server Error")

    }
  }
  @ApiOperation(
    nickname = "replaceOwner",
    value = "Replaces Owner",
    notes = "Only Id is required. New Object contains only the fields in request body ",
    response = classOf[OwnerDTO],
    httpMethod = "PUT",
    consumes = "application/json",
    produces = "application/json",
    code=201
  )
  @ApiResponses(Array(
    new ApiResponse(code = 500, message = "Internal Server Error")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "Object", value = "Object to be replaced", required = true, dataType = "models.api.OwnerDTO", paramType = "body")
  ))
  def putOwner = Action.async { implicit request =>
    val json = request.body.asJson
    val stock = json.get.as[OwnerDTO]
    ownerDAO.replace(stock).map {
        case Left(err: DatabaseError) => Status(err.statusCode)(err.message)
        case Right(succ: SuccessWithStatusCode[Boolean]) => Status(succ.status)
        case _ => Status(500)("Internal Server Error")
    }
  }

}
