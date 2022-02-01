package learning.akkastreams

import java.nio.file.Paths

import akka.Done
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._
import akka.util.ByteString

import scala.concurrent._
import scala.concurrent.duration.DurationLong

object FirstSteps extends App {

  val source = Source(1 to 100)

  val done: Future[Done] = source
    .filter(_ % 10 == 0)
    .runForeach(i ⇒ println(i))

  val factorials = source.scan(BigInt(1))((acc, next) ⇒ acc * next)

  val result1: Future[IOResult] =
    factorials
      .map(num ⇒ ByteString(s"$num\n"))
      .runWith(FileIO.toPath(Paths.get("factorials1.txt")))

  def lineSink(filename: String): Sink[ByteString, Future[IOResult]] =
    Flow[ByteString]
      .map(s ⇒ ByteString(s + "\n"))
      .toMat(FileIO.toPath(Paths.get(filename)))(Keep.right)

  val result2: Future[IOResult] =
    factorials
      .map(num ⇒ ByteString(s"$num\n"))
      .runWith(lineSink("factorials2.txt"))

  val result3: Future[Done] =
    factorials
        .zipWith(Source(0 to 100))((num, idx) => s"$idx! = $num")
        .throttle(1, 1.second)
        .runForeach(println)

  // if you don't do this, the program never terminates
  //result1.onComplete(_ ⇒ system.terminate())
  //result2.onComplete(_ ⇒ system.terminate())
  result3.onComplete(_ ⇒ system.terminate())

}
