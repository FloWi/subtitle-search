package webapp

import cats.effect.{IO, SyncIO}
import colibri.{Observable, Subject}
import outwatch._
import outwatch.dsl._
import webapp.AssetsInput.FileEntry
import webapp.components.VttMerger
import webapp.facade.{SubtitleEntry, WebVttParser}

import scala.scalajs.js

// Outwatch documentation:
// https://outwatch.github.io/docs/readme.html

object Main {
  def main(args: Array[String]): Unit =
    Outwatch.renderInto[SyncIO]("#app", app).unsafeRunSync()

  def app = div(
    AssetsInput.allAssetFiles.map { files =>
      renderUI(AssetsInput.flatten(files))
    },
  )

  def renderUI(files: List[FileEntry]): HtmlVNode = {

    val selectedSubtitleFileSubject = Subject.behavior(Option.empty[FileEntry])
    val subtitleFiles               = files.filter(_.name.endsWith(".vtt"))

    val selectedSubtitleSub: Observable[Option[(FileEntry, String)]] = selectedSubtitleFileSubject.mapEffect {
      case Some(file) =>
        AssetsInput.loadSubtitle(file).map(content => Some(file -> content))
      case None       => IO(None)
    }

    div(
      h2("Selected File"),
      selectedSubtitleSub.map {
        case Some((file, content)) =>
          val subtitleEntries: js.Array[SubtitleEntry] = WebVttParser.parse(content).entries
          val mergedSentences                          = VttMerger.mergeSentences(subtitleEntries.toList.take(10))

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
              mergedSentences.map { sub =>
                tr(
                  td(sub.originalEntries.mkString(", ")),
                  td(sub.from),
                  td(sub.to),
                  td(sub.sentence),
                )

              },
            ),
          )

          val original = table(
            thead(
              tr(
                th("id"),
                th("from"),
                th("to"),
                th("text"),
              ),
            ),
            tbody(
              subtitleEntries.map { sub =>
                js.Dynamic.global.console.log(sub)
                tr(
                  td(sub.id),
                  td(sub.from),
                  td(sub.to),
                  td(sub.text),
                )

              },
            ),
          )

          div(
            h3(file.toString),
            h3("merged"),
            merged,
            h3("original"),
            original,
            pre(content),
          )
        case None                  =>
          VModifier.empty
      },
      h2("All Subtitle Files"),
      ul(
        subtitleFiles.map { file =>
          li(s"${file.path}/${file.name}", onClick.as(Some(file)) --> selectedSubtitleFileSubject)
        },
      ),
    )
  }

}
