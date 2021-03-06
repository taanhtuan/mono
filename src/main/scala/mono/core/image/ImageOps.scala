package mono.core.image

import java.nio.file.Path

import cats.free.Free.inject
import cats.free.{ Free, Inject }

import scala.language.higherKinds

class ImageOps[F[_]](implicit I: Inject[ImageOp, F]) {
  def store(
    userId: Int, file: Path, caption: Option[String]
  ): Free[F, Either[String, Image]] =
    inject[ImageOp, F](StoreImage(userId, file, caption))

  def find(imageId: Int): Free[F, Option[Image]] =
    inject[ImageOp, F](FindImage(imageId))

  def getById(imageId: Int): Free[F, Image] =
    inject[ImageOp, F](GetImageById(imageId))

  def getByIds(imageIds: Seq[Int]): Free[F, Seq[Image]] =
    inject[ImageOp, F](GetImagesByIds(imageIds))

  def getFile(image: Image): Free[F, Path] =
    inject[ImageOp, F](GetImageFile(image))
}

object ImageOps {
  implicit def ops[F[_]](implicit I: Inject[ImageOp, F]): ImageOps[F] = new ImageOps[F]
}