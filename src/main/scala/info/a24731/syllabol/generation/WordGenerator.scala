package info.a24731.syllabol.generation

import com.mifmif.common.regex.Generex

import scala.util.Try

trait WordGenerator:
  def generate(expression: String, count: Int): Try[List[String]]

class GenerexWordGenerator extends WordGenerator:
  override def generate(expression: String, count: Int): Try[List[String]] = Try {
    val generex = new Generex(expression)
    List.fill(count)(generex.random())
  }
