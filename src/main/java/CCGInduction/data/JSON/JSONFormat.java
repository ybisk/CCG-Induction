package CCGInduction.data.JSON;

import CCGInduction.ccg.CCGCategoryUtilities;
import CCGInduction.grammar.Grammar;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import CCGInduction.Configuration;
import CCGInduction.data.LexicalToken;
import CCGInduction.data.Sentence;
import CCGInduction.data.Tagset;
import CCGInduction.utils.Logger;
import CCGInduction.utils.TextFile;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class for reading JSON files as defined by sivareddy.
 * Stores a series of entities, words and syntactic parses
 * @author ybisk
 */
public class JSONFormat {

  static final GsonBuilder gsonBuilder = new GsonBuilder();
  private static final Gson gsonReader = new Gson();

  public EntityObj[] entities;
  public WordObj[] words;
  public SynParObj[] synPars;
  public SynParObj[] goldSynPars;
  public String sentence;
  public double parses;
  public String[] answerSubset;

  // Used by Siva's code
  // Not requred for class equality
  public Integer boundedVarCount;
  public Integer freeVarCount;
  public Integer freeEntityCount;
  public Integer foreignEntityCount;
  public Integer negationCount;

  public static String pargString(PARGDep[] deps, JSONFormat json) {
    String s = "<s> " + deps.length + "\n";
    for (PARGDep dep : deps)
      s += dep.toPrettyString(json);
    return s + "<\\s>";
  }

  public static String conllString(CoNLLDep[] deps, JSONFormat json) {
    String s = "";
    for (CoNLLDep dep : deps)
      s += dep.toPrettyString(json);
    return s;
  }

  public boolean equals(Object o) {
    if (!JSONFormat.class.isInstance(o))
      return false;
    JSONFormat other = (JSONFormat)o;
    return Arrays.deepEquals(entities, other.entities)
        && Arrays.deepEquals(words, other.words)
        && Arrays.deepEquals(synPars, other.synPars)
        && (sentence == null ? other.sentence == null : sentence.equals(other.sentence));
    // Does not require variable accounts in equality
  }

  @Override
  public String toString() {
    for (WordObj word : words){
      if (word.lemma != null && (word.upos.equals("NUM") || word.pos.equals("CD"))) {
        word.word = word.lemma;
        word.lemma= null;
      }
    }
    return gsonBuilder.disableHtmlEscaping().create().toJson(this);
  }

  /**
   * Returns the FBID for word with index i
   * @param index word index
   * @return Freebase MID
   */
  public String FBID_for_entity(int index) {
    if (entities == null) {
      return "";
    }
    for (EntityObj eo : entities) {
      if (eo.index == index) {
        return eo.entity;
      }
    }
    return "";
  }

  /**
   * Returns the string for word i
   * @param index word index
   * @return string of word
   */
  public String word(int index) {
    return words[index].word;
  }

  /**
   * Returns the string for tag i
   * @param index tag index
   * @return string of POS tag
   */
  public String tag(int index) {
    return words[index].pos;
  }

  public void addWord(String word, String lemma, String pos, String cpos, String upos, String cat, String ner) {
    addWord(word, lemma, pos, cpos, upos, new String[]{cat}, ner);
  }
  /**
   * Creates a new WordObj and populates the values
   */
  public void addWord(String word, String lemma, String pos, String cpos, String upos, String[] cat, String ner) {
    if (words == null)
      words = new WordObj[1];
    else
      words = Arrays.copyOf(words, words.length + 1);
    words[words.length-1] = new WordObj();
    words[words.length-1].word  = word;
    words[words.length-1].lemma = lemma;
    words[words.length-1].pos   = pos;
    words[words.length-1].cpos  = cpos;
    words[words.length-1].upos  = upos;
    words[words.length-1].ner   = ner;
    words[words.length-1].supertags = cat;
  }

  /**
   * If there is no JSON object for a given sentence, this will create one from the LexicalToken objects
   * @param sentenceObj Sentence to edit
   * @param grammar Grammar with hashes
   */
  public static void createFromSentence(Sentence sentenceObj, Grammar grammar) {
    if (sentenceObj.JSON != null)
      return;

    JSONFormat JSON = new JSONFormat();
    JSON.words = new WordObj[sentenceObj.length()];
    LexicalToken token;
    for (int index = 0; index < sentenceObj.length(); ++index) {
      token = sentenceObj.get(index);
      JSON.words[index] = new WordObj();
      JSON.words[index].word  = token.word(grammar);
      JSON.words[index].pos   = token.fine().toString();
      JSON.words[index].lemma = token.lemma(grammar);
      JSON.words[index].cpos  = token.coarse().toString();
      JSON.words[index].upos  = token.universal().toString();
      JSON.words[index].supertags(token.cat(grammar));
      JSON.words[index].cluster = token.induced() != null ? token.induced().toString() : null;
      if (Configuration.tagType.equals(Tagset.TAG_TYPE.Induced))
        JSON.words[index].cluster  = token.tag().toString();

      // Add entities if present
      if (!token.FBid().isEmpty()) {
        if (JSON.entities == null) {
          JSON.entities = new EntityObj[0];
        }
        JSON.entities = Arrays.copyOf(JSON.entities, JSON.entities.length+1);
        // Create entity object with dummy resolver score
        JSON.entities[JSON.entities.length-1] = new EntityObj(index, token.FBid(), 1.0);
      }
    }

    // Assign reference to sentence
    sentenceObj.JSON = JSON;
  }

  /**
   * Checks if there are any words in the JSON
   * @return If empty
   */
  boolean isEmpty() { return words == null || words.length == 0;}

  /**
   * Returns the sentence length excluding punctuation
   * @return length
   */
  public int length_noP() {
    String period = ".";
    int i = 0;
    for (WordObj word : words) {
      i += word.upos.equals(period) ? 0 : 1;
    }
    return i;
  }

  /**
   * Main class for converting standard input formats (CoNLL, AUTO) to JSON
   * TODO: Convert PARG files
   * @param args Source file type and list of files to convert
   */
  public static void main(String[] args) throws IOException {
    if (args.length < 2) {
      System.out.println("Please provide input format:  AUTO, CONLL, NAACL");
      System.out.println("Please provide input format a list of files");
      return;
    }

    fileType type = fileType.valueOf(args[0]);

    // Iterate over files
    for (int i = 1; i < args.length; ++i) {
      System.out.print("Converting: " + args[i]);
      ArrayList<JSONFormat> input = null;
      switch(type) {
        case AUTO:
          input = readAUTO(args[i]);
          break;
        case CONLL:
          input = readCoNLL(args[i], false);
          break;
        case NAACL:
          input = readCoNLL(args[i], true);
          break;
        case PARG:
          System.err.println("PARG is not currently supported");
          return;
      }
      BufferedWriter writer = TextFile.Writer(args[i].split(".gz")[0] + ".json.gz");
      GsonBuilder gson = new GsonBuilder();
      for (JSONFormat JSON : input) {
        writer.write(gson.disableHtmlEscaping().create().toJson(JSON) + "\n");
      }
      System.out.println("\t\t" + input.size() + " sentences");
      writer.close();
    }
  }

  /**
   * Read in a JSON file.  One JSON per line
   * @param filename Source file
   * @return ArrayList of JSON objects
   */
  public static ArrayList<JSONFormat> readJSON(String filename) {
    ArrayList<JSONFormat> parses = new ArrayList<>();
    List<String> file = TextFile.Read(filename);
    for (String line : file)
      parses.add(gsonReader.fromJson(line, JSONFormat.class));
    return parses;
  }

  /**
   * Create JSON object from serialized string
   * @param serializedString JSON formatted string
   */
  public static JSONFormat deSerialize(String serializedString) {
    return gsonReader.fromJson(serializedString, JSONFormat.class);
  }

  /**
   * Reads in an AUTO file to pack parses, words and tags into a JSON.  Parse failures are ignored
   * @param filename Source File
   * @return JSON version of file
   */
  public static ArrayList<JSONFormat> readAUTO(String filename) {
    ArrayList<JSONFormat> parses = new ArrayList<>();
    List<String> file =  TextFile.Read(filename);
    String[] split, leaf;
    JSONFormat JSON;
    for (String line : file) {
      if (line.charAt(0) == '(') {
        split = CCGCategoryUtilities.lexical_cat.split(line);

        // Create JSON and place parse inside it
        JSON = new JSONFormat();
        JSON.synPars = new SynParObj[] { new SynParObj() };
        JSON.synPars[0].synPar = line;
        JSON.words = new WordObj[split.length-1];

        // For each leaf node
        for (int i = 1; i < split.length; ++i) {
          leaf = Logger.whitespace_pattern.split(CCGCategoryUtilities.close_lexical.split(split[i])[0]);
          JSON.words[i-1] = new WordObj();
          if (leaf.length == 6) {
            // space   Category  Tag  Aug-Tag  Word  IndexedCategory
            //JSON.words[i-1].supertags(leaf[1]);
            JSON.words[i-1].pos = leaf[2];
            JSON.words[i-1].word = leaf[4];
          } else {
            // C&C Style
            // space   Category  Word  lemma  Tag  extrax3
            //JSON.words[i-1].supertags(leaf[1]);
            JSON.words[i-1].word = leaf[2];
            JSON.words[i-1].pos = leaf[4];
          }
        }
        parses.add(JSON);
      }
    }
    return parses;
  }

  /**
   * Reads in a CoNLL file to words and tags in a JSON.  Dependency data is ignored
   * @param filename Source File
   * @return JSON version of file
   */
  public static ArrayList<JSONFormat> readCoNLL(String filename, boolean withUPOS) {
    ArrayList<JSONFormat> sentences = new ArrayList<>();
    List<String> file = TextFile.Read(filename);
    String[] split;
    JSONFormat JSON = null;
    for (String line : file) {
      if (line.trim().length() != 0)
        split = Logger.whitespace_pattern.split(line.trim());
      else
        split = null;
      if (split == null && JSON != null) {
        WordObj[] words = new WordObj[0];
        for (WordObj word : JSON.words) {
          if (word != null) {
            words = Arrays.copyOf(words, words.length+1);
            words[words.length-1] = word;
          }
        }
        JSON.words = words;
        sentences.add(JSON);
      } else if (split != null) {          // Force existance of
        int index = Integer.parseInt(split[0]) - 1;
        if (index == 0) {
          JSON = new JSONFormat();
          JSON.words = new WordObj[0];
        }
        // Index word lemma cpos pos upos
        JSON.words = Arrays.copyOf(JSON.words,index+1);     // FIXME:  Ineffecient
        JSON.words[index] = new WordObj();
        JSON.words[index].word  = split[1];
        JSON.words[index].lemma = split[2];
        JSON.words[index].cpos  = split[3];
        JSON.words[index].pos   = split[4];
        if (withUPOS) {
          JSON.words[index].upos = split[5];
        } else if (split[4].equals("-1")) {
          JSON.words[index].upos = ".";
        } else {
          JSON.words[index].upos = "O";
        }
        if (split.length > 6 + (withUPOS ? 1 : 0) && !split[6 + (withUPOS ? 1 : 0)].equals("_")) {
          if (JSON.synPars == null) {
            JSON.synPars = new SynParObj[]{new SynParObj()};
            JSON.synPars[0].conllParse = new CoNLLDep[0];
          }
          JSON.synPars[0].conllParse = Arrays.copyOf(JSON.synPars[0].conllParse, index + 1);
        }
        if (split.length > 7 + (withUPOS ? 1 : 0) && !split[7 + (withUPOS ? 1 : 0)].equals("_")) {
          if (JSON.synPars == null) {
            JSON.synPars = new SynParObj[]{new SynParObj()};
            JSON.synPars[0].conllParse = new CoNLLDep[0];
          }
          JSON.synPars[0].conllParse[index] = new CoNLLDep(index + 1, Integer.valueOf(split[6 + (withUPOS ? 1 : 0)]), split[7 + (withUPOS ? 1 : 0)]);
        }
      }
    }
    if (JSON != null && !JSON.isEmpty())
      sentences.add(JSON);

    return sentences;
  }

  enum fileType {
    AUTO, CONLL, NAACL, PARG
  }


}
