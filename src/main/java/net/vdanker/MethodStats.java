package net.vdanker;

import net.vdanker.mappers.FileMapper;
import net.vdanker.mappers.GitStreams;
import net.vdanker.mappers.InputStreamMapper;
import net.vdanker.parser.model.GitTreeObject;
import net.vdanker.parser.model.JavaMethod;
import net.vdanker.walker.CollectFilesVisitor;
import org.eclipse.jgit.lib.ObjectLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.*;

public class MethodStats {

    public static void main(String[] args) {
        Map<String, IntSummaryStatistics> statistics =
                GitStreams.fromBareRepository(new File("/Users/juan/workspace/nzqa-bare/eqa-apps-exams.git"))
                        .streamTreeObjects()
                        .filter(t -> t.name().endsWith(".java"))
                        .filter(t -> t.path().contains("/test/"))
                        .map(GitTreeObject::is)
                        .map(InputStreamMapper::toJavaStats)
                        .map(js -> js.getValue().methods())
                        .flatMap(Collection::stream)
                        .collect(groupingBy(JavaMethod::name, summarizingInt(JavaMethod::blockStatements)));

        statistics.entrySet().stream()
                .filter(e -> e.getValue().getCount() > 1)
                .forEach(e -> System.out.println(e.getKey() + " " + e.getValue()));
    }
}
