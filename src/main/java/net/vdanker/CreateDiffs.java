package net.vdanker;

import net.vdanker.mappers.GitStreams;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.patch.FileHeader;
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

import static net.vdanker.WalkAllCommits.getExtension;
import static org.eclipse.jgit.diff.RawTextComparator.WS_IGNORE_ALL;

public class CreateDiffs {

    static String URL = "jdbc:h2:tcp://localhost:9092/./test";

    public static void main(String[] args) throws ClassNotFoundException, SQLException {
        createTable(URL);

        Path of = Path.of("../bare");
        File[] files = of.toFile().listFiles();
        List<File> list = Arrays.stream(files).filter(File::isDirectory).toList();
        list = list.stream().filter(l -> l.getName().equals("eqa-common-security2.git")).toList();

        list.forEach(l -> {
            System.out.println(l.getName());
            String name = l.getName().replaceAll("\\.git", "");
            saveDiffs(getDiffs(name, l.getAbsolutePath()));
        });
    }

    private static void saveDiffs(List<Diff> list) {
        try (Connection connection = DriverManager.getConnection(URL)) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO diffs(commit, proj, oldpath, newpath, changetype, filetype) VALUES (?, ?, ?, ?, ?, ?)")) {

                list.forEach(item -> {
                    try {
                        ps.setString(1, item.id);
                        ps.setString(2, item.proj);
                        ps.setString(3, item.oldPath);
                        ps.setString(4, item.newPath);
                        ps.setString(5, item.type);
                        ps.setString(6, item.fileType);
                        ps.addBatch();
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                });

                ps.executeBatch();
                ResultSet generatedKeys = ps.getGeneratedKeys();
                while (generatedKeys.next()) {
                    System.out.printf("%d", generatedKeys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    record Diff(String id, String proj, String oldPath, String newPath, String type, String fileType) {}

    private static List<Diff> getDiffs(String name, String dir) {
        List<Diff> result = new ArrayList<>();

        try (Repository repo = GitStreams.fromBareRepository(new File(dir)).getRepository()) {
            Ref head = repo.findRef("HEAD");
            if (head.getObjectId() == null) return result;

            try (RevWalk rw = new RevWalk(repo)) {
                rw.setRetainBody(false);
                rw.markStart(rw.parseCommit(head.getObjectId()));

                RevCommit c1 = null;
                RevTree t1 = null;
                RevTree t2;

                for (RevCommit c2 : rw) {
                    if (c2.getParentCount() > 1) continue;
                    t2 = c2.getTree();

                    if (t1 != null) {
                        try (ObjectReader reader = repo.newObjectReader()) {
                            CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
                            oldTreeIter.reset(reader, t2);

                            CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
                            newTreeIter.reset(reader, t1);

                            try (Git git = new Git(repo)) {
                                List<DiffEntry> diffs = git.diff()
                                        .setNewTree(newTreeIter)
                                        .setOldTree(oldTreeIter)
                                        .call();

                                for (DiffEntry entry : diffs) {
                                    if (!DiffEntry.ChangeType.MODIFY.equals(entry.getChangeType())) continue;
                                    String type = getExtension(entry.getNewPath());
                                    result.add(new Diff(
                                            c1.getId().getName(),
                                            name,
                                            entry.getOldPath(),
                                            entry.getNewPath(),
                                            entry.getChangeType().toString(),
                                            type));

                                    if ("java".equals(type)) {
                                        try (DiffFormatter df = new DiffFormatter(System.out)) {
                                            df.setRepository(repo);
                                            df.setContext(0);
                                            df.setDiffComparator(WS_IGNORE_ALL);
                                            df.format(entry);

                                            FileHeader fileHeader = df.toFileHeader(entry);
                                            for (Edit edit : fileHeader.toEditList()) {
                                                Edit.Type et = edit.getType();
                                                var a = edit.toString();
                                                System.out.println(a);
                                            }

                                        }
                                    }
                                }
                            } catch (GitAPIException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }

                    c1 = c2;
                    t1 = t2;
                }

                rw.dispose();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    private static void createTable(String url) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url)) {
            try (Statement s = connection.createStatement()) {
                s.execute("DROP TABLE IF EXISTS diffs");
                s.execute("""
                    CREATE TABLE diffs (
                        ID IDENTITY NOT NULL PRIMARY KEY,
                        commit VARCHAR(64) NOT NULL,
                        proj VARCHAR(64) NOT NULL,
                        changetype VARCHAR(64), 
                        oldpath VARCHAR(512),
                        newpath VARCHAR(512),
                        filetype VARCHAR(64)
                        )
                """);
            }
        }
    }
}
