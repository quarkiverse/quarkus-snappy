package io.quarkiverse.snappy.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class SnappyResourceTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "native-library-version",
            "byte-array-roundtrip",
            "length-and-validity-helpers",
            "raw-offset-roundtrip",
            "string-roundtrip",
            "char-array-roundtrip",
            "short-array-roundtrip",
            "int-array-roundtrip",
            "long-array-roundtrip",
            "float-array-roundtrip",
            "double-array-roundtrip",
            "direct-bytebuffer-roundtrip",
            "snappy-stream-roundtrip",
            "snappy-framed-stream-roundtrip",
            "bitshuffle-int-roundtrip",
            "bitshuffle-long-roundtrip"
    })
    public void operationRoundTripsThroughTheNativeLibrary(String operation) {
        given()
                .when().get("/snappy/check/{operation}", operation)
                .then()
                .statusCode(200)
                .body(is("ok"));
    }
}
