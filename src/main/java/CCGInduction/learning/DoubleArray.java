package CCGInduction.learning;

import CCGInduction.utils.Math.Log;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Provides basic Double array functionality while maintaining the small storage
 * size of doubles rather than Doubles
 * 
 * @author bisk1
 */
public class DoubleArray implements Serializable {

  private double[] da = new double[] {};
  private int size = 0;
  private final static double INCREMENT = 1.5;
  private int length = 0;
  private double sum = Log.ZERO;
  private boolean valid = true;

  /**
   * Default constructor
   */
  public DoubleArray() {}

  /**
   * Create empty array of length s
   * 
   * @param s
   *          length
   */
  public DoubleArray(int s) {
    da = new double[s];
    Arrays.fill(da, Log.ZERO);
    size = s;
    length = s;
  }

  /**
   * Return primitive double array
   * 
   * @return double[]
   */
  public final double[] vals() {
    return da;
  }

  private void set(int i, double v) {
    if (i >= length) {
      da = Arrays.copyOf(da, (int) ((i * INCREMENT) + 1));
      Arrays.fill(da, length, da.length, Log.ZERO);
      length = da.length;
    }
    da[i] = v;
    valid = false;
  }

  /**
   * Adds element value to the end of the array
   * @param value Value to add
   */
  public final synchronized void add(double value) {
    set(size, value);
    size++;
  }

  /**
   * Retrieve index
   * 
   * @param index Index
   * @return value at index
   */
  public final double get(int index) {
    return da[index];
  }

  /**
   * Sums (assumes log space) the value of the array
   * 
   * @return double
   */
  public final double sum() {
    if (!valid) {
      sum = Log.sum(da);
      valid = true;
    }
    return sum;
  }

  /**
   * Returns the product (assumes logspace) of the values in the array
   * 
   * @return double
   */
  public final double prod() {
    Arrays.sort(da);
    double product = Log.ONE;
    for (double d : da) {
      if (d != Log.ZERO) {
        product = Log.mul(product, d);
      }
    }
    return product;
  }

  /**
   * Log space ZERO-ing out
   */
  public final void clear() {
    zero();
    size = 0;
  }

  private void zero() {
    Arrays.fill(da, Log.ZERO);
    sum = Log.ZERO;
    valid = true;
  }

  /**
   * Merges a given double[]
   * 
   * @param array values to insert
   */
  public void addAll(double[] array) {
    // TODO(bisk1): This should be a single operation
    for (double d : array) {
      add(d);
    }
  }

  @Override
  public String toString() {
    return Arrays.toString(Arrays.copyOf(da, size));
  }
}
