package CCGInduction.utils;

import java.io.Serializable;

/**
 * An immutable data-structure for pairs of integers
 * 
 * @author bisk1
 */
final public class IntPair implements Serializable, Comparable<Object> {
  private static final long serialVersionUID = 5162012L;
  private final long A, B;
  private final int hash;

  /**
   * Constructor for 2 ints ( e.g. binary production )
   * 
   * @param a First int
   * @param b Second in
   */
  public IntPair(long a, long b) {
    A = a;
    B = b;
    hash = Long.valueOf(7919 * a + 7829 * b).hashCode();
  }

  /**
   * Constructor for 1 int ( e..g unary production )
   * 
   * @param a Single Int
   */
  public IntPair(long a) {
    A = a;
    B = -1;
    hash = Long.valueOf(7919 * a).hashCode();
  }

  @Override
  public int hashCode() {
    return hash;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!IntPair.class.isInstance(o)) {
      return false;
    }
    IntPair obj = (IntPair) o;
    return obj.A == A && obj.B == B;
  }

  @Override
  public String toString() {
    return A + " " + B;
  }

  /**
   * Only uses the first item for comparison
   */
  public int compareTo(Object o) {
    IntPair other = (IntPair) o;
    return (int) (other.A - A);
  }

  /**
   * Return the first item
   * @return first item
   */
  public long first() {return A;}
  /**
   * Return the second item
   * @return second item
   */
  public long second() {return B;}
}
