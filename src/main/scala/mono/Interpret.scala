package mono

import cats.data.Coproduct
import cats.~>
import monix.eval.Task
import mono.alias.{ AliasInMemoryInterpreter, AliasOp }
import mono.article.{ ArticleOp, ArticlesInMemoryInterpreter }
import mono.author.{ AuthorOp, AuthorsInMemoryInterpreter }

object Interpret {

  type Op0[A] = Coproduct[ArticleOp, AuthorOp, A]

  type Op[A] = Coproduct[AliasOp, Op0, A]

  def inMemory: Op ~> Task = {
    val i0: Op0 ~> Task = new ArticlesInMemoryInterpreter or new AuthorsInMemoryInterpreter
    new AliasInMemoryInterpreter or i0
  }
}
