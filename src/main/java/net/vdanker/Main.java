package net.vdanker;

import net.vdanker.mappers.FileMapper;
import net.vdanker.mappers.InputStreamMapper;
import net.vdanker.walker.CollectFilesVisitor;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;

public class Main {

    public static void main(String[] args) throws IOException {
        final CollectFilesVisitor visitor = new CollectFilesVisitor();
        Files.walkFileTree(
                FileSystems.getDefault().getPath("src"),
                visitor);

        visitor.stream()
                .filter(f -> f.getName().endsWith(".java"))
                .filter(f -> f.getAbsolutePath().contains("/Main.java"))
                .map(FileMapper::toInputStream)
                .map(InputStreamMapper::toJavaStats)
                .forEach(System.out::println);
    }
}
