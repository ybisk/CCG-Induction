package CCGInduction.parser;

import CCGInduction.grammar.Grammar;
import CCGInduction.models.Model;

/**
 * @author bisk1
 * Parsers must implement parse on a chart which has been parameterized by
 * a grammar.
 * @param <G> Grammar for paramaterization
 */
public interface ParserInterface<G extends Grammar> {

  /**
   * Execute parse on a chart
   * @param chart
   *    Chart we're operating on
   */
  void parse(Model<G> model, Chart<G> chart);
}
