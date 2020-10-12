package quizz.db

import cats.effect.IO

object DatabaseInitializer {

  def initDatabase(cfg: DatabaseConfig): IO[Int] = {
    import doobie._
    import doobie.implicits._
    import doobie.util.ExecutionContexts
    import cats.effect._
    implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContexts.synchronous)

    val xa = Transactor.fromDriverManager[IO](
      "org.postgresql.Driver",                                      // driver classname
      s"jdbc:postgresql://${cfg.host}:${cfg.port}/${cfg.database}", // connect URL (driver-specific)
      cfg.user,                                                     // user
      cfg.password                                                  // password
    )
    val create =
      sql"""   CREATE TABLE IF NOT EXISTS feedback
                (
                    id        SERIAL PRIMARY KEY,
                    timestamp timestamp     NOT NULL,
                    quizzId   varchar(300)  NOT NULL,
                    path      varchar(2000) NOT NULL,
                    comment   varchar(5000) NOT NULL,
                    rate      INT           NOT NULL
                );
        """.update.run
    create.transact(xa)
  }

}
