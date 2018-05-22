package fin.cse.adks.sequenceextractor;

import java.io.File;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
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

}