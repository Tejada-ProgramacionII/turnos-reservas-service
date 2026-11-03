# Etapa de compilación: JDK completo y el wrapper de Maven. Los tests corren en el CI.
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
# Primero solo el pom: las dependencias quedan en caché mientras no cambie.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B dependency:go-offline
COPY src/ src/
RUN ./mvnw -B -DskipTests package

# Etapa de ejecución: solo el JRE y el jar, con un usuario sin privilegios.
FROM eclipse-temurin:25-jre
RUN useradd --system --no-create-home turnos
WORKDIR /app
COPY --from=build /app/target/turnos-reservas-service-*.jar app.jar
USER turnos
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
