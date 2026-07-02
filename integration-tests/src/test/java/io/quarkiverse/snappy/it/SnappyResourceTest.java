package io.quarkiverse.snappy.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.Base64;
import java.util.Random;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
public class SnappyResourceTest {

    @Test
    public void testHelloEndpoint() {
        given()
                .when().get("/snappy")
                .then()
                .statusCode(200)
                .body(is("Hello snappy"));
    }

    @Test
    public void compressedDataSurvivesARoundTripThroughTheNativeLibrary() {
        String payload = "quarkus-snappy integration test payload".repeat(20);

        given()
                .contentType(ContentType.TEXT)
                .body(payload)
                .when().post("/snappy/roundtrip")
                .then()
                .statusCode(200)
                .body(is(payload));
    }

    @Test
    public void compressedDataLargerThanTheDefaultBlockSizeSurvivesARoundTripThroughTheStreamingApi() {
        String payload = "quarkus-snappy streaming payload".repeat(2000);

        given()
                .contentType(ContentType.TEXT)
                .body(payload)
                .when().post("/snappy/stream-roundtrip")
                .then()
                .statusCode(200)
                .body(is(payload));
    }

    @Test
    public void arbitraryBinaryDataSurvivesARoundTrip() {
        byte[] original = new byte[8192];
        new Random(42).nextBytes(original);
        String base64Input = Base64.getEncoder().encodeToString(original);

        String base64Output = given()
                .contentType(ContentType.TEXT)
                .body(base64Input)
                .when().post("/snappy/binary-roundtrip")
                .then()
                .statusCode(200)
                .extract().asString();

        assertArrayEquals(original, Base64.getDecoder().decode(base64Output));
    }
}
