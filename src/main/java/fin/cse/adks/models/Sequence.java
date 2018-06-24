package fin.cse.adks.models;

import java.util.List;

public class Sequence {
    private List<Modification> linkedModificationSequence;

    public Sequence(List<Modification> linkedModificationSequence) {
        this.linkedModificationSequence = linkedModificationSequence;
    }

    public List<Modification> getLinkedModificationSequence() {
        return this.linkedModificationSequence;
    }

    public boolean isIsomorphTo(Sequence other) {
        int n1 = this.getLinkedModificationSequence().size();
        int n2 = other.getLinkedModificationSequence().size();
        if (n1 == n2) {
            for (int i = 0; i < n1; ++i) {
                Modification m1 = this.getLinkedModificationSequence().get(i);
                Modification m2 = other.getLinkedModificationSequence().get(i);
                if (!m1.isIsomorphTo(m2)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}