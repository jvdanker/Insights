package net.vdanker;

import java.io.File;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static net.vdanker.WalkAllCommits.*;

public class CollectCommits {

    static String URL = "jdbc:h2:tcp://localhost:9092/./test";

    public static void main(String[] args) throws ClassNotFoundException, SQLException {
        WalkAllCommits.createTable(URL);
        DbService.createTables(URL);

        File[] files = Path.of("../bare")
                .toFile()
                .listFiles();

        List<File> list = Arrays.stream(files)
                .filter(File::isDirectory)
                .filter(l -> l.getName().equals("eqa-apps-exams.git"))
                .toList();

        list.forEach(l -> {
            System.out.println(l.getName());
            String name = l.getName().replaceAll("\\.git", "");

            saveCommits(collectAllCommits(name, l.getAbsolutePath()));

            saveCommits(collectAllCommits(name, l.getAbsolutePath()));

            CreateDiffs app = new CreateDiffs();
            app.scanAndSave(name, l.getAbsolutePath());
        });
    }
}
