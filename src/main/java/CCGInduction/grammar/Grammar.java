package CCGInduction.grammar;

import CCGInduction.experiments.Action;
import CCGInduction.Configuration;
import CCGInduction.ccg.CCGAtomic;
import CCGInduction.ccg.Direction;
import CCGInduction.ccg.InducedCAT;
import CCGInduction.data.POS;
import CCGInduction.data.Tagset;
import CCGInduction.parser.Cell;
import CCGInduction.parser.ChartItem;
import CCGInduction.parser.Parser;
import CCGInduction.parser.Punctuation;
import CCGInduction.utils.Hash;
import CCGInduction.utils.IntPair;
import CCGInduction.utils.Logger;
import CCGInduction.utils.Math.Log;
import CCGInduction.utils.TextFile;

import java.io.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Defines an abstract grammar which must be extended to actually create rules
 *
 * @author bisk1
 */
public class Grammar implements Serializable {
  private static final long serialVersionUID = 5162012L;

  /**
   * Indicates if the grammar is ready for use
   */
  public boolean initialized = false;
  /**
   * Set of grammar rules
   */
  public final ConcurrentHashMap<Rule, Boolean> requiredRules = new ConcurrentHashMap<>();

  /**
   * Const for TOP
   */
  public long TOP;

  /**
   * Set of lexical categories that can generate a given POS tag
   */
  public final ConcurrentHashMap<POS, ConcurrentHashMap<InducedCAT, Boolean>> LexCats = new ConcurrentHashMap<>();
  /**
   * Set of new lexical categories we have induced but not added
   */
  public final ConcurrentHashMap<POS, ConcurrentHashMap<InducedCAT, Boolean>> newLexCats = new ConcurrentHashMap<>();

  /**
   * Newly introduced Forward Type Raised categories
   */
  private final ConcurrentHashMap<InducedCAT, Boolean> typeRaisedFW = new ConcurrentHashMap<>();
  /**
   * Newly introduced Backward Type Raised categories
   */
  private final ConcurrentHashMap<InducedCAT, Boolean> typeRaisedBW = new ConcurrentHashMap<>();

  /**
   * Is this the first pass at induction
   */
  public boolean firstPrepare = true;

  /** Constant indicating a category tuple with inducedCat S */
  public long S_NT;
  /** Constant indicating a category tuple with inducedCat P */
  public long P_NT;
  /** Constant indicating a category tuple with inducedCat N */
  private long N_NT;
  /** Constant indicating a category tuple with inducedCat M : Chinese*/
  private long M_NT;
  /** Constant indicating a category tuple with inducedCat QP : Chinese*/
  private long QP_NT;
  /** Constant indicating a category tuple with inducedCat conj */
  private long conj_NT;

  /*
   * Rules
   */
  /**
   * Cache of a unary rule
   */
  public final ConcurrentHashMap<IntPair, valid> unaryCheck = new ConcurrentHashMap<>();
  /**
   * Caches if a pair of categories can combine
   */
  public final ConcurrentHashMap<IntPair, valid> combinationCheck = new ConcurrentHashMap<>();

  /**
   * Rules used ( model specific definition of "use" )
   */
  public final ConcurrentHashMap<IntPair, ConcurrentHashMap<Rule,Boolean>> Rules = new ConcurrentHashMap<>();

  public final ConcurrentHashMap<Long, Boolean> learnedWords = new ConcurrentHashMap<>();

  public Grammar() {}

  Grammar(Grammar other) {
    initialized = other.initialized;
    requiredRules.putAll(other.requiredRules);
    Words.putAll(other.Words);
    TOP = other.TOP;
    Categories.putAll(other.Categories);
    iCategories.putAll(other.iCategories);

    // Must do each individually for copy by value
    for (IntPair pair : other.Rules.keySet())
      Rules.put(pair, new ConcurrentHashMap<>(other.Rules.get(pair)));
    for (POS pos : other.LexCats.keySet())
      LexCats.put(pos, new ConcurrentHashMap<>(other.LexCats.get(pos)));
    for (POS pos : other.newLexCats.keySet())
      newLexCats.put(pos, new ConcurrentHashMap<>(other.newLexCats.get(pos)));

    typeRaisedFW.putAll(other.typeRaisedFW);
    typeRaisedBW.putAll(other.typeRaisedBW);
    unaryCheck.putAll(other.unaryCheck);
    combinationCheck.putAll(other.combinationCheck);
    firstPrepare = other.firstPrepare;
    S_NT = other.S_NT;
    P_NT = other.P_NT;
    N_NT = other.N_NT;
    conj_NT = other.conj_NT;
    learnedWords.putAll(other.learnedWords);
  }

  public Grammar copy() {
    return new Grammar(this);
  }

  /**
   * Set of Induced Categories
   */
  public final ConcurrentHashMap<Long,InducedCAT> Categories = new ConcurrentHashMap<>();
  /**
   * Mapping of Induced Categories to Integers
   */
  public final ConcurrentHashMap<InducedCAT, Long> iCategories = new ConcurrentHashMap<>();
  /**
   * Words that can be produced by the grammar or the model
   */
  public final ConcurrentHashMap<Long,String> Words = new ConcurrentHashMap<>();

  /**
   * Maps a lexical string (word) to an integer
   *
   * @param word word
   * @return ID
   */
  public long Lex(String word) {
    long hash_val = Hash.hash(word);
    if (Words.containsKey(hash_val) && !Words.get(hash_val).equals(word)) {
      throw new AssertionError("Hash Conflict: " + Words.get(hash_val) + "\t" + word);
    }
    if (Categories.containsKey(hash_val)) {
      throw new AssertionError("Hash Conflict\t" + word + "\t" + Categories.get(hash_val));
    }
    Words.put(hash_val, word);
    return hash_val;
  }

  /**
   * Checks that grammar has rules and has been initialized
   * @return Grammar has been initialized
   */
  public final boolean isEmpty() {
    return !initialized;
  }

  /**
   * If rule is new, add to datastructures, else return instance
   *
   * @param rule Rule to add
   * @param BC Pair of child categories
   * @return Rule
   */
  final Rule addRuleIfAbsent(Rule rule, IntPair BC) {
    if (Rules.get(BC) == null)
      Rules.putIfAbsent(BC, new ConcurrentHashMap<>());
    Rules.get(BC).put(rule, true);
    if (rule.N == 1) {
      unaryCheck.putIfAbsent(new IntPair(rule.A, rule.B), valid.Unused);
    }
    return rule;
  }
  final Rule addRule(Rule rule, IntPair BC) {
    if (Rules.get(BC) == null)
      Rules.putIfAbsent(BC, new ConcurrentHashMap<>());
    Rules.get(BC).put(rule, true);
    if (rule.N == 1) {
      unaryCheck.put(new IntPair(rule.A, rule.B), valid.Unused);
    }
    return rule;
  }

  public Unary getRule(long a, long b, Rule_Type type, boolean create) {
    Unary temp = new Unary(a, b, type);
    IntPair single = new IntPair(b);

    if (Rules.containsKey(single) && Rules.get(single).containsKey(temp)) {
      return temp;
    }

    if (create) {
      return createRule(a, b, type);
    }

    return null;
  }

  private static final Set<Rule> empty = new HashSet<>();
  public Set<Rule> getRules(long lex_cat) {
    return getRules(new IntPair(lex_cat));
  }

  Set<Rule> getRules(long b, long c) {
    return getRules(new IntPair(b, c));
  }

  public Set<Rule> getRules(IntPair BC) {
    return Rules.containsKey(BC) ? Rules.get(BC).keySet() : empty;
  }

  public static <G extends Grammar> ChartItem<G> LexChartItem(Unary rule, Cell<G> cell) {
    // An actual category
    ChartItem<G> b = new ChartItem<>(rule.B, Rule_Type.LEX, 0, cell);
    b.alphaInit();

    addLexTree(b, new ChartItem.bp_ij<>(Log.ONE, null, 0, 0));

    ChartItem<G> a = new ChartItem<>(rule.A, rule.Type, 0, cell);
    a.parses = 1;
    a.addChild(rule, b, null);
    return a;
  }

  public static <G extends Grammar> void addLexTree(ChartItem<G> b, ChartItem.bp_ij<G> L) {
    for (ChartItem.bp_ij<G> T : b.topK) {
      if (T.equals(L)) {
        return;
      }
    }
    b.topK.add(L);
  }

  public valid unaryCheck(long a, long b) {
    IntPair ab = new IntPair(a, b);
    if (!unaryCheck.containsKey(ab)) {
      return valid.Unknown;
    }
    return unaryCheck.get(ab);
  }

  /**
   * Checks if we know how to combine the given categories.
   *
   * @param leftCategoryID Left child
   * @param rightCategoryID Right child
   * @return [Unused, Valid, Invalid, Unknown]
   */
  public final valid combine(long leftCategoryID, long rightCategoryID) {
    valid v;
    if ((v = combinationCheck.get(new IntPair(leftCategoryID, rightCategoryID))) != null) {
      return v;
    }
    return valid.Unknown;
  }

  /**
   * Prints the grammar (one rule per line) to a file.  Marking if the rule was
   * used during training (+) or omitted (-).
   * @param file_name Target file
   * @throws UnsupportedEncodingException
   * @throws FileNotFoundException
   * @throws IOException
   */
  public void print(String file_name) throws IOException {
    if (!Configuration.printModelsVerbose) return;
    Logger.timestamp("Printing Dists");
    BufferedWriter writer = TextFile.Writer(Configuration.Folder + "/" + file_name);
    for (IntPair BC : Rules.keySet()) {
      for (Rule r : Rules.get(BC).keySet()) {
        if (requiredRules.containsKey(r) && requiredRules.get(r)) {
          writer.write("+    ");
        } else {
          writer.write("-    ");
        }
        writer.write(r.toString(this));
        writer.write("\n");
      }
    }
    writer.close();
  }

  /**
   * Creates seed knowledge necessary for induction. The definition of
   * primitives and the production of basic classes
   */
  public void init() {
    // Initial
    for (POS tag : Tagset.tags()) {
      LexCats.put(tag, new ConcurrentHashMap<>());
      newLexCats.put(tag, new ConcurrentHashMap<>());
    }

    InducedCAT TOP_CCG = new InducedCAT(InducedCAT.TOP);
    TOP = NT(TOP_CCG);

    InducedCAT N = new InducedCAT(InducedCAT.N);
    InducedCAT S = new InducedCAT(InducedCAT.S);
    InducedCAT P = new InducedCAT(InducedCAT.PP);
    InducedCAT M = new InducedCAT(InducedCAT.M);
    InducedCAT QP = new InducedCAT(InducedCAT.QP);
    InducedCAT conj = new InducedCAT(InducedCAT.conj);
    N_NT = NT(N);
    S_NT = NT(S);
    P_NT = NT(P);
    M_NT = NT(M);
    QP_NT = NT(QP);
    conj_NT = NT(conj);
    // Include ``prior knowledge"
    for (POS t : Tagset.noun()) {
      createRule(N_NT, Lex(t.toString()), Rule_Type.PRODUCTION);
    }
    for (POS t : Tagset.QP()) {
      createRule(QP_NT, Lex(t.toString()), Rule_Type.PRODUCTION);
    }
    for (POS t : Tagset.M()) {
      createRule(M_NT, Lex(t.toString()), Rule_Type.PRODUCTION);
    }

    for (POS t : Tagset.verb()) {
      createRule(S_NT, Lex(t.toString()), Rule_Type.PRODUCTION);
    }

    for (POS t : Tagset.prep) {
      createRule(P_NT, Lex(t.toString()), Rule_Type.PRODUCTION);
    }

    for (POS t : Tagset.conj) {
      createRule(conj_NT, Lex(t.toString()), Rule_Type.PRODUCTION);
    }

    for (POS t : Tagset.punct) {
      CCGAtomic atomic_p = new CCGAtomic(t.toString());
      createRule(NT(new InducedCAT(atomic_p)),
          Lex(t.toString()), Rule_Type.PRODUCTION);
      InducedCAT.punc = InducedCAT.add(InducedCAT.punc, atomic_p);
    }

    createRule(TOP, N_NT, Rule_Type.TYPE_TOP);
    createRule(TOP, S_NT, Rule_Type.TYPE_TOP);
    createRule(TOP, NT(S.forward(N)), Rule_Type.TYPE_TOP);
    createRule(TOP, NT(S.backward(N)), Rule_Type.TYPE_TOP);

    for (IntPair BC : Rules.keySet()) {
      for (Rule r : Rules.get(BC).keySet()) {
        unaryCheck.put(new IntPair(r.A, r.B), valid.Unused);
      }
    }

    initialized = true;
  }

  /**
   * Creates a rule a -> b c w/ type,dir,arity
   *
   * @param parent      Parent
   * @param leftChild   L child
   * @param rightChild  R child
   * @param type        Type
   * @param head         Head Direction
   * @param rule_arity  arity
   * @return Binary
   */
  protected Binary createRule(long parent, long leftChild, long rightChild, Rule_Type type,
                              Rule_Direction head, int rule_arity) {

    Binary temp = new Binary(parent, leftChild, rightChild, type, rule_arity, head);

    return (Binary) addRuleIfAbsent(temp, new IntPair(leftChild, rightChild));
  }

  /**
   * Creates a rule a -> b w/ type
   *
   * @param parent  Parent
   * @param child   Child
   * @param type    Type
   * @return Unary
   */
  public Unary createRule(long parent, long child, Rule_Type type) {
    if (type == Rule_Type.TYPE_TOP) {
      InducedCAT ccgCat = Categories.get(child);
      if (ccgCat.D.equals(Direction.None) && !InducedCAT.TOP(ccgCat.atom)) {
        return null;
      }
    }

    Unary temp = new Unary(parent, child, type);

    return (Unary) addRuleIfAbsent(temp, new IntPair(child));
  }

  public Unary createSupervisedRule(long parent, long child, Rule_Type type) {
    Unary temp = new Unary(parent, child, type);

    return (Unary) addRule(temp, new IntPair(child));
  }

  /**
   * Maps InducedCATs to integers
   *
   * @param categoryToHash New category
   * @return Category's hash
   */
  public long NT(InducedCAT categoryToHash) {
    long hash_val = categoryToHash.hashCode();
    InducedCAT conflict;
    if ((conflict = Categories.get(hash_val)) != null)  {
      if(!conflict.equals(categoryToHash)) {
        if (conflict.toString().equals(categoryToHash.toString())) {
          System.err.println("Strings match");
          System.err.println(conflict + "\t" + categoryToHash);
          System.err.println(conflict.Res + "\t" + conflict.D + "\t" + conflict.Arg + "\t" + conflict.arity + "\t" + conflict.modifier + "\t" + conflict.has_conj + "\t" + conflict.atom);
          System.err.println(categoryToHash.Res + "\t" + categoryToHash.D + "\t" + categoryToHash.Arg + "\t" + categoryToHash.arity + "\t" + categoryToHash.modifier + "\t" + categoryToHash.has_conj + "\t" + categoryToHash.atom);
        }
        throw new AssertionError("Hash Conflict: " + conflict + "\t" + categoryToHash
            + "\t" + hash_val + "\t" + conflict.hashCode() + "\t" + categoryToHash.hashCode());
      }
      // If equal, return
      return hash_val;
    } else if (Words.containsKey(hash_val)) {
      throw new AssertionError("Hash Conflict\t" + categoryToHash + "\t" + Words.get(hash_val));
    } else {
      Categories.putIfAbsent(hash_val, categoryToHash.copy());
      iCategories.putIfAbsent(categoryToHash.copy(), hash_val);
    }
    return hash_val;
  }

  /**
   * Makes sure that we have categories constructed and hashes present for later lookup for the components of the
   * category in addition to the category itself
   */
  public long NTRecursively(InducedCAT categoryToHash) {
    if (categoryToHash.Arg != null){
      NTRecursively(categoryToHash.Arg);
      NTRecursively(categoryToHash.Res);
    }
    return NT(categoryToHash);
  }

  /**
   * Cat to string
   * @param category Category ID
   * @return String representation
   */
  public String prettyCat(long category) {
    if (Words.containsKey(category))
      return Words.get(category);
    InducedCAT cat = Categories.get(category);
    return cat == null ? null : cat.toString();
  }

  /**
   * Given a set of newLexCats, we create actual production rules and merge
   * the new lexical categories into LexCats.
   */
  public void incorporateNewRules() {
    int localcount = 0;
    long tag_int;
    HashSet<InducedCAT> LexicalCategories = new HashSet<>();
    for (POS tag : Tagset.tags()) {
      tag_int = Lex(tag.toString());
      if(newLexCats.containsKey(tag)) {   // New punctuation marks won't be in here
        for (InducedCAT c : newLexCats.get(tag).keySet()) {
          LexicalCategories.add(c);
          if (Tagset.Punct(tag) || GrammarInductionUtils.validCategory(c)) {
            createSupervisedRule(NT(c), tag_int, Rule_Type.PRODUCTION);
            LexCats.get(tag).put(c, true);
            localcount += 1;
          }
        }
        newLexCats.get(tag).clear();
      } else {
        newLexCats.put(tag, new ConcurrentHashMap<>());
        LexCats.put(tag, new ConcurrentHashMap<>());
      }
    }

    createTypeRaisedCategories(LexicalCategories);

    long NT_N = NT(new InducedCAT(InducedCAT.N));
    for (InducedCAT tr : typeRaisedFW.keySet()) {
      createRule(NT(tr), NT_N, Rule_Type.FW_TYPERAISE);
    }
    typeRaisedFW.clear();
    for (InducedCAT tr : typeRaisedBW.keySet()) {
      createRule(NT(tr), NT_N, Rule_Type.BW_TYPERAISE);
    }
    typeRaisedBW.clear();
    if (localcount == 0) {
      throw new GrammarException("We didn't induce any new categories in this round.");
    }
    Logger.logln("\rInduced Rules:", Integer.toString(localcount));
  }

  /**
   * Generated TypeRaised Categories.  Checks if T is in existing Lexical Categories
   * @param LexCats  Existing lexical categories.
   */
  public final void createTypeRaisedCategories(HashSet<InducedCAT> LexCats) {
    if(Configuration.typeRaising){
      InducedCAT N = new InducedCAT(InducedCAT.N);
      long NT_N = NT(N);
      InducedCAT S = new InducedCAT(InducedCAT.S);

      //  S/(S\N) --> N
      InducedCAT FW = S.forward(S.backward(N));
      if(LexCats.contains(S.backward(N))) {
        createRule(NT(FW), NT_N, Rule_Type.FW_TYPERAISE);
      }
      //  S\(S/N) --> N
      InducedCAT BW = S.backward(S.forward(N));
      if(LexCats.contains(S.forward(N))) {
        createRule(NT(BW), NT_N, Rule_Type.BW_TYPERAISE);
      }
    }
  }

  /**
   * Performs the parsing action in Cell A of combining categories from leftCategoryChartItems and rightCategoryChartItems
   * if possible
   *  @param cell Cell to combine into
   * @param leftCategoryID Left child
   * @param leftCategoryChartItems Instances of Left Child
   * @param rightCategoryID Right child
   * @param rightCategoryChartItems Instance of Right Child
   * @param Test Are we parsing at test time
   * @param parse_action Parsing power
   */
  public <G extends Grammar> void combine(Cell<G> cell,
                                          final long leftCategoryID, final Collection<ChartItem<G>> leftCategoryChartItems,
                                          final long rightCategoryID, final Collection<ChartItem<G>> rightCategoryChartItems,
                                          boolean Test, Action parse_action) {
    if (leftCategoryChartItems == null || rightCategoryChartItems == null) {
      return;
    }
    //Binary r;
    for (Rule rule : getRules(leftCategoryID, rightCategoryID)) {
      Binary r = (Binary) rule;
      if (!Configuration.hardEntityNConstraints
          || !cell.chart.fullEntity(cell.X, cell.Y)
          || InducedCAT.N(Categories.get(r.A))) {
        for (ChartItem<G> b_cat : leftCategoryChartItems) {
          rightCategoryChartItems.stream()
              .filter(c_cat -> !Test || requiredRules.containsKey(rule))
              .filter(c_cat ->
                  LimitParsingPower(parse_action, b_cat.type(), Categories.get(b_cat.Category).modifier, c_cat.type(),
                      Categories.get(c_cat.Category).modifier, r.Type)
                  && NF.binaryNF(r, b_cat, c_cat)
                  && (parse_action == Action.SupervisedTest || MaxProjectionPunctuation(r, b_cat, c_cat)))
              .forEach(c_cat -> {ChartItem<G> newC = cell.addCat(r, b_cat, c_cat);
                if (newC != null && !Test) {
                  newC.iCAT = Categories.get(r.A).copy();
                }
              });
        }
      }
    }
  }

  /**
   * @return if the chosen parsing power allows for a specific rule to be applied
   */
  private static boolean LimitParsingPower(Action action, Rule_Type leftHist, boolean leftMod,
                                           Rule_Type rightHist, boolean rightMod, Rule_Type ruleType) {
    switch (ruleType) {
      case FW_APPLY:
        return !Rule_Type.TR(leftHist);
      case BW_APPLY:
        return !Rule_Type.TR(rightHist);
      case FW_COMPOSE:
      case FW_XCOMPOSE:
      case FW_2_COMPOSE:
      case FW_3_COMPOSE:
      case FW_4_COMPOSE:
        switch (action) {
          case B1ModTR:
          case B2ModTR:
          case B3Mod_B2TR_B0Else:
            // Primary functor must be either a modifier or type-raised
            if (!leftMod && !Rule_Type.TR(leftHist)) {
              return false;
            }
            if (rightMod)
              return false;
            break;
        }
        return true;
      case BW_COMPOSE:
      case BW_XCOMPOSE:
      case BW_2_COMPOSE:
      case BW_3_COMPOSE:
      case BW_4_COMPOSE:
        switch (action) {
          case B1ModTR:
          case B2ModTR:
          case B3Mod_B2TR_B0Else:
            // Primary functor must be either a modifier or type-raised
            if (!rightMod && !Rule_Type.TR(rightHist)) {
              return false;
            }
            if (leftMod)
              return false;
            break;
        }
        return true;
    }
    return true;
  }

  private boolean MaxProjectionPunctuation(Binary rule, ChartItem left, ChartItem right) {
    // Can only attach FW after another FW punctuation or to a maximal projection
    if (rule.Type == Rule_Type.FW_PUNCT) {
      return right.punc() != Punctuation.BW;
    }

    // Can only attach BW to Punctuation or MaxProj
    if (rule.Type == Rule_Type.BW_PUNCT) {
      return true;
    }

    if (left.punc() == Punctuation.None && right.punc() == Punctuation.None)
      return true;

    if (left.punc() != Punctuation.None) {
      if ((rule.head == Rule_Direction.Right && !Rule_Type.TR(right.type()))
          || (rule.head == Rule_Direction.Left && Rule_Type.TR(left.type())))
        return true;
    }
    if (right.punc() != Punctuation.None) {
      if ((rule.head == Rule_Direction.Left && !Rule_Type.TR(left.type()))
          || (rule.head == Rule_Direction.Right && Rule_Type.TR(right.type())))
        return true;
    }
    return false;
  }


  /**
   * Creates a binary rule from InducedCATs
   *
   * @param ac Parent category
   * @param bc Left child category
   * @param cc Right child category
   * @param type Rule Type
   * @param rule_arity Rule's arity
   * @return Binary rule
   */
  public Binary createRule(InducedCAT ac, InducedCAT bc, InducedCAT cc, Rule_Type type, int rule_arity) {
    Rule_Direction head;
    switch (type) {
      case FW_APPLY:
        if (bc.modifier || AUX(bc)) {
          head = Rule_Direction.Right;
          ac.modifier = cc.modifier;
        } else {
          head = Rule_Direction.Left;
          ac.modifier = bc.Res.modifier;
        }
        break;
      case FW_COMPOSE:
      case FW_2_COMPOSE:
      case FW_3_COMPOSE:
      case FW_4_COMPOSE:
      case FW_5_COMPOSE:
      case FW_XCOMPOSE:
        ac.modifier = bc.modifier && cc.modifier;
        if (bc.modifier) {
          head = Rule_Direction.Right;
        } else {
          head = Rule_Direction.Left;
        }
        break;
      case BW_CONJOIN:
        ac.modifier = bc.modifier && cc.modifier;
        head = Rule_Direction.Left;
        break;
      case BW_PUNCT:
        ac.modifier = bc.modifier;
        head = Rule_Direction.Left;
        break;
      case BW_APPLY:
        if (cc.modifier || AUX(cc)) {
          head = Rule_Direction.Left;
          ac.modifier = bc.modifier;
        } else {
          head = Rule_Direction.Right;
          ac.modifier = cc.Res.modifier;
        }
        break;
      case BW_COMPOSE:
      case BW_2_COMPOSE:
      case BW_3_COMPOSE:
      case BW_4_COMPOSE:
      case BW_5_COMPOSE:
      case BW_XCOMPOSE:
        ac.modifier = bc.modifier && cc.modifier;
        if (cc.modifier) {
          head = Rule_Direction.Left;
        } else {
          head = Rule_Direction.Right;
        }
        break;
      case FW_CONJOIN:
        ac.modifier = cc.modifier;
        head = Rule_Direction.Right;
        break;
      case FW_PUNCT:
        head = Rule_Direction.Right;
        ac.modifier = cc.modifier;
        break;
      case FW_TYPECHANGE:
        head = Rule_Direction.Right;
        break;
      case BW_TYPECHANGE:
        head = Rule_Direction.Left;
        break;
      case FW_SUBSTITUTION:
        head = Rule_Direction.Left;
        break;
      default:
        throw new Parser.FailedParsingAssertion("Invalid: " + type);
    }

    return createRule(NT(ac), NT(bc), NT(cc), type, head, rule_arity);
  }

  private boolean AUX(InducedCAT cat) {
    return cat.Arg != null && cat.Arg.equals(cat.Res) && cat.Arg.Arg != null && InducedCAT.N(cat.Arg.Arg) && InducedCAT.S(cat.Arg.Res);
  }

  /**
   * Incorporates all the data-structures from a copy of the grammmar.  Prseumably one
   * that resided in a thread
   * @param local Grammar copy
   */
  public void merge(Grammar local) {
    requiredRules.putAll(local.requiredRules);
    Words.putAll(local.Words);
    Categories.putAll(local.Categories);
    iCategories.putAll(local.iCategories);
    for (POS tag : local.LexCats.keySet()) {
      if (!LexCats.containsKey(tag))
        LexCats.putIfAbsent(tag, new ConcurrentHashMap<>());
      LexCats.get(tag).putAll(local.LexCats.get(tag));
    }
    for (POS tag : local.newLexCats.keySet()) {
      if (!newLexCats.containsKey(tag))
        newLexCats.putIfAbsent(tag, new ConcurrentHashMap<>());
      newLexCats.get(tag).putAll(local.newLexCats.get(tag));
    }
    typeRaisedBW.putAll(local.typeRaisedBW);
    typeRaisedFW.putAll(local.typeRaisedFW);
    for (IntPair pair : local.unaryCheck.keySet()) {
      unaryCheck.put(pair, valid.max(local.unaryCheck.get(pair), unaryCheck.get(pair)));
    }
    for (IntPair pair : local.combinationCheck.keySet()) {
      combinationCheck.put(pair, valid.max(local.combinationCheck.get(pair), combinationCheck.get(pair)));
    }

    Rules.putAll(local.Rules);
  }

  static public class GrammarException extends AssertionError {
    public GrammarException(String s) {
      super(s);
    }
  }
}