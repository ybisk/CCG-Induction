package CCGInduction.parser;

import CCGInduction.Configuration;
import CCGInduction.data.JSON.JSONFormat;
import CCGInduction.grammar.Grammar;
import CCGInduction.models.Model;
import CCGInduction.utils.Logger;
import CCGInduction.utils.TextFile;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author bisk1
 * @param <G>
 * @param <C>
 *
 */
public class TestTimeParser<G extends Grammar, C extends Chart<G>> extends Parser<G, C> {

  /**
   * String queue of printed viterbi parses (hashed by chart id).
   * This contains AUTO and PARG analyses
   */
  private static ConcurrentHashMap<Integer, String> completed_viterbi_parses;
  /**
   * File for printing JSON readable parses
   */
  private static Writer viterbiFileTestParse;
  /**
   * Next chart id to be written to file
   */
  private static final AtomicInteger next_index = new AtomicInteger(0);
  //private static int next_index;
  private final String base_filename;

  /**
   * Parses test data.  It optionally outputs viterbi parses as well as their
   * dependencies and a lexicon.  Additional data structures are passed by
   * reference to allow for viterbi statistics to be accumulated for the lexicon
   * @param shared_charts  Charts to parse
   * @param shared_model   Model for scoring
   * @param parser_interface Parser
   * @param filename  Test output file prefixes
   * @param exceptions Caught exceptions
   */
  public TestTimeParser(Charts<G, C> shared_charts, Model<G> shared_model,
                        ParserInterface<G> parser_interface, String filename,
                        ArrayList<Exception> exceptions) {
    super(shared_model, shared_charts, parser_interface, exceptions);
    this.base_filename = filename;
  }

  @Override
  public synchronized void setup() {
    if (setup.get())
      return;
    // Create output files
    Logger.logln("Printing V&C files for " + base_filename);
    try {
      completed_viterbi_parses = new ConcurrentHashMap<>();
      viterbiFileTestParse = TextFile.Writer(this.base_filename + ".JSON.gz");
      next_index.set(0);
    } catch (Exception e) {
      this.thrown_exceptions.add(e);
    }
  }

  @Override
  public void map(C chart) throws Exception {
    // Only parse short sentences
    if (chart.sentence.length_noP() <= Configuration.longestTestSentence) {
      // Parse the chart
      super.map(chart);
      // Score the chart
      if (chart.success()) {
        chart.cleanForest(localModel.Test);
        localModel.inside(chart);
        // Get TopK
        chart.TOP.populateTopK(localModel.Test);
        chart.viterbi(localModel.grammar);
      }
    }

    if (chart.sentence.JSON == null)
      JSONFormat.createFromSentence(chart.sentence, localModel.grammar);
    completed_viterbi_parses.put(chart.id, chart.sentence.JSON.toString());

    // Add ours to queue and check if the next write is ready
    boolean can_write_more;
    do {
      can_write_more = attemptToWrite();
    } while(can_write_more);
  }

  private static synchronized boolean attemptToWrite() throws IOException {
    if (completed_viterbi_parses.containsKey(next_index.intValue())) {
      // Print JSON object
      viterbiFileTestParse.write(completed_viterbi_parses.remove(next_index.intValue()) + "\n");

      next_index.incrementAndGet();
      return completed_viterbi_parses.containsKey(next_index.intValue());
    }
    return false;
  }

  @Override
  public synchronized void cleanup() {
    // Close our files
    try {
      boolean can_write_more;
      do {
        can_write_more = attemptToWrite();
      } while(can_write_more);

      if (!completed_viterbi_parses.isEmpty()) {
        throw new Exception("Not empty: " + completed_viterbi_parses.keySet().size() + "\n"+
        next_index.intValue() + "\t" + Arrays.toString(completed_viterbi_parses.keySet().toArray()));
      }
      viterbiFileTestParse.close();
    } catch (Exception e) {
      this.thrown_exceptions.add(e);
    }
  }
}
