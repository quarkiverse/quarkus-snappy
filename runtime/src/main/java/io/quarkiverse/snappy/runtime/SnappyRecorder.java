package io.quarkiverse.snappy.runtime;

import java.util.Optional;

import org.xerial.snappy.Snappy;
import org.xerial.snappy.SnappyLoader;
import org.xerial.snappy.pool.DefaultPoolFactory;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

/**
 * Applies the configured {@code org.xerial.snappy.*} system properties, then forces Snappy's native
 * library to load eagerly at boot instead of lazily on first use, so a broken native setup fails fast.
 * The properties are set before {@link Snappy} is touched because Snappy reads them from static
 * initializers. Loading itself is left entirely to {@code SnappyLoader} (triggered by {@link Snappy}'s
 * own static initializer), to avoid a second, duplicated loading path.
 */
@Recorder
public class SnappyRecorder {

    private final RuntimeValue<SnappyRuntimeConfig> config;

    public SnappyRecorder(RuntimeValue<SnappyRuntimeConfig> config) {
        this.config = config;
    }

    public void loadSnappy() {
        SnappyRuntimeConfig snappyConfig = config.getValue();
        setIfPresent(SnappyLoader.KEY_SNAPPY_LIB_PATH, snappyConfig.libPath());
        setIfPresent(SnappyLoader.KEY_SNAPPY_LIB_NAME, snappyConfig.libName());
        setIfPresent(SnappyLoader.KEY_SNAPPY_TEMPDIR, snappyConfig.tempDir());
        setIfPresent(SnappyLoader.KEY_SNAPPY_USE_SYSTEMLIB, snappyConfig.useSystemLib());
        setIfPresent(DefaultPoolFactory.DISABLE_CACHING_PROPERTY, snappyConfig.poolDisable());

        Snappy.getNativeLibraryVersion();
    }

    private static void setIfPresent(String key, Optional<?> value) {
        if (value.isPresent()) {
            System.setProperty(key, value.get().toString());
        }
    }
}
