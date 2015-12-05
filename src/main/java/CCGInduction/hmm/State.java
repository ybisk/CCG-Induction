package CCGInduction.hmm;

import CCGInduction.utils.Math.LogDouble;

/**
 * State in a HMM Trellis that contains forward and backward probability
 * Created by bisk1 on 2/13/15.
 */
public class State {
  final LogDouble alpha = new LogDouble();
  final LogDouble beta = new LogDouble();

  public void clear() {
    alpha.clear();
    beta.clear();
  }
}
