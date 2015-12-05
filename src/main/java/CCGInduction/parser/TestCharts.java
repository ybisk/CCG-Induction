package CCGInduction.parser;

import CCGInduction.data.Sentence;
import CCGInduction.experiments.Action;
import CCGInduction.grammar.Grammar;
import CCGInduction.models.Model;

/**
 * @author bisk1
 * Collection of InductionChart objects used at test time
 * @version $Revision: 1.0 $
 */
public class TestCharts<G extends Grammar> extends Charts<G, CoarseToFineChart<G>> {

  private final Action typeTest;

  /**
   * Create a collection that holds InductionChart objects
   * @param global_model  Shared Model
   * @param shortest      shortest allowable sentence
   * @param longest       longest allowable sentence
   * @param file          Data source:  Used to instantiate Sentences object
   * @param parseAction   Test vs SupervisedTest
   */
  public TestCharts(Model<G> global_model, int shortest, int longest, String file, Action parseAction) {
    super(global_model, true, shortest, longest, file);
    typeTest = parseAction;
  }

  /**
   * Method createChart.
   * @param globalModel Model<Grammar>
   * @param sent Sentence
   * @return InductionChart
   */
  @Override
  /**
   * Creates and Returns an InductionChart.
   */
  CoarseToFineChart<G> createChart(Model<G> globalModel, Sentence sent) {
    if (typeTest == Action.Test)
      return new InductionChart(sent, globalModel);
    if (typeTest == Action.SupervisedTest)
      return new SupervisedChart(sent, globalModel);
    return null;
  }

}
