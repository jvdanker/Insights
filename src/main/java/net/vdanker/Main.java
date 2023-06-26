package net.vdanker;

import net.vdanker.parser.JavaFileParser;
import net.vdanker.parser.model.JavaMethod;
import net.vdanker.parser.Pair;
import net.vdanker.parser.model.JavaStats;
import net.vdanker.walker.CollectFiles;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Main {

    public static void main(String[] args) throws IOException {
        Path startingDir = FileSystems.getDefault().getPath("src");

        CollectFiles c = new CollectFiles();
        Files.walkFileTree(startingDir, c);

        List<File> files = c.getFiles();
        files = files.stream().filter(f -> f.getAbsolutePath().equals("/Users/juan/workspace/github.com/Insights/src/main/java/net/vdanker/parser/JavaFileParser.java")).toList();
        List<Pair<String, JavaStats>> collect = files.stream()
                .map(f -> {
                    try {
                        return JavaFileParser.parse(Files.newInputStream(f.toPath()));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();

        collect.forEach(System.out::println);
    }
}
