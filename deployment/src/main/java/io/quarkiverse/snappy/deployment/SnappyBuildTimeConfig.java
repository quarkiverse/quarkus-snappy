package io.quarkiverse.snappy.deployment;

import io.quarkus.runtime.annotations.ConfigItem;

public class SnappyBuildTimeConfig {

    @ConfigItem(name = "snappy.enabled", defaultValue = "false")
    public boolean snappyEnabled;

}
