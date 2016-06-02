package model

import org.joda.time.DateTime

import scala.collection.mutable.ListBuffer
import scala.util.Random

case class GameSession(playerId: Long, time: DateTime) {


  val wishList = getWishList
  val log = ListBuffer.empty[String]

  def getWishList: List[Int] = {

    val wish = Random.nextInt(10) :: Random.nextInt(10) :: Random.nextInt(10) :: Random.nextInt(10) :: Nil
    val countList: List[Int] = for {
      x <- wish
    } yield wish.count(y => y == x)

    if (countList == List(1, 1, 1, 1)) wish
    else getWishList
  }


  def doStep(numbers: List[Int]): String = {

    val listWithoutBulls = for {
      (num, wish) <- numbers.zip(wishList)
      if num != wish
    } yield (num, wish)

    val bulls: Int = 4 - listWithoutBulls.length

    val newNumbers = listWithoutBulls.unzip._1
    val newWish = listWithoutBulls.unzip._2

    val cows = (for {
      num <- newNumbers
      if newWish.contains(num)
    } yield num
      ).length

    val answer = s"${numbers.mkString("")} - bulls: $bulls; cows: $cows"
    log += answer
    answer

  }

def isWin(list: List[Int]): Boolean = if (list == wishList) true else false

}