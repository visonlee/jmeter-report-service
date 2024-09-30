package com.lws.jmeter.util;

import org.apache.commons.compress.archivers.*;
import org.apache.commons.compress.archivers.examples.Archiver;
import org.apache.commons.compress.archivers.examples.Expander;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.compress.utils.IOUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CompressUtils {

    private CompressUtils() {
    }

    public static void compressFile(Path file, Path destination) throws IOException, CompressorException {
        String format = FileNameUtils.getExtension(destination);

        try (OutputStream out = Files.newOutputStream(destination);
             BufferedOutputStream buffer = new BufferedOutputStream(out);
             CompressorOutputStream compressor = new CompressorStreamFactory()
                     .createCompressorOutputStream(format, buffer)) {
            IOUtils.copy(Files.newInputStream(file), compressor);
        }
    }

    public static void compressDirectory(String directory, String archive) throws IOException, ArchiveException {
        compressDirectory(Paths.get(directory), Paths.get(archive));
    }

    public static void compressDirectory(Path directory, Path archive) throws IOException, ArchiveException {
        String format = FileNameUtils.getExtension(archive);
        new Archiver().create(format, archive, directory);
    }

    public static void extract(String archive, String destinationDirectory) throws IOException, ArchiveException {
        extract(Paths.get(archive), Paths.get(destinationDirectory));
    }

    public static void extract(Path archive, Path destinationDirectory) throws IOException, ArchiveException {
        new Expander().expand(archive, destinationDirectory);
    }

}
