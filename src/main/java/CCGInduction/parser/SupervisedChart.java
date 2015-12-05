package CCGInduction.parser;

import CCGInduction.data.Sentence;
import CCGInduction.grammar.Grammar;
import CCGInduction.ccg.InducedCAT;
import CCGInduction.grammar.Rule;
import CCGInduction.grammar.Rule_Type;
import CCGInduction.grammar.Unary;
import CCGInduction.models.Model;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by bisk1 on 10/15/14.
 * A data-structure for reading supervised parses from CCGbank AUTO files
 */
class SupervisedChart<G extends Grammar> extends CoarseToFineChart<G> {
  /**
   * Default constructor
   */
  public SupervisedChart() {}

  /**
   * Construct a chart for supervised parses
   * @param base_sentence Underlying sentence
   * @param shared_model  Model reference
   */
  SupervisedChart(Sentence base_sentence, Model<G> shared_model) {
    super(base_sentence, shared_model);
    chart = new Cell[sentence.length()][sentence.length()];
  }


  @Override
  void getLex(int i, boolean test) {
    Cell<G> cell = chart[i][i];

    Grammar iG = model.grammar;
    //POS t = lt.tag();
    //long lex_cat = iG.CAT(iG.Lex(t.toString()), Rule_Type.LEX);
    long lex_cat = cell.chart.words[i];  // Already has UNK and wrapped in CAT( , LEX)
    ChartItem<G> ci;

    // If there is a category already assigned to this word, get the correct production rule
    // Otherwise, get all possible productions
    Set<Rule> rules;
    if (sentence.get(i).cat() != null) {
      rules = new HashSet<>();
      for (long category : sentence.get(i).cat())
        rules.add(iG.getRule(category, lex_cat, Rule_Type.PRODUCTION, true));
    } else {
      rules = iG.getRules(lex_cat);
    }
    for (Rule rule : rules) {
      InducedCAT ic = iG.Categories.get(rule.A);
      ci = Grammar.LexChartItem((Unary) rule, cell);
      if (!test)
        ci.iCAT = ic;
      cell.addCat(ci);
      if (ci.iCAT == null && !test) {
        System.err.println(sentence.get(i).word(iG));
        throw new Parser.FailedParsingAssertion("no iCAT");
      }
    }
  }

}
