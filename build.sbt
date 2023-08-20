ThisBuild / organization       := "io.higherkindness"
ThisBuild / githubOrganization := "47degrees"
ThisBuild / scalaVersion       := "2.13.10"
ThisBuild / crossScalaVersions := Seq("2.12.17", "2.13.10")
ThisBuild / name := "skeuomorph"
//ThisBuild / useCoursier := false
//version := "0.2.1-SNAPSHOT" // TODO let version from coursier pick up?


//publishTo := Some(s"GitHub $githubOwner Apache Maven Packages" at s"https://maven.pkg.github.com/$githubOwner/$githubRepository")

/*
import scala.util.control.NonFatal

lazy val defaultScmInfo: Option[ScmInfo] = {
  import scala.sys.process._

  val identifier  = """([^\/]+?)"""
  val GitHubHttps = s"https://github.com/$identifier/$identifier(?:\\.git)?".r
  val GitHubGit   = s"git://github.com:$identifier/$identifier(?:\\.git)?".r
  val GitHubSsh   = s"git@github.com:$identifier/$identifier(?:\\.git)?".r

  val gitHubScmInfo = (user: String, repo: String) =>
    ScmInfo(
      url(s"https://github.com/$user/$repo"),
      s"scm:git:https://github.com/$user/$repo.git",
      Some(s"scm:git:git@github.com:$user/$repo.git")
    )

  try {
    val remote = List("git", "ls-remote", "--get-url", "origin").!!.trim()
    remote match {
      case GitHubHttps(user, repo) => Some(gitHubScmInfo(user, repo))
      case GitHubGit(user, repo)   => Some(gitHubScmInfo(user, repo))
      case GitHubSsh(user, repo)   => Some(gitHubScmInfo(user, repo))
      case _                       => None
    }
  } catch {
    case NonFatal(_) => None
  }
}*/



// Sources:
// https://stackoverflow.com/questions/66228218/intellij-doesnt-recognize-code-in-build-sbt-and-doesnt-compile
// https://medium.com/@supermanue/how-to-publish-a-scala-library-in-github-bfb0fa39c1e4

//ThisBuild / githubEnabled   := true



// TODO HELP - why getting scm error when uncommenting all this below?
//ThisBuild / gitdomain := "github.com"
//ThisBuild / repo := "statisticallyfit/skeuomorph"


// source of this tokensource declaration = https://stackoverflow.com/questions/66228218/intellij-doesnt-recognize-code-in-build-sbt-and-doesnt-compile
/*ThisBuild /  githubAuthToken := sys.env.get("GITHUB_TOKEN").map(AuthToken) //TokenSource.Environment("GITHUB_TOKEN")*/


/*ThisBuild / githubOwner := "statisticallyfit"
//ThisBuild / githubRepository := "skeuomorph" // TODO why when includ this it gives scm info connection error???

// source of this tokensource declaration = https://stackoverflow.com/questions/66228218/intellij-doesnt-recognize-code-in-build-sbt-and-doesnt-compile
ThisBuild / githubTokenSource := TokenSource.Environment("GITHUB_TOKEN")

ThisBuild / scmInfo := Some(
  ScmInfo(url(s"https://github.com/statisticallyfit/skeuomorph"),
    s"scm:git:https://github.com/statisticallyfit/skeuomorph.git",
    Some(s"scm:git:git@github.com:statisticallyfit/skeuomorph.git"))
)*/

/*ThisBuild / scmInfo := Some(
  ScmInfo(url("http://github.com"), "scm:git@github.com:statisticallyfit/skeuomorph.git")
)

publishMavenStyle := true
credentials += Credentials(
  "GitHub Package Registry",
  "maven.pkg.github.com",
  s"$githubOwner",
  System.getenv("GITHUB_TOKEN")
)*/
//publishMavenStyle := true
////githubOwner := "statisticallyfit"
////githubRepository := "skeuomorph"
////githubTokenSource := TokenSource.GitConfig("github.token")  || TokenSource.Environment("GITHUB_TOKEN")
////githubTokenSource := TokenSource.GitConfig("github.token")  || TokenSource.Environment("GITHUB_TOKEN")
//credentials +=
//     Credentials(
//       "GitHub Package Registry",
//       "https://maven.pkg.github.com",
//       "statisticallyfit", //githubOwner,
//       sys.env.getOrElse("GITHUB_TOKEN", "N/A")
//     )





// NOTE: using the $COURSIER_CACHE location from .bash_profile
// Source = https://alvinalexander.com/scala/sbt-how-publish-library-ivy-repository/
publishTo := Some(Resolver.file("file", new File("/development/tmp/.coursier")))





addCommandAlias("ci-test", "scalafmtCheckAll; scalafmtSbtCheck; microsite/mdoc; +test")
addCommandAlias("ci-docs", "github; documentation/mdoc; headerCreateAll; microsite/publishMicrosite")
addCommandAlias("ci-publish", "github; ci-release")




//---------------------------------------------------------------------------------------------------------
// Added by statisticallyfit
// Reason: for sbt plugins - to make skeuomorph be imported into my project via github


enablePlugins(BuildInfoPlugin)
//enablePlugins(SbtCoursierPlugin)
enablePlugins(GitPlugin)
enablePlugins(SbtDotenv)
enablePlugins(GitHubPackagesPlugin)


lazy val skeuomorph = project
     .in(file("."))
     .settings(commonSettings)
     .settings(moduleName := "skeuomorph")
     .enablePlugins(BuildInfoPlugin) // TODO how to know what is the name of my declared plugins in the plugins.sbt file?
     .enablePlugins(GitPlugin)
     .enablePlugins(SbtDotenv)
     .enablePlugins(GitHubPackagesPlugin)


// Source
// https://stackoverflow.com/a/67908451
// https://xebia.com/blog/git-subproject-compile-time-dependencies-in-sbt/
// https://stackoverflow.com/questions/42206668/scala-sbt-file-dependency-from-github-repository
//https://stackoverflow.com/questions/20136075/using-git-local-repository-as-dependency-in-sbt-project
//https://stackoverflow.com/questions/67861343/sbt-how-to-ensure-that-local-snapshot-dependency-is-picked-up
//lazy val skeuomorphExtendedInLocalCoursier = ProjectRef(file("/development/tmp/.coursier"), "skeuomorph_2.13-0.0.0+1149-7164525f+20230818-1637-SNAPSHOT")
//lazy val skeuomorphExtendedInGit = ProjectRef(uri("https://github.com/statisticallyfit/skeuomorph.git#master"), "skeuomorph")
//lazy val skeuomorph = Project("skeuomorph", file("."))
//     .enablePlugins(BuildInfoPlugin) // TODO how to know what is the name of my declared plugins in the plugins.sbt file?
//     .enablePlugins(SbtDotenv)
//     .enablePlugins(GitHubPackagesPlugin)
//////.dependsOn(skeuomorphExtendedInGit)


// ---------------------------------------------------------------------------------------------------------


lazy val microsite = project
  .dependsOn(skeuomorph)
  .settings(commonSettings)
  .settings(publish / skip := true)
  .settings(mdocSettings)
  .settings(
    micrositeName             := "Skeuomorph",
    micrositeDescription      := "Skeuomorph is a library for transforming different schemas in Scala",
    micrositeBaseUrl          := "/skeuomorph",
    micrositeHighlightTheme   := "tomorrow",
    micrositeDocumentationUrl := "docs",
    Jekyll / includeFilter    := "*.html" | "*.css" | "*.png" | "*.jpg" | "*.gif" | "*.js" | "*.swf" | "*.md" | "*.svg",
    micrositeGithubToken      := Option(System.getenv().get("GITHUB_TOKEN")),
    micrositePushSiteWith     := GitHub4s,
    micrositeFavicons         := Seq(microsites.MicrositeFavicon("favicon.png", "32x32")),
    micrositePalette := Map(
      "brand-primary"   -> "#4A00D8",
      "brand-secondary" -> "#FC00CD",
      "white-color"     -> "#FFF"
    ),
    micrositeExtraMdFiles := Map(
      file("CHANGELOG.md") -> microsites.ExtraMdFileConfig(
        "changelog.md",
        "docs",
        Map("title" -> "changelog", "permalink" -> "docs/changelog/")
      )
    )
  )
  .enablePlugins(MicrositesPlugin)

lazy val documentation = project
  .settings(mdocOut := file("."))
  .settings(publish / skip := true)
  .enablePlugins(MdocPlugin)

lazy val filterConsoleScalacOptions = { options: Seq[String] =>
  options.filterNot(
    Set(
      "-Werror",
      "-Wdead-code",
      "-Wunused:imports",
      "-Ywarn-unused",
      "-Ywarn-unused:imports",
      "-Ywarn-unused-import",
      "-Ywarn-dead-code",
      "-Xfatal-warnings"
    )
  )
}

// General Settings
lazy val commonSettings = Seq(
  scalacOptions ~= (_ filterNot Set("-Xfuture", "-Xfatal-warnings").contains),
  libraryDependencies ++= Seq(
    "org.typelevel"        %% "cats-core"         % "2.9.0",
    "org.typelevel"        %% "cats-effect"       % "3.5.0",
    "io.higherkindness"    %% "droste-core"       % "0.9.0",
    "io.higherkindness"    %% "droste-macros"     % "0.9.0",
    "org.apache.avro"       % "avro"              % "1.11.1",
    "com.github.os72"       % "protoc-jar"        % "3.11.4",
    "com.google.protobuf"   % "protobuf-java"     % "3.23.2",
    "io.circe"             %% "circe-core"        % "0.14.5",
    "io.circe"             %% "circe-parser"      % "0.14.5",
    "io.circe"             %% "circe-yaml"        % "0.14.2",
    "com.julianpeeters"    %% "avrohugger-core"   % "1.4.0"        % Test,
    "org.typelevel"        %% "cats-laws"         % "2.9.0"        % Test,
    "io.circe"             %% "circe-testing"     % "0.14.5"       % Test,
    "org.typelevel"        %% "discipline-specs2" % "1.4.0"        % Test,
    "org.specs2"           %% "specs2-core"       % "4.12.4-js-ec" % Test,
    "org.specs2"           %% "specs2-scalacheck" % "4.12.4-js-ec" % Test,
    "org.scalacheck"       %% "scalacheck"        % "1.17.0"       % Test,
    "io.chrisdavenport"    %% "cats-scalacheck"   % "0.3.2"        % Test,
    "org.scalatra.scalate" %% "scalate-core"      % "1.9.8"        % Test,


/* // Still doesn't work! - why cannot import them?
     "io.get-coursier" % "sbt-coursier" % "2.0.8",
     "com.codecommit" % "sbt-github-packages" % "0.3.1",
     "nl.gn0s1s" % "sbt-dotenv" % "2.1.233",
     "com.eed3si9n" % "sbt-buildinfo" % "0.11.0"
*/

/*
         addSbtPlugin ("io.get-coursier" % "sbt-coursier" % "2.0.8")
         addSbtPlugin ("com.codecommit" % "sbt-github-packages" % "0.3.1")
         addSbtPlugin ("nl.gn0s1s" % "sbt-dotenv" % "2.1.233")
         addSbtPlugin ("com.eed3si9n" % "sbt-buildinfo" % "0.11.0")
*/

  ),
  resolvers ++= (/*Seq(Resolver.githubPackages("statisticallyfit"))
       ++ */Resolver.sonatypeOssRepos("releases")
       //++ Seq(Resolver.mavenLocal)
       ++ Resolver.sonatypeOssRepos("snapshots")
       ++ Seq("jitpack" at "https://jitpack.io") // jitpack for opetushallitus
       //++ Seq("Local Coursier Repository" at ("file://" + "/development/tmp/.coursier"))
       //++ Seq("Local Ivy Repository" at ("file://" + Path.userHome.absolutePath + "/.ivy2/local"))
       //ThisBuild / useCoursier := false)
       )
) ++ compilerPlugins ++ macroSettings ++ Seq(
  libraryDependencies ++= (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, _)) =>
      Seq(
        "org.scalameta" %% "scalameta" % "4.7.8"
      )
    case _ => Seq.empty
  })
)

lazy val mdocSettings = Seq(
  scalacOptions ~= filterConsoleScalacOptions,
  scalacOptions ~= (_ filterNot Set("-Xfatal-warnings", "-Ywarn-unused-import", "-Xlint").contains)
)

lazy val compilerPlugins = Seq(
  libraryDependencies ++= (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, _)) =>
      Seq(
        compilerPlugin("org.typelevel" % "kind-projector"     % "0.13.2" cross CrossVersion.full),
        compilerPlugin("com.olegpy"   %% "better-monadic-for" % "0.3.1")
      )
    case _ => Seq.empty
  })
)

lazy val macroSettings: Seq[Setting[_]] = Seq(
  libraryDependencies ++= Seq(
    scalaOrganization.value % "scala-compiler" % scalaVersion.value % Provided
  ),
  libraryDependencies ++= on(2, 12)(
    compilerPlugin("org.scalamacros" %% "paradise" % "2.1.1" cross CrossVersion.full)
  ).value,
  scalacOptions ++= on(2, 13)("-Ymacro-annotations").value
)

def on[A](major: Int, minor: Int)(a: A): Def.Initialize[Seq[A]] =
  Def.setting {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some(v) if v == (major, minor) => Seq(a)
      case _                              => Nil
    }
  }




// -------------
// Added by statisticallyfit
/*

import scala.concurrent.duration.DurationInt
import lmcoursier.definitions.CachePolicy


csrConfiguration := csrConfiguration.value
     .withTtl(Some(0.seconds))
     .withCachePolicies(Vector(CachePolicy.LocalOnly))
*/
