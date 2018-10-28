package models.db

import common.messages.SlickIO
import common.messages.SlickIO.{Result, SuccessWithStatusCode, _}
import db.codegen.XPostgresProfile
import javax.inject.{Inject, Singleton}
import org.joda.time.DateTime
import slick.jdbc.JdbcBackend.Database
import dbdata.Tables
import models.api.DeviceDTO

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions

/**
  * An implementation dependent DAO.  This could be implemented by Slick, Cassandra, or a REST API.
  */
trait DeviceDAO {

  def lookup(id: Long, showDeleted: Boolean): Future[Result[Any]]

  def getCount(showDeleted: Boolean, owner: Option[Long]): Future[Int]

  def all(from: Int, to: Int, showDeleted: Boolean, owner: Option[Long]): Future[Result[SuccessWithStatusCode[Seq[DeviceDTO]]]]

  def create(deviceDTO: DeviceDTO): Future[Result[SuccessWithStatusCode[DeviceDTO]]]

  def replace(deviceDTO: DeviceDTO): Future[Result[SuccessWithStatusCode[Boolean]]]

  def update(deviceDTO: DeviceDTO): Future[Result[SuccessWithStatusCode[Boolean]]]

  def delete(id: Long): Future[Result[SuccessWithStatusCode[Boolean]]]

  def close(): Future[Unit]
}

/**
  * A Contact DAO implemented with Slick, leveraging Slick code gen.
  *
  * Note that you must run "flyway/flywayMigrate" before "compile" here.
  *
  * @param db the slick database that this contact DAO is using internally, bound through Module.
  * @param ec a CPU bound execution context.  Slick manages blocking JDBC calls with its
  *    own internal thread pool, so Play's default execution context is fine here.
  */
@Singleton
class SlickDeviceDAO @Inject()(db: Database)(implicit ec: ExecutionContext) extends DeviceDAO with Tables {

  override val profile = XPostgresProfile

  import profile.api._

  private val queryById = Compiled(
    (id: Rep[Long]) => Device.filter(_.id === id))

  def lookup(id: Long, showDeleted: Boolean): Future[Result[SuccessWithStatusCode[DeviceDTO]]] = {
    val f: Future[Option[DeviceRow]] = db.run(queryById(id).result.headOption)
    f.map {
      case Some(row) => if(showDeleted || !row.deleted) {
        Right(SlickIO.SuccessWithStatusCode(deviceRowToDevice(row), 200))
      } else {
        Left(DatabaseError("Entity not Found", 404))
      }
      case None => Left(DatabaseError("Entity not Found", 404))
    }
  }

  def getCount(showDeleted: Boolean, owner: Option[Long]): Future[Int] = {
    var l = db.run(Device.result)
       l.map(s => {
         val newSeq = owner match {
           case Some(ownerId: Long) => s.filter(obj => {
             obj.ownerid.getOrElse(0) == ownerId
           })
           case None => s
         }
         if (showDeleted) {
           newSeq.length
         } else {
           newSeq.filterNot(_.deleted).length
         }
        }

       )
  }

  def all(from: Int, to: Int, showDeleted: Boolean, owner: Option[Long]): Future[Result[SuccessWithStatusCode[Seq[DeviceDTO]]]] = {
    val f = db.run(Device.result)
    f.map( seq => {
      val newSeq = owner match {
        case Some(ownerId: Long) => seq.filter( obj => {
          obj.ownerid.getOrElse(0) == ownerId
        })
        case None => seq
      }
      if(showDeleted) {
        Right(SuccessWithStatusCode(newSeq.slice(from, to + 1).map(deviceRowToDevice), 200))
      } else {
        Right(SuccessWithStatusCode(newSeq.filterNot(_.deleted).slice(from, to + 1).map(deviceRowToDevice), 200))
      } 
    }
    )
  }

    def update(device: DeviceDTO): Future[Result[SuccessWithStatusCode[Boolean]]] = {
      db.run(queryById(device.id.getOrElse(0)).result.headOption).map {
          case Some(option) => {
            val oldDevice = deviceRowToDevice(option)
            val newDevice = device.copy(id = device.id,
              ownerId = device.ownerId match {
                case Some(of) => Some(of)
                case None => oldDevice.ownerId
              },
              name = device.name match {
                case Some(of) => Some(of)
                case None => oldDevice.name
              },
              device = device.device match {
                case Some(of) => Some(of)
                case None => oldDevice.device
              },
              deviceSerialNumber = device.deviceSerialNumber match {
                case Some(of) => Some(of)
                case None => oldDevice.deviceSerialNumber
              },
              locationLat = device.locationLat match {
                case Some(of) => Some(of)
                case None => oldDevice.locationLat
              },
              locationLon =  device.locationLon match {
                case Some(of) => Some(of)
                case None => oldDevice.locationLon
              },
              createdDate = device.createdDate match {
                case Some(of) => Some(of)
                case None => oldDevice.createdDate
              },
              changedDate = Some(DateTime.now()),
              deleted = device.deleted match {
                case Some(of) => Some(of)
                case None => oldDevice.deleted
              }
            )

            db.run(queryById(device.id.getOrElse(0)).update(deviceToDeviceRow(newDevice))).map {
              case 0 => Left(DatabaseError("Could not replace entity", 500))
              case _ => Right(SuccessWithStatusCode(true, 204))
            }
          }
          case None => Future(Left(DatabaseError("Could not find entity to replace", 404)))
        }.flatten
    }

  def replace(device: DeviceDTO): Future[Result[SuccessWithStatusCode[Boolean]]] = {
    val f: Future[Option[DeviceRow]] =  db.run(queryById(device.id.getOrElse(0)).result.headOption)
    f.map{
        case Some(option) => {
          val oldContact = deviceRowToDevice(option)
          val newContact = device.copy(
            id = device.id,
            ownerId = device.ownerId,
            name = device.name,
            device = device.device,
            deviceSerialNumber = device.deviceSerialNumber,
            locationLat = device.locationLat,
            locationLon = device.locationLon,
            createdDate = oldContact.createdDate,
            changedDate = Some(DateTime.now()),
            deleted = Some(device.deleted.getOrElse(false))
          )
          db.run(queryById(device.id.getOrElse(0)).update(deviceToDeviceRow(newContact))).map {
            case 0 => Left(DatabaseError("Could not update entity", 500))
            case _ => Right(SuccessWithStatusCode(true, 204))
          }
        }
        case None => Future(Left(DatabaseError("Could not find entity to update", 404)))
      }.flatMap(s => s)

  }


    def delete(id: Long): Future[Result[SuccessWithStatusCode[Boolean]]] = {
        db.run(queryById(id).result.headOption).map {
          case Some(entity) => {
            if (entity.deleted) {
              Future(Left(DatabaseError("could not delete entity", 500)))
            } else {
              db.run(queryById(id).update(entity.copy(deleted = true))).map {
                case 0 => Left(DatabaseError("could not delete entity", 500))
                case _ => Right(SuccessWithStatusCode(true, 204))
              }
            }
          }
          case None => Future(Left(DatabaseError("entity not found", 404)))
        }.flatten
    }

    def create(device: DeviceDTO): Future[Result[SuccessWithStatusCode[DeviceDTO]]] = {
      val deviceToSave = deviceToDeviceRow(device.copy(createdDate = Some(DateTime.now())))
      val action = (Device returning Device.map(_.id)) += deviceToSave
      val successID = db.run(action)
      successID.map {
        case 0 => Future(Left(DatabaseError("failed to create", 500)))
        case long => {
          val idToQuery = long
          val createdContact = db.run(queryById(idToQuery).result.headOption)
          val newContact = createdContact.map {
            case Some(row) => Right(SuccessWithStatusCode(deviceRowToDevice(row), 200))
            case None => Left(DatabaseError("failed to return created entity", 500))
          }
          newContact
        }
      }.flatten
    }

  def close(): Future[Unit] = {
    Future.successful(db.close())
  }

  private def deviceToDeviceRow(device: DeviceDTO): DeviceRow = {
    DeviceRow(
      device.id.getOrElse(0),
      device.ownerId,
      device.name,
      device.device,
      device.deviceSerialNumber,
      device.locationLat,
      device.locationLon,
      device.createdDate,
      device.changedDate,
      device.deleted.getOrElse(false)
    )
  }

  private def deviceRowToDevice(deviceRow: DeviceRow): DeviceDTO = {
    DeviceDTO(
      Some(deviceRow.id),
      deviceRow.ownerid,
      deviceRow.name,
      deviceRow.device,
      deviceRow.deviceSerialNumber,
      deviceRow.locationLat,
      deviceRow.locationLon,
      deviceRow.createdDate,
      deviceRow.changedDate,
      None
    )
  }
}



