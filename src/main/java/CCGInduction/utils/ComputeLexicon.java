package CCGInduction.utils;

import CCGInduction.data.JSON.JSONFormat;
import CCGInduction.data.JSON.SynParObj;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Created by bisk1 on 1/31/15.
 */
public class ComputeLexicon {
  private static final ConcurrentHashMap<String, ConcurrentHashMap<String, AtomicInteger>> lexicon = new ConcurrentHashMap<>();
  private static final ConcurrentHashMap<String, AtomicInteger> totals = new ConcurrentHashMap<>();

  public static void main(String[] args) throws Exception {
    BufferedReader reader = TextFile.Reader(args[0]);
    ExecutorService executor = Executors.newFixedThreadPool(8);
    for (int i = 0; i < 8; ++i)
      executor.execute(new aggregator(reader));
    executor.shutdown();
    executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

    // Sort tag frequencies
    ArrayList<ObjectDoublePair<String>> vals = new ArrayList<>();
    vals.addAll(totals.keySet().stream().map(
        T -> new ObjectDoublePair<>(T, totals.get(T).intValue())).collect(Collectors.toList()));
    Collections.sort(vals);
    double grandTotal = vals.stream().mapToDouble(T -> T.value()).sum();
    double grandCum   = 0.0;
    boolean grandOne = false, grantTwo = false, grandThree = false;

    ArrayList<ObjectDoublePair<String>> cats = new ArrayList<>();
    for (ObjectDoublePair pair : vals) {
      System.out.println(String.format("%-20s   %-10d", pair.content(), (int) pair.value()));
      cats.clear();
      // Sort category frequencies
      cats.addAll(lexicon.get(pair.content()).keySet().stream().map(
          C -> new ObjectDoublePair<>(C, lexicon.get(pair.content()).get(C).intValue())).collect(Collectors.toList()));
      Collections.sort(cats);
      double cum = 0.0;
      boolean ninety = false, ninetynine = false, ninetyninenine = false;
      for (ObjectDoublePair catPair : cats) {
        cum += catPair.value()/pair.value();
        System.out.println(String.format("        %-20s    %-10d    %-7.5f",
            catPair.content(), (int) catPair.value(), catPair.value()/pair.value()));
        if (!ninety && cum > 0.90) {
          System.out.println("---------------------------------------");
          ninety = true;
        }
        if (!ninetynine && cum > 0.99) {
          System.out.println("---------------------------------------");
          ninetynine = true;
        }
        if (!ninetyninenine && cum > 0.999) {
          System.out.println("---------------------------------------");
          ninetyninenine = true;
        }
      }
      grandCum += pair.value()/grandTotal;
      System.out.println();
      if (grandCum > 0.25 && !grandThree) {
        System.out.println("##########  ########## 25% ########## ##########\n");
        grandThree = true;
      }
      if (grandCum > 0.5 && !grandOne) {
        System.out.println("##########  ########## 50% ########## ##########\n");
        grandOne = true;
      }
      if (grandCum > 0.75 && !grantTwo) {
        System.out.println("##########  ########## 75% ########## ##########\n");
        grantTwo = true;
      }
    }
  }

  private static void computeLexicon(String[] tags, String[] cats) {
    for( int i = 0; i < cats.length; ++i) {
      if (!cats[i].equals("BLANK")) {
        if (!lexicon.containsKey(tags[i])) {
          lexicon.putIfAbsent(tags[i], new ConcurrentHashMap<>());
          totals.putIfAbsent(tags[i], new AtomicInteger(0));
        }
        if (!lexicon.get(tags[i]).containsKey(cats[i]))
          lexicon.get(tags[i]).putIfAbsent(cats[i], new AtomicInteger(0));
        lexicon.get(tags[i]).get(cats[i]).getAndIncrement();
        totals.get(tags[i]).getAndIncrement();
      }
    }
  }
  private static void computeLexicon(String[] parse, double score) {
    String[] chunk;
    String tag, cat;
    for (int i = 1; i < parse.length; ++i) {
      chunk = Logger.whitespace_pattern.split(parse[i]);
      tag = chunk[2] + "_" + chunk[4];
      //tag = chunk[1];
      //cat = chunk[4];
      //tag = chunk[4];
      cat = chunk[1];

      if (!lexicon.containsKey(tag)) {
        lexicon.putIfAbsent(tag, new ConcurrentHashMap<>());
        totals.putIfAbsent(tag, new AtomicInteger(0));
      }
      if (!lexicon.get(tag).containsKey(cat))
        lexicon.get(tag).putIfAbsent(cat, new AtomicInteger(0));
      lexicon.get(tag).get(cat).addAndGet((int)score);
      totals.get(tag).addAndGet((int)score);
    }
  }

  static class aggregator implements Runnable {
    final BufferedReader reader;

    aggregator(BufferedReader reader) {
      this.reader = reader;
    }

    @Override
    public void run() {
      String line;
      JSONFormat json;
      try {
        while ((line = reader.readLine()) != null) {

          if (line.contains("Valid Gold")) {
            try {
              json = JSONFormat.deSerialize(line.split("Valid Gold Parses:")[1]);
              // Skip parse failures
              if (json.goldSynPars != null) {
                //ArrayList<String[]> parses = new ArrayList<>();
                for (SynParObj synParObj : json.goldSynPars) {
                  computeLexicon(synParObj.synPar.split("\\(<L"), synParObj.score);
                  //parses.add(synParObj.synPar.split("\\(<L"));
                }
                //String[] res = intersect(parses);
                //String[] tags = getTags(parses.get(0));
                //computeLexicon(tags,res);
              }
            } catch (Exception e) {

            }
          } else {
            json = JSONFormat.deSerialize(line);

            // Skip parse failures
            if (json.synPars != null) {
              int parses = 0;
              for (SynParObj synParObj : json.synPars) {
                if (parses >= 25)
                  continue;
                computeLexicon(synParObj.synPar.split("\\(<L"), 1);
                parses += 1;
              }
            }
          }
        }
      } catch (IOException exception) {
        exception.printStackTrace();
      }
    }
    String[] getTags(String[] parse) {
      String[] tags = new String[parse.length-1];
      for (int i = 1; i < parse.length; ++i)
        tags[i-1] = Logger.whitespace_pattern.split(parse[i])[4];
      return tags;
    }

    String[] intersect(ArrayList<String[]> parses) {
      String[] cats = new String[parses.get(0).length - 1];
      for (String[] parse : parses) {
        for (int i = 1; i < parse.length; ++i)
          cats[i-1] = merge(parse[i],cats[i-1]);
      }
      return cats;
    }

    String merge(String newCat, String current) {
      newCat = Logger.whitespace_pattern.split(newCat)[1];
      if (current == null)
        return newCat;
      if (newCat.equals(current))
        return current;
      return "BLANK";
    }
  }
}
