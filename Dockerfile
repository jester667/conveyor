FROM openjdk:11
ARG JAR_FILE=build/libs/conveor-0.0.1-SNAPSHOT.jar
WORKDIR /opt/app
COPY ${JAR_FILE} conveyor.jar
ENTRYPOINT ["java", "-jar", "conveyor.jar"]