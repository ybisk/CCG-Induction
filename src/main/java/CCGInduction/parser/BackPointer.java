package CCGInduction.parser;

import CCGInduction.grammar.Binary;
import CCGInduction.grammar.Grammar;
import CCGInduction.grammar.Rule;
import CCGInduction.grammar.Unary;

import java.io.Serializable;

/**
 * A CYK backpointer
 * 
 * @author bisk1
 * @param <G>
 */
public class BackPointer<G extends Grammar> implements Serializable {
  private static final long serialVersionUID = -8294892965069578067L;
  // private static final long serialVersionUID = 1L;

  // IF YOU EXTEND THIS CLASS YOU MUST CHANGE THE FUNCTION IN MODEL:
  // BackPointer newBackPointer(Rule rule, ChartItem ... Children){

  /**
   * Rule used
   */
  public final Rule rule;
  /**
   * Rule Type: Left/Right/Unary/Lex
   */
  public long type;
  /**
   * Left Child
   */
  public final ChartItem<G> leftChild;
  /**
   * Right Child
   */
  public final ChartItem<G> rightChild;
  private final int hashcode;

  /**
   * Create backpointer based on children and rule used for combining
   * 
   * @param rule Rule
   * @param leftChild Left child
   * @param rightChild Right child
   */
  public BackPointer(Binary rule, ChartItem<G> leftChild, ChartItem<G> rightChild) {
    this.rule = rule;
    this.leftChild = leftChild;
    this.rightChild = rightChild;
    this.hashcode = this.rule.hashCode() + leftChild.hashCode() + rightChild.hashCode();
  }

  /**
   * Create Unary backpointer based on rule and child chart-item
   * 
   * @param rule Grammatical rule to base backpointer on
   * @param leftChild Child chartitem
   */
  public BackPointer(Unary rule, ChartItem<G> leftChild) {
    this.rightChild = null;
    this.rule = rule;
    this.leftChild = leftChild;
    this.hashcode = this.rule.hashCode() + leftChild.hashCode();
  }

  @Override
  public String toString() {
    if (this.rightChild != null) {
      return this.rule + "|" + this.leftChild + '|' + this.rightChild;
    }
    return this.rule + "|" + this.leftChild;
  }

  @Override
  public int hashCode() {
    return this.hashcode;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!BackPointer.class.isInstance(obj)) {
      return false;
    }
    BackPointer<G> o = (BackPointer<G>) obj;
    return this.rule.equals(o.rule) && this.type == o.type && this.leftChild.equals(o.leftChild)
        && ((this.rightChild == null && o.rightChild == null)
            || (this.rightChild != null && o.rightChild != null && this.rightChild.equals(o.rightChild)));
  }

  /**
   * Backpointer for unary rule
   * @return if Unary rule was used
   */
  public final boolean isUnary() {
    return this.rightChild == null;
  }

  /**
   * Left child getter
   * @return Left Child
   */
  public ChartItem<G> leftChild() {
    return this.leftChild;
  }

  /**
   * Right child getter
   * @return Right child
   */
  public ChartItem<G> rightChild() {
    return this.rightChild;
  }

  /**
   * Setter for Rule type
   * @param v Rule type (LEFT/RIGHT/...)
   */
  public final void Type(long v) {
    this.type = v;
  }

  /**
   * Rule Type for BP getter
   * @return Rule Type
   */
  public final long Type() {
    return this.type;
  }
}
