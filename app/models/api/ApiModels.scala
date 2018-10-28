package models.api
import io.swagger.annotations._


@ApiModel
case class PagedOwnerData(
                           @ApiModelProperty(name = "data", dataType = "List[models.api.OwnerDTO]", value = "List of Owners")
                              data: OwnerDTO,
                           @ApiModelProperty(name = "prev", dataType = "String", value = "Api endpoint for previous Page")
                              prev: String,
                           @ApiModelProperty(name = "next", dataType = "String", value = "Api endpoint for next Page")
                              next: String,
                           @ApiModelProperty(name = "count", dataType = "Long", value = "Number of all available Owners")
                              count: Long
                            )



@ApiModel
case class PagedDeviceData(
                           @ApiModelProperty(name = "data", dataType = "List[models.api.DeviceDTO]", value = "List of Devices")
                           data: DeviceDTO,
                           @ApiModelProperty(name = "prev", dataType = "String", value = "Api endpoint for previous Page")
                           prev: String,
                           @ApiModelProperty(name = "next", dataType = "String", value = "Api endpoint for next Page")
                           next: String,
                           @ApiModelProperty(name = "count", dataType = "Long", value = "Number of all available Devices")
                           count: Long
                         )
