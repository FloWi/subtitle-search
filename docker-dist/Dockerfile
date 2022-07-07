FROM eclipse-temurin:8-jre-alpine

ADD ["./webapp", "/subtitle-search/webapp/"]
ADD ["./fileserver", "/subtitle-search/fileserver"]

ENV WEBAPP_FOLDER=/subtitle-search/webapp
ENV ASSETS_FOLDER=/assets

WORKDIR /subtitle-search
EXPOSE 8080
CMD ["java", "-jar", "/subtitle-search/fileserver/quickstart-assembly-0.0.1-SNAPSHOT.jar"]
