package webapp

import cats.effect.IO
import org.scalajs.dom
import sttp.capabilities
import sttp.client3.SttpBackend
import sttp.client3.impl.cats.FetchCatsBackend
import sttp.model.Uri

object AssetsInput {

  case class Lesson(folderUri: String, videoUri: String, subtitleUri: String)

  private val assetLocation = s"${dom.window.location.origin}/${if (
    dom.window.location.host == "localhost" || dom.window.location.host
      .startsWith("localhost")
  ) ""
  else "assets/"}"

  val fetchBackend: SttpBackend[IO, capabilities.WebSockets] = FetchCatsBackend[IO]()

  def getAssetUri(relativeUri: Uri): Uri =
    Uri.parse(assetLocation).toOption.get.resolve(relativeUri)

  def lessons: IO[List[Lesson]] = {
    import io.circe.generic.auto._
    import sttp.client3._
    import sttp.client3.circe._

    val request = basicRequest
      .get(uri"/api/lessons")
      .response(asJson[List[Lesson]])

    request
      .send(fetchBackend)
      .to[IO]
      .flatMap(resp => IO.fromEither(resp.body))
  }

  def loadSubtitle(fileEntry: Lesson): IO[String] = {
    import sttp.client3._

    val request = basicRequest
      .get(
        getAssetUri(uri"${fileEntry.subtitleUri}"),
      )

    request
      .send(fetchBackend)
      .to[IO]
      .flatMap(resp => IO.fromOption(resp.body.toOption)(new RuntimeException("broken")))

  }
}
