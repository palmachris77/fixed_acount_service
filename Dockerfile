FROM openjdk:8
VOLUME /tmp 
ADD ./target/fixed-term-account-0.0.1-SNAPSHOT.jar service-fixed-term-account.jar
ENTRYPOINT [ "java", "-jar","./service-fixed-term-account.jar" ]