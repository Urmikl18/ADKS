package fin.cse.adks.models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.TreeSet;

import cn.edu.pku.sei.plde.qacrashfix.faultlocalization.MyNode;
import cn.edu.pku.sei.plde.qacrashfix.faultlocalization.NodeGenerator;

/**
 * @author Pavlo Shevchenko (pavlo.shevchenko@st.ovgu.de)
 */
public class Code implements Comparable<Code> {
    private String code;
    private Collection<String> tokens;
    private final boolean distinct = true;

    public Code(String code) {
        this.code = code;
        this.tokens = this.extractTokens();
    }

    public String getCode() {
        return this.code;
    }

    public Collection<String> getTokens() {
        return this.tokens;
    }

    public double codeSimilarity(Code other) {
        Collection<String> t1 = this.getTokens();
        Collection<String> t2 = other.getTokens();
        final double c = 0.1;
        double commonTokens = 0.0;
        Collection<String> min = t1.size() > t2.size() ? t2 : t1;
        Collection<String> max = t1.size() > t2.size() ? t1 : t2;

        for (String token : min) {
            if (max.contains(token)) {
                commonTokens += 1.0;
            }
        }
        return commonTokens / (c * t1.size() + (1 - c) * t2.size());
    }

    private Collection<String> extractTokens() {
        ArrayList<String> tokens = new ArrayList<String>();
        LinkedList<MyNode> nodes = new LinkedList<MyNode>();
        try {
            NodeGenerator ng = new NodeGenerator(this.code);
            nodes = ng.getNodes();
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (int j = 0; j < nodes.size(); j++) {
            MyNode myNode = nodes.get(j);
            tokens.add(myNode.node.toString());
        }
        return this.distinct ? new TreeSet<String>(tokens) : tokens;
    }

    @Override
    public int compareTo(Code other) {
        return this.getCode().compareTo(other.getCode());
    }
}