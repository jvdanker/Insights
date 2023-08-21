package net.vdanker;

public record DiffEdit(
        String commitId,
        String fileId,
        String project,
        String fullPath,
        String changeType,
        int lines,
        int linesFrom, int linesTo,
        int beginA, int endA,
        int beginB, int endB) {
}
