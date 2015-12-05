package CCGInduction.experiments;

import CCGInduction.models.Model;

/**
 * Defines the set of actions that can be performed by an Experiment
 *
 * @author bisk1
 */
public enum Action {
  // Induction
  /** Merges the Training and Testing data */
  MergeTrainTest,
  /** Perform a round of induction on POS tags */
  I,
  /** Perform a round of induction on POS tags and against trees */
  ITr,
  /** Induce Tags ( Baum-Welch ) {@link UnsupervisedInduction} */
  InduceTags,
  /** Loads a lexicon that was (presumably) induced in a previous run */
  LoadLexicon,
  /** Run Baum-Welch updates until convergence */
  BW,
  /** Read Training files */
  readTrainingFiles,
  /** Parse with Application */
  B0,
  /** Parse with Composition: B<sup>1</sup> */
  B1,
  /** Parse, only Modifiers can compose */
  B1Mod,
  /** Parse, only Modifiers and TypeRaised categories can compose */
  B1ModTR,
  /** Parse with B<sup>2</sup> */
  B2,
  /** Parse with B<sup>2</sup>, only for Modifiers categories */
  B2Mod,
  /** Parse with B<sup>2</sup>, only for Modifiers and TR categories */
  B2ModTR,
  /** Parse with B<sup>3</sup> for modifiers (e.g. adv + ditransitive),
   *  Parse with B<sup>2</sup> for Type-Raised categories,
   *  Parse with B<sup>0</sup> for anything else */
  B3Mod_B2TR_B0Else,
  /** Allow for any arity/complexity */
  Supervised,
  SupervisedTest,
  /** Train with Inside-Outside {@link Model#InsideOutside} **/
  IO,
  /** Run Test **/
  Test,
  /** Simply prints a model (loaded or otherwise) to human readable files */
  PrintModel,
  /** Begin emitting words rather than POS tags */
  lexicalize,
  /** Add files to training set */
  GrowTrainingSet,
  /** Eliminates lexical categories based on KL divergence and Conditional Probabilities */
  PruneLexicon,
  RemapTags,
  /** Eliminates lexical emissions from generative distribution */
  TrimDistributions,

  // Model types
  // MLE
  /** Don't factor, simply pretend binary children are a single CFG outcome */
  PCFGModel,
  /** Simple head factored CCG model:  BH 12*/
  AAAI12Model,
  /** Factorizes CCG according to argument Y:  P(Y | Parent)*/
  ArgumentModel,

  // HDP
  /** BH TACL 2013 */
  HDPArgumentModel,
  HDPArgumentModelBabySRLSupervised,

  // Model saving/loading
  Save, Viterbi, LoadWeightedLexicon, Load,

  // Special case for   S\N  to N\N
  readTypeChanging,
}
