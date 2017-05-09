package ciris

import ciris.ConfigError.MissingKey
import org.scalacheck.Gen
import org.scalatest.prop.PropertyChecks
import org.scalatest.{Matchers, WordSpec}

import scala.util.Try

class PropertySpec extends WordSpec with Matchers with PropertyChecks {
  def mixedCase(string: String): Gen[String] = {
    (for {
      lowers ← Gen.listOfN(string.length, Gen.oneOf(true, false))
      (lower, char) ← lowers zip string
    } yield if (lower) char.toLower else char.toUpper).map(_.mkString)
  }

  def mixedCaseEnum[T](values: Array[T])(f: T ⇒ String): Gen[(T, String)] =
    for {
      value ← Gen.oneOf(values)
      valueString ← mixedCase(f(value))
    } yield (value, valueString)

  def fails[A](f: ⇒ A): Boolean = Try(f).isFailure

  def readValue[A](value: String)(implicit reader: ConfigReader[A]): Either[ConfigError, A] =
    reader.read("key")(sourceWith("key", value))

  def readNonExistingValue[A](implicit reader: ConfigReader[A]): Either[ConfigError, A] =
    reader.read("key")(new ConfigSource {
      override def read(key: String): Either[ConfigError, String] =
        Left(MissingKey(key, this))

      override def keyType: String = "test source"
    })

  def sourceWith(key: String, value: String): ConfigSource =
    new ConfigSource {
      override def read(readKey: String): Either[ConfigError, String] =
        Map(key → value).get(readKey).map(Right(_)).getOrElse(Left(MissingKey(readKey, this)))

      override def keyType: String = "test source"
    }

  def sourceWith(entries: (String, String)*): ConfigSource =
    new ConfigSource {
      override def read(key: String): Either[ConfigError, String] =
        entries.toMap.get(key).map(Right(_)).getOrElse(Left(MissingKey(key, this)))

      override def keyType: String = "test source"
    }
}