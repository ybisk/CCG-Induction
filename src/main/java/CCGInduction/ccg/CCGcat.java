package CCGInduction.ccg;

import CCGInduction.Configuration;

import java.io.PrintStream;
import java.util.StringTokenizer;


/**
 * An implementation of CCG categories as recursive data structures, and of the
 * combinatory rules as operations on these. At the moment, no logical form is
 * constructed. Instead, a list of word-word dependencies is generated.
 * <p/>
 * Each category has the following fields:
 * <ul>
 * <li>A <b>catString</b>, the string representation of the category.<br>
 * <li>An <b>id</b> number, which identifies this category token.<br>
 * Required as bookkeeping device for unification.
 * <li>A list of lexical heads <b>heads</b>, which is possibly null (in the case
 * of type-raising).
 * <li>A <b>HeadId</b> number, identifying the lexical heads; also bookkeeping
 * device for unification.
 * <li>A pointer <b>function</b> which points to the parent if this category is
 * part of a complex category, or to null otherwise.
 * <li>A list of <b>unfilled dependencies</b>, which is only non-null if the
 * category is an argument.
 * <li>A list of <b>filledDependencies</b>, which records the
 * dependencies filled by the last application of a combinatory rule. This is
 * used to compute the dependency probabilities during parsing.
 * </ul>
 * A complex category has additionally the following fields:
 * <ul>
 * <li>A <b>result</b> category
 * <li>An <b>argument</b> category
 * <li>An integer <b>argDir</b>, indicating the directionality of the argument.
 * </ul>
 * <p/>
 * This class implements methods which
 * <ul>
 * <li>read in a categorial lexicon from a file
 * <li>when creating a lexical category for a word, treat special cases of
 * lexical categories so that dependencies between argument slots can be
 * passed around.
 * <li>create new categories by performing the operations of the combinatory
 * rules on existing categories
 * <li>perform unification operations necessary to fill word-word
 * dependencies.
 * </ul>
 *
 * @author juliahmr
 */

public class CCGcat implements Cloneable {

  static final String NP = "NP";//TODO
  static final String NOUN = "N";
  static final String S = "S";
  static final String VP = "S\\NP";//TODO
  private static final String CONJFEATURE = "[conj]";
  /**
   * open brackets
   */
  private static final char OB = '(';
  /**
   * closing brackets
   */
  private static final char CB = ')';
  /**
   * forward slash
   */
  private static final char FSLASH = '/';
  /**
   * backward slash
   */
  private static final char BSLASH = '\\';

  /**
   * directionality of argument: forward
   */
  static final int FW = 0;
  /**
   * directionality of argument: backward
   */
  static final int BW = 1;
  /**
   * directionality of argument: unspecified
   */
  private static final int NODIR = -1;

  /**
   * a counter for category instances
   */
  private static int idCounter = 0;
  /**
   * a counter for head instances
   */

  private static int headIdCounter = 1;
  /**
   * the ID of this category -- unifiable categories which are part of the
   * same category have the same ID
   */
  private int id;
  /**
   * the ID of the head of this category. -- parts of ONE category with the
   * same head have the same headID. Coordination and substitution create a
   * new headID.
   */
  private int headId;
  /**
   * the string which denotes this category.
   */
  String catString;
  /**
   * the argument category. Null in an atomic category
   */
  private CCGcat argument;
  /**
   * the result category. Null in an atomic category
   */
  private CCGcat result;
  /**
   * a pointer to the 'parent' of this category
   */
  private CCGcat function;
  /**
   * forward or backward
   */
  private int argDir;
  /**
   * a list of dependencies which should hold for each of the elements of
   * heads
   */
  private DepList dependencies;
  /**
   * a list of the filled dependencies
   */
  private DepList filledDependencies;

  /**
   * a list of heads
   */
  private HeadWordList heads;
  private HeadWordList conjHeads;
  private boolean extracted;// long-range, really
  private boolean bounded;//
  private static final boolean DEBUG = false;

  // ##########################################################
  //
  // CONSTRUCTORS
  // ============
  //
  // ##########################################################

  /**
   * Creates a category from a string denoting the category. Called by
   * parseCat(String)
   */
  private CCGcat(String string) {
    id = newId();
    headId = newHeadId();
    catString = string;
    argument = null;
    result = null;
    argDir = NODIR;
    function = null;
    dependencies = null;
    heads = null;
    filledDependencies = null;
    extracted = false;
    bounded = true;
  }

  /**
   * Build up a category consisting of a result and argument category. This
   * does not clone the result and argument categories!!
   */
  private CCGcat(CCGcat resCat, CCGcat argCat) {
    id = newId();
    headId = -1; // dummy constructor
    catString = null;
    argument = argCat;
    argument.function = this;
    result = resCat;
    result.function = this;
    argDir = NODIR;
    dependencies = null;
    function = null;
    heads = resCat.heads();
    filledDependencies = null;
    extracted = false;
    bounded = true;
  }

  @Override
  public String toString() {
    return catString;
  }

  // ##########################################################
  //
  // ACCESS THE FIELDS
  //
  // ##########################################################

  /**
   * returns the catString -- the string representation of the category
   */
  public String catString() {
    return catString;
  }

  public String catStringIndexed() {
    if (argument == null) {
      return catString();
    }
    return catStringRecIndexedArgs();
  }

  // top-level method to return category where only indices that are not the
  // same as the head are printed.
  private String catStringRecIndexedArgs() {
    if (argument != null) {
      int head = this.headId;
      String argString = argument.catStringRecIndexedArgs(head);
      String resultString = result.catStringRecIndexedArgs(head);
      String slash = "/";
      if (argDir == BW) {
        slash = "\\";
      }
      return resultString + slash + argString;
    }
    return catString();

  }

  /**
   * print out the category with all head indices that are not the same as
   * "head". If an argument is extracted, this is indicated by :U (unbounded)
   * or :B (bounded). Examples: RelPron: (NP_11\NP_11)/(S[dcl]_12/NP_11:U)_12
   * what RelPron: (NP_11\NP_11)/(S[dcl]_12\NP_11:B)_12 what
   */
  private String catStringRecIndexedArgs(int head) {
    String thisCatString;
    String isBounded = null;
    if (this.extracted) {
      if (this.bounded) {
        isBounded = ":B";
      } else {
        isBounded = ":U";
      }
    }
    // A complex category...
    if (argument != null) {
      String argString = argument.catStringRecIndexedArgs(head);
      String resultString = result.catStringRecIndexedArgs(head);
      String slash = "/";
      if (argDir == BW) {
        slash = "\\";
      }
      // ... that does not require the head index:
      if (this.headId == head) {
        thisCatString = '(' + resultString + slash + argString + ')';
      } else {

        thisCatString = isBounded == null ? '('
            + resultString + slash + argString + ")_" + this.headId
            : '(' + resultString + slash + argString
            + ")_" + this.headId + isBounded;
      }

    }
    // An atomic category...
    // ...that does not require the head index:
    else if (this.headId == head) {
      thisCatString = catString();
      // ...that does require the head index:
    } else {
      thisCatString = isBounded == null ? catString() + '_'
          + this.headId : catString() + '_' + this.headId
          + isBounded;
    }
    return thisCatString;
  }

  /**
   * returns the catString without any features
   */
  private String catStringNoFeatures() {
    return noFeatures(catString);
  }

  /**
   * strips off atomic features from category string representations
   */
  private static String noFeatures(String catString) {
    int oIndex = catString.indexOf('[');
    if (oIndex > -1) {
      int cIndex;
      StringBuilder nofeatures = new StringBuilder(catString.substring(0,
          oIndex));
      cIndex = catString.indexOf(']', oIndex);
      oIndex = catString.indexOf('[', cIndex);
      while (cIndex > -1 && oIndex > -1) {
        nofeatures.append(catString.substring(cIndex + 1, oIndex));
        cIndex = catString.indexOf(']', oIndex);
        oIndex = catString.indexOf('[', cIndex);
      }
      if (oIndex == -1) {
        nofeatures.append(catString.substring(cIndex + 1));
      }
      return nofeatures.toString();
    }
    return catString;
  }

  private String indexedCatString() {
    return catStringIndexed();
  }

  /**
   * returns the list of filled dependencies *
   */
  public DepList filledDependencies() {
    return filledDependencies;
  }

  /**
   * returns the argument category of this category
   */
  private CCGcat argument() {
    return argument;
  }

  /**
   * returns the result category of this category
   */
  private CCGcat result() {
    return result;
  }

  /**
   * returns the list of dependencies defined by this category
   */
  private DepList dependencies() {
    return dependencies;
  }

  /**
   * returns the list of head words
   */
  public HeadWordList heads() {
    return heads;
  }

  /**
   * returns the list of head words
   */
  public HeadWordList conjHeads() {
    return conjHeads;
  }

  /**
   * returns the target category. If a category is atomic, it is its own
   * target. The target of a complex category is the target of its result
   * category. This is not encoded as a field, but is equally important!
   */
  private CCGcat target() {
    CCGcat target;
    if (result != null) {
      target = result.target();
    } else {
      target = this;
    }
    return target;
  }

  public static synchronized void resetCounters() {
    // TODO: this method is unsafe with newHeadId()
    /*
     * idCounter = 0; headIdCounter = 1;//reset the counters
     */
  }

  // ##########################################################
  //
  // CHANGE THE FIELDS
  //
  // ##########################################################

  /**
   * sets the dependencies to <tt>dep</tt>
   */
  private void setDeps(DepList dep) {
    if (dep != null) {
      dependencies = dep;
    }
  }

  private void setToExtractedBounded() {
    extracted = true;
    bounded = true;
  }

  private void setToExtractedUnbounded() {
    extracted = true;
    bounded = false;
  }

  /**
   * Prints the filled dependencies.
   */
  void printFilledCCGDeps(DepType depType) {
    System.out.print("###  Filled CCG Dependencies: ");
    if (filledDependencies != null) {
      if (depType.equals(DepType.CCG))
        filledDependencies.print();
      else
        filledDependencies.printDepGrammar();
    } else {
      System.out.println("<null>");
    }
  }

  /**
   * Apply all dependencies defined by <tt>deps</tt> to all heads in
   * <tt>depHeads</tt>. Filled dependencies are appended to
   * <tt>filledDeps</tt>
   */
  private DepList applyCCGDependencies(DepList deps, HeadWordList depHeads,
                                       DepList filledDeps) {
    DepList allDeps = null;
    HeadWordList h = depHeads;
    if (deps != null) {
      while (h != null) {
        DepList d = deps.copy();
        DepList tmp = d;
        while (tmp != null) {
          tmp.argWord = h.headWord();
          tmp.argCat = h.lexCat();
          tmp.argIndex = h.index();
          tmp = tmp.next();
        }
        if (allDeps == null) {
          allDeps = d;
        } else {
          allDeps.append(d);
        }
        h = h.next();
      }
      if (allDeps != null) {
        filledDeps = appendDeps(filledDeps, allDeps);
        allDeps = null;
      } else {
        allDeps = deps.copy();
      }
    }
    dependencies = allDeps;
    return filledDeps;
  }


  /**
   * standard append operation -- appends a copy of <tt>dep</tt> to
   * dependencies; if dependencies are null, copies <tt>dep</tt>.
   */
  private void appendDeps(DepList dep) {
    if (dep != null) {
      if (dependencies != null) {
        dependencies.append(dep.copy());
      } else {
        dependencies = dep.copy();
      }
    }
  }


  /**
   * standard append operation -- appends a copy of <tt>dep2</tt> to
   * <tt>dep1</tt>; if <tt>dep1</tt> is null, copies <tt>dep2</tt>.
   */
  private static DepList appendDeps(DepList dep1, DepList dep2) {
    if (dep2 != null) {
      if (dep1 != null) {
        dep1.append(dep2.copy());
      } else {
        dep1 = dep2.copy();
      }
    }
    return dep1;
  }

  /**
   * set the heads of this category to a copy of the HeadWordList given as
   * argument
   */
  public void setHeads(HeadWordList hw) {
    heads = hw.copy();
  }
  /**
   * set the heads of this category to a copy of the HeadWordList given as
   * argument
   */
  public void setConjHeads(HeadWordList hw) {
    conjHeads = hw.copy();
  }

  /**
   * Append the HeadWordList given as argument to the current head word list
   */
  private void appendHeads(HeadWordList hw) {
    if (hw != null) {
      if (heads != null) {
        heads.append(hw);
      } else {
        heads = hw.copy();
      }
    }
  }

  /**
   * like setHeads, but also applies recursively
   */
  private void setHeadsRec(HeadWordList hw) {
    setHeadsRec(hw, this.headId);
  }

  private void setHeadsRec(HeadWordList hw, int id) {
    if (hw == null) {
      return;
    }
    if (this.headId == id) {
      this.heads = hw.copy();
    }
    if (this.result != null) {
      this.result.setHeadsRec(hw, id);
    }
    if (this.argument != null) {
      this.argument.setHeadsRec(hw, id);
    }
  }

  // ##########################################################
  //
  // CREATE LEXICAL CATEGORIES
  //
  // ##########################################################
  public static CCGcat lexCat(String word, String cat, int index) {
    CCGcat lexCat = parseCat(cat);
    lexCat.adjustAdjs();
    if (word == null) {
      System.err.println("WORD == null;  Cat: " + cat);
      word = "xxx";
    }
    lexCat.heads = new HeadWordList(word, cat, index);
    lexCat.headId = newHeadId();// new lexical category
    lexCat.target().assignHeadsDeps(1, lexCat.heads, lexCat.headId);// lexCat
    lexCat.treatSpecialCases();
    return lexCat;
  }

  private void treatSpecialCases() {//TODO
    if (isAtomic()) {
      return;
    }
    if (DEBUG) {
      System.out.println("LexCat: special case? " + catString());
    }
    treatInducedModifier();
    treatInducedAuxModal();// (S|N_i)|(S|N_i)
    treatInducedRelPron(); // (N_i|N_i)|(S|N_i)
    if (DEBUG) {
      System.out.println("LexCat: " + indexedCatString());
    }
  }

  /**
   * creates a lexical category for a word
   */
  static CCGcat lexCat(String word, String cat) {
    System.out.println("LEXCAT " + cat + " for " + word);
    CCGcat lexCat = parseCat(cat);
    System.out.println("lexcat: " + lexCat.indexedCatString());
    lexCat.adjustAdjs();
    System.out.println("lexcat: " + lexCat.indexedCatString());
    if (word == null) {
      System.out.println("WORD == null;  Cat: " + cat);
      word = "xxx";
    }
    lexCat.heads = new HeadWordList(word, cat);
    lexCat.headId = newHeadId();// lexCat
    lexCat.target().assignHeadsDeps(1, lexCat.heads, lexCat.headId);// lexCat
    System.out.println("lexcat: " + lexCat.indexedCatString());
    lexCat.treatSpecialCases();
    return lexCat;
  }

  /**
   * If the argument is the same as the result category, make them equal
   */
  private void adjustAdjs() {
    if (argument != null && result != null) {
      argument.adjustAdjs();
      if (isAdjunctCat()) {// argument.catString.equals(result.catString)){
        if (DEBUG) {
          System.out.println("ADJUST ADJ " + catString());
        }
        argument.setHeadId(newHeadId());// adjustAdjs -- recursively set all headIds.
        result = argument.copy();// result is equal to argument
        result.dependencies = null;
      }
    }
  }

  /**
   * go from the target category outwards, and assign the head and appropriate
   * dependencies
   */
  private void assignHeadsDeps(int i, HeadWordList head, int headIdNumber) {
    headId = headIdNumber;// set the headId to this number
    if (heads == null) {
      heads = head;
    }

    // this constructor does not deal with lists of headwords,
    // since it only assigns the first element of heads.
    // but that is okay, since this is used for lexical categories only.
    if (argument != null) {
      argument.setHeadId(newHeadId());// also set the argument headID to a
      // new
      // number
      if (heads() != null) {
        argument.setDeps(new DepList("arg", heads(), null, i,
            argDir, isAdjunctOrDeterminerCat()));// record the direction of the argument as well
        i++;
      }
      // Adjuncts: an adjunct to a function passes the function on;
      // whereas an adjunct to an atomic category doesn't need to do that
      if (isAdjunctCat()) {
        // System.out.println("ADJUNCT CAT: " + catString());
        argument.setHeadId(newHeadId());// adjunct: also set argument headID to new.
        result = argument.copy();
      }
    }
    if (function != null) {
      function.assignHeadsDeps(i, head, headIdNumber);
    }
  }

  /**
   * A test for adjunct categories, which are defined here as categories whose
   * argument category string is identical to its result category string, and
   * which do not have any features other than [adj]. This doesn't work in all
   * generality, but does the job for the categories in CCGbank
   */
  private boolean isAdjunctCat() {
    boolean retval = false;
    if (argument != null && argument.catString.equals(result.catString)) {
      int index = argument.catString.indexOf('[');
      if (index > -1) {
        if (argument.catString.startsWith("[adj]", index)) {
          retval = true;
        }
      } else {
        retval = true;
      }
    }
    if (retval && isInducedAuxModal()) {
      retval = false;
    }
    return retval;

  }

  private boolean isAdjunctOrDeterminerCat() {
    boolean retval = false;
    if (catString.equals("NP[nb]/N") || isAdjunctCat()) {
      retval = true;
    }
    return retval;
  }

  private boolean isAtomic() {
    return result == null || argument == null;
  }


  // ##################################################
  // READ IN A CATEGORY FROM A STRING
  // ##################################################

  /**
   * parseCat(String cat) This works only if cat really spans an entire
   * category
   */
  private static CCGcat parseCat(String cat) {
    // Create a new category
    if (cat.endsWith(CONJFEATURE)) {// otherwise it might crash
      int index = cat.lastIndexOf(CONJFEATURE);
      cat = cat.substring(0, index);
    }

    CCGcat newCat = new CCGcat(cat);
    // CASE 1: No brackets
    if (cat.indexOf(OB) == -1 && cat.indexOf(CB) == -1) {

      // CASE 1(a): And no slashes
      if (cat.indexOf(FSLASH) == -1 && cat.indexOf(BSLASH) == -1) {
        // ==> newCat is atomic category
      }
      // CASE 1(b): a slash
      else {
        int slashIndex = 0;
        if (cat.indexOf(FSLASH) == -1 && cat.indexOf(BSLASH) != -1) {
          slashIndex = cat.indexOf(BSLASH);
          newCat.argDir = BW;
        }
        if (cat.indexOf(BSLASH) == -1 && cat.indexOf(FSLASH) != -1) {
          slashIndex = cat.indexOf(FSLASH);
          newCat.argDir = FW;
        }
        // Recurse on rescat
        CCGcat resCat = parseCat(cat.substring(0, slashIndex));
        resCat.function = newCat;
        newCat.result = resCat;
        // Recurse on argcat
        CCGcat argCat = parseCat(cat.substring(slashIndex + 1));
        argCat.function = newCat;
        newCat.argument = argCat;
      }
    }
    // CASE 2: Brackets
    else {
      int obNumber = 0; // the number of unclosed open brackets
      int start = 0; // the start of a new category
      int end = 0; // the end of a new category

      // Iterate through the characters in the string
      for (int i = 0; i < cat.length(); i++) {

        // If: this character is an open bracket
        // Then: if there are no other unclosed open brackets,
        // then: the next character starts a new category
        // - also: increment the number of unclosed open brackets
        if (cat.charAt(i) == OB) {
          if (obNumber == 0) {
            start = i + 1;
          }
          obNumber++;
        }

        // If: this character is a forward slash
        // and there are no unclosed open brackets
        // Then: this is the end of the result category
        if (cat.charAt(i) == FSLASH && obNumber == 0) {
          newCat.argDir = FW;
          if (newCat.result == null) {
            end = i;
            CCGcat resCat = parseCat(cat.substring(start, end));
            resCat.function = newCat;
            newCat.result = resCat;
          }
          start = i + 1;
          end = i + 1;
        }
        // If: this character is a backward slash
        // and there are no unclosed open brackets
        // Then: this is the end of the result category
        if (cat.charAt(i) == BSLASH && obNumber == 0) {
          newCat.argDir = BW;
          if (newCat.result == null) {
            end = i;
            CCGcat resCat = parseCat(cat.substring(start, end));
            resCat.function = newCat;
            newCat.result = resCat;
          }
          start = i + 1;
          end = i + 1;
        }
        // If this is a closing bracket:
        // Then: decrement the number of open unclosed brackets
        if (cat.charAt(i) == CB) {
          obNumber--;
          if (obNumber == 0) {
            end = i;
            if (newCat.result == null) {
              CCGcat resCat = parseCat(cat.substring(start, end));
              resCat.function = newCat;
              newCat.result = resCat;
            } else {
              CCGcat argCat = parseCat(cat.substring(start, end));
              argCat.function = newCat;
              newCat.argument = argCat;
            }
          }
        }
        // If this is the end of the string
        if (i == cat.length() - 1 && cat.charAt(i) != CB) {
          end = i + 1;

          if (newCat.result == null) {
            CCGcat resCat = parseCat(cat.substring(start, end));
            resCat.function = newCat;
            newCat.result = resCat;
          } else {
            CCGcat argCat = parseCat(cat.substring(start, end));
            argCat.function = newCat;
            newCat.argument = argCat;
          }
        }
      }
    }
    return newCat;
  }

  // ##################################################
  // PRINT CATEGORIES
  // ##################################################

  private void printCat(PrintStream out) {
    out.println("   |------------------------");
    printCatRec(1, out);
    out.println("   |------------------------");
    out.println();
  }

  void printCat() {
    printCat(System.out);

  }

  public String print() {
    String out = "";
    out += "   |------------------------\n";
    out = printCatRec(1, out);
    out += "   |------------------------\n";
    return out;
  }

  private String printCatRec(int offset, String out) {
    out = printOffset(offset, out);
    out += "ID:" + id + " HeadId:" + headId + '\n';

    out = printOffset(offset, out);
    out += "Cat: " + catString + '\n';
    // out = printOffset(offset, out);
    // out += "Extracted: " + extracted + " bounded:" + bounded + "\n";
    // if (yield != null){
    // printOffset(offset, out);
    // out.println("Yield: <" + yield + ">");
    // }
    if (heads() != null) {
      out = printOffset(offset, out);
      out += "Head: ";
      out = heads().print(out);
    }
    if (dependencies() != null) {
      out = printOffset(offset, out);
      out += "CCG Deps: ";
      out = dependencies().print(out);
      out += "\n";
    }
    if (filledDependencies != null) {
      out = printOffset(offset, out);
      out += "FilledCCGDeps: ";
      out = filledDependencies.print(out);
      out += "\n";
    }

    if (argument != null) {
      out = printOffset(offset, out);
      if (argDir == FW) {
        out += "FW ";
      }
      if (argDir == BW) {
        out += "BW ";
      }
      out += "Arg: \n";
      out = argument.printCatRec(offset + 1, out);
    }
    if (result != null) {
      out = printOffset(offset, out);
      out += "Result: \n";
      out = result.printCatRec(offset + 1, out);
    }
    return out;
  }

  private void printCatRec(int offset, PrintStream out) {
    printOffset(offset, out);
    out.println("ID:" + id + " HeadId:" + headId);

    printOffset(offset, out);
    out.println("Cat: " + catString);
    // printOffset(offset, out);
    // out.println("Extracted: " + extracted + " bounded:" + bounded);

    if (heads() != null) {
      printOffset(offset, out);
      out.print("Head: ");
      heads().print(out);
    }
    if (dependencies() != null) {
      printOffset(offset, out);
      out.print("CCG Deps: ");
      dependencies().print(out);
      out.print("\n");
    }
    if (filledDependencies != null) {
      printOffset(offset, out);
      out.print("FilledCCGDeps: ");
      filledDependencies.print(out);
      out.print("\n");
    }

    if (argument != null) {
      printOffset(offset, out);
      if (argDir == FW) {
        out.print("FW ");
      }
      if (argDir == BW) {
        out.print("BW ");
      }
      out.println("Arg: ");
      argument.printCatRec(offset + 1, out);
    }
    if (result != null) {
      printOffset(offset, out);
      out.println("Result: ");
      result.printCatRec(offset + 1, out);
    }
  }

  private static void printOffset(int offset, PrintStream out) {
    for (int i = offset; i > 0; i--) {
      out.print("   |");
    }
  }

  private static String printOffset(int offset, String out) {
    for (int i = offset; i > 0; i--) {
      out += "   |";
    }
    return out;
  }

  // ##########################################################
  // COPY
  // ##########################################################
  private CCGcat copy() {
    CCGcat copy = null;
    try {
      copy = (CCGcat) this.clone();
    } catch (Exception E) {
      E.printStackTrace();
    }
    CCGcat tmp, tmpcopy;
    tmp = this;
    tmpcopy = copy;
    tmpcopy.copyHeadsDeps(tmp);
    if (tmp.argument != null) {
      tmpcopy.argument = tmp.argument.copy();
      tmpcopy.result = tmp.result.copy();
      tmpcopy.result.function = tmpcopy;
    }
    return copy;
  }

  private void copyHeadsDeps(CCGcat original) {
    this.headId = original.headId;// copyHeadsDeps
    if (original.heads() != null) {
      this.heads = original.heads().copy();
    } else {
      this.heads = null;
    }
    if (original.dependencies() != null) {
      this.dependencies = original.dependencies().copy();
    }
    if (original.filledDependencies != null) {
      this.filledDependencies = original.filledDependencies.copy();
    } else {
      this.filledDependencies = null;
    }
  }

  // ##########################################################
  // MATCHING, UNIFICATION
  // ##########################################################
  private boolean matches(CCGcat cat) {
    boolean retval = false;
    // strict equality matches
    if (this.catString().equals(cat.catString())
        || (this.catString().equals(NOUN) && cat.catString().equals(NP))
        || (this.catString().equals(NP) && cat.catString().equals(NOUN))) {
      retval = true;
    } else {
      // no features match any features
      if (this.catStringNoFeatures().equals(cat.catString())) {
        retval = true;
      } else {
        // and any features match no features
        if (cat.catStringNoFeatures().equals(this.catString())) {
          retval = true;
        } else {
          // but if both have features, then we need a more thorough
          // check!
          if (cat.catStringNoFeatures().equals(this.catStringNoFeatures())) {
            retval = matchRecursively(cat);
          }
        }
      }
    }
    return retval;
  }

  private boolean matchRecursively(CCGcat cat) {
    boolean retval = false;
    // strict equality matches

    // no features match any features
    if (cat != null) {
      if (this.catString().equals(cat.catString())
          || this.catStringNoFeatures().equals(cat.catString())
          || cat.catStringNoFeatures().equals(this.catString())) {
        retval = true;
      } else {
        if (result != null && argument != null) {
          retval = argument.matchRecursively(cat.argument);
          if (retval) {
            retval = result.matchRecursively(cat.result);
          }
        }
      }
    }
    return retval;
  }

  private boolean matches(String catString1) {
    boolean retval = false;
    if (this.catString != null) {
      if (catString.equals(catString1)) {
        retval = true;
      } else {
        if ((catString.equals(NOUN) && catString1.equals(NP))
            || (catString.equals(NP) && catString1.equals(NOUN))) {
          retval = true;
        } else if (catStringNoFeatures().equals(noFeatures(catString1))) {
          retval = true;
        }
      }
    }
    // System.out.println("Matches: " + catString + " " + catString1 + "? "
    // +
    // retval);
    return retval;
  }

  // ##################################################
  // METHODS FOR CATSTRING
  // - FORWARD, BACKWARD
  // - BRACKETING
  // - REPARSE CATSTRING
  // ##################################################
  private static String forward(String cat1, String cat2) {
    String tmp1, tmp2;
    if (needsBrackets(cat1)) {
      tmp1 = bracket(cat1);
    } else {
      tmp1 = cat1;
    }
    if (needsBrackets(cat2)) {
      tmp2 = bracket(cat2);
    } else {
      tmp2 = cat2;
    }
    return (new StringBuffer(tmp1).append(FSLASH).append(tmp2)).toString();
  }

  private static String backward(String cat1, String cat2) {
    String tmp1, tmp2;
    if (needsBrackets(cat1)) {
      tmp1 = bracket(cat1);
    } else {
      tmp1 = cat1;
    }
    if (needsBrackets(cat2)) {
      tmp2 = bracket(cat2);
    } else {
      tmp2 = cat2;
    }
    return (new StringBuffer(tmp1).append(BSLASH).append(tmp2)).toString();
  }

  /**
   * bracket
   */
  private static String bracket(String cat) {
    return (new StringBuffer("(").append(cat).append(CB)).toString();
  }

  /**
   * boolean: needs brackets? *
   */
  private static boolean needsBrackets(String cat) {
    return cat.indexOf(BSLASH) != -1 || cat.indexOf(FSLASH) != -1;
  }

  /**
   * reparse the cat string in complex category
   */
  private void catStringReparse() {

    if (result != null) {
      result.catStringReparse();
      if (argDir == FW) {
        this.catString = forward(result.catString, argument.catString);
      } else {
        if (argDir == BW) {
          this.catString = backward(result.catString,
              argument.catString);
        } else {
          System.err.println("catStringReparse ERROR: no direction!");
          this.printCat(System.err);
        }
      }
      if (result.function != this) {
        System.err.println("ERROR: result.function != this");
        System.err.println("Result function");
        result.function.printCat(System.err);
        System.err.println("THIS");
        this.printCat(System.err);
      }
    }
  }

  private static synchronized int newId() {
    if (idCounter < Integer.MAX_VALUE) {
      ++idCounter;
    } else {
      idCounter = 0;
    }
    return idCounter;
  }

  private static synchronized int newHeadId() {
    if (headIdCounter < Integer.MAX_VALUE) {
      ++headIdCounter;
    } else {
      headIdCounter = 0;
    }
    return headIdCounter;
  }

  // ##########################################################
  // ##
  // ## THE COMBINATORY RULES
  // ##
  // ##########################################################
  public static CCGcat typeRaiseTo(CCGcat X, String typeRaisedX) {
    CCGcat typeRaised = null;
    if (typeRaisedX.equals("S/(S/NP)")) {
      typeRaised = X.topicalize(S);
    } else {
      if (typeRaisedX.indexOf('/') > -1 && typeRaisedX.indexOf('\\') > -1) {
        CCGcat tmp = parseCat(typeRaisedX);

        if (tmp.argument.result != null
            && tmp.argument.result.catString.equals(tmp.result.catString)) {
          typeRaised = X.typeRaise(tmp.result.catString, tmp.argDir);
        }
      }
    }
    return typeRaised;

  }

  // in LEFT node raising (Y X\Y conj X\Y),
  // all backward args in the second conjunct are long-range deps
  void adjustLongRangeDepsLNR() {
    CCGcat tmp = this;
    if (argument != null && argDir == FW) {
      tmp = tmp.result;
      while (tmp.argument != null && tmp.argDir == FW) {
        tmp = tmp.result;
      }
    }
    if (tmp.argument != null && tmp.argDir == BW
        && (tmp.argument.dependencies != null)) {
      tmp.adjustLongRangeDepsLNRRec();
    }
    // if (tmp.argument != null
    // && tmp.argDir == BW
    // && tmp.argument.conllDependencies != null){ // TODO: CoNLL dependencies?
    // tmp.adjustLongRangeDepsLNRRec();
    // }
  }

  private void adjustLongRangeDepsLNRRec() {
    if (argument != null && argDir == BW
        && argument.dependencies != null) {
      argument.dependencies.setToExtractedUnbounded();
      result.adjustLongRangeDepsLNRRec();
    }
    // if (argument != null
    // && argDir == BW
    // && argument.conllDependencies != null){ // TODO: CoNLL dependencies?
    // argument.conllDependencies.setToExtractedUnbounded();
    // result.adjustLongRangeDepsLNRRec();
    // }

  }

  // in RIGHT node raising (X/Y conj X/Y Y),
  // all forward args are long-range deps
  private void adjustLongRangeDepsRNR() {
    if (argument != null && argDir == FW
        && argument.dependencies != null) {
      argument.dependencies.setToExtractedUnbounded();
      result.adjustLongRangeDepsRNR();
    }
    // if (argument != null
    // && argDir == FW
    // && argument.conllDependencies != null){ // TODO: CoNLL dependencies?
    // argument.conllDependencies.setToExtractedUnbounded();
    // result.adjustLongRangeDepsRNR();
    // }
  }

  private void setHeadId(int headIdNumber) {
    headId = headIdNumber;// setHeadId
    if (result != null) {
      result.setHeadId(headIdNumber);
    }
  }

  // ##################################################
  // APPLICATION
  // ##################################################

  /**
   * Function application. Unify the argument of the functor with arg, and
   * return the result of the functor.
   */

  public static CCGcat apply(CCGcat functor, CCGcat arg) {
    if (functor.argument != null && functor.argument.matches(arg)) {// was arg
      if (DEBUG) {
        System.out.println("### Application: functor");
        functor.printCat();
        System.out.println("### Application: argument");
        arg.printCat();
      }
      CCGcat result = functor.result.copy();
      result.function = null;
      if (DEBUG) {
        System.out.println("Application: intermediate category result");
        result.printCat();
      }
      CCGcat Y = unify(functor.argument, arg);
      if (DEBUG) {
        System.out.println("### Application: intermediate category Y (after unification)");
        Y.printCat(System.out);
      }
      result.replace(functor.argument, Y);

      // CCG
      result.filledDependencies = Y.filledDependencies;
      Y.filledDependencies = null;

      if (functor.argDir == FW) {
        if (functor.catString.equals("NP[nb]/N")) {
          result.catString = NP;
        }
      }
      if (DEBUG) {
        System.out.println("### Application: result");
        result.printCat(System.out);
      }
      return result;
    }
    return null;
  }

  // ##################################################
  // TYPERAISE
  // ##################################################

  /**
   * Typeraising -- typeraise the current category. The current category
   * together with <tt>T</tt> and the direction specify the type-raised
   * category.
   */
  CCGcat typeRaise(String T, int direction) {
    CCGcat t = parseCat(T);
    t.setHeadId(newHeadId());// typeraise
    CCGcat newArg = new CCGcat(t.copy(), this);
    newArg.headId = t.headId;// typeraise: argument
    // newArg = T|X
    CCGcat resCat = new CCGcat(t.copy(), newArg);
    // resCat = T|(T|X);
    resCat.argument.result = resCat.result;
    if (direction == FW) {
      newArg.argDir = BW;
      resCat.argDir = FW;
      newArg.catString = backward(t.catString, this.catString);
      resCat.catString = forward(t.catString, newArg.catString);
    } else {
      newArg.argDir = FW;
      resCat.argDir = BW;
      newArg.catString = forward(t.catString, this.catString);
      resCat.catString = backward(t.catString, newArg.catString);
    }
    resCat.function = null;
    resCat.heads = this.heads();
    resCat.headId = this.headId;// typeRaise
    return resCat;
  }

  // ##################################################
  // TOPICALIZATION
  // ##################################################

  /**
   * topicalization -- similar to type-raising, but yields S/(S/X)
   */
  private CCGcat topicalize(String local_S) {
    CCGcat t = parseCat(local_S);
    t.setHeadId(newHeadId());// topicalize
    CCGcat newArg = new CCGcat(t.copy(), this);
    newArg.headId = t.headId;
    // newArg = S|X
    CCGcat resCat = new CCGcat(t.copy(), newArg);
    // resCat = S|(S|X);
    resCat.argument.result = resCat.result;
    newArg.argDir = FW;
    resCat.argDir = FW;

    newArg.catString = forward(t.catString, this.catString);
    resCat.catString = forward(t.catString, newArg.catString);

    resCat.function = null;
    resCat.heads = this.heads();
    resCat.headId = this.headId;

    return resCat;
  }

  // ##################################################
  // COMPOSE
  // ##################################################

  /**
   * (Generalized) function composition. Forward crossing composition is
   * excluded at the moment.
   */
  public static CCGcat compose(CCGcat functor, CCGcat arg) {
    // functor == X|Y
    // arg == Y|Z$
    // ==> resCat = X|Z$....

    CCGcat resultCat = null;
    if (functor.argument != null) {
      resultCat = arg.copy();
      resultCat.function = null;
      if (functor.heads != null) {
        if (functor.isAdjunctCat() && !arg.isAdjunctCat()) {
          // NEW MARCH 2005
          // modified Nov 2012: added !arg.isAdjunctCat():
          // "very + big_red"
          // should be "very BIG", not "very red".
          // System.out.println("XXX COMPOSITION with adjunct " +
          // functor.catString() + " " + arg.catString() + " " +
          // arg.isAdjunctCat());
          resultCat.heads = arg.heads.copy();
        } else {
          resultCat.heads = functor.heads.copy(); // hack!
        }
      }
      // make tmp point to a category Y/Z$,
      // then replace its result with X.
      CCGcat tmp = resultCat;
      CCGcat functorArg = functor.argument();
      while (tmp != null && tmp.result() != null && !tmp.result().matches(functorArg)) {
        tmp = tmp.result();
      }

      if (tmp.result() != null && tmp.result().matches(functorArg)
        // && !(functor.argDir == FW && tmp.argDir == BW)// no forward crossing composition!
          ) {

        CCGcat functorCopy = functor.copy();
        CCGcat Y = unify(functorCopy.argument(), tmp.result);
        functorCopy.replace(functorCopy.argument(), Y);
        resultCat.replace(tmp.result(), Y);
        tmp.result = functorCopy.result().copy();
        tmp.result.function = tmp;
        resultCat.filledDependencies = Y.filledDependencies;
        Y.filledDependencies = null;

        resultCat.catStringReparse();
        // adjust the head
        while (tmp != null) {
          if (functorCopy.result.heads != null) {
            tmp.heads = functorCopy.result.heads.copy();// again,
          }
          // this is just another copy
          tmp.headId = functorCopy.result.headId;// topicalize
          tmp = tmp.function;
        }
      } else {
        resultCat = null;
      }
    }
    if (DEBUG && resultCat != null) {
      System.out.println("compose result:");
      resultCat.printCat();
    }
    return resultCat;
  }

  /**
   * coordinate two categories. Calls coordinateRec
   */
  public static CCGcat coordinate(CCGcat cat1, CCGcat cat2, DepType deptype) {// X X[conj]
    CCGcat coordination = null;
    if (cat1.matches(cat2.catString)) {
      coordination = cat1.copy();
      coordination.filledDependencies = null;

      if (deptype.equals(DepType.CoNLL)) {
        if (Configuration.CONLL_DEPENDENCIES.equals(CoNLLDependency.X2_X1___X2_CC))
          coordination.removeConLLDeps();
      }

      // before recursing: adjust the long range dependencies:
      coordination.adjustLongRangeDepsRNR();
      coordination.coordinateRec(cat2);

      if (deptype.equals(DepType.CoNLL)) {
        /**
         * Coordination styles: - CCG: Parent->X1, Parent->X2 - CoNLL:
         * Parent->conj, conj->X1, conj->X2: conjHeads = conj - Spanish:
         * Parent->X1, X1->conj, X1->X2 - Other: Parent->X1, X1->X2, X2->conj:
         * conjHeads = X2;
         */
        DepList dConj;
        switch (Configuration.CONLL_DEPENDENCIES) {
          case CC_X1___CC_X2:// X1<-conj->X2
            // cat2.conjheads = conj
            // cat1.heads = X1 head
            dConj = new DepList("arg", cat2.conjHeads, cat1.heads, 1, BW, false);
            dConj.next = null;
            if (coordination.filledDependencies == null) {
              coordination.filledDependencies = dConj.copy();
            } else {
              coordination.filledDependencies.append(dConj);
            }
            if (cat2.conjHeads != null) {
              coordination.setHeadsRec(cat2.conjHeads);// conj is head of everything
              if (coordination.isAdjunctCat()) {
                // System.out.println("DEBUG CONLL ADJUNCT COORDINATION");
                DepList depArg;
                if (coordination.argument != null && coordination.result != null) {
                  depArg = coordination.argument.dependencies();

                  if (depArg != null && depArg.argWord == null) {
                    DepList newDepArg = new DepList(depArg.rel(), coordination.heads(), null,
                        depArg.argIndex, depArg.argDir(), depArg.modifier);
                    // System.out.println("New dep: "); newDepArg.print();
                    coordination.argument.dependencies = newDepArg.copy();
                    coordination.result.dependencies = newDepArg.copy();
                  }
                }
              }
            }
            break;
          case X1_CC___X1_X2:
            // cat1: X1
            // cat2: X2;
            // cat2.conjhead: conj
            // System.out.println("SPANISH cat1: " + cat1.heads.asString()
            // + " cat2: " + cat2.heads.asString()
            // + " cat2.conj: " + cat2.conjHeads.asString());
            dConj = new DepList("arg", cat1.heads, cat2.heads, 1, FW, false);
            dConj.next = new DepList("arg", cat1.heads, cat2.conjHeads, 1, FW, false);
            dConj.next.next = null;
            if (coordination.filledDependencies == null) {
              coordination.filledDependencies = dConj.copy();
            } else {
              coordination.filledDependencies.append(dConj);
            }
            coordination.setHeadsRec(cat1.heads);
            if (coordination.isAdjunctCat()) {
              // System.out.println("ADJUNCT COORDINATION!!!");
              DepList depArg;
              if (coordination.argument != null && coordination.result != null) {
                depArg = coordination.argument.dependencies();
                if (depArg != null && depArg.argWord == null) {
                  DepList newDepArg = new DepList(depArg.rel(), coordination.heads(), null,
                      depArg.argIndex, depArg.argDir(), depArg.modifier);
                  coordination.argument.dependencies = newDepArg.copy();
                  coordination.result.dependencies = newDepArg.copy();
                }
              }
            }
            break;
          case X1_CC___CC_X2: // X1 -> conj -> X2
            // cat1: X1
            // cat2: conj;
            // System.out.println("OTHER: cat1: " + cat1.heads.asString() +
            // " cat2: " + cat2.heads.asString());
            dConj = new DepList("arg", cat1.heads, cat2.conjHeads, 1, FW, false);
            dConj.next = null;
            if (coordination.filledDependencies == null) {
              coordination.filledDependencies = dConj.copy();
            } else {
              coordination.filledDependencies.append(dConj);
            }
            coordination.setHeadsRec(cat1.heads);
            break;
          case X1_X2___X2_CC: // X2->conj, X2->X1
            // cat1: X1
            // cat2: conj;
            // System.out.println("OTHER: cat1: " + cat1.heads.asString() +
            // " cat2: " + cat2.heads.asString());
            dConj = new DepList("arg", cat1.heads, cat2.heads, 1, FW, false);
            dConj.next = null;
            if (coordination.filledDependencies == null) {
              coordination.filledDependencies = dConj.copy();
            } else {
              coordination.filledDependencies.append(dConj);
            }
            coordination.setHeadsRec(cat1.heads);
            break;
          case X2_X1___X2_CC:// julia
            dConj = new DepList("arg", cat2.heads, cat1.heads, 1, BW, false);
            dConj.next = null;
            if (coordination.filledDependencies == null) {
              coordination.filledDependencies = dConj.copy();
            } else {
              coordination.filledDependencies.append(dConj);
            }
            coordination.setHeadsRec(cat2.heads);
            break;
        }
      }
    }
    return coordination;
  }

  /*
 * recursively remove ConLL Dependencies - used for conjuncts, since in
 * dependency grammar there is only a single head
 */
  private void removeConLLDeps() {
    dependencies = null;
    if (argument != null) {
      argument.removeConLLDeps();
    }
    if (result != null) {
      result.removeConLLDeps();
    }
  }

  /**
   * coordinate recursively with another category. Is called by coordinate
   */
  private void coordinateRec(CCGcat cat) {
    if (argument != null) {
      argument.coordinateRec(cat.argument);
    }
    if (result != null) {
      if (result.id == argument.id) {// coordination
        result = argument.copy();
        result.dependencies = null;
      } else {
        result.coordinateRec(cat.result);
      }
    }
    appendHeads(cat.heads());
    appendDeps(cat.dependencies());
  }

  /**
   * Standard substitution: cat1 = (X\Y)/Z cat2=Y/Z ==> X/Z The result Z is Z1
   * and Z2 coordinated.
   */
  public static CCGcat substitute(CCGcat cat1, CCGcat cat2, DepType depType) {
    CCGcat result = null;
    if (cat1.argument != null && cat1.result.argument != null
        && cat1.result.result != null && cat2.argument != null
        && cat2.result != null && cat1.argument.matches(cat2.argument)
        && cat1.result.argument.matches(cat1.result.result)
        && cat1.result.argument.matches(cat2.result)) {

      CCGcat functor = cat1.copy();
      CCGcat arg = cat2.copy();
      CCGcat Z1 = functor.argument;
      CCGcat Y1 = functor.result.argument;
      // CCGcat X1 = functor.result.result;
      CCGcat Z2 = arg.argument;
      CCGcat Y2 = arg.result;

      CCGcat Z3 = coordinate(Z1, Z2, depType);
      functor.replace(Z1.id, Z1.headId, Z3);// substitution
      arg.replace(Z2.id, Z2.headId, Z3);// substitution
      CCGcat Y3 = unify(Y1, Y2);
      functor.replace(Y1, Y3);
      arg.replace(Y2, Y3);

      result = functor.result();
      result.argDir = functor.argDir;
      result.argument = functor.argument;
      result.argument.function = result;
      result.catStringReparse();

      result.filledDependencies = Y3.filledDependencies;
      Y3.filledDependencies = null;

      if (result.result.heads != null) {
        result.heads = result.result.heads.copy();
        result.headId = result.result.headId; // coordination
      }

    }
    return result;
  }

  /**
   * A special rule for punctuation. Returns a copy of the main category. At
   * the moment, does not change the yield.
   */
  public static CCGcat punctuation(CCGcat cat, CCGcat pct) {
    CCGcat result = cat.copy();
    result.filledDependencies = null;
    return result;
  }

  /**
   * Special rule for the intermediate stage of coordination.
   */
  public static CCGcat conjunction(CCGcat cat, CCGcat conjCat, DepType depType) {
    CCGcat result = null;
    try {
      // We only call it if appropriate
      //if (conjCat.catString.equals("conj")) {
        result = cat.copy();

        if (depType.equals(DepType.CCG)) {
          result.filledDependencies = null;
        } else {
          CCGcat X2 = cat;

          // TODO: May not need conjHeads except for CoNLL
          /**
           * Coordination styles: - CCG: Parent->X1, Parent->X2 - CoNLL:
           * Parent->conj, conj->X1, conj->X2 - Spanish: Parent->X1, X1->conj,
           * X1->X2 - Other: Parent->X1, X1->conj conj->X2 - Other2: Parent->X1,
           * X1->X2, X2->conj
           */
          switch (Configuration.CONLL_DEPENDENCIES) {
            case CC_X1___CC_X2:
              result.filledDependencies = new DepList("arg", conjCat.heads(), X2.heads(), 2, FW, false);
              result.conjHeads = conjCat.heads().copy();
              break;
            case X1_CC___X1_X2:
              result.conjHeads = conjCat.heads().copy();
              result.filledDependencies = null;
              break;
            case X1_CC___CC_X2:// conj->X2
              result.filledDependencies = new DepList("arg", conjCat.heads(), X2.heads(), 2, FW, false);
              result.conjHeads = conjCat.heads().copy();
              break;
            case X1_X2___X2_CC:
              result.filledDependencies = new DepList("arg", X2.heads(), conjCat.heads(), 2, BW, false);
              result.conjHeads = X2.heads().copy();// MAY NEED TO BE FIXED
              break;
            case X2_X1___X2_CC:
              result.filledDependencies = new DepList("arg", X2.heads(), conjCat.heads(), 2, BW, false);
              result.conjHeads = null;
              break;
          }
          if (Configuration.CONLL_DEPENDENCIES != CoNLLDependency.X2_X1___X2_CC)
            result.removeConLLDeps();
        }
      //}
    } catch (Exception e) {
      System.err.println(e.getMessage());
      e.printStackTrace();
      System.exit(1);
    }
    return result;
  }


  /**
   * Type-changing rules: change <tt>dtrCat</tt> to a category with string
   * representation <tt>cat</tt>. Copies the head and yield of dtrCat. Calls
   * "assignHeadsDeps".
   */
  public static CCGcat typeChangingRule(CCGcat dtrCat, String cat) {

    CCGcat newCat = null;

    if (cat != null && dtrCat != null) {

      newCat = parseCat(cat);
      newCat.target().assignHeadsDeps(1, newCat.heads, newCat.headId);// TCR

      if (dtrCat.heads != null) {
        newCat.heads = dtrCat.heads.copy();
      }
      newCat.filledDependencies = null;

      // if the argument of newCat matches the argument of dtrCat, then
      // they are the same!
      if (newCat.argument != null
          && dtrCat.argument != null
          && newCat.argument.catString != null
          && dtrCat.argument.catString != null
          && newCat.argument.matches(dtrCat.argument.catString)
          && !(dtrCat.catString.equals("S[to]\\NP") && cat
          .equals("NP\\NP"))
          && !dtrCat.catString.equals("S[dcl]\\NP")
          && !dtrCat.catString.equals("S[b]\\NP")) {

        if (dtrCat.argument.dependencies != null) {
          newCat.argument.dependencies = dtrCat.argument.dependencies
              .copy();
          if (dtrCat.argDir == FW) {
            newCat.argument.dependencies
                .setToExtractedUnbounded();
          }
          if (dtrCat.argDir == BW) {
            newCat.argument.dependencies.setToExtractedBounded();
          }
        }

        // (S\NP)\(S\NP) ==> S\NP
        if (newCat.argument != null && newCat.result != null
            && dtrCat != null && dtrCat.catString != null
            && dtrCat.matches(VP)
            && newCat.argument.matches(dtrCat.catString)
            && newCat.result.matches(dtrCat.catString)) {

          if (dtrCat.argument.dependencies != null) {
            newCat.argument.argument.dependencies = dtrCat.argument.dependencies.copy();
            newCat.argument.argument.dependencies
                .setToExtractedBounded();
            newCat.result.argument.dependencies = dtrCat.argument.dependencies.copy();
            newCat.result.argument.dependencies.setToExtractedBounded();
          }
        }
      }
    }
    return newCat;
  }

  private static CCGcat unify(CCGcat cat1, CCGcat cat2) {
    // assumes that cat1, cat2 have the same categories
    CCGcat newCat;
    DepList copiedUnfilled = null;
    if (DEBUG) {
      System.out.println("ENTER UNIFY " + cat1.catString() + ' ' + cat2.catString());
    }
    if (cat2.hasFeatures()) {
      if (DEBUG) {
        System.out.println("Unify CASE 1 (cat2.hasFeatures)");
      }
      newCat = cat2.copy();
      newCat.id = newId();// unification -- case1
      if (DEBUG) {
        System.out.println("before: newCat.filledCCGDependencies: ");
        newCat.printCat();
      }
      // copiedUnfilled is a list of the unfilledDependencis in newCat
      if (newCat.filledDependencies != null) {
        copiedUnfilled = newCat.filledDependencies.copyUnfilled();
      }
      if (copiedUnfilled != null) {
        copiedUnfilled.print();
      }
      newCat.filledDependencies = newCat.mergeWithCCG(cat1, copiedUnfilled);
      if (DEBUG) {
        System.out.println("after: newCat.filledCCGDependencies: " + newCat.filledDependencies.toString());
        newCat.printCat();
      }
    } else {
      if (DEBUG) {
        System.out.println("Unify CASE 2 (cat2 doesn't have features)");
      }
      newCat = cat1.copy();
      newCat.id = newId();// unification -- case2
      // new
      if (newCat.filledDependencies != null) {
        copiedUnfilled = newCat.filledDependencies.copyUnfilled();
      }
      if (DEBUG) {
        System.out.println("before filledDependenciecs:");
        newCat.printCat();
      }
      newCat.filledDependencies = newCat.mergeWithCCG(cat2, copiedUnfilled);
      if (DEBUG) {
        System.out.println("after filledDependencies: ");
        newCat.printCat();
      }
    }
    return newCat;
  }

  private boolean hasFeatures() {
    return catString != null && catString.indexOf('[') > -1;
  }

  /**
   * merge this category with cat recursively
   */
  private DepList mergeWithCCG(CCGcat cat, DepList currentDeps) {
    /*
     * System.out.println("ENTER mergeWith: " + catString + " " +
     * this.extracted + " " + cat.extracted); if (currentDeps != null)
     * currentDeps.print(); this.printCat(System.out);
     * cat.printCat(System.out);
     */
    if (!this.hasFeatures() && cat.hasFeatures()) {
      catString = cat.catString;
    }
    // RECURSION
    // 1. if the result has the same id as the argument,
    // then treat the argument first, and then copy it over to the result.
    if (argument != null
        && (argument.id == result.id || cat.argument.id == cat.result.id)) {// mergeWith
      if (argument != null) {
        currentDeps = argument.mergeWithCCG(cat.argument, currentDeps);
      }
      result = argument.copy();
    }
    // 2. otherwise, do the argument and result separately
    else {
      if (argument != null) {
        currentDeps = argument.mergeWithCCG(cat.argument, currentDeps);
      }
      if (result != null) {
        currentDeps = result.mergeWithCCG(cat.result, currentDeps);
      }
    }

    // THE BASE STEP: FOR EACH CATEGORY:

    // prepending:
    if (cat.extracted && dependencies != null) {
      // if ONE of them is unbounded, the result is unbounded
      if (cat.bounded && this.bounded) {
        dependencies.setToExtractedBounded();
      } else {
        dependencies.setToExtractedUnbounded();
      }
    }
    // new -- March 06: prepending the other way!
    if (this.extracted && cat.dependencies != null) {
      // if ONE of them is unbounded, the result is unbounded
      if (cat.bounded && this.bounded) {
        cat.dependencies.setToExtractedBounded();
      } else {
        cat.dependencies.setToExtractedUnbounded();
      }
    }

    // COPY THE HEADS (fill the dependencies if they are there)
    if (heads == null && cat.heads != null) {
      heads = cat.heads.copy();
      headId = cat.headId; // TCR
      if (dependencies != null) {
        currentDeps = applyCCGDependencies(dependencies(), heads(), currentDeps);
      }
    } else {
      appendHeads(cat.heads);
      if (dependencies != null && heads != null) {
        currentDeps = applyCCGDependencies(dependencies(), heads(), currentDeps);
      }
    }

    // COPY THE DEPENDENCIES (fill the dependencies if they are there)
    if (dependencies == null && cat.dependencies != null) {
      dependencies = cat.dependencies.copy();

      if (heads != null) {
        // System.out.print("### COPY DEPENDENCIES"); deps().print();
        currentDeps = applyCCGDependencies(dependencies(), heads(), currentDeps);
      }
    } // IF THERE ARE MULTIPLE DEPENDENCIES: APPEND, THEN APPLY
    else {
      if (dependencies != null && cat.dependencies != null) {
        if (extracted) {// the multiple dependencies are extracted!!!
          // cat.dependencies.setToExtracted();
          if (bounded && cat.bounded) {
            cat.dependencies.setToExtractedBounded();
          } else {
            cat.dependencies.setToExtractedUnbounded();
          }
        }
        appendDeps(cat.dependencies);
        if (heads != null) {
          // System.out.print("### APPEND DEPENDENCIES");
          // deps().print();
          currentDeps = applyCCGDependencies(dependencies(), heads(), currentDeps);
        }
      }
    }
    return currentDeps;
  }


  /**
   * recursive replacement of all the parts of cat1 with the corresponding
   * parts of cat2
   */
  private void replace(CCGcat cat1, CCGcat unifiedCat1) {
    if (cat1 != null && unifiedCat1 != null) {
      if (cat1.argument != null) {
        replace(cat1.argument, unifiedCat1.argument);
      }
      if (cat1.result != null) {
        replace(cat1.result, unifiedCat1.result);
      }
      replace(cat1.id, cat1.headId, unifiedCat1);// recursion in replace
    }
  }

  /**
   * REPLACING catId with newCat and headID with newCat.head or deps
   */
  private void replace(int catId, int headID, CCGcat newCat) {

    /*
     * If the catId matches the old catID: - replace old catId with new
     * catId - replace old headId with the new headId - replace cat string
     * with newCat.catString if that ONE has features - copy the heads
     * across from newCat if newCat provides the head. COPY the
     * dependencies if newCat provides the dependencies, else set them
     * to null (debug?) If the headId matches the old headId: - replace old
     * headId with the new headId - copy the heads across from newCat if
     * newCat provides the head. ADD the dependencies from newCat if the
     * categories match (and otherwise???)
     */
    // Case 1: it could be the category itself!
    if (this.id == catId) {// replace: the category id matches the ONE to be
      // replaced

      this.id = newCat.id;// replace old by new
      this.headId = newCat.headId;// replace old by new

      if (!hasFeatures() && newCat.hasFeatures()) {
        catString = newCat.catString;
      }
      if (newCat.heads != null) {
        this.heads = newCat.heads.copy();
      }
      if (newCat.dependencies != null) {
        this.dependencies = newCat.dependencies.copy();
      } else {
        dependencies = null;
      }
      if (newCat.extracted) {
        if (newCat.bounded) {
          this.setToExtractedBounded();
        } else {
          this.setToExtractedUnbounded();
        }
      }
    } else {
      // If the categories have the same headId, but aren't the same
      // category:
      // then instantiate the head if newCat's head is instantiated:

      if (this.headId == headID // replace: (old) head ids are the same
          && this.id != newCat.id) {// replace: heads same, but cats
        // not
        // copy the head across
        if (newCat.heads != null && this.heads == null) {
          this.heads = newCat.heads.copy();
        }
        headId = newCat.headId; // replace old headId with new

        // add the dependencies, but don't change the 'extracted'
        // feature on
        // these cate here.
        if (this.matches(newCat.catString)) {
          appendDeps(newCat.dependencies);
        }
      }
      // recursion on argument:
      if (argument != null) {
        argument.replace(catId, headID, newCat);// replace: recursion on
        // argument
      }
      // recursion on result:
      if (result != null) {
        result.replace(catId, headID, newCat);// replace: recursion on
        // result
      }
    }
  }

  /**
   * just like StatCCGModel.addLexEntry
   */
  private static void addLexEntry(String entryString) {
    StringTokenizer tokenizer = new StringTokenizer(entryString);
    String word = null;
    String cat = null;
    // HashMap lexicon = new HashMap();
    // System.err.println("ENTRY" + entryString);

    if (tokenizer.hasMoreTokens()) {
      word = tokenizer.nextToken();
    }
    if (tokenizer.hasMoreTokens()) {
      cat = tokenizer.nextToken();
    }
    Double prob;
    if (tokenizer.hasMoreTokens()) {
      prob = new Double(tokenizer.nextToken());
      if (prob > 1) {
        // System.err.println("lexical probability: " + prob);
      }
    }
    CCGcat newCat = lexCat(word, cat);
    if (DEBUG && !newCat.isAtomic()) {
      System.out.println("@@@ " + word + ' ' + newCat.indexedCatString()
          + ' ' + prob.intValue());
    }
  }

  // #####################################################

  protected void treatSpecialCasesInducedCats() {
    if (isAtomic()) {
      return;
    }
    System.out.println("\nLexCat: special case? " + indexedCatString());
    treatInducedModifier();
    treatInducedAuxModal();// (S|N_i)|(S|N_i)
    treatInducedRelPron(); // (N_i|N_i)|(S|N_i)
  }

  private boolean isInducedModifier() {// adjustAdj()?
    boolean retval = false;
    if (isAtomic()) {
      return retval;
    }
    if (result.catString().equals(argument.catString())) {
      retval = true;
      // exception: auxiliary verbs....
      if (result.catString().equals("S\\N")
          || result.catString().equals("S/N")) {
        retval = false;
      }
    }
    return retval;
  }

  private void treatInducedModifier() {
    if (isInducedModifier()) {
      if (result.isInducedModifier()) {
        result.treatInducedModifier();
      }
      argument = result.copy();
      //adjustAdjs();
      if (DEBUG) {
        System.out.println("InducedModifier: " + indexedCatString());
      }
    }
  }

  private boolean isInducedAuxModal() {
    boolean retval = false;
    if (isAtomic()) {
      return retval;
    }
    if ((argument.catString.equals("S\\N")
        || argument.catString.equals("S/N"))
        && (result.catString.equals("S\\N") ||
        result.catString.equals("S/N"))) {
      retval = true;
    }
    return retval;
  }

  private void treatInducedAuxModal() {
    if (isInducedAuxModal()) {
      //System.out.println("InducedAuxModal before: " + indexedCatString());
      argument.argument = result.argument.copy();
      if (DEBUG) {
        System.out.println("InducedAuxModal: " + indexedCatString());
      }
    }
  }

  private boolean isInducedRelPron() {
    boolean retval = false;
    if (isAtomic()) {
      return retval;
    }
    if ((argument.catString.equals("S\\N") || argument.catString.equals("S/N"))
        && (result.catString.equals("N\\N") || result.catString.equals("N/N"))
        ||
        ((argument.catString.equals("S\\NP") || argument.catString.equals("S/NP"))
            && (result.catString.equals("NP\\NP") || result.catString.equals("NP/NP")))
        ) {
      retval = true;
    }
    return retval;
  }

  private void treatInducedRelPron() {
    if (isInducedRelPron()) {// (N|N)|(S|N)
      result.argument = result.result.copy();
      argument.argument = result.result.copy();
      argument.argument.setToExtractedBounded();
      if (DEBUG) {
        System.out.println("InducedRelPron: " + indexedCatString());
      }
    }
  }

  public enum DepType {
    CCG, CoNLL
  }
}
