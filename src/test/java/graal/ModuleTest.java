/*
 * Copyright 2018 Transposit Corporation. All Rights Reserved.
 */

package graal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ModuleTest {
  @Mock Folder root;
  @Mock Folder rootnm;
  @Mock Folder sub1;
  @Mock Folder sub1nm;
  @Mock Folder sub1sub1;
  @Mock Folder nmsub1;

  Context context;
  Module require;

  @Before
  public void before() throws Throwable {
    when(root.getPath()).thenReturn("/");
    when(root.getFolder("node_modules")).thenReturn(rootnm);
    when(root.getFolder("sub1")).thenReturn(sub1);
    when(root.getFile("file1.js")).thenReturn("exports.file1 = 'file1';");
    when(root.getFile("file2.json")).thenReturn("{ \"file2\": \"file2\" }");
    when(rootnm.getPath()).thenReturn("/node_modules/");
    when(rootnm.getParent()).thenReturn(root);
    when(rootnm.getFile("nmfile1.js")).thenReturn("exports.nmfile1 = 'nmfile1';");
    when(rootnm.getFolder("nmsub1")).thenReturn(nmsub1);
    when(nmsub1.getFile("nmsub1file1.js")).thenReturn("exports.nmsub1file1 = 'nmsub1file1';");
    when(nmsub1.getParent()).thenReturn(rootnm);
    when(sub1.getPath()).thenReturn("/sub1/");
    when(sub1.getParent()).thenReturn(root);
    when(sub1.getFolder("sub1")).thenReturn(sub1sub1);
    when(sub1.getFolder("node_modules")).thenReturn(sub1nm);
    when(sub1.getFile("sub1file1.js")).thenReturn("exports.sub1file1 = 'sub1file1';");
    when(sub1nm.getPath()).thenReturn("/sub1/node_modules/");
    //    when(sub1nm.getParent()).thenReturn(sub1);
    when(sub1nm.getFile("sub1nmfile1.js")).thenReturn("exports.sub1nmfile1 = 'sub1nmfile1';");
    when(sub1sub1.getPath()).thenReturn("/sub1/sub1/");
    //    when(sub1sub1.getParent()).thenReturn(sub1);
    when(sub1sub1.getFile("sub1sub1file1.js"))
        .thenReturn("exports.sub1sub1file1 = 'sub1sub1file1';");

    context = Context.create();
    require = Require.enable(context, root);
  }

  @Test
  public void itCanLoadSimpleModules() throws Throwable {
    assertEquals("file1", require.require("./file1.js").getMember("file1").asString());
  }

  // not true anymore
  //  @Test
  //  public void itCanEnableRequireInDifferentBindingsOnTheSameEngine() throws Throwable {
  //    context = Context.create();
  //    Value bindings1 = context.getBindings("js");
  //    Value bindings2 = context.getBindings("js");
  //
  //    Require.enable(context, root, bindings1);
  //
  //    assertNull(context.getBindings("js").getMember("require"));
  //    assertNotNull(bindings1.getMember("require"));
  //    assertTrue(bindings2.getMember("require").isNull());
  //    assertEquals("file1", context.eval("js", "require('./file1')").getMember("file1"));
  //
  //    try {
  //      context.eval("js", "require('./file1')");
  //      fail();
  //    } catch (PolyglotException ignored) {
  //    }
  //
  //    Require.enable(context, root, bindings2);
  //    assertNull(context.getBindings("js").getMember("require"));
  //    assertNotNull(bindings2.getMember("require"));
  //    assertEquals("file1", context.eval("js", "require('./file1')").getMember("file1"));
  //  }

  @Test
  public void itCanLoadSimpleJsonModules() throws Throwable {
    assertEquals("file2", require.require("./file2.json").getMember("file2").asString());
  }

  @Test
  public void itCanLoadModulesFromSubFolders() throws Throwable {
    assertEquals(
        "sub1file1", require.require("./sub1/sub1file1.js").getMember("sub1file1").asString());
  }

  @Test
  public void itCanLoadModulesFromSubFoldersInNodeModules() throws Throwable {
    assertEquals(
        "nmsub1file1",
        require.require("nmsub1/nmsub1file1.js").getMember("nmsub1file1").asString());
  }

  @Test
  public void itCanLoadModulesFromSubSubFolders() throws Throwable {
    assertEquals(
        "sub1sub1file1",
        require.require("./sub1/sub1/sub1sub1file1.js").getMember("sub1sub1file1").asString());
  }

  @Test
  public void itCanLoadModulesFromParentFolders() throws Throwable {
    when(sub1.getFile("sub1file1.js")).thenReturn("exports.sub1file1 = require('../file1').file1;");
    assertEquals("file1", require.require("./sub1/sub1file1.js").getMember("sub1file1").asString());
  }

  @Test
  public void itCanGoUpAndDownInFolders() throws Throwable {
    when(sub1.getFile("sub1file1.js")).thenReturn("exports.sub1file1 = require('../file1').file1;");
    assertEquals(
        "file1", require.require("./sub1/../sub1/sub1file1.js").getMember("sub1file1").asString());
  }

  @Test
  public void itCanGoUpAndDownInNodeModulesFolders() throws Throwable {
    assertEquals(
        "nmsub1file1",
        require.require("nmsub1/../nmsub1/nmsub1file1.js").getMember("nmsub1file1").asString());
  }

  @Test
  public void itCanLoadModulesSpecifyingOnlyTheFolderWhenPackageJsonHasAMainFile()
      throws Throwable {
    Folder dir = mock(Folder.class);
    when(dir.getFile("package.json")).thenReturn("{ \"main\": \"foo.js\" }");
    when(dir.getFile("foo.js")).thenReturn("exports.foo = 'foo';");
    when(root.getFolder("dir")).thenReturn(dir);
    assertEquals("foo", require.require("./dir").getMember("foo").asString());
  }

  @Test
  public void
      itCanLoadModulesSpecifyingOnlyTheFolderWhenPackageJsonHasAMainFilePointingToAFileInSubDirectory()
          throws Throwable {
    Folder dir = mock(Folder.class);
    Folder lib = mock(Folder.class);
    when(dir.getFile("package.json")).thenReturn("{ \"main\": \"lib/foo.js\" }");
    when(dir.getFolder("lib")).thenReturn(lib);
    when(lib.getFile("foo.js")).thenReturn("exports.foo = 'foo';");
    when(root.getFolder("dir")).thenReturn(dir);
    assertEquals("foo", require.require("./dir").getMember("foo").asString());
  }

  @Test
  public void
      itCanLoadModulesSpecifyingOnlyTheFolderWhenPackageJsonHasAMainFilePointingToASubDirectory()
          throws Throwable {
    Folder dir = mock(Folder.class);
    Folder lib = mock(Folder.class);
    when(root.getFolder("dir")).thenReturn(dir);
    when(dir.getFolder("lib")).thenReturn(lib);
    when(dir.getFile("package.json")).thenReturn("{\"main\": \"./lib\"}");
    when(lib.getFile("index.js")).thenReturn("exports.foo = 'foo';");
    assertEquals("foo", require.require("./dir").getMember("foo").asString());
  }

  @Test
  public void
      itCanLoadModulesSpecifyingOnlyTheFolderWhenPackageJsonHasAMainFilePointingToAFileInSubDirectoryReferencingOtherFilesInThisDirectory()
          throws Throwable {
    Folder dir = mock(Folder.class);
    Folder lib = mock(Folder.class);
    when(dir.getFile("package.json")).thenReturn("{ \"main\": \"lib/foo.js\" }");
    when(dir.getFolder("lib")).thenReturn(lib);
    when(lib.getFile("foo.js")).thenReturn("exports.bar = require('./bar');");
    when(lib.getFile("bar.js")).thenReturn("exports.bar = 'bar';");
    when(root.getFolder("dir")).thenReturn(dir);
    assertEquals("bar", require.require("./dir").getMember("bar").getMember("bar").asString());
  }

  @Test
  public void itCanLoadModulesSpecifyingOnlyTheFolderWhenIndexJsIsPresent() throws Throwable {
    Folder dir = mock(Folder.class);
    when(dir.getFile("index.js")).thenReturn("exports.foo = 'foo';");
    when(root.getFolder("dir")).thenReturn(dir);
    assertEquals("foo", require.require("./dir").getMember("foo").asString());
  }

  @Test
  public void itCanLoadModulesSpecifyingOnlyTheFolderWhenIndexJsIsPresentEvenIfPackageJsonExists()
      throws Throwable {
    Folder dir = mock(Folder.class);
    when(dir.getFile("package.json")).thenReturn("{ }");
    when(dir.getFile("index.js")).thenReturn("exports.foo = 'foo';");
    when(root.getFolder("dir")).thenReturn(dir);
    assertEquals("foo", require.require("./dir").getMember("foo").asString());
  }

  @Test
  public void itUsesNodeModulesOnlyForNonPrefixedNames() throws Throwable {
    assertEquals("nmfile1", require.require("nmfile1").getMember("nmfile1").asString());
  }

  @Test
  public void itFallbacksToNodeModulesWhenUsingPrefixedName() throws Throwable {
    assertEquals("nmfile1", require.require("./nmfile1").getMember("nmfile1").asString());
  }

  @Test(expected = PolyglotException.class)
  public void itDoesNotUseModulesOutsideOfNodeModulesForNonPrefixedNames() throws Throwable {
    require.require("file1.js");
  }

  @Test
  public void itUsesNodeModulesFromSubFolderForSubRequiresFromModuleInSubFolder() throws Throwable {
    when(sub1.getFile("sub1file1.js"))
        .thenReturn("exports.sub1nmfile1 = require('sub1nmfile1').sub1nmfile1;");
    assertEquals(
        "sub1nmfile1", require.require("./sub1/sub1file1").getMember("sub1nmfile1").asString());
  }

  @Test
  public void itLooksAtParentFoldersWhenTryingToResolveFromNodeModules() throws Throwable {
    when(sub1.getFile("sub1file1.js")).thenReturn("exports.nmfile1 = require('nmfile1').nmfile1;");
    assertEquals("nmfile1", require.require("./sub1/sub1file1").getMember("nmfile1").asString());
  }

  @Test
  public void itCanUseDotToReferenceToTheCurrentFolder() throws Throwable {
    assertEquals("file1", require.require("./file1.js").getMember("file1").asString());
  }

  @Test
  public void itCanUseDotAndDoubleDotsToGoBackAndForward() throws Throwable {
    assertEquals(
        "file1", require.require("./sub1/.././sub1/../file1.js").getMember("file1").asString());
  }

  @Test
  public void thePathOfModulesContainsNoDots() throws Throwable {
    when(root.getFile("file1.js")).thenReturn("exports.path = module.filename");
    assertEquals(
        "/file1.js", require.require("./sub1/.././sub1/../file1.js").getMember("path").asString());
  }

  @Test
  public void itCanLoadModuleIfTheExtensionIsOmitted() throws Throwable {
    assertEquals("file1", require.require("./file1").getMember("file1").asString());
  }

  @Test(expected = PolyglotException.class)
  public void itThrowsAnExceptionIfFileDoesNotExists() throws Throwable {
    require.require("./invalid");
  }

  @Test(expected = PolyglotException.class)
  public void itThrowsAnExceptionIfSubFileDoesNotExists() throws Throwable {
    require.require("./sub1/invalid");
  }

  @Test(expected = PolyglotException.class)
  public void itThrowsEnExceptionIfFolderDoesNotExists() throws Throwable {
    require.require("./invalid/file1.js");
  }

  @Test(expected = PolyglotException.class)
  public void itThrowsEnExceptionIfSubFolderDoesNotExists() throws Throwable {
    require.require("./sub1/invalid/file1.js");
  }

  @Test(expected = PolyglotException.class)
  public void itThrowsAnExceptionIfTryingToGoAboveTheTopLevelFolder() throws Throwable {
    // We need two ".." because otherwise the resolving attempts to load from "node_modules" and
    // ".." validly points to the root folder there.
    require.require("../../file1.js");
  }

  @Test
  public void theExceptionThrownForAnUnknownFileCanBeCaughtInJavaScriptAndHasTheProperCode()
      throws Throwable {
    String code =
        context
            .eval(
                "js",
                "(function() { try { require('./invalid'); } catch (ex) { return ex.message; } })();")
            .asString();
    assertEquals("Module not found: ./invalid", code);
  }

  @Test
  public void rootModulesExposeTheExpectedFields() throws Throwable {
    Value module = context.eval("js", "module");
    Value exports = context.eval("js", "exports");

    assertEquals(exports.toString(), module.getMember("exports").toString());
    assertTrue(module.getMember("children").hasArrayElements());
    assertEquals(module.getMember("children").getArraySize(), 0);
    assertEquals("<main>", module.getMember("filename").asString());
    assertEquals("<main>", module.getMember("id").asString());
    assertEquals(true, module.getMember("loaded").asBoolean());
    assertTrue(module.getMember("parent").isNull());
    assertNotNull(exports);
  }

  @Test
  public void topLevelModulesExposeTheExpectedFields() throws Throwable {
    when(root.getFile("file1.js"))
        .thenReturn(
            "exports._module = module; exports._exports = exports; exports._main = require.main; exports._filename = __filename; exports._dirname = __dirname;");

    Value top = context.eval("js", "module");
    Value module = context.eval("js", "require('./file1')._module");
    Value exports = context.eval("js", "require('./file1')._exports");
    Value main = context.eval("js", "require('./file1')._main");

    assertEquals(exports.toString(), module.getMember("exports").toString());
    assertTrue(module.getMember("children").hasArrayElements());
    assertEquals(module.getMember("children").getArraySize(), 0);
    assertEquals("/file1.js", module.getMember("filename").asString());
    assertEquals("/file1.js", module.getMember("id").asString());
    assertEquals(true, module.getMember("loaded").asBoolean());
    assertEquals(top.toString(), module.getMember("parent").toString());
    assertNotNull(exports);
    assertEquals(top.toString(), main.toString());

    assertEquals("file1.js", exports.getMember("_filename").asString());
    assertEquals("", exports.getMember("_dirname").asString());
  }

  @Test
  public void subModulesExposeTheExpectedFields() throws Throwable {
    when(sub1.getFile("sub1file1.js"))
        .thenReturn(
            "exports._module = module; exports._exports = exports; exports._main = require.main; exports._filename = __filename; exports._dirname = __dirname");

    Value top = context.eval("js", "module");
    Value module = context.eval("js", "require('./sub1/sub1file1')._module");
    Value exports = context.eval("js", "require('./sub1/sub1file1')._exports");
    Value main = context.eval("js", "require('./sub1/sub1file1')._main");

    assertEquals(exports.toString(), module.getMember("exports").toString());
    assertTrue(module.getMember("children").hasArrayElements());
    assertEquals(module.getMember("children").getArraySize(), 0);
    assertEquals("/sub1/sub1file1.js", module.getMember("filename").asString());
    assertEquals("/sub1/sub1file1.js", module.getMember("id").asString());
    assertEquals(true, module.getMember("loaded").asBoolean());
    assertEquals(top.toString(), module.getMember("parent").toString());
    assertNotNull(exports);
    assertEquals(top.toString(), main.toString());

    assertEquals("sub1file1.js", exports.getMember("_filename").asString());
    assertEquals("/sub1", exports.getMember("_dirname").asString());
  }

  @Test
  public void subSubModulesExposeTheExpectedFields() throws Throwable {
    when(sub1sub1.getFile("sub1sub1file1.js"))
        .thenReturn(
            "exports._module = module; exports._exports = exports; exports._main = require.main;");

    Value top = context.eval("js", "module");
    Value module = context.eval("js", "require('./sub1/sub1/sub1sub1file1')._module");
    Value exports = context.eval("js", "require('./sub1/sub1/sub1sub1file1')._exports");
    Value main = context.eval("js", "require('./sub1/sub1/sub1sub1file1')._main");

    assertEquals(exports.toString(), module.getMember("exports").toString());
    assertTrue(module.getMember("children").hasArrayElements());
    assertEquals(module.getMember("children").getArraySize(), 0);
    assertEquals("/sub1/sub1/sub1sub1file1.js", module.getMember("filename").asString());
    assertEquals("/sub1/sub1/sub1sub1file1.js", module.getMember("id").asString());
    assertEquals(true, module.getMember("loaded").asBoolean());
    assertEquals(top.toString(), module.getMember("parent").toString());
    assertNotNull(exports);
    assertEquals(top.toString(), main.toString());
  }

  @Test
  public void requireInRequiredModuleYieldExpectedParentAndChildren() throws Throwable {
    when(root.getFile("file1.js"))
        .thenReturn("exports._module = module; exports.sub = require('./sub1/sub1file1');");
    when(sub1.getFile("sub1file1.js")).thenReturn("exports._module = module;");

    Value top = context.eval("js", "module");
    Value module = context.eval("js", "require('./file1')._module");
    Value subModule = context.eval("js", "require('./file1').sub._module");

    assertTrue(top.getMember("parent").isNull());
    assertEquals(top.toString(), module.getMember("parent").toString());
    assertEquals(module.toString(), subModule.getMember("parent").toString());
    assertEquals(module.toString(), top.getMember("children").getArrayElement(0).toString());
    assertEquals(subModule.toString(), module.getMember("children").getArrayElement(0).toString());
    assertTrue(subModule.getMember("children").hasArrayElements());
    assertEquals(0, subModule.getMember("children").getArraySize());
  }

  @Test
  public void loadedIsFalseWhileModuleIsLoadingAndTrueAfter() throws Throwable {
    when(root.getFile("file1.js"))
        .thenReturn("exports._module = module; exports._loaded = module.loaded;");

    Value top = context.eval("js", "module");
    Value module = context.eval("js", "require('./file1')._module");
    boolean loaded = context.eval("js", "require('./file1')._loaded").asBoolean();

    assertTrue(top.getMember("loaded").asBoolean());
    assertFalse(loaded);
    assertTrue(module.getMember("loaded").asBoolean());
  }

  @Test
  public void loadingTheSameModuleTwiceYieldsTheSameObject() throws Throwable {
    Value first = context.eval("js", "require('./file1');");
    Value second = context.eval("js", "require('./file1');");
    assertEquals(first.toString(), second.toString());
  }

  @Test
  public void loadingTheSameModuleFromASubModuleYieldsTheSameObject() throws Throwable {
    when(root.getFile("file2.js")).thenReturn("exports.sub = require('./file1');");
    Value first = context.eval("js", "require('./file1');");
    Value second = context.eval("js", "require('./file2').sub;");
    assertEquals(first.toString(), second.toString());
  }

  @Test
  public void loadingTheSameModuleFromASubPathYieldsTheSameObject() throws Throwable {
    when(sub1.getFile("sub1file1.js")).thenReturn("exports.sub = require('../file1');");
    Value first = context.eval("js", "require('./file1');");
    Value second = context.eval("js", "require('./sub1/sub1file1').sub;");
    assertEquals(first.toString(), second.toString());
  }

  @Test
  public void scriptCodeCanReplaceTheModuleExportsSymbol() throws Throwable {
    when(root.getFile("file1.js")).thenReturn("module.exports = { 'foo': 'bar' }");
    assertEquals("bar", context.eval("js", "require('./file1').foo;").asString());
  }

  @Test
  public void itIsPossibleToRegisterGlobalVariablesForAllModules() throws Throwable {
    context.getBindings("js").putMember("bar", "bar");
    when(root.getFile("file1.js")).thenReturn("exports.foo = function() { return bar; }");
    assertEquals("bar", context.eval("js", "require('./file1').foo();").asString());
  }

  @Test
  public void engineScopeVariablesAreVisibleDuringModuleLoad() throws Throwable {
    context.getBindings("js").putMember("bar", "bar");
    when(root.getFile("file1.js"))
        .thenReturn("var found = bar == 'bar'; exports.foo = function() { return found; }");
    assertEquals(true, context.eval("js", "require('./file1').foo();").asBoolean());
  }

  @Test
  public void itCanLoadModulesFromModulesFromModules() throws Throwable {
    when(root.getFile("file1.js")).thenReturn("exports.sub = require('./file2.js');");
    when(root.getFile("file2.js")).thenReturn("exports.sub = require('./file3.js');");
    when(root.getFile("file3.js")).thenReturn("exports.foo = 'bar';");

    assertEquals("bar", context.eval("js", "require('./file1.js').sub.sub.foo").asString());
  }

  // Check for https://github.com/coveo/nashorn-commonjs-modules/issues/2
  @Test
  public void itCanCallFunctionsNamedGetFromModules() throws Throwable {
    when(root.getFile("file1.js")).thenReturn("exports.get = function(foo) { return 'bar'; };");

    assertEquals("bar", context.eval("js", "require('./file1.js').get(123, 456)").asString());
  }

  // Checks for https://github.com/coveo/nashorn-commonjs-modules/issues/3

  // This one only failed on older JREs
  @Test
  public void itCanUseHighlightJsLibraryFromNpm() throws Throwable {
    File file = new File("src/test/resources/graal/test2");
    FilesystemFolder root = FilesystemFolder.create(file, "UTF-8");
    require = Require.enable(context, root);
    context.eval("js", "require('highlight.js').highlight('java', '\"foo\"')");
  }

  // This one failed on more recent ones too
  @Test
  public void anotherCheckForIssueNumber3() throws Throwable {
    when(root.getFile("file1.js"))
        .thenReturn(
            "var a = require('./file2'); function b() {}; b.prototype = Object.create(a.prototype, {});");
    when(root.getFile("file2.js"))
        .thenReturn(
            "module.exports = a; function a() {}; a.prototype = Object.create(Object.prototype, {})");
    require = Require.enable(context, root);
    context.eval("js", "require('./file1');");
  }

  // Check for https://github.com/coveo/nashorn-commonjs-modules/issues/4
  @Test
  public void itSupportOverwritingExportsWithAString() throws Throwable {
    when(root.getFile("file1.js")).thenReturn("module.exports = 'foo';");
    assertEquals("foo", context.eval("js", "require('./file1.js')").asString());
  }

  // Check for https://github.com/coveo/nashorn-commonjs-modules/issues/4
  @Test
  public void itSupportOverwritingExportsWithAnInteger() throws Throwable {
    when(root.getFile("file1.js")).thenReturn("module.exports = 123;");
    assertEquals(123, context.eval("js", "require('./file1.js')").asInt());
  }

  // Checks for https://github.com/coveo/nashorn-commonjs-modules/issues/11

  @Test
  public void itCanLoadInvariantFromFbjs() throws Throwable {
    File file = new File("src/test/resources/graal/test3");
    FilesystemFolder root = FilesystemFolder.create(file, "UTF-8");
    require = Require.enable(context, root);
    context.eval("js", "require('fbjs/lib/invariant')");
  }

  // Checks for https://github.com/coveo/nashorn-commonjs-modules/pull/14

  @Test
  public void itCanShortCircuitCircularRequireReferences() throws Throwable {
    File file = new File("src/test/resources/graal/test4/cycles");
    FilesystemFolder root = FilesystemFolder.create(file, "UTF-8");
    require = Require.enable(context, root);
    context.eval("js", "require('./main.js')");
  }

  @Test
  public void itCanShortCircuitDeepCircularRequireReferences() throws Throwable {
    File file = new File("src/test/resources/graal/test4/deep");
    FilesystemFolder root = FilesystemFolder.create(file, "UTF-8");
    require = Require.enable(context, root);
    context.eval("js", "require('./main.js')");
  }

  // Checks for https://github.com/coveo/nashorn-commonjs-modules/issues/15

  @Test
  public void itCanDefinePropertiesOnExportsObject() throws Throwable {
    when(root.getFile("file1.js"))
        .thenReturn("Object.defineProperty(exports, '__esModule', { value: true });");
    context.eval("js", "require('./file1.js')");
  }

  @Test
  public void itIncludesStartLineInException() throws Throwable {
    when(root.getFile("file1.js"))
        .thenReturn("\n\nexports.foo = function() { throw \"bad thing\";};");
    try {
      context.eval("js", "require('./file1').foo();");
      fail("should throw exception");
    } catch (PolyglotException e) {
      assertEquals(3, e.getSourceLocation().getStartLine());
    }
  }

  // Checks for https://github.com/coveo/nashorn-commonjs-modules/issues/22

  @Test
  public void itCanLoadModulesWhoseLastLineIsAComment() throws Throwable {
    when(root.getFile("file1.js")).thenReturn("exports.foo = \"bar\";\n// foo");
    assertEquals("bar", context.eval("js", "require('./file1.js').foo").asString());
  }
}
