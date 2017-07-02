package mono.web

import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives._
import cats.free.Free
import cats.~>
import monix.cats._
import monix.eval.Task
import monix.execution.Scheduler
import mono.core.person.{ Person, PersonOps }
import mono.core.env.EnvOps

import scala.language.higherKinds

object WebTokenCheck {

  def checkToken[F[_]](action: String)(implicit Au: PersonOps[F], E: EnvOps[F], i: F ~> Task, s: Scheduler): Directive1[Person] =
    parameter('token).flatMap { token ⇒
      onSuccess((for {
        claim ← E.parseToken(token)
        authorOpt ← claim match {
          case Some(c) if c.issuer.contains("telegram") && c.audience.exists(_.contains(action)) && c.subject.isDefined ⇒
            Au.findByTelegramId(c.subject.get.toLong)
          case v ⇒
            Free.pure[F, Option[Person]](None)
        }
      } yield authorOpt).foldMap(i).runAsync).flatMap {
        case Some(a) ⇒
          provide(a)
        case None ⇒
          reject
      }
    }

}
