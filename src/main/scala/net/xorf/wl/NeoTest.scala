package net.xorf.wl

import org.neo4j.scala.{Neo4jIndexProvider, DatabaseService, EmbeddedGraphDatabaseServiceProvider, Neo4jWrapper}
import sys.ShutdownHookThread
import org.neo4j.kernel.Traversal
import scala.collection.JavaConversions._
import org.neo4j.graphdb.traversal.{Evaluators, Evaluation, Evaluator}

case class Word(text: String)

object NeoTest extends App with Neo4jWrapper with Neo4jIndexProvider with EmbeddedGraphDatabaseServiceProvider {

  ShutdownHookThread {
    shutdown(ds)
  }

  def neo4jStoreDir = "data/neo"
  override def NodeIndexConfig = ("WordsIndex", Some(Map("provider" -> "lucene", "type" -> "fulltext"))) :: Nil

  try {
    if (args.length < 2) {
      println("need to supply two words on command line")
    }
    else {
      val word1 = args(0)
      val word2 = args(1)

      withTx { implicit neo =>
        if (word1.equals("populate"))
          populate
        else {
          val out = search(word1, word2).map(_.text).mkString(" -> ")
          println(out)
        }
      }
    }
  }
  finally {
    shutdown(ds)
  }

  def search(word1: String, word2: String)(implicit neo: DatabaseService) = {
    val nodeIndex = getNodeIndex("WordsIndex").get
    val startNode = nodeIndex.get("text", word1).getSingle
    val endNode = nodeIndex.get("text", word2).getSingle

    println("starting with: " + startNode.toCC[Word].get.text)
    println("ending   with: " + endNode.toCC[Word].get.text)
    val path = Traversal.description()
      .breadthFirst()
      .evaluator(Evaluators.includeWhereEndNodeIs(endNode))
      .traverse(startNode)

      path.head.nodes.map(_.toCC[Word].get)
  }

  def populate(implicit neo: DatabaseService) {
    val words = scala.io.Source.fromFile("data/alpha").getLines()
    val nodeMap = words.map(w  => (w, createNode(Word(w)))).toArray
    val index = getNodeIndex("WordsIndex").get

    var cnt = 0

    for (i <- 0 until nodeMap.length - 1) {
      index += (nodeMap(i)._2, "text", nodeMap(i)._1)

      for (j <- i until nodeMap.length - 1) {
        cnt += 1
        val dist = DistanceMeasures.levenshtein(nodeMap(i)._1, nodeMap(j)_1)

        if (dist < 2) {
          nodeMap(i)._2 --> "LINKS" --> nodeMap(j)._2
        }
      }

      if (i % 20 == 0) {
        println("processed %d words (%s)".format(i, nodeMap(i)._1))
      }
    }
  }
}
