# Docgen

## About

Docgen was originally written to generate HTML pages from the DocBook 5 XML
that [the FreeMarker Manual](http://freemarker.org/docs/) is written in. As
such, it's not a general purpose DocBook 5 to HTML converter, however, we
use it for other projects as well, so it's not entirely specialized either.

## Usage

Before building for the first time:
* JDK 8 must be installed (tried with 1.8.0_20)
* Apache Ant must be installed (tried with 1.8.1).
* [Node.js](https://nodejs.org/) must be installed (tried with v0.10.32).
* Create a `build.properties` file based on `build.properties.sample`
* Issue `npm install` from the project directory to install Node.js
  dependencies. This need to be repeated if you add new dependencies
  to `gulpfile.js`.

To build Docgen:

```sh
ant all
```

This will create `lib/docgen.jar` and `build/api`.

For documentation see `build/api/index.html`. Especially, read the
documentation of the `Transform` class there.

For some examples see `src/test` and `test.xml`, and of course, `src/manual`
in the [`freemarker` project][fmProj].

For editing DocBook, we are using [XXE](http://www.xmlmind.com/xmleditor/)
with the `src/xxe-addon` installed.

## Building tricks

If you run into dependency errors, you may need to issue:

```sh
ant update-deps
```

If you have modified `docgen`, and want to try the new version in the
[`freemarker` project][fmProj], you will have to issue:

```sh
ant publish-override
```

This will shadow the `docgen` artifact that comes from the Ivy repo on
[freemarker.org](http://freemarker.org). Then, in the `freemarker` project you
have to issue `ant update-deps` so that it picks up your version.

## Eclipse and other IDE-s

You need to run this:

```sh
ant ide-dependencies
```

This will create an `ide-dependencies` library that contains all the jars that
you have to add to the classpath in the IDE. Note that here we assume that you
have run the build or at least `ant update-deps` earlier.

You could also use IvyDE instead, with configuration "IDE", but as the
dependencies hardly ever change, it's unnecessary.

[fmProj]: https://github.com/freemarker/freemarker

## Icon Font Attribution

The icon font in this project was built using [IcoMoon](https://icomoon.io/)
and contains selected icons from:

* [Entypo](http://www.entypo.com/) by [Daniel Bruce](http://www.danielbruce.se/)
* [Font Awesome](http://fontawesome.io) by Dave Gandy.
* [Google's Material Design Icons](https://github.com/google/material-design-icons)
