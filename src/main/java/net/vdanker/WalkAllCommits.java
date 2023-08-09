package net.vdanker;

import net.vdanker.mappers.GitStreams;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.util.*;

public class WalkAllCommits {

    static String URL = "jdbc:h2:tcp://localhost:9092/./test";

    public static void main(String[] args) throws ClassNotFoundException, SQLException {
        createTable(URL);

        File[] files = Path.of("../bare")
                .toFile()
                .listFiles();

        List<File> list = Arrays.stream(files)
                .filter(File::isDirectory)
                .filter(l -> l.getName().equals("test.git"))
                .toList();

        list.forEach(l -> {
            System.out.println(l.getName());
            String name = l.getName().replaceAll("\\.git", "");

            saveCommits(collectAllCommits(name, l.getAbsolutePath()));
        });
    }

    static void saveCommits(List<Commit> list) {
        try (Connection connection = DriverManager.getConnection(URL, "sa", "sa")) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO commits(proj, commit_id, epoch, author) VALUES (?, ?, ?, ?)")) {

                list.forEach(item -> {
                    try {
                        ps.setString(1, item.name);
                        ps.setString(2, item.commitId);
                        ps.setDate(3, new java.sql.Date(item.date.getTime()));
                        ps.setString(4, item.author);
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

    public static String getExtension(String name) {
        if (name.endsWith(".") || name.startsWith(".")) return "?";
        return Optional.ofNullable(name)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(name.lastIndexOf('.') + 1))
                .orElse("?");
    }

    static List<Commit> collectAllCommits(String name, String dir) {
        List<Commit> result = new ArrayList<>();

        try (Repository repo = GitStreams.fromBareRepository(new File(dir)).getRepository()) {
            Ref head = repo.findRef("HEAD");
            if (head == null || head.getObjectId() == null) return result;

            try (RevWalk rw = new RevWalk(repo)) {
                rw.markStart(rw.parseCommit(head.getObjectId()));

                for (RevCommit c : rw) {
                    PersonIdent committerIdent = c.getCommitterIdent();
                    String emailAddress = "".equals(committerIdent.getEmailAddress())
                            ? committerIdent.getName()
                            : committerIdent.getEmailAddress();

                    Date epoch = new Date(c.getCommitTime() * 1000L);
                    result.add(new Commit(name, c.getId().getName(), epoch, mapEmail(emailAddress)));
                }

                rw.dispose();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    record Commit(String name, String commitId, Date date, String author) {}

    private static String mapEmail(String email) {
        String[] split = email.split("@");
        if (split.length > 0) {
            String replace = split[0].toLowerCase().replace('.', ' ');
            if ("marcusm".equals(replace)) return "marcus manning";
            if ("oliverl".equals(replace)) return "oliverlayug";
            if ("rodrigod".equals(replace)) return "rodrigo desouza";
            if ("yashmeek".equals(replace)) return "yashmeet kaur";

            return replace;
        }

        return email;
    }

    static void createTable(String url) throws SQLException {
        try (Connection connection = DriverManager.getConnection(URL, "sa", "sa")) {
            try (Statement s = connection.createStatement()) {
                s.execute("DROP TABLE IF EXISTS commits");
                s.execute("DROP INDEX IF EXISTS idx_commits_commitId");

                s.execute("""
                    CREATE TABLE commits (
                        id IDENTITY NOT NULL PRIMARY KEY,
                        commit_id VARCHAR(64), 
                        proj VARCHAR(64), 
                        epoch DATE, 
                        author VARCHAR(64))
                """);

                s.execute("CREATE INDEX idx_commits_commitId ON COMMITS(commit_id)");
            }
        }
    }
}
