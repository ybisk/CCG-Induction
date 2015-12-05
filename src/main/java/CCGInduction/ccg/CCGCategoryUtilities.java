package CCGInduction.ccg;

import CCGInduction.Configuration;
import CCGInduction.parser.AUTO_TYPE;
import CCGInduction.utils.Logger;

import java.util.ArrayList;
import java.util.regex.Pattern;

public class CCGCategoryUtilities {
  // Compiled Regular expressions
  private static final Pattern bwdDot = Pattern.compile("\\\\\\.");
  private static final Pattern fwdDot = Pattern.compile("/\\.");
  public static final Pattern lexical_cat = Pattern.compile("\\(<L");
  public static final Pattern close_lexical = Pattern.compile(">\\)");
  // Simplify Categories
  private static final Pattern dcl_keeps_its_subject = Pattern.compile("S\\[dcl\\]\\\\N");
  private static final Pattern remove_features = Pattern.compile("\\[.+?\\]");
  private static final Pattern replace_NP_with_N = Pattern.compile("NP");
  private static final Pattern SbN_to_S = Pattern.compile("\\(S\\\\N\\)");
  private static final Pattern D_to_S = Pattern.compile("D");

  /**
   * Get a string[] of categories from an AUTO file produced by this code.
   * Notable distinctions being modifier dots and no category simplification
   * @param str LISP style parse
   * @return Category String[]
   */
  public static String[] AUTOtoCATS(String str){
    return AUTOtoIndex(str, 1);
  }

  /**
   * Get a string[] of tags from an AUTO file produced by this code.
   * @param str LISP style parse
   * @return Category String[]
   */
  public static String[] AUTOtoTags(String str){
    return Configuration.auto_type.equals(AUTO_TYPE.CANDC) ? AUTOtoIndex(str, 4) : AUTOtoIndex(str, 2);
  }

  /**
   * Get a string[] of words from an AUTO file produced by this code.
   * @param str LISP style parse
   * @return Category String[]
   */
  public static String[] AUTOtoWords(String str) {
    return Configuration.auto_type.equals(AUTO_TYPE.CANDC) ? AUTOtoIndex(str, 2) : AUTOtoIndex(str, 4);
  }

  private static String[] AUTOtoIndex(String str, int index){
    str = str.trim();
    String[] pieces = lexical_cat.split(str);
    String chunk;
    ArrayList<String> accepted = new ArrayList<>();
    String piece;
    for (int i = 1; i < pieces.length; ++i ) {
      piece = pieces[i];
      chunk = Logger.whitespace_pattern.split(piece)[index];
      chunk = bwdDot.matcher(chunk).replaceAll("\\\\");
      chunk = fwdDot.matcher(chunk).replaceAll("/");
      accepted.add(chunk);
    }
    return accepted.toArray(new String[accepted.size()]);
  }

  public static String[][] AUTOtoTokens(String str) {
    str = str.trim();
    String[] pieces = lexical_cat.split(str);
    ArrayList<String[]> accepted = new ArrayList<>();
    String piece;
    String[] chunks;
    for (int i = 1; i < pieces.length; ++i ) {
      piece = pieces[i];
      String[] wordTagCategory = new String[3];
      chunks = Logger.whitespace_pattern.split(piece);
      wordTagCategory[0] = chunks[4];
      wordTagCategory[1] = chunks[2];
      wordTagCategory[2] = chunks[1];
      wordTagCategory[2] = bwdDot.matcher(wordTagCategory[2]).replaceAll("\\\\");
      wordTagCategory[2] = fwdDot.matcher(wordTagCategory[2]).replaceAll("/");
      accepted.add(chunks);
    }
    return accepted.toArray(new String[accepted.size()][]);
  }


  /**
   * Produces tag sequence and simplifies the categories
   * @param line AUTO formatted string
   * @return Category String[]
   */
  public static String[] GoldAUTOtoCATS(String line){
    String[] cats = AUTOtoCATS(line);
    for(int i = 0; i < cats.length; ++i) {
      //cats[i] = simplifyCCG(cats[i]);
      cats[i] = dropArgNoFeats(cats[i]);
    }
    return cats;
  }

  /**
   * Simplify CCGcat to remove long-range deps.
   * @param category Treebank Category
   * @return Simplified category
   */
  public static String simplifyCCG(String category) {
    category = replace_NP_with_N.matcher(category).replaceAll("N");
    category = dcl_keeps_its_subject.matcher(category).replaceAll("D\\\\N");
    category = remove_features.matcher(category).replaceAll("");
    category = SbN_to_S.matcher(category).replaceAll("S");
    category = D_to_S.matcher(category).replaceAll("S");
    return category;
  }

  public static int simplifyCCG(String category, int argument) {
    InducedCAT fullCategory = InducedCAT.valueOf(category);
    String category_simplified = simplifyCCG(category);
    InducedCAT simpleCategory = InducedCAT.valueOf(category_simplified);

    if (fullCategory.numberOfArguments() == simpleCategory.numberOfArguments())
      return argument;
    else
      return argument - 1;
  }

  /**
   * Remove features and NP from CCG category.
   * @param category Treebank Category
   * @return Simplified category
   */
  public static String dropArgNoFeats(String category) {
    category = convertChinese(category);
    category = replace_NP_with_N.matcher(category).replaceAll("N");
    category = SbN_to_S.matcher(category).replaceAll("S");
    category = remove_features.matcher(category).replaceAll("");
    return category;
  }

  /**
   * Chinese CCGbank differs in M and QP
   */
  private static String convertChinese(String category) {
    category = category.replaceAll("M", "N[num]");
    category = category.replaceAll("QP", "NP");
    category = category.replaceAll("LCP", "PP[lcp]");
    return category;
  }

  /**
   * Change the argument number if the simplified category has n-1 args
   * @param category  Treebank Category
   * @param argument  Gold Argument number
   * @return New Argument number
   */
  public static int dropArgNoFeats(String category, int argument) {
    category = convertChinese(category);
    InducedCAT fullCategory = InducedCAT.valueOf(category);
    String category_simplified = dropArgNoFeats(category);
    InducedCAT simpleCategory = InducedCAT.valueOf(category_simplified);

    if (fullCategory.numberOfArguments() == simpleCategory.numberOfArguments())
      return argument;
    else
      return argument - 1;
  }

  /**
   * Remove features and NP from CCG category.
   * @param category Treebank Category
   * @return Simplified Category
   */
  public static String noFeats(String category) {
    category = remove_features.matcher(category).replaceAll("");
    category = replace_NP_with_N.matcher(category).replaceAll("N");
    return category;
  }

  private static final Pattern SemMatch = Pattern.compile("\\{(X|Y|Z|T|U|V|W|_)(\\*?)\\}");
  private static final Pattern ArgMatch = Pattern.compile("<[1-9]>");
  private static final Pattern RemoveX = Pattern.compile("\\[X\\]");
  public static String cleanSemanticsFromCategory(String category) {
    category = SemMatch.matcher(category).replaceAll("");
    category = replace_NP_with_N.matcher(category).replaceAll("N");
    category = ArgMatch.matcher(category).replaceAll("");

    category = RemoveX.matcher(category).replaceAll("");
    //category = SbN_to_S.matcher(category).replaceAll("S");

    //category = remove_features.matcher(category).replaceAll("");
    if (category.charAt(0) == '(' && category.charAt(category.length()-1) == ')') {
      category = category.substring(1, category.length()-1);
    }
    return category;
  }

  /**
   * Checks if the given argument is of the same type in both cases.  This gives points for getting the
   * subject of a verb right even if the object is missing.  S\N  (S\N)/N
   * @param goldArg   index we're checking in gold category
   * @param goldCategory  Gold category
   * @param systArg   index we're checking in system category
   * @param systCategory  Syst Predicted category
   * @return If categories match
   */
  public static boolean ArgumentTypesMatch(int goldArg, String goldCategory, int systArg, String systCategory,
                                           boolean syst_has_feats) {
    // Arg N  \/   X/N  N
    // Arg S  \/   X/S  S
    // Mod N       N   N\N...
    // Mod S       S   S\S...
    // Mod N|N
    // Mod S|S

    InducedCAT goldCAT = InducedCAT.valueOf(simplifyCCG(goldCategory));
    goldArg = simplifyCCG(goldCategory, goldArg);
    if (goldCAT == null || goldArg == 0)
      return false;

    InducedCAT systCAT = syst_has_feats ? InducedCAT.valueOf(simplifyCCG(systCategory))
                                        : InducedCAT.valueOf(noFeats(systCategory));
    systArg = syst_has_feats ? simplifyCCG(systCategory, systArg) : systArg;
    if (systCAT == null || systArg == 0)
      return false;

    return
        goldCAT.Arg(goldArg).equals(systCAT.Arg(systArg))
        && goldCAT.Mod(goldArg) == systCAT.Mod(systArg);
  }

  public static boolean softEquality(InducedCAT first, InducedCAT second) {
    return (first == null || second == null) ? (second == null && first == null) :
        removeFeatures(first.copy()).equals(removeFeatures(second.copy()))
        || (InducedCAT.NP(first) && InducedCAT.N(second))   // Allow equality between N and NP
        || (InducedCAT.N(first) && InducedCAT.NP(second));
  }

  public static boolean softNPEquality(InducedCAT first, InducedCAT second) {
    if (first == null || second == null)
      return false;
    InducedCAT first_noF = removeFeatures(first.copy());
    InducedCAT second_noF = removeFeatures(second.copy());
    return (InducedCAT.NP(first_noF) || InducedCAT.N(first_noF))   // Allow equality between N and NP
            && (InducedCAT.N(second_noF) || InducedCAT.NP(second_noF));
  }

  public static boolean softEqualityNoConj(InducedCAT first, InducedCAT second) {
    first = first.copy();
    first.has_conj = false;
    second = second.copy();
    second.has_conj = false;
    return softEquality(first, second);
  }

  private static InducedCAT removeFeatures(InducedCAT category) {
    category.CCGbank_feature = null;
    category.modifier = false;
    if (category.Arg != null)
      removeFeatures(category.Arg);
    if (category.Res != null)
      removeFeatures(category.Res);
    return category;
  }
}
