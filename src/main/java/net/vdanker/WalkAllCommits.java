package net.vdanker;

import net.vdanker.mappers.GitStreams;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.TreeMap;

public class WalkAllCommits {

    public static void main(String[] args) throws IOException, GitAPIException {
        try (FileWriter fw = new FileWriter("/Users/juan/workspace/github.com/Insights/presentation/public/commits-per-day.csv")) {
            PrintWriter pw = new PrintWriter(fw);
            pw.println("epoch,count");

            Map<Integer, Integer> commitsPerDay = new TreeMap<>();
            try (Repository repo = GitStreams.fromBareRepository(new File("../bare/eqa-apps-exams.git")).getRepository()) {
                try (Git git = new Git(repo)) {
                    Iterable<RevCommit> commits = git.log().all().call();
                    for (RevCommit c1 : commits) {
                        var daysSince1970 = (int)Math.round(Math.floor(c1.getCommitTime() / 86400));
                        commitsPerDay.merge(daysSince1970, 1, Integer::sum);
                    }
                }
            }
            commitsPerDay.forEach((k,v) ->  pw.printf("%s,%d\n", k, v));
        }
    }
}

