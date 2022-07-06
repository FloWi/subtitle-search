cd docker-dist || exit

cp ../webapp/target/scala-2.13/scalajs-bundler/main/dist/* .

docker build --tag subtitle-search .
