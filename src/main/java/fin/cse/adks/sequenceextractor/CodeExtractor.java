package fin.cse.adks.sequenceextractor;

import java.util.ArrayList;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import fin.cse.adks.utils.Pair;
import fin.cse.adks.utils.Tokenizer;

public class CodeExtractor {
    private ArrayList<Pair<Code, Code>> codePairs;

    public CodeExtractor() {
        this.codePairs = new ArrayList<Pair<Code, Code>>();
    }

    public ArrayList<Pair<Code, Code>> getCodePairs() {
        return this.codePairs;
    }

    public void extractCodePairs(ArrayList<Pair<Post, Post>> posts) {
        for (Pair<Post, Post> pair : posts) {
            Post qPost = pair.first;
            Post aPost = pair.second;
            ArrayList<Code> qCode = this.getCode(qPost);
            ArrayList<Code> aCode = this.getCode(aPost);
            ArrayList<Pair<Code, Code>> cP = this.createCodePairs(qCode, aCode);
            this.codePairs.addAll(cP);
        }
    }

    private ArrayList<Code> getCode(Post post) {
        ArrayList<Code> result = new ArrayList<Code>();
        Document doc = Jsoup.parse(post.getBody());
        Elements codes = doc.select("code");
        for (Element code : codes) {
            result.add(new Code(code.text()));
        }
        return result;
    }

    private ArrayList<Pair<Code, Code>> createCodePairs(ArrayList<Code> qCode, ArrayList<Code> aCode) {
        ArrayList<Pair<Code, Code>> result = new ArrayList<Pair<Code, Code>>();
        for (Code qC : qCode) {
            for (Code aC : aCode) {
                if (this.codeSimilarity(qC, aC) >= 0.75) {
                    result.add(new Pair<Code, Code>(qC, aC));
                }
            }
        }
        return result;
    }

    private double codeSimilarity(Code c1, Code c2) {
        ArrayList<String> t1 = Tokenizer.getTokens(c1);
        ArrayList<String> t2 = Tokenizer.getTokens(c2);
        final double c = 0.1;
        double commonTokens = 0.0;
        ArrayList<String> min = t1.size() > t2.size() ? t2 : t1;
        ArrayList<String> max = t1.size() > t2.size() ? t1 : t2;

        for (String token : min) {
            if (max.contains(token)) {
                commonTokens += 1.0;
            }
        }
        return commonTokens / (c * t1.size() + (1 - c) * t2.size());
    }

}