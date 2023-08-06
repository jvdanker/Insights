package net.vdanker;

import net.vdanker.mappers.GitStreams;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

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

        CreateDiffs app = new CreateDiffs();
        app.scanAndSave(list);
    }

    private void scanAndSave(List<File> list) {
        list.forEach(l -> {
            System.out.println(l.getName());
            String name = l.getName().replaceAll("\\.git", "");

            this.diffEntries.clear();
            getDiffs(name, l.getAbsolutePath());

            DbService.saveDiffEntries(this.diffEntries);
            DbService.saveDiffsEdits(this.diffEntries);
            DbService.saveDiffs(this.diffEntries);
        });
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
        List<DiffEntry> diffsBetweenTwoTrees =
                getDiffsBetweenTwoTrees(name, repo, c1, c2);
        this.diffEntries.addAll(diffsBetweenTwoTrees);

//        List<DiffEdit> list = diffsBetweenTwoTrees.stream()
////                .filter(e -> Set.of("java").contains(e.fileType()))
//                .map(entry -> createDiffEdit(name, repo, c1, entry.entry()))
//                .toList();
//        this.diffEdits.addAll(list);
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
                    String type = getExtension(entry.getNewPath());

                    DiffEdits diffEdits = createDiffEdits(name, repo, c1, entry);

                    result.add(new DiffEntry(
                            c1.getId().getName(),
                            c2 == null ? "" : c2.getId().getName(),
                            name,
                            entry.getOldPath(),
                            entry.getNewPath(),
                            entry.getChangeType().toString(),
                            type,
                            entry,
                            diffEdits));
                }
            } catch (GitAPIException e) {
                throw new RuntimeException(e);
            }
        }

        return result;
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
                    name,
                    commit.getId().getName(),
                    entry.getNewPath(),
                    fileHeader.toEditList(),
                    bos.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
