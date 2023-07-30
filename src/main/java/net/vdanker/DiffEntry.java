package net.vdanker;

record DiffEntry(String commit1Id, String commit2Id, String proj, String oldPath, String newPath, String type, String fileType) {
}
