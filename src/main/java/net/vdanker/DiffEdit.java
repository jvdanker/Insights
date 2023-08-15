package net.vdanker;

public record DiffEdit(String project, String commitId,
                       String fileId, String filename, String type, int lines, int linesFrom, int linesTo,
                       int beginA, int endA, int beginB, int endB) {
}
