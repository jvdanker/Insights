package net.vdanker;

import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DbService {
    static void createTables(String url) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, "sa", "sa")) {
            try (Statement s = connection.createStatement()) {
                s.execute("DROP TABLE IF EXISTS diffentries");
                s.execute("DROP INDEX IF EXISTS idx_diffentries_commit1");
//                s.execute("DROP INDEX IF EXISTS idx_diffentries_commit2");
//                s.execute("DROP INDEX IF EXISTS idx_diffentries_commit3");
                s.execute("""
                    CREATE TABLE diffentries (
                        ID IDENTITY NOT NULL PRIMARY KEY,
                        commit1 VARCHAR(64) NOT NULL,
                        commit2 VARCHAR(64) NOT NULL,
                        oldId VARCHAR(64) NOT NULL,
                        newId VARCHAR(64) NOT NULL,
                        project VARCHAR(64) NOT NULL,
                        fullPath VARCHAR(1024) NOT NULL,
                        changetype VARCHAR(64)
                        )
                """);
                s.execute("CREATE INDEX idx_diffentries_commit1 ON diffentries(commit1)");
//                s.execute("CREATE INDEX idx_diffentries_commit2 ON diffentries(commit1, filetype)");
//                s.execute("CREATE INDEX idx_diffentries_commit3 ON diffentries(filetype)");

                s.execute("DROP TABLE IF EXISTS diffsedits");
                s.execute("DROP INDEX IF EXISTS idx_diffsedits_commit");
                s.execute("DROP INDEX IF EXISTS idx_diffsedits_fileid");
                s.execute("""
                    CREATE TABLE diffsedits (
                        ID IDENTITY NOT NULL PRIMARY KEY,
                        commit VARCHAR(64) NOT NULL,
                        fileId VARCHAR(64) NOT NULL,
                        project VARCHAR(64) NOT NULL,
                        fullPath VARCHAR(256) NOT NULL,
                        editType VARCHAR(64), 
                        lines INT,
                        linesFrom INT,
                        linesTo INT,
                        beginA INT,
                        endA INT,
                        beginB INT,
                        endB INT
                        )
                """);
                s.execute("CREATE INDEX idx_diffsedits_commit ON diffsedits(commit)");
                s.execute("CREATE INDEX idx_diffsedits_fileid ON diffsedits(fileId)");

                s.execute("DROP TABLE IF EXISTS diffs");
                s.execute("DROP INDEX IF EXISTS idx_diffs_commit");
                s.execute("""
                    CREATE TABLE diffs (
                        commit VARCHAR(64) NOT NULL PRIMARY KEY,
                        diff VARCHAR2
                        )
                """);
                s.execute("CREATE INDEX idx_diffs_commit ON diffs(commit)");
            }
        }
    }

    static void saveDiffs(List<DiffEntry> diffEntries) {
//        System.out.println(list);
//        System.out.println("------------------");
        Map<String, String> diffs = diffEntries.stream()
                .map(DiffEntry::diffEdits).collect(
                Collectors.toMap(DiffEdits::commitId, DiffEdits::diff, (e1, e2) -> e1 + "\n" + e2)
        );

        try (Connection connection = DriverManager.getConnection(CreateDiffs.URL, "sa", "sa")) {
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

    static void saveDiffsEdits(List<DiffEntry> diffEntries) {
        try (Connection connection = DriverManager.getConnection(CreateDiffs.URL, "sa", "sa")) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO diffsedits(" +
                            "commit, fileId, project, fullpath, editType, lines, linesFrom, linesTo, beginA, endA, beginB, endB) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ? ,?, ?, ?)")) {

                diffEntries.stream()
                        .map(DiffEntry::diffEdits)
                        .flatMap(item -> item.edits().stream().map(item::toDiffEdit))
                        .forEach(item -> addDiffEditToBatch(ps, item));

                ps.executeBatch();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static void addDiffEditToBatch(PreparedStatement ps, DiffEdit item) {
        try {
            ps.setString(1, item.commitId());
            ps.setString(2, item.fileId());
            ps.setString(3, item.project());
            ps.setString(4, item.fullPath());
            ps.setString(5, item.changeType());
            ps.setInt(6, item.lines());
            ps.setInt(7, item.linesFrom());
            ps.setInt(8, item.linesTo());
            ps.setInt(9, item.beginA());
            ps.setInt(10, item.endA());
            ps.setInt(11, item.beginB());
            ps.setInt(12, item.endB());
            ps.addBatch();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    static void saveDiffEntries(List<DiffEntry> diffEntries) {
        try (Connection connection = DriverManager.getConnection(CreateDiffs.URL, "sa", "sa")) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO diffentries(" +
                            "commit1, commit2, oldId, newId, project, fullPath, changetype) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?)")) {

                diffEntries.forEach(item -> {
                    try {
                        ps.setString(1, item.commit1Id());
                        ps.setString(2, item.commit2Id());
                        ps.setString(3, item.oldId());
                        ps.setString(4, item.newId());
                        ps.setString(5, item.project());
                        ps.setString(6, item.fullPath());
                        ps.setString(7, item.changeType());
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
}
