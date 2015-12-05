package CCGInduction.evaluation;

import CCGInduction.ccg.CCGCategoryUtilities;
import CCGInduction.utils.Logger;

import java.util.ArrayList;

/**
 * Datastructure for storing a sentence's dependencies
 */
public class Graph {
  final ArrayList<Edge> edges = new ArrayList<>();
  int sentence_length;

  Graph(int length) { sentence_length = length; }

  void addEdge(int dep, int head, String cat, int slot, EdgeType edgeType) {
    Edge e = new Edge(dep, head,cat,slot, edgeType);
    if (!edgeType.equals(EdgeType.CANDC) || !e.cat.equals("conj")) {
      edges.add(e);
    }
  }

  void addEdge(String line, EdgeType edgeType) {
    int dep = -1;
    int head = -1;
    String cat = "";
    int slot = -1;
    String[] split = Logger.whitespace_pattern.split(line);
    if (edgeType.equals(EdgeType.CONLL)) {
      dep = Integer.valueOf(split[0]);
      head =Integer.valueOf(split[7]);
      cat = split[8];
      slot = -1;
    } else if (edgeType.equals(EdgeType.PARG)) {
      dep = Integer.valueOf(split[0]);
      head = Integer.valueOf(split[1]);
      cat = split[2];
      slot = Integer.valueOf(split[3]);
    } else if (edgeType.equals(EdgeType.CANDC)) {
      String[] v = split[3].split("_");
      dep = Integer.parseInt(v[v.length-1]) - 1;
      v = split[0].split("_");
      head = Integer.parseInt(v[v.length-1]) - 1;
      cat = CCGCategoryUtilities.cleanSemanticsFromCategory(split[1]);
      // Argument index on (S\N)\(S\N) is 1 not
      slot = Integer.parseInt(split[2]);
    }

    Edge e = new Edge(dep, head,cat,slot, edgeType);
    if (!edgeType.equals(EdgeType.CANDC) || !e.cat.equals("conj")) {
      edges.add(e);
    }
  }

  /**
   * Checks if a system edge (other) is contained by the graph.  Metric varies slightly if C&C
   * @param other Edge to check
   * @param mode  Style of evaluation
   * @param edgeType  If comparing to C&C, PARG or CoNLL
   * @return Contains check
   */
  Edge contains(Edge other, EvalMode mode, EdgeType edgeType, boolean syst_has_feats) {
    String syst_cat;
    int syst_ind;
    for(Edge e : edges){
      switch(mode){
      case Undirected:
        if ((e.from == other.to && e.to == other.from)        // If reversed
            || (e.from == other.from && e.to == other.to)) {  // If forward
          return e;
        }
        break;
      case Directed:
        if (e.from == other.from && e.to == other.to) {       // If forward
          return e;
        }
        break;
      case NoFeatures:
        if (e.from == other.from                        // Forward
        && e.to == other.to
        && e.arg == other.arg                           // Arguments and no features
        && CCGCategoryUtilities.noFeats(other.cat).equals(CCGCategoryUtilities.noFeats(e.cat))) {
          return e;
        }
        break;
      case Argument:
        if (e.from == other.from                        // Forward
        && e.to == other.to
        && e.arg == other.arg
        && CCGCategoryUtilities.ArgumentTypesMatch(e.arg, e.cat, other.arg, other.cat, syst_has_feats)) {
          return e;
        }
        break;
      case SimplifiedDCL:
        syst_cat = other.cat;
        syst_ind = other.arg;
        if (syst_has_feats) {
          syst_cat = CCGCategoryUtilities.simplifyCCG(syst_cat);
          syst_ind = edgeType.equals(EdgeType.CANDC) ? syst_ind : CCGCategoryUtilities.simplifyCCG(other.cat, syst_ind);
        }

        if (e.from == other.from
         && e.to == other.to
         && ((edgeType.equals(EdgeType.PARG) && syst_ind == CCGCategoryUtilities.simplifyCCG(e.cat, e.arg)
            || edgeType.equals(EdgeType.CANDC) && syst_ind == e.arg))
         && syst_cat.equals(CCGCategoryUtilities.simplifyCCG(e.cat))) {
          return e;
        }
        break;
      case Simplified:
        syst_cat = other.cat;
        syst_ind = other.arg;
        if (syst_has_feats) {
          syst_cat = CCGCategoryUtilities.dropArgNoFeats(syst_cat);
          syst_ind = edgeType.equals(EdgeType.CANDC) ? syst_ind : CCGCategoryUtilities.dropArgNoFeats(other.cat, syst_ind);
        }
        if (e.from == other.from                        // Forward
         && e.to == other.to
         && ((edgeType.equals(EdgeType.PARG) && syst_ind == CCGCategoryUtilities.dropArgNoFeats(e.cat,e.arg))
               || (edgeType.equals(EdgeType.CANDC) && syst_ind == e.arg))      // C&C arguments are off by one
         && syst_cat.equals(CCGCategoryUtilities.dropArgNoFeats(e.cat))) {
          return e;
        }
        break;
      case Labeled:
        if (e.from == other.from                        // Forward
        && e.to == other.to
        && e.arg == other.arg                           // Arguments and simple cats
        && other.cat.equals(e.cat)) {
          return e;
        }
        break;
      default:
        throw new AssertionError("Invalid Metric: " + mode);
      }
    }
    return null;
  }

  int size(EvalMode mode) {
    if (mode != null && mode == EvalMode.SimplifiedDCL){
      int count = 0;
      for (Edge edge : edges)
        count += CCGCategoryUtilities.simplifyCCG(edge.cat, edge.arg) != 0 ? 1 : 0;
      return count;
    }
    return edges.size();
  }

  public String toString() {
    String s = "";
    for(Edge edge : edges)
      s += edge.toString() + '\n';
    return s;
  }

}
