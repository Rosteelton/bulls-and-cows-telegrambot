package bot

import java.net.URLEncoder

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import com.typesafe.config.{Config, ConfigFactory}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.util.ByteString
import info.mukel.telegram.bots.v2.{ChatActions, Commands, Polling, TelegramBot}
import info.mukel.telegram.bots.v2.api.Implicits._
import info.mukel.telegram.bots.v2.methods._
import info.mukel.telegram.bots.v2.model.InputFile.FromByteString
import info.mukel.telegram.bots.v2.model._
import spray.json.DefaultJsonProtocol
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

object ImageBot extends TelegramBot with Polling with Commands with ChatActions with DefaultJsonProtocol {

  def config: Config = ConfigFactory.load()

  override def token: String = config.getString("ImageBot.imageBotToken")

  override def handleMessage(message: Message): Unit = {
    for (text <- message.text) {
      typing(message)
      val stringToURL: String = URLEncoder.encode(text, "UTF-8")

      val bingURLPart = s"https://api.datamarket.azure.com/Bing/Search/Image?Query=%27$stringToURL%27&" + "$format=json&$top=1&ImageFilters=%27Style%3aPhoto%2bSize%3aSmall%27"
      val authorization = headers.Authorization(BasicHttpCredentials("", config.getString("ImageBot.bingToken")))
      val response: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = Uri(bingURLPart), headers = List(authorization)))
      response.onComplete {
        case Success(HttpResponse(status, headers, entity, protocol)) =>
          val responseEntity: ResponseEntity = entity.withContentType(ContentTypes.`application/json`)
          val responseString: String = Unmarshal(responseEntity).to[String].value.get.get
          val imgURL = responseString.substring(responseString.indexOf("\"MediaUrl\":"), responseString.indexOf("\",\"SourceUrl\":")).substring(12)

          println(imgURL)
          val imgFormat = imgURL.substring(imgURL.lastIndexOf(".") + 1)
          println(imgFormat)
          Try(Http().singleRequest(HttpRequest(uri = Uri(imgURL)))) match {
            case Success(value) =>
              value.onComplete({
                case Success(httpResponse) =>
                  if (httpResponse.status.intValue() == 200) {
                    for {
                      bytes <- Unmarshal(httpResponse).to[ByteString]
                    } {
                      uploadPhoto(message) // hint the user
                      val jpg = InputFile.FromByteString("picture.jpg", bytes)
                      api.request(SendPhoto(message.sender, jpg))
                    }
                  } else if (httpResponse.status.intValue() == 301) {
                    for {
                      item <- httpResponse.headers
                      if item.name() == "Location"
                    } {
                      val newURL = item.value()
                      val bytesF = getByteStringOfPictureFromURL(newURL)
                      bytesF.onComplete({
                        case Success(bytes) =>
                          uploadPhoto(message)
                          val jpg: FromByteString = InputFile.FromByteString("picture.jpg", bytes)
                          api.request(SendPhoto(message.sender, jpg))
                        case Failure(_) =>
                        case Failure(_) => api.request(SendMessage(message.chat.id, "Sorry, I don't have this picture"))
                      })
                    }
                  } else {
                    api.request(SendMessage(message.chat.id, "Sorry, I don't have this picture"))
                  }
                case Failure(_) => api.request(SendMessage(message.chat.id, "Sorry, I don't have this picture"))
              })
            case Failure(_) => api.request(SendMessage(message.chat.id, "Sorry, I don't have this picture"))
          }
      }
    }
  }

  def getByteStringOfPictureFromURL(URL: String): Future[ByteString] = {
    for {
      response <- Http().singleRequest(HttpRequest(uri = Uri(URL)))
      if response.status.isSuccess()
      bytes <- Unmarshal(response).to[ByteString]
    } yield {
      bytes
    }
  }
}
