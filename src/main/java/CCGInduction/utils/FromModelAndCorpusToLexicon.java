package CCGInduction.utils;

import CCGInduction.ccg.InducedCAT;
import CCGInduction.data.JSON.JSONFormat;
import CCGInduction.data.JSON.WordObj;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * Created by bisk1 on 1/12/15.
 */
public class FromModelAndCorpusToLexicon {

  static final HashMap<String,HashMap<String,Double>> conditionalProbabilities = new HashMap<>();
  static final HashMap<String,Integer> tokenCounts = new HashMap<>();
  static final HashMap<String,String> clusterAssignments = new HashMap<>();
  static final HashMap<String,HashMap<String,Double>> accumulatedCounts = new HashMap<>();
  public static void main(String[] args) throws Exception {

    // Read the Model
    String model = args[0];
    BufferedReader reader = TextFile.Reader(model);
    String line;
    String[] split;
    while((line = reader.readLine()) != null) {
      split = Logger.whitespace_pattern.split(line);
      if (split.length == 6) {
        String cat = split[1].substring(2);
        String word = split[4].substring(2).toLowerCase();
        Double prob = Double.valueOf(split[5]);
        if (!conditionalProbabilities.containsKey(word))
          conditionalProbabilities.put(word, new HashMap<>());
        conditionalProbabilities.get(word).put(cat,prob);
      }
    }

    // Read Corpus (type classification and token counts)
    String corpus = args[1];
    reader = TextFile.Reader(corpus);
    while((line = reader.readLine()) != null) {
      JSONFormat json = JSONFormat.deSerialize(line);
      for (WordObj wordObj : json.words) {
        if (wordObj.word == null)
          System.err.println(line);
        String word = wordObj.word.toLowerCase();
        String cluster = wordObj.cluster;
        if (!tokenCounts.containsKey(word)) {
          tokenCounts.put(word, 0);
          clusterAssignments.put(word, cluster);
        }
        tokenCounts.put(word,tokenCounts.get(word)+1);
      }
    }

    // Accumulate the counts
    for (String word : tokenCounts.keySet()) {
      String cluster = clusterAssignments.get(word);
      if (!accumulatedCounts.containsKey(cluster))
        accumulatedCounts.put(cluster,new HashMap<>());
      HashMap<String, Double> perCluster = accumulatedCounts.get(cluster);
      if (conditionalProbabilities.get(word) != null) {  // Discard word that were unk ... ?
        for (String category : conditionalProbabilities.get(word).keySet()) {
          if (!perCluster.containsKey(category))
            perCluster.put(category, 0.0);
          perCluster.put(category, perCluster.get(category) + tokenCounts.get(word) * conditionalProbabilities.get(word).get(category));
        }
      }
    }
    // Normalize
    for (String cluster : accumulatedCounts.keySet()) {
      Double total = 0.0;
      for (String category : accumulatedCounts.get(cluster).keySet())
        total += accumulatedCounts.get(cluster).get(category);
      for (String category : accumulatedCounts.get(cluster).keySet())
        accumulatedCounts.get(cluster).put(category, accumulatedCounts.get(cluster).get(category) / total);
    }

    InducedCAT S = new InducedCAT(InducedCAT.S);
    InducedCAT N = new InducedCAT(InducedCAT.N);
    InducedCAT SbN = S.backward(N);
    InducedCAT SfN = S.forward(N);
    int conflict = 0;
    int total = 0;
    // Write the lexicon
    BufferedWriter writer = TextFile.Writer("NewLexicon.txt.gz");
    for (String cluster : accumulatedCounts.keySet()) {
      ArrayList<ObjectDoublePair<String>> vals = new ArrayList<>();
      for (String category : accumulatedCounts.get(cluster).keySet())
        vals.add(new ObjectDoublePair<>(category, accumulatedCounts.get(cluster).get(category)));
      Collections.sort(vals);
      double noun = 0.0, verb = 0.0;
      for (ObjectDoublePair<String> pair : vals) {
        if (pair.value() > 0.001) {
          writer.write(String.format("1  %-10f   %-20s    %-5s\n", pair.value(), pair.content(), cluster));

          InducedCAT cat = InducedCAT.valueOf(pair.content());
          if (cat.equals(N))
            noun += pair.value();
          if (cat.equals(SbN) || cat.equals(SfN)
              || SbN.equals(cat.Res) || SfN.equals(cat.Res)
              || (cat.Res != null && cat.Res.Res != null && (SbN.equals(cat.Res.Res) || SfN.equals(cat.Res.Res))))
            verb += pair.value();
        }
      }
      //System.out.println(String.format("%-5s  noun:%7.4f    verb:%7.4f", cluster, 100*noun, 100*verb));
      System.out.println(String.format("%-5s  %s  %s", cluster, noun >= 0.001 ? "noun" : "", verb >= 0.001 ? "verb top" : ""));
      total += 1;
    }
    writer.close();
    System.out.println(conflict + " / " + total);
  }


}
