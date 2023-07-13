package org.exaxis.ll1

import scala.io.Source
import scala.util.Try

trait Peekable[T]:
  def peek:Try[T]
  def poll:Try[T]

/**
 * Create a peekable iterator to allow the caller to peek() the next item without moving the current position.
 *
 * @param it an Iterator
 * @tparam T the type of value in the iterator
 */
class PeekableIterator[T](private val it:Iterator[T]) extends Peekable[T]:
  private var nextToken:Try[T] = Try(it.next)
  def peek:Try[T] = nextToken
  def poll:Try[T] =
    val currentToken = nextToken
    nextToken = Try(it.next)
    currentToken

/**
 * Companion object for creating PeekableIterators from an iterator or by chaining iteratars.
 */
object PeekableIterator:
  def apply[T](it:Iterator[T]):Peekable[T] = new PeekableIterator[T](it)

  def apply[TInput, TOutput](it1:Iterator[TInput], getNext: Peekable[TInput]=>TOutput ): Peekable[TOutput] =
    val input = PeekableIterator[TInput](it1)
    PeekableIterator[TOutput](new Iterator[TOutput] {
      override def hasNext: Boolean = input.peek.isSuccess
      override def next(): TOutput = getNext(input)
    })

