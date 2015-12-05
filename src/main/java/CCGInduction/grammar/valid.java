package CCGInduction.grammar;

/**
 * Constants for indicating proper usage of a rule based on experiment specific
 * criterion
 * 
 * @author bisk1
 */
public enum valid {
  /**
   * Can be used during parsing
   */
  Valid,
  /**
   * Should be ignored
   */
  Invalid,
  /**
   * A rule that has been introduced but not yet evaluated
   */
  Unknown,
  /**
   * Unused rule
   */
  Unused;

  public static valid max(valid one, valid two) {
    if (one == Valid || two == Valid)
      return Valid;
    if (one == Invalid || two == Invalid)
      return Invalid;
    if (one == Unused || two == Unused)
      return Unused;
    return Unknown;
  }
}

