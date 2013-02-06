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

  def seed(dataFile: String) {
    withTx { implicit neo =>
      createAndIndex(dataFile)
    }
  }

  def connect() {
    withTx { implicit neo =>
      calcDists()
    }
  }


  def count = {
    withTx { implicit neo =>
      val itr = GlobalGraphOperations.at(neo.gds).getAllNodes.iterator
      var cnt = 0
      for (i <- itr) cnt += 1
      cnt
    }
  }

  private def getNode(search: String)(implicit idx: Index[Node]) = {
    val n = Option(idx.get("text", search).getSingle)
    n match {
      case Some(v) => Right(v)
      case None => Left(FindError("could not find word: %s".format(search)))
    }
  }

  private def createAndIndex(dataFile: String)(implicit neo: DatabaseService) {
    println("building nodes")
    val words = scala.io.Source.fromFile(dataFile).getLines().toIterable
    index(words)
  }

  private def index(words: Iterable[String])(implicit neo: DatabaseService) = {
    val index = getNodeIndex("WordsIndex").get
    val nodes = words.map(w => createNode(Word(w))).toIterable

    println("indexing...")
    for (n <- nodes)
      index += (n, "text", n.toCC[Word].get.text)

    nodes.zip(words)
  }

  private def extract(n: Node) = n.toCC[Word].get.text

  private def calcDists()(implicit neo: DatabaseService) {
    val nodes = GlobalGraphOperations.at(neo.gds).getAllNodes.filter(x => !x.toCC[Word].isEmpty)

    nodes.toSeq.combinations(2).toSeq.par.foreach { e =>
      val left = extract(e(0))
      val right = extract(e(1))

      val dist = DistanceMeasures.levenshtein1(left, right)
      if (dist < 3) {
        println("%s <- %d -> %s".format(left, dist, right))
        e(0) --> "LINKS" --> e(1)
      }
    }
  }
}
