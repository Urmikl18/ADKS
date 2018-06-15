package fin.cse.adks.sequenceextractor;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import fin.cse.adks.models.Code;
import fin.cse.adks.models.Post;
import fin.cse.adks.utils.Pair;
import fin.cse.adks.utils.Tokenizer;

/**
 * @author Pavlo Shevchenko (pavlo.shevchenko@st.ovgu.de) CodeExtractor:
 *         extracts code pairs from a list of Q&A posts.
 */
public class CodeExtractor {
    private String importPath;
    private String exportPath;

    private ArrayList<Pair<Code, Code>> codePairs;

    private final XMLInputFactory inFactory = XMLInputFactory.newInstance();
    private final XMLOutputFactory outFactory = XMLOutputFactory.newInstance();

    private static final String ELEMENT_ROW = "row";
    public static final boolean DISTINCT_TOKENS = true;

    private HashSet<Code> qCode;
    private HashSet<Code> aCode;

    public CodeExtractor(String importPath, String exportPath) {
        this.importPath = importPath;
        this.exportPath = exportPath;
        this.codePairs = new ArrayList<Pair<Code, Code>>(1000000);
        this.qCode = null;
        this.aCode = null;
    }

    /**
     * @return a list of code pairs extracted from Q&A posts.
     */
    public ArrayList<Pair<Code, Code>> getCodePairs() {
        return this.codePairs;
    }

    /**
     * Processes a list of posts and saves extracted code pairs.
     */
    public void extractCodePairs() {
        try {
            final XMLEventReader reader = inFactory.createXMLEventReader(new FileInputStream(this.importPath));
            int progress = 0;
            while (reader.hasNext()) {
                final XMLEvent event = reader.nextEvent();
                if (event.isStartElement() && event.asStartElement().getName().getLocalPart().equals(ELEMENT_ROW)) {
                    Post post = Post.fromXML(event);
                    if (post.getType() == Post.QUESTION) {
                        this.qCode = this.getCode(post);
                    } else {
                        this.aCode = this.getCode(post);
                        ArrayList<Pair<Code, Code>> cP = this.createCodePairs(qCode, aCode);
                        this.codePairs.addAll(cP);
                        if ((++progress) % 10000 == 0) {
                            System.out.format("Processed %d post pairs\n", progress);
                            System.out.format("Extracted %d code pairs\n", this.codePairs.size());
                        }
                    }
                }
            }
            this.saveCodePairs();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private HashSet<Code> getCode(Post post) {
        HashSet<Code> result = new HashSet<Code>();
        Document doc = Jsoup.parse(post.getBody());
        Elements codes = doc.select("code");
        Elements pres = doc.select("pre");
        for (Element code : codes) {
            result.add(new Code(code.text()));
        }
        for (Element pre : pres) {
            result.add(new Code(pre.text()));
        }
        return result;
    }

    private ArrayList<Pair<Code, Code>> createCodePairs(HashSet<Code> qCode, HashSet<Code> aCode) {
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
        Collection<String> t1 = Tokenizer.getTokens(c1, DISTINCT_TOKENS);
        Collection<String> t2 = Tokenizer.getTokens(c2, DISTINCT_TOKENS);
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

    private void saveCodePairs() throws XMLStreamException, IOException {
        final XMLStreamWriter writer = outFactory.createXMLStreamWriter(new FileWriter(this.exportPath));
        writer.writeStartDocument();
        writer.writeStartElement("codes");

        for (Pair<Code, Code> pair : this.codePairs) {
            saveCodePair(writer, pair.first, pair.second);
        }

        writer.writeEndElement();
        writer.writeEndDocument();

        writer.flush();
        writer.close();
    }

    private void saveCodePair(final XMLStreamWriter writer, Code before, Code after) throws XMLStreamException {
        // save Q
        writer.writeStartElement(ELEMENT_ROW);
        writer.writeAttribute("Before", before.getCode());
        writer.writeAttribute("After", after.getCode());
        writer.writeEndElement();
    }

    public static void main(String[] args) {
        new CodeExtractor(args[0], args[1]).extractCodePairs();
    }

}