package CCGInduction.utils;

import junit.framework.TestCase;

import java.io.BufferedReader;
import java.util.HashMap;

public class HashTest extends TestCase {

  public void testHash() throws Exception {
    // Checks for hash conflicts
    BufferedReader reader = TextFile.Reader("src/test/java/edu/illinois/cs/nlp/hdpccg/utils/vocab.txt.gz");
    HashMap<Long,String> strings = new HashMap<>();
    String line;
    while ((line = reader.readLine()) != null) {
      long hash = Hash.hash(line.trim().toLowerCase());
      assertFalse("Conflict: " + hash + '\t' + strings.get(hash) + '\t' + line, strings.containsKey(hash) && !strings.get(hash).equals(line.trim().toLowerCase()));
      strings.put(hash, line.trim().toLowerCase());
    }
    System.out.println("Successfully hashed: " + strings.size() + " CCGbank items");

    reader = TextFile.Reader("src/test/java/edu/illinois/cs/nlp/hdpccg/utils/BLIIP.txt.gz");
    while ((line = reader.readLine()) != null) {
      long hash = Hash.hash(line.trim().toLowerCase());
      assertFalse("Conflict: " + hash + '\t' + strings.get(hash) + '\t' + line, strings.containsKey(hash) && !strings.get(hash).equals(line.trim().toLowerCase()));
      strings.put(hash, line.trim().toLowerCase());
    }
    System.out.println("Successfully hashed: " + strings.size() + " BLIIP items");

  }
}