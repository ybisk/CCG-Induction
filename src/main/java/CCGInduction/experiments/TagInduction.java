package CCGInduction.experiments;

import CCGInduction.grammar.Grammar;
import CCGInduction.Configuration;
import CCGInduction.data.Sentences;
import CCGInduction.hmm.BigramModel;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.zip.GZIPInputStream;

/**
 * Simple experiment class for calling the HMM inducer
 * Created by bisk1 on 2/13/15.
 */
public class TagInduction extends Experiment {

  public TagInduction(Configuration configuration) throws Exception {
    super(configuration);
    grammar = new Grammar();
    training_sentences = new Sentences(grammar, Configuration.shortestSentence,
        Configuration.longestSentence, Configuration.trainFile);
    model = new BigramModel(training_sentences, grammar);
  }

  @Override
  public void perform(Action action) throws Exception {
    switch (action) {
      case LoadLexicon:
        ((BigramModel)model).initFromData();
        break;
      case InduceTags:
        ((BigramModel)model).train();
        break;
      case PrintModel:
        model.print("Model");
        break;
      case Load:
        ObjectInputStream ios = new ObjectInputStream(
          new BufferedInputStream(new GZIPInputStream(new FileInputStream(Configuration.loadModelFile))));
        model = (BigramModel) ios.readObject();
        ((BigramModel)model).sentences = training_sentences;
        break;
      case Viterbi:
        ((BigramModel)model).viterbi();
      case Save:
        model.writeToDisk();
        break;
    }
  }
}
