package net.vdanker;

import net.vdanker.mappers.GitStreams;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;

public class CollectFiles {

    static String URL = "jdbc:h2:tcp://localhost:9092/./test";

    public static void main(String[] args) throws ClassNotFoundException, SQLException {
        createTable(URL);

        File[] files = Path.of("../bare")
                .toFile()
                .listFiles();

        List<File> list = Arrays.stream(files)
                .filter(File::isDirectory)
//                .filter(l -> l.getName().equals("eqa-apps-exams.git"))
                .toList();

        list.forEach(l -> {
            System.out.println(l.getName());
            String name = l.getName().replaceAll("\\.git", "");

            saveRepoFiles(collectAllFiles(name, l.getAbsolutePath()));
        });
    }

    static void saveRepoFiles(List<RepoFile> list) {
        try (Connection connection = DriverManager.getConnection(URL, "sa", "sa")) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO files(object_id, project, module, path, filename, fullpath, extension, size) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {

                list.forEach(item -> {
                    try {
                        ps.setString(1, item.objectId());
                        ps.setString(2, item.project());
                        ps.setString(3, item.module());
                        ps.setString(4, item.path());
                        ps.setString(5, item.filename());
                        ps.setString(6, item.fullpath());
                        ps.setString(7, item.extension());
                        ps.setLong(8, item.size());
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

    static List<RepoFile> collectAllFiles(String name, String dir) {
        List<RepoFile> result = new ArrayList<>();

        try (Repository repo = GitStreams.fromBareRepository(new File(dir)).getRepository()) {
            Ref head = repo.findRef("HEAD");
            if (head == null || head.getObjectId() == null) return result;

            try (RevWalk rw = new RevWalk(repo)) {
                rw.markStart(rw.parseCommit(head.getObjectId()));

                RevCommit commit = rw.next();

                try (TreeWalk treeWalk = new TreeWalk(repo)) {
                    treeWalk.addTree(commit.getTree());
                    treeWalk.setRecursive(true);

                    while (treeWalk.next()) {
                        ObjectId objectId = treeWalk.getObjectId(0);
                        ObjectLoader loader = repo.open(objectId);

                        Path path = Path.of(treeWalk.getPathString());
                        Path parent = path.getParent();
                        String filename = path.getFileName().toString();

                        String module = "";
                        if (parent != null) {
                            String temp = parent.toString();
                            StringTokenizer st = new StringTokenizer(temp, "/");
                            if (st.hasMoreTokens()) {
                                String s = st.nextToken();
                                if (!"src".equals(s)) {
                                    module = s;
                                }
                            }
                        }

                        result.add(new RepoFile(
                                objectId.name(),
                                name,
                                module,
                                (parent == null ? "" : parent.toString()),
                                filename,
                                (parent == null ? filename : parent + "/" + filename),
                                getExtension(filename),
                                loader.getSize()));
                    }
                }

                rw.dispose();
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

    record RepoFile(String objectId, String project, String module, String path, String filename, String fullpath, String extension, Long size) {}

    static void createTable(String url) throws SQLException {
        try (Connection connection = DriverManager.getConnection(URL, "sa", "sa")) {
            try (Statement s = connection.createStatement()) {
                s.execute("DROP TABLE IF EXISTS files");
                s.execute("DROP INDEX IF EXISTS idx_files_ext");
                s.execute("DROP INDEX IF EXISTS idx_files_oid");
                s.execute("DROP INDEX IF EXISTS idx_files_fp");

                s.execute("""
                    CREATE TABLE files (
                        id IDENTITY NOT NULL PRIMARY KEY,
                        object_id VARCHAR(64) NOT NULL,
                        project VARCHAR(64), 
                        module VARCHAR(64), 
                        path VARCHAR(1024), 
                        filename VARCHAR(1024),
                        fullpath VARCHAR(1024), 
                        extension VARCHAR(64), 
                        size LONG
                    )""");

                s.execute("CREATE INDEX idx_files_ext ON FILES(extension)");
                s.execute("CREATE INDEX idx_files_oid ON FILES(object_id)");
                s.execute("CREATE INDEX idx_files_fp ON FILES(fullpath)");
            }
        }
    }
}
