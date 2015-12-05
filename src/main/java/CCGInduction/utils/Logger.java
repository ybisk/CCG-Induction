package CCGInduction.utils;

import CCGInduction.Main;
import CCGInduction.learning.DoubleArray;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * File provides printing/logging utilities and array operations
 *
 * @author bisk1
 */
public class Logger {

  public static BufferedWriter Output_file;

  /**
   * Regex Pattern for whitespace
   */
  public static final Pattern underscore_pattern = Pattern.compile("_");
  /**
   * Regex Pattern for splitting word_tag
   */
  public static final Pattern whitespace_pattern = Pattern.compile("\\s+");

  /**
   * Tracks timestamps for operation
   */
  public final static DoubleArray TIMES = new DoubleArray();
  /**
   * Tracks timestamps per operation
   */
  public final static ArrayList<String> STAMPS = new ArrayList<>();

  public Logger(String filename) {
    Output_file = TextFile.Writer(filename);
  }

  /**
   * Print array <doubles> to String, w/ full/limited precision
   *
   * @param doubles        values to print
   * @param printAllValues If all values (even empty) should be printed
   * @return String of double array
   */
  public static String toString(double[] doubles, boolean printAllValues) {
    if (printAllValues) {
      return Arrays.toString(doubles);
    }
    String s = "[";
    for (Double d : doubles) {
      if (d == 0) {
        s += String.format("%9d, ", 0);
      } else {
        s += String.format("%1.6f, ", d);
      }
    }
    s = s.substring(0, s.lastIndexOf(","));
    return s + "]";
  }

  /**
   * Add a timestamp
   *
   * @param timestampMessage action assigned to this timestamp
   */
  public static void timestamp(String timestampMessage) {
    STAMPS.add(timestampMessage);
    TIMES.add((double) (new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()).getTime()
            - Main.Start) / 60000);
  }

  /**
   * Prints dots to screen depending on <numberOfOperations>
   *
   * @param numberOfOperations Number steps algorithm has taken
   */
  public static void stat(int numberOfOperations) {
    if (total != 0)
      percent(numberOfOperations);
    else {
      if (numberOfOperations > 0 && numberOfOperations % 100 == 0) {
        log(".");
      }
      if (numberOfOperations > 0 && numberOfOperations % 1000 == 0) {
        log("\t");
      }
      if (numberOfOperations > 0 && numberOfOperations % 10000 == 0) {
        log("\n");
      }
    }
  }

  public static int total = 0;

  /**
   * Print percentage
   *
   * @param numberOfOperations count
   */
  public static void percent(float numberOfOperations) {
    if (numberOfOperations % 1000 == 0) {
      Logger.log("\r" + (int) (100 * numberOfOperations / (total - 1)) + "%");
    }
    if (numberOfOperations >= total - 1) {
      Logger.log("\r100%\t");
    }
  }

  /**
   * Prints a <logMessage> both to screen and the log file
   *
   * @param logMessage Message to print to log
   */
  public static void log(String logMessage) {
    System.out.print(logMessage);
    logOnly(logMessage);
  }

  /**
   * Print a message only to the log file, not the screen
   * @param logMessage Message to print
   */
  public static void logOnly(String logMessage) {
    try {
      if (Output_file != null) Output_file.write(logMessage);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * log(logMessage) with newline
   *
   * @param logMessage Message to print to log
   */
  public static void logln(String logMessage) {
    log(logMessage + "\n");
  }

  /**
   * Logs a formatted message.   Title:  value
   */
  public static void logln(String Title, String Value) {
    log(String.format("%-20s   %s\n",Title,Value.trim()));
  }

  /**
   * Escapes characters for printing to LaTeX
   *
   * @param stringToEscape input
   * @return Escaped string
   */
  public static String escape_chars(String stringToEscape) {
    if (stringToEscape.isEmpty()) {
      return stringToEscape;
    }
    return stringToEscape.replaceAll("\\$", "\\\\\\$").replaceAll("_", "\\\\_")
        .replaceAll("%", "\\\\%").replaceAll("&", "\\\\&")
        .replaceAll("#", "\\\\#").replaceAll("\\^", "caret")
        .replaceAll("\\{", "\\\\\\$\\\\\\{\\\\\\$").replaceAll("\\}", "\\\\\\$\\\\\\}\\\\\\$");
  }

  public static void printTimes() {
    Logger.logln("");
    for (int i = 0; i < STAMPS.size(); i++) {
      Logger.logOnly(String.format("%-30s %f", STAMPS.get(i), TIMES.get(i)));
    }

    Logger.logln("\nDistribution of Time");
    HashMap<String, Double> diffs = new HashMap<>();
    int count = 1; // For Yonatan
    double pd = 0.0;
    String pS = "Startup";
    diffs.put("Startup", 0.0);
    for (int i = 0; i < STAMPS.size(); i++) {
      String stamp = STAMPS.get(i);
      Double time = TIMES.get(i);

      if (stamp.contains("Map")) {
        stamp += " " + count;
      }
      if (stamp.equals("Test")) {
        count += 1;
      }

      if (!diffs.containsKey(stamp)) {
        diffs.put(stamp, 0.0);
      }
      diffs.put(pS, diffs.get(pS) + (time - pd));

      pS = stamp;
      pd = time;
    }
    ArrayList<ObjectDoublePair<String>> pairs =
        new ArrayList<>();
    for (String label : diffs.keySet()) {
      pairs.add(new ObjectDoublePair<>(label, diffs.get(label)));
    }
    Collections.sort(pairs);
    Logger.logln(String.format("%-30s %4s %s", "Action", "%", "  Min"));
    Logger.logln("------------------------------------------");
    for (ObjectDoublePair<String> pair : pairs) {
      Logger.logln(String.format("%-30s %3.2f %2.5f", pair.content(),
          100 * pair.value() / pd, pair.value()));
    }
  }

  public static void close() {
    try {
      if (Output_file != null) Output_file.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
