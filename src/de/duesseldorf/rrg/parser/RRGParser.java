package de.duesseldorf.rrg.parser;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import de.duesseldorf.frames.Situation;
import de.duesseldorf.rrg.RRG;
import de.duesseldorf.rrg.RRGNode;
import de.duesseldorf.rrg.RRGTree;
import de.duesseldorf.rrg.parser.SimpleRRGParseItem.NodePos;

/**
 * TODO
 * extract a ItemBuilder class
 * addToChartAndAgenda method also checks for goal items, or check for goal
 * items in the chart class
 * extract methods/class for testing the requirements
 * extract Deducer class that holds the inference rules
 * 
 * @author david
 *
 */
public class RRGParser {

    private Situation situation;
    private SimpleRRGParseChart chart;
    private ConcurrentSkipListSet<SimpleRRGParseItem> agenda;
    private RequirementFinder requirementFinder;
    private Deducer deducer;

    public RRGParser(Situation sit) {
        this.situation = sit;
        this.requirementFinder = new RequirementFinder();
        this.deducer = new Deducer();
    }

    public boolean parseSentence(List<String> toksentence) {
        this.agenda = new ConcurrentSkipListSet<SimpleRRGParseItem>();
        this.chart = new SimpleRRGParseChart(toksentence.size());
        scan(toksentence);
        // Debug:
        System.out.println("Done scanning: ");
        // System.out.println(chart.toString());
        // System.out.println("Agenda size: " + agenda.size());
        while (!agenda.isEmpty()) {
            SimpleRRGParseItem currentItem = agenda.pollFirst();
            System.out.println("cI: " + currentItem);
            noleftsister(currentItem);
            moveup(currentItem);
        }
        System.out.println(chart.toString());
        System.out.println("Agenda size: " + agenda.size());
        return false;
    }

    /**
     * 
     * @param consequent
     * @param antecedents
     */
    private void addToChartAndAgenda(SimpleRRGParseItem consequent,
            SimpleRRGParseItem... antecedents) {
        if (chart.addItem(consequent, antecedents)) {
            agenda.add(consequent);
        }
        // Debug
        System.out.println("cons: " + consequent + "\n\n");
    }

    private void moveup(SimpleRRGParseItem currentItem) {

        System.out.println("currentnode: " + currentItem.getNode());
        boolean moveupreq = requirementFinder.moveupReq(currentItem);
        if (moveupreq) {
            SimpleRRGParseItem newItem = deducer.applyMoveUp(currentItem);

            addToChartAndAgenda(newItem, currentItem);
        }
    }

    /**
     * note that NLS is the only deduction rule that can be done for items in
     * BOT position on a leftmost daughter node
     * 
     * @param currentItem
     */
    private void noleftsister(SimpleRRGParseItem currentItem) {

        boolean nlsrequirements = requirementFinder.nlsReq(currentItem);
        if (nlsrequirements) {
            Set<ParseItem> backpointers = new HashSet<ParseItem>();
            backpointers.add(currentItem);
            SimpleRRGParseItem newItem = new SimpleRRGParseItem(currentItem,
                    null, SimpleRRGParseItem.NodePos.TOP, null, null, null,
                    null, backpointers);
            addToChartAndAgenda(newItem, currentItem);
        }
    }

    /**
     * apply the scanning deduction rule
     */
    private void scan(List<String> sentence) {
        // Look at all trees
        for (RRGTree tree : ((RRG) situation.getGrammar()).getTrees()) {
            // Look at all words
            for (int start = 0; start < sentence.size(); start++) {
                String word = sentence.get(start);

                // See if the word is a lex Node of the tree
                if (tree.getLexNodes().containsValue(word)) {
                    for (Entry<RRGNode, String> lexLeaf : tree.getLexNodes()
                            .entrySet()) {
                        // If so, create a new item and add it to the chart and
                        // agenda
                        if (lexLeaf.getValue().equals(word)) {
                            SimpleRRGParseItem scannedItem = new SimpleRRGParseItem(
                                    tree, lexLeaf.getKey(), NodePos.BOT, start,
                                    start + 1, new LinkedList<Gap>(), false,
                                    new HashSet<ParseItem>());
                            addToChartAndAgenda(scannedItem);
                        }
                    }
                }
            }
        }
    }
}
