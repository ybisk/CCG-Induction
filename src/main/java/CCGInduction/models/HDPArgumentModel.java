package CCGInduction.models;

import CCGInduction.Configuration;
import CCGInduction.data.Tagset;
import CCGInduction.grammar.*;
import CCGInduction.learning.*;
import CCGInduction.parser.BackPointer;
import CCGInduction.parser.ChartItem;
import CCGInduction.parser.Punctuation;
import CCGInduction.utils.Logger;
import CCGInduction.utils.Math.Log;
import CCGInduction.utils.ObjectDoublePair;
import CCGInduction.data.POS;
import CCGInduction.utils.IntPair;
import CCGInduction.utils.TextFile;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.*;
import java.util.stream.Collectors;

/**
 *  Y. Bisk and J. Hockenmaier, “An HDP Model for Inducing Combinatory Categorial Grammars,”
 *  Transactions of the Association for Computational Linguistics, pp. 75–88, 2013.
 * @author bisk1 Nov 3, 2012
 */

public class HDPArgumentModel extends ArgumentModel {
  private static final long serialVersionUID = -7999789468829182236L;
  /**
   * Default constructor
   */
  public HDPArgumentModel() {}

  /**
   * base_Args are the stick breaking weights over non-terminals
   */
  PYDistribution base_Args;
  /**
   * base_Tags are the stick breaking weights over tags
   */
  PYDistribution base_Tags;
  /**
   * base_Words are the stick breaking weights over words
   */
  PYDistribution base_Words;
  /**
   * Constant for base measure
   */
  private static long BETA;
  /**
   * Any access to conditioning array that only contains beta
   */
  static CondOutcomePair BETA_array;
  /** Punctuation marks : p( wp | L/R ) */
  PYDistribution base_Punct;
  /** p( wp | L/R, Prev ) */
  PYDistribution p_PunctPrev;
  /** Is there L/R Punct, no cond context */
  PYDistribution base_HasPunct;
  /** Is there a L/R Punct, no cond history */
  PYDistribution p_HasPunctNoHist;

  /**
   * Create an HDP Argument Model which has stick breaking weights base_Args and
   * base_Tags
   * 
   * @param grammar Grammar instance
   */
  public HDPArgumentModel(Grammar grammar) {
    super(grammar);

    double[] hyper = Configuration.alphaPower;
    double logDiscount = Configuration.logDiscount;

    // Arguments
    base_Args = new PYDistribution(this, "base_Args", hyper[0], logDiscount, true);
    base_Args.dirichletPrior = true;
    p_Arg.processParameters(hyper[0], logDiscount, true);

    // POS Tags
    base_Tags = new PYDistribution(this, "base_Tags", hyper[1], logDiscount, true);
    base_Tags.dirichletPrior = true;
    p_Tag.processParameters(hyper[1], logDiscount, true);


    BETA = this.grammar.Lex("#BETA#");
    BETA_array = new CondOutcomePair(BETA);

    // Should be in lexicalize
    base_Words = new PYDistribution(this, "base_Words", hyper[2], logDiscount, false);
    base_Words.dirichletPrior = true;
    p_Word.processParameters(hyper[2], logDiscount, false);

    // Punctuation
    this.base_Punct       = new PYDistribution(this, "base_Punct",      hyper[3], logDiscount, true);
    this.p_PunctPrev      = new PYDistribution(this, "p_PunctPrev",     hyper[3], logDiscount, true);
    this.p_Punct.processParameters(hyper[3], logDiscount, true);
    this.base_HasPunct    = new PYDistribution(this, "base_HasPunct",   hyper[3], logDiscount, true);
    this.p_HasPunctNoHist = new PYDistribution(this, "p_HasPunctNoHist",hyper[3], logDiscount, true);
    this.p_HasPunct.processParameters(hyper[3], logDiscount, true);
    setup();
  }

  HDPArgumentModel(HDPArgumentModel model) {
    super(model);
    base_Args = p_Arg.BaseDistribution == null ? model.base_Args.copy() : p_Arg.BaseDistribution;
    base_Tags = p_Tag.BaseDistribution == null ? model.base_Tags.copy() : p_Tag.BaseDistribution;
    base_Words = p_Word.BaseDistribution == null? model.base_Words.copy() : p_Word.BaseDistribution;
    p_PunctPrev = p_Punct.BaseDistribution == null? model.p_PunctPrev.copy() : p_Punct.BaseDistribution;
    base_Punct = p_PunctPrev.BaseDistribution == null? model.base_Punct.copy() : p_PunctPrev.BaseDistribution;
    p_HasPunctNoHist = p_HasPunct.BaseDistribution == null? model.p_HasPunctNoHist.copy() : p_HasPunct.BaseDistribution;
    base_HasPunct = p_HasPunctNoHist.BaseDistribution == null? model.base_HasPunct.copy() : p_HasPunctNoHist.BaseDistribution;
    setup();
  }

  private void setup() {
    Distributions.add(base_Args);
    Distributions.add(base_Tags);
    Distributions.add(base_Words);
    Distributions.add(base_Punct);
    Distributions.add(p_PunctPrev);
    Distributions.add(base_HasPunct);
    Distributions.add(p_HasPunctNoHist);
  }

  public Model<Grammar> copy() {
    return new HDPArgumentModel(this);
  }

  @Override
  public void init() {
    p_Comb.init();
    p_Type.init();

    base_Args.initSticks();
    base_Tags.initSticks();

    // if(lexicalized){
    base_Words.initSticks();
    p_Word.Init(base_Words);
    // }

    p_Tag.Init(base_Tags);
    p_Arg.Init(base_Args);

    if(!Configuration.ignorePunctuation) {
      base_Punct.initSticks();
      p_PunctPrev.Init(base_Punct);
      p_Punct.Init(p_PunctPrev);
      base_HasPunct.initSticks();
      p_HasPunctNoHist.Init(base_HasPunct);
      p_HasPunct.Init(p_HasPunctNoHist);
    }
    initialized = true;
  }

  /**
   * Adds counts and probabilities for the newly seen outcomes
   */
  public void newSticks() {
    base_Args.newSticks();
    base_Tags.newSticks();

    // if(lexicalized){
    base_Words.newSticks();
    p_Word.newSticks();
    // }

    p_Tag.newSticks();
    p_Arg.newSticks();

    if(!Configuration.ignorePunctuation) {
      base_Punct.newSticks();
      p_PunctPrev.newSticks();
      p_Punct.newSticks();
      base_HasPunct.newSticks();
      p_HasPunctNoHist.newSticks();
      p_HasPunct.newSticks();
    }
  }

  @Override
  public CondOutcomePair backoff(CondOutcomePair cxt, Distribution d) {
    if (d.identifier.equals(this.p_Word.identifier)) {
      return BETA_array;
    }
    if (d.identifier.equals(this.p_Arg.identifier) || d.identifier.equals(this.p_Tag.identifier)) {
      return BETA_array;
    }
    return PunctBackoff(cxt,d);
  }

  CondOutcomePair PunctBackoff(CondOutcomePair cxt, Distribution d) {
    if (d.identifier.equals(this.p_Punct.identifier)) {
      return new CondOutcomePair(cxt.condVariable(0), cxt.condVariable(1));
    }
    if (d.identifier.equals(this.p_PunctPrev.identifier)) {
      return new CondOutcomePair(cxt.condVariable(0));
    }
    if (d.identifier.equals(this.p_HasPunctNoHist.identifier)) {
      return BETA_array;
    }
    if (d.identifier.equals(this.p_HasPunct.identifier)) {
      return new CondOutcomePair(cxt.condVariable(0));
    }
    throw new FailedModelAssertion("Invalid distribution: " + d.identifier);
  }

  @Override
  public void count(ChartItem<Grammar> parent, BackPointer<Grammar> backPointer, double countValue,
                    CountsArray countsArray) {
    ArgumentBackPointer ca = (ArgumentBackPointer) backPointer;
    if(!Configuration.ignorePunctuation &&
        (backPointer.rule.Type == Rule_Type.FW_PUNCT || backPointer.rule.Type == Rule_Type.BW_PUNCT)) {
      punctuationCount(countsArray, countValue, parent,ca);
      return;
    }

    long rt = ca.Type();
    if (lexicalTransition && rt == LEX) {
      countsArray.add(p_Word, emitWord_cond(parent), parent.word(), countValue);
      countsArray.add(base_Words, new CondOutcomePair(parent.word(),BETA_array), countValue);
    } else {
      countsArray.add(p_Type, type_cond(parent), ca.Type(), countValue);

      if (rt == LEX) {
        countsArray.add(p_Tag, emit_cond(parent), parent.tag(), countValue);
        countsArray.add(base_Tags, new CondOutcomePair(parent.tag(),BETA_array), countValue);

        // if(lexicalized){
        countsArray.add(p_Word, emitWord_cond(parent), parent.word(), countValue);
        countsArray.add(base_Words, new CondOutcomePair(parent.word(),BETA_array), countValue);
        // }
      } else {
        countsArray.add(p_Arg, arg_cond(parent, ca), ca.Y(), countValue);
        countsArray.add(p_Comb, comb_cond(parent, ca), ca.combinator(), countValue);
        countsArray.add(base_Args, new CondOutcomePair(ca.Y(),BETA_array), countValue);
      }
    }
    if(!Configuration.ignorePunctuation) {
      punctuationCount(countsArray, countValue, parent,ca);
    }
  }

  @Override
  void punctuationCount(CountsArray cA, double val, ChartItem<Grammar> parent, ArgumentBackPointer backPointer) {
    if(backPointer.rule.Type == Rule_Type.FW_PUNCT){
      // Decide to emit  L/R &&  Emit tag
      cA.add(p_HasPunctNoHist, new CondOutcomePair(TRUE, LEFT_array), val);
      cA.add(base_HasPunct, new CondOutcomePair(TRUE,BETA_array),val);
      cA.add(p_PunctPrev, new CondOutcomePair(backPointer.leftChild.tag(),PunctPrev(LEFT, backPointer.rightChild().punc())),val);
      cA.add(base_Punct, new CondOutcomePair(backPointer.leftChild.tag(),LEFT_array),val);
    } else if (backPointer.rule.Type == Rule_Type.BW_PUNCT){
      cA.add(p_HasPunctNoHist, new CondOutcomePair(TRUE, RIGHT_array), val);
      cA.add(base_HasPunct, new CondOutcomePair(TRUE,RIGHT_array),val);
      cA.add(p_PunctPrev, new CondOutcomePair(backPointer.rightChild.tag(), PunctPrev(RIGHT, backPointer.leftChild().punc())),val);
      cA.add(base_Punct, new CondOutcomePair(backPointer.rightChild.tag(),RIGHT_array),val);
    } else if (!(backPointer.rule instanceof Unary) || backPointer.rule.Type == Rule_Type.TYPE_TOP){
      // Decide not to emit L and not to emit R
      cA.add(p_HasPunctNoHist, new CondOutcomePair(FALSE, LEFT_array), val);
      cA.add(p_HasPunctNoHist, new CondOutcomePair(FALSE, RIGHT_array), val);
      cA.add(base_HasPunct,new CondOutcomePair(FALSE,BETA_array),val);
      cA.add(base_HasPunct,new CondOutcomePair(FALSE,BETA_array),val);
    }
    super.punctuationCount(cA, val, parent, backPointer);
  }

  @Override
  public void update() {
    if (lexicalTransition) {
      Logger.logln("Only update lexical");
      base_Words.updateMLE();
      p_Word.updateVariational();
      lexicalTransition = false;
      return;
    }

    p_Comb.updateMLE();
    p_Type.updateMLE();

    base_Args.updateMLE();
    p_Arg.updateVariational();
    base_Tags.updateMLE();
    p_Tag.updateVariational();
    //if(lexicalized){
    base_Words.updateMLE();
    p_Word.updateVariational();
    //}

    if(!Configuration.ignorePunctuation) {
      base_Punct.updateMLE();
      p_PunctPrev.updateVariational();
      p_Punct.updateVariational();
      base_HasPunct.updateMLE();
      p_HasPunctNoHist.updateVariational();
      p_HasPunct.updateVariational();
    }
  }

  @Override
  public void p_emit(ChartItem<Grammar> parent, ArgumentBackPointer backPointer, double v) {
    if(!Configuration.ignorePunctuation &&    // Emited at max projection
        Tagset.Punct(parent.cell.chart.sentence.get(backPointer.leftChild.X).tag())){
      return;
    }
    if (!Test) {
      CountsArray localCounts = parent.cell.chart.priorCounts;
      base_Tags.addContext(BETA_array, parent.tag());
      base_Words.addContext(BETA_array, parent.word());
      if (Configuration.accumulateCounts) {
        localCounts.add(base_Tags, new CondOutcomePair(parent.tag(), BETA_array), v);
        localCounts.add(base_Words, new CondOutcomePair(parent.word(), BETA_array), v);
      }
    }
    super.p_emit(parent, backPointer, v);
  }

  @Override
  protected void p_Y(ChartItem<Grammar> parent, ArgumentBackPointer backPointer, double v) {
    if (!Test) {
      if(backPointer.rule.Type != Rule_Type.FW_PUNCT && backPointer.rule.Type != Rule_Type.BW_PUNCT) {
        base_Args.addContext(BETA_array, backPointer.Y());
        if (Configuration.accumulateCounts)
          parent.cell.chart.priorCounts.add(base_Args, new CondOutcomePair(backPointer.Y(),BETA_array), v);
      }
    }
    super.p_Y(parent, backPointer, v);
  }

  @Override
  void p_PunctY(ChartItem<Grammar> parent, ArgumentBackPointer backPointer, double v) {
    if(!Test) {
      long tag;
      CountsArray localCounts = parent.cell.chart.priorCounts;
      if(backPointer.rule.Type == Rule_Type.FW_PUNCT) {
        base_HasPunct.addContext(BETA_array,TRUE);
        p_HasPunctNoHist.addContext(LEFT_array,TRUE);
        tag = parent.cell.chart.tags[backPointer.leftChild().X];
        base_Punct.addContext(LEFT_array, tag);
        p_PunctPrev.addContext(PunctPrev(LEFT, backPointer.rightChild().punc()), tag);
        if (Configuration.accumulateCounts)
          localCounts.add(base_Punct, new CondOutcomePair(tag,LEFT_array),v);
        if(Configuration.uniformPrior && Configuration.accumulateCounts) {
          localCounts.add(base_HasPunct, new CondOutcomePair(TRUE,BETA_array),v);
          localCounts.add(p_HasPunctNoHist, new CondOutcomePair(TRUE,LEFT_array),v);
          localCounts.add(p_PunctPrev, new CondOutcomePair(tag,PunctPrev(LEFT, backPointer.rightChild().punc())),v);
        }
      } else if (backPointer.rule.Type == Rule_Type.BW_PUNCT){
        base_HasPunct.addContext(BETA_array,TRUE);
        p_HasPunctNoHist.addContext(RIGHT_array,TRUE);
        tag = parent.cell.chart.tags[backPointer.rightChild().X];
        base_Punct.addContext(RIGHT_array, tag);
        p_PunctPrev.addContext(PunctPrev(RIGHT, backPointer.leftChild().punc()), tag);
        if (Configuration.accumulateCounts)
          localCounts.add(base_Punct, new CondOutcomePair(tag,RIGHT_array),v);
        if(Configuration.uniformPrior && Configuration.accumulateCounts) {
          localCounts.add(base_HasPunct, new CondOutcomePair(TRUE,BETA_array),v);
          localCounts.add(p_HasPunctNoHist, new CondOutcomePair(TRUE,RIGHT_array),v);
          localCounts.add(p_PunctPrev, new CondOutcomePair(tag,PunctPrev(RIGHT, backPointer.leftChild().punc())),v);
        }
      } else {
        if(!Configuration.ignorePunctuation && !(backPointer.rule instanceof Unary)) {
          // Decide not to emit L and not to emit R
          base_HasPunct.addContext(BETA_array,FALSE);
          base_HasPunct.addContext(BETA_array,FALSE);
          p_HasPunctNoHist.addContext(LEFT_array,FALSE);
          p_HasPunctNoHist.addContext(RIGHT_array,FALSE);
          if(Configuration.uniformPrior && Configuration.accumulateCounts) {
            localCounts.add(base_HasPunct, new CondOutcomePair(FALSE,BETA_array), v);
            localCounts.add(base_HasPunct, new CondOutcomePair(FALSE,BETA_array),v);
            localCounts.add(p_HasPunctNoHist, new CondOutcomePair(FALSE,LEFT_array),v);
            localCounts.add(p_HasPunctNoHist, new CondOutcomePair(FALSE,RIGHT_array),v);
          }
        }
      }
      super.p_PunctY(parent, backPointer, v);
    }
  }

  @Override
  void p_TOP(ChartItem<Grammar> parent, ChartItem<Grammar> Child, double v) {
    if(!Test && !Configuration.ignorePunctuation){
      CountsArray localCounts = parent.cell.chart.priorCounts;
      // Decide not to emit L and not to emit R
      base_HasPunct.addContext(new CondOutcomePair(BETA),FALSE);
      base_HasPunct.addContext(new CondOutcomePair(BETA),FALSE);
      p_HasPunctNoHist.addContext(LEFT_array,FALSE);
      p_HasPunctNoHist.addContext(RIGHT_array,FALSE);
      if(Configuration.uniformPrior && Configuration.accumulateCounts) {
        localCounts.add(base_HasPunct, new CondOutcomePair(BETA), FALSE,v);
        localCounts.add(base_HasPunct,new CondOutcomePair(BETA), FALSE, v);
        localCounts.add(p_HasPunctNoHist, new CondOutcomePair(FALSE,LEFT_array),v);
        localCounts.add(p_HasPunctNoHist, new CondOutcomePair(FALSE,RIGHT_array),v);
      }
      super.p_TOP(parent, Child, v);
    }
  }


  /**
   * Returns conditioning variables array for direction of the punctuation and previous direction
   * @param dir current direction
   * @param p previous direction
   * @return CondOutcomePair
   */
  private static CondOutcomePair PunctPrev(long dir, Punctuation p){
    return new CondOutcomePair(dir,punc(p));
  }

  @Override
  public String prettyCond(CondOutcomePair conditioningVariables, Distribution d) {
    if (d == this.base_Args) {
      // FIXME:  This is fixed unchanging String value?
      return "Beta:" + grammar.Words.get(conditioningVariables.condVariable(0));
    }
    if (d == this.base_Tags) {
      return "Beta:" + grammar.Words.get(conditioningVariables.condVariable(0));
    }
    if (d == this.base_Words) {
      return "Beta:" + grammar.Words.get(conditioningVariables.condVariable(0));
    }
    if (d == this.p_PunctPrev){
      return "D:" + grammar.Words.get(conditioningVariables.condVariable(0))
          + " T:" + grammar.Words.get(conditioningVariables.condVariable(1));
    }
    if (d == this.p_HasPunctNoHist) {
      return "Beta:" + grammar.Words.get(conditioningVariables.condVariable(0));
    }
    if (d == this.base_Punct || d == this.base_HasPunct) {
      return "Beta:" + grammar.Words.get(conditioningVariables.condVariable(0));
    }
    return super.prettyCond(conditioningVariables, d);
  }

  @Override
  public String prettyOutcome(long out, Distribution d) {
    if (d == this.base_Args) {
      return "Y:" + grammar.prettyCat(out);
    }
    if (d == this.base_Tags) {
      return "t:" + grammar.prettyCat(out);
    }
    if (d == this.base_Words) {
      return "w:" + grammar.prettyCat(out);
    }
    if (d == this.base_Punct || d == this.p_PunctPrev) {
      return "m:" + grammar.prettyCat(out);
    }
    if (d == this.base_HasPunct || d == this.p_HasPunctNoHist) {
      return "D:" + grammar.Words.get(out);
    }
    return super.prettyOutcome(out, d);
  }

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    super.writeExternal(out);
    out.writeObject(base_Args);
    out.writeObject(base_Tags);
    out.writeObject(base_Words);
    out.writeObject(base_HasPunct);
    out.writeObject(p_PunctPrev);
    out.writeObject(base_Punct);
    out.writeObject(p_HasPunctNoHist);
    out.writeLong(BETA);
    out.writeObject(BETA_array);
  }

  @Override
  public void readExternal(ObjectInput in) throws IOException,
  ClassNotFoundException {
    super.readExternal(in);
    base_Args = (PYDistribution) in.readObject();
    base_Tags = (PYDistribution) in.readObject();
    base_Words = (PYDistribution) in.readObject();
    base_HasPunct = (PYDistribution) in.readObject();
    p_PunctPrev = (PYDistribution) in.readObject();
    base_Punct = (PYDistribution) in.readObject();
    p_HasPunctNoHist = (PYDistribution) in.readObject();
    Distributions.add(base_Args);
    base_Args.model = this;
    Distributions.add(base_Tags);
    base_Tags.model = this;
    Distributions.add(base_Words);
    base_Words.model = this;
    Distributions.add(base_HasPunct);
    base_HasPunct.model = this;
    Distributions.add(p_PunctPrev);
    p_PunctPrev.model = this;
    Distributions.add(base_Punct);
    base_Punct.model = this;
    Distributions.add(p_HasPunctNoHist);
    p_HasPunctNoHist.model = this;
    BETA = in.readLong();
    BETA_array = (CondOutcomePair) in.readObject();
  }

  /**
   * Computes KL divergece on all lexical distributions so those with no movement can be discarded
   * @throws Exception
   */
  public void disallowLowCondProbCats() throws Exception{
    initialized = false;
    // Compute the set of lexical categories to remove

    // Compute the conditional probabilities.   p( cat | tag )
    // p( c | t ) = p( t | c ) * p (c) / p (t)
    //            = p_Tag      * p (c) / base_Tag
    // p(c) == #cat / #tags
    HashMap<Long,HashMap<Long,Double>> CondProb = new HashMap<>();
    double total = Log.ZERO;
    for(CondOutcomePair pair : base_Tags.Counts.keySet()){
      total = Log.add(total, base_Tags.Counts.get(pair).value());
    }
    for(CondOutcomePair pair : base_Tags.Probabilities.keySet()){
      long tag = pair.outcome;
      CondProb.put(tag,new HashMap<>());
      double p_t = base_Tags.Probabilities.get(pair);

      for(CondOutcomePair cat : p_Tag.conditioning_contexts.keySet()) {
        // if exists  c --> t
        if (p_Tag.Probabilities.containsKey(new CondOutcomePair(tag, cat))){
          double p_t_c = p_Tag.P(new CondOutcomePair(tag, cat));

          Double count_c = Log.ZERO;
          for(CondOutcomePair t_c : p_Tag.conditioning_contexts.get(cat).keySet()){
            count_c = Log.add(count_c,p_Tag.Counts.get(t_c).value());
          }
          double p_c = Log.div(count_c, total);
          CondProb.get(tag).put(cat.condVariable(0), Log.div(Log.mul(p_t_c,p_c), p_t));
        }
      }
    }

    BufferedWriter Lexicon;
    if (!Configuration.printModelsVerbose) Lexicon = null;
    else Lexicon = TextFile.Writer(Configuration.Folder + "/Lexicon" + lexCount + ".txt.gz");
    // Ban those lexical rules
    for(POS t : Tagset.tags()){
      IntPair cat = new IntPair(grammar.Lex(t.toString()));
      if(grammar.Rules.containsKey(cat)){
        for(Rule r : grammar.Rules.get(cat).keySet()){
          if(!Tagset.Punct(t) && !Tagset.CONJ(t)) {
            if(!CondProb.containsKey(r.B) || !CondProb.get(r.B).containsKey(r.A)){
              grammar.unaryCheck.put(new IntPair(r.A,r.B), valid.Invalid);
            } else if(Math.exp(CondProb.get(r.B).get(r.A)) < Configuration.CondProb_threshold){
              grammar.unaryCheck.put(new IntPair(r.A,r.B), valid.Invalid);
            }
          }
          if (Lexicon != null) {
            if (CondProb.containsKey(r.B) && CondProb.get(r.B).containsKey(r.A)) {
              Lexicon.write(String.format("%13.10f   %-15s  %-5s\n",
                  Math.exp(CondProb.get(r.B).get(r.A)),
                  grammar.prettyCat(r.A), grammar.prettyCat(r.B)));
            } else {
              Lexicon.write(String.format("%13.10f   %-15s  %-5s\n",
                  -1.0, grammar.prettyCat(r.A), grammar.prettyCat(r.B)));
            }
          }
        }
      }
    }
    if (Lexicon != null) Lexicon.close();
    lexCount += 1;
  }
  private static int lexCount=0;

  public void trimDistributions() {
    ArrayList<ObjectDoublePair<CondOutcomePair>> categories = new ArrayList<>();
    categories.addAll(p_Tag.conditioning_contexts.keySet().stream().map(
        K -> new ObjectDoublePair<>(K, p_Tag.Counts.get(K).value())).collect(Collectors.toList()));
    Collections.sort(categories);
    for (ObjectDoublePair cat_count : categories) {
      Outcomes emissions = new Outcomes(p_Tag.conditioning_contexts.get(cat_count.content()).size());
      for (CondOutcomePair tag : p_Tag.conditioning_contexts.get(cat_count.content()).keySet())
        emissions.add(tag, p_Tag.P(tag));
      emissions.sort();

      double cum = 0.0;
      Set<CondOutcomePair> toKeep = new HashSet<>();
      for (int i = 0; i < emissions.length(); ++i) {
        if (cum < 0.95) {
          toKeep.add(emissions.pairs[i]);
          cum += Math.exp(emissions.vals[i]);
        }
      }

      Set<CondOutcomePair> allowedEmissions = new HashSet<>();
      allowedEmissions.addAll(p_Tag.conditioning_contexts.get(cat_count.content()).keySet().stream()
          .filter(pair -> !grammar.unaryCheck(pair.condVariable(0), pair.outcome).equals(valid.Invalid)) // FIXME: what about unused?
          .collect(Collectors.toList()));

      if (allowedEmissions.size() > toKeep.size()) {
        allowedEmissions.removeAll(toKeep);
        for (CondOutcomePair toRemove : allowedEmissions)
          grammar.unaryCheck.put(new IntPair(toRemove.condVariable(0),toRemove.outcome), valid.Invalid);
        return;
      }
    }
  }
}
