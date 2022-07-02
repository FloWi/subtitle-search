package webapp.model

import cats.effect.IO
import colibri.Observable
import webapp.AssetsInput
import webapp.AssetsInput.FileEntry
import webapp.components.VttMerger
import webapp.components.VttMerger.SubtitleSentence
import webapp.facade.{SubtitleEntry, WebVttParser}

import scala.scalajs.js

object Model {
  case class Lecture(title: String, videoFile: VideoFile, subtitleFile: SubtitleFile, sentences: List[SubtitleSentence])
  sealed trait AssetFile {
    def folder: String
  }
  case class VideoFile(entry: FileEntry)    extends AssetFile {
    override val folder: String = entry.path.replace(s"/${entry.name}", "")
  }
  case class SubtitleFile(entry: FileEntry) extends AssetFile {
    override val folder: String = entry.path.replace(s"/${entry.name}", "")
  }

  case class SearchResult(searchTerm: String, entries: List[SearchResultEntry])
  case class SearchResultEntry(lecture: Lecture, matchingSentences: List[SubtitleSentence])

  object Lecture {
    def search(lectures: List[Lecture], text: String): SearchResult = {
      val entries = lectures.flatMap { l =>
        val matches = l.sentences.filter(s => s.sentence.toLowerCase.contains(text.toLowerCase))
        if (matches.isEmpty) List.empty
        else List(SearchResultEntry(l, matches))
      }
      SearchResult(text, entries)
    }

    def loadAndParseSubtitle(file: SubtitleFile): IO[List[SubtitleSentence]] =
      AssetsInput.loadSubtitle(file.entry).map { content =>
        val subtitleEntries: js.Array[SubtitleEntry] = WebVttParser.parse(content).entries
        val mergedSentences                          = VttMerger.mergeSentences(subtitleEntries.toList.take(10))
        mergedSentences
      }

    def load(allFiles: List[FileEntry]): IO[List[Lecture]] = {

      import cats.implicits._

      val allSubtitles: List[AssetFile] = allFiles.filter(_.name.endsWith(".vtt")).map(SubtitleFile)
      val allVideos                     = allFiles.filter(_.name.endsWith(".mp4")).map(VideoFile)

      val merged = allSubtitles.groupBy(_.folder) |+| allVideos.groupBy(_.folder)

      merged.toList.traverse { case (path, files) =>
        // let it crash
        println(s"loading path: '$path'")
        val video    = files.collectFirst { case e: VideoFile => e }.get
        val subtitle = files.collectFirst { case e: SubtitleFile => e }.get

        loadAndParseSubtitle(subtitle).map { sentences =>
          Lecture(path.split('/').lastOption.getOrElse(path), video, subtitle, sentences)
        }
      }.map(_.sortBy(_.title))

    }
  }

}
