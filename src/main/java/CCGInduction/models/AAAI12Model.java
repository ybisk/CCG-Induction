package CCGInduction.models;

import CCGInduction.Configuration;
import CCGInduction.grammar.*;
import CCGInduction.learning.CondOutcomePair;
import CCGInduction.learning.CountsArray;
import CCGInduction.learning.Distribution;
import CCGInduction.parser.BackPointer;
import CCGInduction.parser.ChartItem;
import CCGInduction.utils.Math.Log;

/**
 * Y. Bisk and J. Hockenmaier, “Simple Robust Grammar Induction with Combinatory Categorial Grammars,”
 * in Proceedings of the Twenty-Sixth Conference on Artificial Intelligence (AAAI-12),
 * Toronto, Canada, July 2012, pp. 1643–1649.
 *
 * Y. Bisk and J. Hockenmaier, “Induction of Linguistic Structure with Combinatory Categorial Grammars,”
 * in NAACL HLT Workshop on Induction of Linguistic Structure, Montr ́eal, Canada, June 2012, pp. 90–95
 * @author bisk1
 */
public class AAAI12Model extends Model<Grammar> {
  /**
   * ----- Distributions ----- Type p_Expansion ( Lexical, { Combinators } )
   * Emission p_Emission ( Words / Tags ) Arguments p_Argument ( categories that
   * form Y: X/Y Y/Z or X -> Y) p( c | X|Z ) x p( Y | X|Z , c ) where Z \in {
   * Cats, \emptyset } where | \in { /, \ }
   */

  private final Distribution p_Expansion;
  private final Distribution p_Emission;
  private final Distribution p_Sister;
  private final Distribution p_Head;
  private static long LEX, UNARY, LEFT, RIGHT;

  /**
   * Instantiate a AAAI12 Model
   * 
   * @param g Grammar
   */
  public AAAI12Model(Grammar g) {
    this.grammar = g;
    this.p_Expansion = new Distribution(this, "p_Expansion");
    this.p_Emission = new Distribution(this, "p_Emission");
    this.p_Head = new Distribution(this, "p_Head");
    this.p_Sister = new Distribution(this, "p_Sister");
    setup();
  }

  /**
   * Copy constructor
   * @param model Model to copy
   */
  public AAAI12Model(AAAI12Model model) {
    grammar =     model.grammar.copy();
    p_Emission =  model.p_Emission.copy();
    p_Expansion = model.p_Expansion.copy();
    p_Head =      model.p_Head.copy();
    p_Sister =    model.p_Sister.copy();
    Test        = model.Test;
    setup();
  }

  private void setup() {
    this.createFine = false;
    this.Distributions.add(p_Sister);
    this.Distributions.add(p_Head);
    this.Distributions.add(p_Head);
    this.Distributions.add(p_Expansion);
    AAAI12Model.LEX = grammar.Lex("#LEX#");
    AAAI12Model.LEFT = grammar.Lex("#LEFT#");
    AAAI12Model.RIGHT = grammar.Lex("#RIGHT#");
    AAAI12Model.UNARY = grammar.Lex("#UNARY#");
  }

  @Override
  public void buildUnaryContext(ChartItem<Grammar> parent, BackPointer<Grammar> backPointer) {
    super.buildUnaryContext(parent, backPointer);
    Unary rule = (Unary) backPointer.rule;

    double value;
    if (rule.Type.equals(Rule_Type.PRODUCTION)) {
      value = Log.div(Math.log(parent.outside_parses), Math.log(parent.cell.chart.parses));
      backPointer.Type(LEX);
    } else {
      backPointer.Type(UNARY);
      value = Log.div(Math.log(parent.outside_parses * backPointer.leftChild().parses),
          Math.log(parent.cell.chart.parses));
    }
    p_exp(parent, backPointer, value);
    if (rule.Type.equals(Rule_Type.PRODUCTION)) {
      p_emit(parent, backPointer, value);
    } else {
      p_head(parent, backPointer, backPointer.leftChild(), value);
    }
  }

  @Override
  public void buildBinaryContext(ChartItem<Grammar> parent, BackPointer<Grammar> backPointer) {
    super.buildBinaryContext(parent, backPointer);
    Binary rule = (Binary) backPointer.rule;
    if ((rule.head.equals(Rule_Direction.Left) && !Rule_Type.TR(backPointer.leftChild().type()))
        || (rule.head.equals(Rule_Direction.Right) && Rule_Type.TR(backPointer.rightChild().type()))) {
      backPointer.Type(LEFT);
    } else {
      backPointer.Type(RIGHT);
    }

    double value = Log.div(Math.log(parent.outside_parses * backPointer.leftChild().parses * backPointer.rightChild().parses),
        Math.log(parent.cell.chart.parses));
    p_exp(parent, backPointer, value);
    if (rule.head.equals(Rule_Direction.Left)) {
      p_head(parent, backPointer, backPointer.leftChild(), value);
      p_sister(parent, backPointer, backPointer.leftChild(), backPointer.rightChild(), value);
    } else {
      p_head(parent, backPointer, backPointer.rightChild(), value);
      p_sister(parent, backPointer, backPointer.rightChild(), backPointer.leftChild(), value);
    }
  }

  @Override
  public double prob(ChartItem<Grammar> parent, BackPointer<Grammar> backpointer) {
    long rt = backpointer.Type();
    double val = this.p_Expansion.P(exp_cond(parent), rt);

    if (rt == LEX) {
      val = Log.mul(val, p_Emission.P(emit_cond(parent, backpointer), parent.word()));
    } else if (rt == UNARY) {
      val = Log.mul(val, p_Head.P(head_cond(parent, backpointer), backpointer.leftChild().Category));
    } else if (rt == LEFT) {
      val = Log.mul(val, p_Head.P(head_cond(parent, backpointer), backpointer.leftChild().Category),
          p_Sister.P(sis_cond(parent, backpointer, backpointer.leftChild()), backpointer.rightChild().Category));
    } else {
      val = Log.mul(val, p_Head.P(head_cond(parent, backpointer), backpointer.rightChild().Category),
          p_Sister.P(sis_cond(parent, backpointer, backpointer.rightChild()), backpointer.leftChild().Category));
    }
    return val;
  }

  @Override
  public void count(ChartItem<Grammar> parent, BackPointer<Grammar> backPointer, double countValue,
                    CountsArray countsArray) {
    long rt = backPointer.Type();
    countsArray.add(p_Expansion, exp_cond(parent), rt, countValue);

    if (rt == LEX) {
      countsArray.add(p_Emission, emit_cond(parent, backPointer), parent.word(), countValue);
    } else if (rt == UNARY) {
      countsArray.add(p_Head, head_cond(parent, backPointer), backPointer.leftChild().Category, countValue);
    } else if (rt == LEFT) {
      countsArray.add(p_Head, head_cond(parent, backPointer), backPointer.leftChild().Category, countValue);
      countsArray.add(p_Sister, sis_cond(parent, backPointer, backPointer.leftChild()), backPointer.rightChild().Category, countValue);
    } else {
      countsArray.add(p_Head, head_cond(parent, backPointer), backPointer.rightChild().Category, countValue);
      countsArray.add(p_Sister, sis_cond(parent, backPointer, backPointer.rightChild()), backPointer.leftChild().Category, countValue);
    }
  }

  private void p_exp(ChartItem<Grammar> p, BackPointer<Grammar> ca, double v) {
    if (!Test) {
      p_Expansion.addContext(exp_cond(p), ca.Type());
      if (Configuration.uniformPrior) {
        p_Expansion.priorCounts(exp_cond(p), ca.Type(), v);
      }
    }
  }

  private void p_emit(ChartItem<Grammar> p, BackPointer<Grammar> ca, double v) {
    if (!Test) {
      p_Emission.addContext(emit_cond(p, ca), p.word());
      if (Configuration.uniformPrior) {
        p_Emission.priorCounts(emit_cond(p, ca), p.word(), v);
      }
    }
  }

  private void p_head(ChartItem<Grammar> p, BackPointer<Grammar> ca, ChartItem<Grammar> h, double v) {
    if (!Test) {
      p_Head.addContext(head_cond(p, ca), h.Category);
      if (Configuration.uniformPrior) {
        p_Head.priorCounts(head_cond(p, ca), h.Category, v);
      }
    }
  }

  private void p_sister(ChartItem<Grammar> p, BackPointer<Grammar> ca, ChartItem<Grammar> h, ChartItem<Grammar> s,
                        double v) {
    if (!Test) {
      p_Sister.addContext(sis_cond(p, ca, h), s.Category);
      if (Configuration.uniformPrior) {
        p_Sister.priorCounts(sis_cond(p, ca, h), s.Category, v);
      }
    }
  }

  private static CondOutcomePair exp_cond(ChartItem<Grammar> p) {
    return new CondOutcomePair(p.Category);
  }

  private static CondOutcomePair emit_cond(ChartItem<Grammar> p, BackPointer<Grammar> ca) {
    return new CondOutcomePair(p.Category, ca.Type());
  }

  private static CondOutcomePair head_cond(ChartItem<Grammar> p, BackPointer<Grammar> ca) {
    return new CondOutcomePair(p.Category, ca.Type());
  }

  private static CondOutcomePair sis_cond(ChartItem<Grammar> p, BackPointer<Grammar> ca, ChartItem<Grammar> h) {
    return new CondOutcomePair(p.Category, ca.Type(), h.Category);
  }

  @Override
  public String prettyCond(CondOutcomePair conditioningVariables, Distribution d) {
    if (d == this.p_Expansion) {
      return prettyExpansionCond(conditioningVariables);
    }
    if (d == this.p_Emission) {
      return prettyEmissionCond(conditioningVariables);
    }
    if (d == this.p_Head) {
      return prettyHeadCond(conditioningVariables);
    }
    if (d == this.p_Sister) {
      return prettySisterCond(conditioningVariables);
    }
    return null;
  }

  private String prettyExpansionCond(CondOutcomePair cxt) {
    return "P:" + grammar.prettyCat(cxt.condVariable(0));
  }

  private String prettyEmissionCond(CondOutcomePair cxt) {
    return "P:" + grammar.prettyCat(cxt.condVariable(0))
        + " T:" + grammar.Words.get(cxt.condVariable(1));
  }

  private String prettyHeadCond(CondOutcomePair cxt) {
    return "P:" + grammar.prettyCat(cxt.condVariable(0))
        + " D:" + grammar.Words.get(cxt.condVariable(1));
  }

  private String prettySisterCond(CondOutcomePair cxt) {
    return "P:" + grammar.prettyCat(cxt.condVariable(0))
        + " D:" + grammar.Words.get(cxt.condVariable(1))
        + " H:" + grammar.prettyCat(cxt.condVariable(2));
  }

  @Override
  public String prettyOutcome(long out, Distribution d) {
    if (d == this.p_Expansion) {
      return "T:" + grammar.Words.get(out);
    }
    if (d == this.p_Emission) {
      return "w:" + grammar.prettyCat(out);
    }
    if (d == this.p_Head) {
      return "H:" + grammar.prettyCat(out);
    }
    if (d == this.p_Sister) {
      return "S:" + grammar.prettyCat(out);
    }
    return null;
  }

  @Override
  public CondOutcomePair backoff(CondOutcomePair cxt, Distribution d) {
    return null;
  }

  @Override
  public Model<Grammar> copy() {
    return new AAAI12Model(this);
  }
}
