package io.quarkiverse.snappy.it;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.xerial.snappy.BitShuffle;
import org.xerial.snappy.Snappy;
import org.xerial.snappy.SnappyFramedInputStream;
import org.xerial.snappy.SnappyFramedOutputStream;
import org.xerial.snappy.SnappyInputStream;
import org.xerial.snappy.SnappyOutputStream;

/**
 * Runs each Snappy operation inside the deployed process so the round trip executes against the
 * real (native) library, and returns {@code ok} only when the result is correct. The operation to
 * run is selected by path so a single parameterized test can exercise the whole API surface in both
 * JVM and native modes.
 */
@Path("/snappy/check")
@ApplicationScoped
public class SnappyResource {

    private final Map<String, Check> checks = new LinkedHashMap<>();

    public SnappyResource() {
        checks.put("native-library-version", this::nativeLibraryVersion);
        checks.put("byte-array-roundtrip", this::byteArrayRoundtrip);
        checks.put("length-and-validity-helpers", this::lengthAndValidityHelpers);
        checks.put("raw-offset-roundtrip", this::rawOffsetRoundtrip);
        checks.put("string-roundtrip", this::stringRoundtrip);
        checks.put("char-array-roundtrip", this::charArrayRoundtrip);
        checks.put("short-array-roundtrip", this::shortArrayRoundtrip);
        checks.put("int-array-roundtrip", this::intArrayRoundtrip);
        checks.put("long-array-roundtrip", this::longArrayRoundtrip);
        checks.put("float-array-roundtrip", this::floatArrayRoundtrip);
        checks.put("double-array-roundtrip", this::doubleArrayRoundtrip);
        checks.put("direct-bytebuffer-roundtrip", this::directByteBufferRoundtrip);
        checks.put("snappy-stream-roundtrip", this::snappyStreamRoundtrip);
        checks.put("snappy-framed-stream-roundtrip", this::snappyFramedStreamRoundtrip);
        checks.put("bitshuffle-int-roundtrip", this::bitShuffleIntRoundtrip);
        checks.put("bitshuffle-long-roundtrip", this::bitShuffleLongRoundtrip);
    }

    @GET
    @Path("/{operation}")
    @Produces(MediaType.TEXT_PLAIN)
    public String check(@PathParam("operation") String operation) {
        Check check = checks.get(operation);
        if (check == null) {
            return "unknown operation: " + operation;
        }
        try {
            check.run();
            return "ok";
        } catch (Throwable t) {
            return "fail: " + t;
        }
    }

    private void nativeLibraryVersion() {
        String version = Snappy.getNativeLibraryVersion();
        require(version != null && !version.isBlank(), "empty native library version");
    }

    private void byteArrayRoundtrip() throws Exception {
        byte[] original = sampleText().getBytes(StandardCharsets.UTF_8);
        byte[] restored = Snappy.uncompress(Snappy.compress(original));
        require(Arrays.equals(original, restored), "byte[] mismatch");
    }

    private void lengthAndValidityHelpers() throws Exception {
        byte[] original = sampleText().getBytes(StandardCharsets.UTF_8);
        byte[] compressed = Snappy.compress(original);
        require(Snappy.maxCompressedLength(original.length) >= compressed.length, "maxCompressedLength too small");
        require(Snappy.uncompressedLength(compressed) == original.length, "uncompressedLength mismatch");
        require(Snappy.isValidCompressedBuffer(compressed), "valid buffer reported invalid");
    }

    private void rawOffsetRoundtrip() throws Exception {
        byte[] original = sampleText().getBytes(StandardCharsets.UTF_8);
        byte[] compressed = new byte[Snappy.maxCompressedLength(original.length)];
        int compressedLength = Snappy.rawCompress(original, 0, original.length, compressed, 0);
        byte[] restored = new byte[Snappy.uncompressedLength(compressed, 0, compressedLength)];
        Snappy.rawUncompress(compressed, 0, compressedLength, restored, 0);
        require(Arrays.equals(original, restored), "raw offset mismatch");
    }

    private void stringRoundtrip() throws Exception {
        String original = sampleText();
        require(original.equals(Snappy.uncompressString(Snappy.compress(original))), "string mismatch");
    }

    private void charArrayRoundtrip() throws Exception {
        char[] original = sampleText().toCharArray();
        require(Arrays.equals(original, Snappy.uncompressCharArray(Snappy.compress(original))), "char[] mismatch");
    }

    private void shortArrayRoundtrip() throws Exception {
        short[] original = { -1, 0, 1, 12345, -12345, Short.MAX_VALUE, Short.MIN_VALUE };
        require(Arrays.equals(original, Snappy.uncompressShortArray(Snappy.compress(original))), "short[] mismatch");
    }

    private void intArrayRoundtrip() throws Exception {
        int[] original = { -1, 0, 1, 987654, -987654, Integer.MAX_VALUE, Integer.MIN_VALUE };
        require(Arrays.equals(original, Snappy.uncompressIntArray(Snappy.compress(original))), "int[] mismatch");
    }

    private void longArrayRoundtrip() throws Exception {
        long[] original = { -1L, 0L, 1L, 9_876_543_210L, Long.MAX_VALUE, Long.MIN_VALUE };
        require(Arrays.equals(original, Snappy.uncompressLongArray(Snappy.compress(original))), "long[] mismatch");
    }

    private void floatArrayRoundtrip() throws Exception {
        float[] original = { -1.5f, 0f, 3.14159f, Float.MAX_VALUE, Float.MIN_VALUE };
        require(Arrays.equals(original, Snappy.uncompressFloatArray(Snappy.compress(original))), "float[] mismatch");
    }

    private void doubleArrayRoundtrip() throws Exception {
        double[] original = { -1.5d, 0d, 2.718281828d, Double.MAX_VALUE, Double.MIN_VALUE };
        require(Arrays.equals(original, Snappy.uncompressDoubleArray(Snappy.compress(original))), "double[] mismatch");
    }

    private void directByteBufferRoundtrip() throws Exception {
        byte[] payload = sampleText().getBytes(StandardCharsets.UTF_8);
        ByteBuffer input = ByteBuffer.allocateDirect(payload.length);
        input.put(payload).flip();

        ByteBuffer compressed = ByteBuffer.allocateDirect(Snappy.maxCompressedLength(payload.length));
        Snappy.compress(input, compressed);

        ByteBuffer restored = ByteBuffer.allocateDirect(payload.length);
        Snappy.uncompress(compressed, restored);

        byte[] out = new byte[restored.remaining()];
        restored.get(out);
        require(Arrays.equals(payload, out), "direct ByteBuffer mismatch");
    }

    private void snappyStreamRoundtrip() throws Exception {
        byte[] original = sampleText().getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (SnappyOutputStream out = new SnappyOutputStream(buffer)) {
            out.write(original);
        }
        byte[] restored;
        try (SnappyInputStream in = new SnappyInputStream(new ByteArrayInputStream(buffer.toByteArray()))) {
            restored = in.readAllBytes();
        }
        require(Arrays.equals(original, restored), "SnappyStream mismatch");
    }

    private void snappyFramedStreamRoundtrip() throws Exception {
        byte[] original = sampleText().getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (SnappyFramedOutputStream out = new SnappyFramedOutputStream(buffer)) {
            out.write(original);
        }
        byte[] restored;
        try (SnappyFramedInputStream in = new SnappyFramedInputStream(new ByteArrayInputStream(buffer.toByteArray()))) {
            restored = in.readAllBytes();
        }
        require(Arrays.equals(original, restored), "SnappyFramedStream mismatch");
    }

    private void bitShuffleIntRoundtrip() throws Exception {
        int[] original = { 1, 2, 3, 100, 1000, 100000, -50 };
        require(Arrays.equals(original, BitShuffle.unshuffleIntArray(BitShuffle.shuffle(original))),
                "bitshuffle int[] mismatch");
    }

    private void bitShuffleLongRoundtrip() throws Exception {
        long[] original = { 1L, 2L, 3L, 100L, 1_000_000_000L, -50L };
        require(Arrays.equals(original, BitShuffle.unshuffleLongArray(BitShuffle.shuffle(original))),
                "bitshuffle long[] mismatch");
    }

    private static String sampleText() {
        return "quarkus-snappy native probe: fast compression over the JNI boundary. ".repeat(64);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    @FunctionalInterface
    private interface Check {
        void run() throws Exception;
    }
}
