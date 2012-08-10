package org.mechaflow.utils.language

import util.parsing.combinator.lexical.StdLexical
import scala.util.parsing.input.CharArrayReader.EofCh

/**
 * Splits input stream of characters into tokens for a parser.
 */
class LanguageLexer extends StdLexical {

  val hexDigits = Set[Char]() ++ "0123456789abcdefABCDEF".toArray

  case class SingleQuotedStringLiteral(chars: String) extends Token {
    override def toString = "\'"+chars+"\'"
  }


  override lazy val token: Parser[Token] =
    (identChar ~ rep(identChar | digit) ^^ {
      case first ~ rest => processIdent(first :: rest mkString "")
    }
      | string ^^ StringLit
      | singleQuotedString ^^ SingleQuotedStringLiteral
      | number ~ letter ^^ {
      case n ~ l => ErrorToken("Invalid number format : " + n + l)
    }
      | number ^^ NumericLit
      | EofCh ^^^ EOF
      | delim
      | '\"' ~> failure("Unterminated string")
      | failure("Illegal character")
      )

  /**
   * A string is a collection of zero or more Unicode characters, wrapped in
   * double quotes, using backslash escapes.
   */
  lazy val string = '\"' ~> rep(charSeq | chrExcept('\"', '\n', EofCh)) <~ '\"' ^^ {
    _ mkString ""
  }


  /**
   * Single quoted string
   */
  lazy val singleQuotedString = '\'' ~> rep(charSeq | chrExcept('\'', '\n', EofCh)) <~ '\'' ^^ {
    _ mkString ""
  }


  /**
   * Floating point number.
   */
  lazy val number = intPart ~ opt(fracPart) ~ opt(expPart) ^^ {
    case i ~ f ~ e =>
      i + optString(".", f) + optString("", e)
  }

  lazy val intPart = zero | intList
  lazy val intList = nonzero ~ rep(digit) ^^ {
    case x ~ y => (x :: y) mkString ""
  }
  lazy val fracPart = '.' ~> rep(digit) ^^ {
    _ mkString ""
  }
  lazy val expPart =
    exponent ~ opt(sign) ~ rep1(digit) ^^ {
      case e ~ s ~ d =>
        e.toString + optString("", s) + d.mkString("")
    }

  lazy val zero: Parser[String] = '0' ^^^ "0"
  lazy val nonzero = elem("nonzero digit", d => d.isDigit && d != '0')
  lazy val exponent = elem("exponent character", d => d == 'e' || d == 'E')

  lazy val sign = elem("sign character", d => d == '-' || d == '+')

  lazy val charSeq: Parser[String] =
    ('\\' ~ '\"' ^^^ "\""
      | '\\' ~ '\\' ^^^ "\\"
      | '\\' ~ '/' ^^^ "/"
      | '\\' ~ 'b' ^^^ "\b"
      | '\\' ~ 'f' ^^^ "\f"
      | '\\' ~ 'n' ^^^ "\n"
      | '\\' ~ 'r' ^^^ "\r"
      | '\\' ~ 't' ^^^ "\t"
      | '\\' ~> 'u' ~> unicodeBlock)

  lazy val hexDigit = elem("hex digit", hexDigits.contains(_))


  private def optString[A](pre: String, a: Option[A]) = a match {
    case Some(x) => pre + x.toString
    case None => ""
  }

  private lazy val unicodeBlock = hexDigit ~ hexDigit ~ hexDigit ~ hexDigit ^^ {
    case a ~ b ~ c ~ d =>
      new String(Array(Integer.parseInt(List(a, b, c, d) mkString "", 16)), 0, 1)
  }

}
