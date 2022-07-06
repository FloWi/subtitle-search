package webapp

import cats.effect.{IO, SyncIO}
import colibri.{BehaviorSubject, Observable, Subject}
import org.scalajs.dom.KeyCode
import outwatch._
import outwatch.dsl._
import sttp.client3.UriContext
import webapp.components.VttMerger.SubtitleSentence
import webapp.model.Model.{Lecture, SearchResult}

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
    lessons  <- AssetsInput.lessons
    lectures <- Lecture.load(lessons)
  } yield lectures

  def markSearchTermInText(searchTerm: String, sentence: SubtitleSentence) = {

    val idx               = sentence.sentence.toLowerCase.indexOf(searchTerm.toLowerCase)
    val before            = sentence.sentence.substring(0, idx)
    val term              = sentence.sentence.substring(idx, idx + searchTerm.length)
    val after             = sentence.sentence.substring(idx + searchTerm.length)
    val isMatchInsideWord =
      before.lastOption.exists(c => !c.isWhitespace) || after.headOption.exists(c => !c.isWhitespace)

    val markedSection = VNode.html("mark")(term)
    val formatted     =
      if (isMatchInsideWord) markedSection(padding := "2px 0px")
      else markedSection

    println(s"searchterm: $searchTerm; insideWord: $isMatchInsideWord, sentence: '$sentence'")
    p(
      span(before, formatted, after),
    )

  }

  def renderUI(lectures: List[Lecture]): HtmlVNode = {

    val selectedLectureSubject: BehaviorSubject[(Option[Lecture], Option[SubtitleSentence])] =
      Subject.behavior(Option.empty -> Option.empty)

    val searchText          = Subject.behavior("")
    val submittedSearchText = Subject.behavior("")

    val selectedSubtitleLocation: BehaviorSubject[Option[(Lecture, SubtitleSentence, Option[SearchResult])]] =
      Subject.behavior(Option.empty)

    val searchResultSub: Observable[SearchResult] = submittedSearchText.distinctOnEquals.flatMap { text =>
      if (text.nonEmpty) {
        Observable(Lecture.search(lectures, text))
      }
      else {
        Observable(SearchResult(text, List.empty))
      }
    }

    searchResultSub.unsafeForeach(_ => selectedSubtitleLocation.unsafeOnNext(None))

    val searchDiv = div(
      submitTextbox(searchText, submittedSearchText),
      searchResultSub.map { result =>
        val results = result.entries

        val resultHtml = {
          if (results.isEmpty) VModifier.empty
          else
            table(
              thead(
                tr(
                  th("from"),
                  th("to"),
                  th("sentence"),
                  th("jump"),
                ),
              ),
              tbody(
                results.map { sr =>
                  VModifier(
                    tr(td(colSpan := 4, h5(sr.lecture.title))),
                    sr.matchingSentences.map { sub =>
                      tr(
                        td(sub.from),
                        td(sub.to),
                        td(markSearchTermInText(result.searchTerm, sub)),
                        td(
                          button(
                            "Jump",
                            onClick.as(Some((sr.lecture, sub, Some(result)))) --> selectedSubtitleLocation,
                          ),
                          onClick.as((Some(sr.lecture), Some(sub))) --> selectedLectureSubject,
                        ),
                      )

                    },
                  )
                },
              ),
            )
        }

        div(
          resultHtml,
        )

      },
      h2("All Lessons"),
      ul(
        lectures.map { lecture =>
          li(
            cursor.pointer,
            s"${lecture.title}",
            onClick.as((Some(lecture), None)) --> selectedLectureSubject,
          )
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
        case None                                         => VModifier.empty
        case Some((lecture, sentence, maybeSearchResult)) =>
          val sentenceNode =
            maybeSearchResult.map(sr => markSearchTermInText(sr.searchTerm, sentence)).getOrElse(p(sentence.sentence))
          val tbl          = table(
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
                td(sentenceNode),
              ),
            ),
          )

          div(
            tbl,
            h3(lecture.title),
            video(
              src                        := AssetsInput.getAssetUri(uri"${lecture.lesson.videoUri}#t=${sentence.from / 1000}").toString,
              VModifier.attr("controls") := true,
              tpe                        := "video/mp4",
              width                      := "90%",
            ),
          )
      },
    )

    val wholeLectureDisplay = div(
      selectedLectureSubject.map {
        case (None, _)                     => VModifier.empty
        case (Some(lecture), maybeSection) =>
          table(
            thead(
              tr(
                th("from"),
                th("to"),
                th("sentence"),
                th("jump"),
              ),
            ),
            tbody(
              lecture.sentences.map { sub =>
                tr(
                  VModifier.ifTrue(maybeSection.contains(sub))(backgroundColor := "var(--marked)"),
                  td(sub.from),
                  td(sub.to),
                  td(sub.sentence),
                  td(
                    button(
                      "Jump",
                      onClick.as(Some((lecture, sub, None))) --> selectedSubtitleLocation,
                      onClick.as((Some(lecture), Some(sub))) --> selectedLectureSubject,
                    ),
                  ),
                )
              },
            ),
          )

      },
    )
    div(
      div(
        display.flex,
        flexDirection.row,
        VModifier.style("gap") := "5%",
        div(width := "30%", h2("Search"), searchDiv),
        div(width := "65%", h2("Watch"), videoDiv),
      ),
      wholeLectureDisplay,
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
      button("clear", onClick.as("") --> text, onClick.as("") --> submitted),
      button("search", onClick(text) --> submitted),
    )
  }

}
