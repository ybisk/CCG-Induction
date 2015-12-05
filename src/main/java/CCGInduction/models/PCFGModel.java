package CCGInduction.models;

import CCGInduction.Configuration;
import CCGInduction.grammar.Grammar;
import CCGInduction.grammar.Rule_Type;
import CCGInduction.learning.CondOutcomePair;
import CCGInduction.learning.Distribution;
import CCGInduction.parser.BackPointer;
import CCGInduction.parser.ChartItem;
import CCGInduction.utils.Math.Log;
import CCGInduction.grammar.Unary;
import CCGInduction.learning.CountsArray;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Basic PCFG model where children are generated as binary tuples
 * <hr>
 * <b>Distributions</b>
 * </hr>
 * <table>
 * <tr>
 * <td><b>p_Expansion</b></td>
 * <td>(B,C) | A</td>
 * </tr>
 * </table>
 * <hr>
 * <b>Generative Story</b>
 * </hr>
 * <ul style="list-style: none;">
 * <li>for node <i><b>i</b></i> in parse tree:
 * <ul style="list-style: none;">
 * <li>B,C ~ p_Expansion(z_i)</li>
 * </ul>
 * </li>
 * </ul>
 * 
 * @author bisk1
 */
public class PCFGModel extends Model<Grammar> {
  private final Distribution p_Expansion;
  private final HashMap<CondOutcomePair, Integer> cxtsMap =  new HashMap<>();
  private final ConcurrentHashMap<Long, CondOutcomePair> cxts = new ConcurrentHashMap<>();

  /**
   * Create model with grammar and configuration
   * 
   * @param grammar Grammar used to parse
   */
  public PCFGModel(Grammar grammar) {
    createFine = false;
    this.grammar = grammar;
    p_Expansion = new Distribution(this, "p_Expansion");
    Distributions.add(p_Expansion);
  }

  public PCFGModel(PCFGModel model) {
    createFine = false;
    this.grammar = model.grammar.copy();
    p_Expansion =  model.p_Expansion.copy();
    Distributions.add(p_Expansion);
    Test        = model.Test;
  }

  public Model<Grammar> copy() {
    return new PCFGModel(this);
  }

  @Override
  public void buildUnaryContext(ChartItem<Grammar> parent, BackPointer<Grammar> backPointer) {
    super.buildUnaryContext(parent, backPointer);

    Unary u = (Unary) backPointer.rule;

    double v;
    if (u.Type.equals(Rule_Type.PRODUCTION)) {
      v = Log.div(Math.log(parent.outside_parses), Math.log(parent.cell.chart.parses));
    } else {
      v = Log.div(Math.log(parent.outside_parses * backPointer.leftChild().parses),
          Math.log(parent.cell.chart.parses));
    }
    p_BC(parent, backPointer, v);
  }

  @Override
  public void buildBinaryContext(ChartItem<Grammar> parent, BackPointer<Grammar> backPointer) {
    super.buildBinaryContext(parent, backPointer);

    double v = Log.div(Math.log(parent.outside_parses * backPointer.leftChild().parses * backPointer.rightChild().parses),
        Math.log(parent.cell.chart.parses));
    p_BC(parent, backPointer, v);
  }

  @Override
  public double prob(ChartItem<Grammar> parent, BackPointer<Grammar> backpointer) {
    return p_Expansion.P(A_cond(parent), BC_out(backpointer));
  }

  @Override
  public void count(ChartItem<Grammar> parent, BackPointer<Grammar> backPointer, double countValue,
      CountsArray countsArray) {
    countsArray.add(p_Expansion, A_cond(parent), BC_out(backPointer), countValue);
  }

  private void p_BC(ChartItem<Grammar> P, BackPointer<Grammar> bp, double v) {
    if (!Test) {
      p_Expansion.addContext(A_cond(P), BC_out(bp));
      if (Configuration.uniformPrior) {
        p_Expansion.priorCounts(A_cond(P), BC_out(bp), v);
      }
    }
  }

  private static CondOutcomePair A_cond(ChartItem<Grammar> P) {
    return new CondOutcomePair(P.Category);
  }

  private int BC_out(BackPointer<Grammar> bp) {
    CondOutcomePair ia = new CondOutcomePair(bp.leftChild().Category, bp.rightChild().Category);
    Integer i;
    if ((i = cxtsMap.get(ia)) != null) {
      return i;
    }
    return BC(ia);
  }

  private synchronized int BC(CondOutcomePair bc) {
    Integer i = cxtsMap.get(bc);
    if (i != null) {
      return i;
    }
    int index = cxts.size() - 1;
    cxts.put((long)index, bc);
    cxtsMap.put(bc, index);
    return index;
  }

  @Override
  public String prettyCond(CondOutcomePair conditioningVariables, Distribution d) {
    if (d == this.p_Expansion) {
      return prettyExpansionCond(conditioningVariables);
    }
    return null;
  }

  private String prettyExpansionCond(CondOutcomePair cxt) {
    return "P:" + grammar.prettyCat(cxt.condVariable(0));
  }

  @Override
  public String prettyOutcome(long out, Distribution d) {
    CondOutcomePair ia = cxts.get(out);
    if (ia.conditioning_variables[1] == -1) {
      return grammar.prettyCat(ia.conditioning_variables[0]);
    }
    return String.format("B: %-10s C: %-10s", grammar.prettyCat(ia.conditioning_variables[0]),
        grammar.prettyCat(ia.conditioning_variables[1]));
  }

  @Override
  public CondOutcomePair backoff(CondOutcomePair cxt, Distribution d) {
    return null;
  }

}
