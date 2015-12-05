package CCGInduction.experiments;

import CCGInduction.Configuration;
import CCGInduction.grammar.Grammar;
import CCGInduction.parser.*;
import CCGInduction.ccg.InducedCAT;
import CCGInduction.data.POS;
import CCGInduction.data.Sentence;
import CCGInduction.data.Sentences;
import CCGInduction.data.Tagset;
import CCGInduction.models.Model;
import CCGInduction.utils.IntPair;
import CCGInduction.utils.Logger;
import CCGInduction.utils.TextFile;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

/**
 * All experiments must extend the abstract class Experiment by defining the
 * meaning of actions.
 * 
 * @author bisk1
 * @param <G> 
 */
public class Experiment<G extends Grammar> {
  protected G grammar;
  public Sentences training_sentences;

  final ArrayList<ArrayList<String>> ADDED_WORDS = new ArrayList<>();
  private int test_iterations = 0;
  final ArrayList<TestCharts> TESTINGCHARTS = new ArrayList<>();

  /**
   * Configuration file
   */
  static Configuration config;
  /**
   * Model that experiment uses
   */
  Model<G> model;

  // Thread management
  protected static final ArrayList<Exception> exceptions = new ArrayList<>();
  protected static ExecutorService executor;

  /**
   * Create an experiment with a configuration file
   * @param config Configuration
   */
  Experiment(Configuration config) {
    Experiment.config = config;
  }

  /**
   * Implementation of parser/model actions
   * 
   * @param action Action for experiment to carry out
   * @throws Exception
   */
  public void perform(Action action) throws Exception {
    switch(action) {
      case readTrainingFiles:
        // -------------------------- Create Tagset ------------------------ //
        if (Configuration.TAGSET != null) {
          if (new File(Configuration.TAGSET).exists())   // FIXME Hack
            Tagset.readTagMapping(Configuration.TAGSET);
        }
        Tagset.print();
        training_sentences = new Sentences(grammar, Configuration.shortestSentence,
            Configuration.longestSentence, Configuration.trainFile);
        kwords(training_sentences);
        break;
      case Test:
        Test("Test", Action.Test);
        break;
      case Save:
        model.writeToDisk();
        break;
      case PrintModel:
        model.print("Print");
        break;
      case Load:
        String modelFile = Configuration.loadModelFile;
        if ((modelFile != null && !new File(modelFile).exists())  || modelFile == null) {
          System.err.println("Model file does not exist. Exiting...");
          System.exit(-1);
        }

        System.out.println("Loading model " + modelFile);
        ObjectInputStream ios = new ObjectInputStream(
            new BufferedInputStream(new GZIPInputStream(new FileInputStream(modelFile))));
        model = (Model<G>) ios.readObject();
        grammar = model.grammar;
        Tagset.print();
        InducedCAT.createAtomics();
        kwords(training_sentences);
        ios.close();
        model.print("loaded");
        model.grammar.print("loaded");
        break;
      default:
        throw new Exception("Invalid: " + action);
    }
  }

  /**
   * Get grammar reference
   * 
   * @return Grammar
   */
  public G grammar() { return grammar; }


  private void addWordsToKnown(HashMap<Long, Integer> frequency,
                               double freq_threshold, String type, Writer file) throws IOException {
    file.write("\n--------\t" + type + "\t----------\n");
    ArrayList<IntPair> valPairs = new ArrayList<>();
    for (long key : frequency.keySet()) {
      valPairs.add(new IntPair(frequency.get(key), key));
    }
    Collections.sort(valPairs);
    ADDED_WORDS.add(new ArrayList<>());
    if (Configuration.threshold > 0 && freq_threshold < 1) {
      double accumulatedMass = 0.0;
      long total = 0;
      for (long k : frequency.keySet()) {
        total += frequency.get(k);
      }
      for (IntPair ip : valPairs) {
        if (grammar.learnedWords.containsKey(ip.second())) {
          // We're already capturing some large chunk o' da mass
          accumulatedMass += (double) frequency.get(ip.second()) / total;
        }
      }

      for (IntPair ip : valPairs) {
        if (!grammar.learnedWords.containsKey(ip.second())) {
          if (accumulatedMass < freq_threshold) {
            grammar.learnedWords.put(ip.second(), true);
            accumulatedMass += (double) frequency.get(ip.second()) / total;
            file.write(String.format("Adding %5s: %20s %5d %1.5f\n", type,
                grammar.Words.get(ip.second()), ip.first(), accumulatedMass));
            ADDED_WORDS.get(ADDED_WORDS.size() - 1).add(grammar.Words.get(ip.second()));
          } else {
            return;
          }
        }
      }
    } else if (freq_threshold >= 1) {
      int K = (int) freq_threshold;
      for (IntPair current : valPairs) {
        if (!grammar.learnedWords.containsKey(current.second())) {
          if (current.first() >= K) {
            grammar.learnedWords.put(current.second(), true);
            file.write(String.format("%4d %-14s \n", current.first(), grammar.Words.get(current.second())));
            ADDED_WORDS.get(ADDED_WORDS.size() - 1).add(grammar.Words.get(current.second()));
          }
        }
      }
    }
  }

  private int vocab_count = 1;

  public void kwords(Sentences docs) throws Exception {
    // E.g. Testing, not starting a leapfrog training
    if (docs == null || !grammar.learnedWords.isEmpty()) {
      return;
    }
    Logger.logln("Computing word frequencies");
    // Skip if we're not lexicalizing
    if (Configuration.lexFreq == 0 && Configuration.nounFreq == 0
        && Configuration.verbFreq == 0 && Configuration.funcFreq == 0) {
      return;
    }
    Writer vocab_writer = TextFile.Writer(Configuration.Folder + "/Vocab." + vocab_count + ".txt.gz");
    HashMap<Long, Integer> frequency = new HashMap<>();
    HashMap<Long, Integer> nounfrequency = new HashMap<>();
    HashMap<Long, Integer> verbfrequency = new HashMap<>();

    // Add most common lexical items
    Sentence ia;
    while ((ia = docs.next()) != null) {
      for (int k = 0; k < ia.length(); k++) {
        POS t = ia.get(k).tag();
        long w = ia.get(k).word();
        if (Configuration.lexFreq != 0) {
          addWordToHash(frequency, w);
        }
        if (Configuration.nounFreq != 0 && Tagset.noun(t)) {
          addWordToHash(nounfrequency, w);
        }
        if (Configuration.verbFreq != 0 && Tagset.verb(t)) {
          addWordToHash(verbfrequency, w);
        }
      }
    }
    addWordsToKnown(frequency,     Configuration.lexFreq,  "word", vocab_writer);
    addWordsToKnown(nounfrequency, Configuration.nounFreq, "noun", vocab_writer);
    addWordsToKnown(verbfrequency, Configuration.verbFreq, "verb", vocab_writer);
    vocab_writer.close();
    ++vocab_count;
    docs.reset_index();
  }

  private static void addWordToHash(HashMap<Long, Integer> freq, long w) {
    if (freq.containsKey(w)) {
      freq.put(w, freq.get(w) + 1);
    } else {
      freq.put(w, 1);
    }
  }

  void Test(String base_filename, Action action) throws Exception {
    model.Test = true;
    Logger.timestamp("Testing");
    if (TESTINGCHARTS.isEmpty()) {
      for (String file : Configuration.testFile) {
        TESTINGCHARTS.add(new TestCharts(model, 0, Integer.MAX_VALUE, file, action));
      }
    }

    int document_number = 1;
    for (TestCharts testing_charts : TESTINGCHARTS) {
      // Read all sentences and set max percent for logger
      testing_charts.readSentences();
      model.grammar.print("loadedTest");
      createPool();
      String file_name = Configuration.Folder + '/' + base_filename + '.'
          + test_iterations + '.' + document_number;
      for (int i = 0; i < Configuration.threadCount; ++i) {
        executor.execute(new TestTimeParser<>(testing_charts, model,
            new InductionParser(action), file_name, exceptions));
      }
      closePool();
      ++document_number;
    }
    ++test_iterations;
    System.gc();
    model.Test = false;
  }



  protected static void createPool() {
    executor = Executors.newFixedThreadPool(Configuration.threadCount);
  }

  protected static void closePool() throws Exception {
    executor.shutdown();
    executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    if (!exceptions.isEmpty())
      throw exceptions.get(0);
  }

  void parseAndSerialize(SerializableCharts<G,CoarseToFineChart<G>> charts_to_parse, Action action) throws Exception {
    createPool();
    for (int i = 0; i < Configuration.threadCount; ++i) {
      executor.execute(new InductionUniformInitParser<>(charts_to_parse, new InductionParser(action), model, exceptions));
    }
    closePool();
  }
}
