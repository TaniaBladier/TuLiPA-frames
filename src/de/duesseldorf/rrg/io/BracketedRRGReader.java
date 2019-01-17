package de.duesseldorf.rrg.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import de.duesseldorf.rrg.RRG;
import de.duesseldorf.rrg.RRGTree;

public class BracketedRRGReader {

    File grammar = null;
    private SystemLogger log;

    public BracketedRRGReader(File grammar) {
        this.grammar = grammar;
        this.log = new SystemLogger(System.err, true);
    }

    /**
     * create a Reader for the trees file and go through it line by line,
     * creating an RRG object consisting of lexicalised trees.
     * 
     * @return
     */
    public RRG parseRRG() {
        Set<RRGTree> resultingTrees = new HashSet<RRGTree>();
        // TODO all references to the treestrings can be removed when proper
        // trees are created
        List<String> treeStrings = new LinkedList<String>();
        try {
            BufferedReader tsvFileReader = new BufferedReader(
                    new FileReader(grammar));
            String nextLine = tsvFileReader.readLine();
            while (nextLine != null) {
                // the most innovative condition to filter out lines without
                // trees
                if (nextLine.contains("(")) {
                    treeStrings.add(nextLine);
                    RRGTree treeFromCurrentLine = new TreeFromBracketedStringRetriever(
                            nextLine).createTree();
                    log.info("created tree: " + treeFromCurrentLine);
                }
                nextLine = tsvFileReader.readLine();
            }
            tsvFileReader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // for (String treeString : treeStrings) {
        // System.out.println(treeString);
        // }
        System.exit(1);
        return new RRG(resultingTrees);
    }
}
