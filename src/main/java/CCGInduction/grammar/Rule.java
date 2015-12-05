package CCGInduction.grammar;

import java.io.Serializable;

/**
 * Specifies a Rule object
 * 
 * @author bisk1
 */
public abstract class Rule implements Serializable {
  private static final long serialVersionUID = -6645705343180657255L;

  /**
   * Parent
   */
  public final long A;
  /**
   * Type
   */
  public final Rule_Type Type;
  /**
   * Child
   */
  public final long B;

  /**
   * Number of children
   */
  public final int N;

  /**
   * Define a rule
   *
   * @param a Parent
   * @param b Child
   * @param type Type
   * @param n   unary/binary
   */
  public Rule(long a, long b, Rule_Type type, int n) {
    A = a;
    B = b;
    Type = type;
    N = n;
  }

  /**
   * Pretty print for a rule with it's probability
   * 
   * @param g
   *          Grammar
   * @param P
   *          Probability
   * @return String
   */
  public abstract String toString(Grammar g, double P);

  /**
   * Pretty print for a rule
   * 
   * @param g
   *          Grammar
   * @return String
   */
  public abstract String toString(Grammar g);

  @Override
  public int hashCode() {
    return Long.valueOf(A + B + Type.hashCode()).hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!Rule.class.isInstance(o)) {
      return false;
    }
    Rule r = (Rule) o;
    return A == r.A && Type.equals(r.Type) && B == r.B && N == r.N;
  }
}
