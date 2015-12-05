package CCGInduction.evaluation;

import CCGInduction.data.JSON.CoNLLDep;
import CCGInduction.data.JSON.JSONFormat;
import CCGInduction.data.JSON.SynParObj;
import CCGInduction.data.JSON.WordObj;
import CCGInduction.utils.Logger;
import CCGInduction.utils.TextFile;

import java.io.BufferedReader;
import java.util.ArrayList;

/**
 * Class for evaluating performance on CoNLL style dependencies.
 * Assumes output in JSON file from our system
 */
public class CoNLLDependencies extends DependencyEvaluation {

  public static void main(String[] args) throws Exception {
    CoNLLDependencies eval = new CoNLLDependencies();
    Arguments arguments = new Arguments(args);

    ArrayList<Graph> gold = eval.readGold(arguments.gold_file, arguments.maxLength);
    ArrayList<ArrayList<Graph>> syst = eval.readSystem(arguments.syst_file, arguments.maxLength);

    switch(arguments.mode) {
      case Undirected:
        evaluate(gold, syst, EvalMode.Undirected, EdgeType.CONLL, arguments);
        break;
      case Directed:
        evaluate(gold, syst, EvalMode.Directed, EdgeType.CONLL, arguments);
        break;
      case All:
        arguments.verbose = false;
        arguments.verbose_length = false;
        evaluate(gold, syst, EvalMode.Undirected, EdgeType.CONLL, arguments);
        evaluate(gold, syst, EvalMode.Directed, EdgeType.CONLL, arguments);
        break;
      default:
        System.err.println("Please choose an evaluation mode: " +
            "[Undirected, Directed, All]");
        break;
    }
  }

  public ArrayList<Graph> readGold(String filename, int maxLength) throws Exception {
    String line;
    int edges = 0;
    ArrayList<Graph> graphs = new ArrayList<>();
    Graph current = new Graph(-1);
    BufferedReader file = TextFile.Reader(filename);
    while ((line = file.readLine()) != null) {
      // New line --> end of a graph, start of a new one
      if (line.trim().isEmpty()){
        if (current.size(null) <= maxLength) {
          graphs.add(current);
          edges += current.size(null);
        }
        current = new Graph(-1);
      } else {
        String[] split = Logger.whitespace_pattern.split(line);
        // Ignore punctuation edges
        if (!split[5].equals(".")) {
          current.addEdge(line, EdgeType.CONLL);
        }
      }
    }
    System.out.println("Gold edges "  + edges + "\t sents " + graphs.size());
    return graphs;
  }

  public ArrayList<ArrayList<Graph>> readSystem(String filename, int maxLength) {
    ArrayList<JSONFormat> systJSON = JSONFormat.readJSON(filename);
    ArrayList<ArrayList<Graph>> graphs = new ArrayList<>();
    Graph current;
    CoNLLDep[] deps;
    int edges = 0;
    boolean first;
    for (JSONFormat json : systJSON) {
      first = true;
      int count = 0;
      for (WordObj wordObj : json.words) {
        if (!wordObj.upos.equals(".")) {
          count += 1;
        }
      }
      if (count <= maxLength) {
        ArrayList<Graph> graphsForCurrentSentence = new ArrayList<>();
        if (json.synPars != null) {
          for (SynParObj parseObject : json.synPars) {
            current = new Graph(json.length_noP());
            deps = parseObject.conllParse;
            for (CoNLLDep dep : deps) {
              // Ignore punctuation edges
              if (!json.words[dep.index-1].upos.equals(".")) {
                if (first)
                  edges += 1;
                current.addEdge(dep.index, dep.head, dep.label, -1, EdgeType.CONLL);
              }
            }
            graphsForCurrentSentence.add(current);
            first = false;
          }
        } else {
          graphsForCurrentSentence.add(new Graph(json.length_noP()));
        }
        graphs.add(graphsForCurrentSentence);
      }
    }
    System.out.println("Syst edges "  + edges + "\t sents " + graphs.size());
    return graphs;
  }
}
