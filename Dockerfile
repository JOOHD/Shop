# 빌드 스테이지
FROM eclipse-temurin:17-jdk as builder
WORKDIR /workspace/app
COPY . .
RUN ./gradlew build

# 실행 스테이지
FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY --from=builder /workspace/app/build/libs/*.jar /app/app.jar
CMD ["java", "-jar", "app.jar"]

