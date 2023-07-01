package net.vdanker;

import net.vdanker.mappers.GitStreams;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class GetChangedFiles {

    public static void main(String[] args) throws IOException, GitAPIException {
        try (Repository repo = GitStreams.fromBareRepository(new File("../bare/eqa-apps-exams.git")).getRepository()) {
            try (Git git = new Git(repo)) {
                Iterable<RevCommit> commits = git.log().all().call();

                Iterator<RevCommit> iterator = commits.iterator();
                RevCommit c1 = iterator.next();

                for (RevCommit c2 : commits) {
                    System.out.println("LogCommit1: " + c1);
                    System.out.println("LogCommit2: " + c2);

//                    try (RevWalk walk = new RevWalk(repo)) {
                        RevTree t1 = c1.getTree();
//                        RevTree t1 = walk.lookupTree(ObjectId.fromString("b4e74705737b9a4db32182bf279669af5067ab22"));
                        System.out.println("Tree: " + t1);

                        RevTree t2 = c2.getTree();
//                        RevTree t2 = walk.lookupTree(ObjectId.fromString("e9cc7f2661f9f4d45e81988b675324e1d383f461"));
                        System.out.println("Tree: " + t2);

                        compareTrees(repo, git, t1, t2);
//                        walk.dispose();
//                    }

                    c1 = c2;
                    return;
                }
            }
        }
    }

    private static void compareTrees(Repository repo, Git git, RevTree t1, RevTree t2) throws IOException, GitAPIException {
        try (ObjectReader reader = repo.newObjectReader()) {
            CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
            oldTreeIter.reset(reader, t2);

            CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
            newTreeIter.reset(reader, t1);

            List<DiffEntry> diffs = git.diff()
                    .setNewTree(newTreeIter)
                    .setOldTree(oldTreeIter)
                    .call();

            for (DiffEntry entry : diffs) {
                System.out.println(entry.getOldId() + " " + entry.getNewId() + " " + entry);
            }
            if (diffs.isEmpty()) {
                System.out.println("<<<empty commit>>>");
            }

            System.out.println("-------------------");
        }
    }
}

