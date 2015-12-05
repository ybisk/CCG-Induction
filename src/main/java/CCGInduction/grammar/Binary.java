package CCGInduction.grammar;

/**
 * Defines a Binary rule
 * 
 * @author bisk1
 */
final public class Binary extends Rule {
  private static final long serialVersionUID = 7409148494076660936L;

  /**
   * Rule Arity ( 0 for B<sup>0</sup>, 1 for B<sup>1</sup>, ... )
   */
  public final int arity;
  /**
   * Right child category of the parent
   */
  public final long C;
  /**
   * Specifies if left or right child is the functor or modifier
   */
  public final Rule_Direction head;

  /**
   * Create a Binary rule.
   * 
   * @param a Parent
   * @param b Left child
   * @param c Right child
   * @param t Rule Type
   * @param ar Rule Arity
   * @param h Head direction
   */
  public Binary(long a, long b, long c, Rule_Type t, int ar, Rule_Direction h) {
    super(a,b,t,2);
    this.C = c;
    this.arity = ar;
    this.head = h;
  }

  @Override
  public int hashCode() {
    return (int) (super.hashCode()*this.C);
  }

  @Override
  public boolean equals(Object o) {
    boolean s = super.equals(o);
    if (s) {
      Binary b = (Binary) o;
      return this.arity == b.arity && this.C == b.C && this.head == b.head;
    }
    return false;
  }

  @Override
  public String toString(Grammar g, double P) {
    return toString(g) + "\t" + P;
  }

  @Override
  public String toString(Grammar g) {
    return String.format("#BINARY#  %-15s %-20s -> %-20s %-20s %-5d %-5s",
        this.Type, g.prettyCat(this.A), g.prettyCat(this.B),
        g.prettyCat(this.C), this.arity, this.head.toString());
  }
}
