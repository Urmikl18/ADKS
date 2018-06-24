package fin.cse.adks.models;

import java.util.List;

/**
 * Represents a SOFix sequence: a set of linked modifications.
 * 
 * @author Pavlo Shevchenko (pavlo.shevchenko@st.ovgu.de)
 */
public class Sequence {
    private List<Modification> linkedModificationSequence;

    public Sequence(List<Modification> linkedModificationSequence) {
        this.linkedModificationSequence = linkedModificationSequence;
    }

    public List<Modification> getLinkedModificationSequence() {
        return this.linkedModificationSequence;
    }

    /**
     * @param other a sequence to be checked.
     * @return <b>true</b> if current sequence is isomorph to the <b>other</b> as
     *         defined in SOFix
     */
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