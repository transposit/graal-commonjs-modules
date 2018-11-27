/*
 * Copyright 2018 Transposit Corporation. All Rights Reserved.
 */

package graal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.graalvm.polyglot.Context;
import org.junit.Test;

public class ResourceFolderTest {
  private ResourceFolder root =
      ResourceFolder.create(getClass().getClassLoader(), "graal/test1", "UTF-8");

  @Test
  public void rootFolderHasTheExpectedProperties() {
    assertEquals("/", root.getPath());
    assertNull(root.getParent());
  }

  @Test
  public void getFileReturnsTheContentOfTheFileWhenItExists() {
    assertTrue(root.getFile("foo.js").contains("foo"));
  }

  @Test
  public void getFileReturnsNullWhenFileDoesNotExists() {
    assertNull(root.getFile("invalid"));
  }

  @Test
  public void getFolderReturnsAnObjectWithTheExpectedProperties() {
    Folder sub = root.getFolder("subdir");
    assertEquals("/subdir/", sub.getPath());
    assertSame(root, sub.getParent());
    Folder subsub = sub.getFolder("subsubdir");
    assertEquals("/subdir/subsubdir/", subsub.getPath());
    assertSame(sub, subsub.getParent());
  }

  @Test
  public void getFolderNeverReturnsNullBecauseItCannot() {
    assertNotNull(root.getFolder("subdir"));
    assertNotNull(root.getFolder("invalid"));
  }

  @Test
  public void getFileCanBeUsedOnSubFolderIfFileExist() {
    assertTrue(root.getFolder("subdir").getFile("bar.js").contains("bar"));
  }

  @Test
  public void resourceFolderWorksWhenUsedForReal() throws Throwable {
    Context context = Context.create();
    Require.enable(context, root);
    assertEquals("spam", context.eval("js", "require('./foo').bar.spam.spam").asString());
  }
}
