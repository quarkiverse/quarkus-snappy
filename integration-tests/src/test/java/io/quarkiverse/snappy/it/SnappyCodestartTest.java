package io.quarkiverse.snappy.it;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.Language;
import io.quarkus.devtools.testing.codestarts.QuarkusCodestartTest;

public class SnappyCodestartTest {

    @RegisterExtension
    public static final QuarkusCodestartTest codestartTest = QuarkusCodestartTest.builder()
            .languages(Language.JAVA)
            .setupStandaloneExtensionTest("io.quarkiverse.snappy:quarkus-snappy")
            .build();

    @Test
    void readmeDocumentsSnappy() throws Throwable {
        codestartTest.assertThatGeneratedFile(Language.JAVA, "README.md")
                .content()
                .contains("org.xerial.snappy.Snappy")
                .contains("quarkus.snappy.temp-dir");
    }
}
