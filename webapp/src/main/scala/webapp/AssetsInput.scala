package webapp

import cats.effect.IO
import org.scalajs.dom
import sttp.capabilities
import sttp.client3.SttpBackend
import sttp.client3.impl.cats.FetchCatsBackend
import sttp.model.Uri

object AssetsInput {

  case class FileOrDirectoryEntry(
    name: String,
    size: Int,
    `type`: String,
    contents: Option[List[FileOrDirectoryEntry]],
  )

  case class FileEntry(
    path: String,
    name: String,
    size: Int,
  )

  def flatten(fileEntry: FileOrDirectoryEntry): List[FileEntry] =
    fileEntry.contents match {
      case Some(children) => children.flatMap(flatten)
      case None           =>
        List(
          FileEntry(
            fileEntry.name.split('/').dropRight(1).mkString("/"),
            fileEntry.name,
            fileEntry.size,
          ),
        )
    }

  private val assetLocation = s"${dom.window.location.origin}/${if (
    dom.window.location.host == "localhost" || dom.window.location.host
      .startsWith("localhost")
  ) ""
  else "assets/"}"

  val fetchBackend: SttpBackend[IO, capabilities.WebSockets] = FetchCatsBackend[IO]()

  def getAssetUri(relativeUri: Uri): Uri =
    Uri.parse(assetLocation).toOption.get.resolve(relativeUri)

  def allAssetFiles: IO[FileOrDirectoryEntry] = {
    import io.circe.generic.auto._
    import sttp.client3._
    import sttp.client3.circe._

    val request = basicRequest
      .get(
        getAssetUri(uri"listing.json"),
      )
      .response(asJson[List[FileOrDirectoryEntry]])

    request
      .send(fetchBackend)
      .to[IO]
      .flatMap(resp => IO.fromEither(resp.body.map(_.head)))
  }

  def loadSubtitle(fileEntry: FileEntry): IO[String] = {
    import sttp.client3._

    val request = basicRequest
      .get(
        getAssetUri(uri"${fileEntry.name}"),
      )

    request
      .send(fetchBackend)
      .to[IO]
      .flatMap(resp => IO.fromOption(resp.body.toOption)(new RuntimeException("broken")))

  }
}
