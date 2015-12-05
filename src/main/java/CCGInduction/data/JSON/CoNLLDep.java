package CCGInduction.data.JSON;

/**
* Created by bisk1 on 1/27/15.
*/
public class CoNLLDep {
  public final int index;
  public final int head;
  public final String label;

  public CoNLLDep(int ind, int H, String lab) {
    index = ind;
    head = H;
    label = lab;
  }

  @Override
  public String toString() {
    return JSONFormat.gsonBuilder.disableHtmlEscaping().create().toJson(this);
  }

  public String toPrettyString(JSONFormat json) {
    // Index, word, lemma, coarse, fine, upos, feats, head, label
    return String.format("%3d\t%-10s\t%-10s\t%-5s\t%-5s\t%-5s\t_\t%-3d\t%s\n",
        index, json.words[index - 1].word, json.words[index - 1].lemma, json.words[index - 1].cpos,
        json.words[index - 1].pos, json.words[index - 1].upos, head, label);
  }

  @Override
  public boolean equals(Object o){
    if (!(o instanceof CoNLLDep))
      return false;
    CoNLLDep other = (CoNLLDep)o;
    return index == other.index && head == other.head && label.equals(other.label);
  }
}
