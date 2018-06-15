package fin.cse.adks.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.TreeSet;

import cn.edu.pku.sei.plde.qacrashfix.faultlocalization.MyNode;
import cn.edu.pku.sei.plde.qacrashfix.faultlocalization.NodeGenerator;
import fin.cse.adks.models.Code;

/**
 * @author Pavlo Shevchenko (pavlo.shevchenko@st.ovgu.de)
 */
public class Tokenizer {

    /**
     * @param code: code to be tokenized
     * @param distinct: whether a list should contain duplicate tokens.
     * @return a list of code's tokens
     */
    public static Collection<String> getTokens(Code code, boolean distinct) {
        ArrayList<String> tokens = new ArrayList<String>();
        LinkedList<MyNode> nodes = new LinkedList<MyNode>();
        try {
            NodeGenerator ng = new NodeGenerator(code.getCode());
            nodes = ng.getNodes();
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (int j = 0; j < nodes.size(); j++) {
            MyNode myNode = nodes.get(j);
            tokens.add(myNode.node.toString());
        }
        return distinct ? new TreeSet<String>(tokens) : tokens;
    }
}