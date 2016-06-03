package model

import org.joda.time.DateTime


case class Game(id:Long , userId: Long, steps:Int, time: Long, date: DateTime, userName: String) {
}

case class YourStat(name: String, totalGames: Int, averageSteps: Int, averageTime: Long){

  override def toString = {
    val date = new DateTime(averageTime)
    val minutes = date.getMinuteOfHour
    val seconds = date.getSecondOfMinute
    val avTime = s"$minutes min; $seconds sec"
    String.format("%-30s%-5s%-5s%-15s",name,totalGames.toString,averageSteps.toString,avTime)
  }



}
