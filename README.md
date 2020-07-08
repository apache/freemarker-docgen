# Docgen

## About

Docgen is used to generate HTML pages from the two DocBook 5 "book" XML-s that
[the FreeMarker Manual](http://freemarker.org/docs/) and [the FreeMarker
homepage](http://freemarker.org/) is written in. As such, it mostly only
implements the subset of Docgen elements that we actually use, but otherwise
it tries to be reusable in other projects as well.

## Building

Before building for the first time:
* JDK 8 must be used (tried with 1.8.0_212)
* Apache Maven must be installed (tried with 3.6.1), and use JDK 8 for building.
* [Node.js](https://nodejs.org/) must be installed (tried with v10.16.2).
* Create a `build.properties` file based on `build.properties.sample`
* Issue `npm install` from the project directory to install Node.js
  dependencies. This need to be repeated if you add new dependencies
  to `gulpfile.js`.

Possible node.js related problems and solutions:
* "Error: ENOENT, stat <someDirectoryHere>": create that directory manually,
  then retry.
* If the system doesn't find `npm`: Open a new terminal (command window) so
  that it pick up the "path" environment variable changes. Adjust it if
  necessary.
* If the build has once worked, but now keeps failing due to some missing
  modules, or anything strange, delete the "node_modules" directory, and
  issue `npm install` to recreate it.
  
To build Docgen:

```sh
mvn install
```

For some examples see:
* `src/test` and `test.xml` in this project
* `src/manual` in [the `freemarker` project](https://git-wip-us.apache.org/repos/asf/incubator-freemarker.git)
* `src/main/docgen` the [`site` project](https://git-wip-us.apache.org/repos/asf/incubator-freemarker-site.git)

For editing DocBook, we are using [XXE](http://www.xmlmind.com/xmleditor/)
with the `src/xxe-addon` installed.

### Try your modifications

If you want to try your modifications, let's say, by regenerating the
FreeMarker Manual, just create a Run Configuration in you IDE, with main class
`org.freemarker.docgen.TransformCommandLine`, then specify these command line
arguments:

    C:\work\freemarker\git\freemarker-2.3-gae\src\manual
    C:\work\freemarker\git\freemarker-2.3-gae\build\manual
    offline=true

To ease comparing outputs, you can set a fixed value for the last
modification with a java argument like this:

    -Ddocgen.generationTime=2020-07-15T17:00Z

### Compiling LESS and JS

This happens automatically during build, in the `generate-resources`.
The generated output is in `target\resources-gulp`.

## Publishing a new Docgen version

TODO

## Icon Font Attribution

The icon font in this project was built using [IcoMoon](https://icomoon.io/)
and contains selected icons from:

* [Entypo](http://www.entypo.com/) by [Daniel Bruce](http://www.danielbruce.se/)
* [Font Awesome](http://fontawesome.io) by Dave Gandy.
* [Material Design Icons](https://github.com/google/material-design-icons) by Google
