package CCGInduction.grammar;

/**
 * Specifices the set of supported Normal-Forms
 *
 * @author bisk1
 */
public enum Normal_Form {
  /**
   * Full Normal-From Sec 4 - COLING 2010 Hockenmaier and Bisk
   */
  Full,
  /**
   * Sec 4 without punctuation
   */
  Full_noPunct,
  /**
   * Eisner's Normal-Form - as corrected by COLING paper
   */
  Eisner,
  /**
   * Eisner's original Normal-Form
   */
  Eisner_Orig,
  /**
   * No normal-form constraints
   */
  None
}
