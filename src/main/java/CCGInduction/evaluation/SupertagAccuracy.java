package CCGInduction.evaluation;

import CCGInduction.ccg.CCGCategoryUtilities;
import CCGInduction.data.JSON.JSONFormat;
import CCGInduction.utils.ObjectDoublePair;
import CCGInduction.data.JSON.SynParObj;
import CCGInduction.utils.Math.SimpleMath;
import CCGInduction.utils.TextFile;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Class for evaluating SupertagAccuracy/Performance against either CCGbank AUTO
 * files or C&C parses in a JSON object
 */
final class SupertagAccuracy {
  // Verbose data-structures
  private static final HashMap<String,Integer> ST_gold_counts = new HashMap<>();
  private static final HashMap<String,Integer> ST_counts = new HashMap<>();
  private static final HashMap<String,Integer> ST_correct_counts = new HashMap<>();
  // Oracle counts
  private static final HashMap<String,Integer> ST_oracle_counts = new HashMap<>();
  private static final HashMap<String,Integer> ST_oracle_correct_counts = new HashMap<>();

  // Confusion Matrix
  private static final HashMap<String,HashMap<String,Integer>> Predicted_to_Gold = new HashMap<>();
  private static final HashMap<String,HashMap<String,Integer>> OraclePredicted_to_Gold = new HashMap<>();

  static final ArrayList<Integer> sentenceLengths = new ArrayList<>();
  public static void main(String[] args) throws Exception {
    Arguments arguments = new Arguments(args);
    if (arguments.invalid) {
      return;
    }

    // Determine if input is json or regular text file
    ArrayList<String[]> gold;
    System.out.println("Reading: " + arguments.gold_file);
    if (arguments.gold_file.toLowerCase().contains("json")) {
      gold = GoldJSONFileReader(arguments.gold_file);
    } else {
      gold = GoldFileReader(arguments.gold_file, arguments);
    }

    // Determine if input is json or regular text file
    ArrayList<ArrayList<String[]>> system;
    System.out.println("Reading: " + arguments.syst_file);
    if (arguments.syst_file.toLowerCase().contains("json")) {
      system = SystemJSONFileReader(arguments.syst_file, arguments);
    } else {
      system = SystemFileReader(arguments.syst_file);
    }

    // Prune down sentence length
    if (!sentenceLengths.isEmpty()) {
      ArrayList<String[]> newGold = new ArrayList<>();
      ArrayList<ArrayList<String[]>> newSyst = new ArrayList<>();
      for (int i = 0; i < sentenceLengths.size(); ++i) {
        if (sentenceLengths.get(i) <= arguments.maxLength) {
          newGold.add(gold.get(i));
          newSyst.add(system.get(i));
        }
      }
      gold = newGold;
      system = newSyst;
    }

    // Sanity check file sizes
    if (system.size() != gold.size()) {
      System.out.println("Files have different number of sentences: S:" + system.size() + "\t G:" + gold.size());
      System.out.println("Exiting");
      return;
    }

    // compute oracle and viterbi scores for each sentence
    String[] gold_ex, syst_ex;
    int correct = 0, oracle_correct = 0, total = 0;
    for (int i = 0; i < gold.size(); ++i) {
      gold_ex = gold.get(i);
      syst_ex = system.get(i).get(0);
      correct += matches(gold_ex, syst_ex);     // Compute Accuracy
      total += gold_ex.length;
      if(arguments.verbose || arguments.confusion) {   // Predictions for verbose F1
        addCounts(syst_ex, gold_ex, ST_correct_counts, arguments.confusion ? Predicted_to_Gold : null);
        addCounts(gold_ex, ST_gold_counts);
        addCounts(syst_ex, ST_counts);
      }

      if (arguments.oracle) {
        String[] bestSequence = null;                // Find best sentence
        int bestScore = 0;
        for (String[] systemPrediction : system.get(i)){
          int score = matches(gold_ex, systemPrediction);
          if (score > bestScore) {
            bestScore = score;
            bestSequence = systemPrediction;
          }
        }
        oracle_correct += bestScore;          // Compute Accuracy
        if (arguments.verbose || arguments.confusion) {     // Predictions for verbose F1
          addCounts(bestSequence, gold_ex, ST_oracle_correct_counts, arguments.confusion ? OraclePredicted_to_Gold : null);
          addCounts(bestSequence, ST_oracle_counts);
        }
      }
    }

    // Print Accuracy
    System.out.println(String.format("Sentences: %6d  Correct   Total  Accuracy", gold.size()));
    System.out.println(String.format("SuperTag Accuracy: %6d   %6d   %5.2f",
                                    correct, total, SimpleMath.Precision(correct,total)));
    if (arguments.oracle) {
      System.out.println(String.format("SuperTag Oracle  : %6d   %6d   %5.2f",
                                    oracle_correct, total, SimpleMath.Precision(oracle_correct, total)));
    }

    // Sort events
    ArrayList<ObjectDoublePair<String>> vals = new ArrayList<>();
    if (arguments.verbose || arguments.confusion) {
      vals.addAll(ST_gold_counts.keySet().stream().map(
          s -> new ObjectDoublePair<>(s, ST_gold_counts.get(s))).collect(Collectors.toList()));
      Collections.sort(vals);
    }
    // Verbose Analysis
    if (arguments.verbose) {
      System.out.print(String.format("\n%-25s    %5s            %8s", "Category", "Count", "Viterbi"));
      if (arguments.oracle) {
        System.out.println("                   Oracle");
      } else {
        System.out.println();
      }
      System.out.println("------------------------------------------------------------");
      for(ObjectDoublePair<String> pair : vals) {
          System.out.print(String.format("%-25s   %6d   %6.2f   %6.2f   %6.2f",
            pair.content(), (int)pair.value(),
            SimpleMath.Precision(ST_correct_counts.get(pair.content()),ST_counts.get(pair.content())),
            SimpleMath.Precision(ST_correct_counts.get(pair.content()),ST_gold_counts.get(pair.content())),
            SimpleMath.HarmonicMean(ST_correct_counts.get(pair.content()),ST_counts.get(pair.content()),ST_gold_counts.get(pair.content()))));
        if (arguments.oracle) {
          System.out.println(String.format("  |  %6.2f   %6.2f   %6.2f",
              SimpleMath.Precision(ST_oracle_correct_counts.get(pair.content()), ST_oracle_counts.get(pair.content())),
              SimpleMath.Precision(ST_oracle_correct_counts.get(pair.content()), ST_gold_counts.get(pair.content())),
              SimpleMath.HarmonicMean(ST_oracle_correct_counts.get(pair.content()), ST_oracle_counts.get(pair.content()), ST_gold_counts.get(pair.content()))));
        } else {
          System.out.println();
        }
      }
    }
    // Confusion Matrices
    if (arguments.confusion) {
      System.out.println("---------------------- Viterbi Confusion ---------------------------------");
      printConfusionMatrix(Predicted_to_Gold, vals);
      if (arguments.oracle) {
        System.out.println("---------------------- Oracle Confusion ---------------------------------");
        printConfusionMatrix(OraclePredicted_to_Gold, vals);
      }
    }
  }

  /**
   * Compute the overlap between two sequences of categories
   * @param gold   Gold output
   * @param system System's output
   * @return Number of matches
   * @throws Exception
   */
  private static int matches(String[] gold, String[] system) throws Exception {
    if (system.length == 0 || gold.length == 0) {
      return 0;
    } else if (system.length != gold.length) {
      // Try to remedy with `` ''
      system = removeQuotes(system);
      if (system.length != gold.length) {
        System.out.println(Arrays.toString(system));
        System.out.println(Arrays.toString(gold));
        throw new Exception("Lengths don't match: " + system.length + '\t' + gold.length);
      }
    }
    //System.out.println(Arrays.toString(gold));
    //System.out.println(Arrays.toString(system));
    int score = 0;
    for (int i = 0; i < gold.length; ++i) {
      if (!gold[i].equals("RQU")) {
        score += system[i].equals(gold[i]) ? 1 : 0;
      } else if (system[i].equals("\"") || !system[i].equals("''") && !system[i].equals("'")
          && !system[i].equals("``") && !system[i].equals("`")) {
        score += 1;
      }
    }
    return score;
  }

   /**
   * Reads a Gold AUTO file corresponding.  Converts AUTO parses to categories
   * and drops extra arguments.
   * @param filename Gold file
   * @return Gold supertags
   */
  private static ArrayList<String[]> GoldFileReader(String filename, Arguments arguments) {
    ArrayList<String[]> gold = new ArrayList<>();
    List<String> file = TextFile.Read(filename);
    for (String line : file) {
      if (line.charAt(0) ==  '(') {
        String[] cats = CCGCategoryUtilities.GoldAUTOtoCATS(line);
        if (arguments.removePunct){
          String[] tags = CCGCategoryUtilities.AUTOtoWords(line);
          ArrayList<String> newCats = new ArrayList<>();
          for (int i = 0; i < tags.length; ++i) {
            switch (tags[i]) {
              case "-RRB-":
              case "-RCB-":
              case "RRB":
              case "-LRB-":
              case "-LCB-":
              case "LRB":
              case ".":
              case ",":
              case ":":
              case ";":
              case "!":
              case "?":
              case "#":
              case "$":
              case "US$":
              case "C$":
              case "HK$":
              case "-":
              case "--":
              case "...":
                break;
              default:
                switch (cats[i]) {
                  case "-RRB-":
                  case "-RCB-":
                  case "RRB":
                  case "-LRB-":
                  case "-LCB-":
                  case "LRB":
                  case ".":
                  case ",":
                  case ":":
                  case ";":
                  case "!":
                  case "?":
                  case "-":
                  case "#":
                  case "$":
                  case "--":
                  case "...":
                    break;
                  default:
                    newCats.add(cats[i]);
                }
            }
          }
          cats = newCats.toArray(new String[newCats.size()]);
        }
        gold.add(cats);
      } else if(line.contains("NUMPARSE=0")) {
        gold.add(new String[0]);
      }
    }
    return gold;
  }

  /**
   * Reads a JSON file corresponding to gold input.  Converts AUTO parses to categories
   * and drops extra arguments.
   * @param filename Gold file
   * @return Gold Supertags
   */
  private static ArrayList<String[]> GoldJSONFileReader(String filename) {
    ArrayList<JSONFormat> goldJSON = JSONFormat.readJSON(filename);
    ArrayList<String[]> gold = new ArrayList<>();
    for (JSONFormat json : goldJSON) {
      if (json.synPars == null) {
        gold.add(new String[0]);
      } else {
        gold.add(CCGCategoryUtilities.GoldAUTOtoCATS(json.synPars[0].synPar));
      }
    }
    return gold;
  }

  /**
   * Reads an AUTO file corresponding to system output.  Converts AUTO parses to categories
   * but does not change the categories themselves.  Can handle multiple parses for Oracle analysis
   * @param filename System file
   * @return System supertags (top K per sentence)
   */
  private static ArrayList<ArrayList<String[]>> SystemFileReader(String filename) {
    // Read system file
    ArrayList<ArrayList<String[]>> system = new ArrayList<>();
    List<String> file = TextFile.Read(filename);
    for (String line : file) {
      if (!line.trim().isEmpty()) {
        if (line.charAt(0) == 'S' || line.charAt(0) == 'F' || line.charAt(0) == 'I') {
          system.add(new ArrayList<>());
          if (line.charAt(0) == 'F') {
            system.get(system.size()-1).add(new String[0]);
          }
          if (line.contains("-Infinity"))
            system.get(system.size()-1).add(new String[0]);
        } else {
          system.get(system.size()-1).add(CCGCategoryUtilities.GoldAUTOtoCATS(line));
        }
      }
    }
    return system;
  }

  /**
   * Reads a JSON file corresponding to system output.  Converts AUTO parses to categories
   * but does not change the categories themselves.   Can handle multiple parses for Oracle analysis.
   * @param filename System file
   * @return System supertags (top K per sentence)
   */
  private static ArrayList<ArrayList<String[]>> SystemJSONFileReader(String filename, Arguments arguments) {
    // Read system file
    ArrayList<JSONFormat> systemJSON = JSONFormat.readJSON(filename);
    ArrayList<ArrayList<String[]>> system = new ArrayList<>();
    for (JSONFormat json : systemJSON) {
      sentenceLengths.add(json.length_noP());
      system.add(new ArrayList<>());
      if (json.synPars == null) {
        system.get(system.size()-1).add(new String[0]);
      } else {
        for(SynParObj parObj : json.synPars) {
          String[] cats = CCGCategoryUtilities.AUTOtoCATS(parObj.synPar);
          ArrayList<String> newCats = new ArrayList<>();
          if (arguments.removePunct) {
            for (int i = 0; i < cats.length; ++i) {
              if(!json.words[i].upos.equals("."))
                newCats.add(cats[i]);
            }
            cats = newCats.toArray(new String[newCats.size()]);
          }
          system.get(system.size()-1).add(cats);
        }
      }
    }
    return system;
  }


  /**
   * Adds a count for every category that matches in the gold and example sequences
   * @param example  Predicted SuperTag sequence
   * @param gold     Gold SuperTag sequence
   * @param counts   Datastructure for storing counts
   */
  private static void addCounts(String[] example, String[] gold, HashMap<String, Integer> counts,
                                HashMap<String,HashMap<String,Integer>> confusion) {
    if (example == null || example.length == 0 || gold.length == 0) {
      return;
    }
    for (int i = 0; i < gold.length; ++i) {
      if(gold[i].equals(example[i])) {
        if(!counts.containsKey(gold[i])) {
          counts.put(gold[i], 1);
        } else {
          counts.put(gold[i], counts.get(gold[i]) + 1);
        }
      }
      if (confusion != null) {
        if (!confusion.containsKey(gold[i]))
          confusion.put(gold[i], new HashMap<>());
        if (!confusion.get(gold[i]).containsKey(example[i]))
          confusion.get(gold[i]).put(example[i], 1);
        else
          confusion.get(gold[i]).put(example[i], 1 + confusion.get(gold[i]).get(example[i]));
      }
    }
  }

  /**
   * Adds a count for every supertag prediction
   * @param supertags  Tag sequence
   * @param counts     Datastructure for storing counts
   */
  private static void addCounts(String[] supertags, HashMap<String, Integer> counts) {
    if (supertags == null || supertags.length == 0)
      return;
    for (String tag : supertags) {
      if (!counts.containsKey(tag))
        counts.put(tag,1);
      else
        counts.put(tag, counts.get(tag)+1);
    }
  }

  /**
   * Removes all quotes from super-tag sequence
   * @param original Array with quotes
   * @return Quoteless array
   */
  private static String[] removeQuotes(String[] original) {
    ArrayList<String> simplified = new ArrayList<>();
    Collections.addAll(simplified, original);
    while (simplified.contains("``"))
      simplified.remove("``");
    while (simplified.contains("''"))
      simplified.remove("''");

    while (simplified.contains("'") || simplified.contains("`")) {
      simplified.remove("`");
      simplified.remove("'");
    }
    return simplified.toArray(new String[simplified.size()]);
  }

  private static void printConfusionMatrix(HashMap<String, HashMap<String, Integer>> Matrix,
                                           ArrayList<ObjectDoublePair<String>> vals) {
    ArrayList<ObjectDoublePair<String>> confused = new ArrayList<>();
    boolean transitioned = false;
    for(ObjectDoublePair<String> pair : vals) {
      if (!transitioned && pair.content().length() < 16)
        System.out.print(String.format("%-15s \t%6d \t", pair.content(), (int) pair.value()));
      else {
        transitioned = true;
        System.out.print(String.format("%-25s \t%6d \t", pair.content(), (int) pair.value()));
      }
      confused.clear();
      if (!Matrix.containsKey(pair.content())) {
        System.err.println("Missing: " + pair.content());
        continue;
      }
      double sum = Matrix.get(pair.content()).keySet().stream().mapToInt(K -> Matrix.get(pair.content()).get(K)).sum();
      confused.addAll(Matrix.get(pair.content()).keySet().stream().map(
          val -> new ObjectDoublePair<>(val, Matrix.get(pair.content()).get(val) / sum)).collect(Collectors.toList()));
      Collections.sort(confused);
      int count = 0;
      for (ObjectDoublePair<String> option : confused) {
        System.out.print(String.format("%-15s\t %-7.5f\t ", option.content(), option.value()));
        count += 1;
        if (count % 4 == 0)
          if (!transitioned)
            System.out.print(String.format("\n%-15s \t%6s \t", "",""));
          else
          System.out.print(String.format("\n%-25s \t%6s \t", "",""));
      }
      System.out.println();
    }
  }
}
