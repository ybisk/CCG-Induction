package CCGInduction.learning;

import CCGInduction.utils.Math.Log;
import CCGInduction.utils.Math.LogDouble;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maintains logspace counts for context arrays
 *
 * @author bisk1
 */
public class CountsArray implements Serializable {

  private final ConcurrentHashMap<Distribution, ConcurrentHashMap<CondOutcomePair,LogDouble>> counts;

  /**
   * Creates empty data-structure
   */
  public CountsArray() {
    counts = new ConcurrentHashMap<>();
  }

  /**
   * Maps (cond,out) to v if map does not contain (cond,out), else it adds it's value
   *
   * @param d Distribution
   * @param pair Conditioning variables and outcome
   * @param v  Value, or amount to increment
   */
  public final void add(Distribution d, CondOutcomePair pair, double v) {
    if (v == Log.ZERO) {
      throw new Log.MathException(d.toString() + "\tAdding count of ZERO");
    }
    final LogDouble LD = counts.get(d).get(pair);
    if (LD != null)
      LD.add(v);
    else
      counts.get(d).put(pair, new LogDouble(v));
  }

  public final void add(Distribution d, CondOutcomePair cond, long outcome, double v) {
    cond.outcome = outcome;
    add(d,cond,v);
  }

  /**
   * Merge to CountsArray objects
   * @param cA Array to be merged
   */
  public final void addAll(CountsArray cA) {
    for (Distribution d : cA.counts.keySet()) {
      ConcurrentHashMap<CondOutcomePair, LogDouble> localDistCounts = counts.get(d);
      // Attempt to place other distribution's counts, if value is already present, augment it
      cA.counts.get(d).entrySet().stream()
        .filter(condOutcomePairLogDoubleEntry -> localDistCounts.putIfAbsent(condOutcomePairLogDoubleEntry.getKey(), condOutcomePairLogDoubleEntry.getValue()) != null)
        .forEach(condOutcomePairLogDoubleEntry -> localDistCounts.get(condOutcomePairLogDoubleEntry.getKey()).merge(condOutcomePairLogDoubleEntry.getValue()));
    }
  }

  /**
   * Incorporate all counts into the distribution objects' counts
   */
  public final void updateDistributions() {
    for (Distribution D : counts.keySet()) {
      ConcurrentHashMap<CondOutcomePair,LogDouble> dist_counts = counts.get(D);
      for (CondOutcomePair pair : dist_counts.keySet()) {
        D.accumulateCount(pair, dist_counts.get(pair).value());
      }
    }
  }

  public final void updateChangedDistributions() {
    for (Distribution D : counts.keySet()) {
      ConcurrentHashMap<CondOutcomePair,LogDouble> dist_counts = counts.get(D);
      if (dist_counts != null && !dist_counts.isEmpty()) {
        for (CondOutcomePair pair : dist_counts.keySet()) {
          D.accumulateCount(pair, dist_counts.get(pair).value());
        }
        if (D.identifier.contains("base"))
          ((PYDistribution)D).updateMLE();
        else
          ((PYDistribution)D).updateVariational();
        D.clean();    // Remove empty outcomes (old tags)
      }
    }
  }

  /**
   * Accumulate counts from parsing to define a uniform prior
   */
  public final void priorCounts() {
    for (Distribution D : counts.keySet()) {
      ConcurrentHashMap<CondOutcomePair,LogDouble> dist_counts = counts.get(D);
      for (CondOutcomePair pair : dist_counts.keySet()) {
        D.priorCounts(pair, dist_counts.get(pair).value());
      }
    }
  }

  /**
   * Ad a distribution to the list for whom we maintain counts
   * @param d Distribution
   */
  public final void addDist(Distribution d) {
    counts.put(d, new ConcurrentHashMap<>());
  }

  public void clear() {
    for (Distribution D : counts.keySet())
      counts.get(D).clear();
  }
}
