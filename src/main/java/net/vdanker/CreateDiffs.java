package net.vdanker;

import net.vdanker.mappers.GitStreams;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

import java.io.*;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;

import static net.vdanker.WalkAllCommits.getExtension;
import static org.eclipse.jgit.diff.RawTextComparator.WS_IGNORE_ALL;

public class CreateDiffs {

    static String URL = "jdbc:h2:tcp://localhost:9092/./test";

    List<DiffEntry> diffEntries = new ArrayList<>();

    public static void main(String[] args) throws ClassNotFoundException, SQLException {
        DbService.createTables(URL);

        File[] files = Path.of("../bare")
                .toFile()
                .listFiles();

        List<File> list = Arrays.stream(files)
                .filter(File::isDirectory)
                .filter(l -> l.getName().equals("test.git"))
                .toList();
    }

    void scanAndSave(String name, String dir) {
        this.diffEntries.clear();
        getDiffs(name, dir);

        DbService.saveDiffEntries(this.diffEntries);
        DbService.saveDiffsEdits(this.diffEntries);
//        DbService.saveDiffs(this.diffEntries);
    }

    private void getDiffs(String name, String dir) {
        try (Repository repo = GitStreams.fromBareRepository(new File(dir)).getRepository()) {
            Ref head = repo.findRef(Constants.HEAD); //"refs/heads/main");
            if (head.getObjectId() == null) return;

            try (RevWalk rw = new RevWalk(repo)) {
                rw.setRetainBody(false);
                rw.setFirstParent(true);
                rw.markStart(rw.parseCommit(head.getObjectId()));

                RevCommit c1 = rw.next();
                for (RevCommit c2 : rw) {
                    calculateDiffEntriesAndEdits(name, repo, c1, c2);
                    c1 = c2;
                }

                // process first commit
                calculateDiffEntriesAndEdits(name, repo, c1, null);

                rw.dispose();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void calculateDiffEntriesAndEdits(String name, Repository repo, RevCommit c1, RevCommit c2) throws IOException {
        List<DiffEntry> diffsBetweenTwoTrees = getDiffsBetweenTwoTrees(name, repo, c1, c2);
        this.diffEntries.addAll(diffsBetweenTwoTrees);
    }

    private static List<DiffEntry> getDiffsBetweenTwoTrees(
            String name,
            Repository repo,
            RevCommit c1,
            RevCommit c2) throws IOException {
        List<DiffEntry> result = new ArrayList<>();

        AnyObjectId t2 = (c2 == null ? Constants.EMPTY_TREE_ID : c2.getTree());

        try (ObjectReader reader = repo.newObjectReader()) {
            CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
            if (c2 != null) {
                oldTreeIter.reset(reader, t2);
            }

            CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
            newTreeIter.reset(reader, c1.getTree());

            try (Git git = new Git(repo)) {
                List<org.eclipse.jgit.diff.DiffEntry> diffs = git.diff()
                        .setNewTree(newTreeIter)
                        .setOldTree(oldTreeIter)
                        .call();

                for (org.eclipse.jgit.diff.DiffEntry entry : diffs) {
                    DiffEdits diffEdits = createDiffEdits(name, repo, c1, entry);

                    result.add(new DiffEntry(
                            c1.getId().getName(),
                            c2 == null ? "" : c2.getId().getName(),
                            entry.getOldId().name(),
                            entry.getNewId().name(),
                            entry.getChangeType().toString(),
                            entry,
                            diffEdits));
                }
            } catch (GitAPIException e) {
                throw new RuntimeException(e);
            }
        }

        return result;
    }

    private static long getObjectSize(Repository repo, RevCommit c1, org.eclipse.jgit.diff.DiffEntry entry) throws IOException {
        if (org.eclipse.jgit.diff.DiffEntry.ChangeType.DELETE.equals(entry.getChangeType())) {
            return -1;
        }

        try (TreeWalk treeWalk = new TreeWalk(repo)) {
            treeWalk.addTree(c1.getTree());
            treeWalk.setRecursive(true);
            treeWalk.setFilter(PathFilter.create(entry.getNewPath()));
            if (!treeWalk.next()) {
                throw new IllegalStateException("Did not find expected file");
            }

            ObjectId objectId = treeWalk.getObjectId(0);
            ObjectLoader loader = repo.open(objectId);
            return loader.getSize();
        }
    }

    private static DiffEdits createDiffEdits(
            String name,
            Repository repo,
            RevCommit commit,
            org.eclipse.jgit.diff.DiffEntry entry) {

        OutputStream bos = new ByteArrayOutputStream();
        try (DiffFormatter df = new DiffFormatter(bos)) {
            df.setRepository(repo);
            df.setContext(0);
            df.setDiffComparator(WS_IGNORE_ALL);
            df.format(entry);

            FileHeader fileHeader = df.toFileHeader(entry);
            return new DiffEdits(
                    commit.getId().getName(),
                    entry.getNewId().name(),
                    fileHeader.toEditList(),
                    bos.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
