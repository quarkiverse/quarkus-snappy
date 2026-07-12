# Quarkus Snappy

[![Version](https://img.shields.io/maven-central/v/io.quarkiverse.snappy/quarkus-snappy?logo=apache-maven&style=flat-square)](https://search.maven.org/artifact/io.quarkiverse.snappy/quarkus-snappy)

Quarkus Snappy provides [Snappy](https://github.com/xerial/snappy-java) compression and decompression for Quarkus applications, bundling the native library so it also works in native executables without extra configuration.

## Installation

Add the extension to your project:

```xml
<dependency>
    <groupId>io.quarkiverse.snappy</groupId>
    <artifactId>quarkus-snappy</artifactId>
    <version>{project-version}</version>
</dependency>
```

## Usage

Once the extension is added, Snappy is automatically available in your application:

```java
import org.xerial.snappy.Snappy;

// Compress
byte[] compressed = Snappy.compress(data);

// Decompress
byte[] decompressed = Snappy.uncompress(compressed);
```

## Configuration

Snappy's own knobs are exposed as Quarkus configuration properties, honored in native executables as well:

```properties
# Disable the caching buffer pool used by the framed streams
quarkus.snappy.pool-disable=true

# Extract the bundled native library somewhere executable when /tmp is mounted noexec
quarkus.snappy.temp-dir=/var/tmp
```

## Documentation

Full documentation is available at <https://docs.quarkiverse.io/quarkus-snappy/dev/>.
