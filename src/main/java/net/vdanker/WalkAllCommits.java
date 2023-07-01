package net.vdanker;

import net.vdanker.mappers.GitStreams;
import net.vdanker.parser.Pair;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WalkAllCommits {

    public static void main(String[] args) throws IOException, GitAPIException {
        try (FileWriter fw = new FileWriter("/Users/juan/workspace/github.com/Insights/presentation/public/commits-per-day.csv")) {
            PrintWriter pw = new PrintWriter(fw);
            pw.println("epoch,count,committers");

            Map<Integer, Pair<Integer, Set<String>>> commitsPerDay = new TreeMap<>();
            try (Repository repo = GitStreams.fromBareRepository(new File("../bare/eqa-apps-exams.git")).getRepository()) {
                try (Git git = new Git(repo)) {
                    Iterable<RevCommit> commits = git.log().all().call();
                    for (RevCommit c1 : commits) {
                        var daysSince1970 = (int)Math.round(Math.floor(c1.getCommitTime() / 86400));
                        PersonIdent committerIdent = c1.getCommitterIdent();
                        commitsPerDay.merge(
                                daysSince1970,
                                new Pair<>(1, Set.of(committerIdent.getEmailAddress())),
                                WalkAllCommits::sumOfPair);
                    }
                }
            }
            commitsPerDay.forEach((k,v) ->  pw.printf("%s,%d,%d\n", k, v.getKey(), v.getValue().size()));
        }
    }

    private static Pair<Integer, Set<String>> sumOfPair(Pair<Integer, Set<String>> p1, Pair<Integer, Set<String>> p2) {
        return new Pair<>(
                p1.getKey() + p2.getKey(),
                Stream.concat(
                        p1.getValue().stream(),
                        p2.getValue().stream())
                        .collect(Collectors.toSet()));
    }

}

