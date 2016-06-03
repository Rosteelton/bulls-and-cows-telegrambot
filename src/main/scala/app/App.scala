package app

import bot.BullsCowsBot
import scalikejdbc.{AutoSession, ConnectionPool, GlobalSettings, LoggingSQLAndTimeSettings}

/**
  * Created by ansolovev on 31.05.16.
  */
object App extends App{

  Class.forName("com.mysql.jdbc.Driver")
  ConnectionPool.singleton("jdbc:mysql://localhost:3306/bulls_cows_bot", "root", "12345")
  implicit val session = AutoSession
  GlobalSettings.loggingSQLAndTime = new LoggingSQLAndTimeSettings(enabled = false)
  BullsCowsBot.run()
}
