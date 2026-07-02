package io.quarkiverse.snappy.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.xerial.snappy.OSInfo;

public class SnappyUtilsTest {

    @Test
    public void nativeLibFolderPathMatchesTheOneResolvedByOSInfo() {
        String path = SnappyUtils.getNativeLibFolderPathForCurrentOS();

        assertNotNull(path);
        assertEquals(OSInfo.getNativeLibFolderPathForCurrentOS(), path);
    }
}
