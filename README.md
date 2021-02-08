# Docgen

[![Build Status](https://travis-ci.com/apache/freemarker-docgen.svg?branch=master)](https://travis-ci.com/github/apache/freemarker-docgen)


## About

Apache FreeMarker Docgen is an *internal* project under the Apache FreeMarker TLP,
used for generating [the FreeMarker Manual](https://freemarker.apache.org/docs/),
[the FreeMarker homepage](https://freemarker.apache.org), and maybe some more.

Docgen generates static web pages from a DocBook 5 "book" XML. But, it
only implements the small subset of DocBook elements that we actually use, and
it *has no backward compatibility guarantees*.


### Usage

For some examples see:
* The [`freemarker-site` project](https://github.com/apache/freemarker-site)
* [FreeMarker Manual source code](https://github.com/apache/freemarker/tree/2.3-gae/src/manual/en_US)
* `legacy-tests` in this project

For editing DocBook, we are using [XXE](http://www.xmlmind.com/xmleditor/),
with the `xxe-addon` installed, that you can find in this project.


## Building

These tools must be installed:
* JDK 8, tested with Oracle 1.8.0_212
* Apache Maven, tested with 3.6.1
* [Node.js](https://nodejs.org/), tested with 12.18.2, 14.x.x maybe won't work.
  (Node.js is only used to generate static content while building Docgen itself.) 

To build, ensure that `npm` (from Node.js) is in the path, then in the top project directory
(`freemarker-docgen`) issue this:

   ```mvn install```



### Node.js troubleshooting

Possible node.js related problems and solutions:
* "Error: ENOENT, stat <someDirectoryHere>": create that directory manually,
  then retry.
* If the system doesn't find `npm`: Open a new terminal (command window) so
  that it picks up the `PATH` environment variable changes. Adjust it if
  necessary.
* If the build has once worked, but now keeps failing due to some missing
  modules, or anything strange, delete the "node_modules" directory, and
  issue `npm install` to recreate it.


### Running Docgen from your IDE

If you develop/debug Docgen, it's convenient to launch it from your IDE.
As an example, let's generate the  FreeMarker Manual. For that, create a
Run Configuration in you IDE, with main class
`org.freemarker.docgen.cli.Main`, and these command line  arguments
(replace `<FREEMARKER_PROJECT_DIR>` with the actual directory):

    <FREEMARKER_PROJECT_DIR>\src\manual
    <FREEMARKER_PROJECT_DIR>\build\manual
    offline=true

To ease comparing the outputs of different runs, you can set a fixed value
for the last modification with a java argument like this:

    -Ddocgen.generationTime=2020-07-15T17:00Z

### Compiling LESS and JS

This happens automatically during build, in the `generate-resources` Maven phase.
The generated output is in `target\resources-gulp`, which will be included in
the core jar artifact.

## Releasing a new Docgen version

\[TODO] Standard ASF release procedure (staging, voting, etc.), so that we can release
to the Maven Central. Not advertised, no announcements, no backward compatibility
promises, but makes building our dependent projects easier.

## Icon Font Attribution

The icon font in this project was built using [IcoMoon](https://icomoon.io/)
and contains selected icons from:

* [Entypo](http://www.entypo.com/) by [Daniel Bruce](http://www.danielbruce.se/)
* [Font Awesome](http://fontawesome.io) by Dave Gandy.
* [Material Design Icons](https://github.com/google/material-design-icons) by Google
