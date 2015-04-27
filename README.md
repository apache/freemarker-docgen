# Docgen

## About

Docgen was originally written to generate HTML pages from the DocBook 5 XML
that [the FreeMarker Manual](http://freemarker.org/docs/) is written in. As
such, it's not a general purpose DocBook 5 to HTML converter, however, we
use it for other projects as well, so it's not entirely specialized either.

## Usage

Issue:

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

[fmProj]: https://github.com/freemarker/freemarker

### Compiling LESS and Minifying JS

```sh
ant gulp
```

Notes:
* To compile the docgen styles, you must have Node.js installed.
* Create a `build.properties` file and set `nodeJsCommand`. (See `build.properties.sample`)
