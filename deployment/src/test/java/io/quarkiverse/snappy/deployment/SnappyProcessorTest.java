package io.quarkiverse.snappy.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.deployment.builditem.FeatureBuildItem;

public class SnappyProcessorTest {

    private final SnappyProcessor processor = new SnappyProcessor();

    @Test
    public void featureBuildItemAdvertisesSnappy() {
        FeatureBuildItem feature = processor.feature();

        assertEquals("snappy", feature.getName());
    }

    @Test
    public void containerBuildBundlesBothLibcVariantsForX8664() {
        List<String> glibcHost = SnappyProcessor.containerNativeLibraryResources("x86_64");
        List<String> muslHost = SnappyProcessor.containerNativeLibraryResources("x86_64-musl");

        List<String> both = List.of(
                "org/xerial/snappy/native/Linux/x86_64/libsnappyjava.so",
                "org/xerial/snappy/native/Linux/x86_64-musl/libsnappyjava.so");
        assertEquals(both, glibcHost);
        assertEquals(both, muslHost);
    }

    @Test
    public void containerBuildBundlesSingleVariantForOtherArchitectures() {
        List<String> resources = SnappyProcessor.containerNativeLibraryResources("aarch64");

        assertEquals(List.of("org/xerial/snappy/native/Linux/aarch64/libsnappyjava.so"), resources);
        assertTrue(resources.stream().noneMatch(r -> r.contains("musl")));
    }
}
