package io.quarkiverse.snappy.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xerial.snappy.Snappy;

import io.quarkus.test.QuarkusUnitTest;

public class SnappyTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Test
    public void nativeLibraryLoadedByTheRecorderIsUsableForCompression() throws IOException {
        byte[] original = "quarkus-snappy extension round trip".repeat(20).getBytes(StandardCharsets.UTF_8);

        byte[] compressed = Snappy.compress(original);
        byte[] decompressed = Snappy.uncompress(compressed);

        assertArrayEquals(original, decompressed);
    }
}
