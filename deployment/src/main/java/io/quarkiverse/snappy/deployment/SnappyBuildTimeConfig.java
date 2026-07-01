package io.quarkiverse.snappy.deployment;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
@ConfigMapping(prefix = "quarkus.snappy")
public interface SnappyBuildTimeConfig {

    /**
     * Whether to enable Snappy in native mode.
     * <p>
     * This library is unpacked and loaded when the application starts.
     */
    @WithDefault("false")
    boolean enable();

}
