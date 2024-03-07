package io.quarkiverse.snappy.deployment;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "kafka", phase = ConfigPhase.BUILD_TIME)
public class SnappyBuildTimeConfig {

    /**
     * Whether to enable Snappy in native mode.
     * <p>
     * This library is unpacked and loaded when the application starts.
     */
    @ConfigItem(name = "snappy.enabled", defaultValue = "false")
    public boolean snappyEnabled;

}
