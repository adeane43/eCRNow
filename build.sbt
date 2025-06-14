name := "ecr-now"
version := "0.0.1-SNAPSHOT"
organization := "ecrnow"
description := "FHIR App to perform electronic case reporting (ecr) by converting to CDA Format"

scalaVersion := "3.7.1"
ThisBuild / scalaVersion := "3.7.1"
javacOptions ++= Seq("--release", "24")

// Source directories for mixed Java/Scala
Compile / unmanagedSourceDirectories ++= Seq(
  baseDirectory.value / "src" / "main" / "java",
  baseDirectory.value / "src" / "main" / "scala"
)

Test / unmanagedSourceDirectories ++= Seq(
  baseDirectory.value / "src" / "test" / "java",
  baseDirectory.value / "src" / "test" / "scala"
)

// Library dependencies
libraryDependencies ++= Seq(
  // Scala
  "org.scala-lang" %% "scala3-library" % "3.7.1",
  // Spring Boot
  "org.springframework.boot" % "spring-boot-starter-web" % "3.5.0",
  "org.springframework.boot" % "spring-boot-configuration-processor" % "3.5.0" % "optional",
  "org.springframework.boot" % "spring-boot-starter-data-jpa" % "3.5.0",
  "org.springframework.boot" % "spring-boot-starter-actuator" % "3.5.0",
  "org.springframework.boot" % "spring-boot-starter-data-rest" % "3.5.0",
  "org.springframework.boot" % "spring-boot-starter-security" % "3.5.0",
  // Databases
  "com.microsoft.sqlserver" % "mssql-jdbc" % "12.10.0.jre11",
  "org.postgresql" % "postgresql" % "42.7.7",
  // Scheduler
  "com.github.kagkarlsson" % "db-scheduler-spring-boot-starter" % "15.6.0",
  // Interceptor
  "javax.interceptor" % "javax.interceptor-api" % "1.2.2",
  // Lombok (optional, for Java)
  "org.projectlombok" % "lombok" % "1.18.38" % "provided",
  // JSON
  "com.jayway.jsonpath" % "json-path" % "2.9.0",
  "io.jsonwebtoken" % "jjwt-api" % "0.12.6",
  "io.jsonwebtoken" % "jjwt-impl" % "0.12.6" % "runtime",
  "io.jsonwebtoken" % "jjwt-jackson" % "0.12.6" % "runtime",
  // FHIR
  "ca.uhn.hapi.fhir" % "hapi-fhir-base" % "5.4.0",
  "ca.uhn.hapi.fhir" % "hapi-fhir-structures-r4" % "5.4.0",
  "ca.uhn.hapi.fhir" % "hapi-fhir-structures-dstu2" % "5.4.0",
  "ca.uhn.hapi.fhir" % "hapi-fhir-validation-resources-r4" % "5.4.0",
  "ca.uhn.hapi.fhir" % "hapi-fhir-client" % "5.4.0",
  // CQL Evaluator
  "org.opencds.cqf.cql" % "evaluator.spring" % "2.6.0" exclude("javax", "javaee-api"),
  "org.opencds.cqf.cql" % "evaluator.library" % "2.6.0" exclude("javax", "javaee-api"),
  "org.opencds.cqf.cql" % "evaluator.expression" % "2.6.0" exclude("javax", "javaee-api"),
  "org.opencds.cqf.cql" % "evaluator.measure-hapi" % "2.6.0" exclude("javax", "javaee-api"),
  // JSON
  "org.json" % "json" % "20250517",
  "com.fasterxml.jackson.core" % "jackson-core" % "2.19.1",
  "com.fasterxml.jackson.core" % "jackson-annotations" % "2.19.1",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.19.0",
  "com.google.code.gson" % "gson" % "2.13.1",
  // Tuples
  "org.javatuples" % "javatuples" % "1.2",
  // Commons
  "org.apache.commons" % "commons-collections4" % "4.5.0",
  "commons-io" % "commons-io" % "2.19.0",
  "commons-codec" % "commons-codec" % "1.18.0",
  // Schematron
  "com.helger" % "ph-schematron-validator" % "5.6.5",
  // Mail
  "javax.mail" % "javax.mail-api" % "1.6.2",
  "com.sun.mail" % "javax.mail" % "1.6.2",
  "com.sun.mail" % "dsn" % "2.0.1",
  // HTTP
  "org.apache.httpcomponents" % "httpcore" % "4.4.16",
  "org.apache.httpcomponents" % "httpclient" % "4.5.14",
  "org.apache.httpcomponents" % "httpmime" % "4.5.14",
  "com.squareup.okhttp3" % "okhttp" % "4.12.0",
  // Logging
  "ch.qos.logback.contrib" % "logback-json-classic" % "0.1.5",
  "ch.qos.logback.contrib" % "logback-jackson" % "0.1.5",
  // Retry
  "org.springframework.retry" % "spring-retry" % "2.0.12",
  // OpenAPI
  "org.springdoc" % "springdoc-openapi-ui" % "1.8.0",
  // Test
  "org.powermock" % "powermock-api-mockito2" % "2.0.9" % Test,
  "org.powermock" % "powermock-module-junit4" % "2.0.9" % Test,
  "org.mockito" % "mockito-core" % "5.18.0" % Test,
  "org.springframework.boot" % "spring-boot-starter-test" % "3.5.0" % Test,
  "com.h2database" % "h2" % "2.3.232" % Test,
  "com.github.tomakehurst" % "wiremock-jre8" % "3.0.1" % Test,
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % "2.19.0",
  "org.hibernate" % "hibernate-core" % "5.4.0.Final"
)

// Main class for Spring Boot
mainClass in Compile := Some("main.EcrApp")

// Optional: Custom resolvers if needed
resolvers ++= Seq(
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
  "Sonatype OSS Public" at "https://oss.sonatype.org/content/groups/public/"
)
