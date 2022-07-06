FROM ubuntu

#COPY ["package.json", "package-lock.json*", "generate-listing.js", "server.js", "refresh-and-run.sh", "/subtitle-search/"]

RUN apt-get update  \
  && apt-get install -y wget tree \
  && rm -rf /var/lib/apt/lists/* \
  && wget https://github.com/svenstaro/miniserve/releases/download/v0.20.0/miniserve-v0.20.0-x86_64-unknown-linux-musl -O /miniserve-linux \
  && chmod +x /miniserve-linux


#RUN npm install -y

#ENV NODE_ENV=production

COPY ["refresh-and-run.sh", "/subtitle-search/"]
COPY ["*.html", "*.css", "*.js*", "/subtitle-search/webapp/"]

RUN mkdir /subtitle-search/webapp/assets

WORKDIR /subtitle-search
EXPOSE 3000
CMD ["/subtitle-search/refresh-and-run.sh"]
