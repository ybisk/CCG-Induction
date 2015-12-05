package CCGInduction.ccg;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Defines CCG Atomics for InducedCAT
 *
 * @author bisk1
 */
public class CCGAtomic implements Serializable {
  private static final long serialVersionUID = 1L;
  /**
   * Names of all CCG Atomic categories (S,N,CC, ...)
   */
  public static final ArrayList<String> IDS = new ArrayList<>();
  /**
   * Uniq ID
   */
  private final int ID;

  /**
   * Constructor using String, does not duplicate
   * @param val String representation of category
   */
  public CCGAtomic(String val) {
    int i = IDS.indexOf(val);
    if (i == -1) {
      IDS.add(val);
      ID = IDS.size() - 1;
    } else {
      ID = i;
    }
  }

  @Override
  public int hashCode() {
    return ID;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof CCGAtomic && ((CCGAtomic) o).ID == ID;
  }

  @Override
  public String toString() {
    return IDS.get(ID);
  }

  public static CCGAtomic valueOf(char[] str){
    return valueOf(String.valueOf(str));
  }
  private static CCGAtomic valueOf(String str){
    if(IDS.contains(str)) {
      return new CCGAtomic(str);
    }
    return null;
  }
}
