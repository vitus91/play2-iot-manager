package models.api

import io.swagger.annotations.{ApiModel, ApiModelProperty}
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._
import play.api.libs.json.Reads

/**
  * Implementation independent aggregate root.
  */
@ApiModel
case class OwnerDTO(
                  @ApiModelProperty(name = "id", dataType = "Long", required = true, value = "Not Required at POST operation")
                  id: Option[Long],
                  @ApiModelProperty(name = "company", dataType = "String", required = false)
                  company: Option[String],
                  @ApiModelProperty(name = "firstName", dataType = "String", required = false)
                  firstName: Option[String],
                  @ApiModelProperty(name = "lastName", dataType = "String", required = false)
                  lastName: Option[String],
                  @ApiModelProperty(name = "zip", dataType = "Int", required = false)
                  zip: Option[Int],
                  @ApiModelProperty(name = "city", dataType = "String", required = false)
                  city: Option[String],
                  @ApiModelProperty(name = "street", dataType = "String", required = false)
                  street: Option[String],
                  @ApiModelProperty(name = "street2", dataType = "String", required = false)
                  street2: Option[String],
                  @ApiModelProperty(name = "createdDate", required = false, value = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
                  createdDate: Option[DateTime],
                  @ApiModelProperty(name = "changedDate", required = false, value = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
                  changedDate: Option[DateTime],
                  @ApiModelProperty(name = "deleted", dataType = "Boolean", required = false)
                  deleted: Option[Boolean]
                )


object OwnerDTO {
  implicit val ownerReads = Json.reads[OwnerDTO]
  implicit val ownerWrites = Json.writes[OwnerDTO]
}
