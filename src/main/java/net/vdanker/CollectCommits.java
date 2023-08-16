package net.vdanker;

import java.io.File;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static net.vdanker.CollectFiles.collectAllFiles;
import static net.vdanker.CollectFiles.saveRepoFiles;
import static net.vdanker.WalkAllCommits.*;

public class CollectCommits {

    static String URL = "jdbc:h2:tcp://localhost:9092/./test";

    public static void main(String[] args) throws ClassNotFoundException, SQLException {
        WalkAllCommits.createTable(URL);
        DbService.createTables(URL);
        CollectFiles.createTables(URL);

        File[] files = Path.of("../bare")
                .toFile()
                .listFiles();

        List<File> list = Arrays.stream(files)
                .filter(File::isDirectory)
//                .filter(l -> l.getName().equals("test.git"))
                .filter(l -> l.getName().equals("eqa-apps-exams.git"))
//                .filter(l -> apps.contains(l.getName()))
                .toList();

        list.forEach(l -> {
            System.out.println(l.getName());
            String name = l.getName().replaceAll("\\.git", "");

            System.out.println("Collecting commits...");
            saveCommits(collectAllCommits(name, l.getAbsolutePath()));

            System.out.println("Collecting files...");
            saveRepoFiles(collectAllFiles(name, l.getAbsolutePath()));

            System.out.println("Collecting diffs...");
            CreateDiffs app = new CreateDiffs();
            app.scanAndSave(name, l.getAbsolutePath());

            System.out.println("Done.");
        });
    }
}
