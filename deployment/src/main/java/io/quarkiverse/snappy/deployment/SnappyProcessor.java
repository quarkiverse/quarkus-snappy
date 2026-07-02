package io.quarkiverse.snappy.deployment;

import java.util.function.BooleanSupplier;

import com.bruker.snappy.runtime.SnappyRecorder;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageRunnerBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;

class SnappyProcessor {

    private static final String FEATURE = "snappy";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    /**
     * {@code Snappy} and {@code SnappyLoader} are registered as {@link RuntimeInitializedClassBuildItem}
     * because their static initializers extract and {@code System.load()} the native library; if
     * that ran at build time, the load would happen on the build host's JVM instead of the
     * compiled executable's own process.
     * <p>
     * For container builds, the OS is hardcoded to Linux because builder images are always
     * Linux-based regardless of the host OS, but the architecture is still derived from the host
     * (via {@link SnappyUtils#getArchName()}) since Quarkus native container builds never cross-compile.
     */
    @BuildStep(onlyIf = { HasSnappy.class, NativeOrNativeSourcesBuild.class })
    public void build(NativeImageRunnerBuildItem nativeImageRunner,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<NativeImageResourceBuildItem> nativeLibs,
            BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClasses) {
        reflectiveClass.produce(ReflectiveClassBuildItem.builder("org.xerial.snappy.SnappyInputStream",
                "org.xerial.snappy.SnappyOutputStream").methods().fields().build());

        runtimeInitializedClasses.produce(new RuntimeInitializedClassBuildItem("org.xerial.snappy.Snappy"));
        runtimeInitializedClasses.produce(new RuntimeInitializedClassBuildItem("org.xerial.snappy.SnappyLoader"));

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

    @BuildStep(onlyIf = HasSnappy.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void loadSnappyIfEnabled(SnappyRecorder recorder) {
        recorder.loadSnappy();
    }

    public static final class HasSnappy implements BooleanSupplier {

        private final SnappyBuildTimeConfig config;

        public HasSnappy(SnappyBuildTimeConfig config) {
            this.config = config;
        }

        @Override
        public boolean getAsBoolean() {
            return QuarkusClassLoader.isClassPresentAtRuntime("org.xerial.snappy.OSInfo") && config.enable();
        }
    }
}
