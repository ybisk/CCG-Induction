package CCGInduction.models;

import CCGInduction.Configuration;
import CCGInduction.ccg.CCGAtomic;
import CCGInduction.data.Tagset;
import CCGInduction.grammar.*;
import CCGInduction.learning.*;
import CCGInduction.parser.BackPointer;
import CCGInduction.parser.Chart;
import CCGInduction.parser.ChartItem;
import CCGInduction.utils.IntPair;
import CCGInduction.utils.Logger;
import CCGInduction.utils.Math.Log;
import CCGInduction.parser.Charts;
import CCGInduction.utils.TextFile;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

/**
 * Abstract class implemented by others which creates conditioning variable
 * contexts and requires inhereted models implement a way to assign
 * probabilities, initialize/update distributions and accumulate counts.
 * Additionally, these counts can be computed via Inside-Outside or reading from
 * CCGbank
 * 
 * @author bisk1
 */
public strictfp abstract class Model<G extends Grammar> implements Externalizable {
  private static final long serialVersionUID = 5142012L;
  public G grammar;
  public boolean createFine = true;
  public final CountsArray priorCounts = new CountsArray();
  public boolean initialized = false;
  public transient boolean updateDistributions = true;
  public transient boolean fixedGrammar = false;
  private int save_iterations = -1;

  /**
   * Distributions used in the Model
   */
  public final ArrayList<Distribution> Distributions = new ArrayList<>();

  // --- Inside-Outside Counts DSs --- //
  public CountsArray accumulatedCounts;
  public final DoubleArray LL = new DoubleArray();
  // --- Is model being used at test time -- //
  public boolean Test = false;

  // ---- Abstract ---- //
  /**
   * Builds conditioning contexts for Unary rules
   * 
   * @param parent Parent ChartItem
   * @param backPointer Backpointer
   */
  public void buildUnaryContext(ChartItem<G> parent, BackPointer<G> backPointer) {
    if (!Test) {
      grammar.requiredRules.put(backPointer.rule, true);
      grammar.unaryCheck.put(new IntPair(parent.Category, backPointer.leftChild().Category), valid.Valid);
    }
  }

  /**
   * Buils conditioning contexts for Binary rules
   * 
   * @param parent Parent ChartItem
   * @param backPointer Backpointer
   */
  public void buildBinaryContext(ChartItem<G> parent, BackPointer<G> backPointer) {
    if (!Test) {
      grammar.requiredRules.put(backPointer.rule, true);
      grammar.combinationCheck.put(new IntPair(backPointer.leftChild().Category, backPointer.rightChild().Category),
          valid.Valid);
    }
  }

  /**
   * Assigns probability to a context
   * 
   * @param parent Parent ChartItem
   * @param backpointer Backpointer
   * @return Probability of local context
   */
  public abstract double prob(ChartItem<G> parent, BackPointer<G> backpointer);

  /**
   * Accumulate count
   * 
   * @param countsArray Array storing EM counts
   * @param parent Parent ChartItem
   * @param backPointer Backpointer
   * @param countValue Count from Inside-Outside to give each outcome
   */
  protected abstract void count(ChartItem<G> parent, BackPointer<G> backPointer, double countValue, CountsArray countsArray);

  /**
   * Print a human readable version of conditioning variables
   * 
   * @param conditioningVariables Conditioning Variables
   * @param distribution Distribution variables belong to
   * @return Human readable variable array
   */
  public abstract String prettyCond(CondOutcomePair conditioningVariables, Distribution distribution);

  /**
   * Print a human readable version of the outcome variable
   * 
   * @param outcome Outcome variable
   * @param distribution Distribution variable belongs to
   * @return Human readable outcome variable
   */
  public abstract String prettyOutcome(long outcome, Distribution distribution);

  public BackPointer<G> newBackPointer(Rule r, ChartItem<G> Child) {
    return new BackPointer<>((Unary) r, Child);
  }
  public BackPointer<G> newBackPointer(Rule r, ChartItem<G> leftChild, ChartItem<G> rightChild) {
    return new BackPointer<>((Binary) r, leftChild, rightChild);
  }

  // ----- Implemented ----- //
  /**
   * Update all distributions used by the model
   */
  public void update() {
    Logger.logln(grammar.requiredRules.size() + " required Rules");
    if (Configuration.viterbi) {
      Logger.logln("Viterbi Update");
    }
    Distributions.forEach(Distribution::update);
  }

  /**
   * Initialized all distributions
   */
  public void init() {
    // Initialize Distributions
    Distributions.forEach(Distribution::init);
    initialized = true;
  }

  /**
   * Returns if initialized variable has been set.
   * @return if model is initialized
  */
  public boolean initialized() {
      return initialized;
  }

  /**
   * Print all distributions to files
   * 
   * @param filenamePrefix prefix for output files
   * @throws IOException
   */
  public void print(String filenamePrefix) throws IOException {
    if (!Configuration.printModelsVerbose) return;
    Logger.log("Printing human readable distributions.");
    for (Distribution d : Distributions) {
      d.print(filenamePrefix);
      Logger.log(".");
    }
    Logger.log("\n");
  }

  /**
   * Perform inside computation on a chart
   * 
   * @param chart Current chart to score
   */
  public final void inside(Chart<G> chart) {
    chart.likelihood = Log.ZERO;
    if (chart.success()) {
      if (chart.TOP == null) {
        throw new FailedModelAssertion("TOP is null");
      }
      insideRecurse(chart.TOP);
      chart.likelihood = chart.TOP.alpha();
      // Top should have beta = 1
      chart.TOP.betaInit();
    }
    if (chart.likelihood > Log.ONE || chart.likelihood == Log.ZERO) {
      throw new Log.MathException("Inside P (" + chart.likelihood + ") invalid for parseable sentence");
    }
  }

  /**
   * Recursively computes inside probabilities for a chartItem
   * 
   * @param parent Parent ChartItem
   */
  private void insideRecurse(ChartItem<G> parent) {
    if (parent.computedProbability) {
      return;
    }

    parent.computedProbability = true;
    if (parent.children.isEmpty()) { // Productions are observed
      parent.alphaInit();
      if (Test)
        Grammar.addLexTree(parent, new ChartItem.bp_ij<>(Log.ONE, null, 0, 0));
      return; // should already by defined by lex....
    }
    // Variables
    double p;
    // alpha_A += P(A->BC)*alpha_B*alpha_C
    for (BackPointer<G> bp : parent.children) {
      p = prob(parent, bp);
      if (bp.isUnary()) {
        insideRecurse(bp.leftChild());
        parent.updateAlpha(Log.mul(bp.leftChild().alpha(), p));
      } else {
        insideRecurse(bp.leftChild());
        insideRecurse(bp.rightChild());

        parent.updateAlpha(Log.mul(bp.leftChild().alpha(), bp.rightChild().alpha(), p));
      }
    }
  }

  /**
   * Compute the outside probabilities for a chart
   * 
   * @param chart Current chart to score
   */
  public final void outside(Chart<G> chart) {
    // Just traverse the "TOP"s children
    outsideRecurse(chart.TOP);
  }

  /**
   * Recursively compute outside probabilities for a ChartItem
   * 
   * @param parent Parent ChartItem
   */
  private void outsideRecurse(ChartItem<G> parent) {
    parent.seenParents = 0;

    // Local variables
    ChartItem<G> H;
    ChartItem<G> S;
    double p;

    for (BackPointer<G> bp : parent.children) {
      // Probability of backpointer
      p = prob(parent, bp);

      if (bp.isUnary()) {
        // beta_B = beta_A*P(A->B)
        H = bp.leftChild();
        H.updateBeta(Log.mul(parent.beta(), p));

        H.seenParents += 1;

        if (H.seenParents == H.parents && !H.children.isEmpty()) {
          outsideRecurse(H);
        }
      } else {
        H = bp.leftChild();
        S = bp.rightChild();

        H.seenParents += 1;
        S.seenParents += 1;

        // beta_B = beta_A*P(A->BC)*alpha_C
        H.updateBeta(Log.mul(parent.beta(), S.alpha(), p));
        // beta_C = beta_A*P(A->BC)*alpha_B
        S.updateBeta(Log.mul(parent.beta(), H.alpha(), p));

        if (H.seenParents == H.parents && !H.children.isEmpty()) {
          outsideRecurse(H);
        }
        if (S.seenParents == S.parents && !S.children.isEmpty()) {
          outsideRecurse(S);
        }
      }
    }
  }

  public synchronized void accumulateCounts(CountsArray cA) {
    accumulatedCounts.addAll(cA);
  }

  /**
   * Compute the counts for a chart
   * 
   * @param chart Current chart to score
   * @param countsArray Array storing EM counts
   */
  public void counts(Chart<G> chart, CountsArray countsArray) {
    countsRecurse(chart.TOP, chart.likelihood, countsArray);
  }

  /**
   * Recursively compute pseudocounts for a chart
   * 
   * @param parent Parent ChartItem
   * @param likelihood Chart's Log Likelihod
   * @param countsArray Array storing EM counts
   */
  private void countsRecurse(ChartItem<G> parent, double likelihood, CountsArray countsArray) {
    if (parent.computedCounts) {
      return;
    }
    parent.computedCounts = true;

    // Local variables
    double update;
    ChartItem<G> H;
    ChartItem<G> S;

    for (BackPointer<G> bp : parent.children) {
      if (bp.isUnary()) {
        H = bp.leftChild();
        // beta_A * alpha_B * P( A -> B )
        update = Log.div(Log.mul(parent.beta(), H.alpha(), prob(parent, bp)), likelihood);

        count(parent, bp, update, countsArray);

        if (!H.children.isEmpty()) {
          countsRecurse(H, likelihood, countsArray);
        }
      } else {
        H = bp.leftChild();
        S = bp.rightChild();
        // beta_A * alpha_B * alpha_C * P( A -> B C )
        update = Log.div(Log.mul(parent.beta(), H.alpha(), S.alpha(), prob(parent, bp)), likelihood);

        count(parent, bp, update, countsArray);

        if (!H.children.isEmpty()) {
          countsRecurse(H, likelihood, countsArray);
        }
        if (!S.children.isEmpty()) {
          countsRecurse(S, likelihood, countsArray);
        }
      }
    }
  }

  public final void printLexicon() throws IOException {
    if (!Configuration.printModelsVerbose) return;
    Logger.logln("Printing full lexicon");
    HashMap<String, HashSet<String>> catToTag =
        new HashMap<>();
    Unary u;
    String cat, tag;
    for (IntPair BC : grammar.Rules.keySet()) {
      for (Rule r : grammar.Rules.get(BC).keySet()) {
        if (r.N == 1) {
          u = (Unary) r;
          if (u.Type.equals(Rule_Type.PRODUCTION)
            && !grammar.unaryCheck(u.A,u.B).equals(valid.Invalid)) {
            cat = grammar.prettyCat(u.A);
            tag = grammar.prettyCat(u.B);
            if (!catToTag.containsKey(cat)) {
              catToTag.put(cat, new HashSet<>());
            }
            catToTag.get(cat).add(tag);
          }
        }
      }
    }

    ArrayList<String> cats = new ArrayList<>(catToTag.keySet());
    Collections.sort(cats);
    Writer output  = TextFile.Writer(Configuration.Folder + "/Lexicon.gz");
    for (String category : cats) {
      output.write(String.format("%-20s    ", category));
      ArrayList<String> tags = new ArrayList<>(catToTag.get(category));
      Collections.sort(tags);
      for (String pos : tags) {
        output.write(pos + " ");
      }
      output.write("\n");
    }
    output.close();
  }

  /*
   * Induction
   */
  public abstract CondOutcomePair backoff(CondOutcomePair cxt, Distribution d);

  // Functions
  @Override
  @SuppressWarnings("unchecked")
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    save_iterations = in.readInt();
    grammar = (G)in.readObject();
    Tagset.deSerialize((String) in.readObject());
    CCGAtomic.IDS.clear();
    CCGAtomic.IDS.addAll((ArrayList<String>) in.readObject());
  }

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeInt(save_iterations);
    out.writeObject(grammar);
    out.writeObject(Tagset.serialize());
    out.writeObject(CCGAtomic.IDS);
  }

  /**
   * Perform Inside-Outside until convergence
   * @param charts  Data source
   * @param model   Scoring model to update
   * @param threshold Convergence
   * @throws Exception
   */
  public static <G extends Grammar, C extends Chart<G>> void InsideOutside(
          Charts<G, C> charts, Model<G> model, double threshold) throws Exception {
    Logger.timestamp("Inside-Outside");
    double spll = Log.ZERO;
    ArrayList<Exception> exceptions = new ArrayList<>();
    ExecutorService executor;
    for (int iteration = 0; iteration <= Configuration.maxItr; ++iteration) {
      //Logger.log("Iteration: " + iteration + "\n");

      // Run an iteration of inside-outside
      executor = Executors.newFixedThreadPool(Configuration.threadCount);
      for (int i = 0; i < Configuration.threadCount; ++i) {
        executor.execute(new InsideOutside<>(charts, model, exceptions));
      }
      executor.shutdown();
      executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
      if(!exceptions.isEmpty())
          throw exceptions.get(0);

      double newLL = model.LL.prod();
      double v = Math.abs((spll - newLL) / spll);
      Logger.logln(String.format("\rnLL: %11.2f    (o-n)/o: %11.10f", (-1) * newLL, v));
      if (v < threshold || Log.equal(spll, newLL)) {
        Logger.logln("Converged to: " + threshold);
        break;
      }

      //if(iteration > 1 && newLL < spll && !Double.isInfinite(spll)) {
      //  Logger.logln("We took a step backwards.  Stopping due to precision " + spll + "\t" + newLL);
      //  break;
      //}

      if (newLL == Log.ZERO) {
        Logger.logln("Could not parse any of the sentences");
        break;
      }
      spll = newLL;
      model.update(); // Here? is this the cause of the descrepancy
    }
  }


  public void merge(Model<G> local) {
    if (updateDistributions) {
      for (Distribution global_dist : Distributions) {
        local.Distributions.stream().filter(dist -> global_dist.toString().equals(dist.toString())).forEach(global_dist::merge);
      }
      if (local.accumulatedCounts != null) accumulateCounts(local.accumulatedCounts);
      LL.addAll(local.LL.vals());
      priorCounts.addAll(local.priorCounts);
    }
    if (!fixedGrammar) {
      grammar.merge(local.grammar);
    }
  }

  public abstract Model<G> copy();

  public void writeToDisk() {
    try {
      if (!Configuration.saveModelFile.contains(Configuration.Folder))
        Configuration.saveModelFile = Configuration.Folder + "/" + Configuration.saveModelFile;
      String modelFile = Configuration.saveModelFile + ++save_iterations;
      ObjectOutputStream oos = new ObjectOutputStream(
          new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(modelFile))));
      oos.writeObject(this);
      oos.close();
      Logger.logln("Saved:", modelFile);
    } catch (IOException exception) {
      exception.printStackTrace();
      System.exit(1);
    }
  }

  public void clean() {
    Distributions.forEach(Distribution::clean);
  }

  public static class FailedModelAssertion extends AssertionError {
    public FailedModelAssertion(String msg) {
      super(msg);
    }
  }
}
