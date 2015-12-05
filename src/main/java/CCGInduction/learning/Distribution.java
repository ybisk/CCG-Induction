package CCGInduction.learning;

import CCGInduction.utils.ObjectDoublePair;
import CCGInduction.Configuration;
import CCGInduction.models.Model;
import CCGInduction.utils.Logger;
import CCGInduction.utils.Math.Log;
import CCGInduction.utils.Math.LogDouble;
import CCGInduction.utils.TextFile;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Create a distribution object
 * 
 * @author bisk1
 */
public class Distribution implements Serializable {
  private static final long serialVersionUID = 5162012L;
  /** Name of the distribution */
  public final String identifier;
  private final double smallRuleP;
  private int printcount = 0;
  /**
   * Specifies if there's a uniform dirichlet prior
   */
  public boolean dirichletPrior = false;
  boolean initialized = false;

  /**
   * Map a conditioning context to all possible context-outcome pairs
   */
  public final ConcurrentHashMap<CondOutcomePair, ConcurrentHashMap<CondOutcomePair,Boolean>> conditioning_contexts =
  new ConcurrentHashMap<>();

  /**
   * Reference ot the model that uses the distribution. Needed for printing.
   */
  public Model<?> model;
  /** Previously accumulated counts **/
  public final ConcurrentHashMap<CondOutcomePair,LogDouble> Counts = new ConcurrentHashMap<>();
  /** Distribution's probabilities (For VI these may not sum to 1) */
  public final ConcurrentHashMap<CondOutcomePair,Double> Probabilities = new ConcurrentHashMap<>();
  /** New Counts being accumulated */
  final ConcurrentHashMap<CondOutcomePair,LogDouble> CountsNew = new ConcurrentHashMap<>();
  /** Which conditioning variables have counts that have been edited*/
  final ConcurrentHashMap<CondOutcomePair,Boolean> CountsNewEdited = new ConcurrentHashMap<>();
  /** Has the new counts data-structure been updated? */
  boolean CountsNewGlobalEdited = false;

  /**
   * Constructor requires a model and distribution identifier string (e.g.
   * "pDistType")
   * 
   * @param modelReference  Model
   * @param humanReadableID String representation of distribution's name
   */
  public Distribution(Model<?> modelReference, String humanReadableID) {
    model = modelReference;
    identifier = humanReadableID;
    smallRuleP = Configuration.smallRule;
  }

  public Distribution(Distribution other) {
    model = other.model;
    identifier = other.identifier;
    smallRuleP = other.smallRuleP;
    Counts.putAll(other.Counts);
    Probabilities.putAll(other.Probabilities);
    CountsNew.putAll(other.CountsNew);
    CountsNewEdited.putAll(other.CountsNewEdited);
    for (CondOutcomePair pair : other.conditioning_contexts.keySet()) {
      conditioning_contexts.put(pair, new ConcurrentHashMap<>(other.conditioning_contexts.get(pair)));
    }
    CountsNewGlobalEdited = other.CountsNewGlobalEdited;
  }

  public Distribution copy() {
    return new Distribution(this);
  }

  /**
   * Initialize distributions. Use previous iteration counts with +1 smoothing
   * for new productions for a given context to create new distributions.
   */
  public void init() {
    initialized = true;
    HashMap<CondOutcomePair,LogDouble> oldCounts = new HashMap<>(Counts);
    if (conditioning_contexts.isEmpty()) {
      throw new Log.MathException("Creating empty distribution:\t" + this.identifier);
    }
    Probabilities.clear();

    if (Configuration.uniformPrior) {
      update();
      Logger.logOnly(String.format("%-20s initialized as %6d x n array\n",
          this.identifier, conditioning_contexts.size()));
      return;
    }
    Counts.clear();
    CountsNew.clear();

    for (CondOutcomePair context : conditioning_contexts.keySet()) {
      Outcomes full = new Outcomes(conditioning_contexts.get(context).size());
      for (CondOutcomePair pair : conditioning_contexts.get(context).keySet()) {
        // Full
        if (oldCounts.containsKey(pair)) {
          Counts.put(pair, new LogDouble(Log.sloppy_add(oldCounts.get(pair).value(), Log.ONE)));
        } else {
          Counts.put(pair, new LogDouble(Log.ONE));
        }
        full.add(pair, Counts.get(pair).value());
        CountsNew.put(pair, new LogDouble(Log.ZERO));
        Probabilities.put(pair, Log.ZERO);
      }
      normalize(full);
      for (int i = 0; i < full.pairs.length; i++) {
        Probabilities.put(full.pairs[i], full.vals[i]);
      }
      CountsNewEdited.put(context, false);
    }
    CountsNewGlobalEdited = false;

    Logger.logOnly(String.format("%-20s initialized as %6d x %6d array\n",
        this.identifier, conditioning_contexts.size(), conditioning_contexts.size()));
  }

  /**
   * Return Probability
   * @param cond_outcome  Pair of conditioning variables and outcome
   * @return P( outcome given cond ) Probability
   */
  Double P(CondOutcomePair cond_outcome) {
    return Probabilities.get(cond_outcome);
  }

  /**
   * Return probability of outcome | cond
   * @param cond conditioning variable
   * @param l outcome value
   * @return probability
   */
  public Double P(CondOutcomePair cond, long l) {
    return P(new CondOutcomePair(l, cond));
  }

  /**
   * Add counts for a given distribution
   * 
   * @param pair conditioning variable + outcome pair
   * @param v value
   */
  public synchronized void accumulateCount(CondOutcomePair pair, double v) {
    if (v == Log.ZERO) {
      throw new Log.MathException("Adding count of ZERO");
    }
    if (!CountsNew.containsKey(pair)) {
      CountsNewEdited.put(new CondOutcomePair(pair.conditioning_variables), true);
      CountsNewGlobalEdited = true;
      CountsNew.put(pair, new LogDouble(v));
    } else {
      CountsNew.get(pair).add(v);
      CountsNewEdited.put(new CondOutcomePair(pair.conditioning_variables), true);
    }
  }

  /**
   * Allows us to increment uniformPrior counts based on the data
   * 
   * @param cond  Conditioning variables
   * @param outcome outcome variable
   * @param v value
   */
  public synchronized void priorCounts(CondOutcomePair cond, long outcome, double v) {
    cond.outcome = outcome;
    priorCounts(cond, v);
  }

  /**
   * Allows us to increment uniformPrior counts based on the data
   * @param pair (Condititiong, Outcome) pair
   * @param v value
   */
  public synchronized void priorCounts(CondOutcomePair pair, double v) {
    if (v == Log.ZERO) {
      throw new Log.MathException("Adding count of ZERO");
    }
    if (CountsNew.containsKey(pair)) {
      CountsNew.get(pair).add(v);
    } else {
      CountsNew.put(pair, new LogDouble(v));
      CountsNewEdited.put(new CondOutcomePair(pair.conditioning_variables), true);
      CountsNewGlobalEdited = true;
    }
    Counts.put(pair, new LogDouble(Log.ZERO));
  }

  /**
   * Adds a conditioning context and outcome to the distribution object
   * @param cond context
   * @param res  outcome
   */
  public void addContext(CondOutcomePair cond, long res) {
    if (conditioning_contexts.get(cond) == null) {
      conditioning_contexts.putIfAbsent(cond, new ConcurrentHashMap<>());
    }
    conditioning_contexts.get(cond).put(new CondOutcomePair(res, cond), true);
  }

  /**
   * Update probabilities using CountsNew Transfer CountsNew -> Counts
   * 
   */
  public void update() {
    Outcomes full;
    for (CondOutcomePair cond : conditioning_contexts.keySet()) {
      if (CountsNewEdited.containsKey(cond) && CountsNewEdited.get(cond)) {
        full = new Outcomes(conditioning_contexts.get(cond).size());
        for (CondOutcomePair pair : conditioning_contexts.get(cond).keySet()) {
          Counts.put(pair, CountsNew.get(pair));
          CountsNew.put(pair, new LogDouble(Log.ZERO));
          full.add(pair, Counts.get(pair).value());
        }
        if (this.dirichletPrior) {
          full.plusOne();
        }
        normalize(full);
        for (int i = 0; i < full.pairs.length; i++) {
          Probabilities.put(full.pairs[i], full.vals[i]);
        }
      }
      CountsNewEdited.put(cond, false);
    }
    CountsNewGlobalEdited = false;
  }

  /**
   * Print distributions and corresponding counts to a file
   * 
   * @param fileName Output file name
   * @throws IOException
   */
  public void print(String fileName) throws IOException {
    if(!initialized){
      return;
    }
    // Print
    String name = Configuration.Folder + "/";
    if (!fileName.isEmpty()) {
      name += fileName + ".";
    }
    name += toString() + "." + printcount;
    Writer output = TextFile.Writer(name + ".gz");

    // Build data structures
    // Produce an array of counts and probabilities for every set of conditioning variables.
    // These data structures:  out_cond, out_prob can then be sorted for easier vieweing
    HashMap<CondOutcomePair, Outcomes> countsOfADistribution = new HashMap<>();
    HashMap<CondOutcomePair, Outcomes> probsOfADistribution = new HashMap<>();
    HashMap<CondOutcomePair, Double> totalCountsOfADistribution = new HashMap<>();
    for (CondOutcomePair cond : conditioning_contexts.keySet()) {
      Outcomes out_count = new Outcomes(conditioning_contexts.get(cond).size());
      Outcomes out_prob = new Outcomes(conditioning_contexts.get(cond).size());
      for (CondOutcomePair pair : conditioning_contexts.get(cond).keySet()) {
        out_count.add(pair, Counts.get(pair).value());
        out_prob.add(pair, Probabilities.get(pair));
      }
      out_count.sort();
      out_prob.sort();

      countsOfADistribution.put(cond, out_count);
      probsOfADistribution.put(cond, out_prob);
      totalCountsOfADistribution.put(cond, out_count.sum());
    }

    // Array of all sets of conditioning variables in this family of distributions (sorted)
    Outcomes conds = new Outcomes(totalCountsOfADistribution.size());
    for (CondOutcomePair cond : totalCountsOfADistribution.keySet()) {
      conds.add(cond, totalCountsOfADistribution.get(cond));
    }
    conds.sort();
    double div = conds.sum();

    // Print distributions:  Sorted by frequency of the context and then by outcome
    for (int i = 0 ; i < conds.length(); ++i) {
      CondOutcomePair cond = conds.pairs[i];
      // Conditioning Variables,   % of total counts in distribution,   # total counts
      output.write(String.format("%-19s %1.5f    %10.3f\n",
          model.prettyCond(cond, this), Math.exp(Log.div(conds.vals[i], div)), conds.vals[i]));
      Outcomes cout = countsOfADistribution.get(cond);
      Outcomes pout = probsOfADistribution.get(cond);
      // Outcome:   "probability"     counts
      for (int j = 0; j < cout.length(); j++) {
        output.write(String.format("%-10s%-30s%-17.10f%-10s%-17.9f\n", "",
            model.prettyOutcome(pout.pairs[j].outcome, this), Math.exp(pout.vals[j]), "", cout.vals[j]));
      }
      output.write("\n");
    }
    output.close();

    // Also print a lexicon if appropriate
    if (this.identifier.contains("p_Tag") || this.identifier.contains("p_Word")) {
      output = TextFile.Writer(name + ".lex.gz");
      // Probability of Cat
      HashMap<String, Double> p_of_Cat = new HashMap<>();
      HashMap<String, CondOutcomePair> cat_to_int = new HashMap<>();
      HashMap<String, Long> w_to_int = new HashMap<>();
      // Probability of Word
      HashMap<String, Double> p_of_word = new HashMap<>();
      HashMap<String, HashSet<CondOutcomePair>> cats_for_word = new HashMap<>();
      Double word_sum = 0.0;
      ArrayList<ObjectDoublePair<String>> wordProbs = new ArrayList<>();
      for (int c = 0; c < conds.length(); c++) {
        String cat = model.prettyCond(conds.pairs[c], this);
        cat_to_int.put(cat, conds.pairs[c]);
        p_of_Cat.put(cat, Math.exp(Log.div(conds.vals[c], div)));
        Outcomes cout = countsOfADistribution.get(conds.pairs[c]);
        for (int i = 0; i < cout.length(); i++) {
          String w = model.prettyOutcome(cout.pairs[i].outcome, this);
          w_to_int.put(w, cout.pairs[i].outcome);

          if (!cats_for_word.containsKey(w)) {
            cats_for_word.put(w, new HashSet<>());
          }
          cats_for_word.get(w).add(conds.pairs[c]);

          if (!p_of_word.containsKey(w)) {
            p_of_word.put(w, 0.0);
          }
          p_of_word.put(w, p_of_word.get(w) + Math.exp(cout.vals[i]));
          word_sum += Math.exp(cout.vals[i]);
        }
      }
      for (String w : p_of_word.keySet()) {
        wordProbs.add(new ObjectDoublePair<>(w, p_of_word.get(w) / word_sum));
      }
      Collections.sort(wordProbs);
      for (ObjectDoublePair<String> st : wordProbs) {
        String w = st.content();
        // p(c|w) = p(w|c)*p(c)/p(w)
        output.write(String.format("%-19s %1.5f   %5.5f\n", w, p_of_word.get(w) / word_sum, p_of_word.get(w))); // p(w)
        Outcomes out = new Outcomes(cats_for_word.get(w).size());
        for (CondOutcomePair cat : cats_for_word.get(w)) {
          String c = model.prettyCond(cat, this);
          out.add(cat, Math.exp(P(cat_to_int.get(c), w_to_int.get(w))) * p_of_Cat.get(c) / (p_of_word.get(w) / word_sum));
        }
        out.sort();
        for (int i = 0; i < out.length(); i++) {
          CondOutcomePair cat = out.pairs[i];
          if(out.vals[i] != 0.0) {
            output.write(String.format("\t%-40s|\t%-20s%10.9f\n", model.prettyCond(cat, this), w, out.vals[i]));
          }
        }
        output.write("\n");
      }
      output.close();

    }

    printcount += 1;
  }

  /**
   * Use newCounts to compute prob values for conditioning var cond
   * 
   * @param outcomes A set of outcomes
   */
  void normalize(Outcomes outcomes) {
    if (outcomes == null) {
      return;
    }
    if (outcomes.sum() == Log.ZERO) {
      throw new Log.MathException("Divisor in normalization == 0");
    }

    DoubleArray below = new DoubleArray();
    DoubleArray trueV = new DoubleArray();

    ArrayList<Integer> Iterate = new ArrayList<>();

    double divisor = outcomes.sum();
    // +1 smoothing
    for (int var = 0; var < outcomes.pairs.length; var++) {
      // Real + smooth
      double v = Log.div(outcomes.vals[var], divisor);

      // Round
      // If || val - 1 || < EPSILON val = 1
      if (Log.One(v)) {
        v = Log.ONE;
      }

      outcomes.vals[var] = v;
      // Cap min
      if (outcomes.vals[var] <= smallRuleP) {
        trueV.add(outcomes.vals[var]);
        outcomes.vals[var] = smallRuleP;
        below.add(smallRuleP);
      } else {
        Iterate.add(var);
      }
    }
    // Re-normalize
    if (!Log.equal(below.sum(), trueV.sum()) && below.sum() > trueV.sum()) {
      normalizeRecursive(outcomes, Iterate, Log.trySubtract(below.sum(),
          trueV.sum()));
    }

    if (Math.abs(outcomes.sum() - 0.0) > .0001) {
      throw new Log.MathException("Doesn't sum to ONE: " + Arrays.toString(outcomes.vals));
    }
  }

  /**
   * When normalizing, there is a low probability bound on small rules. We
   * therefore have to distribute the extra mass we give them over the rules
   * with probability greater than "low P". This method recursively distributes
   * the mass over a shrinking set of high probability outcomes.
   * 
   * @param outcomes A set of outcomes
   * @param outcomesOverMinProbability Remaining outcomes to steal mass from
   * @param massToRemove Remaining probability mass needed to smooth
   */
  private void normalizeRecursive(Outcomes outcomes, ArrayList<Integer> outcomesOverMinProbability, double massToRemove) {
    if (outcomesOverMinProbability.size() == 0) {
      throw new Log.MathException("Out of outcomes to use for normalization");
    }
    double increment = Log.div(massToRemove, Math.log(outcomesOverMinProbability.size()));
    ArrayList<Integer> Iterate = new ArrayList<>();
    double testVal = Log.add(increment, smallRuleP);

    double toAccountFor = massToRemove;
    for (int col : outcomesOverMinProbability) {
      if (outcomes.vals[col] > testVal) {
        outcomes.vals[col] = Log.subtract(outcomes.vals[col], increment);
        toAccountFor = Log.subtract(toAccountFor, increment);
        Iterate.add(col);
      } else {
        double diff = Log.subtract(outcomes.vals[col], smallRuleP);
        outcomes.vals[col] = smallRuleP;
        toAccountFor = Log.subtract(toAccountFor, diff);
      }
      if (toAccountFor == Log.ZERO) {
        return;
      }
    }
    if (toAccountFor > Configuration.EPSILON) {
      normalizeRecursive(outcomes, Iterate, toAccountFor);
    }

  }

  /**
   * For use during testing if there is a probability defined for the given
   * conditioning and outcome variables.
   * 
   * @param context Conditioning variables
   * @param outcome Outcome variable
   * @return If (cond,out) pair has been seen
   */
  public boolean contains(CondOutcomePair context, long outcome) {
    return Probabilities.containsKey(new CondOutcomePair(outcome, context));
  }

  /**
   * Merge a thread's local copy of the distribution into this one.
   * @param local distribution to merge
   */
  public void merge(Distribution local) {
    if (!local.identifier.equals(identifier)) {
      System.err.println("Tried to merge incompatible distributions: " + identifier + "\t" + local.identifier);
      return;
    }
    //for (CondOutcomePair cond : local.Counts.keySet()) {
    //  if (!Counts.containsKey(cond))
    //    Counts.putIfAbsent(cond, new LogDouble());
    //  Counts.get(cond).merge(local.Counts.get(cond));
    //}
    if (local.CountsNewGlobalEdited) {
      for (CondOutcomePair cond : local.CountsNew.keySet()) {
        if (!CountsNew.containsKey(cond))
          CountsNew.putIfAbsent(cond, new LogDouble());
        CountsNew.get(cond).merge(local.CountsNew.get(cond));
      }
      for (CondOutcomePair cond : local.CountsNewEdited.keySet()) {
        if (!CountsNewEdited.containsKey(cond))
          CountsNewEdited.putIfAbsent(cond, false);
        CountsNewEdited.put(cond, CountsNewEdited.get(cond) || local.CountsNewEdited.get(cond));
      }
      CountsNewGlobalEdited = true;
    }
    for (CondOutcomePair cond : local.conditioning_contexts.keySet()) {
      conditioning_contexts.putIfAbsent(cond, new ConcurrentHashMap<>());
      conditioning_contexts.get(cond).putAll(local.conditioning_contexts.get(cond));
    }
  }

  @Override
  public String toString() {
    return identifier;
  }

  @Override
  public int hashCode() {
    return identifier.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof Distribution && identifier.equals(((Distribution)obj).identifier);
  }

  /**
   * If we have changed the representation (e.g. TagRemapping) or pruned, thre are extraneous
   * values in the data-structures (zeros).  These should be removed.
   */
  public void clean() {
    ArrayList<CondOutcomePair> trueZeros = new ArrayList<>();
    for (CondOutcomePair pair : Counts.keySet()) {
      if (Counts.get(pair) == null || Counts.get(pair).value() == Log.ZERO)
        trueZeros.add(pair);
    }

    for (CondOutcomePair pair : trueZeros) {
      Counts.remove(pair);
      CountsNew.remove(pair);
      CountsNewEdited.remove(pair);
      conditioning_contexts.get(new CondOutcomePair(pair.conditioning_variables)).remove(pair);
    }

    trueZeros.clear();
    trueZeros.addAll(conditioning_contexts.keySet().stream()
        .filter(pair -> conditioning_contexts.get(pair).isEmpty())
        .collect(Collectors.toList()));
    trueZeros.forEach(conditioning_contexts::remove);
  }

  /**
   * Interpolates distribution.  Written for lexical distributions
   */
  public void interpolateDistributions(HashMap<CondOutcomePair, Double> fromFile, Double Lambda, Double OneMinusLambda) {
    for (CondOutcomePair cat_tag : Probabilities.keySet()){
      if (fromFile.containsKey(cat_tag))
        Probabilities.put(cat_tag,
            Log.add(
                Log.mul(Lambda, Probabilities.get(cat_tag)),
                Log.mul(OneMinusLambda, fromFile.get(cat_tag))));
      else
        Probabilities.put(cat_tag, Log.mul(Lambda, Probabilities.get(cat_tag)));
    }
  }

  public final void setProbabilities(CondOutcomePair pair, double val) {
    Probabilities.put(pair,val);
  }
}
