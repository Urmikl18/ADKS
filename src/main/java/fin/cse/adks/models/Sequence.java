package fin.cse.adks.models;

import java.util.List;

import cn.edu.pku.sei.plde.qacrashfix.tree.edits.TreeEditAction;

public class Sequence {
    private int id;
    private List<TreeEditAction> editScript;

    public Sequence(int id, List<TreeEditAction> editScript) {
        this.id = id;
        this.editScript = editScript;
    }

    public int getId() {
        return this.id;
    }

    public List<TreeEditAction> getEditScript() {
        return this.editScript;
    }
}