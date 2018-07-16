package fin.cse.adks.extracting;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;

import fin.cse.adks.models.Post;
import fin.cse.adks.utils.Pair;
import fin.cse.adks.utils.XMLTags;

/**
 * Filters out irrelevant posts from the StackOverflow archive.
 * 
 * @author Pavlo Shevchenko (pavlo.shevchenko@st.ovgu.de)
 */
public class PostAnalyzer {
    private String importPath;
    private String exportPath;
    private int threshold;

    private ArrayList<Post> questions;
    private ArrayList<Pair<Post, Post>> qaList;
    private final XMLInputFactory inFactory = XMLInputFactory.newInstance();
    private final XMLOutputFactory outFactory = XMLOutputFactory.newInstance();

    public PostAnalyzer(String importPath, String exportPath, int threshold) {
        this.importPath = importPath;
        this.exportPath = exportPath;
        this.threshold = threshold;
        this.questions = new ArrayList<Post>(1000000);
        this.qaList = new ArrayList<Pair<Post, Post>>(1000000);
    }

    /**
     * @return a list of extracted questions and answers.
     */
    public ArrayList<Pair<Post, Post>> getQAList() {
        return this.qaList;
    }

    /**
     * Extract Q&A posts from a .zip-archive and saves coresponding questions and
     * answers.
     */
    public void extractQAPosts() {
        try {
            ZipFile xmlZip = new ZipFile(this.importPath);
            Enumeration<? extends ZipEntry> postsXML = xmlZip.entries();
            int progress = 0;
            while (postsXML.hasMoreElements()) {
                ZipEntry postXML = postsXML.nextElement();
                InputStream stream = xmlZip.getInputStream(postXML);
                final XMLEventReader reader = inFactory.createXMLEventReader(stream);

                while (reader.hasNext()) {
                    final XMLEvent event = reader.nextEvent();
                    if (event.isStartElement()
                            && event.asStartElement().getName().getLocalPart().equals(XMLTags.ELEMENT_ROW)) {
                        this.processPost(Post.fromXML(event));
                        ++progress;
                        if (progress % (threshold / 10) == 0) {
                            System.out.format("Processed %d posts\n", progress);
                            if (progress == threshold) {
                                break;
                            }
                        }
                    }
                }
                savePosts();
            }
            xmlZip.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void processPost(Post post) throws XMLStreamException {
        // question post
        if (post.getType() == Post.QUESTION) {
            if (post.getTags().contains("java")) {
                this.questions.add(post);
            }
        }
        // answer post
        else {
            Post q = this.findCorrespondingQuestion(post);
            // there is an accepted answer
            if (q != null) {
                this.qaList.add(new Pair<Post, Post>(q, post));
                this.questions.remove(q);
            }
        }
    }

    private Post findCorrespondingQuestion(Post answer) {
        for (Post q : this.questions) {
            if (q.getAcceptedId() == answer.getId() && answer.getParentId() == q.getId()) {
                return q;
            }
        }
        return null;
    }

    private void savePosts() throws XMLStreamException, IOException {
        final XMLStreamWriter writer = outFactory.createXMLStreamWriter(new FileWriter(this.exportPath));
        writer.writeStartDocument();
        writer.writeStartElement(XMLTags.ELEMENT_POSTS);

        for (Pair<Post, Post> pair : this.qaList) {
            savePost(writer, pair.first, pair.second);
        }

        writer.writeEndElement();
        writer.writeEndDocument();

        writer.flush();
        writer.close();
    }

    private void savePost(final XMLStreamWriter writer, Post question, Post answer) throws XMLStreamException {
        // save Q
        writer.writeStartElement(XMLTags.ELEMENT_ROW);
        writer.writeAttribute(XMLTags.ATTRIBUTE_ID, Integer.toString(question.getId()));
        writer.writeAttribute(XMLTags.ATTRIBUTE_POST_TYPE_ID, Integer.toString(question.getType()));
        writer.writeAttribute(XMLTags.ATTRIBUTE_ACCEPTED_ANSWER_ID, Integer.toString(question.getAcceptedId()));
        writer.writeAttribute(XMLTags.ATTRIBUTE_BODY, question.getBody());
        String tags = "";
        if (!question.getTags().isEmpty()) {
            for (String tag : question.getTags()) {
                tags += "<" + tag + ">";
            }
            writer.writeAttribute(XMLTags.ATTRIBUTE_TAGS, tags);
        }
        writer.writeEndElement();
        // save A
        writer.writeStartElement(XMLTags.ELEMENT_ROW);
        writer.writeAttribute(XMLTags.ATTRIBUTE_ID, Integer.toString(answer.getId()));
        writer.writeAttribute(XMLTags.ATTRIBUTE_POST_TYPE_ID, Integer.toString(answer.getType()));
        writer.writeAttribute(XMLTags.ATTRIBUTE_PARENT_ID, Integer.toString(answer.getParentId()));
        writer.writeAttribute(XMLTags.ATTRIBUTE_BODY, answer.getBody());
        tags = "";
        if (!answer.getTags().isEmpty()) {
            for (String tag : answer.getTags()) {
                tags += "<" + tag + ">";
            }
            writer.writeAttribute(XMLTags.ATTRIBUTE_TAGS, tags);
        }
        writer.writeEndElement();
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Wrong parameters! Try again:");
            System.out.println(
                    "java -jar adks_post_analyzer.jar [path_to_stackoverflow_zip] [path_to_xml_to_store_extracted_posts] [threshold_number_of_posts]");
            return;
        }
        new PostAnalyzer(args[0], args[1], Integer.parseInt(args[2])).extractQAPosts();
    }
}