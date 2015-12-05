package CCGInduction.experiments;

import CCGInduction.Configuration;
import CCGInduction.ccg.InducedCAT;
import CCGInduction.parser.SupervisedCharts;

/**
 * A supervised parser
 */
public class SupervisedParser extends UnsupervisedInduction {
  public SupervisedParser(Configuration configuration) {
    super(configuration);
  }

  public void perform(Action action) throws Exception  {
    switch (action){
      case Supervised:
        // Defined as punctuation for the purposes of supervised parsing
        InducedCAT.punc = InducedCAT.add(InducedCAT.punc, InducedCAT.COLON);
        InducedCAT.punc = InducedCAT.add(InducedCAT.punc, InducedCAT.SEMICOLON);
        InducedCAT.punc = InducedCAT.add(InducedCAT.punc, InducedCAT.COMMA);
        InducedCAT.punc = InducedCAT.add(InducedCAT.punc, InducedCAT.PERIOD);
        InducedCAT.punc = InducedCAT.add(InducedCAT.punc, InducedCAT.RRB);
        InducedCAT.punc = InducedCAT.add(InducedCAT.punc, InducedCAT.LRB);
        InducedCAT.conj_atoms = InducedCAT.add(InducedCAT.conj_atoms, InducedCAT.COLON);
        InducedCAT.conj_atoms = InducedCAT.add(InducedCAT.conj_atoms, InducedCAT.SEMICOLON);
        InducedCAT.conj_atoms = InducedCAT.add(InducedCAT.conj_atoms, InducedCAT.COMMA);
        InducedCAT.conj_atoms = InducedCAT.add(InducedCAT.conj_atoms, InducedCAT.PERIOD);
        InducedCAT.conj_atoms = InducedCAT.add(InducedCAT.conj_atoms, InducedCAT.LRB);
        // Space to accumulate counts
        model.Distributions.forEach(model.priorCounts::addDist);

        charts = new SupervisedCharts(model, training_sentences);
        parseAndSerialize(charts, action);
        break;
      case Test:
        Test("Test", Action.SupervisedTest);
        break;
      default:
        super.perform(action);
    }
  }
}
