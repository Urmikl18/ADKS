package fin.cse.adks.models;

import java.util.List;

import cn.edu.pku.sei.plde.qacrashfix.tree.edits.TreeEditAction;

public class Sequence {
    private int id;
    private List<TreeEditAction> linkedModificationSequence;

    public Sequence(int id, List<TreeEditAction> linkedModificationSequence) {
        this.id = id;
        this.linkedModificationSequence = linkedModificationSequence;
    }

    public int getId() {
        return this.id;
    }

    public List<TreeEditAction> getLinkedModificationSequence() {
        return this.linkedModificationSequence;
    }
}