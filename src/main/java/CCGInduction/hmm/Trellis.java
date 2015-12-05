package CCGInduction.hmm;

import CCGInduction.data.Sentence;
import CCGInduction.utils.Math.Log;
import CCGInduction.utils.Math.LogDouble;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A trellis for an HMM
 * Created by bisk1 on 2/13/15.
 */
public class Trellis implements Serializable{
  final long[] words;
  final State[][] states;
  final LogDouble finalState = new LogDouble(Log.ZERO);
  final LogDouble startState = new LogDouble(Log.ZERO);

  Trellis(Sentence sentence, int K, ConcurrentHashMap<Long,Boolean> knownWords) {
    words = new long[sentence.length()];
    states = new State[sentence.length()][];
    for (int i = 0; i < words.length; ++i){
      words[i] = sentence.get(i).wordOrUnk(knownWords);
      states[i] = new State[K];
      for (int j = 0; j < K; ++j)
        states[i][j] = new State();
    }
  }

  int length() { return words.length; }

  double alpha(int word, int cluster) {
    return states[word][cluster].alpha.value();
  }

  void alpha(int word, int cluster, double val) {
    states[word][cluster].alpha.add(val);
  }

  double beta(int word, int cluster) {
    return states[word][cluster].beta.value();
  }

  void beta(int word, int cluster, double val) {
    states[word][cluster].beta.add(val);
  }

  public void clear() {
    for (State[] col : states) {
      for (State state : col) {
        state.clear();
      }
    }
    finalState.clear();
    startState.clear();
  }

  public void setAlpha(int w, int current, double val) {
    states[w][current].alpha.set(val);
  }
}
