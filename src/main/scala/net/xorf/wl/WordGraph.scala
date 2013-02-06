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
    //system.shutdown()
  }

  //val system = ActorSystem("MySystem")

  def neo4jStoreDir = "data/neo_test"
  override def NodeIndexConfig = ("WordsIndex", Some(Map("provider" -> "lucene", "type" -> "fulltext"))) :: Nil

  def shutdown() {
    //system.shutdown()
  }

  def search(word1: String, word2: String): Either[FindError, Iterable[Word]] = {
    withTx { implicit neo =>
      implicit val nodeIndex = getNodeIndex("WordsIndex").get

      //GlobalGraphOperations.at(neo.gds).getAllNodes.iterator
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

  def bootstrap(dataFile: String) {
    withTx { implicit neo =>
      build(dataFile)
    }
  }

  private def build(dataFile: String)(implicit neo: DatabaseService) {
    println("building nodes")
    val words = scala.io.Source.fromFile(dataFile).getLines.toIterable
    val nodes = index(words)
    connect(nodes)
  }

  private def index(words: Iterable[String])(implicit neo: DatabaseService) = {
    val index = getNodeIndex("WordsIndex").get
    val nodes = words.map(w => createNode(Word(w))).toIterable

    println("indexing...")
    for (n <- nodes)
      index += (n, "text", n.toCC[Word].get.text)

    nodes.zip(words)
  }

  private def connect(nodes: Iterable[(Node, String)])(implicit neo: DatabaseService) {
    println("connecting...")
    val first = (nodes.unzip)._2.zipWithIndex

    first.par.foreach { e =>
      first.foreach { f =>
        val dist = DistanceMeasures.levenshtein3(e._1, f._1)
        if (dist < 3) {
          println(e._2,  f._2)
        }
      }
    }
  }
}
