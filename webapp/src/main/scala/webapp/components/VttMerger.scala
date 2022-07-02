package webapp.components

import webapp.facade.SubtitleEntry

import scala.collection.immutable.Queue

object VttMerger {

  case class SubtitleSentence(sentence: String, originalEntries: List[String], from: Int, to: Int)

  def firstIndexOfPunctuation(str: String): Option[Int] = {
    val regex = "[.?!]".r
    regex.findFirstMatchIn(str).map(_.start)
  }

  def mergeSentences(entries: List[SubtitleEntry]): List[SubtitleSentence] = {

    def helper(
      current: List[SubtitleEntry],
      intermediate: Queue[SubtitleEntry],
      result: Queue[SubtitleSentence],
    ): List[SubtitleSentence] =
      // awesome ending of first sentence. This is the beginning of the 2nd
      current match {
        case ::(head, next) =>
          println(s"analyzing '${head.text}'")
          firstIndexOfPunctuation(head.text) match {
            case None      => helper(next, intermediate.enqueue(head), result)
            case Some(idx) =>
              val (beforeDot, after) = head.text.splitAt(idx)
              val afterDot           = after.drop(1)
              val punctuationChar    = head.text(idx)

              val mergedSentence = intermediate.map(_.text).mkString(" ") + beforeDot + punctuationChar
              val ids            = intermediate.map(_.id) ++ List(head.id)
              val res            =
                SubtitleSentence(mergedSentence, ids.toList, intermediate.map(_.from).minOption.getOrElse(0), head.to)

              if (afterDot.isEmpty)
                helper(current = next, intermediate = Queue.empty, result = result.enqueue(res))
              else {

                val entryWithRestSentence = SubtitleEntry(head.id, head.from, head.to, afterDot)
                helper(current = next, intermediate = Queue(entryWithRestSentence), result = result.enqueue(res))
              }
          }

        case Nil => result.toList
      }

    helper(entries, Queue.empty, Queue.empty)

  }

}
