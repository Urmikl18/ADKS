package fin.cse.adks.sequenceextractor;

import java.util.ArrayList;
import java.util.Arrays;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class Post {
    public static final int QUESTION = 1;

    private int id;
    private int type;
    private int acceptedId;
    private int parentId;
    private String body;
    private ArrayList<String> tags;

    public Post(String id, String type, String acceptedId, String parentId, String body, String tags) {
        this.setId(id);
        this.setType(type);
        this.setAcceptedId(acceptedId);
        this.setParentId(parentId);
        this.setBody(body);
        this.setTags(tags);
    }

    public int getId() {
        return this.id;
    }

    public int getType() {
        return this.type;
    }

    public int getAcceptedId() {
        return this.acceptedId;
    }

    public int getParentId() {
        return this.parentId;
    }

    public String getBody() {
        return this.body;
    }

    public ArrayList<String> getTags() {
        return this.tags;
    }

    public void setId(String id) {
        this.id = Integer.parseInt(id);
    }

    public void setType(String type) {
        this.type = Integer.parseInt(type);
    }

    public void setAcceptedId(String acceptedId) {
        try {
            this.acceptedId = Integer.parseInt(acceptedId);
        } catch (Exception e) {
            this.acceptedId = -1;
        }
    }

    public void setParentId(String parentId) {
        try {
            this.parentId = Integer.parseInt(parentId);
        } catch (Exception e) {
            this.parentId = -1;
        }
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void setTags(String tags) {
        this.tags = new ArrayList<String>();
        if (!tags.isEmpty()) {
            String tmp = tags.replaceAll("><", ",").replaceAll("<", "").replaceAll(">", "");
            this.tags = new ArrayList<String>(Arrays.asList(tmp.split(",")));
        }
    }

    public static Post fromXML(Node node) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element elem = (Element) node;
            String id = elem.getAttribute("Id");
            String acceptedId = elem.getAttribute("AcceptedAnswerId");
            String parentId = elem.getAttribute("ParentId");
            String type = elem.getAttribute("PostTypeId");
            String body = elem.getAttribute("Body");
            String tags = elem.getAttribute("Tags");
            return new Post(id, type, acceptedId, parentId, body, tags);
        } else {
            return null;
        }
    }
}