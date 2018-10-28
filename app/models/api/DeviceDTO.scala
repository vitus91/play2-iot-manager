package models.api

import io.swagger.annotations.{ApiModel, ApiModelProperty}
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._
import play.api.libs.json.Reads

@ApiModel
case class DeviceDTO(
                      @ApiModelProperty(name = "id", dataType = "Long", required = true, value = "Not Required at POST operation")
                      id: Option[Long],
                      @ApiModelProperty(name = "ownerId", dataType = "Long", required = true, value = "Not Required at POST operation")
                      ownerId: Option[Long],
                      @ApiModelProperty(name = "name", dataType = "String", required = false)
                      name: Option[String],
                      @ApiModelProperty(name = "device", dataType = "String", required = false)
                      device: Option[String],
                      @ApiModelProperty(name = "deviceSerialNumber", dataType = "String", required = false)
                      deviceSerialNumber: Option[String],
                      @ApiModelProperty(name = "locationLat", dataType = "String", required = false)
                      locationLat: Option[Double],
                      @ApiModelProperty(name = "locationLon", dataType = "String", required = false)
                      locationLon: Option[Double],
                      @ApiModelProperty(name = "createdDate", required = false, value = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
                      createdDate: Option[DateTime],
                      @ApiModelProperty(name = "changedDate", required = false, value = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
                      changedDate: Option[DateTime],
                      @ApiModelProperty(name = "deleted", dataType = "Boolean", required = false)
                      deleted: Option[Boolean]
                    )

object DeviceDTO {
  implicit val deviceReads = Json.reads[DeviceDTO]
  implicit val deviceWrites = Json.writes[DeviceDTO]
}
