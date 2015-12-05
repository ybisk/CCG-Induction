package CCGInduction.parser;

import CCGInduction.Configuration;
import CCGInduction.grammar.Grammar;
import CCGInduction.ccg.InducedCAT;
import CCGInduction.data.LexicalToken;
import CCGInduction.data.POS;
import CCGInduction.data.Sentence;
import CCGInduction.data.Tagset;
import CCGInduction.grammar.Rule;
import CCGInduction.grammar.Rule_Type;
import CCGInduction.grammar.Unary;
import CCGInduction.models.Model;

import java.util.HashSet;
import java.util.Set;

/**
 * @author bisk1
 *
 */
public class InductionChart<G extends Grammar> extends CoarseToFineChart<G> {

  private static final long serialVersionUID = -7220738053694083394L;

  /**
   * Construct a Chart for use with Induction
   * @param base_sentence Underlying sentence
   * @param shared_model Model reference
   */
  public InductionChart(Sentence base_sentence, Model<G> shared_model) {
    super(base_sentence, shared_model);
  }


  /**
   * Default constructor
   */
  public InductionChart() {}

  @Override
  void getLex(int i, boolean test) {
    Cell<G> cell = chart[i][i];
    LexicalToken lt = sentence.get(i);
    // lexCat = InducedCAT(tag) w/ Rule_Type.LEX ( can look at
    // Grammar.lexCats )
    // Cats = { X -> tag | tag } with Rule_Type.PRODUCTION ( These exist (
    // created in induce ) )

    Grammar iG = model.grammar;
    POS t = lt.tag();
    long lex_cat = iG.Lex(t.toString());
    ChartItem<G> ci;

    // If there is a category already assigned to this word, get the correct production rule
    // Otherwise, get all possible productions
    Set<Rule> rules;
    if (sentence.get(i).cat() != null && sentence.get(i).cat().length != 0) {
      rules = new HashSet<>();
      for (long category : sentence.get(i).cat())
        rules.add(iG.getRule(category, lex_cat, Rule_Type.PRODUCTION, true));
    } else {
      rules = iG.getRules(lex_cat);
    }
    for (Rule rule : rules) {
      InducedCAT ic = iG.Categories.get(rule.A);
      // Also hard constrain at the lexical level
      if (!Configuration.hardEntityNConstraints || !fullEntity(i,i) || InducedCAT.N(ic)) {
        if ((rule.A != iG.P_NT)
            // Tag is conj --> CC || first index
            && (!Tagset.CONJ(t) || Tagset.Punct(t) || InducedCAT.CONJ(ic)
                || cell.X == cell.chart.sentence.firstWord
                || cell.X == cell.chart.sentence.lastWord)
            ) {
          switch (iG.unaryCheck(rule.A, rule.B)) {
          case Unused:
            if (test) {
              break;
            }
          case Valid:
            ci = Grammar.LexChartItem((Unary) rule, cell);
            if (!test)
              ci.iCAT = ic;
            cell.addCat(ci);
            if (ci.iCAT == null && !test) {
              System.out.println(sentence.get(i).word(iG));
              throw new Parser.FailedParsingAssertion("no iCAT");
            }
            break;
          case Unknown:
            if (!test) {
              ci = Grammar.LexChartItem((Unary)rule, cell);
              ci.iCAT = ic;
              cell.addCat(ci);
              if (ci.iCAT == null) {
                throw new Parser.FailedParsingAssertion("no iCAT");
              }
            }
            break;
          case Invalid:
          default:
            // Do nothing
          }
        }
      }
    }
  }
}
