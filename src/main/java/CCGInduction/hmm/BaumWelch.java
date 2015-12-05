package CCGInduction.hmm;

import CCGInduction.learning.Distribution;
import CCGInduction.utils.Math.Log;
import CCGInduction.learning.CountsArray;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Implementation of Baum-Welch training algorithm as runnable over trellis objects
 * Created by bisk1 on 2/13/15.
 */
public class BaumWelch implements Runnable {
  final ConcurrentLinkedQueue<Trellis> workQueue;
  final BigramModel localModel;
  final CountsArray counts = new CountsArray();
  final BigramModel globalModel;
  final int K;

  BaumWelch(ConcurrentLinkedQueue<Trellis> queue, BigramModel model, int k) {
    workQueue = queue;
    globalModel = model;
    localModel = model.copy();
    localModel.accumulatedCounts = new CountsArray();
    for (Distribution D : globalModel.Distributions) {
      counts.addDist(D);
      localModel.accumulatedCounts.addDist(D);
    }
    K = k;
  }

  @Override
  public void run() {
    Trellis current;
    while((current = workQueue.poll()) != null){
      current.clear();
      forward(current);
      backward(current);
      counts(current);
    }
    localModel.accumulateCounts(counts);
    globalModel.merge(localModel);
  }

  /**
   * Compute forward probabilities over the trellis
   */
  private void forward(Trellis trellis) {
    // Start:   -1 --> Current
    for (int current = 0; current < K; ++current) {
      trellis.alpha(0, current, Log.mul(
              localModel.p_Transition(-1, current),
              localModel.p_Emit(trellis.words[0], current))
      );
    }
    // Middle
    for (int w = 1; w < trellis.length(); ++w) {
      // For every previous state
      for (int previous = 0; previous < K; ++previous) {
        // For every current state
        for (int current = 0; current < K; ++current) {
              trellis.alpha(w, current, Log.mul(
                  trellis.alpha(w - 1, previous),             // FWD of Previous state
                  localModel.p_Transition(previous, current), // Transition probability
                  localModel.p_Emit(trellis.words[w], current)// Emit from current
              ));
        }
      }
    }
    // End:    Current --> -1
    for (int previous = 0; previous < K; ++previous) {
      trellis.finalState.add(Log.mul(
          trellis.alpha(trellis.length() - 1, previous),
          localModel.p_Transition(previous, -1)
      ));
    }

    localModel.LL.add(trellis.finalState.value());
  }

  /**
   * Compute backward probabilities over the trellis
   */
  private void backward(Trellis trellis) {
    // Stop probabilities
    for (int current = 0; current < K; ++current)
      trellis.beta(trellis.length()-1, current, localModel.p_Transition(current, -1));

    // Middle
    for (int w = trellis.length()- 2; w >= 0; --w) {
      for (int current = 0; current < K; ++current) {
        // For every next state
        for (int next = 0; next < K; ++next) {
          trellis.beta(w, current, Log.mul(
              trellis.beta(w + 1, next),                  // BWD of next state
              localModel.p_Transition(current, next),
              localModel.p_Emit(trellis.words[w + 1], next)
          ));
        }
      }
    }

    // Start
    for (int current = 0; current < K; ++current) {
      trellis.startState.add(Log.mul(
          trellis.beta(0, current),
          localModel.p_Transition(-1,current),
          localModel.p_Emit(trellis.words[0], current)));
    }
    if(!Log.equal(trellis.startState.value(),trellis.finalState.value()))
      throw new Log.MathException("Should match: " + trellis.startState.value() + "\t" + trellis.finalState.value());
  }

  /**
   * Compute pseudocounts for updating the model
   */
  private void counts(Trellis trellis) {
    double ll = trellis.finalState.value();

    // Start
    for (int next = 0; next < K; ++next) {
      if (trellis.length() > 1) {
        localModel.count_Trans(counts, -1, next, Log.div(
            Log.mul(
                //trellis.startState.value(),
                localModel.p_Transition(-1, next),
                localModel.p_Emit(trellis.words[0], next),
                trellis.beta(0, next)), ll));
      }
      //localModel.count_Emit(counts, trellis.words[0], next, Log.div(
      //    Log.mul(trellis.alpha(0, next), trellis.beta(0, next)), ll));
    }

    // Middle
    for (int w = 0; w < trellis.length()-1; ++w) {
      for (int current = 0; current < K; ++current) {
        localModel.count_Emit(counts, trellis.words[w], current, Log.div(
            Log.mul(trellis.alpha(w,current),trellis.beta(w,current)), ll));
        for (int next = 0; next < K; ++next) {
          localModel.count_Trans(counts, current, next, Log.div(
              Log.mul(
                  trellis.alpha(w, current),
                  localModel.p_Transition(current, next),
                  localModel.p_Emit(trellis.words[w+1], next),
                  trellis.beta(w+1, next)), ll));
        }
      }
    }

    // End
    for (int current = 0; current < K; ++current) {
      localModel.count_Emit(counts, trellis.words[trellis.length()-1], current, Log.div(
          Log.mul(
              trellis.alpha(trellis.length()-1, current),
              trellis.beta(trellis.length()-1, current)), ll));
      localModel.count_Trans(counts, current, -1, Log.div(
          Log.mul(
              trellis.alpha(trellis.length()-1, current),
              localModel.p_Transition(current, -1)
              //trellis.finalState.value()
          ), ll));
    }
  }
}
