package CCGInduction.data.JSON;

/**
* Created by bisk1 on 1/27/15.
*/
public class PARGDep {
  public final String category;
  public final int slot;
  public final int dependent;
  public final int head;
  public final String bounded;
  public PARGDep(String cat, int arg, int dep, int H, String bound) {
    category = cat;
    slot = arg;
    dependent = dep;
    head = H;
    bounded = bound;
  }

  public String toPrettyString(JSONFormat json) {
    return String.format("%2d\t%2d\t%-20s\t%-2d\t%-10s\t%-10s\t%-4s\n",
        dependent, head, category, slot, json.words[dependent].word, json.words[head].word, bounded == null ? "" : bounded);
  }

  @Override
  public String toString() {
    return JSONFormat.gsonBuilder.disableHtmlEscaping().create().toJson(this);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof PARGDep))
      return false;
    PARGDep other = (PARGDep)o;
    return head == other.head && dependent == other.dependent && slot == other.slot &&
        other.category.equals(category);
  }
}
