package mono.web

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import cats.~>
import monix.eval.Task
import mono.article.ArticleOps
import mono.author.AuthorOps
import akka.http.scaladsl.server.Directives._
import mono.alias.AliasOps

import scala.language.higherKinds

class WebApp[F[_]](implicit A: ArticleOps[F], Au: AuthorOps[F], As: AliasOps[F]) extends Web[F] {

  override def route(implicit i: F ~> Task): Route =
    new WebArticle[F].route ~
      new WebAlias[F].route ~
      complete("404")

  def run(host: String = "localhost", port: Int = 9000)(implicit i: F ~> Task, system: ActorSystem, mat: Materializer) =
    Http().bindAndHandle(route, "localhost", 9000)

}
