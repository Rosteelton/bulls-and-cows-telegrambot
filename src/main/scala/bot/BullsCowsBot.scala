package bot


import java.util.concurrent.ConcurrentHashMap

import com.typesafe.config.{Config, ConfigFactory}
import info.mukel.telegram.bots.v2.methods._
import info.mukel.telegram.bots.v2.{Commands, Polling, TelegramBot}
import model.GameSession
import info.mukel.telegram.bots.v2.api.Implicits._
import scala.util.{Failure, Success, Try}
import Messages

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


              if (thisSession.isWin(combination)) {
                api.request(SendMessage(message.chat.id, s"Congratulations, ${message.from.get.firstName}! You win!\n" +
                  s"Moves: ${thisSession.log.length + 1}"))
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
    val session: GameSession = GameSession(msg.sender)
    sessions.put(msg.sender, session)
    reply(Messages.startNewGame)
  }
}
