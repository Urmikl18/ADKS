package fin.cse.adks.sequenceextractor;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;

import cn.edu.pku.sei.plde.qacrashfix.jdt.JDTTreeGenerator;
import cn.edu.pku.sei.plde.qacrashfix.tree.AnswerQuestionMapper;
import cn.edu.pku.sei.plde.qacrashfix.tree.edits.TreeEditAction;
import fin.cse.adks.models.Code;
import fin.cse.adks.models.Sequence;
import fin.cse.adks.utils.Pair;

/**
 * @author Pavlo Shevchenko (pavlo.shevchenko@st.ovgu.de)
 */
public class SequenceExtractor {
    private String importPath;
    private String exportPath;
    private ArrayList<Sequence> sequences;

    private final XMLInputFactory inFactory = XMLInputFactory.newInstance();
    private final XMLOutputFactory outFactory = XMLOutputFactory.newInstance();

    private static final String ELEMENT_ROW = "row";
    private static final String ELEMENT_OPERATION = "operation";

    public SequenceExtractor(String importPath, String exportPath) {
        this.importPath = importPath;
        this.exportPath = exportPath;
        this.sequences = new ArrayList<Sequence>(1000000);
    }

    public ArrayList<Sequence> getSequences() {
        return this.sequences;
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
                    if (progress % 10000 == 0) {
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
                    this.sequences.add(new Sequence(id, editScript));
                }
            }
            this.saveSequences();
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

    private void saveSequences() throws XMLStreamException, IOException {
        final XMLStreamWriter writer = outFactory.createXMLStreamWriter(new FileWriter(this.exportPath));
        writer.writeStartDocument();
        writer.writeStartElement("sequences");

        for (Sequence sequence : this.sequences) {
            saveSequence(writer, sequence);
        }

        writer.writeEndElement();
        writer.writeEndDocument();

        writer.flush();
        writer.close();
    }

    private void saveSequence(final XMLStreamWriter writer, Sequence sequence) throws XMLStreamException {
        writer.writeStartElement(ELEMENT_ROW);
        writer.writeAttribute("Id", Integer.toString(sequence.getId()));
        for (TreeEditAction operation : sequence.getEditScript()) {
            writer.writeStartElement(ELEMENT_OPERATION);
            writer.writeAttribute("Value", operation.toString());
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    public static void main(String[] args) {
        new SequenceExtractor(args[0], args[1]).extractSequences();
    }
}