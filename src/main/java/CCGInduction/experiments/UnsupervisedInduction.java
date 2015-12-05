package CCGInduction.experiments;

import CCGInduction.data.*;
import CCGInduction.grammar.*;
import CCGInduction.models.*;
import CCGInduction.parser.CoarseToFineChart;
import CCGInduction.Configuration;
import CCGInduction.ccg.InducedCAT;
import CCGInduction.learning.CondOutcomePair;
import CCGInduction.parser.InductionCharts;
import CCGInduction.parser.InductionParser;
import CCGInduction.parser.SerializableCharts;
import CCGInduction.utils.IntPair;
import CCGInduction.utils.Logger;
import CCGInduction.utils.TextFile;

import java.io.BufferedReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unsupervised Induction Experiment. Allows for grammar induction and learning
 *
 * @author bisk1
 */
public class UnsupervisedInduction extends Experiment<Grammar> {
  SerializableCharts<Grammar,CoarseToFineChart<Grammar>> charts;
  private int induceFromTrees = 0;
  private static double threshold = Configuration.threshold;
  private Action lastParseAction = null;
  private final HashSet<POS> readCatsForTag = new HashSet<>();


  /**
   * Instantiates the experiment
   *
   * @param configuration Configuration reference
   */
  public UnsupervisedInduction(Configuration configuration) {
    super(configuration);
    grammar = new Grammar();
    InducedCAT.createAtomics();
  }

  @Override
  public Grammar grammar() {
    return grammar;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void perform(Action action) throws Exception {
    switch (action) {
      case MergeTrainTest:
        // adds test files to the training data
        String[] merged = Arrays.copyOf(Configuration.trainFile,
            Configuration.trainFile.length + Configuration.testFile.length);
        System.arraycopy(Configuration.testFile, 0, merged, Configuration.trainFile.length, Configuration.testFile.length);
        Configuration.trainFile = merged;
        break;
      case LoadWeightedLexicon:
        model.priorCounts.priorCounts();
        model.init();
        readWeightedLexicon(Configuration.savedLexicon);
        break;
      case LoadLexicon:
        readLexicon(Configuration.savedLexicon, training_sentences);
        model.printLexicon();
        break;
      case I:
        // Induce new categories/rules
        // Create/Update the model
        if (readCatsForTag.isEmpty())
          induceFromPOS(charts, Arrays.asList(Tagset.tags));
        else {
          List<POS> toInduce = new ArrayList<>();
          for (POS tag : Tagset.tags) {
            // If you're not punctuation but have not been read by the lexicon
            if (!Tagset.Punct(tag) && !readCatsForTag.contains(tag))
              toInduce.add(tag);
          }
          if (toInduce.isEmpty())
            return;
          System.out.println(Arrays.toString(toInduce.toArray()));
          induceFromPOS(charts, toInduce);
        }
        grammar.incorporateNewRules();
        grammar.print("InducedGrammar.gz");
        model.printLexicon();
        break;
      case ITr:
        // When we parse, remember to induceFromTrees
        induceFromTrees += 1;
        Configuration.complexArgs = true;
        break;
      case lexicalize:
        ((ArgumentModel) model).lexicalize();
        threshold = Configuration.threshold;
        // charts.rebuildContexts();
        if (lastParseAction == null) {
          ArgumentModel.lexicalTransition = false;
        }
        break;
      case RemapTags:
        // Road the previous Model
        model.fixedGrammar = false;
        // Grammar is now very messy, use incredibly forgiving threshold to clean up

        // Load new Word --> Cluster mapping
        ConcurrentHashMap<Long,POS> wordClusterMapping = new ConcurrentHashMap<>();
        List<String> BMMM_MapFile = TextFile.Read(Configuration.BMMMClusters);
        String[] split;
        POS tag;
        for (String line : BMMM_MapFile) {
          split = Logger.whitespace_pattern.split(line);
          tag = new POS(split[1]);
          wordClusterMapping.put(grammar.Lex(split[0]), tag);
          Tagset.tags = Tagset.add(Tagset.tags, tag);
        }

        // Assume the data was parsed
        // Remap the tags, and create new parsing rules with new stick weights
        RemapTags(wordClusterMapping);

        // Incorporate new rules and update the sentence's clusters
        charts.clear();
        grammar.incorporateNewRules();
        for (Sentence sentence : training_sentences) {
          for (LexicalToken lt : sentence) {
            if (!Tagset.Punct(lt.tag())) {
              lt.tag(wordClusterMapping.get(lt.rawWord()));
              lt.word(lt.wordOrTag(grammar.learnedWords, grammar));
            }
          }
        }

        // Re-parse with the new grammar, and create new sticks for new constructions
        perform(lastParseAction);
        model.fixedGrammar = false;
        ((HDPArgumentModel) model).newSticks();

        // De-lexicalize the model
        ((ArgumentModel) model).delexicalize();
        break;
      case PruneLexicon:
        ((HDPArgumentModel) model).disallowLowCondProbCats();
        //charts = new InductionCharts(this.model, training_sentences);
        //parseAndSerialize(charts, action);
        //Logger.logln("Parsed: " + charts.size() + "/" + training_sentences.size());
        break;
      case TrimDistributions:
        ((HDPArgumentModel) model).trimDistributions();
        break;
      case readTypeChanging:
        List<String> lines = TextFile.Read(Configuration.typeChangingRules);
        String[] lsplit;
        for (String line : lines) {
          lsplit = Logger.whitespace_pattern.split(line);
          // Ignore Comments
          if (lsplit[0] == "#")
            continue;
          InducedCAT child = InducedCAT.valueOf(lsplit[0]);
          InducedCAT parent = InducedCAT.valueOf(lsplit[1]);
          grammar.createSupervisedRule(grammar.NT(parent), grammar.NT(child), Rule_Type.TYPE_CHANGE);
        }
        break;
      case B0:
      case B1:
      case B1Mod:
      case B1ModTR:
      case B2Mod:
      case B2ModTR:
      case B2:
      case B3Mod_B2TR_B0Else:
        lastParseAction = action;
        model.Distributions.forEach(model.priorCounts::addDist);
        charts = new InductionCharts(this.model, training_sentences);
        while (induceFromTrees > 0) {
          induceFromPOS(charts, Arrays.asList(Tagset.tags));
          treeInduction(charts, action);
          grammar.incorporateNewRules();
          model.printLexicon();
          induceFromTrees -= 1;
        }
        Configuration.complexArgs = false;
        grammar.firstPrepare = false;
        // Reparse
        parseAndSerialize(charts, action);
        Logger.logln("\rParsed:",charts.size() + "/" + training_sentences.size());
        // Reset convergence threshold
        threshold = Configuration.threshold;
        grammar.print("Grammar.gz");
        break;
      case IO:
        // Check if first run
        if (threshold == Configuration.threshold && !model.initialized()) {
          Logger.logln("Initializing Model");
          model.priorCounts.priorCounts();
          model.init();
          grammar.print("Grammar.gz");
          model.print("Init");
          //Test("Init");
        }

        if (Configuration.trainK > 1 || Configuration.viterbi) {
          grammar.requiredRules.clear();
        }
        Model.InsideOutside(charts, model, threshold);
        model.update();
        threshold /= 10;
        model.print("IO");
        break;

      case ArgumentModel:
        model = new ArgumentModel(grammar);
        break;
      case HDPArgumentModel:
        model = new HDPArgumentModel(grammar);
        break;
      case PCFGModel:
        model = new PCFGModel(grammar);
        break;
      case AAAI12Model:
        model = new AAAI12Model(grammar);
        break;
      default:
        super.perform(action);
    }

    switch (action) {
      case AAAI12Model:
      case PCFGModel:
      case ArgumentModel:
      case HDPArgumentModel:
        if (Configuration.source == Training.induction)
          grammar.init();
        else
          grammar.initialized = true;
        charts = new InductionCharts(model, training_sentences);
        break;
      default:
        break;
    }
  }

  private void readWeightedLexicon(String file) {
    List<String> lines = TextFile.Read(file);
    String[] split;
    POS tag;
    InducedCAT cat;
    double count;
    ArgumentModel localModel = (ArgumentModel)model;
    for (String line : lines) {
      split = Logger.whitespace_pattern.split(line);
      cat = InducedCAT.valueOf(split[1]);
      tag = new POS(split[2]);
      count = Double.valueOf(split[0]);
      if (count > 0)
        localModel.p_Tag.accumulateCount(new CondOutcomePair(grammar.Lex(tag.toString()),
            new CondOutcomePair(grammar.NT(cat), ArgumentModel.LEX)), Math.log(count));
    }
    for (CondOutcomePair pair : localModel.p_Tag.Counts.keySet())
      localModel.p_Tag.accumulateCount(pair, Configuration.EPSILON);
    localModel.p_Tag.updateVariational();
  }

  private void readLexicon(String file, Sentences docs) throws Exception {
    // See if there's any custom punctuation
    //noinspection StatementWithEmptyBody
    while (docs.next() != null) ;
    docs.reset_index();

    // Read in all allowed lexical entries
    BufferedReader read = TextFile.Reader(file);
    String line;
    String[] split;
    HashSet<InducedCAT> LexicalCategories = new HashSet<>();
    HashMap<POS, ArrayList<InducedCAT>> CategoryTagPairs = new HashMap<>();
    while ((line = read.readLine()) != null) {
      split = Logger.whitespace_pattern.split(line.trim());
      double cond_prob = Double.valueOf(split[0]);
      POS tag = new POS(split[2]);
      if (!CategoryTagPairs.containsKey(tag)) {
        CategoryTagPairs.put(tag, new ArrayList<>());
      }
      InducedCAT cat = InducedCAT.valueOf(split[1]);
      if (cat == null)
        throw new Grammar.GrammarException("Invalid Category: " + split[1]);
      LexicalCategories.add(cat);
      if (!Tagset.Punct(tag) && !Tagset.CONJ(tag)) {
        if (cond_prob >= Configuration.CondProb_threshold) { // Make parameter
          CategoryTagPairs.get(tag).add(cat);
        }
      } else {
        CategoryTagPairs.get(tag).add(cat);
      }
    }
    grammar.createTypeRaisedCategories(LexicalCategories);

    // For tag read in, invalidate induced categories
    for (POS tag : CategoryTagPairs.keySet()) {
      // Get induced Rules
      long lex_cat = grammar.Lex(tag.toString());
      Set<Rule> rules = grammar.getRules(lex_cat);
      HashSet<Rule> readRules = new HashSet<>();
      grammar.LexCats.put(tag, new ConcurrentHashMap<>());

      // Rules from the lexicon  are Valid
      for (InducedCAT cat : CategoryTagPairs.get(tag)) {
        long hashedCategory = grammar.NT(cat);
        Unary readRule = grammar.createRule(hashedCategory, lex_cat, Rule_Type.PRODUCTION);
        grammar.unaryCheck.put(new IntPair(readRule.A, readRule.B), valid.Valid);
        grammar.LexCats.get(tag).put(cat, true);
        readCatsForTag.add(tag);
        readRules.add(readRule);
      }

      // Rules for this tag which aren't in the lexicon are invalid
      rules.stream().filter(
          inducedRule -> !readRules.contains(inducedRule)).forEach(
          inducedRule -> grammar.unaryCheck.put(new IntPair(inducedRule.A, inducedRule.B), valid.Invalid));
    }
  }

  private void induceFromPOS(SerializableCharts induceFrom, List<POS> tags) throws Exception {
    createPool();
    for (int i = 0; i < Configuration.threadCount; ++i) {
      executor.execute(new GrammarInductionFromTags(model, (InductionCharts) induceFrom, grammar, tags));
    }
    closePool();
  }

  private void treeInduction(SerializableCharts induceFrom, Action action) throws Exception {
    createPool();
    for (int i = 0; i < Configuration.threadCount; ++i) {
      executor.execute(new GrammarInductionFromTrees(model, (InductionCharts) induceFrom,
          model.grammar, new InductionParser(action), exceptions));
    }
    closePool();
  }

  private void RemapTags(ConcurrentHashMap<Long, POS> wordClusterMapping) throws Exception {
    // Invalidate all productions
    for(POS t : Tagset.tags()){
      if (!Tagset.Punct(t)) {
        IntPair tag = new IntPair(grammar.Lex(t.toString()));
        if (grammar.Rules.containsKey(tag)) {
          for (Rule r : grammar.Rules.get(tag).keySet()) {
            grammar.unaryCheck.put(new IntPair(r.A, r.B), valid.Invalid);
          }
        }
        grammar.newLexCats.put(t, new ConcurrentHashMap<>());
        grammar.LexCats.put(t, new ConcurrentHashMap<>());
      }
    }

    Configuration.uniformPrior = true; // FIXME: Hack to get counts in p_emit
    model.priorCounts.clear();
    // Get new prior counts
    createPool();
    for (int i = 0; i < Configuration.threadCount; ++i)
      executor.execute(new TagMapper(this.model, charts, exceptions, wordClusterMapping));
    closePool();

    // Update the distributions
    Logger.logln("Updating Tag distributions");
    model.priorCounts.updateChangedDistributions();
    model.print("remap");
    model.initialized = true;
  }

}
