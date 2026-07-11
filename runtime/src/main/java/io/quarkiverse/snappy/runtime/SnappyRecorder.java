package io.quarkiverse.snappy.runtime;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Optional;

import org.xerial.snappy.Snappy;
import org.xerial.snappy.SnappyError;
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

    private static final String NATIVE_LIB_FILE = "libsnappyjava.so";
    private static final String GLIBC_RESOURCE = "/org/xerial/snappy/native/Linux/x86_64/" + NATIVE_LIB_FILE;
    private static final String MUSL_RESOURCE = "/org/xerial/snappy/native/Linux/x86_64-musl/" + NATIVE_LIB_FILE;

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

        selectLibcMatchingNativeLibrary(snappyConfig);

        try {
            Snappy.getNativeLibraryVersion();
        } catch (LinkageError | SnappyError e) {
            throw new IllegalStateException(
                    nativeLoadErrorMessage(snappyConfig.useSystemLib().orElse(false), snappyConfig.tempDir()), e);
        }
    }

    /**
     * A message pointing at the likely cause of a native library load failure and the configuration
     * property that addresses it, since the underlying {@link UnsatisfiedLinkError} is opaque.
     */
    static String nativeLoadErrorMessage(boolean useSystemLib, Optional<String> tempDir) {
        if (useSystemLib) {
            return "Snappy's native library failed to load. quarkus.snappy.use-system-lib is enabled, so a "
                    + "system-installed libsnappyjava must be available on the native library path; install it "
                    + "or unset the property to use the library bundled with the extension.";
        }
        String extractionDir = tempDir.orElse("the JVM temporary directory (java.io.tmpdir)");
        return "Snappy's native library failed to load. It is extracted to " + extractionDir + " before loading; "
                + "if that directory is mounted noexec, set quarkus.snappy.temp-dir to an executable location.";
    }

    /**
     * In an x86_64 Linux native executable, points Snappy at the bundled native library matching the
     * executable's own libc. When both the glibc and musl variants are bundled (a container build, see
     * {@code SnappyProcessor#containerNativeLibraryResources}), {@code OSInfo.getArchName()} would pick
     * the variant based on the runtime host's {@code /lib/ld-musl-x86_64.so.1}, which can differ from
     * the libc the executable was actually linked against and yield an {@link UnsatisfiedLinkError}. The
     * executable's real libc is read from {@code /proc/self/maps} instead, and the matching variant is
     * extracted and pinned with {@code org.xerial.snappy.lib.path}, which Snappy honors over its own
     * architecture detection. Skipped outside a native image, when the user already pointed Snappy
     * elsewhere, or when only one variant is present (a JVM run or a non-container native build, where
     * Snappy's own detection already matches the running JVM).
     */
    private static void selectLibcMatchingNativeLibrary(SnappyRuntimeConfig snappyConfig) {
        if (snappyConfig.libPath().isPresent() || snappyConfig.useSystemLib().orElse(false)) {
            return;
        }
        if (!isNativeImageRuntime() || !isLinuxX8664()
                || resourceMissing(GLIBC_RESOURCE) || resourceMissing(MUSL_RESOURCE)) {
            return;
        }

        String resource = isMuslProcess() ? MUSL_RESOURCE : GLIBC_RESOURCE;
        String tempDirBase = snappyConfig.tempDir().orElseGet(() -> System.getProperty("java.io.tmpdir"));
        File extracted = extractNativeLibrary(resource, tempDirBase);
        if (extracted != null) {
            System.setProperty(SnappyLoader.KEY_SNAPPY_LIB_PATH, extracted.getParent());
            System.setProperty(SnappyLoader.KEY_SNAPPY_LIB_NAME, extracted.getName());
        }
    }

    private static boolean isNativeImageRuntime() {
        return "runtime".equals(System.getProperty("org.graalvm.nativeimage.imagecode"));
    }

    private static boolean isLinuxX8664() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        return os.contains("linux") && (arch.equals("x86_64") || arch.equals("amd64"));
    }

    private static boolean isMuslProcess() {
        try {
            String maps = Files.readString(Path.of("/proc/self/maps"));
            return maps.contains("ld-musl") || maps.contains("libc.musl");
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }

    private static boolean resourceMissing(String resource) {
        return SnappyRecorder.class.getResource(resource) == null;
    }

    private static File extractNativeLibrary(String resource, String tempDirBase) {
        try (InputStream in = SnappyRecorder.class.getResourceAsStream(resource)) {
            if (in == null) {
                return null;
            }
            File dir = Files.createTempDirectory(Path.of(tempDirBase), "quarkus-snappy-").toFile();
            dir.deleteOnExit();
            File lib = new File(dir, NATIVE_LIB_FILE);
            Files.copy(in, lib.toPath(), StandardCopyOption.REPLACE_EXISTING);
            lib.setReadable(true);
            lib.setExecutable(true);
            lib.deleteOnExit();
            return lib;
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private static void setIfPresent(String key, Optional<?> value) {
        if (value.isPresent()) {
            System.setProperty(key, value.get().toString());
        }
    }
}
