package org.exaxis.ll1

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.prop.TableDrivenPropertyChecks.*
import scala.io.Source
import scala.util.Try

class ParserSpec extends AnyFlatSpec {

  "The parser " should "be able to parse english text" in {
    val source = Source.fromString("Oh, that's the way, uh-huh uh-huh.\nI like it, uh-huh, uh-huh!\nThat's the way, uh-huh uh-huh.\nI like it, uh-huh, uh-huh?\nThat's the way, uh-huh uh-huh.\nI like it, uh-huh, uh-huh.\nThat's the way, uh-huh uh-huh.\nI like it, uh-huh, uh-huh.")
    val sentences = Parser.parse(source)
    sentences should have length 8

    val wordCounts = Table("words", 6, 5, 5, 5, 5, 5, 5, 5)
    val sentenceIt = sentences.iterator
    forAll(wordCounts) { words =>
      sentenceIt.next should have length words
    }

  }
import org.scalatest.prop.Tables.Table

"The parser " should "ignore empty sentences" in {
    val source = Source.fromString("?.!")
    val sentences = Parser.parse(source)
    sentences shouldBe empty
  }

  "The parser " should "ignore empty sentences around a valid sentence" in {
    val source = Source.fromString("?This is one sentence.!")
    val sentences = Parser.parse(source)
    sentences should have length 1
  }

  "The parser " should "be able to parse the bible" in {
    val source = Source.fromFile("src/test/resources/bible.txt")
    val sentences = Parser.parse(source)
    sentences should have length 28925
    val firstSentenceWords = Table("word", "In", "the", "beginning", "God", "created", "the", "heaven", "and", "the", "earth")

    val firstSentence = sentences.head
    firstSentence should have length 10

    val firstSentenceIt = firstSentence.iterator
    forAll(firstSentenceWords) { word =>
      firstSentenceIt.next should equal(word)
    }

    sentences.last should have length 1
    val last = sentences.last.head should equal ("Amen")
  }
}
