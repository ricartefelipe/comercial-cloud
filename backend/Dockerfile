# eclipse-temurin:21-jdk
FROM eclipse-temurin@sha256:ebfbb18d11ff163ee83b21258acf12248af48d22a0fdfdc68fa1a1516c05976a AS build
WORKDIR /app
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline
COPY src ./src
RUN ./mvnw -B package -DskipTests

# eclipse-temurin:21-jre
FROM eclipse-temurin@sha256:0b99b3376fbb50dbf9c0af8535fdd5b8eb0d3fdfc8b1e4967f383b6919f6901a
# Correções de segurança da base publicadas depois da imagem entram em cada build.
RUN apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get upgrade -y --no-install-recommends \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /app/target/quarkus-app/lib/ /app/lib/
COPY --from=build /app/target/quarkus-app/*.jar /app/
COPY --from=build /app/target/quarkus-app/app/ /app/app/
COPY --from=build /app/target/quarkus-app/quarkus/ /app/quarkus/

EXPOSE 8080
USER 1000
ENTRYPOINT ["java", "-jar", "quarkus-run.jar"]
