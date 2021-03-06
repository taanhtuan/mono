package mono.bot.script

import java.time.Instant

import cats.free.Free
import mono.core.alias.{ Alias, AliasOps }
import mono.core.article.{ Article, ArticleOps }
import mono.core.person.PersonOps
import mono.bot.BotScript.{ Op, Scenario }
import mono.bot.BotState.{ ArticleContext, Idle, InitNewArticle }
import mono.bot._
import ArticleScript.showArticleContext

class NewArticleScript(implicit
  B: BotOps[BotScript.Op],
                       A:  ArticleOps[BotScript.Op],
                       Au: PersonOps[BotScript.Op],
                       As: AliasOps[BotScript.Op]) extends Script {
  def createArticle(title: String, m: Incoming.Meta): Free[BotScript.Op, Article] =
    for {
      au ← Au.ensureTelegram(m.chat.id, m.chat.title.getOrElse("??? " + m.chat.id))
      _ ← m.chat.alias.fold(Free.pure[BotScript.Op, Option[Alias]](None))(alias ⇒ As.tryPointTo(alias, au, force = false))
      a ← A.create(au.id, "ru", title, Instant.now())
      _ ← showArticleContext(a, m)
    } yield a

  override val scenario: Scenario = {
    case (_, Command("new", Some(title), m)) ⇒
      createArticle(title, m).map(a ⇒ ArticleContext(a.id))

    case (_, Command("new", _, m)) ⇒
      for {
        _ ← B.reply("Введите заголовок", m)
      } yield InitNewArticle

    case (InitNewArticle, Plain(title, m)) ⇒
      createArticle(title, m).map(a ⇒ ArticleContext(a.id))

    case (InitNewArticle | Idle, File(fileId, Some("text/plain"), fileName, _, m)) ⇒
      for {
        read ← ArticleScript.readTextFile(fileId)
        (title, text) = read
        a ← createArticle(title.orElse(fileName).orElse(m.chat.title).getOrElse("???"), m)
        _ ← A.setText(a.id, text)
      } yield ArticleContext(a.id)
  }
}

object NewArticleScript {
  def apply()(implicit
    B: BotOps[Op],
              A:  ArticleOps[Op],
              Au: PersonOps[Op],
              Ao: AliasOps[Op]): Script = new NewArticleScript
}
