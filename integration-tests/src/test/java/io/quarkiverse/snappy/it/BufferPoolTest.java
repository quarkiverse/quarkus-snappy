package io.quarkiverse.snappy.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * The application process is started with {@code org.xerial.snappy.pool.disable=true}, so Snappy must
 * select {@code QuiescentBufferPool} over the default {@code CachingBufferPool}.
 * <p>
 * {@code DefaultPoolFactory} reads that property from a static field initializer. Quarkus initializes
 * every package at build time unless it is registered for runtime initialization, so in a native
 * executable the property is read on the build host and the chosen pool is frozen into the image
 * heap. Without {@code org.xerial.snappy.pool} registered as runtime-initialized, this test passes in
 * JVM mode and fails in native mode, which is exactly the divergence it guards against.
 */
@QuarkusTest
public class BufferPoolTest {

    @Test
    public void bufferPoolSelectionHonorsTheSystemPropertyOfTheRunningProcess() {
        given()
                .when().get("/snappy/buffer-pool")
                .then()
                .statusCode(200)
                .body(is("QuiescentBufferPool"));
    }
}
