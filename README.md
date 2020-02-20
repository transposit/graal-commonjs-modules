# CommonJS Modules Support for Graaljs

This library adds support for CommonJS modules (aka `require`) inside a Graaljs script engine. It is based on the specification for [NodeJS modules](https://nodejs.org/api/modules.html) and it supports loading modules from the `node_modules` folder just as Node does. Of course, it doesn't provide an implementation for Node's APIs, so any module that depends on those won't work.

It is somehow similar to [jvm-npm](https://github.com/nodyn/jvm-npm) which I used before, but it is 100% implemented in Java and supports loading files through other means than the filesystem; you only need to implement a simple interface and you should be good to go. Also, having the implementation in Java allows using it with a JS interpreter on which access to Java packages has been disabled for sandboxing purposes.

Out-of-the-box, the library supports loading modules from the filesystem and from Java resources.

# Using the library with maven

The jar isn't published publicly. Run `mvn package` to create the jar, then install the jar as a local repo by running:

```
mvn org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file \
  -Dfile=~/.m2/repository/transposit/graal-commonjs-modules/1.0.3/graal-commonjs-modules-1.0.3.jar \
  -DgroupId=transposit \
  -DartifactId=graal-commonjs-modules \
  -Dversion=1.0.3 \
  -Dpackaging=jar \
  -DlocalRepositoryPath=src/main/resources \
```

then add this repository definition to your `pom.xml`:

```xml
<repositories>
  <repository>
    <id>graal-commonjs-modules</id>
    <url>file://${basedir}/src/main/resources</url>
  </repository>
</repositories>
```

and reference the dependency as usual:

```xml
<dependency>
  <groupId>transposit</groupId>
  <artifactId>graal-commonjs-modules</artifactId>
  <version>1.0.3</version>
</dependency>
```

# Usage

Enabling `require` in an existing Nashorn interpreter can be done very easily:

```java
Context context = Context.create();
Require.enable(context, myRootFolder);
```

This will expose a new global `require` function at the engine scope. Any code that is then run using this engine can make use of `require`.

The second argument specifies the root `Folder` from which modules are made available. `Folder` is an interface exposing a few calls that need to be implemented by backing providers to enable loading files and accessing subfolders.

As of now, the library comes with built-in support for loading modules either through the filesystem or through Java resources.

## Loading modules from the filesystem

Use the `FilesystemFolder.create` method to create an implementation of `Folder` rooted at a particular location in the filesystem:

```java
FilesystemFolder rootFolder = FilesystemFolder.create(new File("/path/to/my/folder"), "UTF-8");
Require.enable(context, rootFolder);
```

You need to specify the encoding of the files. Most of the time UTF-8 will be a reasonable choice.

The resulting folder is rooted at the path you specified, and JavaScript code won't be able to "escape" that root by using `../../..`. In other words, it behaves as is the root folder was the root of the filesystem.


## Loading modules from Java resources

Use the `ResourceFolder.create` method to create an implementation of `Folder` backed by Java resources:

```java
ResourceFolder rootFolder = ResourceFolder.create(getClass().getClassLoader(), "graal/nashorn_modules/test1", "UTF-8");
Require.enable(engine, rootFolder);
```

As for `FilesystemFolder`, you need to specify the encoding for the files that are read.
