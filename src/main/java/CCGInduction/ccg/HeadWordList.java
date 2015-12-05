package CCGInduction.ccg;

import java.io.IOException;
import java.io.PrintStream;

/**
 * This is just an implementation of a list of headwords. The whole concept of
 * 'headwords' might have to be replaced by a proper treatment of categories ONE
 * day...
 * @author juliahmr
 */

public class HeadWordList implements Cloneable {
  /** the head word */
  private final String headWord;
  /** the lexical category */
  private final String lexCat;
  /**
   * the lexical probability <i>P(word | cat)</i> -- not that this is <b>not</b>
   * a log probability!!!
   */

  private double lexProb;
  private HeadWordList next;
  private final int index;

  /**
   * The constructor for parsing.
   */
  public HeadWordList(String word, String cat, double prob, int i) {
    headWord = word;
    lexCat = cat;
    lexProb = prob;
    index = i;
    // System.err.println("new headwordlist: " +
    // StatCCGModel.wordMap.translateCode(word) + " " +
    // StatCCGModel.catMap.translateCode(cat) + " " + prob);
    next = null;
  }

  /** The constructor for training -- am not sure this is required..!! */
  public HeadWordList(String word, String cat) {
    headWord = word;
    lexCat = cat;
    lexProb = 0;
    index = 1;
    next = null;
  }

  /** The constructor for parsing..!! */
  public HeadWordList(String word, String cat, int i) {
    headWord = word;
    lexCat = cat;
    lexProb = 0;
    index = i;
    next = null;
  }

  /** A dummy constructor */
  public HeadWordList() {
    headWord = null;
    lexCat = null;
    next = null;
    index = -1;
  }

  /** returns the category or the string "CAT" */
  String cat() {
    if (lexCat != null)
      return lexCat;
    else
      return "CAT";
  }

  public String lexCat() {
    return lexCat;
  }

  /** returns the head word or the string "WORD" */
  public String word() {
    if (headWord != null)
      return headWord;
    else
      return "WORD";
  }

  public String headWord() {
    return headWord;
  }

  // public int wordToInt(){
  // if (headWord != null && !headWord.equals(""))
  // return StatCCGModel.wordMap.translateCodeToInt(headWord);
  // else return -1;
  // }
  /** returns the lexical probability */
  public double lexProb() {
    // System.out.println("lexProb of " + word() + " given " + cat() +": " +
    // lexProb);
    return lexProb;
  }

  /** Returns the next item in the list */
  public HeadWordList next() {
    return next;
  }

  /** Adds a new item to this list */
  private void add(String word, String cat) {
    this.last().next = new HeadWordList(word, cat);
  }

  /**
   * Appends an entire <tt>HeadWordList</tt> to this list.
   */
  public void append(HeadWordList list2) {
    HeadWordList copy = null;
    HeadWordList tmp, tmpcopy;
    try {
      copy = (HeadWordList) list2.clone();
    } catch (Exception E) {
      E.printStackTrace();
    }
    this.last().next = copy;
    tmp = list2;
    tmpcopy = copy;

    while (tmp.next != null) {
      try {
        tmpcopy.next = (HeadWordList) tmp.next.clone();
      } catch (Exception E) {
        E.printStackTrace();
      }
      tmp = tmp.next;
      tmpcopy = tmpcopy.next;
    }

  }

  private HeadWordList last() {
    HeadWordList tmp = this;
    while (tmp.next != null) {
      tmp = tmp.next;
    }
    return tmp;
  }

  public int index() {
    return index;
  }

  /**
   * Returns a copy of this list. Used by the constructors of
   * <tt>BinaryNode</tt> during parsing.
   */
  public HeadWordList copy() {
    HeadWordList copy = null;
    HeadWordList tmp, tmpcopy;
    try {
      copy = (HeadWordList) this.clone();
    } catch (Exception E) {
      E.printStackTrace();
    }

    tmp = this;
    tmpcopy = copy;

    while (tmp.next != null) {
      try {
        tmpcopy.next = (HeadWordList) tmp.next.clone();
      } catch (Exception E) {
        E.printStackTrace();
      }
      tmp = tmp.next;
      tmpcopy = tmpcopy.next;
    }
    return copy;
  }

  /** returns the key for lexical probabilities */
  public String lexProbKey() {
    return (new StringBuffer(word()).append('|').append(cat())).toString();
  }

  /** set the lexical probability to <tt>prob</tt> */
  public void setLexProb(double prob) {
    lexProb = prob;
  }

  /** tests whether this index is in this headlist */
  public boolean containsWord(int wordIndex) {
    boolean retval = false;

    HeadWordList tmp = this;
    while (tmp != null && !(retval)) {
      if (wordIndex == index) {
        retval = true;
        break;
      }
      tmp = tmp.next;
    }

    return retval;
  }

  // ==== equalLists(HeadWordList list1, HeadWordList list2)
  /**
   * checks whether two HeadWordLists are the same. If two lists contain the
   * same items but in a different order, they are not considered equal.
   */
  public static boolean equalLists(HeadWordList list1, HeadWordList list2) {
    boolean yesno;
    if (list1 == null && list2 == null) {
      yesno = true;
    } else {
      if (list1.size() == list2.size()) {
        yesno = true;
        HeadWordList tmp1 = list1;
        HeadWordList tmp2 = list2;
        while (tmp1 != null && yesno) {
          if (tmp1.headWord != null
              && tmp1.headWord.equals(tmp2.headWord)
              && tmp1.lexCat != null
              && tmp1.lexCat.equals(tmp2.lexCat)) {} else {
            yesno = false;
            break;
          }
          tmp1 = tmp1.next;
          tmp2 = tmp2.next;
        }
      } else {
        yesno = false;
      }
    }
    return yesno;
  }

  public String asString() {
    StringBuilder tmp = new StringBuilder(word()).append('|')
        .append(cat()).append(':').append(index).append(" ");
    HeadWordList nextItem = next;
    while (nextItem != null) {
      tmp.append(nextItem.word()).append('|').append(next.cat());
      nextItem = nextItem.next;
    }
    return tmp.toString();
  }

  /** prints the entire list on ONE line */
  private void printRec(PrintStream out) {
    out.print(word() + "|" + cat() + " " + index);
    if (next != null) {
      next.printRec(out);
    }
  }

  private String printRec(String out) {
    out += word() + "|" + cat() + " " + index;
    if (next != null) {
      next.printRec(out);
    }
    return out;
  }

  private void print() {
    print(System.out);
  }

  public void printNoLine() {
    System.out.print('[');
    printRec(System.out);
    System.out.print(']');
  }

  public void print(PrintStream out) {
    out.print('[');
    printRec(out);
    out.println(']');
  }

  public String print(String out) {
    out += "[";
    out = printRec(out);
    out += "]\n";
    return out;
  }

  private int size() {
    int size = 1;
    if (this.next() != null) {
      size += this.next().size();
    }
    return size;
  }

  /** just for testing */
  public static void main(String[] args) throws IOException {
    HeadWordList list = new HeadWordList("This", "NP");
    HeadWordList copy;
    list.add("is", "S\\NP/NP");
    list.add("a", "NP/N");
    list.add("list", "N");
    list.print();
    copy = list.copy();
    copy.add("copy!!!", ":-)");
    copy.print();
    list.print();
    list.next().print();
    list.append(copy);
    list.print();
    list.append(copy);
    list.print();
    copy.print();
  }

}

// --- END OF FILE:
// /home/julia/CCG/code/MyParsers/DependencyModel/code/HeadWordList.java

