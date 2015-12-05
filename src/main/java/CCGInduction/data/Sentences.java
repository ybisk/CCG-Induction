package CCGInduction.data;

import CCGInduction.data.JSON.JSONFormat;
import CCGInduction.grammar.Grammar;
import CCGInduction.utils.Logger;
import CCGInduction.utils.TextFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The purpose of this class is to simply provide a method for reading
 * sentence objects.  While not currently threaded, there is nothing
 * prohibiting it from becoming so.
 * @author bisk1
 */
public class Sentences implements Iterable<Sentence> {
  /** Location of file on disk */
  private String[] file_location;
  /**
   * Current file being read
   */
  private int current_file = 0;
  /** Instance of the model, used for scoring/parsing */
  private final Grammar grammar;
  /** Should we save or stream the data */
  private final boolean streaming;
  /** Buffered data source */
  private BufferedReader reader;
  /** Stored data */
  private final ArrayList<Sentence> data;
  /** Has the data been read */
  public boolean all_data_has_been_read = false;
  /** Next chart to be read */
  private final AtomicInteger current_index = new AtomicInteger(0);
  private final int shortest_allowable_sentence;
  private final int longest_allowable_sentence;
  private int returned_count = 0;

  /**
   * Creates a sentence reading object
   * @param global_grammar
   *    Stores token codebooks
   * @param stream
   *    Specifies whether we should keep around sentences we've read
   * @param files
   *    Where to read the raw data from
   * @param shortest
   *    Don't return sentences shorter than this value
   * @param longest
   *    Don't return sentences longer than this value
   */
  public Sentences(Grammar global_grammar, boolean stream, int shortest, int longest, String ... files) {
    this.longest_allowable_sentence = longest;
    this.shortest_allowable_sentence = shortest;
    this.file_location = files;
    this.grammar = global_grammar;
    this.streaming = stream;
    if (this.streaming) {
      data = null;
    } else {
      data = new ArrayList<>();
    }

    openCurrentFileForReading();
  }

  /**
   * Creates a sentences object which does not stream the data (default)
   * @param global_grammar
   *    Stores token codebooks
   * @param shortest
   *    Don't return sentences shorter than this value
   * @param longest
   *    Don't return sentences longer than this value
   * @param files  Files to read
   */
  public Sentences(Grammar global_grammar, int shortest, int longest, String... files) {
    this(global_grammar, false, shortest, longest, files);
  }

  public Sentences(Grammar global_grammar, ArrayList<Sentence> sentences) {
    this.grammar = global_grammar;
    this.streaming = false;
    this.shortest_allowable_sentence = 0;
    this.longest_allowable_sentence = Integer.MAX_VALUE;
    data = sentences;
  }

  /**
   * Returns the next sentence.  Reading from disk is synchronized.
   * From memory is not.
   * @return
   *    Next sentence from file
   */
  private synchronized Sentence readSentence() {
    // Streaming and read all data
    if (this.all_data_has_been_read && this.streaming) {
      return null;
    }
    // Read all data but not streaming
    if (this.all_data_has_been_read) {
      int next_sentence = current_index.getAndIncrement();
      if (next_sentence < data.size()) {
        return data.get(next_sentence);
      }
      return null;
    }

    try {
      String read = this.reader.readLine();
      // Nothing in the buffer to read:  Close the buffer and open a new file
      if (read == null && this.current_file + 1 < this.file_location.length) {
        reader.close();
        ++this.current_file;
        openCurrentFileForReading();
        // Now read the first line of the new file
        read = this.reader.readLine();
      } else if (read == null && this.current_file + 1 >= this.file_location.length) {
        Logger.logln("\n" + returned_count + " sentences read from "
            + Arrays.toString(this.file_location));
        this.all_data_has_been_read = true;
        Logger.total = returned_count;
        return null;
      }

      // Read actual sentence
      Sentence current_sentence = new Sentence();
      if (read.charAt(0) == '{') {
        readJSONSentence(read, current_sentence, grammar);
      } else if (read.charAt(0) == 'I') {
        readAUTOSentence(read, current_sentence);
      } else {
        readCoNLLSentence(read, current_sentence);
      }

      // If no words were added to the sentence, return null
      // Otherwise, save sentence if appropriate
      if (current_sentence.length() > 0) {
        current_sentence.computeFirstAndLast();
        if (!streaming) {
          data.add(current_sentence);
          current_index.getAndIncrement();
        }
        return current_sentence;
      }
    } catch (IOException exception) {
      exception.printStackTrace();
    }
    return null;
  }

  public void loadIntoMemory() {
    while(next() != null);
  }

  private synchronized void readCoNLLSentence(String strLine, Sentence current_sentence) throws IOException {
    // While there's another file to read
    // Or we've not completed reading the current one
    do {
      if (strLine != null && strLine.trim().length() > 0) {
        current_sentence.addWord(strLine, grammar);
      } else {
        // Newline marks end of sentence
        break;
      }
    } while ((strLine = this.reader.readLine()) != null);
  }

  public static void readJSONSentence(String strLine, Sentence current_sentence, Grammar inductionGrammar) {
    current_sentence.JSON = JSONFormat.deSerialize(strLine);
    for (int i = 0; i < current_sentence.JSON.words.length; ++i) {
      current_sentence.addWord(current_sentence.JSON.words[i],
          current_sentence.JSON.FBID_for_entity(i), inductionGrammar);
    }
  }

  private synchronized void readAUTOSentence(String strLine, Sentence current_sentence) throws IOException {
    do {
      if (strLine != null && strLine.trim().length() > 0) {
        // If we're at a parse
        if (strLine.charAt(0) == '(') {
          current_sentence.copy(new Sentence(strLine, grammar));
          return;
        }
        // Otherwise, keep reading until not an ID line
      }
    } while ((strLine = this.reader.readLine()) != null);
  }

  /**
   * Returns the first sentence of length <= length parameter and discards
   * any read sentence which is too long.
   * @return
   *    A sentence shorter than or equal to length
   */
  public synchronized Sentence next() {
    // TODO(bisk1): synchronized for simplicity.  Not truly necessary
    Sentence current_sentence;
    while ((current_sentence = readSentence()) != null) {
      if (current_sentence.length_noP() <= this.longest_allowable_sentence
          && current_sentence.length_noP() >= this.shortest_allowable_sentence) {
        current_sentence.id = returned_count++;
        Logger.stat(current_sentence.id);
        return current_sentence;
      }
      // TODO(bisk1): If the sentence doesn't fit the bounds, delete it
    }
    return null;
  }

  /**
   * If we'd like to treat multiple data sources as though they were a single
   * entity, this allows for a file to be added to the list that should be read
   * @param file_name Additional source data file
   */
  public void addFile(String file_name) {
    file_location = Arrays.copyOf(file_location, file_location.length + 1);
    file_location[file_location.length - 1] = file_name;
    // Reset everything
    reset_index();
    this.all_data_has_been_read = false;
    current_file = 0;
    openCurrentFileForReading();
  }

  /**
   * Reset current document to zero when done reading the data
   */
  public void reset_index() {
    current_index.set(0);
    returned_count = 0;
    if (streaming) {
      current_file = 0;
      this.all_data_has_been_read = false;
      openCurrentFileForReading();
    }
  }

  /**
   * Returns the number of sentences that have been read or saved
   * depending on if we are streaming or saving
   * @return
   *  Max of the number of saved and read sentences
   */
  public int size() { if(streaming) {
    return current_index.get();
  } return data.size(); }

  /**
   * Refreshes the File reader with a reference to the currently indexed file
   */
  void openCurrentFileForReading() {
    reader = TextFile.Reader(this.file_location[current_file]);
  }

  @Override
  public Iterator<Sentence> iterator() {
    return data.iterator();
  }

  public void writeToDisk(String filename) throws IOException {
    Writer writer = TextFile.Writer(filename);
    for (Sentence sentence : this) {
      JSONFormat.createFromSentence(sentence, grammar);
      writer.write(sentence.JSON.toString() + "\n");
    }
    writer.close();
  }
}
