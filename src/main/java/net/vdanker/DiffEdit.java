package net.vdanker;

import org.eclipse.jgit.diff.Edit;

import java.util.List;

public record DiffEdit(String project, String commitId, String filename, List<Edit> edits, String diff) {
}
