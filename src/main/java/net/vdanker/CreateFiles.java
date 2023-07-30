package net.vdanker;

import net.vdanker.mappers.GitStreams;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.*;
import java.util.Date;
import java.util.*;

public class CreateFiles {

    static String URL = "jdbc:h2:tcp://localhost:9092/./test";

    public static void main(String[] args) throws ClassNotFoundException, SQLException {
        createTable(URL);

        Path of = Path.of("../bare");
        File[] files = of.toFile().listFiles();
        List<File> list = Arrays.stream(files).filter(File::isDirectory).toList();
//        list = list.stream().filter(l -> l.getName().equals("eqa-common-security2.git")).toList();

        list.forEach(l -> {
            System.out.println(l.getName());
            String name = l.getName().replaceAll("\\.git", "");

            saveFiles(getFiles(name, l.getAbsolutePath()));
        });
    }

    private static void saveFiles(List<Files> list) {
        try (Connection connection = DriverManager.getConnection(URL, "sa", "sa")) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO files(proj, filename, path, type) VALUES (?, ?, ?, ?)")) {

                list.forEach(item -> {
                    try {
                        ps.setString(1, item.proj);
                        ps.setString(2, item.filename);
                        ps.setString(3, item.path);
                        ps.setString(4, item.type);
                        ps.addBatch();
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                });

                ps.executeBatch();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    record Files(String proj, String filename, String path, String type) {}

    private static List<Files> getFiles(String name, String dir) {
        List<Files> result = new ArrayList<>();

        try (Repository repo = GitStreams.fromBareRepository(new File(dir)).getRepository()) {
            Ref head = repo.findRef("HEAD");
            if (head.getObjectId() == null) return result;

            try (RevWalk rw = new RevWalk(repo)) {
                rw.setRetainBody(false);

                RevCommit c = rw.parseCommit(head.getObjectId());
                RevTree tree = c.getTree();

                try (TreeWalk tw = new TreeWalk(repo)) {
                    tw.addTree(tree);
                    tw.setRecursive(true);
                    while (tw.next()) {
                        String type = getExtension(tw.getNameString());
                        result.add(new Files(name, tw.getNameString(), tw.getPathString(), type));
                    }
                }

                rw.dispose();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    public static String getExtension(String name) {
        if (name.endsWith(".") || name.startsWith(".")) return "?";
        return Optional.ofNullable(name)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(name.lastIndexOf('.') + 1))
                .orElse("?");
    }

    private static void createTable(String url) throws SQLException {
        try (Connection connection = DriverManager.getConnection(URL, "sa", "sa")) {
            try (Statement s = connection.createStatement()) {
                s.execute("DROP TABLE IF EXISTS files");
                s.execute("""
                    CREATE TABLE files (
                        ID IDENTITY NOT NULL PRIMARY KEY,
                        proj VARCHAR(64), 
                        filename VARCHAR(512),
                        path VARCHAR(512),
                        type VARCHAR(512)
                        )
                """);
            }
        }
    }
}
