# 1단계: Gradle로 빌드
FROM gradle:8.5-jdk21 AS build

# 작업 디렉토리 설정
WORKDIR /home/gradle/src

# Gradle 캐시 최적화를 위해 의존성 먼저 복사
COPY --chown=gradle:gradle build.gradle settings.gradle ./
COPY --chown=gradle:gradle gradle ./gradle

# 의존성 다운로드 (캐싱)
RUN gradle dependencies --no-daemon || true

# 소스 코드 복사
COPY --chown=gradle:gradle . .

# 빌드 (테스트 제외)
RUN gradle clean build --no-daemon -x test

# 2단계: 실행 이미지 생성
FROM eclipse-temurin:21-jre-jammy

# 작업 디렉토리
WORKDIR /app

# 빌드된 JAR 파일 복사
# build/libs 디렉토리의 jar 파일 (보통 프로젝트명-버전.jar)
COPY --from=build /home/gradle/src/build/libs/*.jar app.jar

# 메모리 최적화 (Render 512MB RAM 환경)
ENV JAVA_OPTS="-Xmx384m -Xms256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Djava.security.egd=file:/dev/./urandom"

# Render 기본 포트
EXPOSE 10000

# 애플리케이션 실행
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]