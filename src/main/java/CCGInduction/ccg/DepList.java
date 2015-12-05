package CCGInduction.ccg;

import java.io.PrintStream;

/** A class for the list of dependencies 
 * @author juliahmr
 */
public class DepList implements Cloneable {
  private final String rel;// the relationship between the two words (atm, only
                           // 'arg')
  private final String headWord;
  public final String headCat;
  public int headIndex;// the index of the argument word in the chart
  public String argWord;
  public String argCat;
  public final int argPos;// the argument slot
  public int argIndex;// the index of the argument word in the chart
  public boolean extracted;
  public boolean bounded;
  public boolean modifier;
  DepList next;
  private final int argDir;
  public String label = null;// PROPBANK
  public int start;
  public int end;
  public boolean compl = false;// if true, head is the linguistic head and arg
                               // is its complement; o/w head is an adjunct
  /** directionality of argument: forward */
  private static final int FW = 0;// cf. CCGcat
  /** directionality of argument: backward */
  private static final int BW = 1; // cf.CCGcat

  public String rel() {
    return rel;
  }

  public int argDir() {
    return argDir;
  }

  DepList(String relation, HeadWordList head, HeadWordList dependent,
          int argPosition, int dir, boolean mod) {

    rel = relation;
    argDir = dir;
    if (head != null) {
      // System.out.println("new head: "); head.print();
      headWord = head.headWord();
      headCat = head.lexCat();
      headIndex = head.index();
    } else {
      headWord = null;
      headCat = null;
      headIndex = -1;
    }
    if (dependent != null) {
      argWord = dependent.headWord();
      argCat = dependent.lexCat();
      argIndex = dependent.index();
    } else {
      argWord = null;
      argCat = null;
      argIndex = -1;
    }
    argPos = argPosition;
    extracted = false;
    bounded = true;
    modifier = mod;
    next = null;
  }

  // // PROPBANK
  // DepList(String relation,
  // HeadWordList head,
  // HeadWordList dependent,
  // int argPosition,
  // int dir,
  // // PROPBANK
  // String mylabel,
  // int mystart,
  // int myend,
  // // end PROPBANK
  // boolean mycompl
  // ){ // PROPBANK
  // label = mylabel;
  // start = mystart;
  // end = myend;
  // // end PROPBANK
  // compl = mycompl;
  // rel = relation;
  // argDir = dir;
  // if (head != null){
  // // System.out.println("new head: "); head.print();
  // headWord = head.headWord;
  // headCat = head.lexCat;
  // headIndex = head.index();
  // }
  // else {
  // headWord = null;
  // headCat = null;
  // headIndex = -1;
  // }
  // if (dependent != null){
  // argWord = dependent.headWord;
  // argCat = dependent.lexCat;
  // argIndex = dependent.index();
  // }
  // else {
  // argWord = null;
  // argCat = null;
  // argIndex = -1;
  // }
  // argPos = argPosition;
  // extracted = false;
  // bounded = true;
  // next = null;
  // }
  public void append(DepList deplist) {
    if (next != null) {
      next.append(deplist);
    } else {
      next = deplist;
    }
  }

  public void print() {
    print(System.out);
  }


  public void print(PrintStream out) {
    out.print('[');
    printRec(out);
    out.print(']');

  }

  public String print(String out) {
    out += "[";
    out = printRec(out);
    out += "]\n";
    return out;
  }

  public void printDepGrammar() {
    printDepGrammar(System.out);
  }

  void printDepGrammar(PrintStream out) {
    printDepGrammarRec(out);
    out.println();
  }

  private void printDepGrammarRec(PrintStream out) {
    if (this.modifier)
      out.print(headWord + " <-- " + argWord);
    else
      out.print(headWord + " --> " + argWord);
    if (next != null) {
      out.print(", ");
      next.printDepGrammarRec(out);
    }
  }

  private void printRec(PrintStream out) {
    boolean BRIEF = false;//true;
    if (BRIEF) {
      if (this.modifier)
        out.print(headWord + "<-" + argWord);
      else
        out.print(headWord + "->" + argWord);
      if (next != null) {
        out.print(", ");
        next.printRec(out);
      }
      return;
    }

    if (rel != null) {

      if (extracted) {
        if (bounded) {
          out.print(rel + "_BOUNDED(");
        } else {
          out.print(rel + "_UNBOUNDED(");
        }
      } else
        out.print(rel + '(');

      out.print("H=<" + headWord + ',' + headCat + ">, ");
      // if (headIndex != -1){
      // out.print("H:"+headIndex +", ");
      // }
      out.print("Arg" + argPos + ", ");
      out.print("A=<" + argWord + ',' + argCat + '>');
      // if (argIndex != -1){
      // out.print("A:"+argIndex +", ");
      // }
      out.print(')');
    }
    if (next != null) {
      out.print(", ");

      next.printRec(out);
    }
  }

  private String printRec(String out) {
    boolean BRIEF = true;
    if (BRIEF) {
      if (this.modifier)
        out += headWord + "<-" + argWord;
      else
        out += headWord + "->" + argWord;
      if (next != null) {
        out += ", ";
        next.printRec(out);
      }
      return out;
    }

    if (rel != null) {
      if (extracted) {
        if (bounded) {
          out += rel + "_BOUNDED(";
        } else {
          out += rel + "_UNBOUNDED(";
        }
      } else
        out += rel + '(';

      out += "H=<" + headWord + ',' + headCat + ">, ";
      // if (headIndex != -1){
      // out.print("H:"+headIndex +", ");
      // }
      out += "Arg" + argPos + ", ";
      out += "A=<" + argWord + ',' + argCat + '>';
      // if (argIndex != -1){
      // out.print("A:"+argIndex +", ");
      // }

      out += ")";
      if (modifier) {
        out += "MOD";
      } else
        out += "ARG";
    }
    if (next != null) {
      out += ", ";

      out += next.printRec(out);
    }
    return out;
  }

  public boolean isLocal() {
    return !extracted;
  }

  public boolean isFWextracted() {
    boolean retval = extracted;
    if (retval && argDir == BW) {
      retval = false;
    }
    return retval;
  }

  public boolean isBWextracted() {
    boolean retval = extracted;
    if (retval && argDir == FW) {
      retval = false;
    }
    return retval;
  }

  public void setToExtractedBounded() {
    extracted = true;
    if (bounded)
      bounded = true;
    if (next != null) {
      next.setToExtractedBounded();
    }
  }

  public void setToExtractedUnbounded() {
    extracted = true;
    bounded = false;
    if (next != null) {
      next.setToExtractedUnbounded();
    }
  }

  public boolean containsDependency(int index) {
    boolean retval = false;
    DepList tmp = this;
    while (tmp != null && !(retval)) {
      if (index == tmp.argIndex || index == tmp.headIndex) {
        retval = true;
        break;
      }
      tmp = tmp.next;
    }
    return retval;
  }

  void printGeneratedWords(HeadWordList heads,
                           HeadWordList sisterHeads) {
    if (rel != null) {
      boolean foundHead = false;
      HeadWordList tmpHead = sisterHeads;
      while (tmpHead != null && !(foundHead)) {
        if (tmpHead.word().equals(argWord)) {
          System.out.println("### Generate ArgWord:   " + headWord
                             + '|' + headCat + " ==> " + argWord
                             + '|' + argCat);
          foundHead = true;
        } else {
          if (tmpHead.word().equals(headWord)) {
            System.out.println("### Generate HeadWord:  " + argWord
                              + '|' + argCat + " ==> " + headWord
                              + '|' + headCat);
            foundHead = true;
          } else {
            tmpHead = tmpHead.next();
          }
        }
      }
      if (!(foundHead)) {
        tmpHead = heads;
        while (tmpHead != null && !(foundHead)) {
          if (tmpHead.word().equals(headWord)) {
            System.out.println("### Generate ArgWord:   " + headWord
                               + '|' + headCat + " ==> " + argWord
                               + '|' + argCat);
            foundHead = true;
          } else {
            if (tmpHead.word().equals(argWord)) {
              System.out.println("### Generate HeadWord:  " + argWord
                                 + '|' + argCat + " ==> " + headWord
                                 + '|' + headCat);
              foundHead = true;
            } else {
              tmpHead = tmpHead.next();
            }
          }
        }
      }
      if (!(foundHead)) {
        System.out.println("### ERROR -- Cannot generate dependency: "
          	               + " head:" + headWord + '|' + headCat
          	               + " dep:" + argWord + '|' + argCat);
      }
      if (next != null) {
        next.printGeneratedWords(heads, sisterHeads);
      }
    }
  }

  public DepList next() {
    return next;
  }

  public DepList copy() {
    DepList copy = null;
    DepList tmp, tmpcopy;
    try {
      copy = (DepList) this.clone();
    } catch (Exception E) {
      E.printStackTrace();
    }
    tmp = this;
    tmpcopy = copy;
    while (tmp.next != null) {
      try {
        tmpcopy.next = (DepList) tmp.next.clone();
      } catch (Exception E) {
        E.printStackTrace();
      }
      tmp = tmp.next;
      tmpcopy = tmpcopy.next;
    }
    return copy;
  }

  // return a list of the unfilled dependencies
  public DepList copyUnfilled() {
    DepList copy = null;
    DepList tmp, tmpcopy;
    if (headWord == null || argWord == null) {
      try {
        copy = (DepList) this.clone();
      } catch (Exception E) {
        E.printStackTrace();
      }
    }
    tmp = this;
    tmpcopy = copy;
    while (tmp.next != null) {
      if (tmp.next.headWord == null || tmp.next.argWord == null) {
        if (copy == null) {
          try {
            copy = (DepList) tmp.next.clone();
          } catch (Exception E) {
            E.printStackTrace();
          }
          tmpcopy = copy;
        } else {
          try {
            tmpcopy.next = (DepList) tmp.next.clone();
            tmpcopy = tmpcopy.next;
          } catch (Exception E) {
            E.printStackTrace();
          }
        }
      }
      tmp = tmp.next;
    }
    if (copy != null) {
      // System.out.print("### original: "); this.print();
      // System.out.print("###     copy: "); copy.print();
    }
    // System.out.println("exit copyUnfilled");
    return copy;
  }

  void setToModifier() {
    modifier = true;
    if (next != null) {
      next.setToModifier();
    }
  }
}

// --- END OF FILE:
// /home/julia/CCG/code/StatisticalParser/code/CurrentCVSed/StatCCG/DepList.java

