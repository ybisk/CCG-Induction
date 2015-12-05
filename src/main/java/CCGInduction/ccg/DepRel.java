package CCGInduction.ccg;


/*
 * @author juliahmr
 */
public class DepRel {
  public final String cat;
  public final int slot;
  public final boolean extracted;
  public final boolean bounded;

  // if PROPBANK:
  private String label = null;
  private int start = 0;
  private int end = 0;
  // public boolean compl = false;
  public boolean modifier = false;

  // end PROPBANK
  public DepRel(String myCat, int mySlot, boolean myExtracted,
                boolean myBounded, boolean myMod) {
    cat = myCat;
    slot = mySlot;
    extracted = myExtracted;
    bounded = myBounded;
    modifier = myMod;
  }

  // PROPBANK:
  private DepRel(String myCat, int mySlot, boolean myExtracted,
                 boolean myBounded, String myLabel,
                 int myStart, int myEnd, boolean myCompl) {
    cat = myCat;
    slot = mySlot;
    extracted = myExtracted;
    bounded = myBounded;
    label = myLabel;
    start = myStart;
    end = myEnd;
    modifier = myCompl;// was compl
  }

  public DepRel copy() {
    // PROPBANK
    DepRel copy;
    if (label != null) {
      copy = new DepRel(cat, slot, extracted, bounded,
                        label, start, end, modifier);
    } else
      // END PROPBANK
      copy = new DepRel(cat, slot, extracted, bounded, modifier);
    return copy;
  }

  // METHODS
  // =======
  // for PROPBANK
  String maxProj() {
    if (label != null) {
      return (new StringBuffer(" ")).append(label).append(' ')
              .append(start).append(' ').append(end).append(' ').toString();
    } else
      return " ";
  }



  public static void copyDepRel(DepRel[][] target, DepRel[][] source) {
    if (source != null) {
      for (int i = 0; i < source.length; i++) {
        for (int j = 0; j < source.length; j++) {
          if (source[i][j] != null) {
            target[i][j] = source[i][j].copy();
          }
        }
      }
    }
  }

  public static void addDependency(DepRel[][] target, DepList filled) {
    if (filled != null) {
      fillDepRel(target, filled.argIndex, filled.headIndex, filled.headCat,
          filled.argPos, filled.extracted, filled.bounded, filled.modifier);
    } else {
      System.out.println("ERROR: addDependency: filled is null");
      // printAsTree(System.out);
    }
  }

  private static void fillDepRel(DepRel[][] target, int arg, int head,
                                 String cat, int slot, boolean extracted,
                                 boolean bounded, boolean modifier) {
    target[arg][head] = new DepRel(cat, slot, extracted, bounded, modifier);
  }
}

// --- END OF FILE: /home/julia/CCG/StatCCGChecked/StatCCG/DepRel.java

