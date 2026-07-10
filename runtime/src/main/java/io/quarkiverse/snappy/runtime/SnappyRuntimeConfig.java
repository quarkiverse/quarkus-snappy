package io.quarkiverse.snappy.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

/**
 * Exposes Snappy's own runtime knobs as Quarkus configuration. Each value maps to a
 * {@code org.xerial.snappy.*} system property that Snappy reads from a static initializer, which the
 * recorder sets before the native library loads. Left unset, a property is not written, so Snappy keeps
 * its own default.
 */
@ConfigMapping(prefix = "quarkus.snappy")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface SnappyRuntimeConfig {

    /**
     * Directory to load the native library from instead of extracting the bundled one. Maps to
     * {@code org.xerial.snappy.lib.path}.
     */
    Optional<String> libPath();

    /**
     * Name of the native library file to load from {@link #libPath()}. Maps to
     * {@code org.xerial.snappy.lib.name}.
     */
    Optional<String> libName();

    /**
     * Directory the bundled native library is extracted to before loading. Defaults to the JVM's
     * {@code java.io.tmpdir} when unset. Useful when the default temporary directory is mounted
     * {@code noexec}. Maps to {@code org.xerial.snappy.tempdir}.
     */
    @ConfigDocDefault("java.io.tmpdir")
    Optional<String> tempDir();

    /**
     * Load a system-installed {@code libsnappyjava} instead of the bundled one. The bundled library is
     * still embedded in a native image. Maps to {@code org.xerial.snappy.use.systemlib}.
     */
    @ConfigDocDefault("false")
    Optional<Boolean> useSystemLib();

    /**
     * Disable the caching buffer pool used by the framed streams, selecting the non-caching
     * {@code QuiescentBufferPool}. Maps to {@code org.xerial.snappy.pool.disable}.
     */
    @ConfigDocDefault("false")
    Optional<Boolean> poolDisable();
}
