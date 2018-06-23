package fin.cse.adks.sequenceextractor;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import cn.edu.pku.sei.plde.qacrashfix.jdt.JDTTreeGenerator;
import cn.edu.pku.sei.plde.qacrashfix.tree.AnswerQuestionMapper;
import cn.edu.pku.sei.plde.qacrashfix.tree.TreeNode;
import cn.edu.pku.sei.plde.qacrashfix.tree.edits.CopyAction;
import cn.edu.pku.sei.plde.qacrashfix.tree.edits.DeleteAction;
import cn.edu.pku.sei.plde.qacrashfix.tree.edits.InsertAction;
import cn.edu.pku.sei.plde.qacrashfix.tree.edits.InsertRootAction;
import cn.edu.pku.sei.plde.qacrashfix.tree.edits.MoveAction;
import cn.edu.pku.sei.plde.qacrashfix.tree.edits.ReplaceAction;
import cn.edu.pku.sei.plde.qacrashfix.tree.edits.TreeEditAction;
import cn.edu.pku.sei.plde.qacrashfix.tree.edits.UpdateAction;
import fin.cse.adks.models.Code;
import fin.cse.adks.models.Sequence;
import fin.cse.adks.utils.Pair;

/**
 * @author Pavlo Shevchenko (pavlo.shevchenko@st.ovgu.de)
 */
public class SequenceExtractor {
    private String importPath;
    private ArrayList<Sequence> sequences;
    private ArrayList<ArrayList<Sequence>> categories;

    private final XMLInputFactory inFactory = XMLInputFactory.newInstance();

    private static final String ELEMENT_ROW = "row";

    public SequenceExtractor(String importPath) {
        this.importPath = importPath;
        this.sequences = new ArrayList<Sequence>(100000);
        this.categories = new ArrayList<ArrayList<Sequence>>(10000);
    }

    public ArrayList<Sequence> getSequences() {
        return this.sequences;
    }

    public ArrayList<ArrayList<Sequence>> getCategories() {
        return this.categories;
    }

    /**
     * Extracts sequences, respresented as edit scripts that transform the first
     * component of the code pair into the second.
     */
    public void extractSequences() {
        try {
            final XMLEventReader reader = inFactory.createXMLEventReader(new FileInputStream(this.importPath));
            int progress = 1;
            while (reader.hasNext()) {
                final XMLEvent event = reader.nextEvent();
                if (event.isStartElement() && event.asStartElement().getName().getLocalPart().equals(ELEMENT_ROW)) {
                    if (progress % 1000 == 0) {
                        System.out.format("Processed %d code pairs\n", progress);
                        System.out.format("Extracted %d sequences\n", this.sequences.size());
                    }
                    ++progress;
                    Pair<Integer, Pair<Code, Code>> codePair = this.extractCodePair(event);
                    int id = codePair.first;
                    Code qCode = codePair.second.first;
                    Code aCode = codePair.second.second;
                    JDTTreeGenerator quesGenerator = new JDTTreeGenerator(qCode.getCode());
                    JDTTreeGenerator ansGenerator = new JDTTreeGenerator(aCode.getCode());

                    AnswerQuestionMapper aqMapper = new AnswerQuestionMapper(ansGenerator.getTree(),
                            quesGenerator.getTree());
                    List<TreeEditAction> editScript = new ArrayList<>();
                    try {
                        editScript = aqMapper.getEditingScripts();
                    } catch (Exception e) {
                        // intentional continue: can't compute edit script
                        continue;
                    }
                    for (List<TreeEditAction> linkedModificationSequence : linkModifications(editScript)) {
                        this.sequences.add(new Sequence(id, linkedModificationSequence));
                    }
                }
            }
            System.out.format("Extracted %d sequences\n", this.sequences.size());
            this.categories = this.categorizeSequences(this.sequences);
            System.out.format("Found %d categories\n", categories.size());
        } catch (XMLStreamException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private Pair<Integer, Pair<Code, Code>> extractCodePair(final XMLEvent event) throws XMLStreamException {
        String id = event.asStartElement().getAttributeByName(new QName("Id")).getValue();
        String qCode = event.asStartElement().getAttributeByName(new QName("Before")).getValue();
        String aCode = event.asStartElement().getAttributeByName(new QName("After")).getValue();
        return new Pair<Integer, Pair<Code, Code>>(Integer.parseInt(id),
                new Pair<Code, Code>(new Code(qCode), new Code(aCode)));
    }

    private List<List<TreeEditAction>> linkModifications(List<TreeEditAction> editScript) {
        List<List<TreeEditAction>> links = new ArrayList<>(editScript.size());
        for (int i = 0; i < editScript.size(); ++i) {
            links.add(new ArrayList<TreeEditAction>());
            links.get(i).add(editScript.get(i));
            for (int j = 0; j < editScript.size(); ++j) {
                if (areLinked(editScript.get(i), editScript.get(j))) {
                    links.get(i).add(editScript.get(j));
                }
            }
        }

        List<List<TreeEditAction>> result = new ArrayList<>();
        for (int i = 0; i < links.size(); ++i) {
            if (!links.get(i).isEmpty()) {
                result.add(new ArrayList<>());
                for (TreeEditAction action : links.get(i)) {
                    result.get(result.size() - 1).add(action);
                    for (int j = i + 1; j < links.size(); ++j) {
                        links.get(j).remove(action);
                    }
                }
            }
        }

        return result;
    }

    private boolean areLinked(TreeEditAction m1, TreeEditAction m2) {
        if (!m1.equals(m2)) {
            TreeNode t1 = getTarget(m1);
            TreeNode s2 = getSource(m2);
            TreeNode t2 = getTarget(m2);
            if (t1 != null) {
                if (t1.isLeaf() && t2 != null && t2.isLeaf()) {
                    TreeNode t1P = t1.getParent();
                    return (s2 != null && t1P != null && t1.getParent().equals(s2.getParent()))
                            || (t2 != null && t1P != null && t1.getParent().equals(t2.getParent()));
                } else {
                    return (s2 != null && t1.equals(s2.getParent())) || (t2 != null && t1.equals(t2.getParent()));
                }
            }
        }
        return false;
    }

    public TreeNode getSource(TreeEditAction m) {
        if (m instanceof CopyAction) {
            CopyAction tmp = (CopyAction) m;
            return tmp.getOldNode();
        }
        if (m instanceof DeleteAction) {
            DeleteAction tmp = (DeleteAction) m;
            return tmp.getDeletedNode();
        }
        if (m instanceof InsertAction) {
            return null;
        }
        if (m instanceof InsertRootAction) {
            return null;
        }
        if (m instanceof MoveAction) {
            MoveAction tmp = (MoveAction) m;
            return tmp.getReferenceNode();
        }
        if (m instanceof ReplaceAction) {
            ReplaceAction tmp = (ReplaceAction) m;
            return tmp.getReplacedNode();
        }
        if (m instanceof UpdateAction) {
            UpdateAction tmp = (UpdateAction) m;
            return tmp.getNode();
        }
        return null;
    }

    public TreeNode getTarget(TreeEditAction m) {
        if (m instanceof CopyAction) {
            CopyAction tmp = (CopyAction) m;
            return tmp.getCopiedNode();
        }
        if (m instanceof DeleteAction) {
            return null;
        }
        if (m instanceof InsertAction) {
            InsertAction tmp = (InsertAction) m;
            return tmp.getInsertedNode();
        }
        if (m instanceof InsertRootAction) {
            InsertRootAction tmp = (InsertRootAction) m;
            return tmp.getNewRoot();
        }
        if (m instanceof MoveAction) {
            MoveAction tmp = (MoveAction) m;
            return tmp.getMovedNode();
        }
        if (m instanceof ReplaceAction) {
            ReplaceAction tmp = (ReplaceAction) m;
            return tmp.getNewNode();
        }
        if (m instanceof UpdateAction) {
            UpdateAction tmp = (UpdateAction) m;
            return tmp.getNode();
        }
        return null;
    }

    private ArrayList<ArrayList<Sequence>> categorizeSequences(ArrayList<Sequence> sequences) {
        ArrayList<ArrayList<Sequence>> result = new ArrayList<>();
        for (Sequence sequence : sequences) {
            int category = findCategory(sequence, result);
            if (category == -1) {
                result.add(new ArrayList<Sequence>());
                result.get(result.size() - 1).add(sequence);
            } else {
                result.get(category).add(sequence);
            }
        }
        return result;
    }

    private int findCategory(Sequence sequence, ArrayList<ArrayList<Sequence>> categories) {
        for (int i = 0; i < categories.size(); ++i) {
            Sequence representative = categories.get(i).get(0);
            if (areIsomorph(sequence, representative)) {
                return i;
            }
        }
        return -1;
    }

    private boolean areIsomorph(Sequence s1, Sequence s2) {
        int n1 = s1.getLinkedModificationSequence().size();
        int n2 = s2.getLinkedModificationSequence().size();
        if (n1 == n2) {
            for (int i = 0; i < n1; ++i) {
                TreeEditAction m1 = s1.getLinkedModificationSequence().get(i);
                TreeEditAction m2 = s2.getLinkedModificationSequence().get(i);
                if (!areIsomorph(m1, m2)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private boolean areIsomorph(TreeEditAction m1, TreeEditAction m2) {
        // own idea
        if (m1 instanceof CopyAction && m2 instanceof CopyAction) {
            TreeNode p1 = getTarget(m1).getParent();
            TreeNode p2 = getTarget(m2).getParent();
            return p1 != null && p2 != null && p1.getLabel().equals(p2.getLabel());
        }
        if (m1 instanceof DeleteAction && m2 instanceof DeleteAction) {
            TreeNode p1 = getSource(m1).getParent();
            TreeNode p2 = getSource(m2).getParent();
            return p1 != null && p2 != null && p1.getLabel().equals(p2.getLabel());
        }
        if (m1 instanceof InsertAction && m2 instanceof InsertAction) {
            return getTarget(m1).getLabel().equals(getTarget(m2).getLabel());
        }
        if (m1 instanceof InsertRootAction && m2 instanceof InsertRootAction) {
            return getTarget(m1).getLabel().equals(getTarget(m2).getLabel());
        }
        if (m1 instanceof MoveAction && m2 instanceof MoveAction) {
            TreeNode p1 = getTarget(m1).getParent();
            TreeNode p2 = getTarget(m2).getParent();
            return p1 != null && p2 != null && p1.getLabel().equals(p2.getLabel());
        }
        if (m1 instanceof ReplaceAction && m2 instanceof ReplaceAction) {
            return getSource(m1).getLabel().equals(getSource(m2).getLabel())
                    && getTarget(m1).getLabel().equals(getTarget(m2).getLabel());
        }
        if (m1 instanceof UpdateAction && m2 instanceof UpdateAction) {
            return getSource(m1).getLabel().equals(getSource(m2).getLabel());
        }
        return false;
    }

}