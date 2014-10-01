# Docgen

## What is this?

Docgen is originally the sub-project of FreeMarker that is used to generate
HTML pages from the DocBook 5 XML that the FreeMarker Manual is written in. As
such, it's not a general purpose DocBook 5 to HTML converter, however, we use
it for other projects as well, so it's not entirely specialized either.

## How to use it?

Issue:

```shell
ant all
```

This will create lib/docgen.jar and build/api. Then, for more documentation see:

```
build/api/index.html
```

Especially, read the documentation of "Transform" there.

For some examples see:

```
src/test
```

and

```
test.xml
```

For editing DocBook-s we recomend XXE with the `src/xxe-addion` installed.
