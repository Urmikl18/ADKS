package fin.cse.adks;

import java.util.ArrayList;

import fin.cse.adks.sequenceextractor.Code;
import fin.cse.adks.sequenceextractor.CodeExtractor;
import fin.cse.adks.sequenceextractor.Post;
import fin.cse.adks.sequenceextractor.PostAnalyzer;
import fin.cse.adks.utils.Pair;

public class App {
    public static void main(String[] args) {
        System.out.println("Extracted posts:");
        PostAnalyzer pa = new PostAnalyzer("/home/urmikl18/Documents/SoSe18/adks/implementation/posts2k.xml");
        pa.extractQAPosts();
        ArrayList<Pair<Post, Post>> extractedPosts = pa.getQAList();
        for (Pair<Post, Post> pair : extractedPosts) {
            System.out.println("\t(Q: " + pair.first.getId() + " , A: " + pair.second.getId() + ")");
        }

        System.out.println("==================================================");

        System.out.println("Extracted code pairs:");
        CodeExtractor ce = new CodeExtractor();
        ce.extractCodePairs(extractedPosts);
        ArrayList<Pair<Code, Code>> codePairs = ce.getCodePairs();
        for (Pair<Code, Code> pair : codePairs) {
            System.out.println("\t(Q: " + pair.first.getCode() + " , A: " + pair.second.getCode() + ")");
        }

        System.out.println("==================================================");

    }
}
