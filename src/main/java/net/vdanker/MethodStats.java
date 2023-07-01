package net.vdanker;

import net.vdanker.mappers.GitStreams;
import net.vdanker.mappers.InputStreamMapper;
import net.vdanker.parser.model.JavaMethod;
import net.vdanker.parser.model.JavaStats;

import java.io.File;
import java.util.*;

import static java.util.stream.Collectors.*;

public class MethodStats {

    public static void main(String[] args) {
        var statistics = GitStreams.fromBareRepository(new File("../bare/eqa-apps-exams.git"))
                .streamTreeObjects()
                .filter(t -> t.name().endsWith("UnallocatedSuppResultsDto.java"))
                .filter(t -> !t.path().contains("/test/"))
                .map(InputStreamMapper::toJavaStats)
                .map(JavaStats::methods)
                .flatMap(Collection::stream)
                .toList();

        var allMethods = statistics.stream()
                .filter(e -> e.complexity() > 15)
                .collect(summarizingInt(JavaMethod::complexity));
        System.out.println(allMethods);

        statistics.stream()
                .filter(e -> e.complexity() > allMethods.getAverage())
                .filter(e -> !e.name().startsWith("get") && !e.name().startsWith("set"))
                .forEach(e -> System.out.printf(
                        "%s %d %d %d\n",
                        e.fqName(),
                        e.blockStatements(),
                        e.complexity(),
                        e.localVariableDeclarations()));
    }
}
