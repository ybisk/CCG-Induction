package CCGInduction.utils.Math;

/**
 * A class of basic math operations to avoid duplication
 * Created by bisk1 on 9/11/14.
 */
public class SimpleMath {
  /**
   * Computes the Harmonic Mean (F1)
   * @param correct     # successful matches
   * @param gold_count  # Gold occurrences
   * @param syst_count  # System occurrences
   * @return Harmonic Mean
   */
  public static double HarmonicMean(Integer correct, Integer gold_count, Integer syst_count) {
    if (correct == null || gold_count == null || syst_count == null ||
        correct == 0 || gold_count == 0 || syst_count == 0) {
      return 0.0;
    }

    Double precision = 1.0*correct/syst_count;
    Double recall = 1.0*correct/gold_count;
    return 100*2*precision*recall/(precision+recall);
  }

  /**
   * Computes the precision (or recall).  correct / count
   * @param correct  numerator
   * @param count denominator
   * @return  100.0*correct/count
   */
  public static double Precision(Integer correct, Integer count){
    if (correct == null || count == null || correct == 0 || count == 0) {
      return 0.0;
    }
    return 100.0*correct/count;
  }
}
