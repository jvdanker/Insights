package net.vdanker;

import net.vdanker.mappers.FileMapper;
import net.vdanker.mappers.InputStreamMapper;
import net.vdanker.walker.CollectFilesVisitor;
import org.antlr.v4.runtime.Token;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

public class TokenStatistics {
    public static void main(String[] args) throws IOException {
        final CollectFilesVisitor visitor = new CollectFilesVisitor();
        Files.walkFileTree(
                FileSystems.getDefault().getPath("src/main/java"),
                visitor);

        visitor.stream()
                .filter(f -> f.getName().endsWith(".java"))
                .map(FileMapper::toInputStream)
                .map(InputStreamMapper::toTokenEmitter)
                .flatMap(Collection::stream)
                .filter(t -> t.getChannel() == 0)
                .collect(groupingBy(Token::getType, groupingBy(Token::getText, counting())))
                .forEach((k,v) -> System.out.printf("%s - %s\n", k, v));
    }
}
