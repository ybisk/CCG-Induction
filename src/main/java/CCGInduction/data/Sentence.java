package CCGInduction.data;

import CCGInduction.Configuration;
import CCGInduction.ccg.CCGCategoryUtilities;
import CCGInduction.data.JSON.JSONFormat;
import CCGInduction.experiments.Training;
import CCGInduction.grammar.Grammar;
import CCGInduction.ccg.CCGAtomic;
import CCGInduction.ccg.InducedCAT;
import CCGInduction.data.JSON.WordObj;
import CCGInduction.grammar.Rule_Type;
import CCGInduction.utils.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Data-structure for data read in that stores all information given.
 *
 * @author bisk1
 */
public class Sentence implements Externalizable, Iterable<LexicalToken> {
  private LexicalToken[] sentence = new LexicalToken[0];
  /**
   * Full sentence with punctuation
   */
  public LexicalToken[] sentence_wP = new LexicalToken[0];

  // Store the input JSON.  We are not serializaing this object
  // so it will be returned to null during training.  But will last
  // long enough for access during testing.
  public transient JSONFormat JSON;
  public transient String AUTOparse;

  public int firstWord = -1;
  public int lastWord = 0;

  /**
   * Unique (within a Sentences collection) indicator for a sentence
   */
  public int id;

  /**
   * Is this a question or a statement (based on final punctuation)
   */
  private boolean statement = false;
  private boolean question = false;

  /**
   * Empty sentence
   */
  public Sentence() {
  }

  /**
   * Creates a Sentence object from an AUTO file.
   *
   * @param AUTO    Sentence parse
   * @param grammar Grammar instance
   */
  public Sentence(String AUTO, Grammar grammar) {
    String[] words = CCGCategoryUtilities.AUTOtoWords(AUTO);
    String[] tags = CCGCategoryUtilities.AUTOtoTags(AUTO);
    String[] categories = CCGCategoryUtilities.AUTOtoCATS(AUTO);
    for (int i = 0; i < words.length; ++i) {
      WordObj word = new WordObj();
      word.word = words[i];
      word.pos = tags[i];
      if (Configuration.source != Training.supervised)
        word.supertags = new String[] {categories[i]};
      addWord(word, "", grammar);
    }

    if (words[words.length-1].equals("?"))
      question = true;
    else
      statement = true;
    AUTOparse = AUTO;
  }

  /**
   * Copies a sentence's data-structures of Lexical Items
   *
   * @param toCopy  Sentence to copy
   */
  void copy(Sentence toCopy) {
    sentence = Arrays.copyOf(toCopy.sentence, toCopy.sentence.length);
    sentence_wP = Arrays.copyOf(toCopy.sentence_wP, toCopy.sentence_wP.length);
    JSON = toCopy.JSON;
    AUTOparse = toCopy.AUTOparse;
    statement = toCopy.statement;
    question = toCopy.question;
  }

  /**
   * Add a word to a sentence ( requires knowledge of the grammar ). Adds all
   * tag types and lemma.
   *
   * @param line    A CoNLL/NAACL style line
   * @param grammar Grammar with hashes of words
   */
  final void addWord(String line, Grammar grammar) {
    String[] s = Logger.whitespace_pattern.split(line);

    long lemma = grammar.Lex(s[2]);
    long raw = grammar.Lex(s[1]);
    long word;
    String w;
    if (s[3].contains("null")) {
      s[3] = "CD";
    }
    POS tag = new POS(s[3]);
    s[1] = s[1].toLowerCase();
    if (s[1].equals("?")) {
      question = true;
      statement = false;
    } else {
      statement = true;
      question = false;
    }
    if (tag.isNum() || (Configuration.hasUniversalTags && s[5].equals("NUM"))) {
      w = Tagset.convertNumber(s[1]);
      lemma = grammar.Lex(s[1]);
    } else if (s[1].equals("_")) {
      w = "UNDERSCORE";
    } else {
      w = s[1];
    }

    word = grammar.Lex(w);

    POS CPOSTAG = new POS(s[3]);
    POS POSTAG = new POS(process(s[4]));
    POS Universal = new POS("_");
    if (Configuration.hasUniversalTags) {
      Universal = new POS(s[5]);
    }
    if (s[3].equals("_")) {
      CPOSTAG = Universal;
    }
    if (s[4].equals("_")) {
      POSTAG = Universal;
    }

    String fbid = "";
    if (s.length >= 7) {
      fbid = s[6];
    }

    LexicalToken lt = new LexicalToken(raw, word, lemma, CPOSTAG, POSTAG, Universal, new POS("_"), fbid);
    // Use actual word form as the tag for punctuation
    if (!Configuration.ignorePunctuation && Configuration.source != Training.supervised &&
        (Tagset.Punct(lt.tag()))) {// || TAGSET.Punct(lt.coarse()))) {// || TAGSET.Punct(lt.universal()))) {
      POS new_tag = new POS(s[1]);
      lt.tag(new_tag);
      Tagset.addPunctuationTag(new_tag);
      if (Configuration.source.equals(Training.induction)) {
        CCGAtomic atomic_p = new CCGAtomic(new_tag.toString());
        grammar.createRule(grammar.NT(new InducedCAT(atomic_p)), grammar.Lex(new_tag.toString()), Rule_Type.PRODUCTION);
        InducedCAT.punc = InducedCAT.add(InducedCAT.punc, atomic_p);
      }
    }

    sentence_wP = Arrays.copyOf(sentence_wP, sentence_wP.length + 1);
    sentence_wP[sentence_wP.length - 1] = lt;
    if (!Tagset.Punct(lt.tag())) {
      sentence = Arrays.copyOf(sentence, sentence.length + 1);
      sentence[sentence.length - 1] = lt;
    }
  }

  /**
   * Adds a word to the sentence object.  Requires the string representation of the
   * word, tag and Freebase ID.  The grammar then converts the word and tag into longs
   * for the remainder of computation.
   * @param word_obj Word
   * @param FBID     Freebase ID
   * @param g        Grammar
   */
  final void addWord(WordObj word_obj, String FBID, Grammar g) {
    // Hash word and tag to create LexicalToken
    long word = g.Lex(word_obj.word.toLowerCase());

    if (word_obj.word.equals("?")) {
      question = true;
      statement = false;
    } else {
      question = false;
      statement = true;
    }

    long raw = g.Lex(word_obj.word);
    POS POSTAG = new POS(word_obj.pos);

    if (word_obj.cpos == null) {
      word_obj.cpos = "_";
    }
    if (word_obj.upos == null) {
      word_obj.upos = "_";
    }
    POS CPOSTAG = new POS(word_obj.cpos);
    POS Universal = new POS(word_obj.upos);
    long lemma = g.Lex("_");
    if (word_obj.lemma != null) {
      lemma = g.Lex(word_obj.lemma);
    }
    POS Induced = new POS(word_obj.cluster);

    // In the case of numbers, we lexicalize with the word converted: 10.00 -> XX.XX
    // but store the original value in the lemma
    if ((word_obj.pos != null && word_obj.pos.equals("CD"))
        || (word_obj.upos != null && word_obj.upos.equals("NUM"))) {
      lemma = word;
      word = g.Lex(Tagset.convertNumber(word_obj.word));
    }
    LexicalToken lt = new LexicalToken(raw, word, lemma, CPOSTAG, POSTAG, Universal, Induced, FBID);

    // Create emission rule based on SuperTag
    if (word_obj.supertags != null && word_obj.supertags.length != 0) {
      ArrayList<Long> cats = new ArrayList<>();
      for (String cat : word_obj.supertags) {
        // Check if this is a primitive (like punctuation)
        if (Tagset.valueOf(cat) != null) {
          CCGAtomic atomic_p = new CCGAtomic(cat);
          cats.add(g.NT(new InducedCAT(atomic_p)));
        } else {
          cats.add(g.NT(InducedCAT.valueOf(cat)));
        }
      }
      lt.cat(cats);
    }

    // Special case of punctuation changing tagset.
    // We elevate the lexical punctuation emissions to being grammatical symbols
    if (!Configuration.ignorePunctuation && Tagset.Punct(lt.tag()) && Configuration.source != Training.supervised) {
      POS new_tag = new POS(word_obj.word);
      lt.tag(new_tag);
      Tagset.addPunctuationTag(new_tag);
      CCGAtomic atomic_p = new CCGAtomic(new_tag.toString());
      g.createRule(g.NT(new InducedCAT(atomic_p)), g.Lex(new_tag.toString()), Rule_Type.PRODUCTION);
      InducedCAT.punc = InducedCAT.add(InducedCAT.punc, atomic_p);
    }

    // Add as newest word in the sentence
    sentence_wP = Arrays.copyOf(sentence_wP, sentence_wP.length + 1);
    sentence_wP[sentence_wP.length - 1] = lt;
    // Also store a copy of the sentence without punctuation
    if (!Tagset.Punct(lt.tag())) {
      sentence = Arrays.copyOf(sentence, sentence.length + 1);
      sentence[sentence.length - 1] = lt;
    }
  }

  private static String process(String s) {
    if (Configuration.tagType.equals(Tagset.TAG_TYPE.Custom)) {
      if (Configuration.TAGSET.contains("dutch")) {
        return s.split("_")[0];
      }
      if (Configuration.TAGSET.contains("danish")) {
        String n = s.substring(0, 3);
        if (Tagset.STRINGS.contains(n)) {
          return n;
        }
        return s.substring(0, 2);
      }
    }
    return s;
  }

  final void computeFirstAndLast() {
    if (Configuration.ignorePunctuation) {
      lastWord = sentence.length - 1;
      firstWord = 0;
      return;
    }
    for (int i = 0; i < sentence_wP.length; i++) {
      if (!Tagset.Punct(sentence_wP[i].tag())) {
        lastWord = i;
        if (firstWord == -1) {
          firstWord = i;
        }
      }
    }
  }

  /**
   * Returns the length of the sentence without punctuation
   *
   * @return int
   */
  public final int length_noP() {
    return sentence.length;
  }

  /**
   * Returns the length of the sentence (depends on if we are ignoring
   * punctuation )
   *
   * @return int
   */
  public final int length() {
    if (Configuration.ignorePunctuation) {
      return sentence.length;
    }
    return sentence_wP.length;
  }

  /**
   * Return the lexicaltoken at a specific index
   *
   * @param index Index of desired token
   * @return LexicalToken
   */
  public final LexicalToken get(int index) {
    if (Configuration.ignorePunctuation) {
      return sentence[index];
    }
    return sentence_wP[index];
  }

  /**
   * Get the token where index is affected by punctuation
   *
   * @param index Index of desired token in sentence with punctuation
   * @return LexicalToken
   */
  public final LexicalToken getWP(int index) {
    return sentence_wP[index];
  }

  /**
   * Get the token where index is not affected by punctuation
   *
   * @param index Index of desired token in sentence without punctuation
   * @return Lexical Token
   */
  public final LexicalToken getNP(int index) {
    return sentence[index];
  }

  /**
   * Return sentence as a string of pos-tags
   *
   * @return String of sentence tags
   */
  public final String asTags() {
    String toRet = "";
    for (int i = 0; i < length(); i++) {
      toRet += get(i).tag() + " ";
    }
    return toRet;
  }

  /**
   * Returns sentence (with punctuation) as pos-tags
   *
   * @return sentence
   */
  final String withPunctuation() {
    String toRet = "";
    for (LexicalToken token : sentence_wP) {
      toRet += token.fine() + " ";
    }
    return toRet;
  }

  /**
   * Returns sentence yield
   *
   * @param g Grammar
   * @param i from
   * @param j to
   * @return Yield
   */
  final String asWords(Grammar g, int i, int j) {
    String toRet = "";
    for (; i <= j; i++) {
      toRet += get(i).word(g) + " ";
    }
    return toRet;
  }

  final String asWords(Grammar g) {
    return asWords(g, 0, this.length() - 1);
  }

  /**
   * Retrieve sentence as tagged string
   *
   * @param grammar Grammar story word hashes
   * @return String of words and tags in sentence
   */
  public final String toString(Grammar grammar) {
    String toRet = "";
    for (int i = 0; i < length(); i++) {
      toRet += "< " + grammar.Words.get(get(i).word()) + " " + get(i).tag() + " > ";
    }
    return toRet;
  }

  /**
   * Prints to stream ONE word per line
   *
   * @param grammar Grammar
   * @param writer  Buffered Writer
   * @throws IOException
   */
  public final void print(Grammar grammar, Writer writer) throws IOException {
    if (Configuration.ignorePunctuation) {
      for (LexicalToken lt : sentence) {
        lt.print(grammar, writer);
      }
    } else {
      for (LexicalToken lt : sentence_wP) {
        lt.print(grammar, writer);
      }
    }
    writer.write("\n");
  }

  /**
   * Return a subset of the sentence (words and tags)
   *
   * @param leftIndex  left index
   * @param rightIndex right index
   * @param grammar    Grammar with hashes of words
   * @return String of a subset of the sentence
   */
  public final String subString(int leftIndex, int rightIndex, Grammar grammar) {
    String toRet = "";
    for (int i = leftIndex; i <= rightIndex; i++) {
      toRet += "< " + grammar.Words.get(get(i).word()) + " " + get(i).tag() + " > ";
    }
    return toRet;
  }

  /**
   * Return a subset of the sentence
   *
   * @param leftIndex  left index
   * @param rightIndex right index
   * @param grammar    Grammar with hashes of words
   * @return String of subset of sentence with punctuation
   */
  final String subStringWithP(int leftIndex, int rightIndex, Grammar grammar) {
    String toRet = "";
    for (int i = leftIndex; i <= rightIndex; i++) {
      toRet += "< " + grammar.Words.get(sentence_wP[i].word()) + " "
          + sentence_wP[i].tag() + " > ";
    }
    for (int i = rightIndex; i < sentence_wP.length; i++) {
      if (Tagset.Punct(sentence_wP[i].tag())) {
        toRet += "< " + grammar.Words.get(sentence_wP[i].word()) + " "
            + sentence_wP[i].tag() + " > ";
      } else {
        return toRet;
      }
    }
    return toRet;
  }

  /**
   * Redefine punctuation
   */
  public final void refactorPunctuation() {
    ArrayList<LexicalToken> noP = new ArrayList<>();
    for (LexicalToken lt : sentence_wP) {
      if (!Tagset.Punct(lt.tag())) {
        noP.add(lt);
      }
    }
    sentence = new LexicalToken[noP.size()];
    for (int i = 0; i < sentence.length; i++) {
      sentence[i] = noP.get(i);
    }
  }

  @Override
  public void readExternal(ObjectInput in) throws IOException,
      ClassNotFoundException {
    sentence = (LexicalToken[]) in.readObject();
    sentence_wP = (LexicalToken[]) in.readObject();
    computeFirstAndLast();
  }

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeObject(sentence);
    out.writeObject(sentence_wP);
  }

  /**
   * Returns the sentence represented by simply an array of POS tags
   *
   * @return POS[] representation
   */
  public POS[] getTags() {
    POS[] arr = new POS[length()];
    for (int i = 0; i < length(); i++) {
      arr[i] = get(i).tag();
    }
    return arr;
  }

  /**
   * Returns the sentence (w/ Punct) as POS tags
   *
   * @return POS[] with Punct
   */
  public POS[] getTagsWithPunct() {
    POS[] arr = new POS[sentence_wP.length];
    for (int i = 0; i < sentence_wP.length; i++) {
      arr[i] = sentence_wP[i].tag();
    }
    return arr;
  }

  @Override
  public String toString() {
    return withPunctuation();
  }

  @Override
  public Iterator<LexicalToken> iterator() {
    return new Iterator<LexicalToken>() {
      private int currentIndex = 0;

      @Override
      public boolean hasNext() {
        return currentIndex < sentence_wP.length && sentence_wP[currentIndex] != null;
      }

      @Override
      public LexicalToken next() {
        return sentence_wP[currentIndex++];
      }

      @Override
      public void remove() {
        // Not allow for removal
      }
    };
  }

  public boolean isQuestion() {
    return question;
  }

  public boolean isStatement() {
    return statement;
  }
}
