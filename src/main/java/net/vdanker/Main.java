package net.vdanker;

import net.vdanker.mappers.FileMapper;
import net.vdanker.mappers.InputStreamMapper;
import net.vdanker.parser.JavaAstViewer;
import net.vdanker.parser.Pair;
import net.vdanker.parser.model.JavaStats;
import net.vdanker.walker.CollectFilesVisitor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.stream.Stream;

public class Main {

    public static void main(String[] args) throws IOException {
        final CollectFilesVisitor visitor = new CollectFilesVisitor();
        Files.walkFileTree(
                FileSystems.getDefault().getPath("src"),
                visitor);

        Stream<JavaStats> pairStream = visitor.stream()
                .filter(f -> f.getName().endsWith(".java"))
                .filter(f -> f.getAbsolutePath().contains("/Main.java"))
                .map(FileMapper::toInputStream)
                .map(InputStreamMapper::toJavaStats);

//        pairStream.forEach(System.out::println);

        pairStream.forEach(p -> {
            System.out.println(p.fqClassName());
            p.methods().forEach(m -> {
                System.out.printf("  %s - %d\n", m.methodName(), m.blockStatements());

                m.methodCalls().forEach(mc -> System.out.printf("    %s\n", mc));
            });
        });
    }
}
