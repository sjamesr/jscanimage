jscanimage
==========

# Introduction

`jscanimage` is a command-line frontend to the SANE system implemented using
JFreeSane. It is intended mainly for exercising the functions of JFreeSane, but
you might find it useful as a standalone utility.

It has the same limitations as JFreeSane itself, i.e. that you must have a SANE
daemon running on a server.

# Building and running

```sh
$ git clone git@github.com:sjamesr/jscanimage.git
$ cd jscanimage
$ ./gradlew installDist
$ ./build/install/jscanimage/bin/jscanimage
```
