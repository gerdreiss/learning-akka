package bank.http

import cats.data.ValidatedNel
import cats.implicits._

object Validation {

  // field must be present
  trait Required[A] extends (A => Boolean)
  // minimum value
  trait Min[A] extends ((A, Double) => Boolean) // for numerical fields

  // TC instances
  implicit val requiredString: Required[String] = _.nonEmpty
  implicit val minInt: Min[Int]                 = _ >= _
  implicit val minDouble: Min[Double]           = _ >= _

  // usage
  def required[A](value: A)(implicit R: Required[A]): Boolean         = R(value)
  def mininmum[A](value: A, min: Double)(implicit M: Min[A]): Boolean = M(value, min)

  // Validated
  type ValidationResult[A] = ValidatedNel[ValidationFailure, A]

  // Validation Failures
  trait ValidationFailure {
    def errorMessage: String
  }

  case class RequiredFieldMissing(field: String) extends ValidationFailure {
    def errorMessage: String = s"$field is required"
  }

  case class MinValueViolation(field: String, min: Double) extends ValidationFailure {
    def errorMessage: String = s"$field must be greater than $min"
  }

  case class NegativeValueViolation(field: String) extends ValidationFailure {
    def errorMessage: String = s"$field must be positive"
  }

  // "main" API
  def validateMin[A: Min](value: A, min: Double, fieldName: String): ValidationResult[A] =
    if (mininmum(value, min)) value.validNel
    else MinValueViolation(fieldName, min).invalidNel

  def validateRequired[A: Required](value: A, fieldName: String): ValidationResult[A] =
    if (required(value)) value.validNel
    else RequiredFieldMissing(fieldName).invalidNel

  trait Validator[A] {
    def validate(value: A): ValidationResult[A]
  }

  def validateEntity[A](value: A)(implicit V: Validator[A]): ValidationResult[A] =
    V.validate(value)

}
