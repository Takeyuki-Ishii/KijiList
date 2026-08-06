# --- ステージ1: アプリのビルド ---
FROM maven:3-eclipse-temurin-21 AS build
COPY . /app
WORKDIR /app
# 実行権限を付与してビルドを実行
RUN chmod +x mvnw && ./mvnw clean package -DskipTests

# --- ステージ2: アプリの実行 ---
FROM eclipse-temurin:21-jre
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar", "--spring.profiles.active=prod"]