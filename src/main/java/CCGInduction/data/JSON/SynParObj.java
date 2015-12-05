package CCGInduction.data.JSON;

import java.util.Arrays;

/**
 * A syntactic parse in AUTO format with a score or probability
 */
public class SynParObj {
  public String synPar;
  public PARGDep[] depParse;
  public CoNLLDep[] conllParse;
  public double score;

  @Override
  public String toString() {
    return JSONFormat.gsonBuilder.disableHtmlEscaping().create().toJson(this);
  }

  @Override
  public boolean equals(Object o) {
    if (!SynParObj.class.isInstance(o))
      return false;
    SynParObj other = (SynParObj)o;
    return (synPar == null ? other.synPar == null : synPar.equals(other.synPar))
        && (depParse == null ? other.depParse == null : Arrays.equals(depParse, other.depParse))
        && (conllParse == null ? other.conllParse == null : Arrays.equals(conllParse, other.conllParse))
        && score == score;
  }

  }
