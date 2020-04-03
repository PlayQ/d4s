package d4s.env

import java.time.ZonedDateTime

import cats.Functor
import cats.syntax.functor._
import izumi.functional.bio.{BIO, BIOFunctor, Entropy2, F}
import izumi.functional.mono.Entropy
import izumi.fundamentals.platform.time.IzTime
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.derive.MkArbitrary
import org.scalacheck.rng.Seed
import org.scalacheck.{Arbitrary, Gen, ScalacheckShapeless}
import zio.IO

trait DynamoRnd extends ScalacheckShapeless {

  def arb[T: MkArbitrary]: Arbitrary[T] = MkArbitrary[T].arbitrary

  def random[Z: Arbitrary]: Z = arbitrary[Z].pureApply(Gen.Parameters.default, Seed.random())

  def randomIO[Z: Arbitrary]: IO[Nothing, Z]                 = IO.effectTotal(random[Z])
  def randomBIO[F[+_, +_]: BIO, Z: Arbitrary]: F[Nothing, Z] = F.sync(random[Z])

  def randomEntropy[F[_]: Entropy: Functor, Z](instance: Arbitrary[Z]): F[Z] =
    Entropy[F].nextLong().map(l => instance.arbitrary.pureApply(Gen.Parameters.default, Seed(l)))
  def randomEntropy[F[_]: Entropy: Functor, Z: Arbitrary]: F[Z] = randomEntropy(implicitly[Arbitrary[Z]])

  def randomEntropy2[F[+_, +_]: Entropy2: BIOFunctor, Z](instance: Arbitrary[Z]): F[Nothing, Z] = {
    import izumi.functional.bio.catz._
    randomEntropy(instance)
  }
  def randomEntropy2[F[+_, +_]: Entropy2: BIOFunctor, Z: Arbitrary]: F[Nothing, Z] = randomEntropy2(implicitly[Arbitrary[Z]])

  implicit def arbitraryZonedDateTime: Arbitrary[ZonedDateTime] = Arbitrary {
    for {
      year  <- Gen.choose(1900, 2100)
      month <- Gen.choose(1, 12)
      day   <- Gen.choose(1, 28)
      hour  <- Gen.choose(0, 23)
      min   <- Gen.choose(0, 59)
      sec   <- Gen.choose(0, 59)
    } yield {
      ZonedDateTime.of(year, month, day, hour, min, sec, 0, IzTime.TZ_UTC)
    }
  }

}
