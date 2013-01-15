package net.xorf.wl

import org.neo4j.scala.{Neo4jIndexProvider, DatabaseService, EmbeddedGraphDatabaseServiceProvider, Neo4jWrapper}
import sys.ShutdownHookThread
import org.neo4j.kernel.Traversal
import scala.collection.JavaConversions._
import org.neo4j.graphdb.traversal.Evaluators
import org.neo4j.graphdb.index.Index
import org.neo4j.graphdb.{Relationship, PropertyContainer, Node}
import akka.actor.{Props, ActorSystem, Actor}
import org.neo4j.tooling.GlobalGraphOperations

case class Word(text: String)
case class FindError(message: String)

object WordGraph extends Neo4jWrapper with Neo4jIndexProvider with EmbeddedGraphDatabaseServiceProvider {

  ShutdownHookThread {
    shutdown(ds)
    system.shutdown()
  }

  val system = ActorSystem("MySystem")

  def neo4jStoreDir = "data/neo_test"
  override def NodeIndexConfig = ("WordsIndex", Some(Map("provider" -> "lucene", "type" -> "fulltext"))) :: Nil

  def shutdown() {
    system.shutdown()
  }

  def search(word1: String, word2: String): Either[FindError, Iterable[Word]] = {
    withTx { implicit neo =>
      implicit val nodeIndex = getNodeIndex("WordsIndex").get

      GlobalGraphOperations.at(neo.gds).getAllNodes.iterator
      val startNode = getNode(word1)
      val endNode = getNode(word2)

      val path = for {
        sn <- startNode.right
        en <- endNode.right
      } yield {
        Traversal.description()
          .breadthFirst()
          .evaluator(Evaluators.includeWhereEndNodeIs(en))
          .traverse(sn)
      }

      for (p <- path.right) yield
        p.head.map(p =>  p.toCC[Word])
          .filter(_ nonEmpty).map(_ get)
    }
  }

  def find(word: String): Either[FindError, Node] = {
    withTx { implicit neo => {
        implicit val nodeIndex = getNodeIndex("WordsIndex").get
        getNode(word)
      }
    }
  }

  def getNode(search: String)(implicit idx: Index[Node]) = {
    val n = Option(idx.get("text", search).getSingle)
    n match {
      case Some(v) => Right(v)
      case None => Left(FindError("could not find word: %s".format(search)))
    }
  }

  case class PopulateWork(leftChunk: Seq[Node], leftOffset: Int, rightChunk: Seq[Node], rightOffset: Int)

  def buildNodes(dataFile: String)(implicit neo: DatabaseService) = {
    println("building nodes")
    val index = getNodeIndex("WordsIndex").get
    val words = scala.io.Source.fromFile(dataFile).getLines()
    val nodes = words.map(w => createNode(Word(w))).toArray
    for (n <- nodes)
      index += (n, "text", n.toCC[Word].get.text)
    nodes
  }

  def connect(nodes: Seq[Node])(implicit neo: DatabaseService) {
    //val chunkSize = scala.math.sqrt(nodes.size).toInt
    val chunkSize = nodes.size
    println("chunk size is: %d".format(chunkSize))
    val groups = nodes.grouped(chunkSize).toArray
    println("bootstrapping %d chunks".format(groups.size))
    for (i <- 0 until groups.length) {
      for (j <- 0 until groups.length) {
        if (j % scala.math.sqrt(groups.length).toInt == 0)
          println("chunk: %d %d".format(i, j))
        val work = PopulateWork(groups(i), chunkSize * i, groups(j), chunkSize * j)
        val actor = system.actorOf(Props[Populator], name = "populator_%d_%d".format(i, j))
        //populate(groups(i), chunkSize * i, groups(j), chunkSize * j)
        actor ! work
      }
    }
  }

  def bootstrap(dataFile: String) {
    withTx { implicit neo =>
      val nodes = buildNodes(dataFile)
      //connect(nodes)
    }
  }

  class Populator extends Actor {
    def receive = {
      case w: PopulateWork => withTx { implicit neo =>
        connectChunk(w.leftChunk, w.leftOffset, w.rightChunk, w.rightOffset)
      }
      case x => println("wrong type: %s".format(x.getClass))
    }
  }

  def connectChunk(leftChunk: Seq[Node], leftOffset: Int, rightChunk: Seq[Node], rightOffset: Int)(implicit neo: DatabaseService) {
    var cnt = 0
      for (i <- 0 until leftChunk.length) {
        val lnode = leftChunk(i).toCC[Word].get

        for (j <- 0 until rightChunk.length) {
          //if (leftOffset + i > rightOffset + j) {
            val rnode = rightChunk(j).toCC[Word].get
            val dist = DistanceMeasures.levenshtein1(lnode.text, rnode.text)
            cnt += 1
            if (dist < 2) {
              leftChunk(i) --> "LINKS" --> rightChunk(j)
            }
          //}
        }
        //if (i % 20 == 0) {
        //  println("processed %d words (%s)".format(i, lnode.text))
        //}
      }
      println("processed %d words".format(cnt))
  }
}
