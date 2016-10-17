package org.jboss.fuse.forge.addon.util;

import org.junit.Assert;
import org.junit.Test;

public class MavenUtilsTest {

    @Test
    public void testIsRedhatVersionMatchesSuffix() {
        Assert.assertTrue(MavenUtils.isRedhatVersion("1.0.0.redhat-00001"));
    }

    @Test
    public void testIsRedhatVersionMatchesFuseSuffix() {
        Assert.assertTrue(MavenUtils.isRedhatVersion("1.0.0.fuse-00001"));
    }

    @Test
    public void testIsRedhatVersionDoesNotMatchInvalidSuffix() {
        Assert.assertFalse(MavenUtils.isRedhatVersion("1.0.0.foo-00001"));
    }

    @Test
    public void testIsRedhatVersionDoesNotMatchAbsentSuffix() {
        Assert.assertFalse(MavenUtils.isRedhatVersion("1.0.0"));
    }
}
