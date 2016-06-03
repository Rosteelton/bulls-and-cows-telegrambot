package model
import scalikejdbc._
import app.App._
import org.joda.time.DateTime

import scala.util.{Failure, Success, Try}

object CommandHandler {


  def saveGameToBD(game: Game): Boolean = {
    Try(sql"insert into games (userId,steps,time,date,userName) values (${game.userId},${game.steps},${game.time},${game.date},${game.userName})".update().apply()) match {
      case Success(some) => true
      case Failure(_) => false
    }
  }

  def selectYourGamesFromBD(userId: Long): List[Game] = {
   sql"select * FROM games where userId=${userId}".map(allColumns).list().apply()
  }

def getYourStat(data: List[Game]): String = {
  data match {
    case Nil => "no data!"
    case _ =>
      val name = data.head.userName
      val totalGames = data.length
      val averageSteps: Long = Math.round(data.map(x => x.steps).sum.toDouble/totalGames)
      val averageTimeMillis: Long = Math.round(data.map(x => x.time).sum.toDouble/totalGames)
      val averageTime = getMinutesAndSecondsFromMillis(averageTimeMillis)
      s"Name: $name\nTotal games: $totalGames\nAverage steps: $averageSteps\nAverage time per game: $averageTime"
  }
}

  def getMinutesAndSecondsFromMillis(millis: Long): String = {
    val date = new DateTime(millis)
    val minutes = date.getMinuteOfHour
    val seconds = date.getSecondOfMinute
    s"$minutes min; $seconds sec"
  }


  val allColumns = (rs: WrappedResultSet) => Game(
    id = rs.long("userId"),
    userId = rs.long("userId"),
    steps = rs.int("steps"),
    time = rs.long("time"),
    date = rs.jodaDateTime("date"),
    userName = rs.string("userName")
  )

}
