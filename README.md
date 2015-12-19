# Docgen

## About

Docgen is used to generate HTML pages from the two DocBook 5 "book" XML-s that
[the FreeMarker Manual](http://freemarker.org/docs/) and [the FreeMarker
homepage](http://freemarker.org/) is written in. As such, it mostly only
implements the subset of Docgen elements that we actually use, but otherwise
it tries to be reusable in other projects as well.

## Building

Before building for the first time:
* JDK 8 must be used (tried with 1.8.0_20)
* Apache Ant must be installed (tried with 1.8.1), and use JDK 8 for building.
* [Node.js](https://nodejs.org/) must be installed (tried with v0.12.4).
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

For some examples see:
* `src/test` and `test.xml` in this project
* `src/manual` in [the `freemarker` project](https://git-wip-us.apache.org/repos/asf/incubator-freemarker.git)
* `src/main/docgen` the [`site` project](https://git-wip-us.apache.org/repos/asf/incubator-freemarker-site.git)

For editing DocBook, we are using [XXE](http://www.xmlmind.com/xmleditor/)
with the `src/xxe-addon` installed. Unfortunatelly, the free edition of XXE
was discontinued long ago, but if there are problems with the old free
version, or you will do serious amount of editing, we can contact XMLmind for
more free licenses (in exchange for showing their logo on the generated pages).

## Building tricks

### Update dependencies

If you run into dependency errors, you may need to issue:

```sh
ant update-deps
```

### Compiling LESS and JS

To compile LESS and JS separately from the regular Ant build, run:

```sh
ant gulp
```

### Trying modifications without publishing

If you have modified `docgen` and want to try the new version then we don't
recomment doing that with Ant, because it's slow and also tricky if you need
to try it in a dependent project. Instead, see the IDE section later.

But if you must do it with Ant, issue:

```sh
ant publish-override
```

This will shadow the `docgen` artifact that comes from the Ivy repo on
[freemarker.org](http://freemarker.org). Then, in the dependent project
issue `ant update-deps` so that it picks up your version.

## Eclipse and other IDE-s

### Add project dependencies

You need to run this:

```sh
ant ide-dependencies
```

This will create an `ide-dependencies` directory that contains all the jars
that you have to add to the classpath in the IDE. Note that here we assume
that you have run the build or at least `ant update-deps` earlier.

You could also use IvyDE instead, with configuration name "IDE", but as the
dependencies hardly ever change, it's unnecessary.

### Try your modifications

If you want to try your modifications, let's say, by regenerating the
FreeMarker Manual, don't fiddle with Ant. Just create a Run Configuration in
Eclipse with main class `org.freemarker.docgen.TransformCommandLine`, then on
the "Arguments" tab enter "Program arguments" like:

    C:\work\freemarker\git\freemarker-2.3-gae\src\manual
    C:\work\freemarker\git\freemarker-2.3-gae\build\manual
    offline=true

To ease comparing outputs, you can set a fixed value for the last
modification time in the "VM arguments" box be entering something like:

    -Ddocgen.generationTime=2015-12-19T17:00Z

## Publishing a new Docgen version

As of this writing, the "docgen" dependency is get by `freemarker` and `site`from the Ivy repo on
`http://freemarker.org/repos/ivy/`. Those modifying docgen should upload the fresh `docgen.jar`
there occasonally. For that, first issue:

```sh
ant server-publish-last-build
```

This won't actually upload anything, but you will find the directory structure to upload
in the `build/dummy-server-ivy-repo` directory. See the README file in the
[site Git repo](https://git-wip-us.apache.org/repos/asf/incubator-freemarker-site.git)
on how to upload content to the FreeMarker homepage!

    
## Icon Font Attribution

The icon font in this project was built using [IcoMoon](https://icomoon.io/)
and contains selected icons from:

* [Entypo](http://www.entypo.com/) by [Daniel Bruce](http://www.danielbruce.se/)
* [Font Awesome](http://fontawesome.io) by Dave Gandy.
* [Material Design Icons](https://github.com/google/material-design-icons) by Google
