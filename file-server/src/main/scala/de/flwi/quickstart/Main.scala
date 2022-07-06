package de.flwi.quickstart

import cats.data.Kleisli
import cats.effect._
import com.comcast.ip4s._
import fs2.io.file
import fs2.io.file.{Files, Path}
import io.circe._
import io.circe.syntax._
import org.http4s.{HttpRoutes, Request, Response, StaticFile}
import org.http4s.dsl.Http4sDsl
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import org.http4s.server.{Router, Server}
import org.http4s.server.staticcontent._
import org.http4s.circe._

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    app.use(_ => IO.never).as(ExitCode.Success)

  val dsl: Http4sDsl[IO] = new Http4sDsl[IO] {}
  import dsl._

  val apiService: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "lessons" =>
      Ok(Listing.allLessons.map(_.asJson))
  }

  def static(file: String, request: Request[IO]): IO[Response[IO]] =
    StaticFile
      .fromPath(fs2.io.file.Path(file), Some(request))
      .getOrElseF(NotFound())

  val httpApp: Kleisli[IO, Request[IO], Response[IO]] =
    Router(
      "api" -> apiService,
      "assets" -> fileService[IO](FileService.Config("./assets")),
      "/" -> fileService[IO](FileService.Config("./webapp"))
    ).orNotFound

  val app: Resource[IO, Server] =
    EmberServerBuilder
      .default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8080")
      .withHttpApp(httpApp)
      .build
}

object Listing {
  case class Lesson(folder: String, video: String, subtitle: String)

  object Lesson {
    implicit val decoder: Decoder[Lesson] =
      io.circe.generic.semiauto.deriveDecoder
    implicit val encoder: Encoder[Lesson] =
      io.circe.generic.semiauto.deriveEncoder
  }

  def allLessons: IO[List[Lesson]] = {
    Files[IO]
      .walk(file.Path("assets"), maxDepth = Int.MaxValue, followLinks = true)
      .filter(f => f.extName == ".vtt" | f.extName == ".mp4")
      .compile
      .toList
      .map(listing)
  }

  def listing(allFiles: List[Path]): List[Lesson] = {

    val byFolder: Map[Path, List[Path]] = allFiles.groupBy(_.parent.get)
    byFolder
      .map { case (folder, files) =>
        val subtitle = files.find(_.extName == ".vtt").get
        val video = files.find(_.extName == ".mp4").get
        Lesson(folder.toString, video.toString, subtitle.toString)
      }
      .toList
      .sortBy(_.folder)
  }

}
