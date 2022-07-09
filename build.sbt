Global / onChangedBuildSource := IgnoreSourceChanges // not working well with webpack devserver

name                     := "Subtitlesearch"
ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.8"

val versions = new {
  val outwatch           = "1.0.0-RC7"
  val funPack            = "0.2.0"
  val scalaTest          = "3.2.12"
  val circeVersion       = "0.14.1"
  val sttpClient3Version = "3.6.2"
  val catsEffectVersion  = "3.3.12"
  val srtVttParser       = "1.1.0"

  val Http4sVersion          = "0.23.13"
  val CirceVersion           = "0.14.2"
  val MunitVersion           = "0.7.29"
  val LogbackVersion         = "1.2.10"
  val MunitCatsEffectVersion = "1.0.7"
  val PureconfigVersion      = "0.17.1"

}

lazy val scalaJsMacrotaskExecutor = Seq(
  // https://github.com/scala-js/scala-js-macrotask-executor
  libraryDependencies       += "org.scala-js" %%% "scala-js-macrotask-executor" % "1.0.0",
  Compile / npmDependencies += "setimmediate"  -> "1.0.5", // polyfill
)

lazy val fileserver = (project in file("file-server"))
  .settings(
    organization         := "de.flwi",
    name                 := "quickstart",
    version              := "0.0.1-SNAPSHOT",
    scalaVersion         := "2.13.8",
    libraryDependencies ++= Seq(
      "org.http4s"            %% "http4s-ember-server"    % versions.Http4sVersion,
      "org.http4s"            %% "http4s-ember-client"    % versions.Http4sVersion,
      "org.http4s"            %% "http4s-circe"           % versions.Http4sVersion,
      "org.http4s"            %% "http4s-dsl"             % versions.Http4sVersion,
      "io.circe"              %% "circe-generic"          % versions.CirceVersion,
      "com.github.pureconfig" %% "pureconfig-generic"     % versions.PureconfigVersion,
      "com.github.pureconfig" %% "pureconfig-cats-effect" % versions.PureconfigVersion,
      "org.scalameta"         %% "munit"                  % versions.MunitVersion           % Test,
      "org.typelevel"         %% "munit-cats-effect-3"    % versions.MunitCatsEffectVersion % Test,
      "ch.qos.logback"         % "logback-classic"        % versions.LogbackVersion         % Runtime,
    ),
    addCompilerPlugin(
      "org.typelevel"              %% "kind-projector"     % "0.13.2" cross CrossVersion.full,
    ),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
    testFrameworks       += new TestFramework("munit.Framework"),
    fork                 := true,
  )

lazy val webapp = (project in file("webapp"))
  .enablePlugins(
    ScalaJSPlugin,
    ScalaJSBundlerPlugin,
    // ScalablyTypedConverterPlugin,
  )
  .settings(scalaJsMacrotaskExecutor)
  .settings(
    libraryDependencies          ++= Seq(
      "io.github.outwatch"            %%% "outwatch"      % versions.outwatch,
      "org.scalatest"                 %%% "scalatest"     % versions.scalaTest % Test,
      "io.circe"                      %%% "circe-generic" % versions.circeVersion,
      "io.circe"                      %%% "circe-parser"  % versions.circeVersion,
      "com.softwaremill.sttp.client3" %%% "circe"         % versions.sttpClient3Version,
      "com.softwaremill.sttp.client3" %%% "cats"          % versions.sttpClient3Version,
    ),
    Compile / npmDevDependencies ++= Seq(
      "@fun-stack/fun-pack"     -> versions.funPack, // sane defaults for webpack development and production, see webpack.config.*.js
      "@plussub/srt-vtt-parser" -> versions.srtVttParser,
    ),
    scalacOptions --= Seq(
      "-Xfatal-warnings",
    ), // overwrite option from https://github.com/DavidGregory084/sbt-tpolecat

    useYarn := true, // Makes scalajs-bundler use yarn instead of npm
    scalaJSLinkerConfig ~= (_.withModuleKind(
      ModuleKind.CommonJSModule,
    )), // configure Scala.js to emit a JavaScript module instead of a top-level script
    scalaJSUseMainModuleInitializer   := true, // On Startup, call the main function
    webpackDevServerPort              := 12345,
    webpack / version                 := "4.46.0",
    startWebpackDevServer / version   := "3.11.3",
    webpackDevServerExtraArgs         := Seq("--color"),
    fullOptJS / webpackEmitSourceMaps := true,
    fastOptJS / webpackBundlingMode   := BundlingMode
      .LibraryOnly(), // https://scalacenter.github.io/scalajs-bundler/cookbook.html#performance
    fastOptJS / webpackConfigFile := Some(baseDirectory.value / "webpack.config.dev.js"),
    fullOptJS / webpackConfigFile := Some(baseDirectory.value / "webpack.config.prod.js"),
    // stIgnore                     ++= List("@plussub/srt-vtt-parser", "setimmediate"),
    Test / requireJsDomEnv        := true,
  )

addCommandAlias("prod", "; prod-webapp; prod-fileserver")
addCommandAlias("prod-webapp", "webapp/fullOptJS/webpack")
addCommandAlias("prod-fileserver", "fileserver/assembly")
addCommandAlias("dev-fileserver", "~; fileserver/reStart")
addCommandAlias("dev", "devInit; devWatchAll; devDestroy")
addCommandAlias("devInit", "; webapp/fastOptJS/startWebpackDevServer")
addCommandAlias("devWatchAll", "~; webapp/fastOptJS/webpack")
addCommandAlias("devDestroy", "webapp/fastOptJS/stopWebpackDevServer")
