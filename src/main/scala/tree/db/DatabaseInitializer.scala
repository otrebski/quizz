package tree.db

import cats.effect.IO
import cats.implicits._
import doobie.util.transactor.Transactor.Aux

object DatabaseInitializer {

  def createTransactor(cfg: DatabaseConfig): IO[Aux[IO, Unit]] =
    IO.delay {
      import cats.effect._
      import doobie._
      import doobie.util.ExecutionContexts
      implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContexts.synchronous)

      val xa: Aux[IO, Unit] = Transactor.fromDriverManager[IO](
        "org.postgresql.Driver",                                      // driver classname
        s"jdbc:postgresql://${cfg.host}:${cfg.port}/${cfg.database}", // connect URL (driver-specific)
        cfg.user,                                                     // user
        cfg.password                                                  // password
      )
      xa
    }

  def initDatabase(xa: Aux[IO, Unit]): IO[Int] = {
    import cats.effect._
    import doobie.implicits._
    import doobie.util.ExecutionContexts
    implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContexts.synchronous)

    val createFeedback: doobie.ConnectionIO[Int] =
      sql"""CREATE TABLE IF NOT EXISTS feedback
            |(
            |    id        SERIAL PRIMARY KEY,
            |    timestamp timestamp     NOT NULL,
            |    treeId    varchar(300)  NOT NULL,
            |    path      varchar(2000) NOT NULL,
            |    comment   varchar(5000) NOT NULL,
            |    rate      INT           NOT NULL
            |);
            | """.stripMargin.update.run
    val createMindmup: doobie.ConnectionIO[Int] =
      sql"""CREATE TABLE IF NOT EXISTS mindmup
         |(
         |    version SERIAL PRIMARY KEY,
         |    name   varchar(500),
         |    json varchar(100000)
         |)""".stripMargin.update.run

    val createTracking: doobie.ConnectionIO[Int] =
      sql"""CREATE TABLE IF NOT EXISTS trackingstep
        | (
        |    id        SERIAL PRIMARY KEY,
        |    treeId    varchar(300)  NOT NULL,
        |    path      varchar(2000) NOT NULL,
        |    date      timestamp     NOT NULL,
        |    session   varchar(5000) NOT NULL,
        |    username  varchar(400)  NULL
        | )""".stripMargin.update.run

    (createFeedback, createMindmup, createTracking).mapN(_ + _ + _).transact(xa)

  }

}
