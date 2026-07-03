package io.quarkiverse.snappy.deployment;

import com.bruker.snappy.runtime.SnappyRecorder;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageRunnerBuildItem;

class SnappyProcessor {

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
     * Linux-based regardless of the host OS, but the architecture is still derived from the host
     * (via {@link SnappyUtils#getArchName()}) since Quarkus native container builds never cross-compile.
     */
    @BuildStep
    public void build(NativeImageRunnerBuildItem nativeImageRunner,
            BuildProducer<NativeImageResourceBuildItem> nativeLibs,
            BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClasses) {
        for (String nativeClass : NATIVE_CLASSES) {
            runtimeInitializedClasses.produce(new RuntimeInitializedClassBuildItem(nativeClass));
        }

        String dir;
        String snappyNativeLibraryName;
        if (nativeImageRunner.isContainerBuild()) {
            dir = "Linux/" + SnappyUtils.getArchName();
            snappyNativeLibraryName = "libsnappyjava.so";
        } else {
            dir = SnappyUtils.getNativeLibFolderPathForCurrentOS();
            snappyNativeLibraryName = System.mapLibraryName("snappyjava");
        }
        nativeLibs.produce(new NativeImageResourceBuildItem("org/xerial/snappy/native/" + dir + "/" + snappyNativeLibraryName));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void loadSnappy(SnappyRecorder recorder) {
        recorder.loadSnappy();
    }
}
