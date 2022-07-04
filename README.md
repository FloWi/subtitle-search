# Subtitle Search

A small webapp that helps you search in subtitles of videos.

![Screenshot](docs/screenshot.png)

## Prerequisites

You should make sure that the following components are pre-installed on your machine:

- [Node.js](https://nodejs.org/en/download/)

## Using the webapp

- Download the release from github
- extract archive into folder
- inside that folder create an assets folder where you put in the mp4- and vtt-files that you need to download from coursera
  - the app expects to have _one_ mp4-file next to _one_ vtt-file in a subfolder.
- run the command below to create a listing of the contents (since the webapp running in the browser cannot list all files in a directory)
- you can now serve the folder as static files (e.g. `npx serve`)

This is how your folder should look like
`tree -l .`

```text
.
├── assets
│     ├── listing.json
│     └── recordings
│         ├── week 1
│         │     ├── 01 What is machine learning
│         │     │     ├── index.mp4
│         │     │     ├── subtitle.txt
│         │     │     └── subtitles-en.vtt
│         │     ├── 02 Supervised learning part 1
│         │     │     ├── index.mp4
│         │     │     ├── subtitle.txt
│         │     │     └── subtitles-en.vtt
│         ├── week 2
│         │     ├── 01 Multiple Features
│         │     │     ├── index.mp4
│         │     │     ├── subtitle.txt
│         │     │     └── subtitles-en.vtt
│         │     ├── 02 Vectorization part 1
│         │     │     ├── index.mp4
│         │     │     ├── subtitle.txt
│         │     │     └── subtitles-en.vtt
├── index.html
├── main-4d68bdac8ec2364a3a59-hashed.js
├── main-4d68bdac8ec2364a3a59-hashed.js.map
└── style.css
```

### workflow

```shell
# extract files from release archive

tar xzf ~/Downloads/subtitle-search-0.0.1.tar.gz
mkdir assets

# copy/move/symlink downloaded videos in assets folder
# e.g.
ln -s ~/Downloads/ml-course/recordings assets/recordings

# create directory listing using npm package `directory-tree` that also comes with a cli tool
(cd assets; npx directory-tree \
--path . \
--attributes size,type,extension \
--exclude "/node_modules|\.DS_Store|listing\.json/" \
--pretty \
--output ./listing.json)

# start http server for static files
npx serve
```

Whenever you change the directory structure (e.g. you downloaded new videos) you need to execute the `npx directory-tree ...` command again to re-generate `assets/listing.json`.

## Working in dev mode

### Prerequisites

You should make sure that the following components are pre-installed on your machine:

- [sbt](https://www.scala-sbt.org/download.html)
- [Node.js](https://nodejs.org/en/download/)
- [Yarn](https://yarnpkg.com/en/docs/install)

Run

```sh
sbt dev
```

Then open `http://localhost:12345` in your browser.

This sbt-task will start webpack dev server, compile your code each time it changes and auto-reload the page.  
Webpack dev server will stop automatically when you stop the `dev` task
(e.g by hitting `Enter` in the sbt shell while you are in `dev` watch mode).

If you existed ungracefully and your webpack dev server is still open (check with `ps -aef | grep -v grep | grep webpack`),
you can close it by running `fastOptJS::stopWebpackDevServer` in sbt.
