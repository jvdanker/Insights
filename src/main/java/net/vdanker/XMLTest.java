package net.vdanker;

import net.vdanker.mappers.GitStreams;
import net.vdanker.mappers.InputStreamMapper;
import net.vdanker.parser.model.JavaMethod;
import net.vdanker.parser.model.JavaStats;

import java.io.File;
import java.util.Collection;

import static java.util.stream.Collectors.summarizingInt;

public class XMLTest {

    public static void main(String[] args) {
        var list = GitStreams.fromBareRepository(new File("../bare/eqa-apps-exams.git"))
                .streamTreeObjects()
                .filter(t -> t.name().endsWith(".xml"))
                .filter(t -> !t.path().contains("/test/"))
                .filter(t -> t.path().startsWith("war/"))
                .filter(t -> t.name().equals("struts-config.xml"))
                .map(InputStreamMapper::parseXml)
                .toList()
                .stream().findFirst();
        System.out.println(list);
    }
}
