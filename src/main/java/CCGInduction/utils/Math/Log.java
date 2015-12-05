package CCGInduction.utils.Math;

/**
 * @author bisk1 Provides logspace operations.
 */
public final strictfp class Log {

  /*
   * Unlike some of the numeric methods of class StrictMath, all implementations
   * of the equivalent functions of class Math are not defined to return the
   * bit-for-bit same results. This relaxation permits better-performing
   * implementations where strict reproducibility is not required. By default
   * many of the Math methods simply call the equivalent method in StrictMath
   * for their implementation. Code generators are encouraged to use
   * platform-specific native libraries or microprocessor instructions, where
   * available, to provide higher-performance implementations of Math methods.
   * Such higher-performance implementations still must conform to the
   * specification for Math. And I repeat:
   * "Code generators are encouraged to use platform-specific native libraries"
   */

  private static final double BIG = StrictMath.log(
      StrictMath.pow(StrictMath.E, 40));
  /**
   * Log ZERO constant
   */
  public static final double ZERO = StrictMath.log(0.0);
  /**
   * Log ONE constant
   */
  public static final double ONE = StrictMath.log(1.0);

  /**
   * Performs a log add using the Manning and Schutze algorithm
   * 
   * @param valueOne Value to add
   * @param valueTwo Value to add
   * @return double
   */
  public static double add(double valueOne, double valueTwo) {
    return sloppy_add(valueOne,valueTwo);
    // We are currently disregarding precision issues
    //    if (Double.isNaN(a) || Double.isNaN(b)) {
    //      throw new MathException("NaN: " + a + "\t" + b);
    //    }
    //    /*
    //     * Manning and Schutze funct log_add =
    //     * if y - x > log_big then y
    //     * elif x - y > log BIG then x
    //     * else min(x,y) + log(exp(x - min(x,y)) + exp(y - min(x,y)))
    //     *
    //     * Scratch work
    //     *  Let min = min(x,y)
    //     *  Let max = max(x,y)
    //     *  min + log(exp(min - min) + exp(max - min))
    //     *  min + log( 1 + exp(max-min) )
    //     *  min + log1p( exp(max-min) )
    //     */
    //    double min = Math.min(a, b);
    //    double max = Math.max(a, b);
    //    if (min == Log.ZERO) {
    //      return max;
    //    }
    //
    //    if (max - min > BIG) {
    //      return max;
    //    }
    //
    //    //double newV = min + Math.log1p(Math.exp(max - min));
    //    double newV = min + Math.log(1 + Math.exp(max - min)); // FIXME: PRECISION
    //    if (newV <= max) {
    //      throw new MathException("Add Unsuccessful " + min + "\t+\t"
    //          + max + "\t=\t" + newV);
    //    }
    //    if (Double.isNaN(newV)) {
    //      throw new MathException("NaN: " + newV);
    //    }
    //    return newV;
  }

  /**
   * Attempts to add two values, returning the larger if precision doesn't allow
   * it
   * 
   * @param valueOne Value to add
   * @param valueTwo Value to add
   * @return double
   */
  public static double sloppy_add(double valueOne, double valueTwo) {
    double min, max;
    if ( valueOne < valueTwo ) {
      min = valueOne;   max = valueTwo;
    } else {
      min = valueTwo;   max = valueOne;
    }
    if (min == Log.ZERO) {
      return max;
    } else if (max - min > BIG) {
      return max;
    } else {
      // This has more precision
      //double newV = min + Math.log1p(Math.exp(max - min));
      double newV = min + Math.log(1 + Math.exp(max - min));
      if (newV <= max) {
        return max;
      }
      if (Double.isNaN(newV)) {
        throw new MathException("NaN: " + newV);
      }
      return newV;
    }
  }

  /**
   * Divides. Returning <numerator>/<denominator>
   * 
   * @param numerator Log space numerator
   * @param denominator Log space denominator
   * @return double
   */
  public static double div(double numerator, double denominator) {
    if (Double.isNaN(numerator)
        || Double.isNaN(denominator)
        || Double.isNaN(numerator - denominator)
        || denominator == Log.ZERO) {
      throw new MathException("Invalid: " + numerator + "\t" + denominator);
    }
    return numerator - denominator;
  }

  /**
   * Takes in (logspace) doubles and multiplies them.
   * 
   * @param numbers Values to multiply
   * @return return log space product
   */
  public static double mul(double... numbers) {
    double v = 0.0;
    for (double d : numbers) {
      if (d == Log.ZERO) {
        return Log.ZERO;
      }
      if (Double.isNaN(d)) {
        throw new MathException("NaN: " + d);
      }
      v += d;
    }
    // TODO / FIXME : Check that multiply was successful
    return v;
  }

  /**
   * Attempts to subtract <right> from <left> but does not throw an exception on
   * failure.
   * 
   * @param left Larger value
   * @param right Smaller value
   * @return double
   */
  public static double trySubtract(double left, double right) {
    if (left < right) {
      throw new MathException("Undefined Log for: " + left + " - " + right);
    }
    try {
      return subtract(left, right);
    } catch (Exception e) {
      System.err.println(e.getMessage());
      return left;
    }
  }

  /**
   * Subtracts <right> from <left>
   * 
   * @param left Larger value
   * @param right Smaller value
   * @return double
   */
  public static double subtract(double left, double right) {
    if (equal(left,right)) {
      return Log.ZERO;
    }
    if (left - right > BIG || right == Log.ZERO) {
      return left;
    } else if (left < right) {
      throw new MathException("Undefined Log for: " + left + " - " + right);
    }
    return right + /* Strict */Math.log(/* Strict */Math.exp(left - right) - 1);
  }

  /**
   * Sums a double array
   * 
   * @param array Array of doubles
   * @return Sum
   */
  public static double sum(double[] array) {
    // Stolen from Berkeley Parser / Ch16 Numerical Recipes
    if (array.length == 0) {
      throw new MathException("Can't sum nothing");
    }

    if (array.length == 1)
      return array[0];

    // Find Max
    double max = array[0];
    for (double d : array) {
      if (d > max) {
        max = d;
      }
    }

    if (equal(max, Log.ZERO)) {
      return Log.ZERO;
    }

    double sum = 0.0;
    for (double d : array) {
      sum += /* Strict */Math.exp(d - max);
    }

    double d = max + /* Strict */Math.log(sum);
    if (equal(d, Log.ONE)) {
      d = Log.ONE;
    }
    return d;
  }

  /**
   * Returns the absolute value of the difference of two numbers (<valueOne> and <valueTwo>)
   * 
   * @param valueOne First value
   * @param valueTwo Second value
   * @return ABS of difference
   */
  static double ABSsubtract(double valueOne, double valueTwo) {
    if (valueOne > valueTwo) {
      return subtract(valueOne, valueTwo);
    }
    return subtract(valueTwo, valueOne);
  }

  /**
   * Performs a ``soft" equals check.
   * 
   * @param valueOne First value
   * @param valueTwo Second value
   * @return ``soft" equality
   */
  public static boolean equal(double valueOne, double valueTwo) {
    double maxRelativeError = 0.00000001;
    if (valueOne == valueTwo) {
      return true;
    }
    double relativeError;
    if (Math.abs(valueTwo) > Math.abs(valueOne)) {
      relativeError = Math.abs((valueTwo - valueOne) / valueTwo);
    } else {
      relativeError = Math.abs((valueOne - valueTwo) / valueOne);
    }
    return relativeError <= maxRelativeError;
  }

  /**
   * Check if value soft-equals One
   * @param value Test value
   * @return if equal to one
   */
  public static boolean One(double value) {
    return (value >= 0 && value < 1E-12) || (value < 0 && (-1 * value) < 1E-12);
  }

  /**
   * Check if value soft-equals Zero
   * @param value Test value
   * @return if equal to zero
   */
  static boolean Zero(double value) {
    return value < -15;
  }

  /**
   * Exponentiate an array of log-space doubles
   * 
   * @param vals Values to exponentiate
   * @return double[exp(x)]
   */
  static double[] exp(double... vals) {
    double[] newV = new double[vals.length];
    for (int i = 0; i < vals.length; i++) {
      newV[i] = Math.exp(vals[i]);
    }
    return newV;
  }

  public static class MathException extends RuntimeException {
    public MathException(String s) {
      super(s);
    }
  }
}
