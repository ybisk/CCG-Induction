package CCGInduction.ccg;

/**
 * Types of CoNLL dependency formuation categorized by their treatment of conjunction
 */
public enum CoNLLDependency {
  /** Print no dependencies */
  None,
  /** CC --> X1    CC --> X2   */
  CC_X1___CC_X2,
  /** X1 --> CC    X1 --> X2   */
  X1_CC___X1_X2,
  /** X1 --> CC    CC --> X2   */
  X1_CC___CC_X2,
  /** X1 --> X2    X2 --> CC   */
  X1_X2___X2_CC,
  /** X2 --> X1    X2 --> CC   */
  X2_X1___X2_CC
}
