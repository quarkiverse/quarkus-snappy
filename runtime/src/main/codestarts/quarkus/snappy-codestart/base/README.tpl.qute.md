{#include readme-header /}

## Snappy

This project uses the Snappy extension, which bundles the native Snappy library and makes it work
out of the box in both JVM and native mode.

Compression and decompression go through the `org.xerial.snappy.Snappy` API:

```java
import org.xerial.snappy.Snappy;

byte[] compressed = Snappy.compress("Hello, Snappy!".getBytes(StandardCharsets.UTF_8));
byte[] restored = Snappy.uncompress(compressed);
```

The library reads a few settings from `org.xerial.snappy.*` system properties. The extension exposes
them as `quarkus.snappy.*` configuration so they are also honored in native executables, for example:

```properties
# Extract the bundled native library somewhere executable when /tmp is mounted noexec
quarkus.snappy.temp-dir=/var/tmp
```

See the [extension documentation](https://docs.quarkiverse.io/quarkus-snappy/dev/) for the full
configuration reference.
