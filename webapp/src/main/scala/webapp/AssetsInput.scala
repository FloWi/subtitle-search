package webapp

import cats.effect.IO
import org.scalajs.dom
import sttp.capabilities
import sttp.client3.{SttpBackend, UriContext}
import sttp.client3.impl.cats.FetchCatsBackend
import sttp.model.Uri

object AssetsInput {

  case class Lesson(folderUri: String, videoUri: String, subtitleUri: String)

  val isLocalhost: Boolean = dom.window.location.host == "localhost" || dom.window.location.host
    .startsWith("localhost")

  private val assetLocation =
    if (isLocalhost) uri"http://localhost:8080/assets/"
    else uri"assets/"

  private val apiLocation =
    if (isLocalhost) uri"http://localhost:8080/api/"
    else uri"/api/"

  val fetchBackend: SttpBackend[IO, capabilities.WebSockets] = FetchCatsBackend[IO]()

  def getAssetUri(relativeUri: Uri): Uri =
    assetLocation.resolve(relativeUri)

  def lessons: IO[List[Lesson]] = {
    import io.circe.generic.auto._
    import sttp.client3._
    import sttp.client3.circe._

    val lessonsUri = apiLocation.addPath("lessons")
    println(lessonsUri)
    val request    = basicRequest
      .get(lessonsUri)
      .response(asJson[List[Lesson]])

    request
      .send(fetchBackend)
      .to[IO]
      .flatMap(resp => IO.fromEither(resp.body))
  }

  def loadSubtitle(fileEntry: Lesson): IO[String] = {
    import sttp.client3._

    val uri     = getAssetUri(uri"${fileEntry.subtitleUri}")
    println(uri)
    val request = basicRequest
      .get(
        uri,
      )

    request
      .send(fetchBackend)
      .to[IO]
      .flatMap(resp => IO.fromOption(resp.body.toOption)(new RuntimeException("broken")))

  }
}
