FROM openjdk:17-alpine

RUN apk --no-cache add sbt && sbt --version

WORKDIR /java-app

COPY pom.xml .
COPY src ./src

# Package the Spring Boot application, skipping tests
RUN sbt build -DskipTests

# Define the command to run your application
CMD ["java", "-jar", "./target/ecr-now.war"]
