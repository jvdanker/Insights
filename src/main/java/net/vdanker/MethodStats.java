package net.vdanker;

import net.vdanker.mappers.GitStreams;
import net.vdanker.mappers.InputStreamMapper;
import net.vdanker.parser.model.JavaMethod;
import net.vdanker.parser.model.JavaStats;
import org.eclipse.jgit.lib.Repository;

import java.io.File;
import java.sql.*;
import java.util.*;

public class MethodStats {

    static String URL = "jdbc:h2:tcp://localhost:9092/./test";

    public static void collectAndSave(String dir) {
        var methods = GitStreams.fromBareRepository(new File(dir))
                .streamTreeObjects()
                .filter(t -> t.name().endsWith(".java"))
                .filter(t -> !t.path().contains("/test/"))
                .map(InputStreamMapper::toJavaStats)
                .map(JavaStats::methods)
                .flatMap(Collection::stream)
                .filter(e -> !e.methodName().startsWith("get"))
                .filter(e -> !e.methodName().startsWith("set"))
                .toList();

        saveMethods(methods);
    }

    static void saveMethods(List<JavaMethod> methods) {
        try (Connection connection = DriverManager.getConnection(CreateDiffs.URL, "sa", "sa")) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO methods(" +
                            "file_id, package, class, method, statements, complexity, variables, lineStart, lineEnd) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {

                methods.forEach(item -> {
                    try {
                        ps.setString(1, item.objectId().getName());
                        ps.setString(2, item.packageName());
                        ps.setString(3, item.className());
                        ps.setString(4, item.methodName());
                        ps.setLong(5, item.blockStatements());
                        ps.setLong(6, item.complexity());
                        ps.setLong(7, item.localVariableDeclarations());
                        ps.setLong(8, item.methodStart());
                        ps.setLong(9, item.methodEnd());
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

    static void createTable(String url) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, "sa", "sa")) {
            try (Statement s = connection.createStatement()) {
                s.execute("DROP TABLE IF EXISTS methods");
                s.execute("DROP INDEX IF EXISTS idx_methods_proj_filename");
//                s.execute("DROP INDEX IF EXISTS idx_files_oid");
//                s.execute("DROP INDEX IF EXISTS idx_files_fp");

                s.execute("""
                    CREATE TABLE methods (
                        id IDENTITY NOT NULL PRIMARY KEY,
                        file_id VARCHAR(512) NOT NULL,
                        package VARCHAR(512) NOT NULL,
                        class VARCHAR(64),
                        method VARCHAR(64),
                        statements INT,
                        complexity INT,
                        variables INT,
                        lineStart INT,
                        lineEnd INT)
                    """);

                s.execute("CREATE INDEX idx_methods_proj_filename ON methods(file_id)");
//                s.execute("CREATE INDEX idx_files_oid ON FILES(object_id)");
//                s.execute("CREATE INDEX idx_files_fp ON FILES(fullpath)");
            }
        }
    }
}
