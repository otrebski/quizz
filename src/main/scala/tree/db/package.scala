package tree

import com.typesafe.config.Config

package object db {

  case class DatabaseConfig(
      host: String,
      port: Int,
      database: String,
      user: String,
      password: String
  )

  def databaseConfig(config: Config): DatabaseConfig =
    DatabaseConfig(
      host = config.getString("database.host"),
      port = config.getInt("database.port"),
      database = config.getString("database.dbname"),
      user = config.getString("database.user"),
      password = config.getString("database.password")
    )

}
