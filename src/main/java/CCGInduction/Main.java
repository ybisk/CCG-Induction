package CCGInduction;

import CCGInduction.experiments.*;
import CCGInduction.utils.AndroidPushNotification;
import CCGInduction.utils.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

/**
 * Main function for running experiments in this code base
 * 
 * @author bisk1
 */
public strictfp class Main {
  private static Experiment<?> experiment = null;

  /**
   * Start of experiment
   */
  public static long Start;
  /**
   * Provides the main loop for the program.
   * @param args
   *          args[0] = Configuration File
   * @throws IOException
   */
  public static strictfp void main(String[] args) throws Exception {
    try {
      Start = new java.sql.Timestamp(
          Calendar.getInstance().getTime().getTime()).getTime();
      if (args.length < 1) {
        System.err.println("For training a parser/HMM: java -jar CCGInduction.jar config.txt");
        System.err.println("For Evaluation:");
        System.err.println("\t\tjava -cp CCGInduction.jar CCGInduction.evaluation.PARGDependencies");
        System.err.println("\t\tjava -cp CCGInduction.jar CCGInduction.evaluation.CoNLLDependencies");
        System.err.println("\t\tjava -cp CCGInduction.jar CCGInduction.evaluation.SupertagAccuracy");
        System.err.println("\t\tjava -cp CCGInduction.jar CCGInduction.evaluation.TagEvaluator");
        System.err.println("For Utilities:");
        System.err.println("\t\tjava -cp CCGInduction.jar CCGInduction.utils.JSONFormat");
        System.err.println("\t\tjava -cp CCGInduction.jar CCGInduction.utils.ConvertFromAUTO");
        System.err.println("\t\tjava -cp CCGInduction.jar CCGInduction.utils.ComputeLexicon");
        System.exit(1);
      }

      // ----------------------- Read configuration ---------------------- //
      Configuration config = new Configuration(args);
      // -------------------- Create output directory -------------------- //
      //noinspection ResultOfMethodCallIgnored
      new File(Configuration.Folder).mkdirs();
      new Logger(Configuration.Folder + "/Output.log");
      Configuration.print();
      // -------------------------- Create Experiment -------------------- //
      switch (Configuration.source) {
      case induction:
        experiment = new UnsupervisedInduction(config);
        break;
      case supervised:
        experiment = new SupervisedParser(config);
        break;
      case tagInduction:
        experiment = new TagInduction(config);
        break;
      default:
        System.out.println("Unrecognized Training: " + Configuration.source);
        System.exit(1);
      }

      // -------------------------- Perform Training -------------------- //
      for (Action action : Configuration.trainingRegimen) {
        Logger.logln("Performing:", action.toString());
        Logger.timestamp("Performing " + action);
        experiment.perform(action);
      }
      // -------------------------- Print Times -------------------- //
      Logger.timestamp("Finish");
      if (Configuration.api_key != null) {
        if (!(new File(Configuration.api_key)).exists()) {
          System.err.println("File " + Configuration.api_key + " does not exist");
        } else {
          AndroidPushNotification notification = new AndroidPushNotification(Configuration.api_key);
          notification.notify("Folder: " + Configuration.Folder + "\nFinished: " + Logger.TIMES.get(Logger.STAMPS.size() - 1));
        }
      }

      Logger.printTimes();
    } catch (NullPointerException npe) {
      throw npe;
    } catch (Exception e) {
      e.printStackTrace();
      Logger.log(e.getMessage());
      System.err.println(e.getMessage());
    }

    Logger.close();
  }
}