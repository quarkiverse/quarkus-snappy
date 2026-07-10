package io.quarkiverse.snappy.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * The application sets {@code quarkus.snappy.pool-disable=true}, so Snappy must select
 * {@code QuiescentBufferPool} over the default {@code CachingBufferPool}. This exercises the extension's
 * configuration surface end to end: the recorder turns the Quarkus property into the
 * {@code org.xerial.snappy.pool.disable} system property that {@code DefaultPoolFactory} reads.
 * <p>
 * {@code DefaultPoolFactory} reads that property from a static field initializer. Quarkus initializes
 * every package at build time unless it is registered for runtime initialization, so in a native
 * executable the property would be read on the build host, before the recorder runs, and the chosen
 * pool frozen into the image heap. Without {@code org.xerial.snappy.pool} registered as
 * runtime-initialized, this test passes in JVM mode and fails in native mode, which is exactly the
 * divergence it guards against.
 */
@QuarkusTest
public class BufferPoolTest {

    @Test
    public void bufferPoolSelectionHonorsTheQuarkusConfiguration() {
        given()
                .when().get("/snappy/buffer-pool")
                .then()
                .statusCode(200)
                .body(is("QuiescentBufferPool"));
    }
}
