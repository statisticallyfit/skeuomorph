ThisBuild / libraryDependencySchemes ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
)
// Added by statisticallyfit: HELP why not working when here?
//addSbtPlugin("com.codecommit" % "sbt-github-packages" % "0.3.1")
//addSbtPlugin("nl.gn0s1s" % "sbt-dotenv" % "3.0.0")
//addSbtPlugin("io.get-coursier" % "sbt-coursier" % "2.0.8")

addSbtPlugin("org.scoverage"             % "sbt-scoverage"     % "2.0.7")
addSbtPlugin("com.github.sbt"            % "sbt-ci-release"    % "1.5.12")
addSbtPlugin("com.47deg"                 % "sbt-microsites"    % "1.4.3")
addSbtPlugin("org.scalameta"             % "sbt-scalafmt"      % "2.5.0")
addSbtPlugin("org.scalameta"             % "sbt-mdoc"          % "2.3.7")
addSbtPlugin("de.heikoseeberger"         % "sbt-header"        % "5.9.0")
addSbtPlugin("com.alejandrohdezma"       % "sbt-codecov"       % "0.2.1")
addSbtPlugin("com.alejandrohdezma"       % "sbt-github"        % "0.11.11")
addSbtPlugin("com.alejandrohdezma"       % "sbt-github-header" % "0.11.11")
addSbtPlugin("com.alejandrohdezma"       % "sbt-github-mdoc"   % "0.11.11")
addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat"      % "0.4.2")



// SOURCE OF BELOW CODE = https://gist.github.com/paulp/e8df663712fe5455b433f5dd71c043c6#file-plugins-sbt

lazy val isEnabled: Set[String] = sys.props.getOrElse("plugins", "").split(",").map(_.trim).toSet

def maybeEnable(pair: (String, ModuleID)): Seq[Setting[_]] =
  if (isEnabled(pair._1)) addSbtPlugin(pair._2) else Seq()

Seq[(String, ModuleID)](
  "COURSIER" -> "io.get-coursier" % "sbt-coursier" % "2.0.8",
  "GITHUBPACKAGES" -> "com.codecommit" % "sbt-github-packages" % "0.5.2",
  "DOTENV" -> "nl.gn0s1s" % "sbt-dotenv" % "3.0.0",
  "BUILDINFO" -> "com.eed3si9n" % "sbt-buildinfo" % "0.11.0",
  "SBTGIT" -> "com.github.sbt" % "sbt-git" % "2.0.1"
).flatMap(maybeEnable)