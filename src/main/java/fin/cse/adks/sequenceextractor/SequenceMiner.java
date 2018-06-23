package fin.cse.adks.sequenceextractor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import cn.edu.pku.sei.plde.qacrashfix.tree.edits.DeleteAction;
import cn.edu.pku.sei.plde.qacrashfix.tree.edits.InsertAction;
import cn.edu.pku.sei.plde.qacrashfix.tree.edits.MoveAction;
// import ca.pfv.spmf.algorithms.sequentialpatterns.fournier2008_seqdim.*;
import cn.edu.pku.sei.plde.qacrashfix.tree.edits.TreeEditAction;
import fin.cse.adks.models.Sequence;
import jp.ac.titech.cs.se.sparesort.SequenceDatabase;
import jp.ac.titech.cs.se.sparesort.bide.RecursiveBIDE;

public class SequenceMiner {
    private String importPath;
    private String exportPath;
    private double minsup;

    private Map<String, Integer> itemId;
    private Map<Integer, TreeEditAction> actionId;

    // private ArrayList<Sequences> repairPatterns;
    private ArrayList<List<Integer>> repairPatterns;

    public SequenceMiner(String importPath, String exportPath, double minsup) {
        this.importPath = importPath;
        this.exportPath = exportPath;
        this.minsup = minsup;
        this.itemId = new TreeMap<>();
        this.actionId = new TreeMap<>();
        this.repairPatterns = new ArrayList<>(10000);
    }

    public void mineRepairPatterns() {
        SequenceExtractor se = new SequenceExtractor(this.importPath);
        se.extractSequences();
        ArrayList<ArrayList<Sequence>> categories = se.getCategories();
        this.extractItemIds(categories);
        int k = 0;
        int p = 0;
        for (ArrayList<Sequence> category : categories) {
            if (isWorthMining(category)) {
                // SequenceDatabase sdb = this.categoryToSdb(category);
                // AlgoBIDEPlus miner = new AlgoBIDEPlus(this.minsup);
                // Sequences patterns = miner.runAlgorithm(sdb);
                // System.out.println(patterns.toString(sdb.size()));
                // this.repairPatterns.add(patterns);
                // System.out.format("%d SDBs analyzed\n", k++);
                // System.out.format("%d patterns mined\n", this.repairPatterns.size());
                SequenceDatabase<Integer> sdb = this.categoryToSdb(category);
                sdb.setMiningStrategy(new RecursiveBIDE<Integer>());
                try {
                    Map<List<Integer>, Integer> result = sdb
                            .mineFrequentClosedSequences((int) (category.size() * this.minsup));
                    k++;
                    p += result.keySet().size();
                    this.repairPatterns.addAll(result.keySet());

                } catch (Exception e) {
                    continue;
                }
            }
        }
        System.out.format("Analyzed %d SDBs\n", k);
        System.out.format("Mined %d patterns\n", p);
        for (List<Integer> pattern : this.repairPatterns) {
            System.out.println("--------------------------");
            for (Integer id : pattern) {
                System.out.format("%d: %s\n", id, actionId.get(id).toString());
            }
        }
    }

    private void extractItemIds(ArrayList<ArrayList<Sequence>> categories) {
        int id = 0;
        for (ArrayList<Sequence> category : categories) {
            for (Sequence sequence : category) {
                for (TreeEditAction modification : sequence.getLinkedModificationSequence()) {
                    String key = modification.toString();
                    if (!this.itemId.containsKey(key)) {
                        this.itemId.put(key, id);
                        this.actionId.put(id, modification);
                        ++id;
                    }
                }
            }
        }
    }

    private boolean isWorthMining(ArrayList<Sequence> category) {
        boolean cond1 = category.size() < 5;
        Sequence representative = category.get(0);
        long moveActions = representative.getLinkedModificationSequence().stream()
                .filter(action -> action instanceof MoveAction).count();
        long deleteActions = representative.getLinkedModificationSequence().stream()
                .filter(action -> action instanceof DeleteAction).count();
        long insertActions = representative.getLinkedModificationSequence().stream()
                .filter(action -> action instanceof InsertAction).count();
        boolean cond2 = (moveActions + deleteActions) == representative.getLinkedModificationSequence().size();
        boolean cond3 = deleteActions > 2 || insertActions > 2;
        return !(cond1 || cond2 || cond3);
    }

    private SequenceDatabase<Integer> categoryToSdb(ArrayList<Sequence> category) {
        SequenceDatabase<Integer> sdb = new SequenceDatabase<>();
        for (Sequence sequence : category) {
            ArrayList<Integer> intSeq = new ArrayList<>(sequence.getLinkedModificationSequence().size());
            for (TreeEditAction modification : sequence.getLinkedModificationSequence()) {
                intSeq.add(this.itemId.get(modification.toString()));
            }
            sdb.addSequence(intSeq);
        }
        return sdb;
    }

    // private SequenceDatabase categoryToSdb(ArrayList<Sequence> category) {
    // SequenceDatabase sdb = new SequenceDatabase();
    // for (int i = 0; i < category.size(); ++i) {
    // sdb.addSequence(this.mySeqToSpmfSeq(i, category.get(i)));
    // }
    // return sdb;
    // }

    // private
    // ca.pfv.spmf.algorithms.sequentialpatterns.fournier2008_seqdim.Sequence
    // mySeqToSpmfSeq(int id,
    // Sequence sequence) {
    // ca.pfv.spmf.algorithms.sequentialpatterns.fournier2008_seqdim.Sequence
    // spmfSeq = new
    // ca.pfv.spmf.algorithms.sequentialpatterns.fournier2008_seqdim.Sequence(
    // id);
    // Itemset itemset =
    // this.linkedModToItemset(sequence.getLinkedModificationSequence());
    // spmfSeq.addItemset(itemset);
    // return spmfSeq;
    // }

    // private Itemset linkedModToItemset(ArrayList<TreeEditAction> linkedMod) {
    // Itemset itemset = new Itemset();
    // for (TreeEditAction modification : linkedMod) {
    // itemset.addItem(new ItemSimple(this.itemId.get(modification.toString())));
    // }
    // return itemset;
    // }

    public static void main(String[] args) {
        new SequenceMiner(args[0], args[1], Double.parseDouble(args[2])).mineRepairPatterns();
    }
}