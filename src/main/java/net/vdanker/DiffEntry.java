package net.vdanker;

record DiffEntry(
        String commit1Id, String commit2Id,
        String oldId, String newId,
        String changeType,
        org.eclipse.jgit.diff.DiffEntry entry, DiffEdits diffEdits) {
}
