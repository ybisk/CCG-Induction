package CCGInduction.grammar;

/**
 * Defines a Unary rule
 * 
 * @author bisk1
 */
final public class Unary extends Rule {
  private static final long serialVersionUID = -4579850884136077473L;

  /**
   * Define a rule
   * 
   * @param a Parent
   * @param b Child
   * @param type Type
   */
  public Unary(long a, long b, Rule_Type type) {
    super(a,b,type,1);
  }

  @Override
  public String toString(Grammar g, double P) {
    return toString(g) + "\t" + P;
  }

  @Override
  public String toString(Grammar g) {
    return String.format("#UNARY#  %-15s %-20s -> %-20s",
        this.Type, g.prettyCat(A), g.prettyCat(B));
  }

}
