package net.vdanker;

import java.util.List;

public record GetDiffsResult(List<DiffEntry> diffs, List<DiffEdits> edits) {
}
