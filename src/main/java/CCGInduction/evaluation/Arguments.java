package CCGInduction.evaluation;

/**
 * Simple class which maintains and parses the command line arguments
 * passed to the evaluation scripts
 */
public final class Arguments {
  public String gold_file = "";
  public String syst_file = "";
  public boolean verbose = false;
  public boolean verbose_length = false;
  public boolean verbose_depLength = false;
  public boolean oracle = false;
  public boolean invalid = true;
  public boolean confusion = false;
  public boolean system_has_features = false;
  public EvalMode mode = EvalMode.All;
  public int maxLength = Integer.MAX_VALUE;
  public boolean removePunct = false;

  Arguments(String[] args) {
    String[] split;
    for (String str : args) {
      split = str.split("=");
      switch (split[0]) {
        case "gold":
          gold_file = split[1];
          break;
        case "system":
          syst_file = split[1];
          break;
        case "-v":
          verbose = true;
          break;
        case "-vl":
          verbose_length = true;
          break;
        case "-vdl":
          verbose_depLength = true;
          break;
        case "-o":
          oracle = true;
          break;
        case "-m":
          mode = EvalMode.valueOf(split[1]);
          break;
        case "-l":
          maxLength = Integer.valueOf(split[1]);
          break;
        case "-systFeats":
          system_has_features = true;
          break;
        case "-confusion":
          confusion = true;
          break;
        case "-removePunct":
          removePunct = true;
          break;
        default:
          System.err.println("Ignoring unknown argument: " + split[0]);
          break;
      }
    }

    if (syst_file.isEmpty()) {
      System.out.println("java <Evaluation> gold=<gold file>  system=<system file> [-v -o -m=<mode>]");
      return;
    }
    invalid = false;
  }

  public Arguments(){}
}
