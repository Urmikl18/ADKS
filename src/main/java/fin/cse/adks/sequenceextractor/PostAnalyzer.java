package fin.cse.adks.sequenceextractor;

import java.io.File;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import fin.cse.adks.utils.Pair;

public class PostAnalyzer {
    private String postsPath;
    private ArrayList<Pair<Post, Post>> qaList;

    public PostAnalyzer(String postsPath) {
        this.postsPath = postsPath;
        this.qaList = new ArrayList<Pair<Post, Post>>();
    }

    public String getPostsPath() {
        return this.postsPath;
    }

    public ArrayList<Pair<Post, Post>> getQAList() {
        return this.qaList;
    }

    public void extractQAPosts() {
        ArrayList<Post> questions = new ArrayList<Post>();
        try {
            // open xml file with posts
            File fXmlFile = new File(this.postsPath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
            doc.getDocumentElement().normalize();

            // get posts
            NodeList posts = doc.getElementsByTagName("row");
            for (int i = 0; i < posts.getLength(); ++i) {
                Post post = Post.fromXML(posts.item(i));
                // question post
                if (post.getType() == Post.QUESTION) {
                    if (post.getTags().contains("java")) {
                        questions.add(post);
                    }
                }
                // answer post
                else {
                    Post q = this.findCorrespondingQuestion(questions, post);
                    // there is an accepted answer
                    if (q != null) {
                        this.qaList.add(new Pair<Post, Post>(q, post));
                        questions.remove(q);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Post findCorrespondingQuestion(ArrayList<Post> questions, Post answer) {
        for (Post q : questions) {
            if (q.getAcceptedId() == answer.getId() && answer.getParentId() == q.getId()) {
                return q;
            }
        }
        return null;
    }

    public void saveFilteredPosts(String exportPath) {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // root elements
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("posts");
            doc.appendChild(rootElement);

            for (Pair<Post, Post> pair : this.qaList) {
                // row elements
                Element qPost = doc.createElement("row");
                rootElement.appendChild(qPost);

                // Q Post
                qPost.setAttribute("Id", Integer.toString(pair.first.getId()));
                qPost.setAttribute("PostTypeId", Integer.toString(pair.first.getType()));
                qPost.setAttribute("AcceptedAnswerId", Integer.toString(pair.first.getAcceptedId()));
                qPost.setAttribute("Body", pair.first.getBody());
                String tags = "";
                if (!pair.first.getTags().isEmpty()) {
                    for (String tag : pair.first.getTags()) {
                        tags += "<" + tag + ">";
                    }
                    qPost.setAttribute("Tags", tags);
                }

                // row elements
                Element aPost = doc.createElement("row");
                rootElement.appendChild(aPost);

                // A Post
                aPost.setAttribute("Id", Integer.toString(pair.second.getId()));
                aPost.setAttribute("PostTypeId", Integer.toString(pair.second.getType()));
                aPost.setAttribute("ParentId", Integer.toString(pair.second.getParentId()));
                aPost.setAttribute("Body", pair.second.getBody());
                if (!pair.second.getTags().isEmpty()) {
                    tags = "";
                    for (String tag : pair.second.getTags()) {
                        tags += "<" + tag + ">";
                    }
                    aPost.setAttribute("Tags", tags);
                }
            }

            // write the content into xml file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(exportPath));
            transformer.transform(source, result);

            System.out.println("File saved!");
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (TransformerException tfe) {
            tfe.printStackTrace();
        }
    }

}