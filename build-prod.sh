sbt prod

rm -r docker-dist/fileserver
rm -r docker-dist/webapp

mkdir -p docker-dist/webapp
mkdir -p docker-dist/fileserver

cp webapp/target/scala-2.13/scalajs-bundler/main/dist/* docker-dist/webapp
cp file-server/target/scala-2.13/quickstart-assembly-0.0.1-SNAPSHOT.jar docker-dist/fileserver

#cd docker-dist || exit
#docker build --tag subtitle-search .
