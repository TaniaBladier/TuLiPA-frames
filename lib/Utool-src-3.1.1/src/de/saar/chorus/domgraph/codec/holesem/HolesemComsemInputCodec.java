/* Generated By:JavaCC: Do not edit this line. HolesemComsemInputCodec.java */
package de.saar.chorus.domgraph.codec.holesem;

import java.io.*;
import java.util.*;

import de.saar.chorus.domgraph.graph.*;
import de.saar.chorus.domgraph.codec.*;
import org._3pq.jgrapht.Edge;


/**
 * An input codec for Hole Semantics. We use the concrete syntax
 * of the <a href="http://cslipublications.stanford.edu/site/1575864967.html">
 * Blackburn & Bos (2005) textbook</a> on computational semantics.
 * The codec is thus able to process the USRs generated by the software
 * described in that book.<p>
 *
 * The codec implements the translation of Hole Semantics to normal
 * dominance graphs described in 
 * <a href="http://www.coli.uni-saarland.de/~koller/showpaper.php?id=hs-as-dc">
 * Koller et al., EACL 2003</a>. See {@link #decode(Reader,DomGraph,NodeLabels)}
 * for details. <p>
 *
 * An example input for this codec looks as follows:<br/>
 * {@code some(_A,some(_B,some(_C,and(label(_A),and(hole(_B),} <br/>
 * &nbsp; {@code and(label(_C),and(some(_A,X,_B),and(pred1(_C,foo,X),leq(_C,_B)))))))))} 
 *
 * @author Alexander Koller
 */



@CodecMetadata(name="holesem-comsem", extension=".hs.pl")
public class HolesemComsemInputCodec extends InputCodec implements HolesemComsemInputCodecConstants {
        public static final int ERROR_GRAPH_NOT_NORMAL = 1;
        public static final int ERROR_GRAPH_NOT_HNC = 2;
        public static final int ERROR_GRAPH_NOT_LEAF_LABELLED = 3;
        public static final int ERROR_MULTIPLE_PARENTS = 4;

    private DomGraph graph;
    private NodeLabels labels;
  private List<String> varlist;
  private int nextGensymIndex;

        @CodecConstructor
    public HolesemComsemInputCodec() {
        this((Reader) null);

        varlist = new ArrayList<String>();
    }


     /**
     * Reads an USR representation from a <code>Reader</code>. This method
     * converts the USR into an equivalent labelled dominance graph and
     * stores this graph in a <code>DomGraph</code> and the labels in
     * a <code>NodeLabels</code> object.<p>
     *
     * This method converts a Hole Semantics USR into a dominance graph.
     * It will then normalise this graph by moving the lower ends of
     * dominance edges up to the respective roots. The resulting graph
     * is guaranteed to be a correct encoding of the original USR (in
     * that the pluggings of the USR correspond to the solved forms of
     * the dominance graph) if the normalised dominance graph is hypernormally
     * connected. If it isn't, then the method throws a 
     * {@link de.saar.chorus.domgraph.codec.MalformedDomgraphException}.<p>
     *
     * @param reader the reader from which the USR is read
     * @param graph the dominance graph into which the USR is converted
     * @param labels the node labels of the labelled dominance graph
     * @throws IOException if an I/O error occurred while reading from <code>reader</code>
     * @throws ParserException if a syntactic error occurred while parsing the USR
     * @throws MalformedDomgraphException if the graph cannot be normalised,
     * or the result is not hypernormally connected.
     */
    public void decode(Reader reader, DomGraph graph, NodeLabels labels)
        throws IOException, ParserException, MalformedDomgraphException {
        List<String> topNodes = new ArrayList<String>();

        this.graph = graph;
        this.labels = labels;

        graph.clear();
        labels.clear();
        varlist.clear();

        nextGensymIndex = 1;

        try {
            ReInit(reader);
            Input();
        } catch(Throwable e) {
            throw new ParserException(e);
        }

        // move dom edges up to roots
        List<Edge> allEdges = new ArrayList<Edge>();
        allEdges.addAll(graph.getAllEdges());

        for( int i = 0; i < allEdges.size(); i++ ) {
          Edge edge = allEdges.get(i);

          if( graph.getData(edge).getType() == EdgeType.DOMINANCE ) {
            if( !graph.isRoot((String) edge.getTarget()) ) {
              EdgeData data = graph.getData(edge);
              String src = (String) edge.getSource();
              String rootOfTarget = (String) edge.getTarget();

              while( !graph.isRoot(rootOfTarget) ) {
                List<String> parents = graph.getParents(rootOfTarget, EdgeType.TREE);
                if( parents.size() > 1 ) {
                  throw new MalformedDomgraphException("The graph contains a node with multiple parents", ERROR_MULTIPLE_PARENTS);
                } else {
                  rootOfTarget = parents.get(0);
                }
              }

              // System.err.println("Move target of " + edge + " to " + rootOfTarget);
              graph.remove(edge);
              graph.addEdge(src, rootOfTarget, data);
            }
          }
        }

        CodecTools.graphLabelsConsistencyAssertion(graph, labels);

        // at this point, the graph must be normal; otherwise, the
        // encoding was not applicable in the first place.
        if( !graph.isNormal() ) {
          throw new MalformedDomgraphException("The graph is not normal", ERROR_GRAPH_NOT_NORMAL);
        }

        // likewise for hnc and ll
        if( !graph.isHypernormallyConnected() ) {
            throw new MalformedDomgraphException("The graph is not hypernormally connected", ERROR_GRAPH_NOT_HNC);
        }

        if( !graph.isLeafLabelled() ) {
            throw new MalformedDomgraphException("The graph is not leaf-labelled", ERROR_GRAPH_NOT_LEAF_LABELLED);
        }
    }


    private String stripquotes(String label) {
        if( label.startsWith("\'") ) {
            // strip off first and last character
            return label.substring(1, label.length()-1);
        } else {
            return label;
        }
    }


  /*
   * Management of holes, labels, and anonymous nodes.
   *
   * One complication in the holesem syntax is that a Prolog variable
   * can be used in one of three roles: (a) as a hole, (b) as a label,
   * or (c) as an object-level variable bound by an object-level quantifier.
   * In cases (a) and (b), we mark the variable as hole or label when
   * parsing the hole(Var) or label(Var) declaration, and introduce
   * it into the graph immediately. In case (c), we introduce a new
   * anonymous node whose label is the object variable.
   *
   * Case (c) is applied analogously for processing atomic arguments
   * of labelling atoms: introduce new anomyous node with this label.
   */

  private String makeAnonymousNode(String label) {
    String nodename = "hs" + (nextGensymIndex++);
    graph.addNode(nodename, new NodeData(NodeType.LABELLED));
    labels.addLabel(nodename, label);
    return nodename;
  }

  private void addHole(String x) {
    graph.addNode(x, new NodeData(NodeType.UNLABELLED));
  }

  private void addLabel(String x) {
    graph.addNode(x, new NodeData(NodeType.LABELLED));
  }

  private String lookupOrAnonymous(String x) {
    if( graph.hasNode(x) ) {
      return x;
    } else {
      return makeAnonymousNode(x);
    }
  }

/*
 * grammar
 */

// start symbol: Input
  final public void Input() throws ParseException {
    Term();
  }

  final public void Term() throws ParseException {
  String x,y,z,f;
  Token t;
    if (jj_2_1(3)) {
      jj_consume_token(AND);
      jj_consume_token(21);
      Term();
      jj_consume_token(22);
      Term();
      jj_consume_token(23);
    } else if (jj_2_2(5)) {
      jj_consume_token(SOME);
      jj_consume_token(21);
      t = jj_consume_token(PLVAR);
      jj_consume_token(22);
      Term();
      jj_consume_token(23);
    } else {
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case HOLE:
        jj_consume_token(HOLE);
        jj_consume_token(21);
        x = VariableAsHole();
        jj_consume_token(23);
        break;
      case LABEL:
        jj_consume_token(LABEL);
        jj_consume_token(21);
        x = VariableAsLabel();
        jj_consume_token(23);
        break;
      case LEQ:
        jj_consume_token(LEQ);
        jj_consume_token(21);
        x = Variable();
        jj_consume_token(22);
        y = Variable();
        jj_consume_token(23);
    graph.addEdge(y, x,
                  new EdgeData(EdgeType.DOMINANCE));
        break;
      case PRED1:
        jj_consume_token(PRED1);
        jj_consume_token(21);
        x = Variable();
        jj_consume_token(22);
        f = PrologAtom();
        jj_consume_token(22);
        y = VariableOrAtom();
        jj_consume_token(23);
    labels.addLabel(x, stripquotes(f));
    graph.addEdge(x, y, new EdgeData(EdgeType.TREE));
        break;
      case PRED2:
        jj_consume_token(PRED2);
        jj_consume_token(21);
        x = Variable();
        jj_consume_token(22);
        f = PrologAtom();
        jj_consume_token(22);
        y = VariableOrAtom();
        jj_consume_token(22);
        z = VariableOrAtom();
        jj_consume_token(23);
    labels.addLabel(x, stripquotes(f));
    graph.addEdge(x, y, new EdgeData(EdgeType.TREE));
    graph.addEdge(x, z, new EdgeData(EdgeType.TREE));
        break;
      case SOME:
      case AND:
      case OR:
      case IMP:
      case NOT:
      case ALL:
      case QUE:
      case EQ:
        f = LogicalConstant();
        jj_consume_token(21);
        x = Variable();
        jj_consume_token(22);
        Varlist();
        jj_consume_token(23);
    labels.addLabel(x, stripquotes(f));

    for( String child : varlist ) {
      graph.addEdge(x, child,
                    new EdgeData(EdgeType.TREE));
    }

    varlist.clear();
        break;
      default:
        jj_la1[0] = jj_gen;
        jj_consume_token(-1);
        throw new ParseException();
      }
    }
  }

  final public String LogicalConstant() throws ParseException {
  Token t;
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case AND:
      t = jj_consume_token(AND);
              {if (true) return t.image;}
      break;
    case OR:
      t = jj_consume_token(OR);
              {if (true) return t.image;}
      break;
    case IMP:
      t = jj_consume_token(IMP);
              {if (true) return t.image;}
      break;
    case NOT:
      t = jj_consume_token(NOT);
              {if (true) return t.image;}
      break;
    case ALL:
      t = jj_consume_token(ALL);
              {if (true) return t.image;}
      break;
    case SOME:
      t = jj_consume_token(SOME);
              {if (true) return t.image;}
      break;
    case QUE:
      t = jj_consume_token(QUE);
             {if (true) return t.image;}
      break;
    case EQ:
      t = jj_consume_token(EQ);
            {if (true) return t.image;}
      break;
    default:
      jj_la1[1] = jj_gen;
      jj_consume_token(-1);
      throw new ParseException();
    }
    throw new Error("Missing return statement in function");
  }

  final public String PrologAtom() throws ParseException {
  Token t;
  String s;
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case SOME:
    case AND:
    case OR:
    case IMP:
    case NOT:
    case ALL:
    case QUE:
    case EQ:
      s = LogicalConstant();
    {if (true) return s;}
      break;
    case HOLE:
      t = jj_consume_token(HOLE);
      break;
    case LABEL:
      t = jj_consume_token(LABEL);
      break;
    case PRED1:
      t = jj_consume_token(PRED1);
      break;
    case PRED2:
      t = jj_consume_token(PRED2);
      break;
    case PLATOM:
      t = jj_consume_token(PLATOM);
    {if (true) return stripquotes(t.image);}
      break;
    default:
      jj_la1[2] = jj_gen;
      jj_consume_token(-1);
      throw new ParseException();
    }
    throw new Error("Missing return statement in function");
  }

  final public void Varlist() throws ParseException {
  String t,u;
    t = VariableOrAtom();
                       varlist.add(t);
    label_1:
    while (true) {
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case 22:
        ;
        break;
      default:
        jj_la1[3] = jj_gen;
        break label_1;
      }
      jj_consume_token(22);
      u = VariableOrAtom();
                                                                  varlist.add(u);
    }
  }

  final public String VariableOrAtom() throws ParseException {
  String s;
  Token t;
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case PLVAR:
      // can be a variable -- in that case, the node has been computed
        // by the Variable() rule below
        s = Variable();
                 {if (true) return s;}
      break;
    case HOLE:
    case LABEL:
    case SOME:
    case AND:
    case OR:
    case IMP:
    case NOT:
    case ALL:
    case QUE:
    case EQ:
    case PRED1:
    case PRED2:
    case PLATOM:
      s = PrologAtom();
  {if (true) return makeAnonymousNode(s);}
      break;
    default:
      jj_la1[4] = jj_gen;
      jj_consume_token(-1);
      throw new ParseException();
    }
    throw new Error("Missing return statement in function");
  }

  final public String Variable() throws ParseException {
  Token t;
    // Variable -> PLVAR
      t = jj_consume_token(PLVAR);
    // If the variable has been declared as a hole or label, just return
    // this variable. Otherwise, it is an object-level variable, and return
    // an anonymous labelled node.
    {if (true) return lookupOrAnonymous(stripquotes(t.image));}
    throw new Error("Missing return statement in function");
  }

  final public String VariableAsHole() throws ParseException {
  Token t;
    // A variable in the context of a hole(Var) declaration is declared
      // as a hole.
      t = jj_consume_token(PLVAR);
    String s = stripquotes(t.image);
    addHole(s);
    {if (true) return s;}
    throw new Error("Missing return statement in function");
  }

  final public String VariableAsLabel() throws ParseException {
  Token t;
    // A variable in the context of a label(Var) declaration is declared
      // as a label.
      t = jj_consume_token(PLVAR);
    String s = stripquotes(t.image);
    addLabel(s);
    {if (true) return s;}
    throw new Error("Missing return statement in function");
  }

  final private boolean jj_2_1(int xla) {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_1(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(0, xla); }
  }

  final private boolean jj_2_2(int xla) {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_2(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(1, xla); }
  }

  final private boolean jj_3R_6() {
    if (jj_scan_token(PRED1)) return true;
    return false;
  }

  final private boolean jj_3R_17() {
    if (jj_scan_token(EQ)) return true;
    return false;
  }

  final private boolean jj_3R_16() {
    if (jj_scan_token(QUE)) return true;
    return false;
  }

  final private boolean jj_3R_15() {
    if (jj_scan_token(SOME)) return true;
    return false;
  }

  final private boolean jj_3R_5() {
    if (jj_scan_token(LEQ)) return true;
    return false;
  }

  final private boolean jj_3R_14() {
    if (jj_scan_token(ALL)) return true;
    return false;
  }

  final private boolean jj_3R_13() {
    if (jj_scan_token(NOT)) return true;
    return false;
  }

  final private boolean jj_3R_12() {
    if (jj_scan_token(IMP)) return true;
    return false;
  }

  final private boolean jj_3R_11() {
    if (jj_scan_token(OR)) return true;
    return false;
  }

  final private boolean jj_3R_9() {
    Token xsp;
    xsp = jj_scanpos;
    if (jj_3R_10()) {
    jj_scanpos = xsp;
    if (jj_3R_11()) {
    jj_scanpos = xsp;
    if (jj_3R_12()) {
    jj_scanpos = xsp;
    if (jj_3R_13()) {
    jj_scanpos = xsp;
    if (jj_3R_14()) {
    jj_scanpos = xsp;
    if (jj_3R_15()) {
    jj_scanpos = xsp;
    if (jj_3R_16()) {
    jj_scanpos = xsp;
    if (jj_3R_17()) return true;
    }
    }
    }
    }
    }
    }
    }
    return false;
  }

  final private boolean jj_3R_10() {
    if (jj_scan_token(AND)) return true;
    return false;
  }

  final private boolean jj_3R_4() {
    if (jj_scan_token(LABEL)) return true;
    return false;
  }

  final private boolean jj_3R_3() {
    if (jj_scan_token(HOLE)) return true;
    return false;
  }

  final private boolean jj_3_2() {
    if (jj_scan_token(SOME)) return true;
    if (jj_scan_token(21)) return true;
    if (jj_scan_token(PLVAR)) return true;
    if (jj_scan_token(22)) return true;
    if (jj_3R_2()) return true;
    return false;
  }

  final private boolean jj_3R_2() {
    Token xsp;
    xsp = jj_scanpos;
    if (jj_3_1()) {
    jj_scanpos = xsp;
    if (jj_3_2()) {
    jj_scanpos = xsp;
    if (jj_3R_3()) {
    jj_scanpos = xsp;
    if (jj_3R_4()) {
    jj_scanpos = xsp;
    if (jj_3R_5()) {
    jj_scanpos = xsp;
    if (jj_3R_6()) {
    jj_scanpos = xsp;
    if (jj_3R_7()) {
    jj_scanpos = xsp;
    if (jj_3R_8()) return true;
    }
    }
    }
    }
    }
    }
    }
    return false;
  }

  final private boolean jj_3_1() {
    if (jj_scan_token(AND)) return true;
    if (jj_scan_token(21)) return true;
    if (jj_3R_2()) return true;
    return false;
  }

  final private boolean jj_3R_8() {
    if (jj_3R_9()) return true;
    return false;
  }

  final private boolean jj_3R_7() {
    if (jj_scan_token(PRED2)) return true;
    return false;
  }

  public HolesemComsemInputCodecTokenManager token_source;
  SimpleCharStream jj_input_stream;
  public Token token, jj_nt;
  private int jj_ntk;
  private Token jj_scanpos, jj_lastpos;
  private int jj_la;
  public boolean lookingAhead = false;
  private boolean jj_semLA;
  private int jj_gen;
  final private int[] jj_la1 = new int[5];
  static private int[] jj_la1_0;
  static {
      jj_la1_0();
   }
   private static void jj_la1_0() {
      jj_la1_0 = new int[] {0x7ffc0,0x1bf00,0xfbfc0,0x400000,0x1fbfc0,};
   }
  final private JJCalls[] jj_2_rtns = new JJCalls[2];
  private boolean jj_rescan = false;
  private int jj_gc = 0;

  public HolesemComsemInputCodec(java.io.InputStream stream) {
     this(stream, null);
  }
  public HolesemComsemInputCodec(java.io.InputStream stream, String encoding) {
    try { jj_input_stream = new SimpleCharStream(stream, encoding, 1, 1); } catch(java.io.UnsupportedEncodingException e) { throw new RuntimeException(e); }
    token_source = new HolesemComsemInputCodecTokenManager(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 5; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  public void ReInit(java.io.InputStream stream) {
     ReInit(stream, null);
  }
  public void ReInit(java.io.InputStream stream, String encoding) {
    try { jj_input_stream.ReInit(stream, encoding, 1, 1); } catch(java.io.UnsupportedEncodingException e) { throw new RuntimeException(e); }
    token_source.ReInit(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 5; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  public HolesemComsemInputCodec(java.io.Reader stream) {
    jj_input_stream = new SimpleCharStream(stream, 1, 1);
    token_source = new HolesemComsemInputCodecTokenManager(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 5; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  public void ReInit(java.io.Reader stream) {
    jj_input_stream.ReInit(stream, 1, 1);
    token_source.ReInit(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 5; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  public HolesemComsemInputCodec(HolesemComsemInputCodecTokenManager tm) {
    token_source = tm;
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 5; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  public void ReInit(HolesemComsemInputCodecTokenManager tm) {
    token_source = tm;
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 5; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  final private Token jj_consume_token(int kind) throws ParseException {
    Token oldToken;
    if ((oldToken = token).next != null) token = token.next;
    else token = token.next = token_source.getNextToken();
    jj_ntk = -1;
    if (token.kind == kind) {
      jj_gen++;
      if (++jj_gc > 100) {
        jj_gc = 0;
        for (int i = 0; i < jj_2_rtns.length; i++) {
          JJCalls c = jj_2_rtns[i];
          while (c != null) {
            if (c.gen < jj_gen) c.first = null;
            c = c.next;
          }
        }
      }
      return token;
    }
    token = oldToken;
    jj_kind = kind;
    throw generateParseException();
  }

  static private final class LookaheadSuccess extends java.lang.Error { }
  final private LookaheadSuccess jj_ls = new LookaheadSuccess();
  final private boolean jj_scan_token(int kind) {
    if (jj_scanpos == jj_lastpos) {
      jj_la--;
      if (jj_scanpos.next == null) {
        jj_lastpos = jj_scanpos = jj_scanpos.next = token_source.getNextToken();
      } else {
        jj_lastpos = jj_scanpos = jj_scanpos.next;
      }
    } else {
      jj_scanpos = jj_scanpos.next;
    }
    if (jj_rescan) {
      int i = 0; Token tok = token;
      while (tok != null && tok != jj_scanpos) { i++; tok = tok.next; }
      if (tok != null) jj_add_error_token(kind, i);
    }
    if (jj_scanpos.kind != kind) return true;
    if (jj_la == 0 && jj_scanpos == jj_lastpos) throw jj_ls;
    return false;
  }

  final public Token getNextToken() {
    if (token.next != null) token = token.next;
    else token = token.next = token_source.getNextToken();
    jj_ntk = -1;
    jj_gen++;
    return token;
  }

  final public Token getToken(int index) {
    Token t = lookingAhead ? jj_scanpos : token;
    for (int i = 0; i < index; i++) {
      if (t.next != null) t = t.next;
      else t = t.next = token_source.getNextToken();
    }
    return t;
  }

  final private int jj_ntk() {
    if ((jj_nt=token.next) == null)
      return (jj_ntk = (token.next=token_source.getNextToken()).kind);
    else
      return (jj_ntk = jj_nt.kind);
  }

  private java.util.Vector jj_expentries = new java.util.Vector();
  private int[] jj_expentry;
  private int jj_kind = -1;
  private int[] jj_lasttokens = new int[100];
  private int jj_endpos;

  private void jj_add_error_token(int kind, int pos) {
    if (pos >= 100) return;
    if (pos == jj_endpos + 1) {
      jj_lasttokens[jj_endpos++] = kind;
    } else if (jj_endpos != 0) {
      jj_expentry = new int[jj_endpos];
      for (int i = 0; i < jj_endpos; i++) {
        jj_expentry[i] = jj_lasttokens[i];
      }
      boolean exists = false;
      for (java.util.Enumeration e = jj_expentries.elements(); e.hasMoreElements();) {
        int[] oldentry = (int[])(e.nextElement());
        if (oldentry.length == jj_expentry.length) {
          exists = true;
          for (int i = 0; i < jj_expentry.length; i++) {
            if (oldentry[i] != jj_expentry[i]) {
              exists = false;
              break;
            }
          }
          if (exists) break;
        }
      }
      if (!exists) jj_expentries.addElement(jj_expentry);
      if (pos != 0) jj_lasttokens[(jj_endpos = pos) - 1] = kind;
    }
  }

  public ParseException generateParseException() {
    jj_expentries.removeAllElements();
    boolean[] la1tokens = new boolean[24];
    for (int i = 0; i < 24; i++) {
      la1tokens[i] = false;
    }
    if (jj_kind >= 0) {
      la1tokens[jj_kind] = true;
      jj_kind = -1;
    }
    for (int i = 0; i < 5; i++) {
      if (jj_la1[i] == jj_gen) {
        for (int j = 0; j < 32; j++) {
          if ((jj_la1_0[i] & (1<<j)) != 0) {
            la1tokens[j] = true;
          }
        }
      }
    }
    for (int i = 0; i < 24; i++) {
      if (la1tokens[i]) {
        jj_expentry = new int[1];
        jj_expentry[0] = i;
        jj_expentries.addElement(jj_expentry);
      }
    }
    jj_endpos = 0;
    jj_rescan_token();
    jj_add_error_token(0, 0);
    int[][] exptokseq = new int[jj_expentries.size()][];
    for (int i = 0; i < jj_expentries.size(); i++) {
      exptokseq[i] = (int[])jj_expentries.elementAt(i);
    }
    return new ParseException(token, exptokseq, tokenImage);
  }

  final public void enable_tracing() {
  }

  final public void disable_tracing() {
  }

  final private void jj_rescan_token() {
    jj_rescan = true;
    for (int i = 0; i < 2; i++) {
    try {
      JJCalls p = jj_2_rtns[i];
      do {
        if (p.gen > jj_gen) {
          jj_la = p.arg; jj_lastpos = jj_scanpos = p.first;
          switch (i) {
            case 0: jj_3_1(); break;
            case 1: jj_3_2(); break;
          }
        }
        p = p.next;
      } while (p != null);
      } catch(LookaheadSuccess ls) { }
    }
    jj_rescan = false;
  }

  final private void jj_save(int index, int xla) {
    JJCalls p = jj_2_rtns[index];
    while (p.gen > jj_gen) {
      if (p.next == null) { p = p.next = new JJCalls(); break; }
      p = p.next;
    }
    p.gen = jj_gen + xla - jj_la; p.first = token; p.arg = xla;
  }

  static final class JJCalls {
    int gen;
    Token first;
    int arg;
    JJCalls next;
  }

}
