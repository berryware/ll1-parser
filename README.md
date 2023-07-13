# LL(1) Parser example in Scala
From [Wikipedia](https://en.wikipedia.org/wiki/LL_parser), an LL parser (Left-to-right, leftmost derivation) is a top-down parser for a restricted context-free language. An LL parser is called an LL(k) parser if it uses k tokens of lookahead when parsing a sentence. An LL(1) parser therefore has 1 token of lookahead. The lookahead allows you to make decisions on future tokens without removing the token from the input. Many modern day parsers are top down parsers which take a source file, turn it into a set of tokens, and parses those tokens into an abstract syntax tree or some other data structure.

Scala has a `Source` object that allows you to create an `Iterator[Char]` from files or strings. We will use this as the basis of the input for our parser. Unfortunately, iterators do not allow you to peek or look ahead. Once you call `next()` you have consumed the next `Char`. What we need is an iterator that allows you to peek and see the next item. Here is a `PeekableIterator` class that wraps an iterator and provides the `peek()` and `poke()` methods.

```scala
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
```

`PeekableIterator()` takes an iterator parameter and immediately calls next to load the next token. `nextToken` is a `Try[T]` so that it captures either the successful token or the failure. `peek()` returns the `nextToken`, put does not call next on the iterator. `poll()`, on the other hand, will return the `nextToken` and will call `next()` on the iterator and move to the next token.

Now that we can look ahead, let's define the goal of the parser. The parser should parse english text and return a list of sentences where each sentence is a list of words. The return type is `Vector[Vector[String]]` as Vector has better append performance than List. So how do we go from a Peekable[Char] to the `Vector[Vector[String]]`?

Most parsers start with a lexical analysis(tokenizing) step and then a parsing step. Our parsing goal is simple and does not require an LL(1) parser, but we use LL(1) parsing techniques to give readers an example they can use for more complicated parsing. Therefore we will process the source file using both the tokenizing step and a parsing step. The tokenizing step is just another parser but is usually referred to as a lexer. A first implementation could be step 1 as a lexer of Char to `Vector[String]` and step 2 is a Parser of `Vector[String]` to `Vector[Vector[String]]`.

The shortcoming of this design is that the entire file is turned into tokens before any tokens are consumed by the parser. If you try to parse an extremely large file, you could run out of memory building the `Vector[String]` and never parse one token. It would be more efficient, in both time and space, to be able to transform the source file into tokens in a stream-like way where tokens are created lazily and consume the source input as needed.

3 ways to solve this problem were considered: LazyList, Akka Streams, and chained iterators. The addition of Akka Streams seemed like overkill for a simple parser, but it may be the subject of a future article. LazyList's immutability and memoization made it complicated to just create and consume one token at a time. Hence, I was left with chaining iterators together.

The goal of iterator chaining is to allow one iterator to use another iterator to lazily create its next item. To demonstrate this I will chain `Iterator[Char]` to `Iterator[String]`. The chaining is done as an `apply` on the `PeekableIterator` object:

```scala
/**
 * Companion object for creating PeekableIterators from an iterator or by chaining iteratars.
 */
object PeekableIterator:
  def apply[T](it:Iterator[T]):PeekableIterator[T] = new PeekableIterator[T](it)
  
  def apply[TInput, TOutput](it1:Iterator[TInput], getNext: PeekableIterator[TInput]=>TOutput ): PeekableIterator[TOutput] =
    val input = PeekableIterator[TInput](it1)
    PeekableIterator[TOutput](new Iterator[TOutput] {
      override def hasNext: Boolean = input.peek.isSuccess
      override def next(): TOutput = getNext(input)
    })
```
The second `apply` method takes an `Iterator` and a `getNext` function which will be used to dynamically create an iterator that will chain or wrap the first iterator. Its use is shown in the creation of the lexer for the parser.

```scala
    val lexer = PeekableIterator[Char,String](source, lazyLexer)
```

`lazyLexer` is the method that is used as the getNext() for the dynamically created iterator. It takes a `Peekable[Char]` as its parameter and returns a `String`

```scala
  // method used to do the actual tokenizing. It builds one token and returns
  private def lazyLexer(tokenizer: Peekable[Char]): String =
    // ignore the whitespace and look for an end of sentence
    while tokenizer.peek.isSuccess && (tokenizer.peek.get.isWhitespace || tokenizer.peek.get.isOtherPunctuation) do
      tokenizer.poll

    // Represent the End of Sentence as an empty String
    if tokenizer.peek.isSuccess && tokenizer.peek.get.isEndOfSentence then
      tokenizer.poll
      return ""

    // if we are at the end of input pass the failure on
    if tokenizer.peek.isFailure then
      throw tokenizer.peek.failed.get

    // build a word
    val sb = new StringBuilder()
    while tokenizer.peek.isSuccess && !(tokenizer.peek.get.isWhitespace || tokenizer.peek.get.isPunctuation) do
      sb.append(tokenizer.poll.get)

    // return the word
    sb.toString()
  end lazyLexer
```

Finally, the parser takes the words from the lexer and builds a Vector[Vector[String]] to represent a list of sentences. It is written to ignore empty sentences.

```scala
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

```
