package de.duesseldorf.rrg;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import de.duesseldorf.util.GornAddress;
import de.tuebingen.tag.Fs;
import de.tuebingen.tree.Node;

/*
 *  File RRGNode.java
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
public class RRGNode implements Node {

    public enum RRGNodeType {
        STD, // not another type
        ANCHOR, // anchor node
        LEX, // lexical node
        STAR, // root node of a tree used for sister-adjunction
        SUBST, // substitution leaf node
        DDAUGHTER // d-daughter for wrapping substitution, marks the d-edge
    }

    private List<Node> children; // all children of the Node, in order
    private RRGNodeType type; // the type of this node
    private String name; // the name of the node
    private String category; // the cat of a node, or its terminal label
    private GornAddress gornaddress; // the gorn address
    private Fs nodeFs;

    public RRGNode(RRGNode other) {
        this.type = other.getType();
        this.name = other.name;
        this.category = other.category;

        // deep processing of children
        this.children = new LinkedList<Node>();
        for (Node child : other.getChildren()) {
            children.add(new RRGNode((RRGNode) child));
        }
        this.gornaddress = new GornAddress(other.getGornaddress());
    }

    public RRGNode(RRGNodeType type, String name, String category) {
        children = new LinkedList<Node>();
        this.type = type;
        this.name = name;
        this.setCategory(category);
        this.gornaddress = new GornAddress();
    }

    public RRGNode(RRGNodeType type, String name, String category,
            GornAddress gornaddress, Fs nodeFs) {
        this.setNodeFs(nodeFs);
        children = new LinkedList<Node>();
        this.type = type;
        this.name = name;
        this.setCategory(category);
        this.gornaddress = gornaddress;
    }

    /**
     * unifies this node and the other node by replacing (!) this nodes children
     * with {@code other}s chilren, if the categories of both nodes
     * match.
     * 
     * @param other
     * @return {@code true} iff the unification succeeded
     */
    public boolean nodeUnification(RRGNode other) {
        boolean result = false;
        if (other.getCategory().equals(this.getCategory())) {
            this.setChildren(new LinkedList<Node>(other.getChildren()));
            result = true;
        }
        return result;
    }

    /**
     * returns true iff unification of this node and the other node is possible,
     * i.e. iff the categories of both nodes
     * match.
     * 
     * @param other
     * @return {@code true} iff the unification succeeded
     */
    public boolean nodeUnificationPossible(RRGNode other) {
        boolean result = false;
        if (other.getCategory().equals(this.getCategory())) {
            // this.setChildren(new LinkedList<Node>(other.getChildren()));
            result = true;
            // System.out.println(
            // children.get(1).toString() + children.get(1).getChildren());
        }
        return result;
    }

    public List<Node> getChildren() {
        return children;
    }

    public void setChildren(List<Node> children) {
        this.children = children;
    }

    public void addRightmostChild(Node node) {
        this.children.add(node);
    }

    /**
     * Add the node {@code node} as the {@code position}'s child of
     * {@code this}.
     * 
     * @param node
     * @param position
     */
    public void addXchild(Node node, int position) {
        this.children.add(position, node);
    }

    /**
     * 
     * @return the syntactic category of a node or its terminal label
     */
    public String getCategory() {
        return category;
    }

    private void setCategory(String category) {
        this.category = category;
    }

    public RRGNodeType getType() {
        return this.type;
    }

    // not yet implemented
    public String getName() {
        return this.name;
    }

    // not yet implemented
    public void setName(String name) {
        System.out
                .println("RRGNode.setName was called but is not implemented. ");
        for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
            System.out.println(ste);
        }
    }

    public GornAddress getGornaddress() {
        return gornaddress;
    }

    public void setGornAddress(GornAddress gornaddress) {
        this.gornaddress = gornaddress;
    }

    @Override
    public int hashCode() {
        return Objects.hash(gornaddress, children, type, name, category);
    }

    @Override
    public boolean equals(Object obj) {
        if (this != null && obj != null && obj instanceof RRGNode) {
            return this.hashCode() == obj.hashCode();
        }
        return false;
    }

    /**
     * returns true iff the node category and node type is the same
     * and weakEquals is true for all children of this
     * 
     * @param other
     * @return
     */
    public boolean weakEquals(RRGNode other) {
        boolean baseCase = this.getCategory().equals(other.getCategory())
                && this.getType().equals(other.getType());
        if (!baseCase) {
            // System.out.println("no basecase: " + this + " VS " + other);
            return false;
        }
        // look at the children
        if (this.getChildren().size() != other.getChildren().size()) {
            return false;
        }
        for (int i = 0; i < this.getChildren().size(); i++) {
            RRGNode thisChild = (RRGNode) getChildren().get(i);
            RRGNode otherChild = (RRGNode) other.getChildren().get(i);
            if (!thisChild.weakEquals(otherChild)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return a String representation of this node, without children
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(this.gornaddress.toString());
        sb.append(" ");
        sb.append(this.category);
        sb.append(" ");
        sb.append(this.name);
        sb.append(" (");
        sb.append(this.type.name());
        sb.append(")");
        if (nodeFs != null) {
            sb.append(nodeFs.toString());
        }
        return sb.toString();
    }

    public void setType(RRGNodeType type) {
        this.type = type;
    }

    public Fs getNodeFs() {
        return nodeFs;
    }

    public void setNodeFs(Fs nodeFs) {
        this.nodeFs = nodeFs;
    }

}
