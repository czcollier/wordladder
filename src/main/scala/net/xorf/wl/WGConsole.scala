package net.xorf.wl

import Profiling._
import akka.actor.ActorSystem

object WGConsole extends App {

  //val system = ActorSystem("MySystem")

  print("wg~> ")
  Iterator.continually(Console.readLine()).takeWhile(_ != "exit").foreach {
    line => {
      try {
      val cmdArgs = line.split(Array[Char](' ', ','))
      val cmdName = cmdArgs(0)

      cmdName match {
        case "populate" => {
          println("populating...")
          val dataFile = cmdArgs(1)
          timed(printTime("populated graph in: ")) {
            WordGraph.bootstrap(dataFile)
          }
        }
        case "search" => {
          val word = cmdArgs(1)
          println(WordGraph.find(word))
        }
        case "find" => {
          println("finding...")
          val word1 = cmdArgs(1)
          val word2 = cmdArgs(2)
          val out = WordGraph.search(word1, word2)
          out match {
            case Right(x) => x.map(_.toString).mkString(" -> ")
            case Left(x) => x.message
          }
          println(out)
        }
        case x => println("unknown command: '%s'".format(x))
      }
      print("wg~> ")
      }
      catch {
        case e: Throwable => println(e.getStackTrace.mkString("\n"))
      }
    }

    //system.shutdown()
    WordGraph.shutdown()
  }
}
