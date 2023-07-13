package org.exaxis.ll1

import scala.io.Source

object Parser:
  def parse(source:Source):Vector[Vector[String]] =
    // create the lexer by chaining the source to the lazylexer
    var sentences:Vector[Vector[String]] = Vector.empty
    var sentence:Vector[String] = Vector.empty
    val lexer = PeekableIterator[Char,String](source, lazyLexer)

    while lexer.peek.isSuccess do
      if lexer.peek.get.isEmpty then
        // ignore empty sentences
        if sentence.nonEmpty then
          sentences = sentences :+ sentence
          sentence = Vector.empty
        end if
        lexer.poll
      else
        sentence = sentence :+ lexer.poll.get
      end if
    end while

    if sentence.nonEmpty then
      sentences = sentences :+ sentence
    end if

    sentences
  end parse

  // method used to do the actual tokenizing. It builds one token and returns
  private def lazyLexer(tokenizer: Peekable[Char]): String =
    // ignore the whitespace and look for an end of sentence
    while tokenizer.peek.isSuccess && (tokenizer.peek.get.isWhitespace || tokenizer.peek.get.isOtherPunctuation) do
      tokenizer.poll

    if tokenizer.peek.isSuccess && tokenizer.peek.get.isEndOfSentence then
      tokenizer.poll
      return ""

    if tokenizer.peek.isFailure then
      throw tokenizer.peek.failed.get

    // build a word
    val sb = new StringBuilder()
    while tokenizer.peek.isSuccess && !(tokenizer.peek.get.isWhitespace || tokenizer.peek.get.isPunctuation) do
      sb.append(tokenizer.poll.get)

    // return the word
    sb.toString()
  end lazyLexer

// Char extension class to help identify the terminals for the lexer
extension (ch: Char)
  def isPunctuation: Boolean = ch.isEndOfSentence || ch.isOtherPunctuation
  def isEndOfSentence: Boolean =
    ch match
      case '.' | '?' | '!' => true
      case _ => false
  def isOtherPunctuation: Boolean =
    ch match
      case ',' | '"' | ';' | ':' | '`' | '(' | ')' | '{' | '}' | '[' | ']' => true
      case _ => false
