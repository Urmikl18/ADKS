package fin.cse.adks.sequenceextractor;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import cn.edu.pku.sei.plde.qacrashfix.jdt.JDTTreeGenerator;
import cn.edu.pku.sei.plde.qacrashfix.tree.AnswerQuestionMapper;
import cn.edu.pku.sei.plde.qacrashfix.tree.edits.TreeEditAction;
import fin.cse.adks.models.Code;
import fin.cse.adks.models.Modification;
import fin.cse.adks.models.Sequence;
import fin.cse.adks.utils.Pair;
import fin.cse.adks.utils.XMLTags;

/**
 * Extracts sequences from code samples and clusters them to form categories as
 * described in SOFix.
 * 
 * @author Pavlo Shevchenko (pavlo.shevchenko@st.ovgu.de)
 */
public class SequenceExtractor {
    private String importPath;
    private ArrayList<Sequence> sequences;
    private ArrayList<ArrayList<Sequence>> categories;

    private final XMLInputFactory inFactory = XMLInputFactory.newInstance();

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
                if (event.isStartElement()
                        && event.asStartElement().getName().getLocalPart().equals(XMLTags.ELEMENT_ROW)) {
                    if (progress % 1000 == 0) {
                        System.out.format("Processed %d code pairs\n", progress);
                        System.out.format("Extracted %d sequences\n", this.sequences.size());
                    }
                    ++progress;
                    // extract code pair from xml
                    Pair<Integer, Pair<Code, Code>> codePair = this.extractCodePair(event);
                    int id = codePair.first;
                    Code qCode = codePair.second.first;
                    Code aCode = codePair.second.second;
                    // parse buggy and fixed code samples
                    JDTTreeGenerator quesGenerator = new JDTTreeGenerator(qCode.getCode());
                    JDTTreeGenerator ansGenerator = new JDTTreeGenerator(aCode.getCode());
                    // compute the mapping between two ASTs
                    AnswerQuestionMapper aqMapper = new AnswerQuestionMapper(ansGenerator.getTree(),
                            quesGenerator.getTree());
                    List<TreeEditAction> editScript = new ArrayList<>();
                    // get the edit script
                    try {
                        editScript = aqMapper.getEditingScripts();
                    } catch (Exception e) {
                        // intentional continue: can't compute edit script
                        continue;
                    }
                    // edit script to intern representation of the modification
                    List<Modification> modifications = editScript.stream().map(action -> new Modification(id, action))
                            .collect(Collectors.toList());
                    ;
                    // split modifications into linked modification sequences.
                    for (List<Modification> linkedModificationSequence : linkModifications(modifications)) {
                        this.sequences.add(new Sequence(linkedModificationSequence));
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
        String id = event.asStartElement().getAttributeByName(new QName(XMLTags.ATTRIBUTE_ID)).getValue();
        String qCode = event.asStartElement().getAttributeByName(new QName(XMLTags.ATTRIBUTE_BUGGY)).getValue();
        String aCode = event.asStartElement().getAttributeByName(new QName(XMLTags.ATTRIBUTE_FIXED)).getValue();
        return new Pair<Integer, Pair<Code, Code>>(Integer.parseInt(id),
                new Pair<Code, Code>(new Code(qCode), new Code(aCode)));
    }

    private List<List<Modification>> linkModifications(List<Modification> modifications) {
        List<List<Modification>> links = new ArrayList<>(modifications.size());
        for (int i = 0; i < modifications.size(); ++i) {
            links.add(new ArrayList<>());
            links.get(i).add(modifications.get(i));
            for (int j = 0; j < modifications.size(); ++j) {
                if (modifications.get(i).isLinkedTo(modifications.get(j))) {
                    links.get(i).add(modifications.get(j));
                }
            }
        }

        List<List<Modification>> result = new ArrayList<>();
        for (int i = 0; i < links.size(); ++i) {
            if (!links.get(i).isEmpty()) {
                result.add(new ArrayList<>());
                for (Modification modification : links.get(i)) {
                    result.get(result.size() - 1).add(modification);
                    for (int j = i + 1; j < links.size(); ++j) {
                        links.get(j).remove(modification);
                    }
                }
            }
        }

        return result;
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
            if (sequence.isIsomorphTo(representative)) {
                return i;
            }
        }
        return -1;
    }
}