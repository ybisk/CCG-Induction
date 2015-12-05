package CCGInduction.utils;

import CCGInduction.Configuration;
import CCGInduction.grammar.Grammar;
import CCGInduction.models.Model;
import CCGInduction.parser.Chart;
import CCGInduction.parser.Charts;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple abstract class which executes a map operation on all charts objects.
 * The map function can change the values of the charts.  All methods
 * @author bisk1
 * @param <G> 
 * @param <C> 
 *
 */
public abstract class Mapper<G extends Grammar, C extends Chart<G>> implements Runnable {
  static final AtomicInteger threads_running = new AtomicInteger(0);
  static final AtomicInteger threads_spawned = new AtomicInteger(0);
  protected static final AtomicBoolean setup = new AtomicBoolean(false);
  protected final Model<G> localModel;
  protected final Model<G> globalModel;

  /**
   * Globally shared collection over which all map operations will be run
   */
  protected final Charts<G,C> charts;
  /**
   * exceptions which were thrown during mapping that resulted in early
   * termination
   */
  protected final ArrayList<Exception> thrown_exceptions;

  /**
   *  Default constructor sets all fields to null
   */
  protected Mapper(Model<G> model, Charts<G,C> shared_charts, ArrayList<Exception> exceptions) {
    globalModel = model;
    localModel = model.copy();
    this.charts = shared_charts;
    this.thrown_exceptions = exceptions;
  }
  
  public final void run() {
    try {
      // Synchronized start, waits for a single thread to call setup
      int threads = threads_running.incrementAndGet();
      threads_spawned.incrementAndGet();
      setup();                            // Synchronized call
      setup.set(true);
      // Process all charts
      C chart;
      while ((chart = charts.next()) != null) {
        chart.model = localModel;
        map(chart);
      }
      // Perform a reduce step
      reduce();
    } catch (Exception e) {
      e.printStackTrace();
      this.thrown_exceptions.add(e);
      return;
    }
    // If every thread has been spawned and everyone has
    // finished, close out
    if (threads_running.decrementAndGet() == 0
        && threads_spawned.get() == Configuration.threadCount) {
      cleanup();
      setup.set(false);
      threads_spawned.set(0);
      try {
        charts.reset_index();
      } catch (Exception e) {
        thrown_exceptions.add(e);
      }
    }
  }
  
  /**
   * Abstract function which when implemented edits a chart, potentially
   * computing features/values/etc
   * @param chart
   *    Chart to operate on
   * @throws Exception
   */
  protected abstract void map(C chart) throws Exception;
  
  /**
   * A final function called after all data has been processed, potentially
   * used to consolidate statistics or write accumulated data.
   * @throws Exception 
   */
  protected void reduce() throws Exception { globalModel.merge(localModel); }
  
  /**
   * Create any data structures or do any preprocessing.  This is only called
   * once per thread pool.  Not called for every thread. 
   */
  protected synchronized void setup() {}
  
  /**
   * Delete and/or finalize (e.g. closing streams) any remaining objects
   */
  protected synchronized void cleanup() {}
}
