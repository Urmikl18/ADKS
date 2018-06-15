package fin.cse.adks.models;

import java.util.ArrayList;
import java.util.Arrays;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.XMLEvent;

/**
 * @author Pavlo Shevchenko Data class to represent a StackOverflow post.
 */
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

    public static Post fromXML(final XMLEvent event) throws XMLStreamException {
        String id = event.asStartElement().getAttributeByName(new QName("Id")).getValue();
        String type = event.asStartElement().getAttributeByName(new QName("PostTypeId")).getValue();
        Attribute attAccId = event.asStartElement().getAttributeByName(new QName("AcceptedAnswerId"));
        String acceptedId = attAccId != null ? attAccId.getValue() : null;
        Attribute attParId = event.asStartElement().getAttributeByName(new QName("ParentId"));
        String parentId = attParId != null ? attParId.getValue() : null;
        String body = event.asStartElement().getAttributeByName(new QName("Body")).getValue();
        Attribute attTags = event.asStartElement().getAttributeByName(new QName("Tags"));
        String tags = attTags != null ? attTags.getValue() : "";
        return new Post(id, type, acceptedId, parentId, body, tags);
    }
}