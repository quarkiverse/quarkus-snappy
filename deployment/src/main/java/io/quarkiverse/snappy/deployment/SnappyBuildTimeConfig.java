package io.quarkiverse.snappy.deployment;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "kafka", phase = ConfigPhase.BUILD_TIME)
public class SnappyBuildTimeConfig {

    @ConfigItem(name = "snappy.enabled", defaultValue = "false")
    public boolean snappyEnabled;

}
