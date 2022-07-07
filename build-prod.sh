sbt prod

rm docker-dist/*.jar
rm -r docker-dist/webapp

mkdir -p docker-dist/webapp
cp webapp/target/scala-2.13/scalajs-bundler/main/dist/* docker-dist/webapp
cp file-server/target/scala-2.13/quickstart-assembly-0.0.1-SNAPSHOT.jar docker-dist/

#cd docker-dist || exit
#docker build --tag subtitle-search .
