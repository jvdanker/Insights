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

        final List<File> files = visitor.getFiles();
        final List<File> filteredFiles = files.stream().filter(f -> f.getAbsolutePath().equals("/Users/juan/workspace/github.com/Insights/src/main/java/net/vdanker/parser/JavaFileParser.java")).toList();

        List<Pair<String, JavaStats>> collect = filteredFiles.stream()
                .map(FileMapper::toInputStream)
                .map(InputStreamMapper::toJavaStats)
                .toList();

        collect.forEach(System.out::println);
    }


}
