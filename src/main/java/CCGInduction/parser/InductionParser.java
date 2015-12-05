package CCGInduction.parser;

import CCGInduction.Configuration;
import CCGInduction.ccg.CCGCategoryUtilities;
import CCGInduction.ccg.InducedCAT;
import CCGInduction.experiments.Action;
import CCGInduction.grammar.*;
import CCGInduction.utils.Math.Log;
import CCGInduction.ccg.Direction;
import CCGInduction.models.Model;
import CCGInduction.utils.IntPair;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Creates a parser for use with Induction.  The amount of power and whether
 * themodel.grammar is fixed are passed as arguments to the constructor.
 * @author bisk1
 */
public class InductionParser<G extends Grammar> extends CYKParser<G> {

  /**
   * Creates a parser for use with Induction.  The amount of power and whether
   * themodel.grammar is fixed are passed as arguments to the constructor.
   * @param power Combinator strength
   */
  public InductionParser(Action power) {
    super(power == Action.Test || power == Action.SupervisedTest);
    this.parse_action = power;
  }

  @Override
  public void parse(Model<G> model, Chart<G> chart) {
    if (chart.sentence.JSON != null && chart.sentence.JSON.synPars != null) {
      chart.fromAUTO(this);
    } else if (this.parse_action.equals(Action.Supervised)) {
      chart.fromAUTO(this);
      // FIXME:  A quick hack to give everyone a parse count
      for (Cell[] arr : chart.chart) {
        for (Cell<?> cell : arr) {
          if (cell != null && cell.values() != null)
            for (ChartItem<?> ci : cell.values())
              ci.parses = 1;
        }
      }
    } else {
      super.parse(model, chart);
    }
  }

  @Override
  protected void binaryCell(Model<G> model, int i, int j, Chart<G> chart) {
    chart.chart[i][j] = new Cell<>(chart, i, j);
    Cell<G> A = chart.chart[i][j];
    if (chart.punctuationBracketing(i, j) && (!Configuration.hardBracketConstraints || !chart.crossingBrackets(i,j))) {
      for (int k = i; k <= j - 1; k++) {
        Cell<G> B = chart.chart[i][k];
        Cell<G> C = chart.chart[k + 1][j];
        // For every left hand side category
        for (long b_cat : B.cats()) {
          C.cats().stream()
              .filter(c_cat -> canCombine(model, b_cat, c_cat, parse_action))
              .forEach(c_cat -> model.grammar.combine(A, b_cat, B.values(b_cat), c_cat, C.values(c_cat), test, parse_action));
        }
      }
      getUnary(model, A, Rule_Type.TYPE_CHANGE);
      if (!Configuration.lexTROnly && Configuration.typeRaising || parse_action == Action.SupervisedTest) {
        getUnary(model, A, Rule_Type.FW_TYPERAISE);
        getUnary(model, A, Rule_Type.BW_TYPERAISE);
      }
    }
  }

  @Override
  protected void checkForSuccess(Model<G> model, Chart<G> chart) {
    // Empty sentence ( e.g. a sentence of just punctuation )
    if (chart.getLength() == 0) {
      chart.parses = 0;
      chart.TOP = null;
      return;
    }

    // Check if successful parse
    Cell<G> A = chart.chart[0][chart.getLength() - 1];
    if ((test && chart.sentence.length_noP() > Configuration.longestTestSentence) || A.isEmpty()) {
      chart.parses = 0;
      chart.TOP = null;
      return;
    }

    Grammar iG = model.grammar;
    chart.TOP = new ChartItem<>(iG.TOP, Rule_Type.TYPE_TOP, 0, A);
    chart.TOP.alphaOVERRIDE(Log.ZERO);

    boolean S = false;
    boolean Q = false;
    // Check what types of resultant categories we were able to get
    // FIXME: This doesn't handle complex TOP
    for (ChartItem<G> ci : A.values()) {
      InducedCAT result = ci.iCAT != null ? ci.iCAT : model.grammar.Categories.get(ci.Category);
      // If we have and S or S|
      if (InducedCAT.S(result)
          || (Configuration.complexTOP && !result.D.equals(Direction.None)
          && InducedCAT.S(result.Res) && InducedCAT.N(result.Arg)))
        S = true;
      if (InducedCAT.Q(result))
        Q = true;
    }

    boolean question = false; // = chart.sentence.isQuestion();
    boolean statement = chart.sentence.isStatement();

    for (ChartItem<G> ci : A.values()) {
      // if we carried a category with us, grab it, otherwise look it up.
      InducedCAT result = ci.iCAT != null ? ci.iCAT : model.grammar.Categories.get(ci.Category);

      boolean valid = false;

      // If Question and we have a Q
      if (question && Q && InducedCAT.Q(result))
        valid = true;

      // If Statement and we have an S or S|N
      if (statement && S && (InducedCAT.S(result)
          || (Configuration.complexTOP && !result.D.equals(Direction.None)
              && InducedCAT.S(result.Res) && InducedCAT.N(result.Arg))))
        valid = true;

      // No S or Q but something else
      if (!S && !Q && (Configuration.complexTOP || result.atom != null))
        valid = true;

      if (valid) {
        Unary u = iG.getRule(iG.TOP, ci.Category, Rule_Type.TYPE_TOP,false);
        if (u != null && (!test || iG.requiredRules.containsKey(u))) {
          chart.TOP.addChild(u, ci, null);
          chart.TOP.parses += ci.parses;
        }
      }
    }

    if (chart.TOP == null || (chart.TOP.children.isEmpty() && (chart.TOP.topK == null || chart.TOP.topK.isEmpty()))) {
      chart.TOP = null;
      chart.parses = 0;
      return;
    }
    A.addCat(chart.TOP);
    chart.parses += chart.TOP.parses;
  }

  @Override
  void getUnary(Model<G> model, Cell<G> cell, Rule_Type type) {
    HashMap<ChartItem<G>, ChartItem<G>> newCats = new HashMap<>();
    for (ChartItem<G> cat : cell.values()) {
      IntPair B = new IntPair(cat.Category);
      for (Rule r :model.grammar.getRules(B)) {
        Unary u = (Unary) r;
        if (u.Type.equals(type) ) {
          switch (model.grammar.unaryCheck(u.A, u.B)) {
          case Unused:
            if (test) {
              break;
            }
          case Valid:
            if (NF.unaryNF(cat.type(), u.Type)) {
              ChartItem<G> c = new ChartItem<>(u.A, u.Type, 0, cell);
              if(!test)
                c.iCAT = model.grammar.Categories.get(u.A);
              ChartItem<G> c_prev;
              if ((c_prev = cell.getCat(c)) != null) {
                c = c_prev;
              } else if ((c_prev = newCats.get(c)) != null) {
                c = c_prev;
              } else {
                newCats.put(c, c);
              }

              if (c.iCAT == null && !test) {
                throw new Parser.FailedParsingAssertion("Null iCAT:\t" + model.grammar.prettyCat(c.Category));
              }

              if (c.addChild(u, cat, null)) {
                c.parses += cat.parses;
              }
              if (cell.X == 0
                  && cell.Y == (cell.chart.getLength() - 1)
                  && type.equals(Rule_Type.TYPE_TOP)
                  && (cell.chart.TOP == null)) {
                cell.chart.TOP = c;
              }
            }
            break;
          case Invalid:
            break;
          default:
            throw new Parser.FailedParsingAssertion("Invalid outcome: " + model.grammar.unaryCheck(u.A, u.B));
          }
        }
      }
    }
    cell.addAllCats(newCats);
  }

  /**
   * Check if two categories can combine according to CCG
   * 
   * @param leftCategory Left category
   * @param rightCategory Right category
   * @param action  Parse power
   * @return If the categories can combine
   */
  private boolean canCombine(Model<G> model, long leftCategory, long rightCategory, Action action) {
    switch (model.grammar.combine(leftCategory, rightCategory)) {
    case Unused:
      return (action != Action.Test && action != Action.SupervisedTest); // T: if not testing
    case Valid:
      return true;
    case Invalid:
      return false;
    case Unknown:
      if (action == Action.Test || action == Action.SupervisedTest) {
        return false;
      }
      return createInductionRule(model, leftCategory, rightCategory, action) != null;
    default:
      return false;
    }
  }

  /**
   * Defines a set of rules and 'valid' value for a given pair of categories.
   * 
   * @param leftCategory Left category
   * @param rightCategory Right category
   * @param canCombine Can these categories combine?
   * @param rule Grammatical rule used to combine given categories
   */
  private void combine(Model<G> model, long leftCategory, long rightCategory, valid canCombine, Rule rule) {
    IntPair ip = new IntPair(leftCategory, rightCategory);
    if (canCombine == null) {
      throw new Parser.FailedParsingAssertion("Failed to find way to combine");
    }
    model.grammar.Rules.putIfAbsent(ip, new ConcurrentHashMap<>());

    model.grammar.Rules.get(ip).put(rule,true);
    model.grammar.combinationCheck.put(ip, canCombine);

  }

  private void invalidCombination(Model<G> model, long b_cat, long c_cat) {
    model.grammar.combinationCheck.put(new IntPair(b_cat,c_cat), valid.Invalid);
  }


  /**
   * Returns an induction rule if the outcome matches the parent.  Otherwise it throws an exception
   * @param parent    Required outcome
   * @param left      left category
   * @param right     right category
   * @return  A binarymodel.grammar rule
   */
  public Binary createSupervisedRule(Model<G> model, InducedCAT parent, InducedCAT left, InducedCAT right) {
    Binary rule = createRule(model, Action.Supervised, parent, left, right);
    model.grammar.NTRecursively(parent);
    if (rule == null) {
      throw new Parser.FailedParsingAssertion("Null Rule:\t" + parent.toString() + "\t"
          + left.toString() + "\t" + right.toString());
    }
    InducedCAT Induced = model.grammar.Categories.get(rule.A);
    InducedCAT LeftC = model.grammar.Categories.get(rule.B);
    InducedCAT RightC = model.grammar.Categories.get(rule.C);
    if (Induced.equals(parent) && left.equals(LeftC) && right.equals(RightC))
      return rule;
    throw new Parser.FailedParsingAssertion("Parent categories do not match: "
          + Induced.toString() + "\t" + parent.toString() + "\n" + rule.toString(model.grammar));
  }


  /**
   * Returns a new unary rule: Type-Raise or Type-Change
   * @param parent Parent category
   * @param child  Child category
   * @return Unary rule
   */
  public Unary createInductionRule(Model<G> model, InducedCAT parent, InducedCAT child) {
    // Check possible rule types
    Unary u;
    // Type-Raising:   T/(T\X) -->  X
    if(parent.Res != null && parent.Arg != null && parent.Arg.Res != null
       && parent.Res.equals(parent.Arg.Res) && parent.Arg.Arg.equals(child)) {
      if (parent.D.equals(Direction.FW))
        u = model.grammar.createSupervisedRule(model.grammar.NT(parent), model.grammar.NT(child), Rule_Type.FW_TYPERAISE);
      else
        u = model.grammar.createSupervisedRule(model.grammar.NT(parent), model.grammar.NT(child), Rule_Type.BW_TYPERAISE);
    } else if (parent.atom != null && InducedCAT.TOP.equals(parent.atom)) {
      u = model.grammar.createSupervisedRule(model.grammar.NT(parent), model.grammar.NT(child), Rule_Type.TYPE_TOP);
    } else {
      u = model.grammar.createSupervisedRule(model.grammar.NT(parent), model.grammar.NT(child), Rule_Type.TYPE_CHANGE);
    }
    return u;
  }


  /**
   * Create rules for combining B C, if possible
   *
   * @param leftCategory Left category
   * @param rightCategory Right category
   * @param action Parse power
   */
  private Binary createInductionRule(Model<G> model, long leftCategory, long rightCategory, Action action) {
    InducedCAT b = model.grammar.Categories.get(leftCategory);
    InducedCAT c = model.grammar.Categories.get(rightCategory);
    return createRule(model,action, b, c);
  }

  private Binary createRule(Model<G> model, Action action, InducedCAT... categories) {
    InducedCAT parent = null;
    if (categories.length == 3)
      parent = categories[0];
    InducedCAT left = categories[categories.length-2];
    InducedCAT right = categories[categories.length-1];
    Binary rule;

    if (action != Action.Supervised) {
      //    // Disallow [punct,conj]
      if (left.D.equals(Direction.None) && InducedCAT.PUNC(left.atom) && InducedCAT.CONJ(right)) {
        return null;
      }
      if (left.D.equals(Direction.None) && InducedCAT.CONJ(left) && right.atom != null && InducedCAT.PUNC(right.atom)) {
        return null;
      }
      // B should never be a FW_CONJOIN  (X[conj] must be on the right)
      if (left.has_conj) {
        return null;
      }
    }

    rule = FW_CONJOIN (model, action, parent, left, right);  // conj  X       --> X[conj]
    if (rule != null) return rule;
    rule = BW_CONJOIN (model, action, parent, left, right);  // X     X[conj] --> X
    if (rule != null) return rule;
    rule = FW_PUNCT   (model, action, parent, left, right);  // Punc  R     --> P
    if (rule != null) return rule;
    rule = BW_PUNCT   (model, action, parent, left, right);  // L     Punc  --> P
    if (rule != null) return rule;
    rule = FW_APPLY   (model, action, parent, left, right);  // L/R   R     --> P
    if (rule != null) return rule;
    rule = BW_APPLY   (model, action, parent, left, right);  // L     L\R   --> P
    if (rule != null) return rule;
    rule = FW_COMPOSE (model, action, parent, left, right);  // X/Y   Y/Z   --> X/Z
    if (rule != null) return rule;
    rule = FW_XCOMPOSE(model, action, parent, left, right);  // X/Y   Y\Z   --> X\Z
    if (rule != null) return rule;
    rule = BW_COMPOSE (model, action, parent, left, right);  // Y\Z   X\Y   --> X\Z
    if (rule != null) return rule;
    rule = BW_XCOMPOSE(model, action, parent, left, right);  // Y/Z   X\Y   --> X/Z
    if (rule != null) return rule;
    rule = GEN_COMPOSE(model, action, parent, left, right);  // X/Y   (Y|Z)|..|Z'
    if (rule != null) return rule;


    // If this is an unhandled rule type and one of the children is a conj
    // Treat as a type-changing rule
    if (action == Action.Supervised) {
      rule = SUBSTITUTION(model, parent, left, right);
      if (rule != null) return rule;
      rule = TYPE_CHANGE(model, parent, left, right);
      if (rule != null) return rule;
    }

    // We can't handle this pair
    invalidCombination(model, model.grammar.NT(left), model.grammar.NT(right));
    return null;
  }

  /**
   * We want cases where we are joining a conjunction type [conj, comma, ...] with a category X
   * to return an unlike category Y
   * @return New rule if possible
   */
  private Binary TYPE_CHANGE(Model<G> model, InducedCAT parent, InducedCAT left, InducedCAT right) {
    Binary rule;
    if (InducedCAT.CONJ(left)
        && (!CCGCategoryUtilities.softEquality(parent, right) || (!parent.hasFeat() && right.hasFeat()))) {
      rule = model.grammar.createRule(parent, left, right, Rule_Type.FW_TYPECHANGE, 0);
    } else if (InducedCAT.CONJ(right)   // S/S --> S[dcl]/S[dcl]  ,     X --> Y ,
        && (!CCGCategoryUtilities.softEquality(parent, left) || (!parent.hasFeat() && left.hasFeat()))) {
      rule = model.grammar.createRule(parent, left, right, Rule_Type.BW_TYPECHANGE, 0);
    } else {
      if (InducedCAT.CONJ(left))
        rule = model.grammar.createRule(parent, left, right, Rule_Type.FW_TYPECHANGE, 0);
      else if(InducedCAT.CONJ(right))
        rule = model.grammar.createRule(parent, left, right, Rule_Type.BW_TYPECHANGE, 0);
      // FIXME: The following is a catch-all hack
      else
        rule = model.grammar.createRule(parent, left, right, Rule_Type.FW_TYPECHANGE, 0);
    }
    if (rule != null)
      combine(model, rule.B, rule.C, valid.Unused, rule);
    return rule;
  }

  private Binary SUBSTITUTION(Model<G> model, InducedCAT parent, InducedCAT left, InducedCAT right) {
    // Y|Z   (X\Y)|Z  --> X|Z       TODO:  Should be vertical on Z
    if (left.Res != null && right.Res != null && right.Res.Arg != null
        && CCGCategoryUtilities.softEquality(left.Res, right.Res.Arg)
        && CCGCategoryUtilities.softEquality(parent, right.Res.Res.forward(left.Arg))) {
      Binary rule = model.grammar.createRule(parent, left, right, Rule_Type.FW_SUBSTITUTION, 1);
      combine(model, rule.B, rule.C, valid.Unused, rule);
      return rule;
    }
    // TODO:  BW_SUBSTITUTION (X\Y)|Z Y|Z   --> X|Z
    return null;
  }

  private Binary FW_CONJOIN(Model<G> model, Action action, InducedCAT parent, InducedCAT left, InducedCAT right) {
    // conj   conj\conj    is not a conjoin
    if ((left.Arg != null && InducedCAT.CONJ(left.Arg)) || (right.Arg != null && InducedCAT.CONJ(right.Arg)))
      return null;
    // Allow for: conj X[conj]    as in     X , and X
    if ((right.has_conj && InducedCAT.CONJ(left)) || (InducedCAT.CONJ(left) && !InducedCAT.CONJ(right))
        || (action == Action.Supervised && parent.has_conj)) {
      InducedCAT newC = right.copy();
      newC.has_conj = true;

      // Sometimes... we decide to make a possible conjunction punctuation
      if (action == Action.Supervised && !parent.has_conj)
        return null;
      if (action == Action.Supervised && !CCGCategoryUtilities.softEqualityNoConj(parent, right))
        return null;
      if (action == Action.Supervised)
        newC = parent;

      Binary rule = model.grammar.createRule(newC, left, right, Rule_Type.FW_CONJOIN, 0);
      combine(model, rule.B, rule.C, valid.Unused, rule);
      return rule;
    }
    return null;
  }

  /**
   * X  -->   X   X[conj]
   * @return  Rule if possible
   */
  private Binary BW_CONJOIN(Model<G> model, Action action, InducedCAT parent, InducedCAT left, InducedCAT right) {
    if (right.has_conj && right.equalsWithoutConj(left)) {
      InducedCAT newC = left;
      if (action == Action.Supervised)
        newC = parent;
      Binary rule = model.grammar.createRule(newC, left, right, Rule_Type.BW_CONJOIN, 0);
      combine(model, rule.B, rule.C, valid.Unused, rule);
      return rule;
    }
    return null;
  }

  private Binary FW_PUNCT(Model<G> model, Action action, InducedCAT parent, InducedCAT left, InducedCAT right) {
    // X -> Punc X FW_PUNCT
    if (InducedCAT.PUNC(left.atom)) {
      // If the parent and child don't match
      // or we go from a category with features to one without, use type-changing
      if (action == Action.Supervised &&
          (!CCGCategoryUtilities.softEqualityNoConj(parent,right) || (!parent.hasFeat() && right.hasFeat())))
        return null;
      Binary rule;
      if (action == Action.Supervised)    // N --> , N[conj]   this type of bull-shit
        rule = model.grammar.createRule(parent, left, right, Rule_Type.FW_PUNCT, 0);
      else {
        if (right.has_conj)
          return null;
        rule = model.grammar.createRule(right, left, right, Rule_Type.FW_PUNCT, 0);
      }
      combine(model, rule.B, rule.C, valid.Unused, rule);
      return rule;
    }
    return null;
  }

  private Binary BW_PUNCT(Model<G> model, Action action, InducedCAT parent, InducedCAT left, InducedCAT right) {
    // X -> X Punc BW_PUNCT
    if (InducedCAT.PUNC(right.atom)){
      if (action == Action.Supervised && !parent.equals(left))
        return null;
      InducedCAT newC = left;
      if (action == Action.Supervised)
        newC = parent;
      Binary rule = model.grammar.createRule(newC, left, right, Rule_Type.BW_PUNCT, 0);
      combine(model, rule.B, rule.C, valid.Unused, rule);
      return rule;
    }
    return null;
  }

  /**
   * Attempt to perform a FW_APPLY between left and right, with the result optionally checked against parent
   * X/Y  Y  --> X
   *
   * @return New rule or null
   */
  private Binary FW_APPLY(Model<G> model, Action action, InducedCAT parent, InducedCAT left, InducedCAT right) {
    if (action == Action.Supervised) {
      if (left.D.equals(Direction.FW)
          && CCGCategoryUtilities.softEquality(left.Arg, right)
          && CCGCategoryUtilities.softEquality(parent, left.Res)) {
        Binary rule = model.grammar.createRule(parent, left, right, Rule_Type.FW_APPLY, 0);
        combine(model, rule.B, rule.C, valid.Valid, rule);
        return rule;
      }
    } else if (!left.has_conj && left.D.equals(Direction.FW) && left.Arg.equals(right)) {
      // Parent is left.Result
      Binary rule = model.grammar.createRule(left.Res.copy(), left, right, Rule_Type.FW_APPLY, 0);
      combine(model, rule.B, rule.C, valid.Unused, rule);
      return rule;
    }
    return null;
  }

  /**
   * Attempt to perform a BW_APPLY between left and right, with the result optionally checked against parent
   * Y  X\Y  -->  X
   *
   * @return New rule or null
   */
  private Binary BW_APPLY(Model<G> model, Action action, InducedCAT parent, InducedCAT left, InducedCAT right) {
    if (action == Action.Supervised) {
      if (right.D.equals(Direction.BW)
          && CCGCategoryUtilities.softEquality(right.Arg, left)
          && CCGCategoryUtilities.softEquality(parent, right.Res)) {
        Binary rule = model.grammar.createRule(parent, left, right, Rule_Type.BW_APPLY, 0);
        combine(model, rule.B, rule.C, valid.Valid, rule);
        return rule;
      }
    } else if (right.D.equals(Direction.BW) && right.Arg.equals(left)) {
      if (left.has_conj || right.has_conj)
        return null;
      // Parent is right.Result
      Binary rule = model.grammar.createRule(right.Res.copy(), left, right, Rule_Type.BW_APPLY, 0);
      combine(model, rule.B, rule.C, valid.Unused, rule);
      return rule;
    }
    return null;
  }

  /**
   * Attempt to perform a FW_COMPOSE between left and right, with the result optionally checked against parent
   * X/Y  Y/Z  -->  X/Z
   *
   * @return New rule or null
   */
  private Binary FW_COMPOSE(Model<G> model, Action action, InducedCAT parent, InducedCAT left, InducedCAT right) {
    if (action == Action.Supervised) {
      if (left.D.equals(Direction.FW) && right.D.equals(Direction.FW)
          && CCGCategoryUtilities.softEquality(left.Arg, right.Res)
          && CCGCategoryUtilities.softEquality(parent, left.Res.forward(right.Arg))) {
        Binary rule = model.grammar.createRule(parent, left, right, Rule_Type.FW_COMPOSE, 1);
        combine(model, rule.B, rule.C, valid.Valid, rule);
        return rule;
      }
    } else {
      if (left.has_conj || right.has_conj)
        return null;
      switch (action) {
        case B0:
          return null;
        default:  // B1, B2
          break;
        case B1Mod:
        case B2Mod:
          // Primary functor must be a modifier and cannot compose into a modifier
          if (!left.modifier || right.modifier) {
            return null;
          }
          break;
      }
      if (left.D.equals(Direction.FW) && right.D.equals(Direction.FW) && left.Arg.equals(right.Res)) {
        // Parent is left.Res / right.Arg
        Binary rule = model.grammar.createRule(left.Res.forward(right.Arg), left, right, Rule_Type.FW_COMPOSE, 1);
        combine(model, rule.B, rule.C, valid.Unused, rule);
        return rule;
      }
    }
    return null;
  }

  /**
   * Attempt to perform a FW_XCOMPOSE between left and right, with the result optionally checked against parent
   * X/Y  Y/Z  -->  X/Z
   *
   * @return New rule or null
   */
  private Binary FW_XCOMPOSE(Model<G> model, Action action, InducedCAT parent, InducedCAT left, InducedCAT right) {
    if (action == Action.Supervised) {
      if (left.D.equals(Direction.FW) && right.D.equals(Direction.BW)
          && CCGCategoryUtilities.softEquality(left.Arg, right.Res)
          && CCGCategoryUtilities.softEquality(parent, left.Res.backward(right.Arg))) {
        Binary rule = model.grammar.createRule(parent, left, right, Rule_Type.FW_XCOMPOSE, 1);
        combine(model, rule.B, rule.C, valid.Valid, rule);
        return rule;
      }
    } else {
      if (left.has_conj || right.has_conj)
        return null;
      switch (action) {
        case B0:
          return null;
        default:  // B1, B2
          break;
        case B1Mod:
        case B2Mod:
          // Primary functor must be a modifier and cannot compose into a modifier
          if (!left.modifier || right.modifier) {
            return null;
          }
          break;
      }
      // ---- Special Case ----
      // / \ B_arg = B_res = C_arg = C_res B_res \ C_arg && C_res / B_arg
      if (left.D.equals(Direction.FW)
          && right.D.equals(Direction.BW) && left.Arg.equals(left.Res)
          && right.Arg.equals(right.Res) && left.Arg.equals(right.Arg)) {
        // We are not handling this case
        return null;
      }
      // 1) If modifier, ok else, cannot x-compose into modifier
      if (left.D.equals(Direction.FW) && right.D.equals(Direction.BW)
          && left.Arg.equals(right.Res, action) && (left.modifier || !right.modifier)) {
        // Parent is left.Res \ right.Arg
        Binary rule = model.grammar.createRule(left.Res.backward(right.Arg), left, right, Rule_Type.FW_XCOMPOSE, 1);
        combine(model, rule.B, rule.C, valid.Unused, rule);
        return rule;
      }
    }
    return null;
  }

  private Binary BW_COMPOSE(Model<G> model, Action action, InducedCAT parent, InducedCAT left, InducedCAT right) {
    if (action == Action.Supervised) {
      if (left.D.equals(Direction.BW) && right.D.equals(Direction.BW)
          && CCGCategoryUtilities.softEquality(left.Res, right.Arg)
          && CCGCategoryUtilities.softEquality(parent, right.Res.backward(left.Arg))) {
        Binary rule = model.grammar.createRule(parent, left, right, Rule_Type.BW_COMPOSE, 1);
        combine(model, rule.B, rule.C, valid.Valid, rule);
        return rule;
      }
    } else {
      if (left.has_conj || right.has_conj)
        return null;
      switch (action) {
        case B0:
          return null;
        default:  // B1, B2
          break;
        case B1Mod:
        case B2Mod:
          // Primary functor must be a modifier and cannot compose into a modifier
          if (left.modifier || !right.modifier) {
            return null;
          }
          break;
      }
      if (left.D.equals(Direction.BW) && right.D.equals(Direction.BW) && left.Res.equals(right.Arg)) {
        // Parent is  right.Res \ left.Arg
        Binary rule = model.grammar.createRule(right.Res.backward(left.Arg), left, right, Rule_Type.BW_COMPOSE, 1);
        combine(model, rule.B, rule.C, valid.Unused, rule);
        return rule;
      }
    }
    return null;
  }

  private Binary BW_XCOMPOSE(Model<G> model, Action action, InducedCAT parent, InducedCAT left, InducedCAT right) {
    if (action == Action.Supervised) {
      if (left.D.equals(Direction.FW) && right.D.equals(Direction.BW)
          && CCGCategoryUtilities.softEquality(left.Res, right.Arg)
          && CCGCategoryUtilities.softEquality(parent, right.Res.forward(left.Arg))) {
        Binary rule = model.grammar.createRule(parent, left, right, Rule_Type.BW_XCOMPOSE, 1);
        combine(model, rule.B, rule.C, valid.Valid, rule);
        return rule;
      }
    } else {
      if (left.has_conj || right.has_conj)
        return null;
      switch (action) {
        case B0:
          return null;
        default:  // B1, B2
          break;
        case B1Mod:
        case B2Mod:
          // Primary functor must be a modifier and cannot compose into a modifier
          if (left.modifier || !right.modifier) {
            return null;
          }
          break;
      }
      if (left.D.equals(Direction.FW) && right.D.equals(Direction.BW)
          && left.Res.equals(right.Arg) && (right.modifier || !left.modifier)) {
        // Parent is right.Res / left.Arg
        Binary rule = model.grammar.createRule(right.Res.forward(left.Arg), left, right, Rule_Type.BW_XCOMPOSE, 1);
        combine(model, rule.B, rule.C, valid.Unused, rule);
        return rule;
      }
    }
    return null;
  }

  private Binary GEN_COMPOSE(Model<G> model, Action action, InducedCAT parent, InducedCAT left, InducedCAT right) {
    if (action == Action.B0 || action == Action.B1 || action == Action.B1Mod || action == Action.B1ModTR)
      return null;

    // Attempt to compose the categories and then asses the validity of the result under our constraints
    InducedCAT fwCompose = null;
    if (left.D == Direction.FW)
      fwCompose = InducedCAT.GenComp(left, right, Rule_Type.FW_COMPOSE, 1, 100, action);
    InducedCAT bwCompose = null;
    if (right.D == Direction.BW)
      bwCompose = InducedCAT.GenComp(left, right, Rule_Type.BW_COMPOSE, 1, 100, action);
    if (fwCompose == null && bwCompose == null) {
      invalidCombination(model, model.grammar.NT(left), model.grammar.NT(right));
      return null;
    }

    InducedCAT newCat = null;
    Rule_Type type = null;
    int arity = 0;
    if (action == Action.Supervised) {
      newCat = parent;
      if (CCGCategoryUtilities.softEquality(parent, fwCompose)) {
        type = Rule_Type.FW_COMPOSE;
        arity = fwCompose.composition_arity;
      } else if (CCGCategoryUtilities.softEquality(parent, bwCompose)) {
        type = Rule_Type.BW_COMPOSE;
        arity = bwCompose.composition_arity;
      } else
        return null;
    } else {
      if (left.has_conj || right.has_conj)
        return null;
      if (fwCompose != null) {
        switch (action) {
          case B2:
            break;
          case B2Mod:
            // Primary functor must be a modifier and cannot compose into a modifier
            if (!left.modifier || right.modifier) {
              return null;
            }
            break;
          case B1ModTR:
          case B2ModTR:
          case B3Mod_B2TR_B0Else:
            if (right.modifier)
              return null;
            break;
        }
        if ((action == Action.B3Mod_B2TR_B0Else && left.modifier && fwCompose.composition_arity <= 3)
            || fwCompose.composition_arity <= 2) {
          newCat = fwCompose;
          arity = fwCompose.composition_arity;
          type = Rule_Type.FW_COMPOSE;
        }
      }
      else if (bwCompose != null) {
        switch (action) {
          case B2:
            break;
          case B2Mod:
            // Primary functor must be a modifier and cannot compose into a modifier
            if (left.modifier || !right.modifier) {
              return null;
            }
            break;
          case B1ModTR:
          case B2ModTR:
          case B3Mod_B2TR_B0Else:
            if (left.modifier)
              return null;
            break;
        }
        if ((action == Action.B3Mod_B2TR_B0Else && right.modifier && bwCompose.composition_arity <= 3)
            || bwCompose.composition_arity <= 2) {
          newCat = bwCompose;
          arity = bwCompose.composition_arity;
          type = Rule_Type.BW_COMPOSE;
        }
      }
    }
    if (newCat == null) {
      // Neither are null.... crap or... conditions not met
      invalidCombination(model, model.grammar.NT(left), model.grammar.NT(right));
      return null;
    } else {
      if (type == Rule_Type.FW_COMPOSE) {
        switch (arity) {
          case 2:
            type = Rule_Type.FW_2_COMPOSE;
            break;
          case 3:
            type = Rule_Type.FW_3_COMPOSE;
            break;
          case 4:
            type = Rule_Type.FW_4_COMPOSE;
            break;
          case 5:
            type = Rule_Type.FW_5_COMPOSE;
            break;
          default:
            throw new Parser.FailedParsingAssertion("Invalid arity: " + newCat.composition_arity + "\t"
                + left + "\t" + right);
        }
      } else if (type == Rule_Type.BW_COMPOSE) {
        switch (arity) {
          case 2:
            type = Rule_Type.BW_2_COMPOSE;
            break;
          case 3:
            type = Rule_Type.BW_3_COMPOSE;
            break;
          case 4:
            type = Rule_Type.BW_4_COMPOSE;
            break;
          case 5:
            type = Rule_Type.BW_5_COMPOSE;
            break;
          default:
            throw new Parser.FailedParsingAssertion("Invalid arity: " + newCat.composition_arity + "\t"
            + left + "\t" + right);
        }
      }

      Binary rule = model.grammar.createRule(newCat, left, right, type, newCat.composition_arity);
      if (action == Action.Supervised)
        combine(model, rule.B, rule.C, valid.Valid, rule);
      else
        combine(model, rule.B, rule.C, valid.Unused, rule);
      return rule;
    }
  }
}
