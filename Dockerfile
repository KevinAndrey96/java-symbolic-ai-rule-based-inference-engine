FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
COPY examples ./examples
RUN mvn -B -q package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /build/target/inference-engine.jar ./inference-engine.jar
COPY examples ./examples
USER app
ENTRYPOINT ["java", "-jar", "inference-engine.jar"]
CMD ["--example", "animals", "--goal", "cheetah"]
