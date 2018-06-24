package fin.cse.adks.models;

import cn.edu.pku.sei.plde.qacrashfix.tree.TreeNode;
import cn.edu.pku.sei.plde.qacrashfix.tree.edits.*;

public class Modification implements Comparable<Modification> {
    private int codeId;
    private TreeEditAction action;

    public Modification(int codeId, TreeEditAction action) {
        this.codeId = codeId;
        this.action = action;
    }

    public int getCodeId() {
        return this.codeId;
    }

    public TreeEditAction getTreeEditAction() {
        return this.action;
    }

    public boolean isLinkedTo(Modification other) {
        TreeEditAction m1 = this.getTreeEditAction();
        TreeEditAction m2 = other.getTreeEditAction();
        if (!m1.equals(m2)) {
            TreeNode t1 = getTarget(m1);
            TreeNode s2 = getSource(m2);
            TreeNode t2 = getTarget(m2);
            if (t1 != null) {
                if (t1.isLeaf() && t2 != null && t2.isLeaf()) {
                    TreeNode t1P = t1.getParent();
                    return (s2 != null && t1P != null && t1.getParent().equals(s2.getParent()))
                            || (t2 != null && t1P != null && t1.getParent().equals(t2.getParent()));
                } else {
                    return (s2 != null && t1.equals(s2.getParent())) || (t2 != null && t1.equals(t2.getParent()));
                }
            }
        }
        return false;
    }

    private TreeNode getSource(TreeEditAction m) {
        if (m instanceof CopyAction) {
            CopyAction tmp = (CopyAction) m;
            return tmp.getOldNode();
        }
        if (m instanceof DeleteAction) {
            DeleteAction tmp = (DeleteAction) m;
            return tmp.getDeletedNode();
        }
        if (m instanceof InsertAction) {
            return null;
        }
        if (m instanceof InsertRootAction) {
            return null;
        }
        if (m instanceof MoveAction) {
            MoveAction tmp = (MoveAction) m;
            return tmp.getReferenceNode();
        }
        if (m instanceof ReplaceAction) {
            ReplaceAction tmp = (ReplaceAction) m;
            return tmp.getReplacedNode();
        }
        if (m instanceof UpdateAction) {
            UpdateAction tmp = (UpdateAction) m;
            return tmp.getNode();
        }
        return null;
    }

    private TreeNode getTarget(TreeEditAction m) {
        if (m instanceof CopyAction) {
            CopyAction tmp = (CopyAction) m;
            return tmp.getCopiedNode();
        }
        if (m instanceof DeleteAction) {
            return null;
        }
        if (m instanceof InsertAction) {
            InsertAction tmp = (InsertAction) m;
            return tmp.getInsertedNode();
        }
        if (m instanceof InsertRootAction) {
            InsertRootAction tmp = (InsertRootAction) m;
            return tmp.getNewRoot();
        }
        if (m instanceof MoveAction) {
            MoveAction tmp = (MoveAction) m;
            return tmp.getMovedNode();
        }
        if (m instanceof ReplaceAction) {
            ReplaceAction tmp = (ReplaceAction) m;
            return tmp.getNewNode();
        }
        if (m instanceof UpdateAction) {
            UpdateAction tmp = (UpdateAction) m;
            return tmp.getNode();
        }
        return null;
    }

    protected boolean isIsomorphTo(Modification other) {
        TreeEditAction m1 = this.getTreeEditAction();
        TreeEditAction m2 = other.getTreeEditAction();
        // own idea
        if (m1 instanceof CopyAction && m2 instanceof CopyAction) {
            TreeNode p1 = getTarget(m1).getParent();
            TreeNode p2 = getTarget(m2).getParent();
            return p1 != null && p2 != null && p1.getLabel().equals(p2.getLabel());
        }
        if (m1 instanceof DeleteAction && m2 instanceof DeleteAction) {
            TreeNode p1 = getSource(m1).getParent();
            TreeNode p2 = getSource(m2).getParent();
            return p1 != null && p2 != null && p1.getLabel().equals(p2.getLabel());
        }
        if (m1 instanceof InsertAction && m2 instanceof InsertAction) {
            return getTarget(m1).getLabel().equals(getTarget(m2).getLabel());
        }
        if (m1 instanceof InsertRootAction && m2 instanceof InsertRootAction) {
            return getTarget(m1).getLabel().equals(getTarget(m2).getLabel());
        }
        if (m1 instanceof MoveAction && m2 instanceof MoveAction) {
            TreeNode p1 = getTarget(m1).getParent();
            TreeNode p2 = getTarget(m2).getParent();
            return p1 != null && p2 != null && p1.getLabel().equals(p2.getLabel());
        }
        if (m1 instanceof ReplaceAction && m2 instanceof ReplaceAction) {
            return getSource(m1).getLabel().equals(getSource(m2).getLabel())
                    && getTarget(m1).getLabel().equals(getTarget(m2).getLabel());
        }
        if (m1 instanceof UpdateAction && m2 instanceof UpdateAction) {
            return getSource(m1).getLabel().equals(getSource(m2).getLabel());
        }
        return false;
    }

    @Override
    public int compareTo(Modification other) {
        return this.action.toString().compareTo(other.action.toString());
    }

    @Override
    public String toString() {
        return this.action.toString();
    }
}