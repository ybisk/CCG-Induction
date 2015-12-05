package CCGInduction.data;

import CCGInduction.Configuration;
import CCGInduction.grammar.Grammar;
import CCGInduction.hmm.BigramModel;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static CCGInduction.data.Tagset.TAG_TYPE.*;

/**
 * A Lexical item which contains part-of-speech tags, word forms and optional
 * category
 * 
 * @author bisk1
 */
public class LexicalToken implements Serializable {
  private final long[] words = new long[3];
  /**
   * The POS tags ( coarse, fine, universal, induced )
   */
  private final POS[] tags = new POS[4];
  private long[] categories;
  private String freebase = "";

  /**
   * Definte a lexical item
   * 
   * @param word    word hash
   * @param lemma   lemma hash
   * @param CPOSTAG Coarse POS tag
   * @param POSTAG  Fine POS tag
   * @param UNIVERSAL Universal POS tag
   */
  public LexicalToken(long raw, long word, long lemma, POS CPOSTAG, POS POSTAG, POS UNIVERSAL, POS INDUCED, String fbid) {
    words[0] = word;
    words[1] = lemma;
    words[2] = raw;
    tags[0] = CPOSTAG;
    tags[1] = POSTAG;
    tags[2] = UNIVERSAL;
    tags[3] = INDUCED;
    if (!fbid.equals("_")) {
      freebase = fbid;
    }
  }

  /**
   * Set's the POS tag
   * 
   * @param posTag POS tag
   */
  public void tag(POS posTag) {
    switch (Configuration.tagType) {
    case Coarse:
      tags[0] = posTag;
      break;
    case Custom:
    case Fine:
      tags[1] = posTag;
      break;
    case Universal:
      tags[2] = posTag;
      break;
    case Induced:
      tags[3] = posTag;
      break;
    default:
      throw new Grammar.GrammarException("Invalid tag type:" + Configuration.tagType);
    }
  }

  /**
   * Retrieves the POS tag
   * 
   * @return POS
   */
  public POS tag() {
    switch (Configuration.tagType) {
    case Coarse:
      return tags[0];
    case Custom:
    case Fine:
      return tags[1];
    case Universal:
      return tags[2];
    case Induced:
      return tags[3];
    }
    return null;
  }

  /**
   * Returns induced Tag
   * @return Induced POS tag
   */
  public POS induced() {
    return tags[3];
  }

  /**
   * Return word if it's been seen, else TAG
   * 
   * @param learnedWords Learned words
   * @param grammar Grammar
   * @return hash of word
   */
  public long wordOrTag(ConcurrentHashMap<Long, Boolean> learnedWords, Grammar grammar) {
    long w = word();
    if (!learnedWords.containsKey(w)) {
      return grammar.Lex("UNK:" + tag());
    }
    return w;
  }

  /**
   * Prints to stream a lexical line word lemma coarse fine universal induced
   * 
   * @param grammar Grammar
   * @param writer Buffered Output Writer
   * @throws IOException
   */
  public final void print(Grammar grammar, Writer writer) throws IOException {
    String word = grammar.Words.get(words[0]);
    String lemma = grammar.Words.get(words[1]);
    String CPOS = tags[0].toString();
    String POS = tags[1].toString();
    String UPOS = tags[2].toString();
    String IPOS = tags[3].toString();
    writer.write(String.format("%-25s %-20s %-10s %-10s %-10s %-10s\n", word, lemma, CPOS, POS, UPOS, IPOS));
  }

  /**
   * Get word
   * 
   * @return word hash
   */
  public long word() {
    return words[0];
  }

  public long rawWord() {
    return words[2];
  }

  /**
   * Get word as String
   * 
   * @param grammar Grammar
   * @return String representation of word
   */
  public String word(Grammar grammar) {
    return grammar.Words.get(words[0]);
  }

  /**
   * Get word as String
   *
   * @param val New Word representation
   */
  public void word(long val) {
    words[0] = val;
  }

  /**
   * Get Lemma
   * 
   * @return lemma hash
   */
  public long lemma() {
    return words[1];
  }

  /**
   * Get Lemma as String
   * 
   * @param grammar Grammar
   * @return String representation of lemma
   */
  public String lemma(Grammar grammar) {
    return grammar.Words.get(words[1]);
  }

  /**
   * Get Coarse tag
   * 
   * @return Coarse POS
   */
  public POS coarse() {
    return tags[0];
  }

  /**
   * Get Fine tag
   * 
   * @return Fine POS
   */
  public POS fine() {
    return tags[1];
  }

  /**
   * Get Universal tag
   * 
   * @return Universal POS
   */
  public POS universal() {
    return tags[2];
  }

  /**
   * Set Induced POS tag
   * 
   * @param inducedTag Induced POS Tag
   */
  public void induced(POS inducedTag) {
    tags[3] = inducedTag;
  }

  /**
   * Get Category
   * 
   * @return SuperTags' hash
   */
  public long[] cat() {
    return categories;
  }

  /**
   * Set lexical category
   * @param superTags Category
   */
  public void cat(List<Long> superTags){
    categories = new long[superTags.size()];
    for (int i = 0; i < categories.length; ++i)
      categories[i] = superTags.get(i);
  }

  /**
   * Get Category as string.
   * 
   * @param grammar Grammar
   * @return String representation of SuperTag
   */
  public String[] cat(Grammar grammar) {
    if (categories == null)
      return null;
    String[] cats = new String[categories.length];
    for (int i = 0; i < cats.length; ++i)
      cats[i] = grammar.prettyCat(categories[i]);
    return cats;
  }

  public String FBid() {
    return freebase;
  }

  public long wordOrUnk(ConcurrentHashMap<Long, Boolean> knownWords) {
    if (knownWords.containsKey(rawWord()))
      return rawWord();
    else
      return BigramModel.UNK;
  }
}
