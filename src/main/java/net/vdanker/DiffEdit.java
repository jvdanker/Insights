package net.vdanker;

public record DiffEdit(String project, String commitId, String filename, String type, int lines, int linesFrom, int linesTo,
                       int beginA, int endA, int beginB, int endB) {
}
