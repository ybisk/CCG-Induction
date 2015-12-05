package CCGInduction.grammar;

import CCGInduction.models.ArgumentBackPointer;
import CCGInduction.parser.BackPointer;
import CCGInduction.parser.ChartItem;
import CCGInduction.utils.Math.Log;
import CCGInduction.ccg.InducedCAT;
import CCGInduction.data.LexicalToken;
import CCGInduction.data.POS;
import CCGInduction.data.Tagset;
import CCGInduction.models.HDPArgumentModel;
import CCGInduction.models.Model;
import CCGInduction.parser.InductionChart;
import CCGInduction.parser.SerializableCharts;
import CCGInduction.utils.Mapper;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class implements Map for redefining the underlying pos-tags used by the charts
 * Created by bisk1 on 2/2/15.
 */
public class TagMapper extends Mapper<Grammar,InductionChart<Grammar>> {
  private final ConcurrentHashMap<Long,POS> clusterMapping; // FIXME:  Map Long --> Long

  public TagMapper(Model<Grammar> model, SerializableCharts serializableCharts,
                   ArrayList<Exception> exceptions,
                   ConcurrentHashMap<Long, POS> wordClusterMapping) {
    super(model, serializableCharts, exceptions);
    clusterMapping = new ConcurrentHashMap<>(wordClusterMapping);  // make a local copy
    localModel.Distributions.forEach(localModel.priorCounts::addDist);
  }

  @Override
  protected void map(InductionChart<Grammar> chart) throws Exception {
    localModel.inside(chart);
    localModel.outside(chart);
    chart.priorCounts = localModel.priorCounts;


    long[] new_words = new long[chart.sentence.length()];
    long[] new_tags  = new long[chart.sentence.length()];
    int cur = 0;
    for (LexicalToken LT : chart.sentence) {
      new_tags[cur] = localModel.grammar.Lex(clusterMapping.get(LT.rawWord()).toString());
      if (localModel.grammar.learnedWords.containsKey(LT.word()))
        new_words[cur] = chart.words[cur];
      else
        new_words[cur] = localModel.grammar.Lex("UNK:" + clusterMapping.get(LT.rawWord()));
      ++cur;
    }

    // Get new counts
    newTagCounts(chart.TOP, chart.likelihood, new_tags, new_words);
  }

  void newTagCounts(ChartItem<Grammar> parent, double likelihood, long[] new_tags, long[] new_words) {
    if (parent.computedCounts)
      return;
    parent.computedCounts = true;
    for (BackPointer<Grammar> bp : parent.children) {
      if (bp.rule.Type.equals(Rule_Type.PRODUCTION)) {
        // Compute the count with previous model settings
        double update = Log.div(Log.mul(parent.beta(), bp.leftChild.alpha(),
            localModel.prob(parent, bp)), likelihood);

        long old_tag = parent.cell.chart.tags[parent.X];
        long old_word = parent.cell.chart.words[parent.X];   // Need for unk
        // Don't change the punctuation
        if (!Tagset.Punct(parent.cell.chart.sentence.get(parent.X).tag())) {
          // Update the tag array
          POS newCluster = clusterMapping.get(parent.cell.chart.sentence.get(parent.X).rawWord());
          parent.cell.chart.tags[parent.X] = new_tags[parent.X];
          parent.cell.chart.words[parent.X] = new_words[parent.X];
          // Update the grammar
          InducedCAT parentCat = localModel.grammar.Categories.get(parent.Category);
          localModel.grammar.newLexCats.get(newCluster).put(parentCat, true);
        }

        ((HDPArgumentModel)localModel).p_emit(parent, (ArgumentBackPointer)bp, update);
        // Put back for next category
        parent.cell.chart.tags[parent.X] = old_tag;
        parent.cell.chart.words[parent.X] = old_word;
      } else {
        newTagCounts(bp.leftChild(), likelihood, new_tags, new_words);
        if (bp.rightChild != null)
          newTagCounts(bp.rightChild(), likelihood, new_tags, new_words);
      }
    }
  }
}
