package net.vdanker;

import net.vdanker.mappers.GitStreams;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.lib.Ref;
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
        list = list.stream().filter(l -> l.getName().equals("eqa-apps-claims.git")).toList();

        list.forEach(l -> {
            System.out.println(l.getName());
            String name = l.getName().replaceAll("\\.git", "");

            Optional<GetDiffsResult> diffs = getDiffs(name, l.getAbsolutePath());
            if (diffs.isPresent()) {
                saveDiffEntries(diffs.get().diffs());
                saveDiffsEdits(diffs.get().edits());
                saveDiffs(diffs.get().edits());
            }
        });
    }

    private static void saveDiffs(List<DiffEdit> list) {
        Map<String, String> diffs = list.stream().collect(
                Collectors.toMap(DiffEdit::commitId, DiffEdit::diff, (e1, e2) -> e1)
        );

        try (Connection connection = DriverManager.getConnection(URL, "sa", "sa")) {
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

    private static void saveDiffsEdits(List<DiffEdit> list) {
        try (Connection connection = DriverManager.getConnection(URL, "sa", "sa")) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO diffsedits(proj, commit, filename, editType, lines, linesFrom, linesTo) VALUES (?, ?, ?, ?, ?, ?, ?)")) {

                list.forEach(item -> {
                    item.edits().forEach(edit -> {
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

                            ps.setString(1, item.project());
                            ps.setString(2, item.commitId());
                            ps.setString(3, item.filename());
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
        try (Connection connection = DriverManager.getConnection(URL, "sa", "sa")) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO diffentries(commit1, commit2, proj, oldpath, newpath, changetype, filetype) VALUES (?, ?, ?, ?, ?, ?, ?)")) {

                list.forEach(item -> {
                    try {
                        ps.setString(1, item.commit1Id());
                        ps.setString(2, item.commit2Id());
                        ps.setString(3, item.proj());
                        ps.setString(4, item.oldPath());
                        ps.setString(5, item.newPath());
                        ps.setString(6, item.type());
                        ps.setString(7, item.fileType());
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

    private static Optional<GetDiffsResult> getDiffs(String name, String dir) {
        List<DiffEntry> diffEntries = new ArrayList<>();
        List<DiffEdit> diffEdits = new ArrayList<>();

        try (Repository repo = GitStreams.fromBareRepository(new File(dir)).getRepository()) {
            Ref head = repo.findRef(Constants.HEAD); //"refs/heads/main");
            if (head.getObjectId() == null) return Optional.empty();

            try (RevWalk rw = new RevWalk(repo)) {
                rw.setRetainBody(false);
                rw.setFirstParent(true);
                rw.markStart(rw.parseCommit(head.getObjectId()));

                RevCommit c1 = null;
                RevTree t1 = null;
                RevTree t2;

                for (RevCommit c2 : rw) {
                    t2 = c2.getTree();

                    if (t1 != null) {
                        getDiffsBetweenTwoTrees(
                                name,
                                diffEntries,
                                diffEdits,
                                repo,
                                c1,
                                t1,
                                t2,
                                c2);
                    }

                    c1 = c2;
                    t1 = t2;
                }

                if (t1 != null) {
                    getDiffsBetweenTwoTrees(name,
                            diffEntries,
                            diffEdits,
                            repo,
                            c1,
                            t1,
                            Constants.EMPTY_TREE_ID,
                            null);
                }

                rw.dispose();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return Optional.of(new GetDiffsResult(diffEntries, diffEdits));
    }

    private static void getDiffsBetweenTwoTrees(
            String name,
            List<DiffEntry> diffEntries,
            List<DiffEdit> diffEdits,
            Repository repo,
            RevCommit c1,
            AnyObjectId t1,
            AnyObjectId t2,
            RevCommit c2) throws IOException {

        try (ObjectReader reader = repo.newObjectReader()) {
            CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
            if (!t2.equals(Constants.EMPTY_TREE_ID)) {
                oldTreeIter.reset(reader, t2);
            }

            CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
            newTreeIter.reset(reader, t1);

            try (Git git = new Git(repo)) {
                List<org.eclipse.jgit.diff.DiffEntry> diffs = git.diff()
                        .setNewTree(newTreeIter)
                        .setOldTree(oldTreeIter)
                        .call();

                for (org.eclipse.jgit.diff.DiffEntry entry : diffs) {
                    String type = getExtension(entry.getNewPath());

                    diffEntries.add(new DiffEntry(
                            c1.getId().getName(),
                            c2 == null ? "" : c2.getId().getName(),
                            name,
                            entry.getOldPath(),
                            entry.getNewPath(),
                            entry.getChangeType().toString(),
                            type));

                    if ("java".equals(type)) {
                        DiffEdit diffEdit = createDiffEdits(name, repo, c2, entry);
                        diffEdits.add(diffEdit);
                    }
                }
            } catch (GitAPIException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static DiffEdit createDiffEdits(
            String name,
            Repository repo,
            RevCommit commit,
            org.eclipse.jgit.diff.DiffEntry entry) throws IOException {

        OutputStream bos = new ByteArrayOutputStream();
        try (DiffFormatter df = new DiffFormatter(bos)) {
            df.setRepository(repo);
            df.setContext(0);
            df.setDiffComparator(WS_IGNORE_ALL);
            df.format(entry);

            FileHeader fileHeader = df.toFileHeader(entry);
            return new DiffEdit(
                    name,
                    commit.getId().getName(),
                    entry.getNewPath(),
                    fileHeader.toEditList(),
                    bos.toString());
        }
    }

    private static void createTable(String url) throws SQLException {
        try (Connection connection = DriverManager.getConnection(URL, "sa", "sa")) {
            try (Statement s = connection.createStatement()) {
                s.execute("DROP TABLE IF EXISTS diffentries");
                s.execute("""
                    CREATE TABLE diffentries (
                        ID IDENTITY NOT NULL PRIMARY KEY,
                        commit1 VARCHAR(64) NOT NULL,
                        commit2 VARCHAR(64) NOT NULL,
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
