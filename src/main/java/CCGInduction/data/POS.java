package CCGInduction.data;

import java.io.Serializable;

/**
 * Object representation of POS
 * 
 * @author bisk1
 */
final public class POS implements Serializable {
  private static final long serialVersionUID = -6196587630699275664L;
  /**
   * integer ID
   */
  private final int id;

  /**
   * POS constructor using string.
   * 
   * @param pos String of POS tag
   */
  public POS(String pos) {
    id = Tagset.add(pos);
  }

  @Override
  public boolean equals(Object o) {
    return this == o || POS.class.isInstance(o) && id == ((POS)o).id;
  }

  @Override
  public int hashCode() {
    return id;
  }

  @Override
  public String toString() {
    return Tagset.STRINGS.get(id);
  }

  /**
   * Returns true if the tag type is a number.  This only works for PTB and Universal
   * @return if tag is a number type
   */
  public boolean isNum() {
    return this.toString().equals("NUM") || this.toString().equals("CD");
  }
}
