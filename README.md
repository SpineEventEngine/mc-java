# Spine Model Compiler for Java

[![Ubuntu build][ubuntu-build-badge]][gh-actions]
[![codecov.io](https://codecov.io/github/SpineEventEngine/mc-java/coverage.svg?branch=master)](https://codecov.io/github/SpineEventEngine/mc-java?branch=master) &nbsp;
[![license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)

[gh-actions]: https://github.com/SpineEventEngine/mc-java/actions
[ubuntu-build-badge]: https://github.com/SpineEventEngine/mc-java/actions/workflows/build-on-ubuntu.yml/badge.svg


This repository hosts the Java-specific parts of the Spine Model Compiler.

### Usage

To use the Java part of the Model Compiler in Gradle, 
declare the dependency in `buildscript` block:

```kotlin

buildscript {
    //...
    
    val mcJavaVersion = ...

    dependencies {
        classpath("io.spine.tools:spine-mc-java-plugins:${mcJavaVersion}:all")
    }
    // ...
}

//...

apply plugin: "io.spine.mc-java"

```

Then, configure the plugin in scope of the particular Gradle project:

```kotlin


modelCompiler {
    java {
        // Specify the options here.
    }
}
```

See [mc-java-protoc](./mc-java-protoc/README.md) documentation for more detail.

### Model Compilers for other languages:
* [JavaScript][mc-js]
* [Dart][mc-dart]

See the common parts of Model Compiler at [SpineEventEngine/model-compiler][model-compiler].

### Environment

The modules in this repository are built with Java 11.

### Performance testing

This repo includes the `BuildSpeed` submodule with the performance tests for the Spine tools.
The tests are executed in a GH Action. To run the tests locally, launch the `checkPerformance`
Gradle task. The task execution time will be printed to the console and 
the [journal file](./BuildSpeed/journal.log) will be updated.


[model-compiler]: https://github.com/SpineEventEngine/model-compiler
[mc-js]: https://github.com/SpineEventEngine/mc-js
[mc-dart]: https://github.com/SpineEventEngine/mc-dart
