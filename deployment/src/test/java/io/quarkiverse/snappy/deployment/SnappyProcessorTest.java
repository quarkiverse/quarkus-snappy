package io.quarkiverse.snappy.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.quarkus.deployment.builditem.FeatureBuildItem;

public class SnappyProcessorTest {

    private final SnappyProcessor processor = new SnappyProcessor();

    @Test
    public void featureBuildItemAdvertisesSnappy() {
        FeatureBuildItem feature = processor.feature();

        assertEquals("snappy", feature.getName());
    }
}
