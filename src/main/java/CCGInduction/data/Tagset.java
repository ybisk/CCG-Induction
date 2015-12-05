package CCGInduction.data;

import CCGInduction.utils.Logger;
import CCGInduction.utils.TextFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Specifices the tagset. This includes defining classes.
 * 
 * @author bisk1
 */
public class Tagset {
  /**
   * String representations of the tags
   */
  public final static ArrayList<String> STRINGS = new ArrayList<>();
  /**
   * A None tag.
   */
  final static POS NONE = new POS("NONE");
  /**
   * Verbs
   */
  private static POS[] verbs = new POS[0];
  /**
   * Verbs allowed to be produced by TOP
   */
  public static POS[] verbsTOP = new POS[0];
  /**
   * Nouns
   */
  private static POS[] nouns = new POS[0];
  /**
   * QP
   */
  private static POS[] QPs = new POS[0];
  /**
   * M
   */
  private static POS[] Ms = new POS[0];
  /**
   * Conjunctions
   */
  public static POS[] conj = new POS[] {}; //new POS(","),new POS(";"), new POS(":")
  /**
   * Punctuation
   */
  public static POS[] punct = new POS[] { new POS(":"), // new POS(",")
    new POS("."), new POS("?"), new POS("!"),
    //  Unicode for new POS("»"), new POS("«"), new POS("“"), new POS("”"),
    new POS(String.valueOf(Character.toChars('\u00BB'))),
    new POS(String.valueOf(Character.toChars('\u00AB'))),
    new POS(String.valueOf(Character.toChars('\u201C'))),
    new POS(String.valueOf(Character.toChars('\u201D'))),
    new POS("("), new POS(")"),
    new POS("{"), new POS("}"), new POS("["),
    new POS("]"), new POS("<"), new POS(">"),
    new POS("-"), new POS("--"), new POS("``"),
    new POS("\'\'"), new POS("\"") };
  /**
   * Full tagset
   */
  public static POS[] tags = new POS[0];
  /**
   * Prepositions
   */
  public static POS[] prep = new POS[0];

  /**
   * Blank constructor used with Induced tagsets
   */
  public Tagset() {}

  /**
   * Reads tagset from (filename). Each line is of the form: <br>
   * Format:
   * <table>
   * <tr>
   * <td><b>tag</b></td>
   * <td><b>class</b></td>
   * <td><b>comment/information</b></td>
   * </tr>
   * <tr>
   * <td>NNP</td>
   * <td>noun</td>
   * <td>// Proper noun, singular/td>
   * </tr>
   * </table>
   * where class = noun, verb, func, punct
   * 
   * @param br Reader to a file with mappings
   * @throws IOException
   */
  public static void readTagMapping(BufferedReader br) throws IOException {
    String strLine;
    while ((strLine = br.readLine()) != null) {
      strLine = strLine.split("//")[0];
      if (strLine.length() > 0) {
        String[] vals = strLine.split("\\s+");

        // Case 1: Bracketing [ ] bracket
        if (vals.length == 3 && vals[2].equals("bracket")) {
          POS left = new POS(vals[0]);
          POS right = new POS(vals[1]);
          punct = add(punct, left);
          punct = add(punct, right);
          tags = add(tags, left);
          tags = add(tags, right);
        } else {
          POS tag = new POS(vals[0]);
          tags = add(tags, tag);
          for (int i = 1; i < vals.length; i++) {
            if (vals[i].equals("verb")) {
              verbs = add(verbs, tag);
            }
            if (vals[i].equals("top")) {
              verbsTOP = add(verbsTOP, tag);
            }
            if (vals[i].equals("noun")) {
              nouns = add(nouns, tag);
            }
            if ((vals[i].equals("QP")))
              QPs = add(QPs, tag);
            if ((vals[i].equals("M")))
              Ms = add(Ms, tag);
            if (vals[i].equals("conj")) {
              conj = add(conj, tag);
            }
            if (vals[i].equals("punct")) {
              punct = add(punct, tag);
            }
            if (vals[i].equals("prep")) {
              prep = add(prep, tag);
            }
          }
        }
      }
    }
    // Allow all verbs as TOP if none are specifically specified
    if (verbsTOP.length == 0) {
      verbsTOP = Arrays.copyOf(verbs, verbs.length);
    }
    // print();
    br.close();
  }


    public static void readTagMapping(String filename) throws IOException {
      readTagMapping(TextFile.Reader(filename));
    }

  /**
   * Prints the current tagset and mappings to screen.
   */
  public static void print() {
    Logger.logln("POS Mapping:\n" + serialize());
  }

  /**
   * Return string representation of all tags
   * @return Serialized (human readable) string
   */
  public static String serialize() {
    return "verbs:\t" + simpleArray(verbs) + "\n" +
        "nouns:\t" + simpleArray(nouns) + "\n" +
        "QPs:\t" + simpleArray(QPs) + "\n" +
        "Ms:\t" + simpleArray(Ms) + "\n" +
        "TOP:\t" + simpleArray(verbsTOP) + "\n" +
        "conj:\t" + simpleArray(conj) + "\n" +
        "punct:\t" + simpleArray(punct) + "\n" +
        "prep:\t" + simpleArray(prep) + "\n" +
        "tags:\t" + simpleArray(tags) + "\n";
  }

  public static String simpleArray(POS[] array) {
    StringBuilder builder = new StringBuilder();
    for (POS pos : array) {
      builder.append(pos);
      builder.append(' ');
    }
    return builder.toString();
  }

  public static void deSerialize(String input) {
    String[] lines = input.split("\\n");
    String[] split;
    for (String line : lines) {
      split = Logger.whitespace_pattern.split(line);
      switch(split[0]){
        case "verbs:":
          verbs = add(verbs, split);
          break;
        case "QPs:":
          QPs = add(QPs, split);
          break;
        case "Ms:":
          Ms = add(Ms, split);
          break;
        case "nouns:":
          nouns = add(nouns, split);
          break;
        case "TOP:":
          verbsTOP = add(verbsTOP, split);
          break;
        case "conj:":
          conj = add(conj, split);
          break;
        case "punct:":
          punct = add(punct, split);
          break;
        case "prep:":
          prep = add(prep, split);
          break;
        case "tags:":
          tags = add(tags, split);
          break;
      }
    }
  }

  public static POS[] add(POS[] set, String[] newTags) {
    for (int i = 1; i < newTags.length; ++i) {
      set = add(set, new POS(newTags[i]));
    }
    return set;
  }

  /**
   * Adds the given tag (newTag) to the provided list (currentTags) if it isn't already
   * present
   * 
   * @param currentTags Existing POS tags
   * @param newTag new POS tags
   * @return updated array
   */
  public static POS[] add(POS[] currentTags, POS newTag) {
    if (!contains(currentTags, newTag)) {
      POS[] temp = Arrays.copyOf(currentTags, currentTags.length + 1);
      temp[temp.length - 1] = newTag;
      return temp;
    }
    return currentTags;
  }

  private static POS[] remove(POS[] takeFrom, POS t) {
    if (contains(takeFrom, t)) {
      POS[] temp = new POS[takeFrom.length - 1];
      int i = 0;
      for (POS test : takeFrom) {
        if (!test.equals(t)) {
          temp[i] = test;
          i++;
        }
      }
      return temp;
    }
    return takeFrom;
  }

  /**
   * Adds tag as punctuation
   * @param tag new Punctuation tag
   */
  static void addPunctuationTag(POS tag) {
    punct = add(punct, tag);
    tags = add(tags, tag);
  }

  /**
   * Creates a new tag from String.
   * @param tagAsString POS tag as string
   * @return Hashed value
   */
  static int add(String tagAsString) {
    int i = STRINGS.indexOf(tagAsString);
    if (i == -1) {
      STRINGS.add(tagAsString);
      return STRINGS.size() - 1;
    }
    return i;
  }

  /**
   * Returns the POS value of a string if present.  Returns null otherwise.
   * @param tagAsString human readable POS
   * @return POS object for tag
   */
  public static POS valueOf(String tagAsString){
    if (!STRINGS.contains(tagAsString)) {
      return null;
    }
    return new POS(tagAsString);
  }

  /**
   * Checks if a given array of tags contains the query
   * 
   * @param currentTags Existing POS tags
   * @param queryTag Tag to check
   * @return If tag is already contained
   */
  static boolean contains(POS[] currentTags, POS queryTag) {
    if (queryTag == null) {
      return false;
    }
    for (POS comp : currentTags) {
      if (comp.equals(queryTag)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns array of verbs
   * @return POS[] of verbs
   */
  public static POS[] verb() {
    return verbs;
  }

  /**
   * Checks if a tag is a verb
   * @param queryTag Tag to check
   * @return If verb
   */
  public static boolean verb(POS queryTag) {
    return contains(verbs, queryTag);
  }

  /**
   * Returns array of nouns
   * @return POS[] of nouns
   */
  public static POS[] noun() {
    return nouns;
  }
  public static POS[] QP() {
    return QPs;
  }
  public static POS[] M() {
    return Ms;
  }

  /**
   * Checks if a tag is a noun
   * @param queryTag Tag to check
   * @return If noun
   */
  public static boolean noun(POS queryTag) {
    return contains(nouns, queryTag);
  }

  /**
   * Returns the full set of tags
   * @return POS[]
   */
  public static POS[] tags() {
    return tags;
  }

  /**
   * Checks if a tag is a conjunction
   * @param queryTag Tag to check
   * @return If conjunction
   */
  public static boolean CONJ(POS queryTag) {
    return contains(conj, queryTag);
  }

  /**
   * Checks if a tag is punctuation
   * @param queryTag Tag to check
   * @return if Punctuation
   */
  public static boolean Punct(POS queryTag) {
    return contains(punct, queryTag);
  }

  /**
   * Removes a tag from all categories
   * @param tag Tag to remove
   */
  static void remove(POS tag) {
    tags = remove(tags, tag);
    verbs = remove(verbs, tag);
    verbsTOP = remove(verbsTOP, tag);
    nouns = remove(nouns, tag);
    conj = remove(conj, tag);
    punct = remove(punct, tag);
    prep = remove(prep, tag);
  }

  /**
   * Checks if a verb is allows to be produced by TOP
   * 
   * @param queryTag Tag to check
   * @return If TOP can go to this tag
   */
  public static boolean verbTOP(POS queryTag) {
    return contains(verbsTOP, queryTag);
  }

  /**
   * Converts a number to a simplified form
   * 
   * @param word Number to simplify
   * @return Simplified string
   */
  public static String convertNumber(String word) {
    if (word.length() == 4 && (word.startsWith("19") || word.startsWith("20"))) {
      return "X-YEAR-X";
    } else if (word.contains("/")) {
      return "X-FRAC-X";
    } else if (word.contains(":")) {
      return "X-TIME-X";
    } else {
      return word.replaceAll("[0-9]", "X");
    }
  }

  /**
   * Types of Tags
   * 
   * @author bisk1
   */
  public enum TAG_TYPE {
    /**
     * Reduced tagset
     */
    Coarse,
    /**
     * Standard tagset
     */
    Fine,
    /**
     * Defined by Slav Petrov
     */
    Universal,
    /**
     * Induced via Baum-Welch code
     */
    Induced,
    /**
     * Non-Standard
     */
    Custom
  }
}
