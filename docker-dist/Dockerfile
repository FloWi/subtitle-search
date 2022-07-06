FROM eclipse-temurin:8-jre-alpine
#COPY ["package.json", "package-lock.json*", "generate-listing.js", "server.js", "refresh-and-run.sh", "/subtitle-search/"]

COPY ["./", "/subtitle-search/"]

ENV WEBAPP_FOLDER=/subtitle-search/webapp
ENV ASSETS_FOLDER=/assets

WORKDIR /subtitle-search
EXPOSE 8080
CMD ["java", "-jar", "/subtitle-search/quickstart-assembly-0.0.1-SNAPSHOT.jar"]
