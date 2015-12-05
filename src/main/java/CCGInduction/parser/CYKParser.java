package CCGInduction.parser;

import CCGInduction.Configuration;
import CCGInduction.experiments.Action;
import CCGInduction.grammar.*;
import CCGInduction.models.Model;
import CCGInduction.utils.IntPair;

import java.util.HashMap;

/**
 * @author bisk1
 * Basic implementation of CYK which requires that Chart types specify their
 * own methods for filling lexical cells
 * @param <G> Grammar type
 */
public abstract class CYKParser<G extends Grammar> implements ParserInterface<G> {

  /**
   * Specify if chart is being used during training or testing
   */
  final boolean test;

  Action parse_action = null;
  /**
   * @param testing
   *    Global charts collection
   */
  CYKParser(boolean testing) {
    this.test = testing;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void parse(Model<G> model, Chart<G> chart) {
    chart.chart = new Cell[chart.sentence.length()][chart.sentence.length()];
    for (int s = 0; s < chart.getLength(); s++) {
      for (int i = 0; i < chart.getLength() - s; i++) {
        if (s == 0) {
          lexicalCell(model, i, chart);
        } else {
          binaryCell(model, i, i + s, chart);
        }
      }
    }
    if (this.parse_action != null && this.parse_action == Action.SupervisedTest) {
      getUnary(model, chart.chart[0][chart.chart.length-1], Rule_Type.TYPE_TOP); // Level 3 TOP
      if (chart.TOP != null)
        chart.parses += chart.TOP.parses;
    } else
      checkForSuccess(model, chart);
  }

  /**
   * Fill lexical cell of chart (i,i) with chart type specific getLex and then
   * if appropriate try to type raise the categories.
   * @param i  word index
   * @param chart  Chart to add items to
   */
  void lexicalCell(Model<G> model, int i, Chart<G> chart) {
    chart.chart[i][i] = new Cell<>(chart, i);
    chart.getLex(i, model.Test);
    getUnary(model, chart.chart[i][i], Rule_Type.TYPE_CHANGE );
    if (Configuration.typeRaising || parse_action == Action.SupervisedTest) {
      getUnary(model, chart.chart[i][i], Rule_Type.FW_TYPERAISE);
      getUnary(model, chart.chart[i][i], Rule_Type.BW_TYPERAISE);
    }
  }

  /**
   * Fills a binary cell (i,j)
   * @param i Start of span
   * @param j End of span
   * @param chart Chart to add items to
   */
  protected abstract void  binaryCell(Model<G> model, int i, int j, Chart<G> chart);

  /**
   * Determines if a chart has been/can be succesfully completed with a TOP node
   * @param chart Chart to check for success
   */
  void checkForSuccess(Model<G> model, Chart<G> chart) {
    // Empty sentence ( e.g. a sentence of just punctuation )
    if (chart.getLength() == 0) {
      chart.parses = 0;
      chart.TOP = null;
      return;
    }

    // Check if successful parse
    Cell<G> A = chart.chart[0][chart.getLength() - 1];
    if ((test && chart.sentence.length_noP() > Configuration.longestTestSentence)
        || A.isEmpty()) {
      chart.parses = 0;
      chart.TOP = null;
      return;
    }

    getUnary(model, A, Rule_Type.TYPE_TOP); // Level 3 TOP
    if (chart.TOP == null
        || (chart.TOP.children.isEmpty()
            && (chart.TOP.topK == null || chart.TOP.topK.isEmpty()))) {
      chart.parses = 0;
      chart.TOP = null;
      return;
    }
    A.addCat(chart.TOP);
    chart.parses += chart.TOP.parses;
    // Util.Println(TIMES.intValue() + "\tS: " + sentence.asTags());
  }

  /**
   * Attempts to apply rules of a given type to all categories in the cell
   * @param cell
   *    Cell to fill
   * @param type
   *    Type of rule to apply
   */
  void getUnary(Model<G> model, Cell<G> cell, Rule_Type type) {
    HashMap<ChartItem<G>, ChartItem<G>> newCats = new HashMap<>();
    for (ChartItem<G> cat : cell.values()) {
      IntPair B = new IntPair(cat.Category);
      for (Rule r : model.grammar.getRules(B)) {
        Unary u = (Unary) r;
        if (u.Type.equals(type)) {
          switch (model.grammar.unaryCheck(u.A, u.B)) {
          case Unused:
            if (test) {
              break;
            }
          case Valid:
            if (NF.unaryNF(cat.type(), u.Type)) {
              ChartItem<G> c = new ChartItem<>(u.A, u.Type, 0, cell);
              ChartItem<G> c_prev;
              if ((c_prev = cell.getCat(c)) != null) {
                c = c_prev;
              } else if ((c_prev = newCats.get(c)) != null) {
                c = c_prev;
              } else {
                newCats.put(c, c);
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
            throw new Parser.FailedParsingAssertion("Invalid option for rule: " + model.grammar.unaryCheck(u.A, u.B));
          }
        }
      }
    }
    cell.addAllCats(newCats);
  }

}
