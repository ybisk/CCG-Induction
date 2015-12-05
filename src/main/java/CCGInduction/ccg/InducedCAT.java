package CCGInduction.ccg;

import CCGInduction.experiments.Action;
import CCGInduction.grammar.Rule_Type;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Recursively defined CCG Categories created during induction
 * 
 * @author bisk1
 */
final public class InducedCAT implements Serializable {
  // Shared static knowledge
  static {
    createAtomics();
  }
  /**
   * Basic entity type (N)
   */
  public static CCGAtomic N;
  /**
   * Basic predicate type (S)
   */
  public static CCGAtomic S;
  /**
   * Basic predicate type (Q)
   */
  public static CCGAtomic Q;
  /**
   * Basic Prep type (PP)
   */
  public static CCGAtomic PP;
  /**
   * CCGbank
   */
  public static CCGAtomic NP;
  public static CCGAtomic COLON;
  public static CCGAtomic SEMICOLON;
  public static CCGAtomic COMMA;
  public static CCGAtomic PERIOD;
  public static CCGAtomic RRB;
  public static CCGAtomic LRB;
  /** Chinese */
  public static CCGAtomic LCP; // Localiser phrase
  public static CCGAtomic M;  // Measure rule
  public static CCGAtomic QP;  // Quantifier phrase - a NP modified by a numeral
  private static CCGAtomic LQU;
  private static CCGAtomic RQU;
  // ~SB annotation (S[dcl]\NP_y)/(S[dcl\NP_z)
  // vs coindexation (S[dcl]\NP_y)/(S[dcl\NP_y)
  /**
   * Successful parse denoted with TOP
   */
  public static CCGAtomic TOP;

  public static void createAtomics() {
    /**
     * Basic entity type (N)
     */
    N = new CCGAtomic("N");
    /**
     * Basic predicate type (S)
     */
    S = new CCGAtomic("S");
    /**
     * Basic Prep type (PP)
     */
    PP = new CCGAtomic("PP");
    /**
     * CCGbank
     */
    NP = new CCGAtomic("NP");
    COLON = new CCGAtomic(":");
    SEMICOLON = new CCGAtomic(";");
    COMMA = new CCGAtomic(",");
    PERIOD = new CCGAtomic(".");
    RRB = new CCGAtomic("RRB");
    LRB = new CCGAtomic("LRB");
    /** Chinese */
    LCP = new CCGAtomic("LCP"); // Localiser phrase
    M   = new CCGAtomic("M");   // Measure rule
    QP  = new CCGAtomic("QP");  // Quantifier phrase - a NP modified by a numeral
    LQU = new CCGAtomic("LQU");
    RQU = new CCGAtomic("RQU");
    // ~SB annotation (S[dcl]\NP_y)/(S[dcl\NP_z)
    // vs coindexation (S[dcl]\NP_y)/(S[dcl\NP_y)
    /**
     * Successful parse denoted with TOP
     */
    TOP = new CCGAtomic("TOP");
  }

  /**
   * Set of categories that can be produced by TOP
   */
  private final static CCGAtomic[] top_can_generate = new CCGAtomic[] { N, S };
  /**
   * Special conj type
   */
  public final static CCGAtomic conj = new CCGAtomic("conj");
  public static CCGAtomic[] conj_atoms = new CCGAtomic[] {conj};
  /**
   * Set of categories for use as punctuation
   */
  public static CCGAtomic[] punc = new CCGAtomic[0];

  // Local Variables
  private static final long serialVersionUID = 5162012L;
  /**
   * Category Result
   */
  public final InducedCAT Res;
  /**
   * Category Argument (may be null)
   */
  public final InducedCAT Arg;
  /**
   * Slash direction ( L,R, None )
   */
  public final Direction D;
  /**
   * CCG Atom ( S, N, ... )
   */
  public final CCGAtomic atom;
  /**
   * Category arity
   */
  public final int arity;
  /**
   * Whether category is a modifier, important for X|X
   */
  public boolean modifier;
  /**
   * Used when determining depth of composition
   */
  public int composition_arity;
  /** Does the category have a conj feature?  */
  public boolean has_conj = false;
  /**
   * Adds a "feature" to the CCGCat. Not used for conj
   */
  public CCGAtomic CCGbank_feature;

  /**
   * Define an InducedCAT as a triple of
   * 
   * @param tar  Target
   * @param arg  Argument
   * @param dir  Slash direction
   */
  private InducedCAT(InducedCAT tar, InducedCAT arg, Direction dir) {
    Res = tar;
    Arg = arg;
    D = dir;
    atom = null;
    arity = Res.arity + 1;
    modifier = false;
    composition_arity = 0;
  }

  /**
   * Create a category from aCCGAtomic
   * @param atomicCategory Atomic CCG Category
   */
  public InducedCAT(CCGAtomic atomicCategory) {
    Res = null;
    Arg = null;
    D = Direction.None;
    atom = atomicCategory;
    arity = 0;
    composition_arity = 0;
    modifier = false;
  }

  private InducedCAT(InducedCAT ic) {
    if (ic.Res != null) {
      Res = ic.Res.copy();
      Arg = ic.Arg.copy();
    } else {
      Res = null;
      Arg = null;
    }
    D = ic.D;
    atom = ic.atom;
    arity = ic.arity;
    modifier = ic.modifier;
    composition_arity = ic.composition_arity;
    has_conj = ic.has_conj;

    CCGbank_feature = ic.CCGbank_feature;
  }

  /**
   * Copy the current category
   * 
   * @return InducedCAT
   */
  public InducedCAT copy() {
    return new InducedCAT(this);
  }

  /**
   * Return a new ChartItem, this \ argument
   * 
   * @param argument New argument
   * @return InducedCAT with backwards argument
   */
  public InducedCAT backward(InducedCAT argument) {
    return new InducedCAT(this, argument, Direction.BW);
  }

  /**
   * Return a new ChartItem, this / argument
   * 
   * @param argument New argument
   * @return InducedCAT with forward argument
   */
  public InducedCAT forward(InducedCAT argument) {
    return new InducedCAT(this, argument, Direction.FW);
  }

  /**
   * Attach a feature to the category (see conj)
   * 
   * @param newFeature feature
   */
  public void addFeat(CCGAtomic newFeature) {
    CCGbank_feature = newFeature;
  }

  /**
   * Checks if any of the categories contain a feature
   */
  public boolean hasFeat() {
    if (Arg == null) {
      return CCGbank_feature != null;
    }
    return CCGbank_feature != null || Arg.hasFeat() || Res.hasFeat();
  }
  @Override
  public final String toString() {
    String string;
    if (D.equals(Direction.None)) {
      string = atom.toString();
      if (CCGbank_feature != null) {
        string += "[" + CCGbank_feature + "]";
      }
      if (has_conj) {
        string += "[conj]";
      }
    } else {
      StringBuilder str = new StringBuilder();
      if (Res.D.equals(Direction.None)) {
        str.append(Res.toString());
      } else {
        str.append('(').append(Res.toString()).append(')');
      }

      str.append(D.toString());
      if (modifier) {
        str.append('.');
      }

      if (Arg.D.equals(Direction.None)) {
        str.append(Arg.toString());
      } else {
        str.append('(').append(Arg.toString()).append(')');
      }

      if (CCGbank_feature != null) {
        str.append('[').append(CCGbank_feature).append(']');
      }
      if (has_conj) {
        str.append("[conj]");
      }
      string = str.toString();
    }
    return string;
  }


  public static InducedCAT valueOf(String str){
    // Alters co-indexation in Chinese.
    // p236, Section 6.4.2 of Daniel Tse's thesis
   return valueOf(str.replace("~SB", "").toCharArray());
  }

  private static InducedCAT valueOf(char[] str) {
    // Check if there's a feature on the category
    CCGAtomic feature = null;
    boolean conjunction = false;
    if (str[str.length-1] == ']' && str.length > 1) { // Skip the single punctuation mark ]
      int start = str.length-1;
      for (; start > 0 && str[start] != '['; --start);
      feature = CCGAtomic.valueOf(Arrays.copyOfRange(str,start+1,str.length-1));
      // Handle new features
      if (feature == null)
        feature = new CCGAtomic(String.valueOf(Arrays.copyOfRange(str,start+1,str.length-1)));

      // If we are attaching to an atomic or a category, keep the feature and shorten the string
      // S[conj]   (X|X)|(X|X)[conj]
      // All features attach to atomic's except conj attaches to "whole" category
      if (feature.equals(InducedCAT.conj) || CCGAtomic.valueOf(Arrays.copyOf(str,start)) != null)
        str = Arrays.copyOf(str,start);
      // Otherwise, the feature needs to be recomputed late to attach locally
      // (S\NP)/S[dcl]
      else
        feature = null;
    }
    if (feature != null && feature.equals(InducedCAT.conj)) {
      conjunction = true;
      feature = null;
    }
    // ... we can have up to 2 features, outer is conj
    if (str[str.length-1] == ']' && str.length > 1) {
      int start = str.length-1;
      for (; start > 0 && str[start] != '['; --start);
      feature = CCGAtomic.valueOf(Arrays.copyOfRange(str,start+1,str.length-1));
      // Handle new features
      if (feature == null)
        feature = new CCGAtomic(String.valueOf(Arrays.copyOfRange(str,start+1,str.length-1)));

      // If we are attaching to an atomic or a category, keep the feature and shorten the string
      // S[conj]   (X|X)|(X|X)[conj]
      // All features attach to atomic's except conj attaches to "whole" category
      if (feature.equals(InducedCAT.conj) || CCGAtomic.valueOf(Arrays.copyOf(str,start)) != null)
        str = Arrays.copyOf(str,start);
      // Otherwise, the feature needs to be recomputed late to attach locally
      // (S\NP)/S[dcl]
      else
        feature = null;
    }

    InducedCAT Final;
    CCGAtomic atomic = CCGAtomic.valueOf(str);
    if(atomic != null) {
      Final = new InducedCAT(atomic);
      if (feature != null) {
        Final.addFeat(feature);
      }
      Final.has_conj = conjunction;
      return Final;
    }
    int lOB = 0, lCB = 0;
    int rOB, rCB;
    for (int i = 0 ; i < str.length; ++i){
      if(str[i] == '(') {
        ++lOB;
      }
      if(str[i] == ')') {
        ++lCB;
      }
      if((str[i] == '/' || str[i] == '\\') && lOB == lCB) {
        rOB = 0;
        rCB = 0;
        for(int j = i+1; j < str.length; ++j){
          if(str[j] == '(') {
            ++rOB;
          }
          if(str[j] == ')') {
            ++rCB;
          }
        }
        InducedCAT Left;
        if(lOB > 0) {
          Left = valueOf(Arrays.copyOfRange(str, 1,i-1));
        } else {
          Left = valueOf(Arrays.copyOfRange(str, 0,i));
        }
        InducedCAT Right;
        if(str[i+1] == '.') {
          if(rOB > 0) {
            Right = valueOf(Arrays.copyOfRange(str, i+3, str.length-1));
          } else {
            Right = valueOf(Arrays.copyOfRange(str, i+2, str.length));
          }
        } else {
          if(rOB > 0) {
            Right = valueOf(Arrays.copyOfRange(str, i+2, str.length-1));
          } else {
            Right = valueOf(Arrays.copyOfRange(str, i+1, str.length));
          }
        }
        if (Left == null || Right == null)
          return null;
        if(str[i] == '/') {
          Final = Left.forward(Right);
        } else if (str[i] == '\\') {
          Final = Left.backward(Right);
        } else {
          return null;
        }
        if(str[i+1] == '.' || modifier(Final)) {
          Final.modifier = true;
        }
        if (feature != null)
          Final.addFeat(feature);
        Final.has_conj = conjunction;
        return Final;
      }
    }
    return null;
  }

  /**
   * Check if category is a modifier.  We define modifiers as X|X where
   * X \in { atomic, X|X }    This exempts control verbs (S\N)/(S\N)
   * FIXME: In CCGbank, (S|N)|(S|N) are modifiers if they do not have features
   * @param Final Category
   * @return If modifier
   */
  private static boolean modifier(InducedCAT Final) {
    if (CCGbankModifier(Final))
      return true;
    // Features are not modifiers
    if (Final.Res.equals(Final.Arg) && (Final.Res.CCGbank_feature != null || Final.Arg.CCGbank_feature != null))
      return false;
    if (!Final.Res.equals(Final.Arg)) {
      return false;
    }
    return InducedCAT.atomic(Final.Res) || modifier(Final.Res);
  }

  /**
   * (S|NP)|(S|NP)  is a modifier if there are no features.
   * Induced Categories don't have NPs
   * @param category To test
   * @return if a CCGbank modifier
   */
  private static boolean CCGbankModifier(InducedCAT category) {
    // NP/N  is a modifier
    if (CCGCategoryUtilities.softNPEquality(category.Res, category.Arg))
      return true;
    if (category.Arg == null || category.Arg.Arg == null || !category.Arg.equals(category.Res))
      return false;
    return InducedCAT.S(category.Arg.Res) && InducedCAT.NP(category.Arg.Arg);
  }

  @Override
  public int hashCode() {
    long h = 0L;
    int mul = 1;
    if (Res != null) {
      // add brackets
      h = 31*h + 40;  // Left
      h += Res.hashCode();
      h = 31*h + 41;  // Right
      mul++;
    }
    h += 31*h + D.id; // Slash
    if (Arg != null) {
      h = 31*h + 40;  // Left
      h += Arg.hashCode();
      h = 31*h + 41;  // Right
      mul++;
    }

    if (atom != null)
      h = 31*h + atom.hashCode();

    if (CCGbank_feature != null) {
      h = 31*h + 91; // Left [
      h = 31*h + CCGbank_feature.hashCode();
      mul++;
      h = 31*h + 93; // Left [
    }
    h *= this.has_conj ? 4073 : 1;
    mul *= this.has_conj ? 6451 : 1;
    h *= this.modifier ? 7673 : 1;

    h *= mul;
    return Long.valueOf(h).hashCode();
  }

  public int deprecatedHashCode() {
    char[] value = this.toString().toCharArray();
    int h = 0;
    if (value.length > 0) {

      for (char aValue : value) {
        h = 31 * h + aValue;
      }
    }
    h *= value.length;
    h += this.modifier ? 1 : 0;
    return h;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || !InducedCAT.class.isInstance(o)) {
      return false;
    }
    InducedCAT other = (InducedCAT) o;

    if (!other.D.equals(D)) {
      return false;
    } else if (!D.equals(Direction.None)) {
      return Res.equals(other.Res)
          && Arg.equals(other.Arg)
          && modifier == other.modifier
          && has_conj == other.has_conj
          && (CCGbank_feature == null ? other.CCGbank_feature == null : CCGbank_feature.equals(other.CCGbank_feature));
    } else {
      return other.atom != null
          && atom.equals(other.atom)
          && has_conj == other.has_conj
          && (CCGbank_feature == null ? other.CCGbank_feature == null : CCGbank_feature.equals(other.CCGbank_feature));
    }
  }
  public boolean equals(InducedCAT cat, Action action) {
    if (action == Action.Supervised)
      return CCGCategoryUtilities.softEquality(this, cat);
    else
      return this.equals(cat);
  }

  /**
   * When checking if two categories can BW_CONJOIN X X[conj] we need to compare
   * without the features attached
   * 
   * @param o
   *          other
   * @return boolean
   */
  public boolean equalsWithoutConj(Object o) {
    InducedCAT other = (InducedCAT) o;
    if (!other.D.equals(D)) {
      return false;
    } else if (!D.equals(Direction.None)) {
      return Res.equals(other.Res) && Arg.equals(other.Arg) && modifier == other.modifier
             && (CCGbank_feature == null ? other.CCGbank_feature == null : CCGbank_feature.equals(other.CCGbank_feature));
    } else {
      return atom.equals(other.atom)
             && (CCGbank_feature == null ? other.CCGbank_feature == null : CCGbank_feature.equals(other.CCGbank_feature));
    }
  }

  /**
   * Perform generalized composition
   * 
   * @param leftCategory Left Category
   * @param rightCategory Right Category
   * @param direction Direction of composition
   * @param depth     Depth recursed
   * @param maxArity   Maximum allowable composition arity
   * @param action Parser action/power
   * @return InducedCAT
   */
  public static InducedCAT GenComp(InducedCAT leftCategory, InducedCAT rightCategory,
                                         Rule_Type direction, int depth, int maxArity, Action action) {
    // X/Y Y/Z1...Zn --- leftCategory rightCategory ---

    InducedCAT toReturn = null;

    // If Argument category has no result ( i.e. primitive ) return null
    if (rightCategory.Res == null) {
      return null;
    }
    if (leftCategory.Res == null) {
      return null;
    }
    if (leftCategory.Res.equals(leftCategory.Arg) && leftCategory.Res.equals(rightCategory.Arg) && leftCategory.Res.equals(rightCategory.Res)
        && leftCategory.D.equals(Direction.FW) && rightCategory.D.equals(Direction.BW)) {
      return null;
    }

    // If you modify X, you can't compose into anything that modifies X/X
    // if (leftCategory.Arg.equals(leftCategory.Res) && rightCategory.Arg.equals(rightCategory.Res)){
    // return null;
    // }

    if (direction.equals(Rule_Type.FW_COMPOSE)) {
      if (leftCategory.Arg.equals(rightCategory.Res, action)) {
        toReturn = new InducedCAT(leftCategory.Res.copy(), rightCategory.Arg.copy(), rightCategory.D);
        if (leftCategory.modifier && rightCategory.modifier) {
          toReturn.modifier = true;
        }
        toReturn.composition_arity = 1;
      } else {
        if (depth < maxArity) {
          InducedCAT recursiveCall = GenComp(leftCategory, rightCategory.Res, direction, depth + 1, maxArity, action);
          if (recursiveCall != null) {
            toReturn = new InducedCAT(recursiveCall, rightCategory.Arg, rightCategory.D);
            if (toReturn.modifier && rightCategory.Arg.modifier && toReturn.equals(rightCategory.Arg, action)) {
              toReturn.modifier = true;
            }
            toReturn.composition_arity = recursiveCall.composition_arity + 1;
          }
        } else {
          return null;
        }
      }
    } else {
      if (rightCategory.Arg.equals(leftCategory.Res, action)) {
        toReturn = new InducedCAT(rightCategory.Res.copy(), leftCategory.Arg.copy(), leftCategory.D);
        if (leftCategory.modifier && rightCategory.modifier) {
          toReturn.modifier = true;
        }
        toReturn.composition_arity = 1;
      } else {
        if (depth < maxArity) {
          InducedCAT recursiveCall = GenComp(leftCategory.Res, rightCategory, direction, depth + 1, maxArity, action);
          if (recursiveCall != null) {
            toReturn = new InducedCAT(recursiveCall, leftCategory.Arg, leftCategory.D);
            if (toReturn.modifier && leftCategory.Arg.modifier && toReturn.equals(leftCategory.Arg, action)) {
              toReturn.modifier = true;
            }
            toReturn.composition_arity = recursiveCall.composition_arity + 1;
          }
        } else {
          return null;
        }
      }
    }
    if (toReturn == null || toReturn.composition_arity > maxArity) {
      return null;
    }
    return toReturn;
  }

  // ////////////////////////////////////////////////////////////////
  // Static methods for checking CCG primitives //
  // ////////////////////////////////////////////////////////////////
  /**
   * Add CCGAtomic type
   * 
   * @param existingCategories Existing Categories
   * @param newAtomicCategory Category to add
   * @return updated array
   */
  public static CCGAtomic[] add(CCGAtomic[] existingCategories, CCGAtomic newAtomicCategory) {
    if (contains(existingCategories, newAtomicCategory)) {
      return existingCategories;
    }
    CCGAtomic[] temp = Arrays.copyOf(existingCategories, existingCategories.length + 1);
    temp[temp.length - 1] = newAtomicCategory;
    return temp;
  }

  /**
   * Check if array contains a type
   * 
   * @param existingCategories Currently defined Category
   * @param queryCategory Category to search for
   * @return If set contains query
   */
  public static boolean contains(CCGAtomic[] existingCategories, CCGAtomic queryCategory) {
    for (CCGAtomic test : existingCategories) {
      if (test.equals(queryCategory)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Check if type is TOP
   * 
   * @param queryCategory category to test
   * @return If query is of type TOP
   */
  public static boolean TOP(CCGAtomic queryCategory) {
    return contains(top_can_generate, queryCategory);
  }

  /**
   * Check if type is punctuation
   * 
   * @param queryCategory category to test
   * @return If query is punctuation
   */
  public static boolean PUNC(CCGAtomic queryCategory) {
    return contains(punc, queryCategory);
  }

  /**
   * Check if atomic and type N
   * 
   * @param queryCategory category to test
   * @return if query is an N
   */
  public static boolean N(InducedCAT queryCategory) {
    return queryCategory != null && !queryCategory.has_conj
        && queryCategory.D.equals(Direction.None) && queryCategory.atom.equals(N);
  }

  /**
   * Check if atomic and type N
   *
   * @param queryCategory category to test
   * @return if query is an N
   */
  public static boolean NP(InducedCAT queryCategory) {
    return queryCategory != null && queryCategory.D.equals(Direction.None) && queryCategory.atom.equals(NP);
  }

  /**
   * Check if atomic and type QP
   * @param queryCategory category to test
   * @return if query is an QP
   */
  private static boolean QP(InducedCAT queryCategory) {
    return queryCategory != null && queryCategory.D.equals(Direction.None) && queryCategory.atom.equals(QP);
  }

  /**
   * Check if atomic and type M
   * @param queryCategory category to test
   * @return if query is an M
   */
  private static boolean M(InducedCAT queryCategory) {
    return queryCategory != null && queryCategory.D.equals(Direction.None) && queryCategory.atom.equals(M);
  }

  /**
   * Check if atomic and type LCP
   * @param queryCategory category to test
   * @return if query is an LCP
   */
  private static boolean LCP(InducedCAT queryCategory) {
    return queryCategory != null && queryCategory.D.equals(Direction.None) && queryCategory.atom.equals(LCP);
  }

  /**
   * Check if atomic and type LCP
   * @param queryCategory category to test
   * @return if query is an LCP
   */
  private static boolean LRB(InducedCAT queryCategory) {
    return queryCategory != null && queryCategory.D.equals(Direction.None) && queryCategory.atom.equals(LRB);
  }

  /**
   * Check if atomic and type S
   * 
   * @param queryCategory category to test
   * @return if query is an S
   */
  public static boolean S(InducedCAT queryCategory) {
    return queryCategory != null && !queryCategory.has_conj
        && queryCategory.D.equals(Direction.None) && queryCategory.atom.equals(S);
  }

  /**
   * Check if atomic and type S
   *
   * @param queryCategory category to test
   * @return if query is an S
   */
  public static boolean Q(InducedCAT queryCategory) {
    return queryCategory != null && !queryCategory.has_conj
        && queryCategory.D.equals(Direction.None) && queryCategory.atom.equals(Q);
  }

  /**
   * Check if atomic and type PP
   * 
   * @param queryCategory category to test
   * @return if query is a PP
   */
  private static boolean PP(InducedCAT queryCategory) {
    return queryCategory != null && queryCategory.D.equals(Direction.None) && queryCategory.atom.equals(PP);
  }

  /**
   * Check if atomic and type conj
   * 
   * @param queryCategory category to test
   * @return if query is conj
   */
  public static boolean CONJ(InducedCAT queryCategory) {
    return queryCategory != null && queryCategory.D.equals(Direction.None) && contains(conj_atoms, queryCategory.atom);
  }

  /**
   * Check if atomic
   * 
   * @param queryCategory category to test
   * @return if query is atomic
   */
  public static boolean atomic(InducedCAT queryCategory) {
    return InducedCAT.N(queryCategory)
        || InducedCAT.S(queryCategory)
        || InducedCAT.NP(queryCategory)
        || InducedCAT.PP(queryCategory)
        || InducedCAT.CONJ(queryCategory)
        || InducedCAT.QP(queryCategory)
        || InducedCAT.M(queryCategory)
        || InducedCAT.LCP(queryCategory)
        || InducedCAT.LRB(queryCategory);
  }

  /**
   * Check if the category contains an S. e.g. S, S|N, ...
   * 
   * @param queryCategory category to test
   * @return if query contains an S
   */
  private static boolean sentential(InducedCAT queryCategory) {
    return queryCategory != null && (InducedCAT.S(queryCategory) || sentential(queryCategory.Res));
  }

  /**
   * Recursively computes the number of arguments the category takes.
   * @return Number of arguments
   */
  public int depth() {
    if(this.Arg == null || this.modifier) {
      return 0;
    }
    return this.Res.depth() + 1;
  }

  /**
   * Check if category has a conj features
   * @param queryCategory category to test
   * @return if category has conj feature
   */
  public static boolean CONJ(CCGAtomic[] queryCategory) {
    return queryCategory != null && contains(queryCategory,conj);
  }

  /**
   * Counts the number of arguments (arity) of a category
   */
  public int numberOfArguments() {
    if (this.Arg == null)
      return 0;
    else
      return this.Res.numberOfArguments() + 1;
  }

  /**
   * Counts the number of nominal arguments of a category
   */
  public int numberOfNomArguments() {
    if (this.Arg == null)
      return 0;
    else {
      if (InducedCAT.N(this.Arg))
        return this.Res.numberOfNomArguments() + 1;
      else  return this.Res.numberOfNomArguments();
    }
  }

  /**
   * Returns the ith argument
   * @param arg argument index
   * @return category
   */
  public InducedCAT Arg(int arg) {
    if (arg == numberOfArguments())
      return Arg;
    else
      return Res.Arg(arg);
  }

  /**
   * Is this this the argument of a modifier functor
   * or a predicate
   */
  public boolean Mod(int arg) {
    if (arg == numberOfArguments())
      return this.modifier;
    else
      return Res.Mod(arg);
  }
}

