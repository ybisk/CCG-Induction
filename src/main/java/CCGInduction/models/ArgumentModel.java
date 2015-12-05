package CCGInduction.models;

import CCGInduction.Configuration;
import CCGInduction.ccg.CCGAtomic;
import CCGInduction.data.Tagset;
import CCGInduction.grammar.*;
import CCGInduction.learning.CondOutcomePair;
import CCGInduction.learning.Distribution;
import CCGInduction.learning.PYDistribution;
import CCGInduction.parser.BackPointer;
import CCGInduction.parser.ChartItem;
import CCGInduction.parser.Punctuation;
import CCGInduction.utils.Math.Log;
import CCGInduction.ccg.InducedCAT;
import CCGInduction.learning.CountsArray;
import CCGInduction.parser.Cell;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An argument model which generates Y given X|Z.
 * Y. Bisk and J. Hockenmaier, “An HDP Model for Inducing Combinatory Categorial Grammars,”
 * Transactions of the Association for Computational Linguistics, pp. 75–88, 2013.
 * @author bisk1
 */
public class ArgumentModel extends Model<Grammar> {
  private static final long serialVersionUID = 7802001491369992846L;

  /**
   * Default constructor
   */
  public ArgumentModel() {
    this.createFine = false;
  }

  /**
   * Probability of a given combinator (parsing action) given parent and
   * argument categories
   */
  PYDistribution p_Comb;
  /**
   * Probability of a give word/tag given a parent category
   */
  public PYDistribution p_Tag;
  /**
   * Probability of a given category as the argument Y in X|Y Y|Z given X|Z and
   * a head direction.
   */
  PYDistribution p_Arg;
  /**
   * Probability of choosing a head direction (Left,Right,Unary) or Lexical
   * production given a category
   */
  PYDistribution p_Type;
  /**
   * p( w | t , c ) which is ignored when we are not lexicalized
   */
  PYDistribution p_Word;
  /**
   * Stores the precomputed argument (Y) for a given grammar rule
   */
  private ConcurrentHashMap<Rule, Long> Y_Cats = new ConcurrentHashMap<>();
  /**
   * Stores the codewords for parsing actions
   */
  private ConcurrentHashMap<Rule_Type, Long> rule_type = new ConcurrentHashMap<>();
  /**
   * Constant for generative action - Lexical
   */
  public static long LEX;
  /**
   * Constant for generative action - Unary
   */
  static long UNARY;
  /**
   * Constant for generative action - Head Left
   */
  static long LEFT;
  /**
   * Constant for genenerative action - Head Right.
   */
  static long RIGHT;
  /**
   * Specify if model is lexicalized
   */
  static boolean lexicalized = false;
  /**
   * Specify if we should hold all but lexical distribution constant
   */
  public static boolean lexicalTransition = false;

  /** Left Punctuation vs Right Punctuation */
  PYDistribution p_HasPunct;
  /** Punctuation mark given position : p( wp | L/R, Prev, P)*/
  PYDistribution p_Punct;

  /** True, we have punctuation */
  static long TRUE;
  /** False, we have punctuation */
  static long FALSE;
  /** NONE, no punctuation history in previous move */
  private static long NONE;
  /** FW, punctuation history in previous move */
  private static long FW;
  /** BW, punctuation history in previous move */
  private static long BW;
  /** Punctuation conditioned on being on the left of a constituent */
  static CondOutcomePair LEFT_array;
  /** Punctuation conditioned on being on the right of a constituent */
  static CondOutcomePair RIGHT_array;


  /**
   * Model constructor which requires a grammar instance and configuration file
   * 
   * @param grammar Grammar
   */
  public ArgumentModel(Grammar grammar) {
    this.grammar = grammar;
    this.p_Comb = new PYDistribution(this, "p_Comb");
    this.p_Tag = new PYDistribution(this, "p_Tag");
    this.p_Arg = new PYDistribution(this, "p_Arg");
    this.p_Type = new PYDistribution(this, "p_Type");

    // Normally want in lexicalize function?
    this.p_Word = new PYDistribution(this, "p_Word");

    this.p_HasPunct = new PYDistribution(this, "p_HasPunct");
    this.p_Punct = new PYDistribution(this, "p_Punct");

    setup();
  }

  public ArgumentModel(ArgumentModel model) {
    grammar    = model.grammar.copy();
    p_Comb     = model.p_Comb.copy();
    p_Tag      = model.p_Tag.copy();
    p_Arg      = model.p_Arg.copy();
    p_Type     = model.p_Type.copy();
    p_Word     = model.p_Word.copy();
    p_HasPunct = model.p_HasPunct.copy();
    p_Punct    = model.p_Punct.copy();
    Test       = model.Test;
    setup();
  }

  public Model<Grammar> copy() {
    return new ArgumentModel(this);
  }

  private void setup() {
    this.createFine = false;
    this.Distributions.add(this.p_Comb);
    this.Distributions.add(this.p_Tag);
    this.Distributions.add(this.p_Arg);
    this.Distributions.add(this.p_Type);
    this.Distributions.add(this.p_Word);
    this.Distributions.add(this.p_HasPunct);
    this.Distributions.add(this.p_Punct);

    for (Rule_Type rt : Rule_Type.values()) {
      rule_type.put(rt, this.grammar.Lex(rt.toString()));
    }

    ArgumentModel.LEX = this.grammar.Lex("#LEX#");
    ArgumentModel.LEFT = this.grammar.Lex("#LEFT#");
    ArgumentModel.RIGHT = this.grammar.Lex("#RIGHT#");
    ArgumentModel.UNARY = this.grammar.Lex("#UNARY#");
    // Constants
    TRUE = this.grammar.Lex("#TRUE#");
    FALSE = this.grammar.Lex("#FALSE#");
    NONE = this.grammar.Lex("#NONE#");
    FW = this.grammar.Lex("#FW#");
    BW = this.grammar.Lex("#BW#");
    LEFT_array = new CondOutcomePair(LEFT);
    RIGHT_array = new CondOutcomePair(RIGHT);
  }

  @Override
  public void init() {
    this.p_Comb.init();
    this.p_Tag.init();
    this.p_Arg.init();
    this.p_Type.init();

    // Normally want in lexicalize function?
    this.p_Word.init();

    if(!Configuration.ignorePunctuation) {
      this.p_HasPunct.init();
      this.p_Punct.init();
    }
    initialized = true;
  }

  @Override
  public CondOutcomePair backoff(CondOutcomePair cxt, Distribution d) {
    return null;
  }

  @Override
  public void update() {
    this.p_Comb.update();
    this.p_Tag.update();
    this.p_Arg.update();
    this.p_Type.update();

    // Normally want in lexicalize function?
    this.p_Word.update();

    if(!Configuration.ignorePunctuation) {
      this.p_HasPunct.update();
      this.p_Punct.update();
    }
  }

  /**
   * Specify if model should transition to emitting words
   */
  @SuppressWarnings("static-method")
  public void lexicalize() {
    lexicalized = true;
    lexicalTransition = true;
  }
  public void delexicalize() {
    lexicalized = false;
    lexicalTransition = false;
  }

  @Override
  public BackPointer<Grammar> newBackPointer(Rule rule, ChartItem<Grammar> child) {
    return new ArgumentBackPointer((Unary) rule, child);
  }

  @Override
  public BackPointer<Grammar> newBackPointer(Rule rule, ChartItem<Grammar> leftChild, ChartItem<Grammar> rightChild) {
    return new ArgumentBackPointer((Binary) rule, leftChild, rightChild);
  }

  @Override
  public void buildUnaryContext(ChartItem<Grammar> parent, BackPointer<Grammar> backPointer) {
    buildUnaryContext(parent, (ArgumentBackPointer) backPointer);
  }

  /**
   * Finds all values needed for Argument decomposition of a local tree
   * 
   * @param parent  Parent
   * @param backPointer  Backpointer
   */
  void buildUnaryContext(ChartItem<Grammar> parent, ArgumentBackPointer backPointer) {
    super.buildUnaryContext(parent, backPointer);
    Unary rule = (Unary) backPointer.rule;

    // Build context array
    backPointer.combinator(rule_type.get(rule.Type));
    double v = Log.ZERO;
    if (rule.Type.equals(Rule_Type.PRODUCTION)) {
      /* Type */
      backPointer.Type(LEX);
      /* Emissions */
      // TODO: Parses should be top?
      if(!Test)
        v = Log.div(Math.log(parent.outside_parses), Math.log(parent.cell.chart.parses));
      /* P Emit */
      p_emit(parent, backPointer, v);
    } else {
      /* Type */
      backPointer.Type(UNARY);
      Long Y;
      if ((Y = Y_Cats.get(backPointer.rule)) == null) {
        Y = rule.B;
        Y_Cats.putIfAbsent(backPointer.rule, Y);
      }
      /* Arguments and Combinators */
      backPointer.Y(Y);
      // TODO: Parses should be top?
      if (!Test)
        v = Log.div(Math.log(parent.outside_parses*backPointer.leftChild.parses),Math.log(parent.cell.chart.parses));
      p_Y(parent, backPointer, v);
      p_comb(parent, backPointer, v);

      if(parent.Category == parent.cell.chart.TOP.Category) {
        p_TOP(parent, backPointer.leftChild(), v);
      }
    }
    /* Generate Type */
    p_type(parent, backPointer, v);
  }

  @Override
  public void buildBinaryContext(ChartItem<Grammar> parent, BackPointer<Grammar> backPointer) {
    buildBinaryContext(parent, (ArgumentBackPointer) backPointer);
  }

  /**
   * Finds all values needed for Argument decomposition of a Binary local tree
   *
   * @param parent  Parent
   * @param backPointer  Backpointer
   */
  void buildBinaryContext(ChartItem<Grammar> parent, ArgumentBackPointer backPointer) {
    super.buildBinaryContext(parent, backPointer);
    Binary rule = (Binary) backPointer.rule;
    Long Y;
    if ((Y = Y_Cats.get(backPointer.rule)) == null) {
      InducedCAT left = grammar.Categories.get(rule.B);
      InducedCAT right = grammar.Categories.get(rule.C);
      switch (rule.Type) {
      case FW_APPLY:
      case FW_COMPOSE:
      case FW_2_COMPOSE:
      case FW_3_COMPOSE:
      case FW_4_COMPOSE:
      case FW_XCOMPOSE:
        Y = grammar.iCategories.get(left.Arg);
        break;
      case BW_APPLY:
      case BW_COMPOSE:
      case BW_2_COMPOSE:
      case BW_3_COMPOSE:
      case BW_4_COMPOSE:
      case BW_XCOMPOSE:
        Y = grammar.iCategories.get(right.Arg);
        break;
        // X[conj] --> conj X
      case FW_CONJOIN:
      case FW_PUNCT:
        // Y = conj
        Y = grammar.iCategories.get(left);
        break;
        // X --> X X[conj]
      case BW_CONJOIN:
        // Y = [conj]
        Y = grammar.NT(new InducedCAT(new CCGAtomic("[conj]")));
        break;
      case BW_PUNCT:
        Y = grammar.iCategories.get(right);
        break;
      case BW_TYPECHANGE:
        Y = grammar.iCategories.get(left);
        break;
      case FW_TYPECHANGE:
        Y = grammar.iCategories.get(right);
        break;
      case FW_SUBSTITUTION:
        Y = grammar.iCategories.get(right.Res.Arg);
        break;
      default:
        throw new Model.FailedModelAssertion("Not fully implemented: " + rule.toString(grammar));
      }
      Y_Cats.put(backPointer.rule, Y);
    }

    if ((rule.head.equals(Rule_Direction.Left) && !Rule_Type.TR(backPointer.leftChild().type()))
        || (rule.head.equals(Rule_Direction.Right) && Rule_Type.TR(backPointer.rightChild().type()))) {
      backPointer.Type(LEFT);
    } else {
      backPointer.Type(RIGHT);
    }
    backPointer.combinator(rule_type.get(rule.Type));
    backPointer.Y(Y);
    // Should this be TOP.parses?
    double v = Log.ZERO;
    if(!Test)
      v = Log.div(Math.log(parent.outside_parses*backPointer.leftChild.parses*backPointer.rightChild.parses),
                       Math.log(parent.cell.chart.parses));
    p_type(parent, backPointer, v);
    p_comb(parent, backPointer, v);
    p_Y(parent, backPointer, v);
  }

  @Override
  public double prob(ChartItem<Grammar> parent, BackPointer<Grammar> backpointer) {
    return prob(parent, (ArgumentBackPointer) backpointer);
  }

  /**
   * Returns probability based on an ArgumentBackPointer rather than general
   * BackPointer
   *
   * @param parent  Parent
   * @param backPointer  Backpointer
   * @return probability
   */
  double prob(ChartItem<Grammar> parent, ArgumentBackPointer backPointer) {
    if(!Configuration.ignorePunctuation &&
        (backPointer.rule.Type == Rule_Type.FW_PUNCT || backPointer.rule.Type == Rule_Type.BW_PUNCT)) {
      return punctuation(parent,backPointer);
    }

    if (backPointer.Type() == LEX && Tagset.Punct(parent.cell.chart.sentence.get(parent.X).tag())) {
      return Log.ONE;       // Already generated at max-proj attachment
    }
    double value;
    if (backPointer.Type() == LEX) {
      // p( t | c )
      if (!lexicalized || lexicalTransition) {
        value = Log.mul(p_Type.P(type_cond(parent), backPointer.Type()),
                        p_Tag.P(emit_cond(parent),  parent.tag()));
      } else {
        // p( w | c )
          value = Log.mul(p_Type.P(type_cond(parent),     backPointer.Type()),
                          p_Word.P(emitWord_cond(parent), parent.word())); // word will be unk-tag if unk
      }
      // Optional: Would force the single lexical item to be N
      // value = Log.mul(value, entityProbability(parent.cell, parent.Category));
    } else {
      value = Log.mul(p_Type.P(type_cond(parent), backPointer.Type()),
          p_Comb.P(comb_cond(parent, backPointer), backPointer.combinator()),
          p_Arg.P(arg_cond(parent, backPointer), backPointer.Y()),
          bracketProbability(parent.cell),
          entityProbability(parent.cell, parent.Category));
    }
    if(!Configuration.ignorePunctuation) {
      return punctuation(value, backPointer);
    }
    return value;
  }

  /**
   * Re-weights a constituent based on crossing a bracketing
   * @param cell Cell whose span we're weighting
   * @return Scaling factor
   */
  double bracketProbability(Cell<?> cell) {
    if (Configuration.softBracketConstraints) {
      if (!cell.chart.crossingBrackets(cell.X, cell.Y)) {
        return Configuration.softBracketWeightingLog;
      }
      return Configuration.softBracketWeightingPenaltyLog;
    } else {
      return Log.ONE;
    }
  }

  /**
   * Down/Up-weights constituents that correspond to full entity but do/don't
   * result in the category N
   * @param cell Cell whose span we're weighting
   * @param category Category producing the span
   * @return Weighting factor
   */
  double entityProbability(Cell<?> cell, long category) {
    if (Configuration.softEntityNConstraints
        && cell.chart.fullEntity(cell.X, cell.Y)) {
      if (InducedCAT.N(grammar.Categories.get(category))) {
        return Configuration.softBracketWeightingLog;
      } else {
        return Configuration.softBracketWeightingPenaltyLog;
      }
    } else {
      return Log.ONE;
    }
  }

  /**
   * Multipies local tree probability by the stop probabilities for punctuation
   * @param val Current local tree probability
   * @param backPointer  Backpointer
   * @return  New Probability
   */
  double punctuation(double val, ArgumentBackPointer backPointer) {
    if (backPointer.rule instanceof Unary  && backPointer.rule.Type != Rule_Type.TYPE_TOP) {
      return val;
    }

    if (backPointer.rule instanceof Unary){  // This is only At TOP, no other Unary are Max Proj
      // Decide not to emit L and not to emit R
      return Log.mul(val,
          p_HasPunct.P(new CondOutcomePair(LEFT,punc(backPointer.leftChild().punc())), FALSE),
          p_HasPunct.P(new CondOutcomePair(RIGHT,punc(backPointer.leftChild().punc())),FALSE));
    }

    if(((Binary)backPointer.rule).head == Rule_Direction.Left){
      // Decide not to emit L and not to emit R
      return Log.mul(val,
          p_HasPunct.P(new CondOutcomePair(LEFT,punc(backPointer.rightChild().punc())), FALSE),
          p_HasPunct.P(new CondOutcomePair(RIGHT,punc(backPointer.rightChild().punc())),FALSE));
    }
    return Log.mul(val,
        p_HasPunct.P(new CondOutcomePair(LEFT,punc(backPointer.leftChild().punc())), FALSE),
        p_HasPunct.P(new CondOutcomePair(RIGHT,punc(backPointer.leftChild().punc())),FALSE));
  }
  /**
   * Probability of emiting punctuation marks
   * @param parent Parent
   * @param backPointer Backpointer
   * @return Probability
   */
  double punctuation(ChartItem<Grammar> parent, ArgumentBackPointer backPointer) {
    if(backPointer.rule.Type == Rule_Type.FW_PUNCT){
      // Decide to emit  L/R &&  Emit tag
      return Log.mul(p_HasPunct.P(hasPunct_cond(backPointer),TRUE),
          p_Punct.P(new CondOutcomePair(backPointer.leftChild.tag(),emitPunct_cond(parent, backPointer))));
    }
    return Log.mul(p_HasPunct.P(hasPunct_cond(backPointer),TRUE),
        p_Punct.P(new CondOutcomePair(backPointer.rightChild.tag(),emitPunct_cond(parent, backPointer))));
  }
  /**
   * Accumulate stop/emit probabilities for punctuation
   * @param cA Local counts
   * @param val count value
   * @param parent Parent
   * @param backPointer Backpointer
   */
  void punctuationCount(CountsArray cA, double val, ChartItem<Grammar> parent, ArgumentBackPointer backPointer) {
    if(backPointer.rule.Type == Rule_Type.FW_PUNCT){
      // Decide to emit  L/R &&  Emit tag
      cA.add(p_HasPunct, hasPunct_cond(backPointer),TRUE,val);
      cA.add(p_Punct, new CondOutcomePair(backPointer.leftChild.tag(),emitPunct_cond(parent, backPointer)),val);
    } else if (backPointer.rule.Type == Rule_Type.BW_PUNCT){
      cA.add(p_HasPunct,hasPunct_cond(backPointer),TRUE,val);
      cA.add(p_Punct, new CondOutcomePair(backPointer.rightChild.tag(),emitPunct_cond(parent, backPointer)),val);
    } else if (!(backPointer.rule instanceof Unary)){
      if(backPointer.rule instanceof Binary && ((Binary)backPointer.rule).head == Rule_Direction.Left) {
        // Decide not to emit L and not to emit R
        cA.add(p_HasPunct,new CondOutcomePair(LEFT,punc(backPointer.rightChild().punc())), FALSE,val);
        cA.add(p_HasPunct,new CondOutcomePair(RIGHT,punc(backPointer.rightChild().punc())),FALSE,val);
      }
      if(backPointer.rule instanceof Binary && ((Binary)backPointer.rule).head == Rule_Direction.Right) {
        cA.add(p_HasPunct,new CondOutcomePair(LEFT,punc(backPointer.leftChild().punc())), FALSE,val);
        cA.add(p_HasPunct,new CondOutcomePair(RIGHT,punc(backPointer.leftChild().punc())),FALSE,val);
      }
    } else if  (backPointer.rule.Type == Rule_Type.TYPE_TOP){
      cA.add(p_HasPunct,new CondOutcomePair(LEFT,punc(backPointer.leftChild().punc())), FALSE,val);
      cA.add(p_HasPunct,new CondOutcomePair(RIGHT,punc(backPointer.leftChild().punc())),FALSE,val);
    }
  }

  @Override
  public void count(ChartItem<Grammar> parent, BackPointer<Grammar> backPointer, double countValue,
                    CountsArray countsArray)  {
    ArgumentBackPointer ca = (ArgumentBackPointer) backPointer;

    if(!Configuration.ignorePunctuation &&
        (backPointer.rule.Type == Rule_Type.FW_PUNCT || backPointer.rule.Type == Rule_Type.BW_PUNCT)) {
      punctuationCount(countsArray, countValue, parent,ca);
      return;
    }

    long rt = ca.Type();
    if (lexicalTransition) {
      countsArray.add(p_Word, emitWord_cond(parent), parent.word(), countValue);
    } else {
      countsArray.add(p_Type, type_cond(parent), ca.Type(), countValue);

      if (rt == LEX) {
        countsArray.add(p_Tag, emit_cond(parent), parent.tag(), countValue);
        if (lexicalized) {
          countsArray.add(p_Word, emitWord_cond(parent), parent.word(), countValue);
        }
      } else {
        countsArray.add(p_Arg, arg_cond(parent, ca), ca.Y(), countValue);
        countsArray.add(p_Comb, comb_cond(parent, ca), ca.combinator(), countValue);
      }
    }

    if(!Configuration.ignorePunctuation) {
      punctuationCount(countsArray, countValue, parent,ca);
    }
  }

  /**
   * Built context at parent, only if not ignoring punctuation
   * @param parent  Parent(TOP)
   * @param Child  Child chart item
   * @param v   init value
   */
  void p_TOP(ChartItem<Grammar> parent, ChartItem<Grammar> Child, double v) {
    if(!Test && !Configuration.ignorePunctuation){
      // Decide not to emit L and not to emit R
      p_HasPunct.addContext(new CondOutcomePair(LEFT,punc(Child.punc())), FALSE);
      p_HasPunct.addContext(new CondOutcomePair(RIGHT,punc(Child.punc())),FALSE);
      if(Configuration.uniformPrior && Configuration.accumulateCounts) {
        CountsArray localCounts = parent.cell.chart.priorCounts;
        localCounts.add(p_HasPunct, new CondOutcomePair(RIGHT,punc(Child.punc())),FALSE, v);
        localCounts.add(p_HasPunct, new CondOutcomePair(LEFT,punc(Child.punc())), FALSE, v);
      }
    }
  }

  /**
   * Adds a seen conditioning context,outcome pair to the appropriate
   * distribution p_Type
   * 
   * @param parent Parent
   * @param ca back-pointer
   * @param v prior count value
   */
  void p_type(ChartItem<Grammar> parent, ArgumentBackPointer ca, double v) {
    if(ca.rule.Type == Rule_Type.FW_PUNCT || ca.rule.Type == Rule_Type.BW_PUNCT) {
        return;
    }
    if (!Test) {
      p_Type.addContext(type_cond(parent), ca.Type());
      if(Configuration.uniformPrior && Configuration.accumulateCounts){
        parent.cell.chart.priorCounts.add(p_Type, type_cond(parent), ca.Type(), v);
      }
    }
  }

  /**
   * Adds a seen conditioning context,outcome pair to the appropriate
   * distribution p_Comb
   * 
   * @param parent Parent
   * @param backPointer back-pointer
   * @param v prior count value
   */
  void p_comb(ChartItem<Grammar> parent, ArgumentBackPointer backPointer, double v) {
    if(backPointer.rule.Type == Rule_Type.FW_PUNCT || backPointer.rule.Type == Rule_Type.BW_PUNCT) {
        return;
    }
    if (!Test) {
      p_Comb.addContext(comb_cond(parent, backPointer), backPointer.combinator());
      if(Configuration.uniformPrior && Configuration.accumulateCounts){
        parent.cell.chart.priorCounts.add(p_Comb, comb_cond(parent, backPointer), backPointer.combinator(), v);
      }
    }
  }

  /**
   * Adds a seen conditioning context,outcome pair to the appropriate
   * distribution p_Tag
   * 
   * @param parent Parent
   * @param backPointer back-pointer
   * @param v prior count value
   */
  void p_emit(ChartItem<Grammar> parent, ArgumentBackPointer backPointer, double v) {
    if(!Configuration.ignorePunctuation && /// Emited at max projection
        Tagset.Punct(parent.cell.chart.sentence.get(backPointer.leftChild.X).tag())){
      return;
    }
    if (!Test) {
      p_Tag.addContext(emit_cond(parent), parent.tag());
      p_Word.addContext(emitWord_cond(parent), parent.word());
      if(Configuration.uniformPrior && Configuration.accumulateCounts) {
        CountsArray localCounts = parent.cell.chart.priorCounts;
        localCounts.add(p_Tag, emit_cond(parent), parent.tag(), v);
        localCounts.add(p_Word, emitWord_cond(parent), parent.word(), v);
      }
    }
  }

  /**
   * Adds a seen conditioning context,outcome pair to the appropriate
   * distribution p_Arg
   * 
   * @param parent Parent
   * @param backPointer back-pointer
   * @param v prior count value
   */
  void p_Y(ChartItem<Grammar> parent, ArgumentBackPointer backPointer, double v) {
    if (!Test) {
      CountsArray localCounts = parent.cell.chart.priorCounts;
      if(backPointer.rule.Type != Rule_Type.FW_PUNCT && backPointer.rule.Type != Rule_Type.BW_PUNCT) {
        p_Arg.addContext(arg_cond(parent, backPointer), backPointer.Y());
        if(Configuration.uniformPrior && Configuration.accumulateCounts){
          localCounts.add(p_Arg, arg_cond(parent, backPointer), backPointer.Y(), v);
        }
      }
      p_PunctY(parent,backPointer,v);
    }
  }

  /**
   * Generate appropriate conditioning contexts for argument's that either do/don't generate
   * punctuation marks
   * @param parent parent
   * @param backPointer backpointer
   * @param v  init value
   */
  void p_PunctY(ChartItem<Grammar> parent, ArgumentBackPointer backPointer, double v) {
    if (!Test) {
      CountsArray localCounts = parent.cell.chart.priorCounts;
      if(backPointer.rule.Type == Rule_Type.FW_PUNCT || backPointer.rule.Type == Rule_Type.BW_PUNCT) {
        p_HasPunct.addContext(hasPunct_cond(backPointer), TRUE);
        if(Configuration.uniformPrior && Configuration.accumulateCounts) {
          localCounts.add(p_HasPunct, hasPunct_cond(backPointer),TRUE,v);
        }
        if(backPointer.rule.Type == Rule_Type.FW_PUNCT) {
          p_Punct.addContext(emitPunct_cond(parent, backPointer), parent.cell.chart.tags[backPointer.leftChild().X]);
          if(Configuration.uniformPrior && Configuration.accumulateCounts) {
            localCounts.add(p_Punct, new CondOutcomePair(parent.cell.chart.tags[backPointer.leftChild().X],emitPunct_cond(parent,backPointer)),v);
          }
        } else {
          p_Punct.addContext(emitPunct_cond(parent, backPointer), parent.cell.chart.tags[backPointer.rightChild().X]);
          if(Configuration.uniformPrior && Configuration.accumulateCounts) {
            localCounts.add(p_Punct, new CondOutcomePair(parent.cell.chart.tags[backPointer.rightChild().X],emitPunct_cond(parent,backPointer)),v);
          }
        }
      } else {
        if(!Configuration.ignorePunctuation && !(backPointer.rule instanceof Unary)) {
          // Decide not to emit L and not to emit R
          if(backPointer.type == RIGHT) { //((Binary)backPointer.rule).head == Rule_Direction.Right) {
            p_HasPunct.addContext(new CondOutcomePair(LEFT,punc(backPointer.leftChild().punc())), FALSE);
            p_HasPunct.addContext(new CondOutcomePair(RIGHT,punc(backPointer.leftChild().punc())),FALSE);
            if(Configuration.uniformPrior && Configuration.accumulateCounts) {
              localCounts.add(p_HasPunct, new CondOutcomePair(LEFT,punc(backPointer.leftChild().punc())), FALSE, v);
              localCounts.add(p_HasPunct, new CondOutcomePair(RIGHT,punc(backPointer.leftChild().punc())),FALSE,v);
            }
          }
          if(backPointer.type == LEFT) { //((Binary)backPointer.rule).head == Rule_Direction.Left) {
            p_HasPunct.addContext(new CondOutcomePair(LEFT,punc(backPointer.rightChild().punc())), FALSE);
            p_HasPunct.addContext(new CondOutcomePair(RIGHT,punc(backPointer.rightChild().punc())),FALSE);
            if(Configuration.uniformPrior && Configuration.accumulateCounts) {
              localCounts.add(p_HasPunct, new CondOutcomePair(LEFT,punc(backPointer.rightChild().punc())), FALSE, v);
              localCounts.add(p_HasPunct, new CondOutcomePair(RIGHT,punc(backPointer.rightChild().punc())),FALSE,v);
            }
          }
        }
      }
    }
  }

  /**
   * Creates an array of the appropriate conditioning variables ( Parent )
   * 
   * @param parent Parent ChartItem
   * @return Conditioning Variables
   */
  @SuppressWarnings("static-method")
  CondOutcomePair type_cond(ChartItem<Grammar> parent) {
    return new CondOutcomePair(parent.Category);
  }

  /**
   * Creates an array of the appropriate conditioning variables ( Parent, Type,
   * Argument )
   * 
   * @param parent Parent
   * @param backPointer Backpointer
   * @return Conditioning Variables
   */
  @SuppressWarnings("static-method")
  CondOutcomePair comb_cond(ChartItem<Grammar> parent, ArgumentBackPointer backPointer) {
    return new CondOutcomePair(parent.Category, backPointer.Type(), backPointer.Y());
  }

  /**
   * Creates an array of the appropriate conditioning variables ( Parent, Type )
   * 
   * @param parent Parent chart Item
   * @return Conditioning Variables
   */
  @SuppressWarnings("static-method")
  CondOutcomePair emit_cond(ChartItem<Grammar> parent) {
    return new CondOutcomePair(parent.Category);
  }

  /**
   * Creates an array of the appropriate conditioning variables ( Parent, Type )
   * @param parent Parent chart Item
   * @return Conditioning Variables
   */
  @SuppressWarnings("static-method")
  CondOutcomePair emitWord_cond(ChartItem<Grammar> parent) {
    return new CondOutcomePair(parent.Category);
  }

  /**
   * Creates an array of the appropriate conditioning variables ( Parent, Type )
   * 
   * @param parent Parent chart Item
   * @param backPointer Backpointer
   * @return Conditioning Variables
   */
  @SuppressWarnings("static-method")
  CondOutcomePair arg_cond(ChartItem<Grammar> parent, ArgumentBackPointer backPointer) {
    return new CondOutcomePair(parent.Category, backPointer.Type());
  }

  @Override
  public String prettyCond(CondOutcomePair conditioningVariables, Distribution d) {
    if (d == this.p_Comb) {
      return prettyCombCond(conditioningVariables);
    }
    if (d == this.p_Tag) {
      return prettyTagCond(conditioningVariables);
    }
    if (d == this.p_Word) {
      return prettyWordCond(conditioningVariables);
    }
    if (d == this.p_Arg) {
      return prettyArgCond(conditioningVariables);
    }
    if (d == this.p_Type) {
      return prettyTypeCond(conditioningVariables);
    }
    if (d == this.p_Punct) {
      return prettyPunctCond(conditioningVariables);
    }
    if (d == this.p_HasPunct) {
      return prettyHasPunctCond(conditioningVariables);
    }
    return null;
  }

  /**
   * Print human readable combinatator distribution's conditioning variables
   * p(ci|P,t,Y)
   * @param cxt Conditioning Context
   * @return String
   */
  String prettyCombCond(CondOutcomePair cxt) {
    return "P:" + grammar.prettyCat(cxt.condVariable(0))
        + " D:" + grammar.Words.get(cxt.condVariable(1))
        + " Y:" + grammar.prettyCat(cxt.condVariable(2));
  }

  /**
   * Print human readable combinatator distribution's conditioning variables
   * p(tag|P,t)
   * @param cxt Conditioning Context
   * @return String
   */
  protected String prettyTagCond(CondOutcomePair cxt) {
    return "P:" + grammar.prettyCat(cxt.condVariable(0));
  }

  /**
   * Print human readable combinatator distribution's conditioning variables
   * p(word|P,t)
   * @param cxt Conditioning Context
   * @return String
   */
  private String prettyWordCond(CondOutcomePair cxt) {
    return "P:" + grammar.prettyCat(cxt.condVariable(0));
  }

  /**
   * Print human readable argument distribution's conditioning variables
   * p(Y|P,t,cH)
   * @param cxt Conditioning Context
   * @return String
   */
  String prettyArgCond(CondOutcomePair cxt) {
    return "P:" + grammar.prettyCat(cxt.condVariable(0))
        + " D:" + grammar.Words.get(cxt.condVariable(1));
  }

  /**
   * Print human readable type distribution's conditioning variables
   * p(t|P,cH)
   * @param cxt Conditioning Context
   * @return String
   */
  String prettyTypeCond(CondOutcomePair cxt) {
    return "P:" + grammar.prettyCat(cxt.condVariable(0));
  }

  @Override
  public String prettyOutcome(long out, Distribution d) {
    if (d == this.p_Comb) {
      return "T:" + grammar.Words.get(out);
    }
    if (d == this.p_Tag) {
      return "t:" + grammar.prettyCat(out);
    }
    if (d == this.p_Word) {
      return "w:" + grammar.prettyCat(out);
    }
    if (d == this.p_Arg) {
      return "Y:" + grammar.prettyCat(out);
    }
    if (d == this.p_Type) {
      return "D:" + grammar.Words.get(out);
    }
    if (d == this.p_Punct) {
      return "m:" + grammar.prettyCat(out);
    }
    if (d == this.p_HasPunct) {
      return "B:" + grammar.Words.get(out);
    }
    return null;
  }

  /**
   * Conditioning context for emitting punctuation
   * P( * | Dir, prev, Parent )
   * @param p Parent
   * @param ca Backpointer
   * @return conditioning variables
   */
  private static CondOutcomePair emitPunct_cond(ChartItem<Grammar> p, ArgumentBackPointer ca) {
    if(ca.Type() == RIGHT) {
      return new CondOutcomePair(LEFT,punc(ca.rightChild().punc()),p.Category);
    }
    return new CondOutcomePair(RIGHT,punc(ca.leftChild().punc()),p.Category);
  }

  private String prettyPunctCond(CondOutcomePair cxt) {
    return "D:" + grammar.Words.get(cxt.condVariable(0))
        + " H:" + grammar.Words.get(cxt.condVariable(1))
        + " P:" + grammar.prettyCat(cxt.condVariable(2));
  }

  /**
   * Map Punctuation ENUM values to hashed longs
   * @param p punctuation history
   * @return Hashed long values
   */
  static long punc(Punctuation p){
    if(p == Punctuation.FW) {
      return FW;
    }
    if(p == Punctuation.BW) {
      return BW;
    }
    return NONE;
  }

  /**
   * Conditioning variables for having punctuation
   * @param ca Backpointer
   * @return Conditioning context
   */
  private static CondOutcomePair hasPunct_cond(ArgumentBackPointer ca) {
    if(ca.rule.Type.equals(Rule_Type.FW_PUNCT)) {
      return new CondOutcomePair(LEFT,punc(ca.rightChild().punc()));
    }
    return new CondOutcomePair(RIGHT,punc(ca.leftChild().punc()));
  }

  private String prettyHasPunctCond(CondOutcomePair cxt) {
    return "D:" + grammar.Words.get(cxt.condVariable(0))
        + " H:" + grammar.Words.get(cxt.condVariable(1));
  }

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    super.writeExternal(out);
    out.writeObject(p_Comb);
    out.writeObject(p_Tag);
    out.writeObject(p_Arg);
    out.writeObject(p_Type);
    out.writeObject(p_Word);
    out.writeObject(p_HasPunct);
    out.writeObject(p_Punct);
    out.writeObject(Y_Cats);
    out.writeObject(rule_type);
    out.writeLong(LEX);
    out.writeLong(UNARY);
    out.writeLong(LEFT);
    out.writeLong(RIGHT);
    out.writeBoolean(lexicalized);
    out.writeBoolean(lexicalTransition);

    out.writeObject(InducedCAT.punc);

    out.writeLong(TRUE);
    out.writeLong(FALSE);
    out.writeLong(NONE);
    out.writeLong(FW);
    out.writeLong(BW);
    out.writeObject(LEFT_array);
    out.writeObject(RIGHT_array);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    super.readExternal(in);
    this.p_Comb = (PYDistribution) in.readObject();
    this.p_Tag = (PYDistribution) in.readObject();
    this.p_Arg = (PYDistribution) in.readObject();
    this.p_Type = (PYDistribution) in.readObject();
    this.p_Word = (PYDistribution) in.readObject();
    this.p_HasPunct = (PYDistribution) in.readObject();
    this.p_Punct = (PYDistribution) in.readObject();
    this.Distributions.add(p_Comb);
    this.p_Comb.model = this;
    this.Distributions.add(p_Tag);
    this.p_Tag.model = this;
    this.Distributions.add(p_Arg);
    this.p_Arg.model = this;
    this.Distributions.add(p_Type);
    this.p_Type.model = this;
    this.Distributions.add(p_Word);
    this.p_Word.model = this;
    this.Distributions.add(p_Punct);
    this.p_Punct.model = this;
    this.Distributions.add(p_HasPunct);
    this.p_HasPunct.model = this;

    this.Y_Cats = (ConcurrentHashMap<Rule, Long>) in.readObject();
    this.rule_type = (ConcurrentHashMap<Rule_Type, Long>) in.readObject();
    ArgumentModel.LEX = in.readLong();
    ArgumentModel.UNARY = in.readLong();
    ArgumentModel.LEFT = in.readLong();
    ArgumentModel.RIGHT = in.readLong();
    ArgumentModel.lexicalized = in.readBoolean();
    ArgumentModel.lexicalTransition = in.readBoolean();

    InducedCAT.punc = (CCGAtomic[]) in.readObject();

    TRUE = in.readLong();
    FALSE = in.readLong();
    NONE = in.readLong();
    FW = in.readLong();
    BW = in.readLong();
    LEFT_array = (CondOutcomePair)in.readObject();
    RIGHT_array = (CondOutcomePair)in.readObject();
  }

}
