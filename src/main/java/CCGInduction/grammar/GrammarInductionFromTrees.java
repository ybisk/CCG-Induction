package CCGInduction.grammar;

import CCGInduction.ccg.Direction;
import CCGInduction.ccg.InducedCAT;
import CCGInduction.models.Model;
import CCGInduction.parser.*;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Used for inducing new categories from partially parsed charts
 * @author bisk1
 */
public class GrammarInductionFromTrees extends Parser<Grammar,InductionChart<Grammar>> {

  private final Grammar grammar;
  
  /**
   * Parses charts and then induces new categories (stored in grammar) from
   * constituents
   * @param shared_charts Charts to use for induction
   * @param global_grammar Grammar reference
   * @param parser_interface  Parser
   * @param exceptions Caught Exceptions
   */
  public GrammarInductionFromTrees(Model<Grammar> shared_model,
                                   InductionCharts<Grammar> shared_charts,
                                   Grammar global_grammar,
                                   ParserInterface<Grammar> parser_interface,
                                   ArrayList<Exception> exceptions) {
    super(shared_model, shared_charts, parser_interface, exceptions);
    this.grammar = global_grammar;
  }
  
  /**
   * Induces rules by allowing for a lexical item to combine with a constituent
   * higher in the tree. A = cell[i][i] = X/Y B = cell[i+1][j] = Y C =
   * cell[j+1][k] = Z
   * @param chart Chart to use for Induction
   * @throws Exception
   */
  public void map(InductionChart<Grammar> chart) throws Exception {
    // Parse the chart
    super.map(chart);
    
    // Get all the categories per cell
    HashSet<InducedCAT>[][] lexCats = new HashSet[chart.getLength()][chart.getLength()];
    for (int s = 0; s < chart.getLength(); s++) {
      for (int i = 0; i < chart.getLength() - s; i++) {
        HashSet<InducedCAT> temp = new HashSet<>();
        for (ChartItem<Grammar> ci : chart.chart[i][i + s].values())
          if (ci.iCAT != null && !ci.iCAT.modifier && !Rule_Type.TR(ci.type()))
            temp.add(ci.iCAT);
        lexCats[i][i + s] = temp;
      }
    }

    // Cases:
    // i,i = X/Y i+1,j = Y j+1,k = Z (i,i) = (X/Z)/Y
    // i,j-1 = Z j,k-1 = Y k,k = X\Y (k,k) = (X\Z)\Y

    // Ranges:
    // i \in [0 ,L-2)
    // j \in [i+1,L-1)
    // k \in [j+1,L )

    for (int i = 0; i < chart.getLength() - 2; i++) {
      for (int j = i + 1; j < chart.getLength() - 1; j++) {
        for (int k = j + 1; k < chart.getLength(); k++) {
          // Case 1
          for (InducedCAT A : lexCats[i][i]) {
            if (A.D.equals(Direction.FW) && lexCats[i + 1][j].contains(A.Arg)) {
              for (InducedCAT newArg : lexCats[j + 1][k])
                if (GrammarInductionUtils.OKArgument(A.Res, newArg)) {
                  grammar.newLexCats.get(chart.sentence.get(i).tag()).put(
                      A.Res.forward(newArg).forward(A.Arg), true);
                }
            }
          }

          // Case 2
          for (InducedCAT C : lexCats[k][k]) {
            if (C.D.equals(Direction.BW) && lexCats[j][k - 1].contains(C.Arg)) {
              for (InducedCAT newArg : lexCats[i][j - 1])
                if (GrammarInductionUtils.OKArgument(C.Res, newArg)) {
                  grammar.newLexCats.get(chart.sentence.get(k).tag()).put(
                          C.Res.backward(newArg).backward(C.Arg), true);
                }
            }
          }
        }
      }
    }
  }

}
