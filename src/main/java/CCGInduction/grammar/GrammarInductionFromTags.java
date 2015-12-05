package CCGInduction.grammar;

import CCGInduction.Configuration;
import CCGInduction.parser.InductionChart;
import CCGInduction.parser.InductionCharts;
import CCGInduction.ccg.Direction;
import CCGInduction.ccg.InducedCAT;
import CCGInduction.data.POS;
import CCGInduction.data.Sentence;
import CCGInduction.data.Tagset;
import CCGInduction.models.Model;
import CCGInduction.utils.IntPair;
import CCGInduction.utils.Mapper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static CCGInduction.grammar.GrammarInductionUtils.*;

/**
 * Class for inducing categories from a local context
 * @author bisk1
 */
public class GrammarInductionFromTags extends Mapper<Grammar, InductionChart<Grammar>> {

  private final Grammar grammar;
  private final Set<POS> tagsToInduce;
  /**
   * Iterates through sentences and induces new categories from local contexts
   * @param shared_charts
   *  Charts over which we induce.  Here charts are assumed to have only a
   *  sentence.
   * @param global_grammar
   *  Shared grammar object for maintaining categories
   */
  public GrammarInductionFromTags(Model<Grammar> model, InductionCharts<Grammar> shared_charts,
                                  Grammar global_grammar, List<POS> tags) {
    super(model, shared_charts, new ArrayList<>());
    this.grammar = global_grammar;
    tagsToInduce = new HashSet<>(tags);
  }

  @Override
  public synchronized void setup() {
    if (setup.get())
      return;
    for (POS tag : Tagset.tags()) {
      if(!Tagset.Punct(tag)) {
        ConcurrentHashMap<InducedCAT, Boolean> cats = grammar.LexCats.get(tag);
        cats.clear();
        grammar.newLexCats.get(tag).clear();
        // Collect what survived
        IntPair ip = new IntPair(grammar.Lex(tag.toString()));
        for (Rule r : grammar.getRules(ip)) {
          Unary u = (Unary) r;
          if (!Configuration.induceValidOnly || grammar.firstPrepare // firstPrepare means we haven't parsed yet
              || grammar.unaryCheck.get(new IntPair(u.A, u.B)).equals(valid.Valid)
              || u.A == grammar.S_NT) {
            cats.put(grammar.Categories.get(u.A), true);
          }
        }
      }
    }
  }

  @Override
  public void map(InductionChart chart) throws Exception {
    // Build all contexts. These are triples in the case of the intermediate
    // COLON
    try {
      Sentence sent = chart.sentence;
      for (int i = 0; i < sent.length_noP() - 1; i++) {
        POS curT = sent.getNP(i).tag();
        POS nextT = sent.getNP(i + 1).tag();
        if (!Tagset.CONJ(nextT) && (!Tagset.CONJ(curT) || i == 0)) {
          if (tagsToInduce.contains(curT)) {
            if (!grammar.LexCats.containsKey(nextT))
              System.err.println(nextT);
            grammar.LexCats.get(nextT).keySet().stream().filter(R -> !useCFG(R)).forEach(R -> induceRight(curT, R));
          }
          if (tagsToInduce.contains(nextT)) {
            grammar.LexCats.get(curT).keySet().stream().filter(L -> !useCFG(L) || Tagset.CONJ(nextT)).forEach(L -> induceLeft(L, nextT));
          }
        }
      }
    } catch (Exception e) {
      System.err.println("Failed to Induce " + chart.sentence.asTags());
      throw e;
    }
  }

  private void induceLeft(InducedCAT L, POS R) {
    if (!Tagset.Punct(R)) {
      ConcurrentHashMap<InducedCAT, Boolean> newC_j = grammar.newLexCats.get(R);
      ConcurrentHashMap<InducedCAT, Boolean> C_j = grammar.LexCats.get(R);

      if (C_j.isEmpty()) {
        Left_Modify(newC_j, L); // return new modifier X\X
      } else if (!useCFG(L)) {
        // If j isn't a conjuction or punctuation
        // X\Y c_j\L
        // X\X
        C_j.keySet().stream().filter(c_j -> !useCFG(c_j) || Tagset.CONJ(R)).forEach(
          c_j -> {
            if (!Tagset.CONJ(R)) {
              Left_Arg(newC_j, L, c_j); // X\Y c_j\L
            }
            Left_Modify(newC_j, L); // X\X
        });
      }
    }
  }

  private void induceRight(POS L, InducedCAT R) {
    if (!Tagset.Punct(L)) {
      ConcurrentHashMap<InducedCAT, Boolean> newC_i = grammar.newLexCats.get(L);
      ConcurrentHashMap<InducedCAT, Boolean> C_i = grammar.LexCats.get(L);
      // We are new but R isn't, lets modify him
      if (C_i.isEmpty()) {
        Right_Modify(newC_i, R);
        // We've all been around the block
      } else if (!useCFG(R)) {
        // If i isn't a conjunction or punctuation
        C_i.keySet().stream().filter(c_i -> !useCFG(c_i) || Tagset.CONJ(L)).forEach(
          c_i -> {
            if (!Tagset.CONJ(L)) {
              Right_Arg(newC_i, c_i, R);
            }
            Right_Modify(newC_i, R);
        });
      }
    }
  }



  private static boolean useCFG(InducedCAT cat) {
    return cat.D.equals(Direction.None) && (InducedCAT.CONJ(cat) || InducedCAT.PUNC(cat.atom));
  }


}
