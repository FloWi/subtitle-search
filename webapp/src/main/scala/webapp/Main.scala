package webapp

import cats.effect.{IO, SyncIO}
import colibri.{BehaviorSubject, Observable, Subject}
import org.scalajs.dom.KeyCode
import outwatch._
import outwatch.dsl._
import webapp.components.VttMerger.SubtitleSentence
import webapp.model.Model.{Lecture, SearchResult, SearchResultEntry}

// Outwatch documentation:
// https://outwatch.github.io/docs/readme.html

object Main {
  def main(args: Array[String]): Unit =
    Outwatch.renderInto[SyncIO]("#app", app).unsafeRunSync()

  def app = div(
    lecturesIO.map { lectures =>
      renderUI(lectures)
    },
  )

  val lecturesIO: IO[List[Lecture]] = for {
    filesOrDirs <- AssetsInput.allAssetFiles
    files        = AssetsInput.flatten(filesOrDirs)
    lectures    <- Lecture.load(files)
  } yield lectures

  def markSearchTermInText(searchTerm: String, sentence: SubtitleSentence) = {

    val idx    = sentence.sentence.toLowerCase.indexOf(searchTerm.toLowerCase)
    val before = sentence.sentence.substring(0, idx)
    val term   = sentence.sentence.substring(idx, idx + searchTerm.length)
    val after  = sentence.sentence.substring(idx + searchTerm.length)
    p(before, VNode.html("mark")(term), after)

  }

  def renderUI(lectures: List[Lecture]): HtmlVNode = {

    val selectedLectureSubject = Subject.behavior(Option.empty[Lecture])

    val searchText          = Subject.behavior("")
    val submittedSearchText = Subject.behavior("")

    val selectedSubtitleLocation: BehaviorSubject[Option[(SearchResult, Lecture, SubtitleSentence)]] =
      Subject.behavior(Option.empty)

    val searchResultSub: Observable[SearchResult] = submittedSearchText.distinctOnEquals.flatMap { text =>
      if (text.nonEmpty) {
        Observable(Lecture.search(lectures, text))
      }
      else {
        Observable(SearchResult(text, List.empty))
      }
    }

    val searchDiv = div(
      submitTextbox(searchText, submittedSearchText),
      searchResultSub.map { result =>
        val results = result.entries

        results.map { sr =>
          val lecture = sr.lecture
          val merged  = table(
            thead(
              tr(
                th("from"),
                th("to"),
                th("sentence"),
                th("jump"),
              ),
            ),
            tbody(
              sr.matchingSentences.map { sub =>
                tr(
                  td(sub.from),
                  td(sub.to),
                  td(markSearchTermInText(result.searchTerm, sub)),
                  td(button("Jump", onClick.as(Some((result, lecture, sub))) --> selectedSubtitleLocation)),
                )

              },
            ),
          )

          div(
            h3(lecture.title),
            merged,
          )
        }
      },
      h2("All Lessons"),
      ul(
        lectures.map { lecture =>
          li(s"${lecture.title}", onClick.as(Some(lecture)) --> selectedLectureSubject)
        },
      ),
    )

    val videoDiv = div(
      /*
      <video controls preload="metadata">
    <source src="http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4" type="video/mp4"/>
    Video not supported.
</video>
       */
      selectedSubtitleLocation.map {
        case None                          => VModifier.empty
        case Some((sr, lecture, sentence)) =>
          val tbl = table(
            thead(
              tr(
                th("from"),
                th("to"),
                th("sentence"),
              ),
            ),
            tbody(
              tr(
                td(sentence.from),
                td(sentence.to),
                td(markSearchTermInText(sr.searchTerm, sentence)),
              ),
            ),
          )

          div(
            h2("Watch"),
            tbl,
            h3(lecture.title),
            video(
              src                        := s"${lecture.videoFile.entry.path}#t=${sentence.from / 1000}",
              VModifier.attr("controls") := true,
              tpe                        := "video/mp4",
            ),
          )
      },
    )

    div(
      display.flex,
      flexDirection.row,
      searchDiv(width := "1/3"),
      videoDiv(width  := "2/3"),
    )
  }

  def submitTextbox(text: BehaviorSubject[String], submitted: BehaviorSubject[String]) = {

    // Emitterbuilders can be extracted and reused!
    val onEnter = onKeyDown
      .filter(e => e.keyCode == KeyCode.Enter)
      .preventDefault

    div(
      input(
        tpe := "text",
        value <-- text,
        onInput.value --> text,
        onEnter(text) --> submitted,
      ),
      button("clear", onClick.as("") --> text),
      button("search", onClick(text) --> submitted),
    )
  }

}
