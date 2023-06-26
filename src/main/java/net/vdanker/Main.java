package net.vdanker;

import net.vdanker.parser.JavaFileParser;
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

        final List<File> files = visitor.getFiles();
        final List<File> filteredFiles = files.stream().filter(f -> f.getAbsolutePath().equals("/Users/juan/workspace/github.com/Insights/src/main/java/net/vdanker/parser/JavaFileParser.java")).toList();

        List<Pair<String, JavaStats>> collect = filteredFiles.stream()
                .map(Main::parseFile)
                .toList();

        collect.forEach(System.out::println);
    }

    static Pair<String, JavaStats> parseFile(File f) {
        try {
            return JavaFileParser.parse(Files.newInputStream(f.toPath()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
