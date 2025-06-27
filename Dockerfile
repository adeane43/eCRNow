FROM openjdk:17-alpine

RUN apk --no-cache add sbt && sbt --version

WORKDIR /java-app

COPY pom.xml .
COPY src ./src

# Define the command to run your application
CMD ["sbt", "clean install run", "./target/ecr-now.war"]
