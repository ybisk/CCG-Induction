package CCGInduction;


import CCGInduction.ccg.CoNLLDependency;
import CCGInduction.experiments.Action;
import CCGInduction.experiments.Training;
import CCGInduction.grammar.Normal_Form;
import CCGInduction.utils.Math.Log;
import CCGInduction.ccg.AUTOConversion;
import CCGInduction.data.Tagset.TAG_TYPE;
import CCGInduction.parser.AUTO_TYPE;
import CCGInduction.utils.Logger;
import CCGInduction.utils.TextFile;

import java.io.*;
import java.util.Arrays;

/**
 * @author bisk1 Set of configuration variables read from mandatory Config.txt
 *         argument at runtime
 */
public strictfp final class Configuration implements Serializable {
  private static final long serialVersionUID = 5162012L;
  /**
   * Includes/Ignores punctuation from data
   */
  public static boolean ignorePunctuation = false;

  // // GRAMMAR ////
  /**
   * POS tag file location
   */
  public static String TAGSET;
  /**
   * Parse with {Coarse,Fine,Universal} tags
   */
  public static TAG_TYPE tagType = TAG_TYPE.Fine;
  /**
   * Add column for NAACL Shared Task input/output
   */
  public static boolean hasUniversalTags = true;
  /**
   * Parse with NF: {Full,Full_noPunct,Eisner,Eisner_Orig,None}
   */
  public static Normal_Form NF = Normal_Form.Full; // Full, Full_noPunct, Eisner,
  // Eisner_Orig, None
  /**
   * Allow TypeRaising
   */
  public static boolean typeRaising = true;
  /**
   * Restrict TypeRaising to lexical items
   */
  public static boolean lexTROnly = false;

  /**
   * Allow for both  (X/X)\X  and  (X\X)/X
   */
  public static boolean allowXbXbX = false;

  // // Grammar Induction ////
  /**
   * EM init w/ Uniform Trees
   */
  public static boolean uniformPrior = false;
  public static final boolean accumulateCounts = true;
  /**
   * Maximum lexical arity
   */
  public static int maxArity = 3;
  /**
   * Maximum lexical arity for Modifiers
   */
  public static int maxModArity = 2;
  public static boolean induceValidOnly = true;
  /**
   * Allow complex arguments
   */
  public static boolean complexArgs = false;
  /**
   * Allow TOP to complex arguments
   */
  public static boolean complexTOP = false;
  /**
   * Raise variational hyperparameter to n
   */
  public static double[] alphaPower = new double[] { 0.0, 0.0, 0.0 };
  public static boolean ALPHA_SCHEME = false;
  /**
   * PY Discount factor 0 <= d < 1
   */
  private static double discount = 0.0; // PY Discount factor
  /**
   * Log form of PY Discount
   */
  public static double logDiscount = Math.log(discount); // PY Discount factor
  // Induced Tag Set ////
  public static String BMMMClusters;

  // // TRAINING ////
  /**
   * Specifies the order and set of commands to be executed during the
   * experiment
   */
  public static Action[] trainingRegimen = new Action[] { Action.I, Action.I, Action.B1ModTR, Action.Test };
  /**
   * Specifies which style of experiment is being run
   */
  public static Training source = Training.induction;
  /**
   * Hard vs Soft EM
   */
  public static boolean viterbi = false; // Hard vs Soft EM
  /**
   * Max # of EM/BW Iterations
   */
  public static int maxItr = 2000; // Maximum iterations default:2000
  /**
   * EM/BW convergence threshold
   */
  public static double threshold = 0.001; // EM/SGD Convergence threshold
  /**
   * Used instead of zeros for small values
   */
  public final static double EPSILON = -20;

  // // TRAINING MODEL ////
  /**
   * TopK parses to be computed during training
   */
  public static int trainK = 1;
  /**
   * Minimum prob/value allowed for a rule
   */
  public static double smallRule = -25;
  /**
   * When interpolating models, lambda cannot grow larger than 1-smallRule
   */
  private static double largeRule;
  /**
   * Clusters for HMM
   */
  public static int NumClusters = 45;

  // // TRAINING DATA ////
  /**
   * Training file(s), comma delimited
   */
  public static String[] trainFile = new String[] {"data/Corpus.PTB"};
  /**
   * Shortest sentence to consider
   */
  public static int shortestSentence = 1;
  /**
   * Longest sentence to consider
   */
  public static int longestSentence = 20;

  // // SAVE ////
  /**
   * Folder for output files
   */
  public static String Folder = "";
  /**
   * Files to read model from
   */
  public static String saveModelFile = "Model";
  public static String loadModelFile = "Model";
  /**  Load a lexicon   */
  public static String savedLexicon = "Lexicon.txt.gz";
  /** Threshold for discarding categories from conditional distribution */
  public static double CondProb_threshold = 0.01;
  /** Choose AUTO vs C&C output format */
  public static AUTO_TYPE auto_type = AUTO_TYPE.CCGBANK;


  // // Push notifications ////
  public static String api_key;
  // // Lexical Learning ////
  /**
   * # or percentage of words to learn
   */
  public static double lexFreq = 5;
  /**
   * # or percentage of nouns to learn
   */
  public static double nounFreq = 0;
  /**
   * # or percentage of verbs to learn
   */
  public static double verbFreq = 0;
  /**
   * # or percentage of function words to learn
   */
  public static double funcFreq = 0;

  // // TESTING ////
  /**
   * List of test files ( comma delimited )
   */
  public static String[] testFile = new String[] { "data/CCGbank.00.PTB" };
  /**
   * Should we generate TeX files for the parses
   */
  public static String TEX_LANGUAGE = "other";
  /** Should we generate CoNLL style dependencies for the top-K */
  public static CoNLLDependency CONLL_DEPENDENCIES = CoNLLDependency.CC_X1___CC_X2;
  /**
   * Longest allowable test sentence (else: right branch)
   */
  public static int longestTestSentence = 200;
  public static int testK = 1;

  // // SYSTEM ////
  /**
   * Max allowable threads for parallelization
   */
  public static int threadCount = Math.min(100, Runtime.getRuntime().availableProcessors());

  /**
   * Whether to print all intermediate model files
   */
  public static boolean printModelsVerbose = true;

  //// Knowledge Graph ////
  public static boolean hardBracketConstraints = false;
  public static boolean hardEntityNConstraints = false;
  public static boolean softBracketConstraints = false;
  public static boolean softEntityNConstraints = false;
  private static double softBracketWeighting = 0.9;
  public static double softBracketWeightingLog = Math.log(softBracketWeighting);
  public static double softBracketWeightingPenaltyLog = Math.log(1.0-softBracketWeighting);


  ////  AUTO Conversion script ////
  public static String AUTOFileToConvert;
  public static AUTOConversion ConvertAUTO = AUTOConversion.TEX;

  //// Special cases ////
  public static String typeChangingRules;

  /**
   * Use command line arguments var=val to set parameters
   *
   * @param cmdLine command line arguments
   * @throws Exception
   */
  public Configuration(String[] cmdLine) throws Exception {
    this(cmdLine[0]);
    if (cmdLine.length > 1) {
      String[] split;
      for (String arg : cmdLine) {
        split = arg.split("=", 2);
        if (split.length == 2) {
          process(split[0], split[1]);
        }
      }
    }
  }

  /**
   * Read parameters from file. Ignore comments (#)
   *
   * @param file Configuration file
   */
  public Configuration(String file) {
    try {
      // Read in Documents
      FileInputStream fstream = new FileInputStream(file);
      DataInputStream in = new DataInputStream(fstream);
      BufferedReader br = new BufferedReader(new InputStreamReader(in));
      String strLine;
      String[] args;
      while ((strLine = br.readLine()) != null) {
        if (!strLine.isEmpty()
            && (strLine.isEmpty() || strLine.charAt(0) != '#')) {
          args = strLine.split("#")[0].trim().split("=", 2);
          if (args.length > 1 && !args[0].equals("#") && !args[1].equals("#")) {
            process(args[0], args[1]);
          }
        }
      }
      br.close();
      largeRule = Log.subtract(Log.ONE, smallRule);
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println("Configuration malformed");
      System.exit(1);
    }
  }

  /**
   * A default constructor for setting properties manually.
   */
  public Configuration() { }

  /**
   * Assign value (val) to variable (v)
   *
   * @param v variable
   * @param val value
   * @throws Exception
   */
  private static void process(String v, String val) throws Exception {
    String var = v.toLowerCase();
    switch (var) {
      case "bmmmclusters":
        BMMMClusters = val;
        break;
      case "api_key":
        api_key = val;
        break;
      case "threshold":
        threshold = Double.parseDouble(val);
        break;
      case "viterbi":
        viterbi = Boolean.parseBoolean(val);
        break;
      case "trainfile":
        if (val.length() > 0 && val.charAt(0) == '[')
          val = val.substring(1,val.length()-1);
        trainFile = val.split(",");
        break;
      case "testfile":
        if (val.length() > 0 && val.charAt(0) == '[')
          val = val.substring(1,val.length()-1);
        testFile = val.split(",");
        break;
      case "maxitr":
        maxItr = Integer.parseInt(val);
        break;
      case "nf":
        NF = Normal_Form.valueOf(val);
        break;
      case "lexfreq":
        lexFreq = Double.parseDouble(val);
        break;
      case "folder":
        Folder = val;
        break;
      case "savemodelfile":
        saveModelFile = val;
        break;
      case "loadmodelfile":
        loadModelFile = val;
        break;
      case "savedlexicon":
        savedLexicon = val;
        break;
      case "condprob_threshold":
        CondProb_threshold = Double.valueOf(val);
        break;
      case "tagset":
        TAGSET = val;
        break;
      case "shortestsentence":
        shortestSentence = Integer.parseInt(val);
        break;
      case "typeraising":
        typeRaising = Boolean.parseBoolean(val);
        break;
      case "trainingregimen":
        if (val.length() > 0 && val.charAt(0) == '[')
          val = val.substring(1,val.length()-1);
        String[] spl = val.split(",");
        trainingRegimen = new Action[spl.length];
        for (int i = 0; i < spl.length; i++) {
          trainingRegimen[i] = Action.valueOf(spl[i].trim());
        }
        break;
      case "traink":
        trainK = Integer.parseInt(val);
        break;
      case "testk":
        testK = Integer.parseInt(val);
        break;
      case "source":
        source = Training.valueOf(val);
        break;
      case "threadcount":
        threadCount = Integer.parseInt(val);
        break;
      case "ignorepunctuation":
        ignorePunctuation = Boolean.parseBoolean(val);
        break;
      case "inducevalidonly":
        induceValidOnly = Boolean.parseBoolean(val);
        break;
      case "longestsentence":
        longestSentence = Integer.parseInt(val);
        break;
      case "tex_language":
        TEX_LANGUAGE = val.toLowerCase();
        break;
      case "conll_dependencies":
        CONLL_DEPENDENCIES = CoNLLDependency.valueOf(val);
        break;
      case "nounfreq":
        nounFreq = Double.parseDouble(val);
        break;
      case "verbfreq":
        verbFreq = Double.parseDouble(val);
        break;
      case "funcfreq":
        funcFreq = Double.parseDouble(val);
        break;
      case "lextronly":
        lexTROnly = Boolean.parseBoolean(val);
        break;
      case "allowxbxbx":
        allowXbXbX = Boolean.parseBoolean(val);
        break;
      case "complexargs":
        complexArgs = Boolean.parseBoolean(val);
        break;
      case "complextop":
        complexTOP = Boolean.parseBoolean(val);
        break;
      case "longesttestsentence":
        longestTestSentence = Integer.parseInt(val);
        break;
      case "tagtype":
        tagType = TAG_TYPE.valueOf(val);
        break;
      case "hasuniversaltags":
        hasUniversalTags = Boolean.parseBoolean(val);
        break;
      case "printModelsVerbose":
        printModelsVerbose = Boolean.parseBoolean(val);
        break;
      case "uniformprior":
        uniformPrior = Boolean.parseBoolean(val);
        break;
      case "discount":
        discount = Double.parseDouble(val);
        logDiscount = Math.log(discount);
        break;
      case "alpha_scheme":
        ALPHA_SCHEME = Boolean.valueOf(val);
        break;
      case "alphapower":
        if (val.length() > 0 && val.charAt(0) == '[')
          val = val.substring(1,val.length()-1);
        String[] vals = val.trim().split(",");
        alphaPower = new double[vals.length];
        for (int i = 0; i < vals.length; i++) {
          alphaPower[i] = Double.parseDouble(vals[i]);
        }
        break;
      case "maxarity":
        maxArity = Integer.parseInt(val);
        break;
      case "maxmodarity":
        maxModArity = Integer.parseInt(val);
        break;
      case "smallrule":
        smallRule = Double.parseDouble(val);
        largeRule = Log.subtract(Log.ONE, smallRule);
        break;
      case "numclusters":
        NumClusters = Integer.parseInt(val);
        break;
      case "hardbracketconstraints":
        hardBracketConstraints = Boolean.parseBoolean(val);
        break;
      case "hardentitynconstraints":
        hardEntityNConstraints = Boolean.parseBoolean(val);
        break;
      case "softbracketconstraints":
        softBracketConstraints = Boolean.parseBoolean(val);
        break;
      case "softentitynconstraints":
        softEntityNConstraints = Boolean.parseBoolean(val);
        break;
      case "softbracketweighting":
        softBracketWeighting = Double.parseDouble(val);
        softBracketWeightingLog = Math.log(softBracketWeighting);
        softBracketWeightingPenaltyLog = Math.log(1.0 - softBracketWeighting);
        break;
      case "auto_type":
        auto_type = AUTO_TYPE.valueOf(val);
        break;
      case "typechangingrules":
        typeChangingRules = val;
        break;
      case "autofiletoconvert":
        AUTOFileToConvert = val;
        break;
      case "convertauto":
        ConvertAUTO = AUTOConversion.valueOf(val);
        break;
      default:
        if (!var.isEmpty() && (var.isEmpty() || var.charAt(0) != '#')) {
          throw new Exception("Unknown variable: \t\"" + var  + "\tConfiguration malformed");
        }
        break;
    }
  }

  static Writer settings;
  /**
   * Write all variables to screen and log file
   */
  static void print() {
    settings = TextFile.Writer(Folder + "/Settings.properties");
    Logger.logln("Saving Configuration to " + Folder + "/Settings.properties");

    printConfig("ignorePunctuation", ignorePunctuation, "Includes/Ignores punctuation from data");

    printConfig("##### Grammar #####", "", "");
    // // GRAMMAR ////
    printConfig("TAGSET", TAGSET, "POS tag file location");
    printConfig("tagType", tagType.toString(), "Parse with {Coarse,Fine,Universal,Induced} tags");
    printConfig("hasUniversalTags", hasUniversalTags, "Add column for NAACL Shared Task input/output");
    printConfig("NF", NF.toString(), "Parse with NF: {Full,Full_noPunct,Eisner,Eisner_Orig,None}");
    printConfig("trainingRegimen", Arrays.toString(trainingRegimen), "Operations for experiment");
    printConfig("typeRaising", typeRaising, "Allow TypeRaising");
    printConfig("lexTROnly", lexTROnly, "Restrict TypeRaising to lexical items");
    printConfig("allowXbXbX", allowXbXbX, "Allow for (X/X)\\X and (X\\X)/X");

    printConfig("##### Grammar Induction #####", "", "");
    // // Grammar Induction ////
    printConfig("uniformPrior", uniformPrior, "EM init w/ Uniform Trees");
    printConfig("maxArity", maxArity, "Maximum lexical arity");
    printConfig("maxModArity", maxModArity, "Maximum lexical arity for Modifiers");
    printConfig("induceValidOnly", induceValidOnly, "");
    printConfig("complexArgs", complexArgs, "Allow complex arguments");
    printConfig("complexTOP", complexTOP, "Allow TOP to complex arguments ");
    printConfig("ALPHA_SCHEME", ALPHA_SCHEME, "Should hyper-parameters be used as constants or X^a schemes?");
    printConfig("alphaPower", Arrays.toString(alphaPower), "Raise variational hyperparameter to n");
    printConfig("discount", discount, "PY Discount factor 0 <= d < 1");
    printConfig("typeChangingRules",typeChangingRules, "Special Unary Type-Changing rules");

    printConfig("##### Tagset Induction #####", "", "");
    // Induced TagSet ////
    printConfig("BMMMClusters", BMMMClusters, "New BMMM Tag mapping");

    printConfig("##### Training #####", "", "");
    // // TRAINING ////
    printConfig("source", source.toString(), "Training setups: induction, supervised");
    printConfig("viterbi", viterbi, "");
    printConfig("maxItr", maxItr, "Max # of EM/BW Iterations");
    printConfig("threshold", threshold, "EM/BW convergence threshold");
    printConfig("# EPSILON", EPSILON, "");

    printConfig("##### Training Model #####", "", "");
    // // TRAINING MODEL ////
    printConfig("trainK", trainK, "TopK parses to be computed during training");
    printConfig("smallRule", smallRule, "Minimum prob/val allowed for a rule");
    printConfig("# largeRule", largeRule, "When interpolating models, lambda cannot grow larger than 1#smallRule");
    printConfig("NumClusters", NumClusters, "Number of clusters to induce with HMM");

    printConfig("##### Training Data #####", "", "");
    // // TRAINING DATA ////
    printConfig("trainFile", Arrays.toString(trainFile), "Training file(s), comma delimited ");
    printConfig("shortestSentence", shortestSentence, "Shortest sentence to consider");
    printConfig("longestSentence", longestSentence, "Longest sentence to consider");

    printConfig("##### Misc #####","","");
    // // SAVE ////
    printConfig("Folder", Folder, "Folder for output files");
    printConfig("saveModelFile", saveModelFile, "file to write the model");
    printConfig("loadModelFile", loadModelFile, "file to read the model");
    printConfig("savedLexicon", savedLexicon, "Lexicon to load");
    printConfig("CondProb_threshold", CondProb_threshold , "Threshold for discarding categories based on cond prob");
    // // SYSTEM ////
    printConfig("threadCount", threadCount, "Number of threads to use");
    ////  Push Notification ////
    printConfig("api_key", api_key, "API Key for push notification from notifymyandroid.com");

    printConfig("##### Induction's Lexical Learning #####","","");
    // // Lexical Learning ////
    printConfig("lexFreq", lexFreq, "# or percentage of words to learn");
    printConfig("nounFreq", nounFreq, "# or percentage of nouns to learn");
    printConfig("verbFreq", verbFreq, "# or percentage of verbs to learn");
    printConfig("funcFreq", funcFreq,
        "# or percentage of function words to learn");

    printConfig("##### Testing #####","","");
    // // TESTING ////
    printConfig("testFile", Arrays.toString(testFile),
        "List of test files ( comma delimited )");
    printConfig("TEX_LANGUAGE", TEX_LANGUAGE, "Are the sentences in chinese or other?");
    printConfig("CONLL_DEPENDENCIES", CONLL_DEPENDENCIES.toString(),
        "Whether CoNLL style viterbi parses should be printed and how conjunction should be treated");
    printConfig("longestTestSentence", longestTestSentence, "Longest allowable test sentence (else: right branch)");
    printConfig("testK", testK, "Number of parses to produces at test time");
    printConfig("AUTO_TYPE", auto_type.toString(), "CCGBANK vs CANDC auto files");

    printConfig("##### AUTO Conversion #####", "", "");
    printConfig("AUTOFileToConvert", AUTOFileToConvert, "Input file [1 auto per line] to convert");
    printConfig("ConvertAUTO", ConvertAUTO.toString(), "Format to convert AUTOs to");

    printConfig("##### Knowledge Graph #####", "", "");
    printConfig("hardBracketConstraints", hardBracketConstraints, "Use Hard Entity constraints when parsing");
    printConfig("hardEntityNConstraints", hardEntityNConstraints, "Use Hard Entity as N constraints when parsing");
    printConfig("softBracketConstraints", softBracketConstraints, "Use Soft Entity constraints when scoring");
    printConfig("softEntityNConstraints", softEntityNConstraints, "Use Soft Entity as N constraints when scoring");
    printConfig("softBracketWeighting", softBracketWeighting, "1-Penalty for violating entity constraints when scoring");
    try {
      settings.close();
      settings = null;
    } catch (IOException exception) {
      exception.printStackTrace();
      System.exit(1);
    }
  }

  /**
   * Prints String vals to log with description
   */
  private static void printConfig(String var, String val, String des) {
    try {
      settings.write(String.format("%-50s  # %-50s\n", var + '=' + (val == null ? "null" : val), des));
    } catch (IOException exception) {
      exception.printStackTrace();
      System.exit(1);
    }
  }

  /**
   * Prints int vals to log
   */
  private static void printConfig(String var, int val, String des) {
    printConfig(var, Integer.toString(val), des);
  }

  /**
   * Prints double vals to log
   */
  private static void printConfig(String var, double val, String des) {
    printConfig(var, Double.toString(val), des);
  }

  /**
   * Prints boolean vals to log
   */
  private static void printConfig(String var, boolean val, String des) {
    printConfig(var, Boolean.toString(val), des);
  }
}
