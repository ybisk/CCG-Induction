package CCGInduction.utils.Math;

import java.util.Arrays;
import java.util.Random;

/**
 * Library for sampling random numbers from various distributions or for
 * computing the digamma function
 * 
 * @author bisk1
 */
public final class Sample {
  /**
   * Create Sample data-structure
   */
  private Sample() {}

  private final Random rand = new Random(9052012);

  /**
   * Sample from Uniform distribution [alpha,beta] Mean: 0.5*(alpha + beta) Var:
   * (1/12)(beta - alpha)^2
   * 
   * @param alpha Start of distribution range
   * @param beta  End of distribution range
   * @return Sample
   */
  double Uniform(double alpha, double beta) {
    return (beta - alpha) * rand.nextFloat() + alpha;
  }

  /**
   * Sample from Uniform distribution [0.0,1.0] Mean: 0.5 Var: 1/12
   * 
   * @return Sample
   */
  double Uniform() {
    return rand.nextFloat();
  }

  /**
   * Sample from Beta distribution with shape parameters. E[X] = alpha / (alpha
   * + beta) Var[X] = alpha*beta / ( (alpha+beta)^2 * (alpha + beta + 1) )
   * @return sample
   */
  double Beta(double alpha, double beta) {
    return Dirichlet(new double[] { alpha, beta })[0];
  }

  /**
   * Sample from Dirichlet distribution with hyper params E[Xi] = alpha_i /
   * sum(params) Var[Xi] = alpha_i(sum(params) - alpha_i) / ( sum(params)^2 *
   * (sum(params)+1) )
   * 
   * @param params Hyperparameters
   * @return dirichlet sample
   */
  double[] Dirichlet(double[] params) {
    double[] sample = new double[params.length];
    double sum = 0.0;
    for (int i = 0; i < params.length; i++) {
      sample[i] = Gamma(params[i], 1);
      sum += sample[i];
    }
    for (int i = 0; i < params.length; i++)
      sample[i] /= sum;
    return sample;
  }

  /**
   * Samples from Gamma distribution and scales Mean: shape / scale Var: shape /
   * scale^2
   * 
   * @param shape Shape parameter
   * @param scale Scaling
   * @return sample
   */
  double Gamma(double shape, double scale) {
    return Gamma(shape) * scale;
  }

  /**
   * Sample from a Gamma distribution with shape
   * 
   * @param shape Shape parameter
   * @return sample
   */
  final double Gamma(double shape) {
    double u, v, w, x, y, z;
    double b, c;

    if (shape <= 0.0)
      return 0.0;

    // Exponential
    if (shape == 1.0)
      return -Math.log(Uniform());

    // John's Gamma Generator (Devroye 1986 - 418)
    if (shape < 1.0) {
      while (true) {
        u = Uniform();
        v = Uniform();
        x = Math.pow(u, 1 / shape);
        y = Math.pow(v, 1 / (1 - shape));
        if (x + y <= 1) {
          // X is gamma(shape) distributed
          return -Math.log(Uniform()) * x / (x + y);
        }
      }
    }
    // Best's algorithm (Devroye 1986 - 410)
    b = shape - 1;
    c = 3 * shape - 0.75;
    while (true) {
      u = Uniform();
      v = Uniform();
      w = u * (1 - u);
      y = Math.sqrt(c / w) * (u - 0.5);
      x = b + y;
      if (x >= 0) {
        z = 64 * w * w * w * v * v;
        if (z <= 1 - 2 * y * y / x 
            || Math.log(z) <= 2 * (b * Math.log(x / b) - y))
          return x;
      }
    }
  }

  /**
   * Digamma constant
   */
  private final static double S = 0.00001;
  /**
   * Digamma constant
   */
  private final static double STHREE = 0.08333333333;
  /**
   * Digamma constant
   */
  private final static double SFOUR = 0.0083333333333;
  /**
   * Digamma constant
   */
  private final static double SFIVE = 0.003968253968;
  /**
   * Digamma constant
   */
  private final static double BIG = 8.5;
  /**
   * Digamma constant
   */
  private final static double D = -0.5772156649;

  /**
   * Compute the digamma of x
   * 
   * @param logspace Input value
   * @return digamma(exp[logspace])
   */
  public static double digamma(double logspace) {
    // http://people.sc.fsu.edu/~jburkardt/c_src/asa103/asa103.c
    /* Check the input. */
    if (logspace == Log.ZERO)
      throw new Log.MathException("Invalid input: " + logspace);

    // If the logspace value is so small we can't exponentiate it due to precision
    // then return a zero in logspace
    double x = Math.exp(logspace);
    if (x <= 0.0)
      return Log.ZERO;

    /* Initialize. */
    double y = x;
    double value = 0.0;
    /* Use approximation if argument <= S. */
    if (y <= S) {
      value = D - 1.0 / y;
      return value;
    }
    /* Reduce to DIGAMA(X + N) where (X + N) >= C. */
    while (y < BIG) {
      value -= 1.0 / y;
      y += 1.0;
    }
    /* Use Stirling's (actually de Moivre's) expansion if argument > C. */
    double r = 1.0 / y;
    value = value + Math.log(y) - 0.5 * r;
    r *= r;
    value -= r * (STHREE - r * (SFOUR - r * SFIVE));

    return value;
  }

  /*
   * a simple approximation to the digamma function:
   * "Infinite Family of Approximations of the Digamma Function" Isa Muqattash
   * and Mohammed Yahdi //
   * "Sharp Inequalities for the Psi Function and Harmonic Numbers" // Feng Qi
   * and Bai-Ni Guo public static double digamma(double x) { if(x <= 0) throw
   * new
   * IllegalArgumentException("argument to digamma less than or equal to ZERO");
   * return Math.log(x + 0.5) - 1.0 / x; }
   */

  // //////// ---------- ---------- LogSpace ---------- ---------- ----------
  // ---------- //////////

  /**
   * Test class
   * 
   * @param args Commandline arguments are ignored
   */
  public static void main(String[] args) {
    Sample sam = new Sample();
    System.out.println(String.format("%2s %-20s %-20s %-20s",
                                     "#", "Uniform", "Beta", "Gamma"));
    for (int i = 1; i <= 10; i++) {
      System.out.println(String.format("%2d %1.15f %1.15f %2.15f", 
                         i, sam.Uniform(), sam.Beta(i, 1), sam.Gamma(i, 1)));
    }
    System.out.println(String.format("%2s %8s Dirichlet", "a", "variance"));
    for (int i = 1; i <= 10; i++) {
      double[] sample = sam.Dirichlet(new double[] { i, i, i, i, i });

      double mean = 0.0;
      for (double aSample : sample) mean += aSample;
      mean /= sample.length;

      double var = 0.0;
      for (double aSample : sample) var += Math.pow(aSample - mean, 2);
      var /= sample.length;
      System.out.println(String.format("%2d %2.5f %s",
                                       i, var, Arrays.toString(sample)));
    }
    double x;
    for (x = 0.1; x < 10; x += 0.1)
      System.out.println(String.format("digamma(%g) = %g, exp(digamma(%g)) = %g"
                         , x, digamma(x), x, Math.exp(digamma(x))));
  }
}
