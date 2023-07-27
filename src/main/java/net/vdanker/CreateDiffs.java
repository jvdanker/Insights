package net.vdanker;

import net.vdanker.mappers.GitStreams;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import java.io.*;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

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

            Optional<GetDiffsResult> diffs = getDiffs(name, l.getAbsolutePath());
            if (diffs.isPresent()) {
                saveDiffEntries(diffs.get().diffs());
                saveDiffsEdits(diffs.get().edits);
                saveDiffs(diffs.get().edits);
            }
        });
    }

    private static void saveDiffs(List<EditsResult> list) {
        Map<String, String> diffs = list.stream().collect(
                Collectors.toMap(e -> e.commitId, e -> e.diff, (e1, e2) -> e1)
        );

        try (Connection connection = DriverManager.getConnection(URL)) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO diffs (commit, diff) VALUES (?, ?)")) {

                diffs.forEach((k,v) -> {
                    try {
                        ps.setString(1, k);
                        ps.setString(2, v);
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

    private static void saveDiffsEdits(List<EditsResult> list) {
        try (Connection connection = DriverManager.getConnection(URL)) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO diffsedits(proj, commit, filename, editType, lines, linesFrom, linesTo) VALUES (?, ?, ?, ?, ?, ?, ?)")) {

                list.forEach(item -> {
                    item.edits.forEach(edit -> {
                        try {
                            /*
                            An edit where beginA == endA && beginB < endB is an insert edit,
                            that is sequence B inserted the elements in region [beginB, endB) at beginA.

                            An edit where beginA < endA && beginB == endB is a delete edit,
                            that is sequence B has removed the elements between [beginA, endA).

                            An edit where beginA < endA && beginB < endB is a replace edit,
                            that is sequence B has replaced the range of elements between [beginA, endA) with those found in [beginB, endB).
                             */
                            /*
                            INSERT(43-43,43-45)
                            REPLACE(44-45,46-50)
                            INSERT(69-69,74-76)
                            REPLACE(70-71,77-78)
                            INSERT(72-72,79-82)
                            REPLACE(73-77,83-87)
                            REPLACE(78-81,88-93)
                            REPLACE(83-85,95-100)
                            REPLACE(87-88,102-107)
                            REPLACE(90-93,109-111)
                             */
                            int lines = 0;
                            Edit.Type type = edit.getType();

                            switch (type) {
                                case INSERT -> lines = (edit.getEndB() - edit.getBeginB());
                                case DELETE -> lines = (edit.getEndA() - edit.getBeginA());
                                case REPLACE -> lines = (edit.getEndB() - edit.getBeginB()) - (edit.getEndA() - edit.getBeginA());
                            }

                            if (lines < 0) {
                                type = Edit.Type.DELETE;
                                lines *= -1;
                            }

                            int linesFrom = (edit.getEndA() - edit.getBeginA());
                            int linesTo = (edit.getEndB() - edit.getBeginB());

                            ps.setString(1, item.project);
                            ps.setString(2, item.commitId);
                            ps.setString(3, item.filename);
                            ps.setString(4, type.toString());
                            ps.setInt(5, lines);
                            ps.setInt(6, linesFrom);
                            ps.setInt(7, linesTo);
                            ps.addBatch();
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    });
                });

                ps.executeBatch();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static void saveDiffEntries(List<DiffEntry> list) {
        try (Connection connection = DriverManager.getConnection(URL)) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO diffentries(commit, proj, oldpath, newpath, changetype, filetype) VALUES (?, ?, ?, ?, ?, ?)")) {

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
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    record DiffEntry(String id, String proj, String oldPath, String newPath, String type, String fileType) {}

    record GetDiffsResult(List<DiffEntry> diffs, List<EditsResult> edits) {}

    private static Optional<GetDiffsResult> getDiffs(String name, String dir) {
        List<DiffEntry> diffEntries = new ArrayList<>();
        List<EditsResult> editsResult = new ArrayList<>();

        try (Repository repo = GitStreams.fromBareRepository(new File(dir)).getRepository()) {
            Ref head = repo.findRef("HEAD");
            if (head.getObjectId() == null) return Optional.empty();

            try (RevWalk rw = new RevWalk(repo)) {
                rw.setRetainBody(false);
                rw.markStart(rw.parseCommit(head.getObjectId()));

                RevCommit c1 = null;
                RevTree t1 = null;
                RevTree t2;

                for (RevCommit c2 : rw) {
                    if (c2.getParentCount() > 1) continue;
                    t2 = c2.getTree();

                    if (t1 != null) { // && c1.getId().getName().equals("064e628d8aa172f9a57762d36242baa851da3f12")) {
                        try (ObjectReader reader = repo.newObjectReader()) {
                            CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
                            oldTreeIter.reset(reader, t2);

                            CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
                            newTreeIter.reset(reader, t1);

                            try (Git git = new Git(repo)) {
                                List<org.eclipse.jgit.diff.DiffEntry> diffs = git.diff()
                                        .setNewTree(newTreeIter)
                                        .setOldTree(oldTreeIter)
                                        .call();

                                for (org.eclipse.jgit.diff.DiffEntry entry : diffs) {
                                    if (!org.eclipse.jgit.diff.DiffEntry.ChangeType.MODIFY.equals(entry.getChangeType())) continue;

                                    String type = getExtension(entry.getNewPath());

                                    diffEntries.add(new DiffEntry(
                                            c1.getId().getName(),
                                            name,
                                            entry.getOldPath(),
                                            entry.getNewPath(),
                                            entry.getChangeType().toString(),
                                            type));

                                    if ("java".equals(type)) {
                                        OutputStream bos = new ByteArrayOutputStream();
                                        try (DiffFormatter df = new DiffFormatter(bos)) {
                                            df.setRepository(repo);
                                            df.setContext(0);
                                            df.setDiffComparator(WS_IGNORE_ALL);
                                            df.format(entry);

                                            FileHeader fileHeader = df.toFileHeader(entry);
                                            editsResult.add(new EditsResult(
                                                    name,
                                                    c1.getId().getName(),
                                                    entry.getNewPath(),
                                                    fileHeader.toEditList(),
                                                    bos.toString()));

                                            for (Edit edit : fileHeader.toEditList()) {
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

        return Optional.of(new GetDiffsResult(diffEntries, editsResult));
    }

    record EditsResult(String project, String commitId, String filename, List<Edit> edits, String diff) {}

    private static void createTable(String url) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url)) {
            try (Statement s = connection.createStatement()) {
                s.execute("DROP TABLE IF EXISTS diffentries");
                s.execute("""
                    CREATE TABLE diffentries (
                        ID IDENTITY NOT NULL PRIMARY KEY,
                        commit VARCHAR(64) NOT NULL,
                        proj VARCHAR(64) NOT NULL,
                        changetype VARCHAR(64), 
                        oldpath VARCHAR(512),
                        newpath VARCHAR(512),
                        filetype VARCHAR(64)
                        )
                """);

                s.execute("DROP TABLE IF EXISTS diffsedits");
                s.execute("""
                    CREATE TABLE diffsedits (
                        ID IDENTITY NOT NULL PRIMARY KEY,
                        commit VARCHAR(64) NOT NULL,
                        proj VARCHAR(64) NOT NULL,
                        filename VARCHAR(256) NOT NULL,
                        editType VARCHAR(64), 
                        lines INT,
                        linesFrom INT,
                        linesTo INT
                        )
                """);

                s.execute("DROP TABLE IF EXISTS diffs");
                s.execute("""
                    CREATE TABLE diffs (
                        commit VARCHAR(64) NOT NULL PRIMARY KEY,
                        diff VARCHAR2
                        )
                """);
            }
        }
    }
}
