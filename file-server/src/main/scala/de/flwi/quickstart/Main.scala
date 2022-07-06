package de.flwi.quickstart

import cats.data.Kleisli
import cats.effect._
import com.comcast.ip4s._
import fs2.io.file
import fs2.io.file.{Files, Path => Fs2Path}
import io.circe._
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import org.http4s.server.staticcontent._
import org.http4s.server.{Router, Server}
import org.http4s.{HttpRoutes, Request, Response, StaticFile}
import org.typelevel.log4cats.slf4j.Slf4jFactory
import org.typelevel.log4cats.{LoggerFactory, SelfAwareStructuredLogger}
import pureconfig.ConfigSource
import pureconfig.module.catseffect.syntax.CatsEffectConfigSource

object Main extends IOApp {
  implicit val logging: LoggerFactory[IO] = Slf4jFactory[IO]

  // we summon LoggerFactory instance, and create logger
  val logger: SelfAwareStructuredLogger[IO] = LoggerFactory[IO].getLogger

  override def run(args: List[String]): IO[ExitCode] = for {
    conf <- Config.load
    _    <- validateEnvironment(conf)
    _    <- logger.info(s"starting server with this config")
    _    <- logger.info(s"\n$conf")
    res  <- app(conf).use(_ => IO.never).as(ExitCode.Success)
  } yield res

  val dsl: Http4sDsl[IO] = new Http4sDsl[IO] {}
  import dsl._

  def validateEnvironment(config: Config): IO[Unit] =
    validatePathExists(Fs2Path(config.webAppFolder)) *> validatePathExists(Fs2Path(config.assetsFolder))

  def validatePathExists(path: fs2.io.file.Path): IO[Unit] =
    Files[IO].exists(path).flatMap {
      case false => IO.raiseError(new IllegalArgumentException(s"$path doesn't exist"))
      case true  => IO.unit
    }

  def apiService(implicit config: Config): HttpRoutes[IO] = HttpRoutes.of[IO] { case GET -> Root / "lessons" =>
    Ok(Listing.allLessons(config).map(_.asJson))
  }

  def static(file: String, request: Request[IO]): IO[Response[IO]] =
    StaticFile
      .fromPath(fs2.io.file.Path(file), Some(request))
      .getOrElseF(NotFound())

  def httpApp(implicit config: Config): Kleisli[IO, Request[IO], Response[IO]] =
    Router(
      "api"    -> apiService,
      "assets" -> fileService[IO](FileService.Config(config.assetsFolder)),
      "/"      -> fileService[IO](FileService.Config(config.webAppFolder)),
    ).orNotFound

  def app(implicit config: Config): Resource[IO, Server] =
    EmberServerBuilder
      .default[IO]
      .withHost(Ipv4Address.fromString(config.host).get)
      .withPort(Port.fromInt(config.port).get)
      .withHttpApp(httpApp)
      .build
}

object Listing {
  case class Lesson(folderUri: String, videoUri: String, subtitleUri: String)

  object Lesson {
    implicit val decoder: Decoder[Lesson] =
      io.circe.generic.semiauto.deriveDecoder
    implicit val encoder: Encoder[Lesson] =
      io.circe.generic.semiauto.deriveEncoder
  }

  def allLessons(config: Config): IO[List[Lesson]] =
    Files[IO]
      .walk(
        file.Path(config.assetsFolder),
        maxDepth = Int.MaxValue,
        followLinks = true,
      )
      .filter(f => f.extName == ".vtt" | f.extName == ".mp4")
      .compile
      .toList
      .map(listing)

  def listing(allFiles: List[Fs2Path]): List[Lesson] = {

    val byFolder: Map[Fs2Path, List[Fs2Path]] = allFiles.groupBy(_.parent.get)
    byFolder.map { case (folder, files) =>
      val subtitle = files.find(_.extName == ".vtt").get
      val video    = files.find(_.extName == ".mp4").get
      Lesson(folder.toString, video.toString, subtitle.toString)
    }.toList
      .sortBy(_.folderUri)
  }
}

case class Config(
  port: Int,
  host: String,
  webAppFolder: String,
  assetsFolder: String,
) {
  override def toString: String =
    s"""        host: $host
       |        port: $port
       |webAppFolder: $webAppFolder
       |assetsFolder: $assetsFolder
       |""".stripMargin
}

object Config {
  import pureconfig.generic.auto._

  def load: IO[Config] =
    ConfigSource.defaultApplication.loadF[IO, Config]()
}
