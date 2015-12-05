package CCGInduction.parser;

import CCGInduction.data.Sentence;
import CCGInduction.grammar.Grammar;
import CCGInduction.models.Model;

/**
 * @author bisk1
 * @param <G> Grammar
 *
 */
public abstract class CoarseToFineChart<G extends Grammar> extends Chart<G> {
  /**
   * Temporary data-structure when performing coarse-to-fine
   */
  public Cell<G>[][] fine;

  /**
   * A Chart which will be used with fine chart items
   * @param base_sentence Sentence
   * @param mod Model
   */
  CoarseToFineChart(Sentence base_sentence, Model<G> mod) {
    super(base_sentence, mod);
  }

  /**
   * Default constructor
   */
  public CoarseToFineChart() {}

  /**
   * Empties unsuccessful charts. Performs coarse-to-fine or context building
   * for successul parses while removing unused constituents
   * 
   */
  @Override
  public void cleanForest(boolean Test) {
    if (success()) {
      if (model.createFine) {
        createFine();
      }
      if (!Test) {
        markUnused(TOP);
        parentCount(TOP);
        TOP.outside_parses = 1;
        outsideParses(TOP);
      }
      trim(TOP);
      if (model.createFine) {
        killCoarse();
        parentCount(TOP);
      }
      cleanChart();
    } else {
      chart = null;
    }
  }


  public void outsideParses(ChartItem<G> p) {
    p.seenParents = 0;
    for (BackPointer<G> bp : p.children) {
      if (bp.isUnary()) {
        ChartItem<G> H = bp.leftChild();
        H.outside_parses += p.outside_parses;

        H.seenParents += 1;
        if (H.seenParents == H.parents && !H.children.isEmpty()) {
          outsideParses(H);
        }
      } else {
        ChartItem<G> H = bp.leftChild;
        ChartItem<G> S = bp.rightChild;

        H.seenParents += 1;
        S.seenParents += 1;

        H.outside_parses += p.outside_parses * S.parses;
        S.outside_parses += p.outside_parses * H.parses;

        if (H.seenParents == H.parents && !H.children.isEmpty()) {
          outsideParses(H);
        }
        if (S.seenParents == S.parents && !S.children.isEmpty()) {
          outsideParses(S);
        }
      }
    }
    p.used = false;
  }

  private void parentCount(ChartItem<G> a) {
    a.used = true;
    for (BackPointer<G> bp : a.children) {
      if (bp.isUnary()) {
        bp.leftChild.parents += 1;

        if (!bp.leftChild.used) {
          parentCount(bp.leftChild);
        }
      } else {
        bp.leftChild.parents += 1;
        bp.rightChild.parents += 1;

        if (!bp.leftChild.used) {
          parentCount(bp.leftChild);
        }
        if (!bp.rightChild.used) {
          parentCount(bp.rightChild);
        }
      }
    }
  }

  public void markUnused(ChartItem<G> P) {
    if (!P.used) {
      return;
    }
    for (BackPointer<G> bp : P.children) {
      markUnused(bp.leftChild);
      if (!bp.isUnary()) {
        markUnused(bp.rightChild);
      }
    }
    P.used = false;
  }


  /**
   * Delete the coarse chart and replace it with the fine chart
   */
  void killCoarse() {
    if (TOP.cell.Fine) {
      chart = null;
      chart = fine;
      fine = null;
    } else {
      // START was never reassigned
      chart = null;
      fine = null;
      TOP = null;
    }

  }

  /**
   * Creates a new Cell[][] for fine chart items
   */
  @SuppressWarnings("unchecked")
  void createFine() {
    fine = new Cell[chart.length][chart.length];
    for (int s = 0; s < chart.length; s++) {
      for (int i = 0; i < chart.length - s; i++) {
        fine[i][i + s] = new Cell<>(this, i, i + s);
        fine[i][i + s].Fine = true;
      }
    }
  }
}
