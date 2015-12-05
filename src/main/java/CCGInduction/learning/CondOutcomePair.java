package CCGInduction.learning;

import java.io.Serializable;
import java.util.Arrays;

/**
 * An integer array
 * 
 * @author bisk1
 */
public strictfp class CondOutcomePair implements Serializable {

  /**
   * Array of conditioning variables
   */
  public final long[] conditioning_variables;
  /**
   * Outcome variable
   */
  public long outcome = -1;

  /**
   * Create a final data-structure with n conditiong variables and one outcome
   * @param cond Conditioning values
   */
  public CondOutcomePair(long... cond) {
    conditioning_variables = cond;
  }

  /**
   * Create a pair using another pair that's missing it's outcome
   * @param l outcome ID
   * @param cond conditioning variables
   */
  public CondOutcomePair(long l, CondOutcomePair cond) {
    conditioning_variables = cond.conditioning_variables;
    outcome = l;
  }

  /**
   * Get index i
   * 
   * @param i
   *          index
   * @return int
   */
  public long condVariable(int i) {
    return conditioning_variables[i];
  }

  @Override
  public boolean equals(Object o) {
    return (this == o) ||
        (CondOutcomePair.class.isInstance(o)
            && Arrays.equals(conditioning_variables, ((CondOutcomePair) o).conditioning_variables)
            && (outcome == ((CondOutcomePair) o).outcome));
  }

  @Override
  public int hashCode() {
    return (int) (Arrays.hashCode(conditioning_variables) + 5851*outcome);
  }

  @Override
  public String toString() {
    return outcome + " | " + Arrays.toString(conditioning_variables);
  }

}
