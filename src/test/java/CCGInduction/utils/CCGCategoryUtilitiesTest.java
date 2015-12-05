package CCGInduction.utils;

import CCGInduction.ccg.CCGCategoryUtilities;
import junit.framework.TestCase;

import java.util.Arrays;

public class CCGCategoryUtilitiesTest extends TestCase {
  // Example sentences:
  // I          yearn                     to              learn   CCG
  // N           S/.S                   S/.S            (S\N)/N    N
  // N (S[dcl]\NP)/(S[to]\NP)  (S[to]\NP)/(S[b]\NP) (S[b]\NP)/NP   N

  public void testAUTOtoCATS() throws Exception {
    String[] SuperTags = new String[] {"N", "S/S", "S/S", "(S\\N)/N", "N"};
    String AUTO = "(<T S 1 2> " +
        "(<L N PRP PRP I NP>) (<T S\\N 1 2> "+
        "(<L S/.S VBP VBP yearn S/S>) (<T S\\N 1 2> "+
        "(<L S/.S TO TO to S/S_46>) (<T S\\N 1 2> "+
        "(<L (S\\N)/N VB VB learn (S\\N_69)/N_70>)" +
        "(<L N NNP NNP CCG N>) ) ) ) )";
    String[] converted = CCGCategoryUtilities.AUTOtoCATS(AUTO);
    assertTrue(Arrays.equals(SuperTags, converted));
  }

  public void testGoldAUTOtoCATS() throws Exception {
    String[] SuperTags = new String[] {
        "N", "(S\\N)/(S\\N)", "(S\\N)/(S\\N)", "(S\\N)/N", "N"};
    String AUTO = "(<T S[dcl] 1 2> " +
        "(<L NP PRP PRP I NP>) (<T S[dcl]\\NP 1 2> "+
        "(<L (S[dcl]\\NP)/(S[to]\\NP) VBP VBP yearn (S[dcl]\\NP_1)/(S[to]_2\\NP_3:B)>) (<T S[to]\\NP 1 2> "+
        "(<L (S[to]\\NP)/(S[b]\\NP) TO TO to (S[to]\\NP_45)/(S[b]_46\\NP_45:B)_46>) (<T S[b]\\NP 1 2> "+
        "(<L (S[b]\\NP)/NP VB VB learn (S[b]\\NP_69)/NP_70>) (<T NP 0 1> "+
        "(<L N NNP NNP CCG N>) ) ) ) ) )";
    String[] converted = CCGCategoryUtilities.GoldAUTOtoCATS(AUTO);
    assertTrue(Arrays.equals(SuperTags, converted));
  }

  public void testSimplifyCCG() throws Exception {
    // Only simplifies categories that are not [dcl]
    String CCGbank = "(S[dcl]\\NP)/(S[to]\\NP)";
    assertEquals("(S\\N)/S", CCGCategoryUtilities.simplifyCCG(CCGbank));
  }

  public void testDropArgNoFeats() throws Exception {
    String Complex = "(S[dcl]\\NP)/(S[to]\\NP)";
    assertEquals("(S\\N)/(S\\N)", CCGCategoryUtilities.dropArgNoFeats(Complex));
    // Simplifies categories that don't have features
    String Complex_noFeats = "(S\\NP)/(S\\NP)";
    assertEquals("S/S", CCGCategoryUtilities.dropArgNoFeats(Complex_noFeats));
  }

  public void testDropArgNoFeats1() throws Exception {
    String Complex = "(S[to]\\NP)/(S[b]\\NP)";
    int Argument = 2;  // Corresponding to (S[b]\NP)
    assertEquals(2, CCGCategoryUtilities.dropArgNoFeats(Complex,Argument));

    String Complex_noFeats = "(S\\NP)/(S\\NP)";
    int Argument_noFeats = 2;  // Corresponding to (S\NP)
    assertEquals(1, CCGCategoryUtilities.dropArgNoFeats(Complex_noFeats,Argument_noFeats));
  }

  public void testNoFeats() throws Exception {
    String Complex = "(S[dcl]\\NP)/(S[to]\\NP)";
    assertEquals("(S\\N)/(S\\N)", CCGCategoryUtilities.noFeats(Complex));
    // Does not simplify categories
    String Complex_noFeats = "(S\\NP)/(S\\NP)";
    assertEquals("(S\\N)/(S\\N)", CCGCategoryUtilities.noFeats(Complex_noFeats));
  }

  public void testCleanSemanticsFromCategory() throws Exception {
    // Linguistic features
    String CandC = "((S[to]{_}\\NP{Z}<1>){_}/(S[b]{Y}<2>\\NP{Z*}){Y}){_}";
    assertEquals("(S[to]\\N)/(S[b]\\N)", CCGCategoryUtilities.cleanSemanticsFromCategory(CandC));
    // Features are null
    String CandCX = "((S[X]{_}\\NP{Z}<1>){_}/(S[X]{Y}<2>\\NP{Z*}){Y}){_}";
    assertEquals("(S\\N)/(S\\N)", CCGCategoryUtilities.cleanSemanticsFromCategory(CandCX));
  }
}