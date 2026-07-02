package com.bruker.snappy.runtime;

import org.xerial.snappy.Snappy;

import io.quarkus.runtime.annotations.Recorder;

/**
 * Forces Snappy's native library to load eagerly at boot instead of lazily on first use, so a
 * broken native setup fails fast. Loading itself is left entirely to {@code SnappyLoader}
 * (triggered by {@link Snappy}'s own static initializer), to avoid a second, duplicated
 * loading path.
 */
@Recorder
public class SnappyRecorder {

    public void loadSnappy() {
        Snappy.getNativeLibraryVersion();
    }
}
