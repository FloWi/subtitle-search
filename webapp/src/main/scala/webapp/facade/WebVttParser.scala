package webapp.facade

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

@JSImport("@plussub/srt-vtt-parser", JSImport.Default)
@js.native
object WebVttParser extends js.Object {
  def parse(raw: String): ParsedResult = js.native
}

@js.native
trait ParsedResult extends js.Object {
  val entries: js.Array[SubtitleEntry]
}

@js.native
trait SubtitleEntry extends js.Object {
  val id: String   = js.native
  val from: Int    = js.native
  val to: Int      = js.native
  val text: String = js.native
}

object SubtitleEntry {
  def apply(id: String, from: Int, to: Int, text: String): SubtitleEntry =
    js.Dynamic
      .literal(
        id = id,
        from = from,
        to = to,
        text = text,
      )
      .asInstanceOf[SubtitleEntry]
}
