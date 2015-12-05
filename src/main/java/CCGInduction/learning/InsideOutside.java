package CCGInduction.learning;

import CCGInduction.grammar.Grammar;
import CCGInduction.Configuration;
import CCGInduction.models.Model;
import CCGInduction.parser.Chart;
import CCGInduction.parser.Charts;
import CCGInduction.utils.Logger;
import CCGInduction.utils.Mapper;

import java.util.ArrayList;

/**
 * @author bisk1
 * @param <G>
 * @param <C>
 *
 */
public class InsideOutside<G extends Grammar, C extends Chart<G>> extends Mapper<G,C> {

  private final CountsArray localCounts = new CountsArray();

  /**
   * Performs count computation over charts
   * @param shared_charts Global reference to charts
   * @param shared_model  Global model reference
   * @param exceptions    Caught exceptions
   */
  public InsideOutside(Charts<G, C> shared_charts, Model<G> shared_model, ArrayList<Exception> exceptions) {
    super(shared_model, shared_charts, exceptions);

    this.localModel.Distributions.forEach(localCounts::addDist);

    this.localModel.accumulatedCounts = new CountsArray();
    localModel.Distributions.forEach(this.localModel.accumulatedCounts::addDist);
  }

  @Override
  public synchronized void setup() {
    if (setup.get())
      return;
    this.globalModel.accumulatedCounts = new CountsArray();
    globalModel.Distributions.forEach(this.globalModel.accumulatedCounts::addDist);
    this.globalModel.LL.clear();
  }

  @Override
  public void map(C chart) throws Exception {
    if (chart.success()) {
      localModel.inside(chart);
      localModel.outside(chart);
      localModel.counts(chart, localCounts);
      if (Configuration.trainK > 1 || Configuration.viterbi) {
        // Build topK
        // chart.TOP.getTopK();
        if (chart.TOP.topK == null || chart.TOP.topK.isEmpty()) {
          Logger.log("ERROR\n");
          chart.debugChart();
          throw new Exception("Successful with no parses: " + chart.parses + " --vs-- " + 0);
        }
      }
      localModel.LL.add(chart.likelihood);
    }
  }

  @Override
  public void reduce() throws Exception {
    this.localModel.accumulateCounts(localCounts);
    super.reduce();
  }

  @Override
  public synchronized void cleanup() {
    try {
      this.globalModel.accumulatedCounts.updateDistributions();
    } catch (Exception e) {
      this.thrown_exceptions.add(e);
    }
  }

}
