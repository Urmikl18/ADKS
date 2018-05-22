package fin.cse.adks.utils;

import java.util.ArrayList;
import java.util.LinkedList;

import cn.edu.pku.sei.plde.qacrashfix.faultlocalization.MyNode;
import cn.edu.pku.sei.plde.qacrashfix.faultlocalization.NodeGenerator;
import fin.cse.adks.sequenceextractor.Code;

public class Tokenizer {

    public static ArrayList<String> getTokens(Code code) {
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
        return tokens;
    }
}