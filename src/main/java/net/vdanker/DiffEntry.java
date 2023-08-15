package net.vdanker;

record DiffEntry(
        String commit1Id, String commit2Id, String proj,
        String oldPath, String newPath, String type, String fileType,
        long size, org.eclipse.jgit.diff.DiffEntry entry, DiffEdits diffEdits) {
}
