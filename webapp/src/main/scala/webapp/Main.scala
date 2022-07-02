package webapp

import cats.effect.{IO, SyncIO}
import colibri.Subject
import outwatch._
import outwatch.dsl._
import webapp.model.Model.Lecture

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

  def renderUI(lectures: List[Lecture]): HtmlVNode = {

    val selectedLectureSubject = Subject.behavior(Option.empty[Lecture])

    div(
      selectedLectureSubject.map {
        case Some(lecture) =>
          val merged = table(
            thead(
              tr(
                th("original ids"),
                th("from"),
                th("to"),
                th("sentence"),
              ),
            ),
            tbody(
              lecture.sentences.map { sub =>
                tr(
                  td(sub.originalEntries.mkString(", ")),
                  td(sub.from),
                  td(sub.to),
                  td(sub.sentence),
                )

              },
            ),
          )

          div(
            h3(lecture.title),
            h3("merged"),
            merged,
          )
        case None          =>
          VModifier.empty
      },
      h2("All Lessons"),
      ul(
        lectures.map { lecture =>
          li(s"${lecture.title}", onClick.as(Some(lecture)) --> selectedLectureSubject)
        },
      ),
    )
  }

}
