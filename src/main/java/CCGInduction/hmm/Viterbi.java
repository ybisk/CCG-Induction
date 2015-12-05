package CCGInduction.hmm;

import CCGInduction.data.POS;
import CCGInduction.data.Sentence;
import CCGInduction.data.Sentences;
import CCGInduction.utils.Math.Log;

import java.util.Arrays;

/**
 * A Runnable object which fills the induced cluster with the viterbi path through the HMM
 * Created by bisk1 on 2/13/15.
 */
public class Viterbi implements Runnable {
  final Sentences workQueue;
  final BigramModel localModel;
  final int K;

  Viterbi(Sentences queue, BigramModel model, int k) {
    workQueue = queue;
    localModel = model.copy();
    K = k;
  }

  @Override
  public void run() {
    Trellis current;
    Sentence sentence;
    while((sentence = workQueue.next()) != null){
      current = new Trellis(sentence, K, localModel.grammar.learnedWords);
      current.clear();
      int[] path = viterbi(current);
      for (int i = 0; i < path.length; ++i) {
        sentence.get(i).induced(new POS(String.valueOf(path[i])));
        if (sentence.JSON != null)
          sentence.JSON.words[i].cluster = String.valueOf(path[i]);
      }
    }
  }

  private int[] viterbi(Trellis trellis) {
    int[][] paths = new int[trellis.length()][K];
    for (int[] path : paths)
      Arrays.fill(path, -1);

    // Start
    for (int current = 0; current < K; ++current) {
      try {
        trellis.alpha(0, current,
            Log.mul(localModel.p_Transition(-1, current), localModel.p_Emit(trellis.words[0], current)));
        paths[0][current] = current;
      } catch (NullPointerException npe) {
        System.err.println(localModel.grammar.Words.get(trellis.words[0]));
        throw npe;
      }
    }

    // Walk Middle
    double val;
    for (int w = 1; w < trellis.length(); ++w) {
      for (int previous = 0; previous < K; ++previous) {
        for (int current = 0; current < K; ++current) {
          // choose best person to transition from
          val = Log.mul(
              trellis.alpha(w - 1, previous),
              localModel.p_Transition(previous, current),
              localModel.p_Emit(trellis.words[w], current));
          if (val > trellis.alpha(w,current)) {
            trellis.setAlpha(w, current, val);
            paths[w][current] = previous;
          }
        }
      }
    }

    // End
    int finalState = -1;
    for (int current = 0; current < K; ++current) {
      val = Log.mul(trellis.alpha(trellis.length() - 1, current), localModel.p_Transition(current, -1));
      if (val > trellis.finalState.value()) {
        trellis.finalState.set(val);
        finalState = current;
      }
    }

    // Find best path (backtracking)
    int[] path = new int[trellis.length()];
    path[path.length-1] = finalState;
    for (int w = trellis.length() - 2; w >= 0; w--) {
      path[w] = paths[w+1][path[w+1]];
    }
    return path;
  }

}
