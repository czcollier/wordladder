import sbt._
import Keys._
import cc.spray.revolver.RevolverPlugin._

object Build extends sbt.Build {
  import Dependencies._

  lazy val myProject = Project("wordladder", file("."))
    .settings(Revolver.settings: _*)
    .settings(
      organization  := "net.xorf",
      version       := "0.9.0",
      scalaVersion  := "2.9.1",
      scalacOptions := Seq("-deprecation", "-encoding", "utf8"),
      javaOptions   := Seq("-Xdebug", "-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"),
      resolvers     ++= Dependencies.resolutionRepos,
      libraryDependencies ++= Seq(
        Compile.akkaActor,
        //Compile.sprayCan,
        //Compile.sprayServer,
        //Compile.casbah,
        //Compile.liftMongo,
        //Compile.liftMongoRecord,
        //Compile.liftJson,
        //Compile.dataService,
        //Compile.rogue,
        //Compile.salat,
        Compile.jodaTime,
        Compile.neo4jKernel,
        Compile.neo4jLucene,
        Compile.neo4jShell,
        Compile.neo4jRest,
        Compile.neo4jScala,

        Test.specs2,

        Container.akkaSlf4j,
        Container.slf4j,
        Container.logback
      )
    )
}

object Dependencies {
  //val bhStdRepo = "/home/ccollier/devel/bullhorn/skunkworks/bh-test-ivy-repo"

  val resolutionRepos = Seq(
    "Typesafe repo"           at "http://repo.typesafe.com/typesafe/releases/",
    "Scala Tools Releases"    at "https://oss.sonatype.org/content/groups/scala-tools/",
    "Scala Tools Snapshots"   at "https://oss.sonatype.org/content/repositories/snapshots/",
    "spray repo"              at "http://repo.spray.cc/",
    "Novus Releases"          at "http://repo.novus.com/releases",
    "neo4j-public-repository" at "http://m2.neo4j.org/content/repositories/releases",
    "tinkerprop"              at "http://tinkerpop.com/maven2",
    "Fakod Releases"          at "https://raw.github.com/FaKod/fakod-mvn-repo/master/releases",
    "Fakod Snapshots"         at "https://raw.github.com/FaKod/fakod-mvn-repo/master/snapshots"
    //Resolver.file("bhpub",
    //  file(bhStdRepo)) (Patterns(
    //    Seq("ivy/ivy-[module]-[revision].[ext]"), Seq("[artifact]-[revision].[ext]"), isMavenCompatible = false))

  )

  object Versions {
    val akka        = "2.0.5"
    val spray       = "0.9.0"
    val sprayCan    = "0.9.3"
    val casbah      = "2.4.1"
    val specs2      = "1.7.1"
    val slf4j       = "1.6.4"
    val logback     = "1.0.0"
    val lift        = "2.4"
    val rogue       = "1.1.8"
    val salat       = "1.9.1"
    val jodaTime    = "1.6"
    val squeryl     = "0.9.5-2"
    val dispatch    = "0.9.2"
    val dataService = "1.0.1-SNAPSHOT"
    val neo4jScala  = "0.2.0-M2-SNAPSHOT"
    val neo4j       = "1.8"

  }

  object Compile {
    val scopeName = "compile"

    val akkaActor       = "com.typesafe.akka"         %  "akka-actor"          % Versions.akka        % "compile"
    val sprayCan        = "cc.spray"                  %  "spray-can"           % Versions.sprayCan    % "compile"
    val sprayServer     = "cc.spray"                  %  "spray-server"        % Versions.spray       % "compile"
    val casbah          = "org.mongodb"               %% "casbah"              % Versions.casbah      % "compile"
    val liftMongo       = "net.liftweb"               %% "lift-mongodb"        % Versions.lift        % "compile"
    val liftMongoRecord = "net.liftweb"               %% "lift-mongodb-record" % Versions.lift        % "compile"
    val liftJson        = "net.liftweb"               %% "lift-json"           % Versions.lift        % "compile"
    val rogue           = "com.foursquare"            %% "rogue"               % Versions.rogue       % "compile" intransitive()
    val salat           = "com.novus"                 %% "salat"               % Versions.salat       % "compile"
    val jodaTime        = "joda-time"                 %  "joda-time"           % Versions.jodaTime    % "compile"
    val squeryl         = "org.squeryl"               %% "squeryl"             % Versions.squeryl     % "compile"
    val dispatch        = "net.databinder.dispatch"   %% "dispatch-core"       % Versions.dispatch    % "compile"
    val dataService     = "com.bullhorn"              %  "data-service"        % Versions.dataService % "compile"
    val neo4jKernel     = "org.neo4j"                 % "neo4j-kernel"         % Versions.neo4j       % "compile"
    val neo4jLucene     = "org.neo4j"                 % "neo4j-lucene-index"   % Versions.neo4j       % "compile"
    val neo4jShell      = "org.neo4j"                 % "neo4j-shell"          % Versions.neo4j       % "compile"
    val neo4jRest       = "org.neo4j"                 % "neo4j-rest-graphdb"   % Versions.neo4j       % "compile"
    val neo4jScala      = "org.neo4j"                 % "neo4j-scala"          % Versions.neo4jScala  % "compile"
  }

  object Test {
    val specs2      = "org.specs2"                %% "specs2"          % Versions.specs2  % "test"
  }

  object Container {
    val akkaSlf4j   = "com.typesafe.akka"         %  "akka-slf4j"      % Versions.akka
    val slf4j       = "org.slf4j"                 %  "slf4j-api"       % Versions.slf4j
    val logback     = "ch.qos.logback"            %  "logback-classic" % Versions.logback
  }
}
