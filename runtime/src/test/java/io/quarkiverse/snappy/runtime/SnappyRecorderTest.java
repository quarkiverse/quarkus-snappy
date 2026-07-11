package io.quarkiverse.snappy.runtime;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

public class SnappyRecorderTest {

    @Test
    public void loadErrorPointsAtTheSystemLibraryWhenItIsEnabled() {
        String message = SnappyRecorder.nativeLoadErrorMessage(true, Optional.empty());

        assertTrue(message.contains("quarkus.snappy.use-system-lib"), message);
    }

    @Test
    public void loadErrorPointsAtTheDefaultTemporaryDirectoryWhenNoneIsConfigured() {
        String message = SnappyRecorder.nativeLoadErrorMessage(false, Optional.empty());

        assertTrue(message.contains("quarkus.snappy.temp-dir"), message);
        assertTrue(message.contains("java.io.tmpdir"), message);
    }

    @Test
    public void loadErrorNamesTheConfiguredTemporaryDirectory() {
        String message = SnappyRecorder.nativeLoadErrorMessage(false, Optional.of("/var/tmp"));

        assertTrue(message.contains("/var/tmp"), message);
    }
}
