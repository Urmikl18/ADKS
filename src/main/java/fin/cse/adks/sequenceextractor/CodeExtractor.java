package fin.cse.adks.sequenceextractor;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
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

/**
 * Extracts code pairs from a list of Q&A posts.
 * 
 * @author Pavlo Shevchenko (pavlo.shevchenko@st.ovgu.de)
 */
public class CodeExtractor {
    private String importPath;
    private String exportPath;

    private ArrayList<Pair<Code, Code>> codePairs;

    private final XMLInputFactory inFactory = XMLInputFactory.newInstance();
    private final XMLOutputFactory outFactory = XMLOutputFactory.newInstance();

    private static final String ELEMENT_ROW = "row";

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
                double sim = qC.codeSimilarity(aC);
                if (sim >= 0.75 && sim < 1) {
                    result.add(new Pair<Code, Code>(qC, aC));
                }
            }
        }
        return result;
    }

    private void saveCodePairs() throws XMLStreamException, IOException {
        final XMLStreamWriter writer = outFactory.createXMLStreamWriter(new FileWriter(this.exportPath));
        writer.writeStartDocument();
        writer.writeStartElement("codes");

        for (int i = 0; i < this.codePairs.size(); ++i) {
            Pair<Code, Code> pair = this.codePairs.get(i);
            this.saveCodePair(writer, i + 1, pair.first, pair.second);
        }

        writer.writeEndElement();
        writer.writeEndDocument();

        writer.flush();
        writer.close();
    }

    private void saveCodePair(final XMLStreamWriter writer, int id, Code before, Code after) throws XMLStreamException {
        writer.writeStartElement(ELEMENT_ROW);
        writer.writeAttribute("Id", Integer.toString(id));
        writer.writeAttribute("Before", before.getCode());
        writer.writeAttribute("After", after.getCode());
        writer.writeEndElement();
    }

    public static void main(String[] args) {
        new CodeExtractor(args[0], args[1]).extractCodePairs();
    }

}