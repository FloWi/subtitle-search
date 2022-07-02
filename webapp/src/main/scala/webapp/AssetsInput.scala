package webapp

import cats.effect.IO
import sttp.client3.impl.cats.FetchCatsBackend

object AssetsInput {

  case class FileOrDirectoryEntry(
    path: String,
    name: String,
    size: Int,
    `type`: String,
    children: Option[List[FileOrDirectoryEntry]],
  )

  case class FileEntry(
    path: String,
    name: String,
    size: Int,
  )

  def flatten(fileEntry: FileOrDirectoryEntry): List[FileEntry] =
    fileEntry.children match {
      case Some(children) => children.flatMap(flatten)
      case None           =>
        List(
          FileEntry(
            fileEntry.path,
            fileEntry.name,
            fileEntry.size,
          ),
        )
    }

  private val assetLocation = ""

  val fetchBackend = FetchCatsBackend[IO]()

  def allAssetFiles: IO[FileOrDirectoryEntry] = {
    import io.circe.generic.auto._
    import sttp.client3._
    import sttp.client3.circe._

    val request = basicRequest
      .get(
        uri"${assetLocation}/listing.json",
      )
      .response(asJson[FileOrDirectoryEntry])

    request
      .send(fetchBackend)
      .to[IO]
      .flatMap(resp => IO.fromEither(resp.body))
  }

  def loadSubtitle(fileEntry: FileEntry): IO[String] = {
    import sttp.client3._

    val request = basicRequest
      .get(
        uri"${assetLocation}/".resolve(uri"${fileEntry.path}").resolve(uri"${fileEntry.name}"),
      )

    request
      .send(fetchBackend)
      .to[IO]
      .flatMap(resp => IO.fromOption(resp.body.toOption)(new RuntimeException("broken")))

  }
}
