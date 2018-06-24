package fin.cse.adks.sequenceextractor;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import cn.edu.pku.sei.plde.qacrashfix.tree.edits.DeleteAction;
import cn.edu.pku.sei.plde.qacrashfix.tree.edits.InsertAction;
import cn.edu.pku.sei.plde.qacrashfix.tree.edits.MoveAction;
import fin.cse.adks.models.Modification;
import fin.cse.adks.models.Sequence;
import fin.cse.adks.utils.XMLTags;
import jp.ac.titech.cs.se.sparesort.SequenceDatabase;
import jp.ac.titech.cs.se.sparesort.bide.RecursiveBIDE;

public class SequenceMiner {
    private String importPath;
    private String exportPath;
    private int minsup;

    private ArrayList<List<Modification>> repairPatterns;

    private final XMLOutputFactory outFactory = XMLOutputFactory.newInstance();

    public SequenceMiner(String importPath, String exportPath, int minsup) {
        this.importPath = importPath;
        this.exportPath = exportPath;
        this.minsup = minsup;
        this.repairPatterns = new ArrayList<>(150);
    }

    public void mineRepairPatterns() {
        SequenceExtractor se = new SequenceExtractor(this.importPath);
        se.extractSequences();
        ArrayList<ArrayList<Sequence>> categories = se.getCategories();
        int k = 0;
        for (ArrayList<Sequence> category : categories) {
            if (isWorthMining(category)) {
                SequenceDatabase<Modification> sdb = this.categoryToSdb(category);
                sdb.setMiningStrategy(new RecursiveBIDE<Modification>());
                try {
                    Map<List<Modification>, Integer> result = sdb.mineFrequentClosedSequences(this.minsup);
                    k++;
                    this.repairPatterns.addAll(result.keySet());
                } catch (Exception e) {
                    continue;
                }
            }
        }
        System.out.format("Analyzed %d SDBs\n", k);
        System.out.format("Mined %d patterns\n", this.repairPatterns.size());
        try {
            this.savePatterns();
        } catch (XMLStreamException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private boolean isWorthMining(ArrayList<Sequence> category) {
        boolean cond1 = category.size() < 5;
        Sequence representative = category.get(0);
        long moveActions = representative.getLinkedModificationSequence().stream()
                .filter(modification -> modification.getTreeEditAction() instanceof MoveAction).count();
        long deleteActions = representative.getLinkedModificationSequence().stream()
                .filter(modification -> modification.getTreeEditAction() instanceof DeleteAction).count();
        long insertActions = representative.getLinkedModificationSequence().stream()
                .filter(modification -> modification.getTreeEditAction() instanceof InsertAction).count();
        boolean cond2 = (moveActions + deleteActions) == representative.getLinkedModificationSequence().size();
        boolean cond3 = deleteActions > 2 || insertActions > 2;
        return !(cond1 || cond2 || cond3);
    }

    private SequenceDatabase<Modification> categoryToSdb(ArrayList<Sequence> category) {
        SequenceDatabase<Modification> sdb = new SequenceDatabase<>();
        for (Sequence sequence : category) {
            sdb.addSequence(sequence.getLinkedModificationSequence());
        }
        return sdb;
    }

    private void savePatterns() throws XMLStreamException, IOException {
        final XMLStreamWriter writer = outFactory.createXMLStreamWriter(new FileWriter(this.exportPath));
        writer.writeStartDocument();
        writer.writeStartElement(XMLTags.ELEMENT_PATTERNS);

        for (List<Modification> pattern : this.repairPatterns) {
            this.savePattern(writer, pattern);
        }

        writer.writeEndElement();
        writer.writeEndDocument();

        writer.flush();
        writer.close();
    }

    private void savePattern(final XMLStreamWriter writer, List<Modification> pattern) throws XMLStreamException {
        writer.writeStartElement(XMLTags.ELEMENT_ROW);
        String codeIds = "";
        for (Modification modification : pattern) {
            codeIds += modification.getCodeId() + " ";
        }
        writer.writeAttribute(XMLTags.ATTRIBUTE_CODE_ID, codeIds.trim());
        for (Modification modification : pattern) {
            writer.writeStartElement(XMLTags.ELEMENT_MOD);
            writer.writeAttribute(XMLTags.ATTRIBUTE_VALUE, modification.toString());
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    public static void main(String[] args) {
        new SequenceMiner(args[0], args[1], Integer.parseInt(args[2])).mineRepairPatterns();
    }
}