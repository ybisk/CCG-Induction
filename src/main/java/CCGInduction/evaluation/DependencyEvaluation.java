package CCGInduction.evaluation;

import CCGInduction.ccg.CCGCategoryUtilities;
import CCGInduction.utils.Math.SimpleMath;
import CCGInduction.utils.ObjectDoublePair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * This class computes dependency score and optionally prints verbose analysis
 */
public class DependencyEvaluation {
  // Edges for verbose
  private static final HashMap<String,Integer> gold_edge_counts = new HashMap<>();
  private static final HashMap<String,Integer> syst_edge_counts = new HashMap<>();
  private static final HashMap<String,Integer> oracle_syst_edge_counts = new HashMap<>();
  private static final HashMap<String,Integer> correct_edge_counts = new HashMap<>();
  private static final HashMap<String,Integer> oracle_correct_edge_counts = new HashMap<>();
  private static int correct = 0;
  private static int oracle_correct = 0;
  private static int syst_total_count = 0;
  private static int oracle_syst_total_count = 0;
  private static int gold_total_count = 0;
  private static final HashMap<Integer, Integer> correct_byLen = new HashMap<>();
  private static final HashMap<Integer, Integer> oracle_correct_byLen = new HashMap<>();
  private static final HashMap<Integer, Integer> syst_total_count_byLen = new HashMap<>();
  private static final HashMap<Integer, Integer> oracle_syst_total_count_byLen = new HashMap<>();
  private static final HashMap<Integer, Integer> gold_total_count_byLen = new HashMap<>();
  private static final HashMap<Integer, Integer> correct_byDepLen = new HashMap<>();
  private static final HashMap<Integer, Integer> oracle_correct_byDepLen = new HashMap<>();
  private static final HashMap<Integer, Integer> syst_total_count_byDepLen = new HashMap<>();
  private static final HashMap<Integer, Integer> oracle_syst_total_count_byDepLen = new HashMap<>();
  private static final HashMap<Integer, Integer> gold_total_count_byDepLen = new HashMap<>();
  private static Arguments arguments;
  private static EvalMode mode;
  private static boolean compute_verbose_f1 = false;

  /**
   * Evaluate a set of Gold dependencies against a set of K per sentence Predicted
   * @param gold   Gold dependency graphs
   * @param system Predicted dependency graphs
   * @param mode   Style of evaluation
   * @param edgeType     If comparing to C&C, PARG, CoNLL
   * @param arguments   Pass commandline arguments
   */
  public static void evaluate(ArrayList<Graph> gold, ArrayList<ArrayList<Graph>> system, EvalMode mode,
                              EdgeType edgeType, Arguments arguments) {
    reset(mode, arguments);
    // You can only compute verbose F1 if system predicts labels
    if (mode == EvalMode.Simplified
        || mode == EvalMode.SimplifiedDCL
        || mode == EvalMode.NoFeatures
        || mode == EvalMode.Labeled)
      compute_verbose_f1 = true;

    // For every sentence
    for (int i = 0; i < gold.size(); ++i) {
      Graph goldGraph = gold.get(i);
      ArrayList<Graph> systGraphs = system.get(i);
      goldGraph.sentence_length = systGraphs.get(0).sentence_length;
      if (goldGraph.size(edgeType == EdgeType.CONLL ? null : mode) != 0 && (goldGraph.sentence_length <= arguments.maxLength)) {
        gold_total_count += goldGraph.size(edgeType == EdgeType.CONLL ? null : mode);


        addVerboseGoldEdges(goldGraph, edgeType);

        // Compute Correct edges and # of predicted edges
        correct += score(goldGraph, systGraphs.get(0), mode, edgeType, arguments.system_has_features);
        syst_total_count += systGraphs.get(0).size(arguments.system_has_features ? mode : null);
        addVerboseSystemEdges(systGraphs.get(0), goldGraph, edgeType, arguments.system_has_features);

        // Compute oracle's correct edges and # of predicted edges
        int bestScore = 0;
        Graph bestGraph = systGraphs.get(0);          // Init with viterbi
        if ((goldGraph.size(null) != bestGraph.size(null)) && (bestGraph.size(null) != 0))
          System.err.println("Dependency numbers don't match: " + i);
        if (arguments.oracle) {
          // Choose best system graph
          for (Graph systemGraph : systGraphs) {
            int score = score(goldGraph, systemGraph, mode, edgeType, arguments.system_has_features);
            if (score > bestScore) {
              bestScore = score;
              bestGraph = systemGraph;
            }
          }
          oracle_syst_total_count += bestGraph.size(arguments.system_has_features ? mode : null);
          oracle_correct += bestScore;
          addVerboseSystemOracleEdges(bestGraph, goldGraph, edgeType, arguments.system_has_features);
        }
      }
    }
    printScores();
    printVerbose();
  }

  private static void reset(EvalMode evalMode, Arguments args){
    arguments = args;
    mode = evalMode;
    gold_edge_counts.clear();
    syst_edge_counts.clear();
    oracle_correct_edge_counts.clear();
    correct_edge_counts.clear();
    oracle_correct_edge_counts.clear();
    correct = 0;
    oracle_correct = 0;
    syst_total_count = 0;
    oracle_syst_total_count = 0;
    gold_total_count = 0;

    correct_byLen.clear();
    oracle_correct_byLen.clear();
    syst_total_count_byLen.clear();
    oracle_syst_total_count_byLen.clear();
    gold_total_count_byLen.clear();

    correct_byDepLen.clear();
    oracle_correct_byDepLen.clear();
    syst_total_count_byDepLen.clear();
    oracle_syst_total_count_byDepLen.clear();
    gold_total_count_byDepLen.clear();
  }

  private static void addVerboseGoldEdges(Graph goldGraph, EdgeType edgeType) {
    // gold edge counts
    if(arguments.verbose) {
      for(Edge gold_edge : goldGraph.edges) {
        String cat = simplifyGoldCategory(gold_edge, edgeType);

        if(!gold_edge_counts.containsKey(cat)) {
          gold_edge_counts.put(cat,0);
        }
        gold_edge_counts.put(cat, gold_edge_counts.get(cat)+1);
      }
    }

    if(arguments.verbose_length) {
      if (!gold_total_count_byLen.containsKey(goldGraph.sentence_length))
        gold_total_count_byLen.put(goldGraph.sentence_length, 0);
      gold_total_count_byLen.put(goldGraph.sentence_length, gold_total_count_byLen.get(goldGraph.sentence_length) + goldGraph.size(mode));
    }
    if (arguments.verbose_depLength) {
      for(Edge gold_edge : goldGraph.edges) {
        Integer length = Math.abs(gold_edge.from - gold_edge.to);
        if (!gold_total_count_byDepLen.containsKey(length))
          gold_total_count_byDepLen.put(length, 0);
        gold_total_count_byDepLen.put(length, gold_total_count_byDepLen.get(length) + 1);
      }
    }
  }

  private static void addVerboseSystemEdges(Graph systGraph, Graph goldGraph, EdgeType edgeType, boolean systFeats) {
    if (arguments.verbose) {
      // For every edge produced by the syst
      for (Edge system_edge : systGraph.edges) {
        String cat;
        if (arguments.system_has_features && mode.equals(EvalMode.Simplified))
          cat = CCGCategoryUtilities.dropArgNoFeats(system_edge.cat) + ' ' + (edgeType.equals(EdgeType.PARG) ? CCGCategoryUtilities.dropArgNoFeats(system_edge.cat, system_edge.arg) : system_edge.arg);
        else if (mode.equals(EvalMode.SimplifiedDCL))
          cat = CCGCategoryUtilities.simplifyCCG(system_edge.cat) + ' ' + (edgeType.equals(EdgeType.PARG) ? CCGCategoryUtilities.simplifyCCG(system_edge.cat, system_edge.arg) : system_edge.arg);
        else
          cat = system_edge.cat + ' ' + (system_edge.arg == -1 ? "" : system_edge.arg);

        Edge gold = goldGraph.contains(system_edge, mode, edgeType, systFeats);
        if (compute_verbose_f1) {
          if (gold != null) {
            if (!correct_edge_counts.containsKey(cat)) {
              correct_edge_counts.put(cat, 0);
            }
            correct_edge_counts.put(cat, correct_edge_counts.get(cat) + 1);
          }
          if (!syst_edge_counts.containsKey(cat))
            syst_edge_counts.put(cat, 0);
          syst_edge_counts.put(cat, syst_edge_counts.get(cat) + 1);
        } else {
          if (gold != null) {
            cat = simplifyGoldCategory(gold, edgeType);

            if (!correct_edge_counts.containsKey(cat)) {
              correct_edge_counts.put(cat, 0);
            }
            correct_edge_counts.put(cat, correct_edge_counts.get(cat) + 1);
          }
          if (!syst_edge_counts.containsKey(cat))
            syst_edge_counts.put(cat, 0);
          syst_edge_counts.put(cat, syst_edge_counts.get(cat) + 1);
        }
      }
    }

    if (arguments.verbose_length) {
      if (!correct_byLen.containsKey(systGraph.sentence_length)) {
        correct_byLen.put(systGraph.sentence_length, 0);
        syst_total_count_byLen.put(systGraph.sentence_length, 0);
      }

      correct_byLen.put(systGraph.sentence_length, correct_byLen.get(systGraph.sentence_length)
          + score(goldGraph, systGraph, mode, edgeType, arguments.system_has_features));
      syst_total_count_byLen.put(systGraph.sentence_length, syst_total_count_byLen.get(systGraph.sentence_length)
          + systGraph.size(arguments.system_has_features ? mode : null));
    }

    if (arguments.verbose_depLength) {
      for (Edge edge : correct_edges(goldGraph, systGraph, mode, edgeType, systFeats)) {
        Integer length = Math.abs(edge.from - edge.to);
        if (!correct_byDepLen.containsKey(length))
          correct_byDepLen.put(length, 0);
        correct_byDepLen.put(length, correct_byDepLen.get(length) + 1);
      }

      for (Edge edge : systGraph.edges) {
        Integer length = Math.abs(edge.from - edge.to);
        if (!syst_total_count_byDepLen.containsKey(length))
          syst_total_count_byDepLen.put(length, 0);
        syst_total_count_byDepLen.put(length, syst_total_count_byDepLen.get(length) + 1);
      }
    }
  }
  private static void addVerboseSystemOracleEdges(Graph bestGraph, Graph goldGraph, EdgeType edgeType, boolean systFeats) {
    if (arguments.verbose && arguments.oracle) {
      // For every edge produced by the best syst
      for (Edge system_edge : bestGraph.edges) {
        String cat;
        if (arguments.system_has_features && mode.equals(EvalMode.Simplified))
          cat = CCGCategoryUtilities.dropArgNoFeats(system_edge.cat) + ' ' + (edgeType.equals(EdgeType.PARG) ? CCGCategoryUtilities.dropArgNoFeats(system_edge.cat, system_edge.arg) : system_edge.arg);
        else if (mode.equals(EvalMode.SimplifiedDCL))
          cat = CCGCategoryUtilities.simplifyCCG(system_edge.cat) + ' ' + (edgeType.equals(EdgeType.PARG) ? CCGCategoryUtilities.simplifyCCG(system_edge.cat, system_edge.arg) : system_edge.arg);
        else
          cat = system_edge.cat + ' ' + (system_edge.arg == -1 ? "" : system_edge.arg);

        Edge gold = goldGraph.contains(system_edge, mode, edgeType, systFeats);
        if (compute_verbose_f1) {
          if (gold != null) {
            if (!oracle_correct_edge_counts.containsKey(cat)) {
              oracle_correct_edge_counts.put(cat, 0);
            }
            oracle_correct_edge_counts.put(cat, oracle_correct_edge_counts.get(cat) + 1);
          }
          if (!oracle_syst_edge_counts.containsKey(cat))
            oracle_syst_edge_counts.put(cat, 0);
          oracle_syst_edge_counts.put(cat, oracle_syst_edge_counts.get(cat) + 1);
        } else {
          if (gold != null) {
            cat = simplifyGoldCategory(gold, edgeType);

            if (!oracle_correct_edge_counts.containsKey(cat)) {
              oracle_correct_edge_counts.put(cat, 0);
            }
            oracle_correct_edge_counts.put(cat, oracle_correct_edge_counts.get(cat) + 1);
          }
          if (!oracle_syst_edge_counts.containsKey(cat))
            oracle_syst_edge_counts.put(cat, 0);
          oracle_syst_edge_counts.put(cat, oracle_syst_edge_counts.get(cat) + 1);
        }
      }
    }

    if(arguments.verbose_length) {
      if (!oracle_correct_byLen.containsKey(bestGraph.sentence_length)) {
        oracle_correct_byLen.put(bestGraph.sentence_length, 0);
        oracle_syst_total_count_byLen.put(bestGraph.sentence_length, 0);
      }

      oracle_correct_byLen.put(bestGraph.sentence_length, oracle_correct_byLen.get(bestGraph.sentence_length)
          + score(goldGraph,bestGraph, mode, edgeType, arguments.system_has_features));
      oracle_syst_total_count_byLen.put(bestGraph.sentence_length, oracle_syst_total_count_byLen.get(bestGraph.sentence_length)
          + bestGraph.size(mode));
    }

    if (arguments.verbose_depLength) {
      for (Edge edge : correct_edges(goldGraph, bestGraph, mode, edgeType, systFeats)) {
        Integer length = Math.abs(edge.from - edge.to);
        if (!oracle_correct_byDepLen.containsKey(length))
          oracle_correct_byDepLen.put(length, 0);
        oracle_correct_byDepLen.put(length, oracle_correct_byDepLen.get(length) + 1);
      }

      for (Edge edge : bestGraph.edges) {
        Integer length = Math.abs(edge.from - edge.to);
        if (!oracle_syst_total_count_byDepLen.containsKey(length))
          oracle_syst_total_count_byDepLen.put(length, 0);
        oracle_syst_total_count_byDepLen.put(length, oracle_syst_total_count_byDepLen.get(length) + 1);
      }
    }
  }

  private static String simplifyGoldCategory(Edge gold, EdgeType edgeType) {
    if (mode.equals(EvalMode.Simplified))
      return CCGCategoryUtilities.dropArgNoFeats(gold.cat) + ' ' + (edgeType.equals(EdgeType.PARG) ? CCGCategoryUtilities.dropArgNoFeats(gold.cat, gold.arg) : gold.arg);
    else if (mode.equals(EvalMode.SimplifiedDCL))
      return CCGCategoryUtilities.simplifyCCG(gold.cat) + ' ' + (edgeType.equals(EdgeType.PARG) ? CCGCategoryUtilities.simplifyCCG(gold.cat, gold.arg) : gold.arg);
    else
      return gold.cat + ' ' + (gold.arg == -1 ? "" : gold.arg);
  }

  private static void printScores() {
    // There's still a precision recall on the overall counts
    System.out.print(String.format("%-25s:\t%6.2f\t%6.2f\t%6.2f", mode,
        SimpleMath.Precision(correct, syst_total_count),
        SimpleMath.Precision(correct, gold_total_count),
        SimpleMath.HarmonicMean(correct, gold_total_count, syst_total_count)));
    if (arguments.oracle) {
      System.out.println(String.format("\t|\t%6.2f\t%6.2f\t%6.2f",
          SimpleMath.Precision(oracle_correct, oracle_syst_total_count),
          SimpleMath.Precision(oracle_correct, gold_total_count),
          SimpleMath.HarmonicMean(oracle_correct, gold_total_count, oracle_syst_total_count)));
    } else {
      System.out.println();
    }
  }

  private static void printVerbose() {
    if(arguments.verbose) {
      System.out.println("-------------------------------------------------------");
      ArrayList<ObjectDoublePair<String>> vals = new ArrayList<>();
      for(String cat : gold_edge_counts.keySet()) {
        vals.add(new ObjectDoublePair<>(cat,gold_edge_counts.get(cat)));
      }
      Collections.sort(vals);
      for(ObjectDoublePair<String> pair : vals) {
        if (compute_verbose_f1) {
          System.out.print(String.format("%-25s\t\t%7.0f\t%6.2f\t%6.2f\t%6.2f",
              pair.content(), pair.value(),
              SimpleMath.Precision(correct_edge_counts.get(pair.content()), syst_edge_counts.get(pair.content())),
              SimpleMath.Precision(correct_edge_counts.get(pair.content()), (int) pair.value()),
              SimpleMath.HarmonicMean(correct_edge_counts.get(pair.content()), (int) pair.value(), syst_edge_counts.get(pair.content()))));
        } else {
          System.out.print(String.format("%-25s\t\t%7.0f\t%6.2f",
              pair.content(), pair.value(),
              SimpleMath.Precision(correct_edge_counts.get(pair.content()), (int) pair.value())));
        }
        if (arguments.oracle) {
          if (compute_verbose_f1) {
            System.out.println(String.format("\t|\t%6.2f\t%6.2f\t%6.2f",
                compute_verbose_f1 ?  SimpleMath.Precision(oracle_correct_edge_counts.get(pair.content()), oracle_syst_edge_counts.get(pair.content())) : 0,
                                      SimpleMath.Precision(oracle_correct_edge_counts.get(pair.content()), (int) pair.value()),
                compute_verbose_f1 ?  SimpleMath.HarmonicMean(oracle_correct_edge_counts.get(pair.content()), (int) pair.value(), oracle_syst_edge_counts.get(pair.content())) : 0));
          } else {
            System.out.println(String.format("\t|\t%6.2f",
                SimpleMath.Precision(oracle_correct_edge_counts.get(pair.content()), (int) pair.value())));
          }
        } else {
          System.out.println();
        }
      }
    }
    if(arguments.verbose_length) {
      System.out.println("-------------------------------------------------------");
      int max = 0;
      for (Integer key : gold_total_count_byLen.keySet())
        max = Math.max(max, key);
      int correct_len, system_total, gold_total;
      int oracle_correct = 0, oracle_system_total = 0;
      for (int length = 1; length <= max; ++length) {
        correct_len = 0;
        system_total = 0;
        if (correct_byLen.containsKey(length)) {
          correct_len = correct_byLen.get(length);
          system_total = syst_total_count_byLen.get(length);
        }
        if (gold_total_count_byLen.containsKey(length))
          gold_total = gold_total_count_byLen.get(length);
        else
          continue;
        System.out.print(String.format("%-5d\t\t%6.2f\t%6.2f\t%6.2f", length,
            SimpleMath.Precision(correct_len, system_total),
            SimpleMath.Precision(correct_len, gold_total),
            SimpleMath.HarmonicMean(correct_len, system_total, gold_total)));
        if (arguments.oracle) {
          if (oracle_correct_byLen.containsKey(length)) {
            oracle_correct += oracle_correct_byLen.get(length);
            oracle_system_total += oracle_syst_total_count_byLen.get(length);
          }
          System.out.println(String.format("\t|\t%6.2f\t%6.2f\t%6.2f",
              SimpleMath.Precision(oracle_correct, oracle_system_total),
              SimpleMath.Precision(oracle_correct, gold_total),
              SimpleMath.HarmonicMean(oracle_correct, oracle_system_total, gold_total)));
        } else {
          System.out.println();
        }
      }
    }
    if(arguments.verbose_depLength) {
      System.out.println("-------------------------------------------------------");
      int max = 0;
      for (Integer key : gold_total_count_byDepLen.keySet())
        max = Math.max(max, key);
      int correct_len, system_total, gold_total;
      int oracle_correct = 0, oracle_system_total = 0;
      for (int length = 1; length <= max; ++length) {
        correct_len = 0;
        system_total = 0;
        if (correct_byDepLen.containsKey(length)) {
          correct_len = correct_byDepLen.get(length);
          system_total = syst_total_count_byDepLen.get(length);
        }
        if (gold_total_count_byDepLen.containsKey(length))
          gold_total = gold_total_count_byDepLen.get(length);
        else
          continue;
        System.out.print(String.format("%-5d\t\t%6.2f\t%6.2f\t%6.2f", length,
            SimpleMath.Precision(correct_len, system_total),
            SimpleMath.Precision(correct_len, gold_total),
            SimpleMath.HarmonicMean(correct_len, system_total, gold_total)));
        if (arguments.oracle) {
          if (oracle_correct_byDepLen.containsKey(length)) {
            oracle_correct += oracle_correct_byDepLen.get(length);
            oracle_system_total += oracle_syst_total_count_byDepLen.get(length);
          }
          System.out.println(String.format("\t|\t%6.2f\t%6.2f\t%6.2f",
              SimpleMath.Precision(oracle_correct, oracle_system_total),
              SimpleMath.Precision(oracle_correct, gold_total),
              SimpleMath.HarmonicMean(oracle_correct, oracle_system_total, gold_total)));
        } else {
          System.out.println();
        }
      }
    }
  }

  /**
   * Count the number of equivalent edges between the gold and system graphs.  Equivalent is defined by EvalMode
   * @param gold Gold graph
   * @param syst Predicted graph
   * @param mode Definition of equality
   * @param edgeType  If we are using C and C, PARG or CoNLL as gold
   * @return # of matching edges
   */
  static int score (Graph gold, Graph syst, EvalMode mode, EdgeType edgeType, boolean syst_has_feats) {
    return correct_edges(gold, syst, mode, edgeType, syst_has_feats).size();
  }

  static ArrayList<Edge> correct_edges(Graph gold, Graph syst, EvalMode mode, EdgeType edgeType, boolean syst_has_feats) {
    ArrayList<Edge> edges = new ArrayList<>();
    Edge gold_edge;
    for (Edge system_edge : syst.edges) {
      gold_edge = gold.contains(system_edge, mode, edgeType, syst_has_feats);
      if (gold_edge != null)
        edges.add(gold_edge);
    }
    return edges;
  }
}
