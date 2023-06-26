package net.vdanker.mappers;

import net.vdanker.parser.JavaFileParser;
import net.vdanker.parser.Pair;
import net.vdanker.parser.model.JavaStats;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class InputStreamMapper {
    public static Pair<String, JavaStats> toJavaStats(InputStream is) {
        try {
            return JavaFileParser.parse(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
