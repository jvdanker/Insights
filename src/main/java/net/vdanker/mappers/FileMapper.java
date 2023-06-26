package net.vdanker.mappers;

import net.vdanker.parser.JavaFileParser;
import net.vdanker.parser.Pair;
import net.vdanker.parser.model.JavaStats;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class FileMapper {
    public static InputStream toInputStream(File f) {
        try {
            return Files.newInputStream(f.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
