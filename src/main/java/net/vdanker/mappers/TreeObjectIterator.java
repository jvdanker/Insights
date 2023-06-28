package net.vdanker.mappers;

import net.vdanker.parser.model.GitTreeObject;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.*;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class TreeObjectIterator implements Iterator<GitTreeObject> {

    private final RevCommit commit;
    private final Repository repository;
    private TreeWalk treeWalk;

    public TreeObjectIterator(Repository repository, RevCommit commit) {
        this.repository = repository;
        this.commit = commit;
        init();
    }

    private void init() {
        RevTree tree = this.commit.getTree();

        try {
            treeWalk = new TreeWalk(this.repository);
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            treeWalk.close();;
        }
    }

    @Override
    public boolean hasNext() {
        try {
            return treeWalk.next();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public GitTreeObject next() {
        try {
            ObjectLoader loader = repository.open(treeWalk.getObjectId(0));
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            loader.copyTo(os);

            ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());

            return new GitTreeObject(treeWalk.getObjectId(0), treeWalk.getNameString(), is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
