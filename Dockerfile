FROM maven:4.0.0-rc-5-eclipse-temurin-21
RUN mkdir fimas
WORKDIR fimas
COPY . .
RUN mvn package -Dmaven.test.skip=true
EXPOSE 8085
CMD ["java", "-jar", "target/fimas-0.0.1.jar"]