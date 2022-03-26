package section02

import akka.stream.scaladsl.*
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import scala.util.Success
import scala.util.Failure

object MaterializingStreamsExercise extends App:
  given system: ActorSystem             = ActorSystem("MaterializingStreamsExercise")
  given materializer: ActorMaterializer = ActorMaterializer()
  import system.dispatcher

  val sentences = List(
    "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
    "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.",
    "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur.",
    "Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."
  )

  val sentenceSource = Source(sentences)

  // my solutions
  sentenceSource
    // .toMat(Sink.last[String])(Keep.right).run()
    .runWith(Sink.last[String])
    .onComplete {
      case Success(sentence) => println(s"Last sentence: $sentence")
      case Failure(error)    => println(s"Last sentence graph failed: $error")
    }

  val splittingFlow = Flow[String].map(_.filter(ch => ch.isLetter || ch.isWhitespace).split(" "))
  val countingFlow  = Flow[Array[String]].map(_.length)
  val reducingSink  = Sink.reduce[Int](_ + _)
  sentenceSource
    .viaMat(splittingFlow)(Keep.right)
    .viaMat(countingFlow)(Keep.right)
    .toMat(reducingSink)(Keep.right)
    .run()
    .onComplete {
      case Success(sentence) => println(s"Word count: $sentence")
      case Failure(error)    => println(s"Word count graph failed: $error")
    }

  // Dan's solutions
  val f1 = Source(1 to 10).toMat(Sink.last)(Keep.right).run()
  val f2 = Source(1 to 10).runWith(Sink.last)

  val wordCountSink = Sink.fold[Int, String](0)((acc, sentence) => acc + sentence.split(" ").length)
  val wordCountFlow = Flow[String].fold[Int](0)((acc, sentence) => acc + sentence.split(" ").length)

  sentenceSource
    // .toMat(wordCountSink)(Keep.right).run()
    // .runFold(0)((acc, sentence) => acc + sentence.split(" ").length)
    // .runWith(wordCountSink)
    // .viaMat(wordCountFlow)(Keep.left).toMat(Sink.head)(Keep.right).run()
    .via(wordCountFlow)
    .toMat(Sink.head)(Keep.right)
    .run()
    .onComplete {
      case Success(sentence) => println(s"Dan's word count: $sentence")
      case Failure(error)    => println(s"Dan's word count graph failed: $error")
    }

  wordCountFlow
    .runWith(sentenceSource, Sink.head)
    ._2
    .onComplete {
      case Success(sentence) => println(s"Dan's second word count: $sentence")
      case Failure(error)    => println(s"Dan's second word count graph failed: $error")
    }
