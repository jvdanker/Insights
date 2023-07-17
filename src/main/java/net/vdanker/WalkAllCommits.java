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

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WalkAllCommits {

    public static void main(String[] args) throws IOException, GitAPIException {
        Map<Integer, ResultsCommitsPerDay> commitsPerDay = new TreeMap<>();
        Map<Integer, ResultsCommittersPerDay> committersPerDay = new HashMap<>();
        Set<String> committers = new HashSet<>();

        try (Repository repo = GitStreams.fromBareRepository(new File("../bare/eqa-apps-exams.git")).getRepository()) {
            try (Git git = new Git(repo)) {
                Iterable<RevCommit> commits = git.log().all().call();

//                Iterator<RevCommit> iterator = commits.iterator();
//                RevCommit c1 = iterator.next();

                for (RevCommit c2 : commits) {
                    if (c2.getParentCount() > 1) continue; // skip merge commits

                    var daysSince1970 = (int) Math.round(Math.floor(c2.getCommitTime() / 86400));

                    PersonIdent committerIdent = c2.getCommitterIdent();
//                    int filesTouched = compareTrees(repo, git, c1.getTree(), c2.getTree());

                    String emailAddress = rewrite("".equals(committerIdent.getEmailAddress())
                            ? committerIdent.getName()
                            : committerIdent.getEmailAddress());

                    if (emailAddress.contains("jenkins")) continue;

                    committers.add(emailAddress);

                    committersPerDay.merge(
                            daysSince1970,
                            new ResultsCommittersPerDay(Set.of(emailAddress)),
                            WalkAllCommits::sumOfResultsCommittersPerDay);

                    commitsPerDay.merge(
                            daysSince1970,
                            new ResultsCommitsPerDay(1, Set.of(emailAddress)), // , filesTouched),
                            WalkAllCommits::sumOfResults);

//                    c1 = c2;
                }
            }
        }

        Set<String> list = committers.stream()
                .filter(c -> c.length() > 0)
                .map(WalkAllCommits::rewrite)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        Map<String, Integer> committersMap = new HashMap<>();
        int i = 0;
        for (String c : list) {
            committersMap.put(c, i++);
        }

        Map<Integer, Set<Integer>> committersPerDayAnonymized = new HashMap<>();
        committersPerDay.forEach((k, v) -> {
            Set<Integer> integerStream = v.committers().stream()
                    .map(String::toLowerCase)
                    .map(committersMap::get)
                    .collect(Collectors.toSet());
            committersPerDayAnonymized.put(k, integerStream);
        });

        try (FileWriter fw = new FileWriter("/Users/juan/workspace/github.com/Insights/presentation/public/committers.csv")) {
            PrintWriter pw = new PrintWriter(fw);
            pw.println("index,committer");
            committersMap.forEach((k, v) -> pw.printf("%d,%s\n", v, k));
        }

        try (FileWriter fw = new FileWriter("/Users/juan/workspace/github.com/Insights/presentation/public/committers-per-day.csv")) {
            PrintWriter pw = new PrintWriter(fw);
            pw.println("epoch,committers");
            committersPerDayAnonymized.forEach((k, v) -> pw.printf("%s,%s\n", k, encodeValue(v.toString())));
        }

        try (FileWriter fw = new FileWriter("/Users/juan/workspace/github.com/Insights/presentation/public/commits-per-day.csv")) {
            PrintWriter pw = new PrintWriter(fw);
            pw.println("epoch,count,committers"); // ,files");
            commitsPerDay.forEach((k, v) -> pw.printf("%s,%d,%d\n", k, v.commits(), v.committers().size())); // , v.files()));
        }
    }

    private static String encodeValue(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    static Map<String, String> rewrite = Map.of(
            "jenkins@nzqa.govt.nz", "jenkins@pdjenkins01.nzqa.govt.nz",
            "MarcusM@nzqa.govt.nz", "Marcus.Manning@nzqa.govt.nz",
            "RodrigoD@zd31009-z20.nzqa.govt.nz", "Rodrigo.DeSouza@nzqa.govt.nz",
            "aaron.cai@nzqa.govt.nz", "Aaron.Cai@nzqa.govt.nz",
            "yashmeek@nzqa.govt.nz", "yashmeet.kaur@nzqa.govt.nz",
            "oliverl@zd31826-800.nzqa.govt.nz", "oliver.layug@nzqa.govt.nz",
            "oliverlayug@nzqa.govt.nz", "oliver.layug@nzqa.govt.nz"
    );

    private static String rewrite(String c) {
        if (rewrite.containsKey(c)) {
            return rewrite.get(c);
        }
        return c;
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

    private static ResultsCommitsPerDay sumOfResults(ResultsCommitsPerDay r1, ResultsCommitsPerDay r2) {
        return new ResultsCommitsPerDay(
                r1.commits() + r2.commits(),
                Stream.concat(
                                r1.committers().stream(),
                                r2.committers().stream())
                        .collect(Collectors.toSet()));
//                r1.files() + r2.files());
    }

    private static ResultsCommittersPerDay sumOfResultsCommittersPerDay(ResultsCommittersPerDay r1, ResultsCommittersPerDay r2) {
        return new ResultsCommittersPerDay(
                Stream.concat(r1.committers().stream(), r2.committers().stream())
                        .collect(Collectors.toSet())
        );
    }
}

record ResultsCommitsPerDay(Integer commits, Set<String> committers) {
}

record ResultsCommittersPerDay(Set<String> committers) {
}


