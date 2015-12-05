package CCGInduction.utils.Math;

import java.io.Serializable;

/**
 * Data-structure to replace Double where all operations are in Logspace
 * 
 * @author bisk1
 */
public strictfp class LogDouble implements Serializable {
  private static final long serialVersionUID = 1272014L;
  //private final static int LENGTH = 8;
  //private final double[] da = new double[] {Log.ZERO, Log.ZERO, Log.ZERO, Log.ZERO,
  //                                          Log.ZERO, Log.ZERO, Log.ZERO, Log.ZERO };
  private double sum = Log.ZERO;
  private boolean validSum = true;

  /**
   * Create LogDouble with value ZERO
   */
  public LogDouble() {}

  /**
   * Create LogDouble with value initialValue
   * 
   * @param initialValue Initial value
   */
  public LogDouble(double initialValue) {
    this();
    //da[LENGTH - 1] = initialValue;
    sum = initialValue;
    validSum = true;
  }

  /**
   * Assigns a specific value to the data-structure
   * @param v value
   */
  public final void set(double v) {
    zero();
    //da[LENGTH - 1] = v;
    sum = v;
    validSum = true;
  }

  /**
   * Increment value
   * 
   * @param increment Amount to add
   */
  public final void add(double increment) {
    sum = Log.sloppy_add(sum, increment);
//    if (validSum && sum == Log.ZERO) {
//      da[LENGTH - 1] = increment;
//      sum = increment;
//      return;
//    }
//
//
//    // TODO: Profile this and check if precision is necessary/efficient
//    // Binary search for where to insert (i.e. between the closest two)
//    int i = Arrays.binarySearch(da, increment);
//    if (i < 0) {
//      i = (-1) * i - 1;
//    }
//
//    // which is closer?
//    i = (i == LENGTH) ? i - 1 : i;
//    int closer = (i == 0) ? i :
//      (da[i - 1] == Log.ZERO || increment - da[i - 1] < da[i] - increment)
//      ? i - 1 : i;
//
//    // merge to closest
//    try {
//      da[closer] = Log.add(da[closer], increment);
//    } catch (Exception e) {
//      Util.SimpleError(e.getMessage());
//      // Find closest pair
//      double closest = Double.MAX_VALUE;
//      int index = 0;
//      for (int j = 0; j < LENGTH - 1; j++) {
//        if (da[j + 1] - da[j] < closest) {
//          closest = da[j + 1] - da[j];
//          index = j;
//        }
//      }
//      // These are the closes so it's sloppy but least precision is lost
//      da[index + 1] = Log.sloppy_add(da[index + 1], da[index]);
//      da[index] = increment;
//      Arrays.sort(da);
//    }
//
//    // propogate
//    double tmp;
//    i = closer;
//    while (i < LENGTH - 1 && da[i + 1] < da[i]) {
//      tmp = da[i + 1];
//      da[i + 1] = da[i];
//      da[i] = tmp;
//      i++;
//    }
//
//    // Sum no longer valid
//    validSum = false;
  }

  /**
   * Get value
   * 
   * @return double
   */
  public final double value() {
    if (!validSum) {
      updateSum();
    }
    return sum;
  }

  private void updateSum() {
    //sum = Log.sum(da);
    validSum = true;
  }

  private void zero() {
    //Arrays.fill(da, Log.ZERO);
    sum = Log.ZERO;
    validSum = true;
  }

  /**
   * Zero out data-structure
   */
  public final void clear() {
    zero();
  }

  @Override
  public String toString() {
    try {
      return Double.toString(value());
    } catch (Exception e) {
      System.err.println(e.getMessage());
      e.printStackTrace();
      System.exit(1);
      return null;
    }
  }

  /**
   * Get hidden values that define value
   * 
   * @return double[]
   */
  public final double[] vals() {
    //return da;
    return new double[] {sum};
  }

  /**
   * Add to another LogDouble
   * 
   * @param objectToMerge LogDouble to add
   */
  public final void merge(LogDouble objectToMerge) {
    for (double d : objectToMerge.vals()) {
      add(d);
    }
  }
}
