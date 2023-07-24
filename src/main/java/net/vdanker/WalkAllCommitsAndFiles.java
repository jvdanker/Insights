package net.vdanker;

import net.vdanker.mappers.GitStreams;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WalkAllCommitsAndFiles {

    public static void main(String[] args) throws IOException, GitAPIException {
        try (FileWriter fw = new FileWriter("/Users/juan/workspace/github.com/Insights/presentation/public/commits-per-day.csv")) {
            PrintWriter pw = new PrintWriter(fw);
            pw.println("epoch,count,committers,files");

            Map<Integer, ResultsAndFiles> commitsPerDay = new TreeMap<>();
            try (Repository repo = GitStreams.fromBareRepository(new File("../bare/eqa-apps-exams.git")).getRepository()) {
                try (Git git = new Git(repo)) {
                    Iterable<RevCommit> commits = git.log().all().call();

                    Iterator<RevCommit> iterator = commits.iterator();
                    RevCommit c1 = iterator.next();

                    for (RevCommit c2 : commits) {
                        var daysSince1970 = (int)Math.round(Math.floor(c2.getCommitTime() / 86400));
                        PersonIdent committerIdent = c2.getCommitterIdent();
                        int filesTouched = compareTrees(repo, git, c1.getTree(), c2.getTree());

                        commitsPerDay.merge(
                                daysSince1970,
                                new ResultsAndFiles(1, Set.of(committerIdent.getEmailAddress()), filesTouched),
                                WalkAllCommitsAndFiles::sumOfResults);

                        c1 = c2;
                    }
                }
            }
            commitsPerDay.forEach((k,v) ->  pw.printf("%s,%d,%d,%d\n", k, v.commits(), v.committers().size(), v.files()));
        }
    }

    private static int compareTrees(Repository repo, Git git, RevTree t1, RevTree t2) throws IOException, GitAPIException {
        try (ObjectReader reader = repo.newObjectReader()) {
            CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
            oldTreeIter.reset(reader, t2);

            CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
            newTreeIter.reset(reader, t1);

            List<DiffEntry> diffs = git.diff()
                    .setNewTree(newTreeIter)
                    .setOldTree(oldTreeIter)
                    .call();

            return diffs.size();
//            for (DiffEntry entry : diffs) {
//                System.out.println(entry.getOldId() + " " + entry.getNewId() + " " + entry);
//            }
        }
    }

    private static ResultsAndFiles sumOfResults(ResultsAndFiles r1, ResultsAndFiles r2) {
        return new ResultsAndFiles(
                r1.commits() + r2.commits(),
                Stream.concat(
                        r1.committers().stream(),
                        r2.committers().stream())
                        .collect(Collectors.toSet()),
                r1.files() + r2.files());
    }

}

record ResultsAndFiles(Integer commits, Set<String> committers, Integer files) {

}

