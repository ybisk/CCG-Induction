package CCGInduction.experiments;

/**
 * Specify Experiment type
 * 
 * @author bisk1
 */
public enum Training {
  /** Grammar Induction ( UnsupervisedInduction ) */
  induction,
  /** Reads in CCGbank */
  supervised,
  /** Trains an HMMM */
  tagInduction,
}
