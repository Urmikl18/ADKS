package fin.cse.adks.models;

import java.util.ArrayList;
import java.util.List;

import cn.edu.pku.sei.plde.qacrashfix.tree.TreeNode;
import cn.edu.pku.sei.plde.qacrashfix.tree.edits.CopyAction;
import cn.edu.pku.sei.plde.qacrashfix.tree.edits.DeleteAction;
import cn.edu.pku.sei.plde.qacrashfix.tree.edits.InsertAction;
import cn.edu.pku.sei.plde.qacrashfix.tree.edits.MoveAction;
import cn.edu.pku.sei.plde.qacrashfix.tree.edits.TreeEditAction;
import cn.edu.pku.sei.plde.qacrashfix.tree.edits.UpdateAction;

public class Sequence {
    private int id;
    private List<TreeEditAction> editScript;
    private ArrayList<ArrayList<TreeEditAction>> linkedModifications;

    public Sequence(int id, List<TreeEditAction> editScript) {
        this.id = id;
        this.editScript = editScript;
        this.linkedModifications = this.linkModifications(this.editScript);
    }

    public int getId() {
        return this.id;
    }

    public List<TreeEditAction> getEditScript() {
        return this.editScript;
    }

    public ArrayList<ArrayList<TreeEditAction>> getLinkedModifications() {
        return this.linkedModifications;
    }

    private ArrayList<ArrayList<TreeEditAction>> linkModifications(List<TreeEditAction> editScript) {
        ArrayList<ArrayList<TreeEditAction>> links = new ArrayList<>(editScript.size());
        for (int i = 0; i < editScript.size(); ++i) {
            links.add(new ArrayList<TreeEditAction>());
            links.get(i).add(editScript.get(i));
            for (int j = 0; j < editScript.size(); ++j) {
                if (areLinked(editScript.get(i), editScript.get(j))) {
                    links.get(i).add(editScript.get(j));
                }
            }
            System.out.println("------------------");
            for (TreeEditAction action : links.get(i)) {
                System.out.println(action.toString());
            }
        }
        return links;
    }

    private boolean areLinked(TreeEditAction m1, TreeEditAction m2) {
        if (!m1.equals(m2)) {
            TreeNode t1 = getTarget(m1);
            TreeNode s2 = getSource(m2);
            TreeNode t2 = getTarget(m2);
            if (t1 != null && t2 != null && s2 != null) {
                if (t1.isLeaf() && t2.isLeaf()) {
                    return t1.getParent().equals(s2.getParent()) || t1.getParent().equals(t2.getParent());
                } else {
                    return t1.equals(s2.getParent()) || t1.equals(t2.getParent());
                }
            }
        }
        return false;
    }

    private TreeNode getSource(TreeEditAction m) {
        if (m instanceof InsertAction) {
            return null;
        }
        if (m instanceof UpdateAction) {
            UpdateAction tmp = (UpdateAction) m;
            return tmp.getNode();
        }
        if (m instanceof CopyAction) {
            CopyAction tmp = (CopyAction) m;
            return tmp.getOldNode();
        }
        if (m instanceof MoveAction) {
            MoveAction tmp = (MoveAction) m;
            return tmp.getReferenceNode();
        }
        if (m instanceof DeleteAction) {
            DeleteAction tmp = (DeleteAction) m;
            return tmp.getDeletedNode();
        }
        return null;
    }

    private TreeNode getTarget(TreeEditAction m) {
        if (m instanceof InsertAction) {
            InsertAction tmp = (InsertAction) m;
            return tmp.getInsertedNode();
        }
        if (m instanceof UpdateAction) {
            UpdateAction tmp = (UpdateAction) m;
            return tmp.getNode();
        }
        if (m instanceof CopyAction) {
            CopyAction tmp = (CopyAction) m;
            return tmp.getCopiedNode();
        }
        if (m instanceof MoveAction) {
            MoveAction tmp = (MoveAction) m;
            return tmp.getMovedNode();
        }
        if (m instanceof DeleteAction) {
            return null;
        }
        return null;
    }
}