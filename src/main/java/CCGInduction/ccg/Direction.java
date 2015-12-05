package CCGInduction.ccg;

/**
 * InducedCAT Slash direction
 * 
 * @author bisk1
 */
public enum Direction {
  /**
   * X/Y
   */
  FW(1),
  /**
   * X\Y
   */
  BW(2),
  /**
   * X
   */
  None(3);

  public String toString() {
    switch (this) {
    case BW:
      return "\\";
    case FW:
      return "/";
    case None:
      return "|";
    default:
      return "|";
    }
  }

  /**
   * ID
   */
  final int id;

  Direction(int i) {
    this.id = i;
  }

}
