package webapp.model

import cats.effect.IO
import webapp.AssetsInput
import webapp.AssetsInput.Lesson
import webapp.components.VttMerger
import webapp.components.VttMerger.SubtitleSentence
import webapp.facade.{SubtitleEntry, WebVttParser}

import scala.scalajs.js

object Model {
  case class Lecture(title: String, lesson: Lesson, sentences: List[SubtitleSentence])

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

    def loadAndParseSubtitle(lesson: Lesson): IO[List[SubtitleSentence]] =
      AssetsInput.loadSubtitle(lesson).map { content =>
        val subtitleEntries: js.Array[SubtitleEntry] = WebVttParser.parse(content).entries
        val mergedSentences                          = VttMerger.mergeSentences(subtitleEntries.toList)
        mergedSentences
      }

    def load(allLessons: List[Lesson]): IO[List[Lecture]] = {

      import cats.implicits._

      allLessons.traverse { lesson =>
        loadAndParseSubtitle(lesson).map { sentences =>
          val lectureTitle = lesson.folderUri.split('/').takeRight(2).toList match {
            case week :: title :: Nil => s"${week.capitalize} - ${title.capitalize}"
            case _                    => lesson.folderUri
          }
          Lecture(lectureTitle, lesson, sentences)
        }

      }.map(_.sortBy(_.title))

    }
  }

}
