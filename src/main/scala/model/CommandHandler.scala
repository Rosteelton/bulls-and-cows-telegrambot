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

  def SelectAllGamesFromBD: List[AllStat] = {
sql"select userName, count(userName) as totalGames,AVG(steps) as averageSteps, AVG(time) as averageTime from games GROUP BY userName ORDER BY totalGames DESC limit 5"
        .map(allColumnsStat).list().apply()

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

  def getAllStat(data: List[AllStat]): List[String] = {

    data match {
      case Nil => List("no data!")
      case _ =>
        data.map(x =>{
          val time = getMinutesAndSecondsFromMillis(Math.round(x.averageTime))
          s"Name: ${x.name}\nTotal games: ${x.totalGames}\nAverage Steps: ${Math.round(x.averageSteps)}\nAverage Time: ${time}"
        })
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

  val allColumnsStat = (rs: WrappedResultSet) => AllStat(
    name = rs.string("userName"),
    totalGames = rs.int("totalGames"),
    averageSteps = rs.double("averageSteps"),
    averageTime = rs.long("averageTime")
  )

}
