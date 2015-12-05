package CCGInduction.parser;

import CCGInduction.grammar.Grammar;
import CCGInduction.models.Model;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.FSTObjectOutput;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.zip.GZIPOutputStream;

/**
 * This class is intended to wrap the parser class by adding the
 * ability to save successfully parsed charts
 * @author bisk1
 * @param <G>
 * @param <C>
 *
 */
public class SerializationParser<G extends Grammar, C extends Chart<G>> extends Parser<G,C> {

  // Thread-local Fast Serialization configuration
  private final ArrayList<byte[]> serialized_data = new ArrayList<>();
  static final ThreadLocal<FSTConfiguration> conf = new ThreadLocal() {
    public FSTConfiguration initialValue() {
      return FSTConfiguration.createDefaultConfiguration();
    }};

  /**
   * Parses and then serializes and stores (in the original charts
   * object) the successfully parsed charts.
   * @param shared_charts
   *    Data source
   * @param parser_interface
   *    Parser to be used
   * @param exceptions
   *    Thrown exceptions
   */
  SerializationParser(Model<G> shared_model, SerializableCharts<G,C> shared_charts, CYKParser<G> parser_interface,
                      ArrayList<Exception> exceptions) {
    super(shared_model, shared_charts, parser_interface, exceptions);
  }

  @Override
  protected void map(C chart) throws Exception {
    // Parse the chart
    super.map(chart);
    // Clean the forest
    chart.cleanForest(localModel.Test);
    // Write chart into memory
    if (chart.success()) {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      BufferedOutputStream buffered = new BufferedOutputStream(new GZIPOutputStream(bos));
      FSTObjectOutput oos = conf.get().getObjectOutput(buffered);
      oos.writeObject(chart);
      oos.flush();
      buffered.close();
      serialized_data.add(bos.toByteArray());
    }
  }

  @Override
  protected void reduce() throws Exception {
    // Contribute the serialized charts from this thread
    ((SerializableCharts<G,C>)charts).addData(serialized_data);
    super.reduce();
  }

  @Override
  public synchronized void cleanup() {
    try {
      charts.reset_index();
      ((SerializableCharts<G,C>)charts).readFromMemory();
      globalModel.fixedGrammar = true;
    } catch (Exception e) {
      this.thrown_exceptions.add(e);
    }
  }

}
