
import java.util.Properties

import com.google.inject.AbstractModule
import com.typesafe.config.Config
import javax.inject.{Inject, Provider, Singleton}
import models.db._
import org.flywaydb.core.Flyway
import org.flywaydb.core.internal.util.jdbc.DriverDataSource
import play.api.inject.ApplicationLifecycle
import play.api.{Configuration, Environment, Logger}
import slick.jdbc.JdbcBackend.Database

import scala.concurrent.Future

/**
 * This module handles the bindings for the API to the Slick implementation.
 *
 * https://www.playframework.com/documentation/latest/ScalaDependencyInjection#Programmatic-bindings
 */
class Module(environment: Environment,
             configuration: Configuration) extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[Database]).toProvider(classOf[DatabaseProvider])
    bind(classOf[OwnerDAO]).to(classOf[SlickOwnerDAO])
    bind(classOf[DeviceDAO]).to(classOf[SlickDeviceDAO])
    bind(classOf[OwnerDAOCloseHook]).asEagerSingleton()
    bind(classOf[DeviceDAOCloseHook]).asEagerSingleton()
    bind(classOf[FlywayMigrator]).asEagerSingleton()
  }
}

/** Creates FirebaseApp on Application creation */
class FlywayMigrator @Inject()(env: Environment, configuration: Configuration) {
  Logger.info("Creating Flyway context")
  val driver = configuration.get[String]("bootstrapplay2.database.driver")
  val url = configuration.get[String]("bootstrapplay2.database.url")
  val user = configuration.get[String]("bootstrapplay2.database.user")
  val password =  configuration.get[String]("bootstrapplay2.database.password")
  val flyway = new Flyway
  flyway.setDataSource(new DriverDataSource(env.classLoader, driver, url, user, password, new Properties()))
  //on k8s with sbtReactivePlugin flyway.setLocations("filesystem:modules/flyway/src/main/resources/db/migration")
  flyway.setLocations("filesystem:/opt/docker/conf/migration")
  Logger.info("Flyway/Migrate")
  flyway.migrate()
  flyway.info().all().map(a => Logger.info(a.getChecksum.toString.concat(" ".concat(a.getDescription))).toString)
  Logger.info("MIGRATION FINISHED")
}
@Singleton
class DatabaseProvider @Inject() (config: Config) extends Provider[Database] {
  lazy val get = Database.forConfig("bootstrapplay2.database", config)
}

/** Closes database connections safely.  Important on dev restart. */
class OwnerDAOCloseHook @Inject()(dao: OwnerDAO, lifecycle: ApplicationLifecycle) {
  lifecycle.addStopHook { () =>
    Future.successful(dao.close())
  }
}

/** Closes database connections safely.  Important on dev restart. */
class DeviceDAOCloseHook @Inject()(dao: DeviceDAO, lifecycle: ApplicationLifecycle) {
  lifecycle.addStopHook { () =>
    Future.successful(dao.close())
  }
}


