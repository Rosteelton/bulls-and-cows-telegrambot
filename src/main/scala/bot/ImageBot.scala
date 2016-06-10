package bot

import scala.util.matching.Regex
import java.net.URLEncoder
import spray.json._
import DefaultJsonProtocol._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import com.typesafe.config.{Config, ConfigFactory}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.util.ByteString
import com.typesafe.scalalogging.LazyLogging
import info.mukel.telegram.bots.v2.{ChatActions, Commands, Polling, TelegramBot}
import info.mukel.telegram.bots.v2.api.Implicits.{toOptionEitherL, _}
import info.mukel.telegram.bots.v2.methods._
import info.mukel.telegram.bots.v2.model.InputFile.FromByteString
import info.mukel.telegram.bots.v2.model._
import spray.json.DefaultJsonProtocol

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

object ImageBot extends TelegramBot with Polling with Commands with ChatActions with DefaultJsonProtocol with LazyLogging {

  def config: Config = ConfigFactory.load()

  override def token: String = config.getString("ImageBot.imageBotToken")

  override def handleMessage(message: Message): Unit = {
    for (text <- message.text) {
      logger.info(s"Name: ${message.from.get.firstName}, LastName: ${message.from.get.lastName.getOrElse("Unknown")}, request: ${text}")
      typing(message)

      for {
        pictureURL <- getPictureURLListFromRequest(text)
        respOpt <- fetchPicturesSeq(pictureURL)
        bytes <- getBytePictureFromResponse(respOpt)
      } {
        uploadPhoto(message) // hint the user
        val jpg = InputFile.FromByteString("picture.jpg", bytes)
        api.request(SendPhoto(message.sender, jpg))
      }
    }
  }


  def getPictureURLListFromRequest(text: String): Future[List[String]] = {
    val stringToURL: String = URLEncoder.encode(text, "UTF-8")
    val bingURLPart = s"https://api.datamarket.azure.com/Bing/Search/Image?Query=%27${stringToURL}%27&" + "$format=json&$top=5&ImageFilters=%27Style%3aPhoto%2bSize%3aSmall%27"
    val authorization = headers.Authorization(BasicHttpCredentials("", config.getString("ImageBot.bingToken")))
    val responseF: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = Uri(bingURLPart), headers = List(authorization)))

    for {
      response <- responseF
      responseEntity = response.entity.withContentType(ContentTypes.`application/json`)
      string <- Unmarshal(responseEntity).to[String]
    } yield {
      val jsonAst = string.parseJson

      val URLList = jsonAst.asJsObject.fields.get("d").get.asJsObject.fields.get("results").get.asInstanceOf[JsArray].elements
        .flatMap(_.asJsObject.fields.collect {
          case ("MediaUrl", value) => value.convertTo[String]
        }).toList
      URLList
    }
  }


  //fetch all URL than handling
  def getBytePictureFromURL(URLs: List[String]): Future[ByteString] = {

    val futurePictures: List[Future[HttpResponse]] =
      URLs
        .filter(checkURL)
        .map(fetchPicture)

    val response: Future[HttpResponse] =
      Future
        .sequence(futurePictures)
        .map(_.find(_.status.intValue() == 200).get)

    val bytePicture: Future[ByteString] = response.flatMap(resp => Unmarshal(resp).to[ByteString])
    bytePicture

  }

  def checkURL(URL: String): Boolean = if (URL.matches("[а-яА-Я]")) false else true

  def fetchPicture(someUrl: String): Future[HttpResponse] = {
    Http().singleRequest(HttpRequest(uri = Uri(someUrl)))
  }


  //fetch on by one
  def fetchPicturesSeq(urls: List[String], fetchedSuccessfully: Future[Option[HttpResponse]] = Future(None)): Future[Option[HttpResponse]] = {
    fetchedSuccessfully.flatMap { responseOpt =>
      if (responseOpt.isEmpty) {
        urls match {
          case url :: restUrls =>
            val fetched: Future[Option[HttpResponse]] = fetchPicture(url).map(res => if (res.status.intValue() == 200) Some(res) else None)
            fetchPicturesSeq(restUrls, fetched)
          case Nil => Future(responseOpt)
        }
      } else Future(responseOpt)
    }
  }

  def getBytePictureFromResponse(desiredResponse: Option[HttpResponse]): Future[ByteString] = {
    val result = desiredResponse match {
      case Some(response) =>
        Unmarshal(response).to[ByteString]
      case None =>
        fetchPicture("http://s6.uploads.ru/d/rJSiD.gif")
          .flatMap(Unmarshal(_).to[ByteString])
    }
    result
  }
}