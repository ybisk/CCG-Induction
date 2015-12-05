package CCGInduction.hmm;

import CCGInduction.Configuration;
import CCGInduction.data.LexicalToken;
import CCGInduction.data.Sentence;
import CCGInduction.grammar.Grammar;
import CCGInduction.learning.CondOutcomePair;
import CCGInduction.learning.Distribution;
import CCGInduction.learning.PYDistribution;
import CCGInduction.parser.BackPointer;
import CCGInduction.parser.ChartItem;
import CCGInduction.utils.IntPair;
import CCGInduction.utils.Logger;
import CCGInduction.utils.Math.Log;
import CCGInduction.data.Sentences;
import CCGInduction.learning.CountsArray;
import CCGInduction.models.Model;
import CCGInduction.utils.TextFile;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Stores the distributions needed for a bi-gram HMM
 * Created by bisk1 on 2/13/15.
 */
public class BigramModel extends Model<Grammar> {
  private static final long serialVersionUID = 1470030815620296516L;
  public static long UNK;
  PYDistribution p_Tag$prev = new PYDistribution(this, "p_Tag$prev", Configuration.alphaPower[0], 0.0, false);
  PYDistribution p_Word$tag = new PYDistribution(this, "p_Word$tag", Configuration.alphaPower[1], 0.0, false);

  public Sentences sentences;
  public boolean initialized = false;

  final ArrayList<Trellis> data = new ArrayList<>();

  public BigramModel() {}

  public BigramModel(Sentences sentences, Grammar grammar) throws Exception {
    this.grammar = grammar;
    UNK = grammar.Lex("UNK");
    Distributions.add(p_Tag$prev);
    Distributions.add(p_Word$tag);

    sentences.loadIntoMemory();
    this.sentences = sentences;
    sentences.reset_index();
    kwords(sentences);
    Sentence sentence;
    while ((sentence = sentences.next()) != null)
      data.add(new Trellis(sentence, Configuration.NumClusters, grammar.learnedWords));
    sentences.reset_index();
  }

  BigramModel(BigramModel model) {
    p_Word$tag = model.p_Word$tag.copy();
    p_Tag$prev = model.p_Tag$prev.copy();
    this.grammar = model.grammar.copy();
  }

  private void createContexts() {
    for (Trellis trellis : data) {
      for (long word : trellis.words) {
        for (int k = 0; k < Configuration.NumClusters; ++k) {
          p_Word$tag.addContext(new CondOutcomePair(k), word);
        }
      }
    }
    for (int cur = 0; cur < Configuration.NumClusters; ++cur) {
      p_Tag$prev.addContext(new CondOutcomePair(-1), cur);
      p_Tag$prev.addContext(new CondOutcomePair(cur), -1);
    }
    for (int prev = 0; prev < Configuration.NumClusters; ++prev) {
      for (int cur = 0; cur < Configuration.NumClusters; ++cur) {
        p_Tag$prev.addContext(new CondOutcomePair(prev), cur);
      }
    }
  }

  public void init() {
    createContexts();
    p_Word$tag.init();
    p_Tag$prev.init();   // Remain uniform

    // Initialize distributions
    Random random = new Random();
    for (Long word : grammar.learnedWords.keySet()) {
      for (int c = 0; c < Configuration.NumClusters; ++c) {
        p_Word$tag.accumulateCount(new CondOutcomePair(word, new CondOutcomePair(c)), Math.log(1 + random.nextDouble()));
      }
    }
    p_Word$tag.updateVariationalNoBase();
    initialized = true;
  }

  public void initFromData() {
    createContexts();
    p_Word$tag.init();
    p_Tag$prev.init();

    long word;
    int cluster;
    int prev;
    for (Sentence sentence : sentences) {
      prev = -1;
      for (LexicalToken token : sentence) {
        word = token.rawWord();
        cluster = Integer.valueOf(token.induced().toString());
        if (cluster == -1)
          cluster = Configuration.NumClusters;
        p_Word$tag.accumulateCount(new CondOutcomePair(word, new CondOutcomePair(cluster)), Log.ONE);
        p_Tag$prev.accumulateCount(new CondOutcomePair(cluster, new CondOutcomePair(prev)), Log.ONE);
        prev = cluster;
      }
      p_Tag$prev.accumulateCount(new CondOutcomePair(-1, new CondOutcomePair(prev)), Log.ONE);
    }
    update();
    initialized = true;
  }

  @Override
  public CondOutcomePair backoff(CondOutcomePair cxt, Distribution d) {
    return new CondOutcomePair(-1);
  }

  public int train() throws Exception {
    if (!initialized)
      init();
    ConcurrentLinkedQueue<Trellis> queue = new ConcurrentLinkedQueue<>();
    ExecutorService executor;
    double newLL, change;
    double oldLL = Log.ZERO;

    for (int round = 0; round < 2000; ++round) {
      queue.addAll(data);
      LL.clear();
      accumulatedCounts = new CountsArray();
      Distributions.forEach(accumulatedCounts::addDist);
      executor = Executors.newFixedThreadPool(Configuration.threadCount);
      for (int i = 0; i < Configuration.threadCount; ++i) {
        executor.execute(new BaumWelch(queue, this, Configuration.NumClusters));
      }
      executor.shutdown();
      executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

      newLL = (-1)*LL.prod();
      change = (oldLL - newLL)/ oldLL;
      Logger.logln(String.format("%-15.5f    %-7.5f", newLL, change));
      if (change < Configuration.threshold && !Double.isNaN(change) && !Double.isInfinite(oldLL))
        return round;
      oldLL = newLL;
      accumulatedCounts.updateDistributions();
      update();
    }
    return 2000;
  }

  public void viterbi() throws Exception {
    ExecutorService executor;
    sentences.reset_index();
    executor = Executors.newFixedThreadPool(Configuration.threadCount);
    for (int i = 0; i < Configuration.threadCount; ++i) {
      executor.execute(new Viterbi(sentences, this, Configuration.NumClusters));
    }
    executor.shutdown();
    executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    sentences.reset_index();
    sentences.writeToDisk(Configuration.Folder + "/tagged.json.gz");
  }

  @Override
  public BigramModel copy() {
    return new BigramModel(this);
  }

  @Override
  public double prob(ChartItem parent, BackPointer backpointer) {
    throw new FailedModelAssertion("Wrong type of model");
  }

  @Override
  protected void count(ChartItem parent, BackPointer backPointer, double countValue, CountsArray countsArray) {
    throw new FailedModelAssertion("Wrong type of model");
  }

  @Override
  public String prettyCond(CondOutcomePair conditioningVariables, Distribution distribution) {
    switch (distribution.identifier) {
      case "base_Words":
      case "base_Tags":
        return "NULL";
      case "p_Word$tag":
      case "p_Tag$prev":
        return String.valueOf(conditioningVariables.condVariable(0));
    }
    return null;
  }

  @Override
  public String prettyOutcome(long outcome, Distribution distribution) {
    switch (distribution.identifier) {
      case "base_Words":
      case "p_Word$tag":
        return grammar.Words.get(outcome);
      case "base_Tags":
      case "p_Tag$prev":
        return String.valueOf(outcome);
    }
    return null;
  }

  @Override
  public void update() {
    p_Word$tag.updateVariationalNoBase();
    p_Tag$prev.updateVariationalNoBase();
  }

  double p_Transition(int cur, int next) {
    return p_Tag$prev.P(new CondOutcomePair(cur), next);
  }

  double p_Emit(long word, int cluster) {
    return p_Word$tag.P(new CondOutcomePair(cluster), word);
  }

  void count_Emit(CountsArray counts, long word, int cluster, double val) {
    counts.add(p_Word$tag, new CondOutcomePair(cluster), word, val);
  }

  void count_Trans(CountsArray counts, int cur, int next, double val) {
    counts.add(p_Tag$prev, new CondOutcomePair(cur), next, val);
  }

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    super.writeExternal(out);
    out.writeObject(p_Tag$prev);
    out.writeObject(p_Word$tag);
    out.writeBoolean(initialized);
  }

  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    super.readExternal(in);
    p_Tag$prev = (PYDistribution) in.readObject();
    p_Word$tag = (PYDistribution) in.readObject();
    initialized = in.readBoolean();
  }

  void merge(BigramModel other) {
    p_Word$tag.merge(other.p_Word$tag);
    p_Tag$prev.merge(other.p_Tag$prev);
    LL.addAll(other.LL.vals());
    accumulatedCounts.addAll(other.accumulatedCounts);
    grammar.merge(other.grammar);
  }

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
    Writer vocab_writer = TextFile.Writer(Configuration.Folder + "/Vocab.txt.gz");
    HashMap<Long, Integer> frequency = new HashMap<>();

    // Add most common lexical items
    Sentence ia;
    while ((ia = docs.next()) != null) {
      for (int k = 0; k < ia.length(); k++) {
        long w = ia.get(k).rawWord();
        if (Configuration.lexFreq != 0) {
          if (frequency.containsKey(w)) {
            frequency.put(w, frequency.get(w) + 1);
          } else {
            frequency.put(w, 1);
          }
        }
      }
    }

    ArrayList<IntPair> valPairs = frequency.keySet().stream().map(
        key -> new IntPair(frequency.get(key), key))
        .collect(Collectors.toCollection(ArrayList::new));
    Collections.sort(valPairs);
    for (IntPair current : valPairs) {
      if (!grammar.learnedWords.containsKey(current.second())) {
        if (current.first() >= Configuration.lexFreq) {
          grammar.learnedWords.put(current.second(), true);
          vocab_writer.write(String.format("%4d %-14s \n", current.first(), grammar.Words.get(current.second())));
        }
      }
    }
    vocab_writer.close();
    docs.reset_index();
  }
}
