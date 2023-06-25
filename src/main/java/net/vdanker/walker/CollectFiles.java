package net.vdanker.walker;

import java.io.File;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class CollectFiles extends SimpleFileVisitor<Path> {

    List<File> files = new ArrayList<>();

    public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
        String fileName = path.getFileName().toString();
        if (fileName.endsWith(".java")) {
            files.add(path.toFile());
        }

        return FileVisitResult.CONTINUE;
    }

    public List<File> getFiles() {
        return files;
    }
}
