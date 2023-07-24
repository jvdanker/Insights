package net.vdanker;

import net.vdanker.mappers.GitStreams;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.h2.tools.Server;

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

    public static void main(String[] args) throws ClassNotFoundException, SQLException {
        Class.forName("org.h2.Driver");
        Server server = Server.createTcpServer("-tcpPort", "9092", "-tcpAllowOthers").start();
        System.out.println("Server started...");

        createTable("jdbc:h2:./test");

        Path of = Path.of("../bare");
        File[] files = of.toFile().listFiles();
        List<File> list = Arrays.stream(files).filter(File::isDirectory).toList();
        list.forEach(l -> {
            System.out.println(l.getName());
            saveCommits(
                    walkRepo(l.getName().replaceAll("\\.git", ""), l.getAbsolutePath())
            );
        });

        System.out.println("Done");
        new Scanner(System.in).next();
    }

    private static void saveCommits(List<Object[]> list) {
        try (Connection connection = DriverManager.getConnection("jdbc:h2:./test")) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO commits(proj, epoch, author) VALUES (?, ?, ?)")) {

                list.forEach(item -> {
                    try {
                        ps.setString(1, (String) item[0]);
                        ps.setInt(2, (Integer) item[1]);
                        ps.setString(3, (String) item[2]);
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

    private static List<Object[]> walkRepo(String name, String dir) {
        List<Object[]> result = new ArrayList<>();

        try (Repository repo = GitStreams.fromBareRepository(new File(dir)).getRepository()) {
            try (Git git = new Git(repo)) {
                Iterable<RevCommit> commits = git.log().all().call();

                for (RevCommit c : commits) {
                    if (c.getParentCount() > 1) continue;

                    result.add(
                            new Object[] {
                                    name,
                                    c.getCommitTime(),
                                    mapEmail(c.getCommitterIdent().getEmailAddress())
                            });
                }
            } catch (NoHeadException e) {
                // do nothing
            } catch (GitAPIException | IOException e) {
                throw new RuntimeException(e);
            }
        }

        return result;
    }

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

    private static void createTable(String url) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url)) {
            try (Statement s = connection.createStatement()) {
                s.execute("DROP TABLE IF EXISTS commits");
                s.execute("""
                    CREATE TABLE commits (
                        ID IDENTITY NOT NULL PRIMARY KEY,
                        proj VARCHAR(64), 
                        epoch INT, 
                        author VARCHAR(64))
                """);
            }
        }
    }
}
