package CCGInduction.grammar;

import CCGInduction.Configuration;
import CCGInduction.parser.ChartItem;

/**
 * Class for determining if a parser action abides by Normal-Form cosntraints
 * which are specified in @see Normal_Form
 * 
 * @author bisk1
 */
public class NF {

  /**
   * Determines if a given rule type can be used given the previous rule.
   * 
   * @param previousRuleType Previously used rule type
   * @param currentRuleType  Current rule
   * @return boolean
   */
  public static boolean unaryNF(Rule_Type previousRuleType, Rule_Type currentRuleType) {
    switch (Configuration.NF) {
    case None:
    case Eisner:
    case Eisner_Orig:
      return true;
    case Full_noPunct:
    case Full:
      // NF Constraint 6 (atom and phi) The result of coordination phi cannot be
      // type-raised.
      if (previousRuleType.equals(Rule_Type.FW_CONJOIN)
          || previousRuleType.equals(Rule_Type.BW_CONJOIN)) {
        if (currentRuleType.equals(Rule_Type.FW_TYPERAISE)
            || currentRuleType.equals(Rule_Type.BW_TYPERAISE)) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  /**
   * Determines if a given rule can be used given the histories (type and arity
   * ) of both both child categories
   * 
   * @param parentRuleType Parent type
   * @param parentRuleArity Parent arity
   * @param leftChildRuleType Left child type
   * @param leftChildRuleArity Left child arity
   * @param rightChildRuleType Right child type
   * @param rightChildRuleArity Right child arity
   * @return If binary rule abides by normal-form
   */
  private static boolean binaryNF(Rule_Type parentRuleType, int parentRuleArity,
                                  Rule_Type leftChildRuleType, int leftChildRuleArity,
                                  Rule_Type rightChildRuleType, int rightChildRuleArity) {
    if(leftChildRuleType == null || rightChildRuleType == null) {
      return true;
    }
    switch (Configuration.NF) {
    case Eisner:
      return Eisner_NF(parentRuleType, parentRuleArity, leftChildRuleType,
              leftChildRuleArity, rightChildRuleType, rightChildRuleArity);
    case Eisner_Orig:
      return Eisner_Orig_NF(parentRuleType, leftChildRuleType, rightChildRuleType);
    case Full_noPunct:
      return Full_noP_NF(parentRuleType, parentRuleArity, leftChildRuleType,
              leftChildRuleArity, rightChildRuleType, rightChildRuleArity);
    case Full:
      return Full_NF(parentRuleType, parentRuleArity, leftChildRuleType,
              leftChildRuleArity, rightChildRuleType, rightChildRuleArity);
    case None:
      return true;
    }
    return false;
  }

  public static boolean binaryNF(Binary rule, ChartItem left, ChartItem right) {
    return binaryNF(rule.Type, rule.arity, left.type(), left.arity(), right.type(), right.arity());
  }

  private static boolean Eisner_NF(Rule_Type A, int Aa, Rule_Type B, int Ba, Rule_Type C, int Ca) {
    if (B.equals(Rule_Type.LEX) || C.equals(Rule_Type.LEX)) {
      return false;
    }
    // NF Constraint 1 (B0 and Bn>=1) The output of
    // >B_{n>=1} (resp. <Bn>=1) cannot be primary functor
    // for >B_{n<=1} (resp. <Bn<=1).

    // NF Constraint 2 (B1 and B_{n>=1})
    // The output of >B1 (resp. <B1) cannot be primary functor
    // for >B_{n>=1} (resp. <B_{n>=1}).

    // NF Constraint 3 (Bn>1 and Bm>1) The output of >Bm (resp. <Bm)
    // cannot be secondary functor for >Bn>m (resp. <Bn>m).

    switch (A) {
    case FW_APPLY:
      // NF Constraint 1:
      // The output of forward composition (i.e. arity >= 1 )
      // cannot be the primary functor (B) for forward composition
      // with arity <= 1
      if (FW_COMPOSE(B)) {
        return false;
      }
      break;

    case FW_COMPOSE:
    case FW_XCOMPOSE:
    case FW_2_COMPOSE:
    case FW_3_COMPOSE:
      if (FW_COMPOSE(B)) {
        // NF Constraint 1:
        // The output of forward composition (i.e. arity >= 1 )
        // cannot be the primary functor (B) for forward composition
        // with arity <= 1
        if (Aa == 1) {
          return false;
        }

        // NF Constraint 2:
        // The output of forward composition arity=1, cannot be the primary
        // functor (B) for forward composition with arity >= 1
        if (Ba == 1 && Aa >= 1) {
          return false;
        }
      }
      if (FW_COMPOSE(C)) {
        // NF Constraint 3:
        // the output of forward composition (arity=m) cannot be the secondary
        // functor (C) in forward composition (arity=n) where n > m
        if (Ca < Aa) {
          return false;
        }
      }
      break;

    case BW_APPLY:
      // NF Constraint 1:
      // The output of backward composition arity >= 1
      // cannot be the primary functor (C) for backward composition
      // with arity <= 1
      if (BW_COMPOSE(C)) {
        return false;
      }
      break;

    case BW_COMPOSE:
    case BW_XCOMPOSE:
    case BW_2_COMPOSE:
    case BW_3_COMPOSE:
      if (BW_COMPOSE(C)) {
        // NF Constraint 1:
        // The output of backward composition arity >= 1
        // cannot be the primary functor (C) for backward composition
        // with arity <= 1
        if (Aa == 1) {
          return false;
        }

        // NF Constraint 2:
        // The output of backward composition arity=1, cannot be the primary
        // functor (C) for backward composition with arity >= 1
        if (Ca == 1 && Aa >= 1) {
          return false;
        }
      }
      if (BW_COMPOSE(C)) {
        // NF Constraint 3:
        // the output of backward composition (arity=m) cannot be the secondary
        // functor (B) in backward composition (arity=n) where n > m
        if (Ba < Aa) {
          return false;
        }
      }
      break;

    default:
      return true;
    }

    return true;

  }

  private static boolean Eisner_Orig_NF(Rule_Type A, Rule_Type B, Rule_Type C) {
    if (B.equals(Rule_Type.LEX) || C.equals(Rule_Type.LEX)) {
      return false;
    }

    switch (A) {
    case FW_APPLY:
    case FW_COMPOSE:
    case FW_XCOMPOSE:
    case FW_2_COMPOSE:
    case FW_3_COMPOSE:
      // If B is FW_COMPOSE then A can't be FW_APPLY or FW_COMPOSE
      if (FW_COMPOSE(B)) {
        return false;
      }
      break;
    case BW_APPLY:
    case BW_COMPOSE:
    case BW_XCOMPOSE:
    case BW_2_COMPOSE:
    case BW_3_COMPOSE:
      // If C is BW_COMPOSE then A can't be BW_APPLY or BW_COMPOSE
      if (BW_COMPOSE(C)) {
        return false;
      }
      break;
    default:
      return true;
    }

    return true;
  }

  private static boolean Full_noP_NF(Rule_Type A, int Aa, Rule_Type B, int Ba, Rule_Type C, int Ca) {
    if (B.equals(Rule_Type.LEX) || C.equals(Rule_Type.LEX)) {
      return false;
    }

    // NF Constraint 1 (B0 and Bn>=1) The output of
    // >B_{n>=1} (resp. <Bn>=1) cannot be primary functor
    // for >B_{n<=1} (resp. <Bn<=1).

    // NF Constraint 2 (B1 and B_{n>=1})
    // The output of >B1 (resp. <B1) cannot be primary functor
    // for >B_{n>=1} (resp. <B_{n>=1}).

    // NF Constraint 3 (Bn>1 and Bm>1) The output of >Bm (resp. <Bm)
    // cannot be secondary functor for >Bn>m (resp. <Bn>m).

    switch (A) {
    case FW_APPLY:
      // NF Constraint 1:
      // The output of forward composition (i.e. arity >= 1 )
      // cannot be the primary functor (B) for forward composition
      // with arity <= 1
      if (FW_COMPOSE(B)) {
        return false;
      }

      // NF Constraint 5 (atom and B0) The output of forward type-raising >atom
      // cannot be the functor in application >
      if (B.equals(Rule_Type.FW_TYPERAISE)) {
        return false;
      }

      break;

    case FW_COMPOSE:
    case FW_XCOMPOSE:
    case FW_2_COMPOSE:
    case FW_3_COMPOSE:
      if (FW_COMPOSE(B)) {
        // NF Constraint 1:
        // The output of forward composition (i.e. arity >= 1 )
        // cannot be the primary functor (B) for forward composition
        // with arity <= 1
        if (Aa == 1) {
          return false;
        }

        // NF Constraint 2:
        // The output of forward composition arity=1, cannot be the primary
        // functor (B) for forward composition with arity >= 1
        if (Ba == 1 && Aa >= 1) {
          return false;
        }
      }
      if (FW_COMPOSE(C)) {
        // NF Constraint 3:
        // the output of forward composition (arity=m) cannot be the secondary
        // functor (C) in forward composition (arity=n) where n > m
        if (Ca < Aa) {
          return false;
        }
      }

      if (B.equals(Rule_Type.FW_TYPERAISE)) {
        // NF Constraint 4 (atom and Bn>0)
        // The output of >atom cannot be primary input to
        // >B_{n>0} if the secondary input is the output of <B_{m>n}.
        if (BW_COMPOSE(C)) {
          if (Ca > Aa) {
            return false;
          }
        }
      }

      break;

    case BW_APPLY:
      // NF Constraint 1:
      // The output of backward composition arity >= 1
      // cannot be the primary functor (C) for backward composition
      // with arity <= 1
      if (BW_COMPOSE(C)) {
        return false;
      }

      // NF Constraint 5 (atom and B0) The output of forward type-raising >atom
      // cannot be the functor in application >
      if (C.equals(Rule_Type.BW_TYPERAISE)) {
        return false;
      }

      break;

    case BW_COMPOSE:
    case BW_XCOMPOSE:
    case BW_2_COMPOSE:
    case BW_3_COMPOSE:
      if (BW_COMPOSE(C)) {
        // NF Constraint 1:
        // The output of backward composition arity >= 1
        // cannot be the primary functor (C) for backward composition
        // with arity <= 1
        if (Aa == 1) {
          return false;
        }

        // NF Constraint 2:
        // The output of backward composition arity=1, cannot be the primary
        // functor (C) for backward composition with arity >= 1
        if (Ca == 1 && Aa >= 1) {
          return false;
        }
      }
      if (BW_COMPOSE(C)) {
        // NF Constraint 3:
        // the output of backward composition (arity=m) cannot be the secondary
        // functor (B) in backward composition (arity=n) where n > m
        if (Ba < Aa) {
          return false;
        }
      }

      if (C.equals(Rule_Type.BW_TYPERAISE)) {
        // NF Constraint 4 (atom and Bn>0)
        // The output of <atom cannot be primary input in <B_{n>0}
        // if the secondary input is the output of >B_{m>n}.
        if (FW_COMPOSE(B)) {
          if (Ba > Aa) {
            return false;
          }
        }
      }
      break;
    case BW_CONJOIN:
      // NEW:: NOT IN PAPER:  BW_CONJ can't follow BW_CONJ   X , X , X
      // Creates ambiguity:   X  X[conj]  X[conj]
      if (B.equals(Rule_Type.BW_CONJOIN)) {
        return false;
      }
      break;
    default:
      return true;
    }

    return true;
  }

  private static boolean Full_NF(Rule_Type A, int Aa, Rule_Type B, int Ba, Rule_Type C, int Ca) {
    if (B.equals(Rule_Type.LEX) || C.equals(Rule_Type.LEX)) {
      return false;
    }

    switch (A) {
    case FW_PUNCT:          // NOTE:  Not in implementation of the paper
      if (B.equals(Rule_Type.FW_TYPERAISE) || C.equals(Rule_Type.BW_TYPERAISE)) {
        return false;
      }
      break;
    case BW_PUNCT:
      // Must form constituent first
      if (B.equals(Rule_Type.FW_TYPERAISE) || C.equals(Rule_Type.BW_TYPERAISE)) {
        return false;
      }
      // A BW_PUNCT --> B not FW_PUNCT
      if (B.equals(Rule_Type.FW_PUNCT)) {
        return false;
      }
    }
    return Full_noP_NF(A, Aa, B, Ba, C, Ca);
  }

  private static boolean FW_COMPOSE(Rule_Type T){
    return T.equals(Rule_Type.FW_COMPOSE)
        || T.equals(Rule_Type.FW_XCOMPOSE)
        || T.equals(Rule_Type.FW_2_COMPOSE)
        || T.equals(Rule_Type.FW_3_COMPOSE);
  }
  private static boolean BW_COMPOSE(Rule_Type T){
    return T.equals(Rule_Type.BW_COMPOSE)
        || T.equals(Rule_Type.BW_XCOMPOSE)
        || T.equals(Rule_Type.BW_2_COMPOSE)
        || T.equals(Rule_Type.BW_3_COMPOSE);
  }
}

