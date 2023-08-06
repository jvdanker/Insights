package net.vdanker;

import org.eclipse.jgit.diff.Edit;

import java.util.List;

public record DiffEdits(String project, String commitId, String filename, List<Edit> edits, String diff) {

    /*
    An edit where beginA == endA && beginB < endB is an insert edit,
    that is sequence B inserted the elements in region [beginB, endB) at beginA.

    An edit where beginA < endA && beginB == endB is a delete edit,
    that is sequence B has removed the elements between [beginA, endA).

    An edit where beginA < endA && beginB < endB is a replace edit,
    that is sequence B has replaced the range of elements between [beginA, endA) with those found in [beginB, endB).
     */
    /*
    INSERT(43-43,43-45)
    REPLACE(44-45,46-50)
    INSERT(69-69,74-76)
    REPLACE(70-71,77-78)
    INSERT(72-72,79-82)
    REPLACE(73-77,83-87)
    REPLACE(78-81,88-93)
    REPLACE(83-85,95-100)
    REPLACE(87-88,102-107)
    REPLACE(90-93,109-111)
     */
    public DiffEdit toDiffEdit(Edit edit) {
        int lines = 0;
        Edit.Type type = edit.getType();

        switch (type) {
            case INSERT -> lines = (edit.getEndB() - edit.getBeginB());
            case DELETE -> lines = (edit.getEndA() - edit.getBeginA());
            case REPLACE -> lines = (edit.getEndB() - edit.getBeginB()) - (edit.getEndA() - edit.getBeginA());
        }

        if (lines < 0) {
            type = Edit.Type.DELETE;
            lines *= -1;
        }

        int linesFrom = (edit.getEndA() - edit.getBeginA());
        int linesTo = (edit.getEndB() - edit.getBeginB());

        return new DiffEdit(
                this.project,
                this.commitId,
                this.filename,
                type.toString(),
                lines,
                linesFrom,
                linesTo,
                edit.getBeginA(),
                edit.getEndA(),
                edit.getBeginB(),
                edit.getEndB()
        );
    }
}
