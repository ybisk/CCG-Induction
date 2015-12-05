package CCGInduction.evaluation;

import CCGInduction.utils.ObjectDoublePair;
import CCGInduction.data.JSON.JSONFormat;
import CCGInduction.data.JSON.WordObj;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.stream.Collectors;

public class TagEvaluator {

  /**
   * Reads in a tagged json file and evaluates the clustering
   * Reference:  github.com/christos-c/bmmm
   * @param args Command line options @see Arguments
   */
  public static void main(String[] args) {
    Arguments arguments = new Arguments(args);
    ArrayList<JSONFormat> JSON = JSONFormat.readJSON(arguments.syst_file);
    evaluate(JSON, TagType.UPOS, arguments.removePunct);
    evaluate(JSON, TagType.POS, arguments.removePunct);
    evaluate(JSON, TagType.CPOS, arguments.removePunct);
  }

  /**
   * Computes a Many-to-1 score and a VM score
   * @param jsons Input Sentences
   * @param type  Type of tag to evaluate
   * @param ignorePunct Whether punctuation marks (upos = .) should be ignored
   */
  private static void evaluate(ArrayList<JSONFormat> jsons, TagType type, boolean ignorePunct) {
    HashMap<String,HashMap<String,Integer>> clusterTagCounts = new HashMap<>();
    HashMap<String,Integer> clusterTotals = new HashMap<>();
    HashMap<String,Integer> tagTotals = new HashMap<>();
    Integer counts = 0;
    for (JSONFormat json : jsons) {
      for (WordObj word : json.words) {
        if (ignorePunct && word.upos != null && word.upos.equals("."))
          continue;
        switch (type) {
          case UPOS:
            if (word.upos == null) {
              System.out.println(String.format("%-5s M-1: N/A\tVM: N/A", type));
              return;
            }
            else {
              ++counts;
              updateCount(clusterTagCounts, clusterTotals, tagTotals, word.cluster, word.upos);
            }
            break;
          case CPOS:
            if (word.cpos == null) {
              System.out.println(String.format("%-5s M-1: N/A\tVM: N/A", type));
              return;
            } else {
              ++counts;
              updateCount(clusterTagCounts, clusterTotals, tagTotals,  word.cluster, word.cpos);
            }
            break;
          case POS:
            if (word.pos == null) {
              System.out.println(String.format("%-5s M-1: N/A\tVM: N/A", type));
              return;
            } else {
              ++counts;
              updateCount(clusterTagCounts, clusterTotals, tagTotals, word.cluster, word.pos);
            }
            break;
        }
      }
    }
    // Compute Many-to-One
    double M_1 = 0.0;
    ArrayList<ObjectDoublePair<String>> pairs = new ArrayList<>();
    for (String cluster : clusterTagCounts.keySet()) {
      pairs.clear();
      pairs.addAll(clusterTagCounts.get(cluster).keySet().stream().map(
          tag -> new ObjectDoublePair<>(tag, clusterTagCounts.get(cluster).get(tag))).collect(Collectors.toList()));
      Collections.sort(pairs);
      M_1 += pairs.get(0).value();
    }

    // Compute VM
    double clusterEntropy = entropy(clusterTotals, counts);
    double tagEntropy = entropy(tagTotals, counts);
    double mutualInformation = mutualInformation(clusterTotals, tagTotals, clusterTagCounts, counts);
    // H(CL | T):  Conditional cluster entropy   (and tag | CL)
    double clusterGivenTag = clusterEntropy - mutualInformation;
    double tagGivenCluster = tagEntropy - mutualInformation;
    double c = 1 - (clusterGivenTag/clusterEntropy);
    double h = 1 - (tagGivenCluster/tagEntropy);

    System.out.println(String.format("%-5s M-1: %-7.2f\tVM: %-7.2f", type, 100.0 * M_1 / counts, 100.0*2*h*c/(h+c)));
  }

  private static void updateCount(HashMap<String,HashMap<String,Integer>> clusterTagCount,
                                  HashMap<String,Integer> clusterCounts,
                                  HashMap<String,Integer> tagCounts,
                                  String cluster, String tag) {
    // Votes
    if (!clusterTagCount.containsKey(cluster))
      clusterTagCount.put(cluster, new HashMap<>());
    if (!clusterTagCount.get(cluster).containsKey(tag))
      clusterTagCount.get(cluster).put(tag, 1);
    else
      clusterTagCount.get(cluster).put(tag, clusterTagCount.get(cluster).get(tag) + 1);
    // Cluster Total
    if (!clusterCounts.containsKey(cluster))
      clusterCounts.put(cluster, 1);
    else
      clusterCounts.put(cluster, clusterCounts.get(cluster) + 1);
    // Tag Total
    if (!tagCounts.containsKey(tag))
      tagCounts.put(tag, 1);
    else
      tagCounts.put(tag, tagCounts.get(tag) + 1);
  }

  private static double entropy(HashMap<String, Integer> counts, int total) {
    double entropy = 0;
    double p;
    for (String ID : counts.keySet()) {
      p = 1.0*counts.get(ID)/total;
      if (p != 0.0)
        entropy -= p*Math.log(p)/Math.log(2);
    }
    return entropy;
  }

  private static double mutualInformation(HashMap<String,Integer> clusters,
                                          HashMap<String,Integer> tags,
                                          HashMap<String,HashMap<String,Integer>> votes,
                                          double total) {
    double MI = 0.0;
    double cProb;
    double tProb;
    double coProb;
    for (String cluster : clusters.keySet()) {
      cProb = 1.0*clusters.get(cluster)/total;
      for (String tag : tags.keySet()) {
        tProb = 1.0*tags.get(tag)/total;
        coProb = votes.get(cluster).containsKey(tag) ? 1.0*votes.get(cluster).get(tag)/total : 0.0;
        if (coProb != 0.0) {
          MI += coProb*Math.log(coProb/(tProb*cProb)) / Math.log(2);
        }
      }
    }
    return MI;
  }
}
enum TagType {
  /** Universal Tagset (WordObj.upos) */
  UPOS,
  /** Fine grained (typically PTB) Tagset (WordObj.pos) */
  POS,
  /** Coarse (if applicable) Tagset (WordObj.cpos) */
  CPOS
}
