package CCGInduction.evaluation;

import CCGInduction.data.JSON.JSONFormat;
import CCGInduction.data.JSON.PARGDep;
import CCGInduction.data.JSON.SynParObj;
import CCGInduction.utils.TextFile;

import java.util.ArrayList;
import java.util.List;

public class PARGDependencies extends DependencyEvaluation {

  /**
   * Reads in gold and system files and performs the specified dependency evaluation
   * @param args Command line options @see Arguments
   */
  public static void main(String[] args) {
    Arguments arguments = new Arguments(args);
    if (arguments.invalid) {
      return;
    }
    EdgeType edgeType = EdgeType.PARG;
    ArrayList<Graph> gold;
    if (arguments.gold_file.toLowerCase().contains("json")) {
      gold = JSONFileReader(arguments.gold_file);
      edgeType = EdgeType.CANDC;
    } else {
      gold = PARGFileReader(arguments.gold_file);
    }
    ArrayList<ArrayList<Graph>> syst;
    if (arguments.syst_file.toLowerCase().contains("json")) {
      syst = systJSONFileReader(arguments.syst_file);
    } else {
      syst = systPARGFileReader(arguments.syst_file);   // Maintained for legacy
    }

    if (gold.size() != syst.size()) {
      System.out.println("File sizes don't match:\tG:" + gold.size() + "\tS:" + syst.size());
      return;
    }
    double count = 0;
    for (int i = 0; i < syst.size(); ++i)
      count += syst.get(i).get(0).size(null) == 0 && !gold.get(i).edges.isEmpty() ? 0 : 1;
    System.out.println("Coverage: " + 100*count/gold.size());

    System.out.println(String.format("Gold: %-30s", arguments.gold_file));
    System.out.println(String.format("Syst: %-30s", arguments.syst_file));
    System.out.println("-------------------------------------------------------");
    switch(arguments.mode) {
      case Undirected:
        evaluate(gold, syst, EvalMode.Undirected, edgeType, arguments);
        break;
      case Directed:
        evaluate(gold, syst, EvalMode.Directed, edgeType, arguments);
        break;
      case Argument:
        evaluate(gold, syst, EvalMode.Argument, edgeType, arguments);
        break;
      case NoFeatures:
        evaluate(gold, syst, EvalMode.NoFeatures, edgeType, arguments);
        break;
      case Simplified:
        evaluate(gold, syst, EvalMode.Simplified, edgeType, arguments);
        break;
      case SimplifiedDCL:
        evaluate(gold, syst, EvalMode.SimplifiedDCL, edgeType, arguments);
        break;
      case Labeled:
        evaluate(gold, syst, EvalMode.Labeled, edgeType, arguments);
        break;
      case All:
        // This would otherwise print wayyyy too much
        arguments.verbose = false;
        evaluate(gold, syst, EvalMode.Undirected, edgeType, arguments);
        evaluate(gold, syst, EvalMode.Directed, edgeType, arguments);
        evaluate(gold, syst, EvalMode.Argument, edgeType, arguments);
        evaluate(gold, syst, EvalMode.NoFeatures, edgeType, arguments);
        evaluate(gold, syst, EvalMode.SimplifiedDCL, edgeType, arguments);
        evaluate(gold, syst, EvalMode.Simplified, edgeType, arguments);
        evaluate(gold, syst, EvalMode.Labeled, edgeType, arguments);
        break;
      default:
        System.err.println("Please choose an evaluation mode: " +
            "[Undirected, Directed, Argument, NoFeatures, Simplified, Labeled, All]");
        break;
    }
  }

  private static ArrayList<Graph> JSONFileReader(String filename) {
    ArrayList<JSONFormat> goldJSON = JSONFormat.readJSON(filename);
    ArrayList<Graph> graphs = new ArrayList<>();
    Graph current;
    PARGDep[] deps;
    for (JSONFormat json : goldJSON) {
      if (json.synPars == null) {
        graphs.add(new Graph(json.length_noP()));
      } else {
        deps = json.synPars[0].depParse;
        current = new Graph(json.length_noP());
        // Skip supertags at the end
        for (int i = 0; i < deps.length-1; ++i) {
          current.addEdge(deps[i].dependent, deps[i].head, deps[i].category, deps[i].slot, EdgeType.CANDC);
        }
        graphs.add(current);
      }
    }
    return graphs;
  }

  private static ArrayList<ArrayList<Graph>> systJSONFileReader(String filename) {
    ArrayList<JSONFormat> goldJSON = JSONFormat.readJSON(filename);
    ArrayList<ArrayList<Graph>> graphs = new ArrayList<>();
    Graph current;
    PARGDep[] deps;
    for (JSONFormat json : goldJSON) {
      ArrayList<Graph> graphForCurrentSentence = new ArrayList<>();
      if (json.synPars != null) {
        for (SynParObj parseObject : json.synPars) {
          deps = parseObject.depParse;
          current = new Graph(json.length_noP());
          // Skip open and close <s> </s>
          for (PARGDep dep : deps) {
            current.addEdge(dep.dependent, dep.head, dep.category, dep.slot, EdgeType.PARG);
          }
          graphForCurrentSentence.add(current);
        }
      } else {
        graphForCurrentSentence.add(new Graph(json.length_noP()));
      }
      graphs.add(graphForCurrentSentence);
    }
    return graphs;
  }

  private static ArrayList<Graph> PARGFileReader(String filename) {
    ArrayList<Graph> graphs = new ArrayList<>();
    Graph current = new Graph(-1);
    List<String> file = TextFile.Read(filename);
    for (String line : file) {
      // Start of graph
      if (line.charAt(0) == '<' && line.charAt(1) == 's') {
        current = new Graph(-1);
      }
      // end of graph
      else if (line.charAt(0) == '<' && line.charAt(1) == '\\') {
        graphs.add(current);
      }
      // Read in graph edge
      else {
        current.addEdge(line, EdgeType.PARG);
      }
    }
    return graphs;
  }

  /**
   * Reads in a system's PARG file and converts it to a set of Graphs.  This is deprecated and can
   * only handle Top-1 (viterbi) evaluation.
   * @param filename  File to read
   * @return Set of graphs to score
   */
  @Deprecated
  private static ArrayList<ArrayList<Graph>> systPARGFileReader(String filename) {
    ArrayList<ArrayList<Graph>> graphs = new ArrayList<>();
    Graph current = new Graph(-1);
    List<String> file = TextFile.Read(filename);
    for (String line : file) {
      // Start of graph
      if (line.charAt(0) == '<' && line.charAt(1) == 's') {
        current = new Graph(-1);
      }
      // end of graph
      else if (line.charAt(0) == '<' && line.charAt(1) == '\\') {
        graphs.add(new ArrayList<>());
        graphs.get(graphs.size()-1).add(current);
      }
      // Read in graph edge
      else {
        current.addEdge(line, EdgeType.PARG);
      }
    }
    return graphs;
  }
}
