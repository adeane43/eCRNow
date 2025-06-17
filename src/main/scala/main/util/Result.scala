package main.util

/** 
 * Result type representing either a success or failure.
 * Alias for [[Either[FailureType, SuccessType]]].
 */
type Result[FailureType, SuccessType] = Either[FailureType, SuccessType]

/** Creates a successful result
 * @param value The success value
 * @tparam A The success type
 * @tparam B The failure type
 */
def Success[A, B](value: B): Result[A, B] = Right(value)

/** Creates a failure result
 *
 * @param value The error value
 * @tparam A The success type
 * @tparam B The failure type
 */
def Failure[A, B](value: A): Result[A, B] = Left(value)
