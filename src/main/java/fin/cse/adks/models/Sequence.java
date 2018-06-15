package fin.cse.adks.models;

import java.util.List;

import cn.edu.pku.sei.plde.qacrashfix.tree.edits.TreeEditAction;

public class Sequence {
    private List<TreeEditAction> editScript;

    public Sequence(List<TreeEditAction> editScript) {
        this.editScript = editScript;
    }

    public List<TreeEditAction> getEditScript() {
        return this.editScript;
    }
}