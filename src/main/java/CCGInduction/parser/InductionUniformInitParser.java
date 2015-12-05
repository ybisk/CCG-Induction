package CCGInduction.parser;

import CCGInduction.grammar.Grammar;
import CCGInduction.models.Model;

import java.util.ArrayList;

/**
 * @author bisk1
 * Accumulate uniform initial counts for the model
 */
public class InductionUniformInitParser<G extends Grammar,C extends CoarseToFineChart<G>> extends SerializationParser<G, C> {

  /**
   * Requires knowledge of Chart source, parser, and model
   * @param shared_charts Chart collection
   * @param parser_interface Parser
   * @param shared_model Model
   * @param exceptions Caught exceptions
   */
  public InductionUniformInitParser(SerializableCharts<G, C> shared_charts,
                                    CYKParser<G> parser_interface, Model<G> shared_model,
                                    ArrayList<Exception> exceptions) {
    super(shared_model, shared_charts, parser_interface, exceptions);

    this.localModel.Distributions.forEach(localModel.priorCounts::addDist);
  }

  @Override
  protected synchronized void setup() {
    if (setup.get())
      return;
    super.setup();
    this.globalModel.Distributions.forEach(globalModel.priorCounts::addDist);
  }

  /**
   * Add a reference to the prior counts datastructure which we can fill during
   * parsing
   * @param chart Chart
   * @throws Exception
   */
  @Override
  public void map(C chart) throws Exception{
    chart.priorCounts = localModel.priorCounts;
    super.map(chart);
  }

  @Override
  public synchronized void reduce() throws Exception {
    //this.localModel.priorCounts.addAll(priorCounts);
    super.reduce();
  }
}
