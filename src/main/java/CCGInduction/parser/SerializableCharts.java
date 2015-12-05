package CCGInduction.parser;

import CCGInduction.grammar.Grammar;
import CCGInduction.utils.Logger;
import CCGInduction.data.Sentences;
import CCGInduction.models.Model;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.FSTObjectInput;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;

/**
 * @author bisk1
 * A collection of charts which allows for storing the serialized charts in
 * memory
 * @param <G>
 * @param <T>
 */
public abstract class SerializableCharts<G extends Grammar, T extends Chart<G>> extends Charts<G, T> {
  // Thread-local Fast Serialization configuration
  static final ThreadLocal<FSTConfiguration> conf = new ThreadLocal() {
    public FSTConfiguration initialValue() {
      return FSTConfiguration.createDefaultConfiguration();
    }};

  private byte[][] saved_data = new byte[0][0];
  private byte[][] replacement_data = new byte[0][0];
  private final AtomicInteger total = new AtomicInteger(0);
  private final AtomicInteger current_index = new AtomicInteger(0);
  private boolean read_from_memory = false;
  /**
   * Creates container for Chart objects which are serializable.  Data is pulled
   * from Sentences object
   * @param global_model
   *  Shared model
   * @param sents
   *  Data source
   */
  SerializableCharts(Model<G> global_model, Sentences sents) {
    super(global_model, sents);
  }

  /**
   * Creates container for Chart objects which are serializable.  Takes a source
   * file and bounding sentence lengths for its creation.
   * @param global_model
   *  Shared model
   * @param shortest
   *  Shortest sentence to be included
   * @param longest
   *  Longest sentence to be included
   * @param file
   *  Source text file
   */
  SerializableCharts(Model<G> global_model, int shortest, int longest, String file) {
    super(global_model, shortest, longest, file);
  }

  /**
   * Copy constructor
   * @param chartsToCopy
   *  Source charts
   */
  SerializableCharts(SerializableCharts<G, T> chartsToCopy) {
    super(chartsToCopy.model, chartsToCopy.sentences);
    this.total.set(chartsToCopy.total.get());
    this.saved_data = Arrays.copyOf(chartsToCopy.saved_data, this.total.get());
    this.read_from_memory = true;
  }

  public void clear() {
    saved_data = new byte[0][0];
    total.set(0);
    reset_index();
    read_from_memory = false;
  }

  @Override
  public T next() throws Exception {
    if (read_from_memory) {
      int index = this.current_index.getAndIncrement();
      if (index >= this.total.get()) {
        return null;
      }

      Logger.percent(index);
      byte[] next = this.saved_data[index];
      BufferedInputStream BIS = new BufferedInputStream(new GZIPInputStream(new ByteArrayInputStream(next)));
      FSTObjectInput ois = conf.get().getObjectInput(BIS);

      @SuppressWarnings("unchecked")
      T chart = (T) ois.readObject();
      BIS.close();

      return chart;
    }
    return super.next();
  }

  /**
   * @return The number of stored charts
   */
  public int size() {  return this.total.get(); }

  /**
   *  Specifies that moving forward, next() should deserialize rather than
   *  read anew
   */
  synchronized void readFromMemory() {
    this.read_from_memory = true;
  }

  @Override
  public void reset_index() {
    super.reset_index();
    this.current_index.set(0);
  }

  /**
   * Adds data to the internally stored byte[] of serialized charts
   * @param new_data Data to incorporate
   */
  synchronized void addData(ArrayList<byte[]> new_data) {
    int offset = this.saved_data.length;
    this.saved_data = Arrays.copyOf(this.saved_data,
        this.saved_data.length + new_data.size());
    for (int i = 0; i < new_data.size(); ++i) {
      this.saved_data[offset + i] = new_data.get(i);
    }
    this.total.set(this.saved_data.length);
  }

  /**
   * Merge parsed charts that have been serialized with the existing data
   * @param moreCharts Additional charts
   */
  public synchronized void addData(SerializableCharts<G, T> moreCharts) {
    int offset = this.saved_data.length;
    this.saved_data = Arrays.copyOf(this.saved_data,
        this.saved_data.length + moreCharts.saved_data.length);
    System.arraycopy(moreCharts.saved_data, 0,
        this.saved_data, offset, moreCharts.saved_data.length);
    this.total.set(this.saved_data.length);
  }

}
