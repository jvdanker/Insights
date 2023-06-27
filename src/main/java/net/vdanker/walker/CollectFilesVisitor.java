package net.vdanker.walker;

import java.io.File;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class CollectFilesVisitor extends SimpleFileVisitor<Path> {

    List<File> files = new ArrayList<>();

    public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
        files.add(path.toFile());
        return FileVisitResult.CONTINUE;
    }

    public Stream<File> stream() {
        return this.files.stream();
    }
}
