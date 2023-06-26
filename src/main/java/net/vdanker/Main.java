package net.vdanker;

import net.vdanker.mappers.FileMapper;
import net.vdanker.mappers.InputStreamMapper;
import net.vdanker.parser.Pair;
import net.vdanker.parser.model.JavaStats;
import net.vdanker.walker.CollectFilesVisitor;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.List;

public class Main {

    public static void main(String[] args) throws IOException {
        final CollectFilesVisitor visitor = new CollectFilesVisitor();
        Files.walkFileTree(
                FileSystems.getDefault().getPath("src"),
                visitor);

        visitor.getFiles().stream()
                .filter(f -> f.getAbsolutePath().contains("JavaFileParser.java"))
                .map(FileMapper::toInputStream)
                .map(InputStreamMapper::toJavaStats)
                .forEach(System.out::println);
    }
}
