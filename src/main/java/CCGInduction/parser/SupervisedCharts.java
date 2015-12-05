package CCGInduction.parser;

import CCGInduction.grammar.Grammar;
import CCGInduction.data.Sentence;
import CCGInduction.data.Sentences;
import CCGInduction.models.Model;

/**
 *  Collection of charts read from a supervised AUTO file (e.g. CCGbank)
 */
public class SupervisedCharts<G extends Grammar> extends SerializableCharts<G, SupervisedChart<G>> {

  /**
   * Create a collection that holds SupervisedChart objects
   * @param global_model
   *  Shared model
   * @param sentences
   *  Data source
   */
  public SupervisedCharts(Model<G> global_model, Sentences sentences) {
    super(global_model, sentences);
  }

  /**
   * Create a collection that holds SupervisedChart objects
   * @param global_model Shared Model
   * @param shortest     shortest allowable sentence
   * @param longest      longest allowable sentence
   * @param file         Data source:  Used to instantiate Sentences object
   */
  SupervisedCharts(Model<G> global_model, int shortest, int longest, String file) {
    super(global_model, shortest, longest, file);
  }

  @Override
  SupervisedChart<G> createChart(Model<G> global_model, Sentence sent) {
    return new SupervisedChart<>(sent, global_model);
  }
}
