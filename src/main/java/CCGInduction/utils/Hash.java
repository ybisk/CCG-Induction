package CCGInduction.utils;

import java.io.UnsupportedEncodingException;

/**
 * Computes 64 bit hashes of strings
 * Created by bisk1 on 2/6/15.
 */
public class Hash {

  private static final long[] byteTable = createLookupTable();
  private static final long HSTART = 0xBB40E64DA205B064L;
  private static final long HMULT = 7664345821815920749L;
  /**
   * A 64 bit String hash
   *
   * @param stringToHash String for which to compute a hash
   * @return string's hash value
   */
  public static long hash(String stringToHash) {
    byte[] data;
    try {
      data = stringToHash.getBytes("UTF-8");
      long h = HSTART;
      final long hmult = HMULT;
      final long[] ht = byteTable;
      for (int len = data.length, i = 0; i < len; i++) {
        h = (h * hmult) ^ ht[data[i] & 0xff];
      }
      return h;
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
      throw new AssertionError("Hash failures on encoding");
    }
  }

  private static long[] createLookupTable() {
    long[] Table = new long[256];
    long h = 0x544B2FBACAAF1684L;
    for (int i = 0; i < 256; i++) {
      for (int j = 0; j < 31; j++) {
        h = (h >>> 7) ^ h;
        h = (h << 11) ^ h;
        h = (h >>> 10) ^ h;
      }
      Table[i] = h;
    }
    return Table;
  }

}
