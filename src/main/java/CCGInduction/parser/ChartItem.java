package CCGInduction.parser;

import CCGInduction.grammar.*;
import CCGInduction.Configuration;
import CCGInduction.ccg.InducedCAT;
import CCGInduction.utils.Logger;
import CCGInduction.utils.Math.Log;
import CCGInduction.utils.Math.LogDouble;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.PriorityQueue;

/**
 * Defines a chartitem for CYK parsing
 * 
 * @author bisk1
 * @param <G> Grammar Type
 */
public strictfp class ChartItem<G extends Grammar> implements Externalizable {
  private static final int PRIMEONE = 13249, PRIMETWO = 18061, PRIMETHREE = 23311;
  private static final long serialVersionUID = -5455902914589617363L;

  /** Cell that contains the chart item */
  public Cell<G> cell;
  /** CCG Category for the chart item */
  public long Category;
  /** Cell's position X */
  public int X;
  /** Cell's position Y */
  public int Y;
  /** Indicates if chart item is part of successful parse */
  boolean used = false;

  /*
   * COARSE EQUIVALENCE CLASS
   */
  private Rule_Type type;
  private Punctuation punc = Punctuation.None;
  private int arity = 0;

  /**
   * Rule type used for construction of ChartItem (used by NF)
   * 
   * @return Rule type
   */
  public final Rule_Type type() {
    return type;
  }

  /**
   * Return punctuation type if relevant
   * @return punc
   */
  public final Punctuation punc(){return punc;}
  /**
   * Arity of rule used during construction (used by NF)
   * 
   * @return Arity
   */
  public final int arity() {
    return arity;
  }

  /** Number of child parses */
  public double parses;
  public double outside_parses;
  private LogDouble alphaList;
  private LogDouble betaList;
  /** Backpointers */
  public ArrayList<BackPointer<G>> children;

  /** Tracks number of TIMES counts have been computed */
  public boolean computedCounts = false;
  /** Tracks number of TIMES inside probs have been computed */
  public boolean computedProbability = false;
  /** Number of parents */
  public int parents = 0;
  /** Number of parents that have traversed to node ( during recursion ) */
  public int seenParents = 0;

  // -- Induction -- //
  /** Corresponding InducedCat */
  public InducedCAT iCAT = null;

  /** Top K Parses */
  public final ArrayList<bp_ij<G>> topK = new ArrayList<>();
  private int nextK = 0;
  private PriorityQueue<bp_ij<G>> frontier;
  /** Fine grained ( different equivalence class ) Chart Items */
  public HashSet<ChartItem<G>> FineGrained;

  /**
   * Unary ChartItem constructor w/out punctuation
   */
  public ChartItem(long a, Rule_Type Type, int ar, Cell<G> c) {
    this.Category = a;
    this.cell = c;
    this.X = c.X;
    this.Y = c.Y;

    this.children = new ArrayList<>();
    this.type = Type;
    this.arity = ar;
  }

  /**
   * ChartItem constructor w/ punctuation
   */
  public ChartItem(long a, Rule_Type Type, int ar, Punctuation punc, Cell<G> c) {
    this.Category = a;
    this.cell = c;
    this.X = c.X;
    this.Y = c.Y;

    this.children = new ArrayList<>();
    this.type = Type;
    this.punc = punc;
    this.arity = ar;
  }

  public ChartItem() {}

  /**
   * Increment inside probability
   * 
   * @param v
   *          increment value
   */
  public final void updateAlpha(double v) {
    if (alphaList == null) {
      alphaList = new LogDouble();
    }
    if (v == Log.ZERO) {
      throw new Log.MathException("Adding ZERO to inside probability?");
    }
    alphaList.add(v);
  }

  /**
   * Set inside probability
   */
  public final void alphaInit() {
    alphaList = new LogDouble(Log.ONE);
  }

  /**
   * Get inside probability
   * 
   * @return double
   */
  public final double alpha() {
    double a = alphaList.value();
    if (a > Log.ONE || a == Log.ZERO) {
      Logger.log("ERROR\n");
      cell.chart.debugChart();
      throw new Log.MathException("We have a problem: alpha = "
          + a + '\t' + this.X + ',' + this.Y + '\t' + this.cell.chart.model.grammar.prettyCat(this.Category));
    }
    return a;
  }

  /**
   * Update outside probability
   *
   * @param v   increment
   */
  public final void updateBeta(double v) {
    if (betaList == null) {
      betaList = new LogDouble();
    }
    if (v == Log.ZERO || v > Log.ONE) {
      throw new Log.MathException("Bad Beta: " + v);
    }
    betaList.add(v);
  }

  /**
   * Set initial outside probability to 1
   */
  public final void betaInit() {
    betaList = new LogDouble(Log.ONE);
  }

  /**
   * Get outside probability
   * 
   * @return double
   */
  public final double beta() {
    double b = betaList.value();
    if (b > Log.ONE || b == Log.ZERO) {
      throw new Log.MathException("We have a problem: beta = " + b + '\t' + seenParents + '\t' + parents);
    }
    return b;
  }

  @Override
  public String toString() {
    return cell.chart.model.grammar.prettyCat(Category);
  }

  public void logEquivClass() {
    String c = cell.chart.model.grammar.prettyCat(Category);
    if (Configuration.NF.equals(Normal_Form.Full)) {
      Logger.log(String.format("%-25s %-12s %-2s %-12s", c, type(), arity(), punc().toString().replace("None", "    ")));
    } else {
      Logger.log(String.format("%-25s", c));
    }
    Logger.log(String.format("->%5d\n", children.size()));
  }

  @Override
  public final int hashCode() {
    if (type == null) {
      return Long.valueOf(Category * PRIMEONE + X * PRIMETWO + Y * PRIMETHREE).hashCode();
    }
    if (punc == null) {
      return Long.valueOf(Category * PRIMEONE + X * PRIMETWO + Y * PRIMETHREE + type.hashCode() + arity).hashCode();
    }
    return Long.valueOf(Category * PRIMEONE + X * PRIMETWO + Y * PRIMETHREE + type.hashCode() + punc.hashCode() + arity).hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!ChartItem.class.isInstance(o))
      return false;
    ChartItem<?> obj = (ChartItem<?>) o;
    return this.Category == obj.Category
        && this.X == obj.X
        && this.Y == obj.Y
        && this.arity == obj.arity
        && (obj.type == this.type || this.type.equals(obj.type))
        && (obj.punc == this.punc || this.punc.equals(obj.punc));
  }

  public final boolean addChild(Rule r, ChartItem<G> B, ChartItem<G> C) {
    BackPointer<G> bp;
    // Add a DepStructure for children
    if (C == null) {
      bp = this.cell.chart.model.newBackPointer(r, B);
      if (bp.rule != null) {
        children.add(bp);
        return true;
      }
      return false;
    }
    bp = this.cell.chart.model.newBackPointer(r, B, C);
    if (bp.rule != null) {
      children.add(bp);
      return true;
    }
    return false;
  }

  /**
   * Compue the topK parses
   * 
   * @throws Exception
   */
  public final void populateTopK(boolean Test) throws Exception {
    int K = Test ? Configuration.testK : Configuration.trainK;
    for (int i = 0; topK.size() < K && getTopK(i) != null; ++i);
  }

  /**
   * Returns the i'th best tree
   * 
   * @param index which tree to extract
   * @return Tree i'th tree
   * @throws Exception
   */
  private bp_ij<G> getTopK(int index) throws Exception {
    if (frontier == null) {
      nextK = 0;
      frontier = new PriorityQueue<>();
      for (BackPointer<G> bp : children) {
        Double p = null;
        if (bp.isUnary()) {
          bp_ij<G> B;
          if (bp.leftChild().children.isEmpty()) {
            B = bp.leftChild().topK.get(0);
          } else {
            B = bp.leftChild().getTopK(0);
          }
          if (B != null) {
            double prob = cell.chart.model.prob(this, bp);
            p = Log.mul(B.prob, prob);
          }
        } else {
          bp_ij<G> B = bp.leftChild().getTopK(0);
          bp_ij<G> C = bp.rightChild().getTopK(0);
          if (B != null && C != null) {
            double prob = cell.chart.model.prob(this, bp);
            p = Log.mul(B.prob, C.prob, prob);
          }
        }
        if (p != null) {
          bp_ij<G> newT = new bp_ij<>(p, bp, 0, 0);
          frontier.add(newT);
        }
      }
    }
    if (nextK > index) {
      return topK.get(index);
    }
    double p;
    while (!frontier.isEmpty() && (nextK <= index)) {
      // T = sup(Q)
      bp_ij<G> Tbpij = frontier.poll();
      // C.push(T)
      if (topK == null) {
        throw new AssertionError("topK data-structure is NULL: " + this.toString());
      }
      if (nextK > topK.size())
        throw new AssertionError("We skipped an index");
      topK.add(nextK, Tbpij);
      nextK++;

      if (Tbpij.bp.isUnary()) {
        p = cell.chart.model.prob(this, Tbpij.bp);
        addUnaryShoulder(Tbpij.i, Tbpij.bp, p);
      } else {
        p = cell.chart.model.prob(this, Tbpij.bp);
        addBinaryShoulders(Tbpij.i, Tbpij.j, Tbpij.bp, p);
      }

      if (nextK > index) {
        break;
      }
    }
    if (nextK > index) {
      return topK.get(index);
    }
    return null;
  }

  private void addUnaryShoulder(int i, BackPointer<G> bp, double p) throws Exception {
    bp_ij<G> B = null;
    if (!bp.leftChild().children.isEmpty()) {
      B = bp.leftChild().getTopK(i + 1);
    }
    if (B != null) {
      bp_ij<G> newT = new bp_ij<>(Log.mul(B.prob, p), bp, i + 1, 0);
      if (!frontier.contains(newT)) {
        frontier.add(newT);
      }
    }
  }

  private void addBinaryShoulders(int i, int j, BackPointer<G> bt, double p) throws Exception {
    bp_ij<G> L, R;
    // Q.push(L_{Ti}R_{Tj+1})
    L = bt.leftChild().getTopK(i);
    R = bt.rightChild().getTopK(j + 1);
    if (L != null && R != null) {
      bp_ij<G> newT = new bp_ij<>(Log.mul(L.prob, R.prob, p), bt, i, j + 1);
      if (!frontier.contains(newT)) {
        frontier.add(newT);
      }
    }
    // Q.push(L_{Ti+1}R_{Tj})
    L = bt.leftChild().getTopK(i + 1);
    R = bt.rightChild().getTopK(j);
    if (L != null && R != null) {
      bp_ij<G> newT = new bp_ij<>(Log.mul(L.prob, R.prob, p), bt, i + 1, j);
      if (!frontier.contains(newT)) {
        frontier.add(newT);
      }
    }
  }

  /**
   * For inside value
   * 
   * @param v  value
   */
  public void alphaOVERRIDE(double v) {
    alphaList = new LogDouble(v);
  }


  public Tree<G> PointersToTree(int index) {
    bp_ij<G> pointer = topK.get(index);
    // Lexical
    if (pointer.bp == null) {
      return new Tree<>(this.Category, cell.X, Rule_Type.LEX, this);
    }

    // Else -- Recurse
    Tree<G> tree;
    if(pointer.bp.isUnary()){
      tree = new Tree<>((Unary)pointer.bp.rule, this, pointer.bp, pointer.prob, pointer.bp.leftChild().PointersToTree(pointer.i));
    } else {
      tree = new Tree<>((Binary)pointer.bp.rule, this, pointer.bp, pointer.prob, pointer.bp.leftChild().PointersToTree(pointer.i), pointer.bp.rightChild().PointersToTree(pointer.j));
    }
    tree.X = this.X;
    tree.Y = this.Y;
    return tree;
  }


  public static class bp_ij<G extends Grammar> implements Comparable<bp_ij<G>> {
    final double prob;
    final int i, j;
    final BackPointer<G> bp;

    public bp_ij(double p, BackPointer<G> bp2, int I, int J) {
      prob = p;
      i = I;
      j = J;
      bp = bp2;
    }

    @Override
    public int compareTo(bp_ij<G> obj) {
      return (int)Math.signum(obj.prob - prob);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!bp_ij.class.isInstance(o)) {
        return false;
      }
      bp_ij<G> obj = (bp_ij<G>) o;
      return i == obj.i && j == obj.j && prob == obj.prob && (bp == null ? obj.bp == null : bp.equals(obj.bp));
    }
  }

  /**
   * Return POS tag at this chartitem's cell
   * @return POS tag
   */
  public final long tag() {
    return cell.chart.tags[this.X];
  }

  /**
   * Return word at this chartitem's cell
   * @return Lexical item
   */
  public final long word() {
    return cell.chart.words[this.X];
  }

  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    parents = in.readInt();
    Category = in.readLong();
    children = (ArrayList<BackPointer<G>>) in.readObject();
    type = (Rule_Type) in.readObject();
    punc = (Punctuation) in.readObject();
    arity = in.readShort();
    X = in.readShort();
    Y = in.readShort();
    alphaList = new LogDouble();
    betaList = new LogDouble();
  }

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeInt(parents);
    out.writeLong(Category);
    out.writeObject(children);
    out.writeObject(type);
    out.writeObject(punc);
    out.writeShort(arity);
    out.writeShort(X);
    out.writeShort(Y);
  }
}