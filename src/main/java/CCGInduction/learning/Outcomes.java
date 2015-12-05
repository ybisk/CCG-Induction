package CCGInduction.learning;

import CCGInduction.utils.ObjectDoublePair;
import CCGInduction.utils.Math.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * @author bisk1
 * Datastructure for accumulating outcomes from a distribution which can
 * then be normalized/summed/etc
 */
public class Outcomes {
  /**
   * (Cond,Outcome) pairs
   */
  public final CondOutcomePair[] pairs;
  /**
   * Corresponding values/probs for each (cond,outcome) pair
   */
  public final double[] vals;
  /**
   * Next slot to fill
   */
  private int pointer = 0;

  /**
   * Create internal arrays of length size
   * @param size Number of outcomes
   */
  public Outcomes(int size) {
    pairs = new CondOutcomePair[size];
    vals = new double[size];
  }

  /**
   * Sort the outcomes by value
   */
  public void sort() {
    ArrayList<ObjectDoublePair<CondOutcomePair>> items_to_sort = new ArrayList<>();
    for (int i = 0; i < pairs.length; ++i) {
      items_to_sort.add(new ObjectDoublePair<>(pairs[i], vals[i]));
    }
    Collections.sort(items_to_sort);
    for (int i = 0; i < items_to_sort.size(); ++i) {
      pairs[i] = items_to_sort.get(i).content();
      vals[i] = items_to_sort.get(i).value();
    }
  }

  /**
   * Add one to each count (Dir prior)
   */
  void plusOne() {
    for (int i = 0; i < vals.length; i++) {
      vals[i] = Log.sloppy_add(vals[i], Log.ONE); //FIXME: Something small
    }
  }

  /**
   * Add a cond-outcome pair with a given value
   * @param cop (Cond,Outcome) pair
   * @param v Value
   */
  public void add(CondOutcomePair cop, double v) {
    pairs[pointer] = cop;
    vals[pointer] = v;
    pointer += 1;
  }

  /**
   * Sum the values
   * @return Sum
   */
  public double sum() {
    if (vals.length == 0) {
      return Log.ZERO;
    }
    return Log.sum(vals);
  }

  /**
   * Number of outcomes
   * @return |outs|
   */
  public int length() {
    return pairs.length;
  }

  /**
   * Normalize the values
   */
  public void normalize() {
    double d = sum();
    for (int i = 0; i < vals.length; i++) {
      vals[i] = Log.div(vals[i], d);
    }
  }

  @Override
  public String toString() {
    return Arrays.toString(vals);
  }
}
