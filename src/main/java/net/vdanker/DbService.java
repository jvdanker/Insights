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
                s.execute("DROP INDEX IF EXISTS idx_diffentries_commit2");
                s.execute("DROP INDEX IF EXISTS idx_diffentries_commit3");
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
                s.execute("CREATE INDEX idx_diffentries_commit1 ON diffentries(commit1)");
                s.execute("CREATE INDEX idx_diffentries_commit2 ON diffentries(commit1, filetype)");
                s.execute("CREATE INDEX idx_diffentries_commit3 ON diffentries(filetype)");

                s.execute("DROP TABLE IF EXISTS diffsedits");
                s.execute("DROP INDEX IF EXISTS idx_diffsedits_commit");
                s.execute("""
                    CREATE TABLE diffsedits (
                        ID IDENTITY NOT NULL PRIMARY KEY,
                        commit VARCHAR(64) NOT NULL,
                        proj VARCHAR(64) NOT NULL,
                        filename VARCHAR(256) NOT NULL,
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
                    "INSERT INTO diffsedits(proj, commit, filename, editType, lines, linesFrom, linesTo, beginA, endA, beginB, endB) VALUES (?, ?, ?, ?, ?, ?, ?, ? ,?, ?, ?)")) {

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
            ps.setString(1, item.project());
            ps.setString(2, item.commitId());
            ps.setString(3, item.filename());
            ps.setString(4, item.type());
            ps.setInt(5, item.lines());
            ps.setInt(6, item.linesFrom());
            ps.setInt(7, item.linesTo());
            ps.setInt(8, item.beginA());
            ps.setInt(9, item.endA());
            ps.setInt(10, item.beginB());
            ps.setInt(11, item.endB());
            ps.addBatch();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    static void saveDiffEntries(List<DiffEntry> diffEntries) {
        try (Connection connection = DriverManager.getConnection(CreateDiffs.URL, "sa", "sa")) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO diffentries(commit1, commit2, proj, oldpath, newpath, changetype, filetype) VALUES (?, ?, ?, ?, ?, ?, ?)")) {

                diffEntries.forEach(item -> {
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
}
