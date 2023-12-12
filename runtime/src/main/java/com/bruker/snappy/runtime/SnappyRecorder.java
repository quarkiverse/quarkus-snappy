package com.bruker.snappy.runtime;

import java.io.*;
import java.net.URL;

import org.xerial.snappy.OSInfo;
import org.xerial.snappy.SnappyLoader;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class SnappyRecorder {

    private static boolean hasResource(String path) {
        return SnappyLoader.class.getResource(path) != null;
    }

    private static File extractLibraryFile(URL library, String name) {
        String tmp = System.getProperty("java.io.tmpdir");
        File extractedLibFile = new File(tmp, name);

        try (BufferedInputStream inputStream = new BufferedInputStream(library.openStream());
                FileOutputStream fileOS = new FileOutputStream(extractedLibFile)) {
            byte[] data = new byte[8192];
            int byteContent;
            while ((byteContent = inputStream.read(data, 0, 8192)) != -1) {
                fileOS.write(data, 0, byteContent);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Unable to extract native library " + name + " to " + extractedLibFile.getAbsolutePath(), e);
        }

        extractedLibFile.deleteOnExit();

        return extractedLibFile;
    }

    public void loadSnappy() {
        // Resolve the library file name with a suffix (e.g., dll, .so, etc.)
        String snappyNativeLibraryName = System.mapLibraryName("snappyjava");
        String snappyNativeLibraryPath = "/org/xerial/snappy/native/" + OSInfo.getNativeLibFolderPathForCurrentOS();
        boolean hasNativeLib = hasResource(snappyNativeLibraryPath + "/" + snappyNativeLibraryName);

        if (!hasNativeLib) {
            String errorMessage = String.format("no native library is found for os.name=%s and os.arch=%s", OSInfo.getOSName(),
                    OSInfo.getArchName());
            throw new RuntimeException(errorMessage);
        }

        File out = extractLibraryFile(
                SnappyLoader.class.getResource(snappyNativeLibraryPath + "/" + snappyNativeLibraryName),
                snappyNativeLibraryName);

        System.load(out.getAbsolutePath());
    }
}
