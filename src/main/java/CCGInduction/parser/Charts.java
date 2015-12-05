package CCGInduction.parser;

import CCGInduction.data.Sentence;
import CCGInduction.data.Sentences;
import CCGInduction.grammar.Grammar;
import CCGInduction.models.Model;
import CCGInduction.utils.Logger;

/**
 * @author bisk1
 * A collection for strong Chart objects.  A Chart is parameterized by a
 * grammar.
 * @param <G>
 * @param <T>
 */
public abstract class Charts<G extends Grammar, T extends Chart<G>> {

  /**
   * Global model
   */
  final Model<G> model;
  /**
   * Data source
   */
  public final Sentences sentences;

  /**
   * Create a collection for holding Chart objects
   * @param global_model
   *  Global model reference
   * @param shortest
   *  Shortest allowable sentence
   * @param longest
   *  Longest allowable sentence
   * @param file
   *  Data source
   */
  Charts(Model<G> global_model, int shortest, int longest, String file) {
    this.model = global_model;
    this.sentences = new Sentences(this.model.grammar, shortest, longest, file);
  }

  /**
   * Create a collection for holding Chart objects
   * @param global_model
   *  Global model reference
   * @param streaming
   *  Specifies if the underlying sentences data-structure should maintain state
   * @param shortest
   *  Shortest allowable sentence
   * @param longest
   *  Longest allowable sentence
   * @param file
   *  Data source
   */
  Charts(Model<G> global_model, boolean streaming, int shortest, int longest, String file) {
    this.model = global_model;
    this.sentences = new Sentences(this.model.grammar, streaming, shortest,
        longest, file);
  }

  /**
   * Create a collection for holding Chart objects, whose data source is sents
   * @param global_model
   *  Shared model
   * @param sents
   *  Data source
   */
  Charts(Model<G> global_model, Sentences sents) {
    this.model = global_model;
    this.sentences = sents;
  }

  /**
   * Returns the next Chart.  It creates this chart by passing in the next
   * sentence (possibly from disk or memory) to the createChart function
   * @return
   *  A new chart
   * @throws Exception
   */
  public T next() throws Exception {
    Sentence current = sentences.next();
    if (current == null) {
      return null;
    }
    return createChart(model, current);
  }

  /**
   * Abstract method for how to create a Chart given a sentence.  Allows for
   * children to specify custom logic
   * @param global_model
   *  Shared model
   * @param sent
   *  Base sentence for chart
   * @return
   *  An experiment specific chart
   */
  abstract T createChart(Model<G> global_model, Sentence sent);

  /**
   * Tells the Collection to start reading again from the beginning of the data
   */
  public void reset_index() {
    this.sentences.reset_index();
  }

  public void readSentences() {
    // Spin through them all
    int c = 0;
    for ( ; sentences.next() != null; ++c);
    sentences.reset_index();
    Logger.total = c;
  }
}
