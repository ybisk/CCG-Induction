package CCGInduction.evaluation;

/**
 * Allowable equivalencies between a pair of edges.
 * System:  (X, Y, Category, argument)
 * Gold:    (A, B, GoldCat,  GoldArg)
 */
public enum EvalMode {
  /** X==A && Y==B  ||  X==B && Y==A */
  Undirected,
  /** X==A  && Y == B */
  Directed,
  /** X==A  && Y == B  && argument == GoldArg */
  Argument,
  /** X==A  && Y == B  && Category == GoldCat && arg == GoldArg */
  Labeled,
  /** X==A  && Y == B  && Category == noFeats(GoldCat) && arg == GoldArg */
  NoFeatures,
  /** X==A  && Y == B  && Category == noFeats(GoldCat) && arg == GoldArg [offset by cat] */
  Simplified,
  /** Simplified where we remove all features but DCL */
  SimplifiedDCL,
  /** Perform all of the above comparisons */
  All
}