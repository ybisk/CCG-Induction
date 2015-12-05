package CCGInduction.grammar;

import CCGInduction.Configuration;
import CCGInduction.ccg.Direction;
import CCGInduction.ccg.InducedCAT;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Class for creating new CCG categories and encodes all the restrictions
 */
class GrammarInductionUtils {

  /**
   * Set of allowed argument categories
   * @param Base Return category
   * @param Arg  Argument category
   * @return Whether we allow Arg given Base as return type.
   */
  public static boolean OKArgument(InducedCAT Base, InducedCAT Arg) {
    // Arguments cannot have [conj]
    if (Arg.has_conj) {
      return false;
    }
    // Result's arity isn't already capped
    if (Base.arity >= Configuration.maxArity) {
      return false;
    }
    // Disallow categories of the form X|X where X is a modifier or atomic
    if ((Base.modifier || InducedCAT.atomic(Base)) && Base.equals(Arg)) {
      return false;
    }
    // N is not allowed to take arguments
    if (InducedCAT.N(Base)) {
      return false;
    }
    // conj is not allowed to be an argument
    if (InducedCAT.CONJ(Arg)) {
      return false;
    }
    // Atomic categories take atomic args
    if (InducedCAT.atomic(Base) && !InducedCAT.atomic(Arg)) {
      return false;
    }
    // Can't take complex arg if you have ONE
    if (!InducedCAT.atomic(Arg) && !InducedCAT.atomic(Base.Arg)) {
      return false;
    }

    // max modifier arity applies to categories that contain modifiers.
    if (containsModifier(Base) && Base.arity == Configuration.maxModArity) {
      return false;
    }

    // Creation of Control verbs and modals.
    // X|X must be a modifier unless (S|N)|(S|N)
    // Can only add an argument if base's arg is atomic
    // TODO: Necessary?
    if (Arg.equals(Base) && Base.Arg != null && !InducedCAT.atomic(Base.Arg)) {
      return false;
    }

    // Cap on sentential arguments to 1
    //if (sentential(Arg) && sententialArgument(Base))
    //  return false;

    // Only allow complex arguments if flag is set
    if (!Configuration.complexArgs) {
      return InducedCAT.atomic(Arg);
    }
    // If argument is atomic
    if (InducedCAT.atomic(Arg)) {
      return true;
    }
    // Cannot take modifiers as arguments
    if (Arg.modifier) {
      return false;
    }
    // If argument is not S|N
    return !Arg.D.equals(Direction.None) && InducedCAT.N(Arg.Arg) && InducedCAT.S(Arg.Res);
  }

  static boolean sentential(InducedCAT cat) {
    return (InducedCAT.S(cat) || (cat.Res != null && sentential(cat.Res)));
  }
  static boolean sententialArgument(InducedCAT cat) {
    return !cat.modifier && (cat.Arg != null && sentential(cat.Arg) || (cat.Res != null && sententialArgument(cat.Res)));
  }


  /**
   * Creates categories where leftPOStag can modify rightCategory
   *
   * @param leftPOStagCategories Lexical categories attached to POS tag on left
   * @param rightCategory Right POS tag's category (which we attempt to modify)
   */
  static void Right_Modify(
      ConcurrentHashMap<InducedCAT, Boolean> leftPOStagCategories, InducedCAT rightCategory) {
    if (rightCategory.arity < Configuration.maxModArity
        // x/x x \in {atomic,y|y}
        && (InducedCAT.atomic(rightCategory) || (rightCategory.Arg != null && rightCategory.Arg.equals(rightCategory.Res)))) {
      InducedCAT c = rightCategory.forward(rightCategory);
      c.modifier = true;
      leftPOStagCategories.put(c, true); // j / j
    }
  }

  /**
   * Creates categories where rightPOStag can modify leftCategory
   *
   * @param rightPOStagCategories Lexical categories attached to POS tag on right
   * @param leftCategory Left POS tag's category (which we attempt to modify)
   */
  static void Left_Modify(
      ConcurrentHashMap<InducedCAT, Boolean> rightPOStagCategories, InducedCAT leftCategory) {
    if (leftCategory.arity < Configuration.maxModArity
        // x\x x \in {atomic, y|y}
        && (InducedCAT.atomic(leftCategory) || (leftCategory.Arg != null && leftCategory.Arg.equals(leftCategory.Res)))
        && (!leftCategory.D.equals(Direction.None) || !InducedCAT.CONJ(leftCategory))) {
      InducedCAT c = leftCategory.backward(leftCategory);
      c.modifier = true;
      rightPOStagCategories.put(c, true); // i \ i
    }
  }

  /**
   * Create categories where leftCategory takes rightCategory as an argument
   *
   * @param leftPOStagCategories Lexical categories attached to POS tag on left
   * @param leftCategory   Result category
   * @param rightCategory  Argument category
   */
  static void Right_Arg(
      ConcurrentHashMap<InducedCAT, Boolean> leftPOStagCategories, InducedCAT leftCategory, InducedCAT rightCategory) {

    if (OKArgument(leftCategory, rightCategory)) {
      leftPOStagCategories.put(leftCategory.forward(rightCategory), true); // i / j
    }
  }

  /**
   * Create categories where rightCategory takes leftCategory as an argument
   *
   * @param rightPOStagCategories Lexical categories attached to POS tag on right
   * @param leftCategory       Argument category
   * @param rightCategory      Result category
   */
  static void Left_Arg(
      ConcurrentHashMap<InducedCAT, Boolean> rightPOStagCategories, InducedCAT leftCategory, InducedCAT rightCategory) {
    if (OKArgument(rightCategory, leftCategory)) {

      // You can add \Y to any modifier unless the modifier is Y/Y
      // This eliminates (or allows)  (N/N)\N POS   with   (N\N)/N  IN
      if(rightCategory.modifier && rightCategory.D.equals(Direction.FW) && leftCategory.equals(rightCategory.Arg)) {
        if (!Configuration.allowXbXbX) {
          return;
        } else {
          rightPOStagCategories.put(rightCategory.backward(leftCategory), true); // j \ i
          return;
        }
      }

      // Cannot add \Y to X/Z  where Y and Z are atomic
      // This is to eliminate (S/N)\N and (S\N)/N redundancy
      if (InducedCAT.atomic(leftCategory) &&
          !rightCategory.modifier && InducedCAT.atomic(rightCategory.Arg) && rightCategory.D.equals(Direction.FW)) {
        return;
      }
      rightPOStagCategories.put(rightCategory.backward(leftCategory), true); // j \ i
    }
  }


  private static boolean containsModifier(InducedCAT base) {
    if (base.modifier) {
      return true;
    }
    if (InducedCAT.atomic(base)) {
      return false;
    }
    return containsModifier(base.Res) || containsModifier(base.Arg);
  }

  /**
   * Checks argument and modifier arity constraints
   * @param base Category to test
   * @return If valid
   */
  public static boolean validCategory(InducedCAT base) {
    if (containsModifier(base) && base.arity > Configuration.maxModArity) {
      return false;
    }
    return !(!containsModifier(base) && base.arity > Configuration.maxArity);
  }
}
