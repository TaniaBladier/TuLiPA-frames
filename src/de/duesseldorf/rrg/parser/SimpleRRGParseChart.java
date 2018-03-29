/*
 *  File SimpleRRGParseChart.java
 *
 *  Authors:
 *     David Arps <david.arps@hhu.de
 *     
 *  Copyright:
 *     David Arps, 2018
 *
 *  This file is part of the TuLiPA system
 *     https://github.com/spetitjean/TuLiPA-frames
 *
 *  TuLiPA is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  TuLiPA is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package de.duesseldorf.rrg.parser;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class SimpleRRGParseChart implements ParseChart {

    /**
     * probably to much:
     * map tree -> node -> i -> j -> ws? -> List of Gaps
     */
    // private HashMap<RRGTree, HashMap<RRGNode, HashMap<Integer,
    // HashMap<Integer, HashMap<Boolean, HashSet<Gap>>>>>> chart;

    // map start index to Parse Items to their backpointers
    private Map<Integer, Map<ParseItem, Set<Set<ParseItem>>>> chart;

    public SimpleRRGParseChart(int sentencelength) {
        // chart = new HashMap<RRGTree, HashMap<RRGNode, HashMap<Integer,
        // HashMap<Integer, HashMap<Boolean, HashSet<Gap>>>>>>();
        chart = new HashMap<Integer, Map<ParseItem, Set<Set<ParseItem>>>>(
                sentencelength + 1);
        for (int i = 0; i <= sentencelength; i++) {
            chart.put(i, new HashMap<ParseItem, Set<Set<ParseItem>>>());
        }
    }

    public boolean containsItem(ParseItem item) {
        int startpos = item.startPos();
        System.out.println(
                "Simple...chart.containsItem() is not tested yet and might be wrong in some cases");
        return chart.get(startpos).containsKey(item);
    }

    /**
     * 
     * @param model
     *            find items in the chart that match the template given by
     *            model. To construct the template, equip the item with
     *            concrete models or to leave values unspecified <br>
     *            - give null for {@code tree}, {@code node}, {@code nodePos},
     *            {@code gaps}, {@code wsflag} <br>
     *            - give -2 for {@code start}, {@code end}
     * 
     * @return
     */
    public Set<SimpleRRGParseItem> findUnderspecifiedItem(
            SimpleRRGParseItem model) {
        Set<SimpleRRGParseItem> result = new HashSet<SimpleRRGParseItem>();

        // note 29.03.2018
        // something is null, and the start and end indices are not correct or
        // something in the map is missing or.... Fix this!

        Set<ParseItem> toCheck = new HashSet<ParseItem>();
        int startboundary = model.startPos() == -2 ? 0 : model.startPos();
        System.out.println("start at " + startboundary);
        int endboundary = model.getEnd() == -2 ? chart.size() : model.getEnd();
        for (int i = startboundary; i <= endboundary; i++) {
            toCheck.addAll(chart.get(i).keySet());
        }

        for (ParseItem s : toCheck) {
            boolean endCheck = model.getEnd() == -2
                    || model.getEnd() == ((SimpleRRGParseItem) s).getEnd();
            if (endCheck) {
                boolean treeCheck = model.getTree() == null || model.getTree()
                        .equals(((SimpleRRGParseItem) s).getTree());
                if (treeCheck) {
                    boolean nodeCheck = model.getNode() == null
                            || model.getNode()
                                    .equals(((SimpleRRGParseItem) s).getNode());
                    if (nodeCheck) {
                        boolean posCheck = model.getNodePos() == null
                                || model.getNodePos().equals(
                                        ((SimpleRRGParseItem) s).getNodePos());
                        if (posCheck) {

                            boolean gapCheck = model.getGaps() == null
                                    || model.getGaps().equals(
                                            ((SimpleRRGParseItem) s).getGaps());
                            if (gapCheck) {
                                boolean wsCheck = (Boolean) model
                                        .getwsflag() == null
                                        || ((Boolean) model.getwsflag())
                                                .equals(((SimpleRRGParseItem) s)
                                                        .getwsflag());
                                if (wsCheck) {
                                    result.add((SimpleRRGParseItem) s);
                                }
                            }
                        }
                    }
                }
            }

        }
        return result;
    }

    /**
     * adds an item to the chart if it is not already in there.
     * 
     * @param consequent
     *            the item that should be added to the chart.
     * @param antecedents
     *            the antecedents from which this item was created.
     * @return true if the ParseItem was not already in the chart
     */
    public boolean addItem(ParseItem consequent, ParseItem... antecedents) {
        Set<ParseItem> antes;
        if (antecedents.length > 0) {
            antes = new HashSet<ParseItem>(Arrays.asList(antecedents));
        } else {
            antes = new HashSet<ParseItem>();
        }
        int startpos = consequent.startPos();

        // was the item in the chart before?
        boolean alreadythere = (chart.get(startpos)).containsKey(consequent);
        if (alreadythere) {
            // just put the additional backpointers
            chart.get(startpos).get(consequent).add(antes);
        } else {
            // add the consequent and a fresh set of backpointers
            Set<Set<ParseItem>> backpointers = new HashSet<Set<ParseItem>>();
            backpointers.add(antes);
            chart.get(startpos).put(consequent, backpointers);
        }
        return !alreadythere;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer("Printing chart\n");
        for (Integer i = 0; i < chart.size(); i++) {
            if (i < chart.size()) {
                sb.append("\nstart index " + i + "\n");
            }
            // print the items
            for (Entry<ParseItem, Set<Set<ParseItem>>> chartEntry : chart.get(i)
                    .entrySet()) {
                sb.append(chartEntry.getKey().toString());

                // and their backpointers
                sb.append(" : {");
                for (Set<ParseItem> bpset : chartEntry.getValue()) {
                    if (bpset.size() > 0) {
                        sb.append("{");
                        for (ParseItem parseItem : bpset) {
                            sb.append(parseItem);
                        }
                        sb.append("}, ");
                    }
                }
                sb.append("}\n");
            }
        }
        return sb.toString();
    }
}