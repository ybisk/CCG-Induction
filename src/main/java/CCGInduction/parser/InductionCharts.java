package CCGInduction.parser;

import CCGInduction.grammar.Grammar;
import CCGInduction.data.Sentence;
import CCGInduction.data.Sentences;
import CCGInduction.models.Model;

/**
 * @author bisk1
 *
 */
public class InductionCharts<G extends Grammar> extends SerializableCharts<G, InductionChart<G>> {

  /**
   * Create a collection that holds InductionChart objects
   * @param global_model
   *  Shared model
   * @param sentences
   *  Data source
   */
  public InductionCharts(Model<G> global_model, Sentences sentences) {
    super(global_model, sentences);
  }

  /**
   * Create a collection that holds InductionChart objects
   * @param global_model
   *  Shared Model
   * @param shortest
   *  shortest allowable sentence
   * @param longest
   *  longest allowable sentence
   * @param file
   *  Data source:  Used to instantiate Sentences object
   */
  public InductionCharts(Model<G> global_model, int shortest, int longest, String file) {
    super(global_model, shortest, longest, file);
  }

  @Override
  InductionChart<G> createChart(Model<G> global_model, Sentence sent) {
    return new InductionChart<>(sent, global_model);
  }

}
