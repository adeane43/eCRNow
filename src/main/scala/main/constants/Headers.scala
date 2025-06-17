package main.constants

import kotlin.jvm.JvmField

/** Constants for HTTP headers used in the application */
object Headers {
  /** Request ID for request tracking */
  @JvmField final val X_REQUEST_ID = "X-Request-ID"
  /** Correlation ID for tracing requests across systems */
  @JvmField final val X_CORRELATION_ID = "X-Correlation-ID"
}