package net.vdanker;

import net.vdanker.mappers.FileMapper;
import net.vdanker.mappers.InputStreamMapper;
import net.vdanker.mappers.TestReducer;
import net.vdanker.parser.JavaAstViewer;
import net.vdanker.parser.Pair;
import net.vdanker.walker.CollectFilesVisitor;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;

public class AstViewer {

    public static void main(String[] args) throws IOException {
        final CollectFilesVisitor visitor = new CollectFilesVisitor();
        Files.walkFileTree(
                FileSystems.getDefault().getPath("src/main/java"),
                visitor);

        var context = visitor.stream()
                .filter(f -> f.getName().endsWith(".java"))
//                .filter(f -> f.getAbsolutePath().contains("/Main.java"))
                .map(FileMapper::toInputStream)
                .map(InputStreamMapper::toParser)
                .map(p -> new Pair<>(p, p.compilationUnit()))
                .reduce(new Pair<>(null, null), new TestReducer());

        JavaAstViewer.showAst(context);
    }
}
