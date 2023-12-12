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
import io.quarkus.deployment.pkg.builditem.NativeImageRunnerBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;

class SnappyProcessor {

    private static final String FEATURE = "snappy";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep(onlyIf = { HasSnappy.class, NativeOrNativeSourcesBuild.class })
    public void build(NativeImageRunnerBuildItem nativeImageRunner,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<NativeImageResourceBuildItem> nativeLibs) {
        reflectiveClass.produce(ReflectiveClassBuildItem.builder("org.xerial.snappy.SnappyInputStream",
                "org.xerial.snappy.SnappyOutputStream").methods().fields().build());

        String root = "org/xerial/snappy/native/";
        // add linux64 native lib when targeting containers
        if (nativeImageRunner.isContainerBuild()) {
            String dir = "Linux/x86_64";
            String snappyNativeLibraryName = "libsnappyjava.so";
            String path = root + dir + "/" + snappyNativeLibraryName;
            nativeLibs.produce(new NativeImageResourceBuildItem(path));
        } else { // otherwise the native lib of the platform this build runs on
            String dir = SnappyUtils.getNativeLibFolderPathForCurrentOS();
            String snappyNativeLibraryName = System.mapLibraryName("snappyjava");
            String path = root + dir + "/" + snappyNativeLibraryName;
            nativeLibs.produce(new NativeImageResourceBuildItem(path));
        }
    }

    @BuildStep(onlyIf = HasSnappy.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void loadSnappyIfEnabled(SnappyRecorder recorder) {
        recorder.loadSnappy();
    }

    public static final class HasSnappy implements BooleanSupplier {

        @Override
        public boolean getAsBoolean() {
            return QuarkusClassLoader.isClassPresentAtRuntime("org.xerial.snappy.OSInfo");
        }
    }
}
