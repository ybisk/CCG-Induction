package CCGInduction.models;

import CCGInduction.grammar.Binary;
import CCGInduction.grammar.Grammar;
import CCGInduction.grammar.Unary;
import CCGInduction.parser.BackPointer;
import CCGInduction.parser.ChartItem;

/**
 * A context array which stores in the backpointers the needed information for
 * conditioning contexts in the model
 *
 * @author bisk1
 */
public class ArgumentBackPointer extends BackPointer<Grammar> {
  private static final long serialVersionUID = 4257882733247707580L;
  private long combinator;
  private long Y;

  /**
   * Creates unary backpointer (does not have combinator or argument filled)
   * @param u Unary Rule
   * @param l Child chartItem
   */
  ArgumentBackPointer(Unary u, ChartItem<Grammar> l) {
    super(u, l);
  }

  /**
   * Creates binary backpointer (does not have combinator or argument filled)
   * @param b Binary Rule
   * @param left Left chartitem
   * @param right Right chartitem
   */
  ArgumentBackPointer(Binary b, ChartItem<Grammar> left, ChartItem<Grammar> right) {
    super(b, left, right);
  }

  /**
   * Getter for the combinator
   *
   * @return int
   */
  final long combinator() {
    return combinator;
  }

  /**
   * Sets the combinator
   *
   * @param id Combinator ID
   */
  final void combinator(Long id) {
    combinator = id;
  }

  /**
   * Gets the argument
   *
   * @return int
   */
  final long Y() {
    return Y;
  }

  /**
   * Sets argument
   *
   * @param y Argument Category
   */
  final void Y(long y) {
    Y = y;
  }

  @Override
  public boolean equals(Object obj) {
    ArgumentBackPointer other = (ArgumentBackPointer) obj;
    return combinator == other.combinator
        && Y == other.Y
        && super.equals(other);
  }
}
