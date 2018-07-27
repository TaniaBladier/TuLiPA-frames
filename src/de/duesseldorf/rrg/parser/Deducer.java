package de.duesseldorf.rrg.parser;

import java.util.HashSet;
import java.util.Set;

import de.duesseldorf.rrg.RRGNode;
import de.duesseldorf.rrg.RRGNode.RRGNodeType;
import de.duesseldorf.util.GornAddress;

/*
 *  File Deducer.java
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
public class Deducer {

    /**
     * Assuming that both parameters are fitting antecedents, apply the
     * combineSisters deduction rule
     * 
     * @param leftItem
     * @param rightItem
     * @return
     */
    public SimpleRRGParseItem applyCombineSisters(SimpleRRGParseItem leftItem,
            SimpleRRGParseItem rightItem) {
        Set<Gap> gaps = new HashSet<Gap>(leftItem.getGaps());
        gaps.addAll(rightItem.getGaps());
        SimpleRRGParseItem result = new SimpleRRGParseItem(rightItem, null,
                null, SimpleRRGParseItem.NodePos.TOP, leftItem.startPos(), -1,
                gaps, false, false);
        return result;
    }

    /**
     * Assuming that the {@code currentItem} is a fitting antecedent, apply the
     * moveUp deduction rule
     * 
     * @param currentItem
     * @return
     */
    public SimpleRRGParseItem applyMoveUp(SimpleRRGParseItem currentItem) {
        GornAddress motheraddress = currentItem.getNode().getGornaddress()
                .mother();
        RRGNode mothernode = currentItem.getTree().findNode(motheraddress);
        boolean newwsflag = mothernode.getType().equals(RRGNodeType.DDAUGHTER);
        Set<ParseItem> backpointers = new HashSet<ParseItem>();
        backpointers.add(currentItem);

        SimpleRRGParseItem newItem = new SimpleRRGParseItem(currentItem, null,
                mothernode, SimpleRRGParseItem.NodePos.BOT, -1, -1, null,
                newwsflag, true);

        // Debug
        // System.out.println(motheraddress + " is the mother of "
        // + currentItem.getNode().getGornaddress());
        return newItem;
    }

    /**
     * Assuming that {@code CurrentItem} is a fitting antecedent, apply the
     * noLeftSister deduction rule.
     * 
     * @param currentItem
     * @return
     */
    public SimpleRRGParseItem applyNoLeftSister(
            SimpleRRGParseItem currentItem) {
        return new SimpleRRGParseItem(currentItem, null, null,
                SimpleRRGParseItem.NodePos.TOP, -1, -1, null, null, true);

    }

    /**
     * Creates a new item by applying the left-adjoin deduction rule. The tree
     * in {@code auxTreeRoot} is sis-adjoined to the left of the
     * {@code targetSister} node.
     * 
     * @param targetSister
     * @param auxTreeRoot
     * @return
     */
    public SimpleRRGParseItem applyLeftAdjoin(SimpleRRGParseItem targetSister,
            SimpleRRGParseItem auxTreeRoot) {
        // create the list of gaps of the consequent
        Set<Gap> gaps = new HashSet<Gap>(auxTreeRoot.getGaps());
        gaps.addAll(targetSister.getGaps());

        SimpleRRGParseItem result = new SimpleRRGParseItem(targetSister, null,
                null, null, auxTreeRoot.startPos(), -1, gaps, false, false);
        return result;
    }

    /**
     * Creates a new item by applying the right-adjoin deduction rule. The tree
     * in {@code auxTreeRoot} is sis-adjoined to the right of the
     * {@code targetSister} node.
     * 
     * @param target
     * @param auxTreeRoot
     * @return
     */
    public SimpleRRGParseItem applyRightAdjoin(SimpleRRGParseItem target,
            SimpleRRGParseItem auxTreeRoot) {
        // create the list of gaps of the consequent
        Set<Gap> gaps = new HashSet<Gap>(target.getGaps());
        gaps.addAll(auxTreeRoot.getGaps());

        SimpleRRGParseItem result = new SimpleRRGParseItem(target, null, null,
                null, -1, auxTreeRoot.getEnd(), gaps, null, false);
        return result;
    }

    public SimpleRRGParseItem applyCompleteWrapping(
            SimpleRRGParseItem targetRootItem,
            SimpleRRGParseItem fillerddaughterItem, Gap gap) {
        Set<Gap> gaps = new HashSet<Gap>(targetRootItem.getGaps());
        gaps.remove(gap);
        gaps.addAll(fillerddaughterItem.getGaps());
        SimpleRRGParseItem consequent = new SimpleRRGParseItem(
                fillerddaughterItem, null, null, null,
                targetRootItem.startPos(), targetRootItem.getEnd(), gaps, false,
                false);
        return consequent;
    }

}
