package bot


import java.util.concurrent.ConcurrentHashMap

import com.typesafe.config.{Config, ConfigFactory}
import info.mukel.telegram.bots.v2.methods._
import info.mukel.telegram.bots.v2.{Commands, Polling, TelegramBot}
import model.{AllStat, CommandHandler, Game, GameSession}
import info.mukel.telegram.bots.v2.api.Implicits._
import org.joda.time.DateTime
import scalikejdbc.WrappedResultSet

import scala.util.{Failure, Success, Try}


object BullsCowsBot extends TelegramBot with Polling with Commands {

  val sessions = new ConcurrentHashMap[Long, GameSession]()

  def config: Config = ConfigFactory.load()

  override def token: String = config.getString("token")

  on("/hello") { implicit msg => args =>
    reply(s"Hello ${msg.from.get.firstName} ${msg.from.get.lastName.getOrElse("")}! Have a nice day!")
    Thread.sleep(100)
    if (msg.from.get.id == 55689671) reply(config.getString("replyTo.nastya"))
    if (msg.from.get.firstName == "Anton") reply(config.getString("replyTo.anton"))
  }

  on("/help") { implicit msg => args =>
    reply(Messages.help)
  }

  on("/step") { implicit message => args =>

    if (sessions.containsKey(message.sender)) {
      args.mkString("") match {
        case string: String if string.length == 4 =>
          Try(string.toInt) match {
            case Success(number) =>
              val combination: List[Int] = string.map(_.toString).toList.map(_.toInt)

              val thisSession: GameSession = sessions.get(message.sender)
               // reply(thisSession.wishList.toString()) //fortest

              if (thisSession.isWin(combination)) {
                val timeMillis = new DateTime().getMillis-thisSession.time.getMillis
                val time = new DateTime(new DateTime().getMillis-thisSession.time.getMillis)
                val answerToWinner =
                  s"""Congratulations, ${message.from.get.firstName}! You win!
                  Moves: ${thisSession.log.length + 1}
                  Time: ${time.getMinuteOfHour} minutes; ${time.getSecondOfMinute} seconds
                    """
                val game = Game(0,message.sender, thisSession.log.length+1,timeMillis,new DateTime(),message.from.get.firstName + " " + message.from.get.lastName.get)
                CommandHandler.saveGameToBD(game)
                api.request(SendMessage(message.chat.id, answerToWinner))
                endGame(thisSession)
              } else {
                thisSession.doStep(combination)
                api.request(SendMessage(message.chat.id, thisSession.log.mkString("\n")))
              }


            case Failure(_) => api.request(SendMessage(message.chat.id, "It's not numbers!"))
          }
        case _ => api.request(SendMessage(message.chat.id, "Please type string with 4 numbers!"))
      }
    } else api.request(SendMessage(message.chat.id, "Please use /startnewgame command before"))
  }

  def endGame(session: GameSession) = {
    sessions.remove(session.playerId)
  }

  on("/startnewgame") { implicit msg => _ =>
    if (sessions.containsKey(msg.sender)) sessions.remove(msg.sender)
    val session: GameSession = GameSession(msg.sender,new DateTime())
    sessions.put(msg.sender, session)
    reply(Messages.startNewGame)
  }

  on("/mystat") {
    implicit msg => _ =>
      val list = CommandHandler.selectYourGamesFromBD(msg.sender)
      val statistic = CommandHandler.getYourStat(list)
      reply(statistic)
  }

  on("/allstat") {

    implicit msg => _ =>
      val list: List[AllStat] = CommandHandler.SelectAllGamesFromBD
      val statistic: List[String] = CommandHandler.getAllStat(list)
      reply("TOP 5 PLAYERS:")
      statistic.foreach(x => reply(x))

  }
}
