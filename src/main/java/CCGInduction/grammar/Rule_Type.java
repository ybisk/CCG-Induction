package CCGInduction.grammar;

/**
 * Rule types for use in the grammar
 * 
 * @author bisk1
 */
public enum Rule_Type {
  /**
   * X PUNC --> X ( Head Left)
   */
  BW_PUNCT,
  /**
   * Y\Z X\Y --> X \ Z ( Head depends if modifier )
   */
  BW_COMPOSE,
  /**
   * (Y|Z)|Z' X\Y --> (X|Z)|Z'
   */
  BW_2_COMPOSE,
  /**
   * ((Y|Z)|Z')|Z'' X\Y --> ((X|Z)|Z')Z''
   */
  BW_3_COMPOSE,
  /**
   * (((Y|Z)|Z')|Z'')|Z''' X\Y --> (((X|Z)|Z')|Z'')|Z'''
   */
  BW_4_COMPOSE,
  /**
   * ((((Y|Z)|Z')|Z'')|Z''')|Z'''' X\Y --> ((((X|Z)|Z')|Z'')|Z''')|Z''''
   */
  BW_5_COMPOSE,
  /**
   * Y/Z X\Y --> X / Z
   */
  BW_XCOMPOSE,
  /**
   * X X[conj] ---> X ( Head Left )
   */
  BW_CONJOIN,
  /**
   * Y X\Y --> X
   */
  BW_APPLY,
  /**
   * S\(S/N) ---> N
   */
  BW_TYPERAISE,
  /**
   * Produce lexical item
   */
  PRODUCTION,
  /**
   * S/(S\N) ---> N
   */
  FW_TYPERAISE,
  /**
   * X/Y Y ---> X
   */
  FW_APPLY,
  /**
   * conj X ---> X ( Head Right )
   */
  FW_CONJOIN,
  /**
   * X/Y Y/Z ---> X/Z
   */
  FW_COMPOSE,
  /**
   * X/Y (Y|Z)|Z' --> (X|Z)|Z'
   */
  FW_2_COMPOSE,
  /**
   * X/Y ((Y|Z)|Z')|Z'' --> ((X|Z)|Z')|Z''
   */
  FW_3_COMPOSE,
  /**
   * X/Y (((Y|Z)|Z')|Z'')|Z''' --> (((X|Z)|Z')|Z'')|Z'''
   */
  FW_4_COMPOSE,
  /**
   * X/Y ((((Y|Z)|Z')|Z'')|Z''')|Z'''' --> ((((X|Z)|Z')|Z'')|Z''')|Z''''
   */
  FW_5_COMPOSE,
  /**
   * X/Y Y\Z ---> X\Z
   */
  FW_XCOMPOSE,
  /**
   * PUNC X ---> X
   */
  FW_PUNCT,
  /**
   * Swap N, NP, etc
   */
  TYPE_CHANGE,
  /**
   * TOP -> S
   */
  TYPE_TOP,
  //TO_TYPE_TOP,
  /**
   * The actual lexical item
   */
  LEX,
  /**
   * NULL
   */
  NULL,
  /**
   * CCGbank's CCG breaking rules
   */
  FW_TYPECHANGE,
  BW_TYPECHANGE,
  FW_SUBSTITUTION;

  /**
   * Checks if typeraising
   */
  public static boolean TR(Rule_Type type) {
    return type == Rule_Type.FW_TYPERAISE || type == Rule_Type.BW_TYPERAISE;
  }
}