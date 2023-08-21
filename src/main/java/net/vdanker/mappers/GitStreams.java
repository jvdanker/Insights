package net.vdanker.mappers;

import net.vdanker.parser.model.GitTreeObject;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class GitStreams {

    private final Repository repository;

    private GitStreams(File location) {
        try {
            repository = openRepository(location);
        } catch (IOException | InvalidRefNameException e) {
            throw new RuntimeException(e);
        }
    }

    public static GitStreams fromBareRepository(File location) {
        return new GitStreams(location);
    }

    public Stream<GitTreeObject> streamTreeObjects() {
        try {
            Ref head = repository.exactRef("refs/heads/master");

            // a RevWalk allows to walk over commits based on some filtering that is defined
            try (RevWalk walk = new RevWalk(repository)) {
                if (head == null) {
                    return Stream.empty();
                }
                RevCommit commit = walk.parseCommit(head.getObjectId());
//                System.out.println("Start-Commit: " + commit);
//                System.out.println("Commit-Message: " + commit.getFullMessage());

                return StreamSupport
                        .stream(Spliterators.spliteratorUnknownSize(
                                new TreeObjectIterator(repository, commit),
                                Spliterator.ORDERED),
                                false)
                        .onClose(walk::dispose);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Repository openRepository(File location) throws IOException, InvalidRefNameException {
        if (location.getName().endsWith("test.git")) {
            return new FileRepositoryBuilder()
                    .setGitDir(new File(location, ".git"))
                    .setBare()
                    .build();
        }

        return new FileRepositoryBuilder()
//                .setGitDir(new File(location, ".git"))
                .setGitDir(location)
//                .setInitialBranch("main")
                .setBare()
                .build();
    }

    public Repository getRepository() {
        return repository;
    }
}
