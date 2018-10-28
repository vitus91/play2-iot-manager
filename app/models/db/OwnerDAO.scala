package models.db

import common.messages.SlickIO
import common.messages.SlickIO.{Result, SuccessWithStatusCode, _}
import db.codegen.XPostgresProfile
import javax.inject.{Inject, Singleton}
import org.joda.time.DateTime
import slick.jdbc.JdbcBackend.Database
import dbdata.Tables
import models.api.OwnerDTO

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions

/**
  * An implementation dependent DAO.  This could be implemented by Slick, Cassandra, or a REST API.
  */
trait OwnerDAO {

  def lookup(id: Long, showDeleted: Boolean): Future[Result[Any]]

  def getCount(showDeleted: Boolean, company: Option[String]): Future[Int]

  def all(from: Int, to: Int, showDeleted: Boolean, company: Option[String]): Future[Result[SuccessWithStatusCode[Seq[OwnerDTO]]]]

  def create(owner: OwnerDTO): Future[Result[SuccessWithStatusCode[OwnerDTO]]]

  def replace(owner: OwnerDTO): Future[Result[SuccessWithStatusCode[Boolean]]]

  def update(owner: OwnerDTO): Future[Result[SuccessWithStatusCode[Boolean]]]

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
class SlickOwnerDAO @Inject()(db: Database)(implicit ec: ExecutionContext) extends OwnerDAO with Tables {

  override val profile = XPostgresProfile

  import profile.api._

  private val queryById = Compiled(
    (id: Rep[Long]) => Owner.filter(_.id === id))

  def lookup(id: Long, showDeleted: Boolean): Future[Result[SuccessWithStatusCode[OwnerDTO]]] = {
    val f: Future[Option[OwnerRow]] = db.run(queryById(id).result.headOption)
    f.map {
      case Some(row) => if(showDeleted || !row.deleted) {
        Right(SlickIO.SuccessWithStatusCode(ownerRowToOwner(row), 200))
      } else {
        Left(DatabaseError("Entity not Found", 404))
      }
      case None => Left(DatabaseError("Entity not Found", 404))
    }
  }

  def getCount(showDeleted: Boolean, company: Option[String]): Future[Int] = {
    var l = db.run(Owner.result)
       l.map(s => {
         val newSeq = company match {
           case Some(name: String) => s.filter(obj => {
             obj.company.get == name
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

  def all(from: Int, to: Int, showDeleted: Boolean, company: Option[String]): Future[Result[SuccessWithStatusCode[Seq[OwnerDTO]]]] = {
    val f = db.run(Owner.result)
    f.map( seq => {
      val newSeq = company match {
        case Some(name: String) => seq.filter( obj => {
          obj.company.get == name
        })
        case None => seq
      }
      if(showDeleted) {
        Right(SuccessWithStatusCode(newSeq.slice(from, to + 1).map(ownerRowToOwner), 200))
      } else {
        Right(SuccessWithStatusCode(newSeq.filterNot(_.deleted).slice(from, to + 1).map(ownerRowToOwner), 200))
      } 
    }
    )
  }

    def update(owner: OwnerDTO): Future[Result[SuccessWithStatusCode[Boolean]]] = {
      db.run(queryById(owner.id.getOrElse(0)).result.headOption).map {
          case Some(option) => {
            val oldOwner = ownerRowToOwner(option)
            val newOwner = owner.copy(id = owner.id,
              company = owner.company match {
                case Some(of) => Some(of)
                case None => oldOwner.company
              },
              firstName = owner.firstName match {
                case Some(of) => Some(of)
                case None => oldOwner.firstName
              },
              lastName = owner.lastName match {
                case Some(of) => Some(of)
                case None => oldOwner.lastName
              },
              zip = owner.zip match {
                case Some(of) => Some(of)
                case None => oldOwner.zip
              },
              city = owner.city match {
                case Some(of) => Some(of)
                case None => oldOwner.city
              },
              street =  owner.street match {
                case Some(of) => Some(of)
                case None => oldOwner.street
              },
              street2 = owner.street2 match {
                case Some(of) => Some(of)
                case None => oldOwner.street2
              },
              createdDate = owner.createdDate match {
                case Some(of) => Some(of)
                case None => oldOwner.createdDate
              },
              changedDate = Some(DateTime.now()),
              deleted = owner.deleted match {
                case Some(of) => Some(of)
                case None => oldOwner.deleted
              }
            )

            db.run(queryById(owner.id.getOrElse(0)).update(ownerToOwnerRow(newOwner))).map {
              case 0 => Left(DatabaseError("Could not replace entity", 500))
              case _ => Right(SuccessWithStatusCode(true, 204))
            }
          }
          case None => Future(Left(DatabaseError("Could not find entity to replace", 404)))
        }.flatten
    }

  def replace(owner: OwnerDTO): Future[Result[SuccessWithStatusCode[Boolean]]] = {
    val f: Future[Option[OwnerRow]] =  db.run(queryById(owner.id.getOrElse(0)).result.headOption)
    f.map{
        case Some(option) => {
          val oldContact = ownerRowToOwner(option)
          val newContact = owner.copy(
            id = owner.id,
            company = owner.company,
            firstName = owner.firstName,
            lastName = owner.lastName,
            zip = owner.zip ,
            city = owner.city,
            street =  owner.street,
            street2 = owner.street2,
            createdDate = oldContact.createdDate,
            changedDate = Some(DateTime.now()),
            deleted = Some(owner.deleted.getOrElse(false))
          )
          db.run(queryById(owner.id.getOrElse(0)).update(ownerToOwnerRow(newContact))).map {
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

    def create(owner: OwnerDTO): Future[Result[SuccessWithStatusCode[OwnerDTO]]] = {
      val ownerToSave = ownerToOwnerRow(owner.copy(createdDate = Some(DateTime.now())))
      val action = (Owner returning Owner.map(_.id)) += ownerToSave
      val successID = db.run(action)
      successID.map {
        case 0 => Future(Left(DatabaseError("failed to create", 500)))
        case long => {
          val idToQuery = long
          val createdContact = db.run(queryById(idToQuery).result.headOption)
          val newContact = createdContact.map {
            case Some(row) => Right(SuccessWithStatusCode(ownerRowToOwner(row), 200))
            case None => Left(DatabaseError("failed to return created entity", 500))
          }
          newContact
        }
      }.flatten
    }

  def close(): Future[Unit] = {
    Future.successful(db.close())
  }

  private def ownerToOwnerRow(owner: OwnerDTO): OwnerRow = {
    OwnerRow(
      owner.id.getOrElse(0),

      owner.company,

      owner.firstName,

      owner.lastName,

      owner.zip,

      owner.city,

      owner.street,

      owner.street2,

      owner.createdDate,

      owner.changedDate,

      owner.deleted.getOrElse(false)
    )
  }

  private def ownerRowToOwner(ownerRow: OwnerRow): OwnerDTO = {
    OwnerDTO(
      Some(ownerRow.id),

      ownerRow.company,

      ownerRow.firstName,

      ownerRow.lastName,

      ownerRow.zip,

      ownerRow.city,

      ownerRow.street,

      ownerRow.street2,

      ownerRow.createdDate,

      ownerRow.changedDate,

      None
    )
  }
}



