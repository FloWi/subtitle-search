```shell

sbt assembly

export SERVER_HOST="0.0.0.0" \
export SERVER_PORT="8080" \
export ASSETS_FOLDER="webapp/assets_non_bundled/assets" \
export WEBAPP_FOLDER="webapp/target/scala-2.13/scalajs-bundler/main/dist/"

java -jar target/scala-2.13/quickstart-assembly-0.0.1-SNAPSHOT.jar

```
