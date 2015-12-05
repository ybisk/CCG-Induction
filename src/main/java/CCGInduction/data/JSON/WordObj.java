package CCGInduction.data.JSON;

import java.util.Arrays;

/**
 *  Words are the word form, part of speech tag and NER label if applicable
 */
public class WordObj {
  /* NER label:  PERSON/...  */
  String ner;
  /* Word form  */
  public String word;
  /* Word form  */
  public String lemma;
  /* BMMM Cluster */
  public String cluster;
  /* Part of speech tag  */
  public String pos;
  /* Coarse Part of speech tag  */
  public String cpos;
  /* Universal Part of speech tag  */
  public String upos;
  /* Beam of supertags */
  public String[] supertags;


  /**
   * Supertag setter
   * @param categories SuperTags for word
   */
  public void supertags(String... categories) {
      supertags = categories;
  }

  @Override
  public String toString() {
    return JSONFormat.gsonBuilder.disableHtmlEscaping().create().toJson(this);
  }

  @Override
  public boolean equals(Object o) {
    if(!WordObj.class.isInstance(o))
      return false;
    WordObj other = (WordObj)o;
    return (other.ner   == null   ? ner   == null   : other.ner.equals(ner))
        && (other.word  == null   ? word  == null   : other.word.equals(word))
        && (other.lemma == null   ? lemma == null   : other.lemma.equals(lemma))
        && (other.pos   == null   ? pos   == null   : other.pos.equals(pos))
        && (other.cpos  == null   ? cpos  == null   : other.cpos.equals(cpos))
        && (other.upos  == null   ? upos  == null   : other.upos.equals(upos))
        && (other.cluster == null ? cluster == null : other.cluster.equals(cluster))
        && (other.supertags == null ? supertags == null : Arrays.equals(other.supertags, supertags));
  }
}
