package io.quarkiverse.snappy.deployment;

import java.util.List;

import org.jboss.logging.Logger;

import io.quarkiverse.snappy.runtime.SnappyRecorder;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedPackageBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageRunnerBuildItem;

class SnappyProcessor {

    private static final Logger LOG = Logger.getLogger(SnappyProcessor.class);

    private static final String FEATURE = "snappy";

    /**
     * The full Snappy native chain: the entry points ({@code Snappy}, {@code BitShuffle}), the
     * loader ({@code SnappyLoader}) and the classes that declare the JNI native methods
     * ({@code SnappyNative}, {@code BitShuffleNative}).
     */
    private static final String[] NATIVE_CLASSES = {
            "org.xerial.snappy.Snappy",
            "org.xerial.snappy.SnappyLoader",
            "org.xerial.snappy.SnappyNative",
            "org.xerial.snappy.BitShuffle",
            "org.xerial.snappy.BitShuffleNative"
    };

    /**
     * {@code DefaultPoolFactory} picks the {@code BufferPool} used by the framed streams from the
     * {@code org.xerial.snappy.pool.disable} system property, read in a static field initializer. Left
     * to build-time initialization, that property is read on the build host and the resulting pool is
     * frozen into the image heap, so setting it on the executable has no effect.
     */
    private static final String NATIVE_POOL_PACKAGE = "org.xerial.snappy.pool";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    /**
     * The whole native chain is registered as {@link RuntimeInitializedClassBuildItem} so the
     * library extraction, {@code System.load()} and native-method linkage all happen in the
     * compiled executable at runtime. If any of these classes were initialized at build time, its
     * native methods would be bound against the build host's JVM and fail with
     * {@link UnsatisfiedLinkError} in the executable.
     * <p>
     * For container builds, the OS is hardcoded to Linux because builder images are always
     * Linux-based regardless of the host OS, and the architecture is derived from the host (via
     * {@link SnappyUtils#getArchName()}) since Quarkus native container builds never cross-compile.
     * The libc cannot be derived the same way, so for x86_64 both variants are bundled and the
     * recorder selects the right one at runtime (see {@link #containerNativeLibraryResources(String)}).
     */
    @BuildStep
    public void build(NativeImageRunnerBuildItem nativeImageRunner,
            BuildProducer<NativeImageResourceBuildItem> nativeLibs,
            BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClasses,
            BuildProducer<RuntimeInitializedPackageBuildItem> runtimeInitializedPackages) {
        for (String nativeClass : NATIVE_CLASSES) {
            runtimeInitializedClasses.produce(new RuntimeInitializedClassBuildItem(nativeClass));
        }
        runtimeInitializedPackages.produce(new RuntimeInitializedPackageBuildItem(NATIVE_POOL_PACKAGE));

        List<String> resources;
        if (nativeImageRunner.isContainerBuild()) {
            resources = containerNativeLibraryResources(SnappyUtils.getArchName());
        } else {
            String dir = SnappyUtils.getNativeLibFolderPathForCurrentOS();
            resources = List.of("org/xerial/snappy/native/" + dir + "/" + System.mapLibraryName("snappyjava"));
        }
        for (String resource : resources) {
            nativeLibs.produce(new NativeImageResourceBuildItem(resource));
        }
        LOG.infof("Bundling Snappy native library into the image: %s", String.join(", ", resources));
    }

    /**
     * Native library resources to bundle for a container build with the given host architecture.
     * {@code OSInfo.getArchName()} conflates architecture and libc: on x86_64 it returns
     * {@code x86_64-musl} whenever the host has musl, regardless of the builder image's libc. Since
     * the host cannot tell which libc the executable will use, both x86_64 variants are bundled so the
     * recorder can pick the one matching the executable at runtime. Other architectures have a single
     * variant and are bundled as-is.
     */
    static List<String> containerNativeLibraryResources(String archName) {
        if (archName.equals("x86_64") || archName.equals("x86_64-musl")) {
            return List.of(
                    "org/xerial/snappy/native/Linux/x86_64/libsnappyjava.so",
                    "org/xerial/snappy/native/Linux/x86_64-musl/libsnappyjava.so");
        }
        return List.of("org/xerial/snappy/native/Linux/" + archName + "/libsnappyjava.so");
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void loadSnappy(SnappyRecorder recorder) {
        recorder.loadSnappy();
    }
}
