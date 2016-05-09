package CCGInduction.parser;

import CCGInduction.Configuration;
import CCGInduction.ccg.*;
import CCGInduction.data.JSON.CoNLLDep;
import CCGInduction.data.JSON.JSONFormat;
import CCGInduction.data.JSON.PARGDep;
import CCGInduction.data.JSON.SynParObj;
import CCGInduction.data.LexicalToken;
import CCGInduction.data.Sentence;
import CCGInduction.data.Tagset;
import CCGInduction.grammar.*;
import CCGInduction.utils.IntPair;
import CCGInduction.utils.Logger;
import CCGInduction.utils.Math.Log;
import CCGInduction.data.POS;
import CCGInduction.learning.CountsArray;
import CCGInduction.models.Model;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.regex.Pattern;

/**
 * @author bisk1 A CYK Parse chart
 * @param <G>
 */
public abstract strictfp class Chart<G extends Grammar> implements Externalizable {
  private static final long serialVersionUID = 11122010;
  private static final Pattern backslashPeriod = Pattern.compile("\\\\\\.");
  private static final Pattern forwardslashPeriod = Pattern.compile("/\\.");
  private static final Pattern TeXbackslash = Pattern.compile("\\\\");

  /**
   * Actual 2-D Array of Cells
   */
  public Cell<G>[][] chart;
  /**
   * Underlying sentence
   */
  public Sentence sentence;
  public ChartItem<G> TOP;
  /**
   * Number of parses in the forest
   */
  public double parses = 0;
  /**
   * Fast access to underlying tag sequence
   */
  public long[] tags;
  /**
   * Fast access to underlying word sequence
   */
  public long[] words;
  /**
   * Fast access to KG entity matches
   */
  private boolean[][] crossingEntity;
  /**
   * Fast access for KG entity full spans
   */
  private boolean[][] Entity;
  /**
   * Specifies which constituents are valid (allowed to be filled), as a
   * function of punctuation.  Used during parsing only
   */
  transient private boolean[][] disallowed_constituents;
  private int length;
  /**
   * Reference to the model
   */
  public Model<G> model;
  /**
   * Unique id
   */
  int id; //TODO(bisk1): Fill
  /**
   * Chart's likelihood
   */
  public transient double likelihood = Log.ZERO;
  /**
   * Pointer to counts accumulated from parsing
   */
  public transient CountsArray priorCounts;

  /**
   * Default constructor
   */
  public Chart() {}

  public Chart(Sentence base_sentence, Model<G> global_model) {
    this.id = base_sentence.id;
    this.model = global_model;
    this.sentence = base_sentence;
    if (!Configuration.ignorePunctuation) {
      this.disallowed_constituents = new boolean[sentence.sentence_wP.length][sentence.sentence_wP.length];
      computeSpans();
    }
    // Precompute int values for tags and words
    this.tags = new long[this.sentence.length()];
    this.words = new long[this.sentence.length()];
    this.length = this.sentence.length();
    for (int i = 0; i < tags.length; i++) {
      tags[i] = this.model.grammar.Lex(sentence.get(i).tag().toString());
      words[i] = this.sentence.get(i).wordOrTag(this.model.grammar.learnedWords, model.grammar);
    }
    this.crossingEntity = new boolean[length][length];
    this.Entity = new boolean[length][length];
    ArrayList<IntPair> entities = new ArrayList<>();
    // Otherwise, check for Freebase annotation on words
    String previousToken = "";
    String FBid;
    int i = 0;
    int x = -1;
    for(LexicalToken lt : this.sentence) {
      FBid = lt.FBid();
      // Start of an entity
      if(previousToken.length() == 0 && FBid.length() != 0) {
        x = i;
        // Guaranteed ending
      } else if (previousToken.length() != 0 && FBid.length() == 0) {
        entities.add(new IntPair(x,i-1));
        x = i;
        // Two back to back entities
      } else if (previousToken.length() != 0 && FBid.length() != 0
          && !previousToken.equals(FBid)) {
        entities.add(new IntPair(x,i-1));
        x = i;
      }
      previousToken = FBid;
      ++i;
    }
      // Catch sentence final
      if(previousToken.length() != 0) {
        entities.add(new IntPair(x,i-1));
      }

    // build boolean[][]
    crossingBrackets(entities);
    entitySpan(entities);
  }

  abstract void getLex(int i, boolean Test);

  /**
   * Whether underlying sentence was successfully parsed (i.e. TOP exists )
   * @return
   *  Whether chart was successfully parsed
   */
  public boolean success() { return TOP != null; }

  /**
   * Empties unsuccessful charts. Performs coarse-to-fine or context building
   * for successul parses while removing unused constituents
   */
  public void cleanForest(boolean test) {
    if (success()) {
      trim(TOP);
      cleanChart();
    } else {
      chart = null;
    }
  }

  final void cleanChart() {
    for (int s = 0; s < chart.length; s++) {
      for (int i = 0; i < chart.length - s; i++) {
        if(chart[i][i+s] != null)
          chart[i][i + s].removeUnusedCats();
      }
    }
  }

  public final void trim(ChartItem<G> a) {
    if (a.used) {
      throw new AssertionError("Should not be re-entering this chartitem");
    }
    a.used = true;
    if (this.model.createFine) {
      a.FineGrained = new HashSet<>();
    }

    for (BackPointer<G> bp : a.children) {
      model.grammar.requiredRules.put(bp.rule, true);

      if (bp.isUnary()) {
        if (!bp.leftChild().used) {
          trim(bp.leftChild());
        }

        model.buildUnaryContext(a, bp); // If context can be built
      } else {

        if (!bp.leftChild().used) {
          trim(bp.leftChild());
        }
        if (!bp.rightChild().used) {
          trim(bp.rightChild());
        }

        model.buildBinaryContext(a, bp);
      }
    }
  }

  /**
   * Compute PARG style CCG dependencies for a given Tree
   * @param tree Tree to traverse
   * @return PARG dependencies
   */
  public static PARGDep[] CCGdependencies(Tree<?> tree, Sentence sentence) {
    return printCCGDepRel(dpRecurse(tree, sentence.length()), sentence);
  }

  public static DepRel[][] dpRecurse(Tree<?> a, int length) {
    a.depRel = new DepRel[length][length];
    if (a.rightChild == null && a.leftChild != null) {
      DepRel.copyDepRel(a.depRel, dpRecurse(a.leftChild, length));
    } else if (a.rightChild != null && a.leftChild != null) {
      // Left Traverse
      DepRel.copyDepRel(a.depRel, dpRecurse(a.leftChild, length));
      // Right Traverse
      DepRel.copyDepRel(a.depRel, dpRecurse(a.rightChild, length));
    }

    if (a.ccgcat != null) {
      DepList filled = a.ccgcat.filledDependencies();
      if (filled != null) {
        while (filled != null) {
          DepRel.addDependency(a.depRel, filled);
          filled = filled.next();
        }
      }
    }
    return a.depRel;
  }

  /**
   * Compute CoNLL style dependencies for a given tree
   * @param tree  Tree to traverse
   * @param sentence Sentence of tree
   * @return  CoNLL dependencies
   */
  public static CoNLLDep[] CoNLLdependencies(Tree<?> tree, Sentence sentence) {
    DepRel[][] depr = dpRecurse(tree, sentence.length());
    tree.computeCoNLLHeads();
    return printCoNLLDepRel(depr, tree.headIndex, sentence);
  }


  private static int offset(Sentence sentence, int i) {
    if (!Configuration.ignorePunctuation) {
      return i;
    }
    int seen = -1;
    int j = 0;
    for (; j < sentence.sentence_wP.length; j++) {
      if (!Tagset.Punct(sentence.sentence_wP[j].tag())) {
        seen += 1;
      }
      if (seen == i) {
        return j;
      }
    }
    return j;
  }

  private static CoNLLDep[] insertPunctuation(CoNLLDep[] dependencies, Sentence sentence, int i) {
    if (Configuration.ignorePunctuation) {
      if (i == -1) {
        for (int j = 0; j < sentence.sentence_wP.length; j++) {
          if (Tagset.Punct(sentence.sentence_wP[j].tag())) {
            dependencies[j] = new CoNLLDep(j+1, j+1, "PUNC");
          } else {
            return dependencies;
          }
        }
      } else if (offset(sentence, i) + 1 != offset(sentence, i + 1)) {
        int max = offset(sentence, i + 1);
        for (int j = offset(sentence, i) + 1; j < max; j++) {
          dependencies[j] = new CoNLLDep(j+1, j+1, "PUNC");
        }
      }
    }
    return dependencies;
  }

  public static PARGDep[] printCCGDepRel(DepRel[][] depRel, Sentence sentence) {
    PARGDep[] dependencies = new PARGDep[0];
    if (depRel != null) {
      for (int i = 0; i < depRel.length; i++) {  // was chart.length
        for (int j = 0; j < depRel.length; j++) { // was chart.length
          if (depRel[i][j] != null) {
            if (depRel[i][j].extracted) {
              if (depRel[i][j].bounded) {
                dependencies = Arrays.copyOf(dependencies, dependencies.length + 1);
                dependencies[dependencies.length-1] =
                    new PARGDep(depRel[i][j].cat, depRel[i][j].slot, offset(sentence,i), offset(sentence,j), "<XB>");
                // arg, head, cat, slot, argWord, headWord
              }
              if (!(depRel[i][j].bounded)) {
                dependencies = Arrays.copyOf(dependencies, dependencies.length + 1);
                dependencies[dependencies.length-1] =
                    new PARGDep(depRel[i][j].cat, depRel[i][j].slot, offset(sentence,i), offset(sentence,j), "<XU>");
                // arg, head, cat, slot, argWord, headWord
              }
            } else {
              dependencies = Arrays.copyOf(dependencies, dependencies.length + 1);
              dependencies[dependencies.length-1] =
                  new PARGDep(depRel[i][j].cat, depRel[i][j].slot, offset(sentence,i), offset(sentence,j), null);
            }
          }
        }
      }
    }
    return dependencies;
  }

  public static CoNLLDep[] printCoNLLDepRel(DepRel[][] depRel, int root, Sentence sentence) {
    boolean DEBUG = false;
    int ID;
    int HEAD;
    String DEPREL;
    String PHEAD = "_";
    String PDEPREL = "_";
    /*
     * 1 ID       Token counter, starting at 1 for each new sentence.
     * 2 FORM     Word form or punctuation symbol.
     * 3 LEMMA    Lemma or stem (depending on particular data set) of word form, or an
     *            underscore if not available.
     * 4 CPOSTAG  Coarse-grained part-of-speech tag, where tagset depends on the TAGSET.
     * 5 POSTAG   Fine-grained part-of-speech tag, where the tagset depends on the
     *            TAGSET, or identical to the coarse-grained part-of-speech tag if not
     *            available.
     * 6 FEATS    Unordered set of syntactic and/or morphological
     *            features (depending on the particular TAGSET), separated by a vertical
     *            bar (|), or an underscore if not available.
     * 7 HEAD     Head of the current token, which is either a value of ID or ZERO ('0').
     *            Note that depending on the original treebank annotation, there may be
     *            multiple tokens with an ID of ZERO.
     * 8 DEPREL   Dependency relation to the HEAD. The set of
     *            dependency relations depends on the particular TAGSET. Note that
     *            depending on the original treebank annotation, the dependency relation
     *            may be meaningful or simply 'ROOT'.
     * 9 PHEAD    Projective head of current token, which is either a value of ID or ZERO ('0'),
     *            or an underscore if not available. Note that depending on the original
     *            treebank annotation, there may be multiple tokens an with ID of ZERO.
     *            The dependency structure resulting from the PHEAD column is guaranteed to be
     *            projective (but is not available for all languages), whereas the structures
     *            resulting from the HEAD column will be non-projective for some sentences of some
     *            languages (but is always available).
     * 10 PDEPREL Dependency relation to the PHEAD, or an underscore if not available. The set of
     *            dependency relations depends on the particular TAGSET. Note that depending on the
     *            original treebank annotation, the dependency relation may be meaningful
     *            or simply 'ROOT'.
     */
    CoNLLDep[] dependencies = new CoNLLDep[sentence.sentence_wP.length];
    if (depRel != null) {
      for (int i = 0; i < sentence.length(); i++) {
        if (i == root) {
          HEAD = 0;
          ID = i + 1;
          DEPREL = "CONLL_ROOT";
          int ind = offset(sentence, ID - 1) + 1;
          dependencies[ind-1] = new CoNLLDep(ind, HEAD, DEPREL + '\t' + PHEAD + '\t' + PDEPREL);
        }
        if (DEBUG) {
          Logger.log("printConLL: checking word " + i + " current deps:" + dependencies[i] + '\n');
        }
        for (int j = 0; j < sentence.length(); j++) {
          if (depRel[i][j] != null) {
            // DEPREL = depRel[i][j].cat + "_" + depRel[i][i].slot;
            boolean modifier = depRel[i][j].modifier;
            if (DEBUG) {
              Logger.log("depRel[" + i + "][" + j + "]  not null. modifier? " + modifier + " dependencies[ID-1]: " + dependencies[i]);
            }
            if (modifier) {
              HEAD = i + 1;
              ID = j + 1;
              DEPREL = "CONLL_MOD:" + depRel[i][j].cat + '_' + depRel[i][j].slot;
            } else {
              HEAD = j + 1;
              ID = i + 1;
              // DEPREL = depRel[i][j].cat + "_" + depRel[i][j].slot;
              DEPREL = "CONLL_ARG:" + depRel[i][j].cat + '_' + depRel[i][j].slot;
            }
            int ind = offset(sentence, ID - 1) + 1;
            if (dependencies[ind - 1] == null) {
              int h_ind = offset(sentence, HEAD - 1) + 1;

              dependencies[ind-1] = new CoNLLDep(ind, h_ind,
                  DEPREL + '\t' + PHEAD + '\t' + PDEPREL + '\t' + depRel[i][j].modifier);

              if (DEBUG) {
                Logger.log("dependencies[i]: " + dependencies[ind - 1] + '\n');
              }
            }

            // else {
            // "WARNING: " + FORM + " has two dependencies:" +
            // dependencies[ID-1] + " " +new String(ID + "\t" + FORM + "\t" +
            // LEMMA + "\t" + CPOSTAG + "\t" + POSTAG + "\t" + FEATS + "\t" +
            // HEAD + "\t" + DEPREL + "\t" + PHEAD + "\t" + PDEPREL +
            // depRel[i][j].modifier) + "\n";
            // }
          }
        }
      }
    }
    dependencies = insertPunctuation(dependencies, sentence, -1);
    for (int i = 0; i < sentence.sentence_wP.length; i++) {
      if (dependencies[i] == null) {
        DEPREL = "X";
        ID = i + 1;
        if (ID == sentence.length_noP()) {
          HEAD = 0;
        } else {
          HEAD = offset(sentence, sentence.length_noP() - 1) + 1;
        }
        int ind = offset(sentence, ID - 1) + 1;

        dependencies[i] = new CoNLLDep(ind, HEAD, DEPREL + '\t' + PHEAD + '\t' + PDEPREL);
      }
      dependencies = insertPunctuation(dependencies, sentence, i);
    }
    dependencies = insertPunctuation(dependencies, sentence, sentence.length());
    // blank line separates arguments and adjuncts
    return dependencies;
  }

  public static <T extends Grammar> void featureStructure(Tree<T> BestTree, CCGcat.DepType depType, Model<T> model) {
    fsRecurse(BestTree, depType, model);
    CCGcat.resetCounters();
  }

  private static <T extends Grammar> void fsRecurse(Tree<T> A, CCGcat.DepType depType, Model<T> model) {
    if (A.leftChild != null) {
      if (A.rightChild == null) {
        fsRecurse(A.leftChild, depType, model);
        switch (A.rule.Type) {
        case PRODUCTION:
          A.ccgcat = CCGcat.lexCat(getCat(A.leftChild.parentCategory, model), getCat(A.parentCategory, model), A.X);
          break;
        case TYPE_CHANGE:
        case TYPE_TOP:
          A.ccgcat = CCGcat.typeChangingRule(A.leftChild.ccgcat, getCat(A.parentCategory, model));
          if (A.leftChild.ccgcat.heads() != null) {
            A.ccgcat.setHeads(A.leftChild.ccgcat.heads());
          }
          break;
        case FW_TYPERAISE:
        case BW_TYPERAISE:
          A.ccgcat = CCGcat.typeRaiseTo(A.leftChild.ccgcat, getCat(A.parentCategory, model));
          A.ccgcat.setHeads(A.leftChild.ccgcat.heads());
          break;
        default:
          throw new Parser.FailedParsingAssertion("Chart -- Can't handle: " + A.type);
        }
        if (A.ccgcat == null) {
          throw new Parser.FailedParsingAssertion("Null Unary CCGcat");
        }
      } else {
        fsRecurse(A.leftChild, depType, model);
        fsRecurse(A.rightChild, depType, model);
        if (A.leftChild.ccgcat == null) {
          throw new Parser.FailedParsingAssertion("B's CCGcat is null: " + model.grammar.prettyCat(A.leftChild.parentCategory));
        }
        if (A.rightChild.ccgcat == null) {
          throw new Parser.FailedParsingAssertion("C's CCGcat is null: " + model.grammar.prettyCat(A.rightChild.parentCategory));
        }
        switch (A.rule.Type) {
        case FW_APPLY:
          A.ccgcat = CCGcat.apply(A.leftChild.ccgcat, A.rightChild.ccgcat);
          break;
        case FW_COMPOSE:
        case FW_2_COMPOSE:
        case FW_3_COMPOSE:
        case FW_4_COMPOSE:
        case FW_XCOMPOSE:
          A.ccgcat = CCGcat.compose(A.leftChild.ccgcat, A.rightChild.ccgcat);
          break;
        case FW_TYPECHANGE:
          if (InducedCAT.CONJ((model.grammar.Categories.get(A.leftChild.parentCategory)))) {
            CCGcat tmp = CCGcat.conjunction(A.rightChild.ccgcat, A.leftChild.ccgcat, depType);
            A.ccgcat = CCGcat.typeChangingRule(tmp, getCat(A.parentCategory, model));
            if (tmp.heads() != null) {
              A.ccgcat.setHeads(tmp.heads());
            }
            if (tmp.conjHeads() != null) {
              A.ccgcat.setConjHeads(tmp.conjHeads());
            }
          } else {
            A.ccgcat = CCGcat.typeChangingRule(A.rightChild.ccgcat, getCat(A.parentCategory, model));
          }
          break;
        case FW_PUNCT:
          if (A.parentCategory != A.rightChild.parentCategory)
            A.ccgcat = CCGcat.typeChangingRule(A.rightChild.ccgcat, getCat(A.parentCategory, model));
          else
            A.ccgcat = CCGcat.punctuation(A.rightChild.ccgcat, A.leftChild.ccgcat);
          break;
        case FW_CONJOIN:
          if (model.grammar.Categories.get(A.rightChild.parentCategory).has_conj) // X , and Y
            A.ccgcat = CCGcat.punctuation(A.rightChild.ccgcat, A.leftChild.ccgcat);
          else
            A.ccgcat = CCGcat.conjunction(A.rightChild.ccgcat, A.leftChild.ccgcat, depType);
          break;
        case FW_SUBSTITUTION:
          A.ccgcat = CCGcat.substitute(A.rightChild.ccgcat, A.leftChild.ccgcat, depType);
          break;
        case BW_APPLY:
          A.ccgcat = CCGcat.apply(A.rightChild.ccgcat, A.leftChild.ccgcat);
          break;
        case BW_COMPOSE:
        case BW_2_COMPOSE:
        case BW_3_COMPOSE:
        case BW_4_COMPOSE:
        case BW_XCOMPOSE:
          A.ccgcat = CCGcat.compose(A.rightChild.ccgcat, A.leftChild.ccgcat);
          break;
        case BW_TYPECHANGE:
          A.ccgcat = CCGcat.typeChangingRule(A.leftChild.ccgcat, getCat(A.parentCategory, model));
          break;
        case BW_PUNCT:
          if (A.parentCategory != A.leftChild.parentCategory)
            A.ccgcat = CCGcat.typeChangingRule(A.leftChild.ccgcat, getCat(A.parentCategory, model));
          else
            A.ccgcat = CCGcat.punctuation(A.leftChild.ccgcat, A.rightChild.ccgcat);
          break;
        case BW_CONJOIN:
          A.ccgcat = CCGcat.coordinate(A.leftChild.ccgcat, A.rightChild.ccgcat, depType);
          break;
        //case BW_SUBSTITUTION:
        //  A.ccgcat = CCGcat.substitute(A.leftChild.ccgcat, A.rightChild.ccgcat, depType);
        //  break;
        default:
          throw new Parser.FailedParsingAssertion("Chart -- Can't handle: " + A.type);
        }
        // THIS IS A DISGUSTING HACK TO TRANSFER THE HEADEDNESS FROM A CONJOINED FUNCTOR TO THE CONJUNCTION
        if (depType.equals(CCGcat.DepType.CoNLL)
            && A.headIndex == A.leftChild.headIndex
            && A.leftChild != null
            && A.leftChild.leftChild != null
            && A.leftChild.rightChild != null
            && A.leftChild.rule.Type.equals(Rule_Type.BW_CONJOIN)
            && Configuration.CONLL_DEPENDENCIES.equals(CoNLLDependency.CC_X1___CC_X2)
            && A.ccgcat.filledDependencies() != null
            ) {
          DepList depList = A.ccgcat.filledDependencies();
          if (depList.headIndex >= A.leftChild.leftChild.headIndex && depList.headIndex <= A.leftChild.rightChild.headIndex)
            depList.headIndex = A.ccgcat.heads().index();
        }
        if (depType.equals(CCGcat.DepType.CoNLL)
            && A.rightChild != null
            && A.rightChild.leftChild != null
            && A.rightChild.rightChild != null
            && A.headIndex == A.rightChild.headIndex
            && A.rightChild.rule.Type.equals(Rule_Type.BW_CONJOIN)
            && Configuration.CONLL_DEPENDENCIES.equals(CoNLLDependency.CC_X1___CC_X2)
            && A.ccgcat.filledDependencies() != null
            ) {
          DepList depList = A.ccgcat.filledDependencies();
          if (depList.headIndex >= A.rightChild.leftChild.headIndex && depList.headIndex <= A.rightChild.rightChild.headIndex)
            depList.headIndex = A.ccgcat.heads().index();
        }
        if (A.ccgcat == null) {
          System.err.println(A.type);
          System.err.println(getCat(A.parentCategory, model) + "\t->\t" + getCat(A.leftChild.parentCategory, model)
              + '\t' + getCat(A.rightChild.parentCategory, model));
          System.err.println(A.toString(model, 0));
          System.err.println(A.leftChild.ccgString());
          System.err.println(A.rightChild.ccgString());
          throw new Parser.FailedParsingAssertion("Null CCGcat");
        }
      }
    }
  }

  private static String getCat(long cat, Model model) {
    return forwardslashPeriod.matcher(backslashPeriod.matcher(model.grammar.prettyCat(cat)).replaceAll("\\\\")).replaceAll("/");
  }

  /**
   * Returns TeX (if applicable) and AUTO parse for the Top-K trees
   */
  public void viterbi(Grammar globalGrammar) {
    // Create the output JSON if we didn't read one in
    if (sentence.JSON == null) {
      JSONFormat.createFromSentence(this.sentence, globalGrammar);
    }
    // Build string parse chart
    // If there is a JSON object which was read in, place the parses in this object
    // and serialize the final object
    if (sentence.JSON != null) {
      sentence.JSON.parses = parses;
      sentence.JSON.synPars = new SynParObj[TOP.topK.size()];
      try {
        // For sentence/tree in top-K, build LISP style parse and place it inside a SynParObj
        for (int i = 0; i < TOP.topK.size(); ++i) {
          sentence.JSON.synPars[i] = new SynParObj();
          Tree<G> T = TOP.PointersToTree(i);
          sentence.JSON.synPars[i].score = T.prob;

          //  Build Feature structure for and extract CCG dependencies
          Chart.featureStructure(T, CCGcat.DepType.CCG, model); // CCGCat issue
          StringBuilder parse = new StringBuilder();
          buildAUTORecurse(parse, sentence, globalGrammar, T.leftChild); // B to skip TOP
          sentence.JSON.synPars[i].synPar = parse.toString().trim();

          sentence.JSON.synPars[i].depParse = CCGdependencies(T, sentence);

          // Build Feature structure for and extractCoNLL dependencies
          if (!Configuration.CONLL_DEPENDENCIES.equals(CoNLLDependency.None)) {
            Chart.featureStructure(T, CCGcat.DepType.CoNLL, model); // CCGCat issue
            sentence.JSON.synPars[i].conllParse = CoNLLdependencies(T.leftChild, sentence);
          }
        }
      } catch (Exception e){
        sentence.JSON.synPars = null;
        e.printStackTrace();
      }
      // Serialize the JSON object, store serialized data structure instead of human
      // readable string
    }
  }

  /**
   * Builds a TeX Beamer slide with the parse.  Information is extracted from viterbiParse which has
   * category span information
   * @param viterbiParse Category span information
   * @return A TeX Beamer slide
   */
  public static String buildTeX(ArrayList<ArrayList<ArrayList<String>>> viterbiParse, Sentence sentence, Grammar grammar) {
    StringBuilder TeXParse = new StringBuilder();
    TeXParse.append("\\begin{frame}\\centering\n");
    TeXParse.append("\\adjustbox{max height=\\dimexpr\\textheight-5.5cm\\relax,\n");
    TeXParse.append("           max width=\\textwidth}{\n");
    // Start at len , 0
    //TeXParse += "viterbi: " + BestTree.prob + "\n";
    TeXParse.append("\\deriv{").append(sentence.length()).append("}{\n");

    if (Configuration.TEX_LANGUAGE.equals("chinese")) {
      TeXParse.append("\\text{\\chinese ");
    } else {
      TeXParse.append("{\\rm ");
    }

    if ((sentence.get(0).universal() != null && sentence.get(0).universal().toString().equals("NUM"))
        || (sentence.get(0).tag() != null && sentence.get(0).tag().toString().equals("CD")))
      TeXParse.append(Logger.escape_chars(grammar.Words.get(sentence.get(0).rawWord())));
    else
      TeXParse.append(Logger.escape_chars(grammar.Words.get(sentence.get(0).word())));

    if (Configuration.TEX_LANGUAGE.equals("chinese")) {
      TeXParse.append("\\stopchinese}");
    } else {
      TeXParse.append('}');
    }
    for (int i = 1; i < sentence.length(); i++) {
      if (Configuration.TEX_LANGUAGE.equals("chinese")) {
        TeXParse.append("& \\text{\\chinese ");
      } else {
        TeXParse.append("& {\\rm ");
      }

      if (sentence.get(i).universal().toString().equals("NUM") || sentence.get(i).tag().toString().equals("CD"))
        TeXParse.append(Logger.escape_chars(grammar.Words.get(sentence.get(i).rawWord())));
      else
        TeXParse.append(Logger.escape_chars(grammar.Words.get(sentence.get(i).word())));

      if (Configuration.TEX_LANGUAGE.equals("chinese")) {
        TeXParse.append("\\stopchinese}");
      } else {
        TeXParse.append('}');
      }
    }
    TeXParse.append("\\\\\n");

    TeXParse.append("\\uline{1}");
    for (int i = 1; i < sentence.length(); i++) {
      TeXParse.append("& \\uline{1}");
    }
    TeXParse.append("\\\\\n");

    boolean repeat = false;
      for (int s = 0; s < sentence.length(); s++) {
        int extra = 0;
        for (int i = 0; i < sentence.length() - s; i++) {
          ArrayList<String> strings = viterbiParse.get(i).get(i + s);
          Rule_Type type;
          String cat;
          if (!strings.isEmpty()) {
            if (strings.size() % 2 == 1) {
              cat = Logger.escape_chars(strings.remove(0));//
              TeXParse.append("\\mc{").append(s + 1).append("}{\\it ").append(cat).append('}');
              extra = s;
            } else {
              type = Rule_Type.valueOf(strings.remove(0));
              TeXParse.append(Type(type, s + 1));
              if (type.equals(Rule_Type.FW_CONJOIN)) {
                strings.remove(0);
              } else {
                extra = s;
              }
            }
            if (!strings.isEmpty()) {
              repeat = true;
            }
          }
          if (i == sentence.length() - s - 1) {
            TeXParse.append("\\\\\n");
          } else if (extra == 0) {
            TeXParse.append("\t&");
          } else {
            extra -= 1;
          }
        }
        if (repeat) {
          s -= 1;
          repeat = false;
        }
      }
      TeXParse.append('}');
    return TeXParse + "}\n\\end{frame}\n";
  }

  /**
   * Recursively construct an AUTO format Parse
   */
  public static void buildAUTORecurse(StringBuilder AUTOparse, Sentence sentence, Grammar grammar, Tree Tree) {
    if (Tree.leftChild != null) {
      // LEX
      if (Tree.leftChild.leftChild == null) {

        // Siva needs dummy values in the lexical items
        // (<L ccgcategory word lemma postag dummy dummy dummy/indexed>)
        // Standard AUTO
        // (<L ccgcategory tag tag word indexed>)
        String word = grammar.Words.get(sentence.get(Tree.X).rawWord());
        String lemma = grammar.Words.get(sentence.get(Tree.X).lemma());
        if (Configuration.auto_type.equals(AUTO_TYPE.CANDC)) {
          AUTOparse.append(" (<L ").append(grammar.prettyCat(Tree.parentCategory).replace("\\.", "\\").replace("/.", "/"))
              .append(' ').append(word)
              .append(' ').append(lemma)
              .append(' ').append(sentence.get(Tree.X).tag())
              .append(" _ _ ").append(Tree.ccgcat.catStringIndexed())
              .append('>');
        } else {
          AUTOparse.append(" (<L ").append(grammar.prettyCat(Tree.parentCategory).replace("\\.", "\\").replace("/.", "/"))
              .append(' ').append(sentence.get(Tree.X).tag())
              .append(' ').append(sentence.get(Tree.X).tag())
              .append(' ').append(word)
              .append(' ').append(Tree.ccgcat.catStringIndexed())
              .append('>');
        }
      } else {
        AUTOparse.append(" (<T ").append(grammar.prettyCat(Tree.parentCategory).replace(".", ""));
        if (Configuration.auto_type.equals(AUTO_TYPE.CANDC)) {
          AUTOparse.append(' ').append(op(Tree.rule.Type));
        }
        // UNARY
        if (Tree.rightChild == null) {
          AUTOparse.append(" 0 1>");
        } else {
          if (((Binary)Tree.rule).head.equals(Rule_Direction.Left)) {
            AUTOparse.append(" 0 2>");
          } else {
            AUTOparse.append(" 1 2>");
          }
        }
      }


      if (Tree.leftChild.leftChild != null) {
        buildAUTORecurse(AUTOparse, sentence, grammar, Tree.leftChild);
      }

      if (Tree.rightChild != null) {
        buildAUTORecurse(AUTOparse, sentence, grammar, Tree.rightChild);
      }

      AUTOparse.append(')');
    }
  }

  /**
   * Recursively fills a "set of cells with strings for TeX"
   */
  public static void buildTeXCells(ArrayList<ArrayList<ArrayList<String>>> viterbiParse,
                                   Model model, Tree Tree) {
    if (Tree.leftChild != null) {
      if (Tree.leftChild.leftChild != null) {
        buildTeXCells(viterbiParse, model, Tree.leftChild);
      }

      if (Tree.rightChild != null) {
        buildTeXCells(viterbiParse, model, Tree.rightChild);
      }
    }

    viterbiParse.get(Tree.X).get(Tree.Y).add(Tree.rule.Type.toString());
    viterbiParse.get(Tree.X).get(Tree.Y).add(
        TeXbackslash.matcher(forwardslashPeriod.matcher(backslashPeriod.matcher(model.grammar.prettyCat(Tree.parentCategory)).replaceAll("\\\\")).replaceAll("/")).replaceAll("\\\\bs "));
  }


  /**
   * Returns a human readable version of the grammar rule used
   * Required by Siva
   * @param type Rule Type
   * @return Parse operation
   */
  private static String op(Rule_Type type) {
    switch(type) {
    case TYPE_CHANGE:
      // This seems to match the data but also seems ... wrong
      return "lex";
    case BW_2_COMPOSE:
    case BW_3_COMPOSE:
      return "gbc";
    case BW_APPLY:
    case BW_CONJOIN:
      return "ba";
    case BW_COMPOSE:
      return "bc";
    case BW_PUNCT:
      return "rp";
    case BW_TYPERAISE:
    case FW_TYPERAISE:
      return "tr";
    case FW_2_COMPOSE:
    case FW_3_COMPOSE:
      return "gfc";
    case FW_APPLY:
      return "fa";
    case FW_COMPOSE:
      return "fc";
    case FW_CONJOIN:
      return "conj";
    case FW_PUNCT:
      return "lp";
    case BW_XCOMPOSE:
        return "bx";
    case FW_XCOMPOSE:
        return "fx";
    default:
        throw new Parser.FailedParsingAssertion("Invalid Option: " + type);
    }
  }

  private static String Type(Rule_Type type, int s) {
    switch (type) {
    case FW_APPLY:
      return "\\fapply{" + s + '}';
    case FW_COMPOSE:
    case FW_2_COMPOSE:
    case FW_3_COMPOSE:
      return "\\fcomp{" + s + '}';
    case FW_XCOMPOSE:
      return "\\fxcomp{" + s + '}';
    case BW_APPLY:
      return "\\bapply{" + s + '}';
    case BW_COMPOSE:
    case BW_2_COMPOSE:
    case BW_3_COMPOSE:
      return "\\bcomp{" + s + '}';
    case BW_XCOMPOSE:
      return "\\bxcomp{" + s + '}';
    case TYPE_TOP:
      return "\\comb{" + s + "}{TOP}";
    case FW_PUNCT:
      return "\\comb{" + s + "}{> punc}";
    case BW_PUNCT:
      return "\\comb{" + s + "}{> punc}";
    case FW_CONJOIN:
      return "\t&";
    case BW_CONJOIN:
      return "\\conj{" + s + '}';
    case FW_TYPERAISE:
      return "\\ftype{" + s + '}';
    case BW_TYPERAISE:
      return "\\btype{" + s + '}';
    case TYPE_CHANGE:
      return "\\comb{" + s + "}{TC}";
    default:
      return "";
    }
  }


  /**
   * Prints all chart-items in chart
   */
  public void debugChart() {
    Cell<G>[][] local_chart = this.chart;
    if (local_chart == null) {
      // Util.log("F:  " + sentence.asTags() + "\nnull\n");
      return;
    }
    Logger.log("S:  " + parses + '\t' + sentence.asTags() + '\n');
    for (int i = 0; i < sentence.length(); i++) {
      Logger.log(model.grammar.Words.get(sentence.get(i).word()) + ' ');
    }
    Logger.logln("");
    for (int s = 0; s < local_chart.length; s++) {
      Cell<G> A = local_chart[s][s];
      Logger.log(s + "," + s);
      Logger.log("\tCats:\n");
      A.values().forEach(ChartItem::logEquivClass);
      Logger.log("\n");
    }

    for (int s = 1; s < local_chart.length; s++) {
      for (int i = 0; i < local_chart.length - s; i++) {
        Cell<G> A = local_chart[i][i + s];
        Logger.log(i + "," + (i + s) + '\n');
        if (A != null) {
          A.values().forEach(ChartItem::logEquivClass);
        }
        Logger.log("\n");
      }
    }
    Logger.log("\n");
  }

  /**
   * Original: Don't allow for crossing brackets. Formulation: [ x ] should be
   * parsed [ x -> x followed by [ x ] -> x 1) x1 x2 ... xn is a valid
   * constituent 2) [ x ] is a valid constituent Take 2: TOOD: What about
   * crossing of a boundary: A `` B
   */
  private void computeSpans() {
    POS[] localtags = sentence.getTagsWithPunct();
    int slength = localtags.length;
    boolean[] punc = new boolean[slength];
    for (int i = 0; i < slength; ++i) {
      punc[i] = Tagset.Punct(localtags[i]);
    }
    //    The commented constraints disallow certain cells.  They are being left
    //    here until I decide I don't actually need them.  :)

    //    int first_non_P = 0;
    //    int last_non_P = slength-1;
    //    // first non punct
    //    while(punc[first_non_P] && first_non_P < slength) { ++first_non_P; }
    //    while(punc[last_non_P] && last_non_P > 0) { --last_non_P; }
    //
    //    // Block out columns to top of punctuation at the end of the sentence
    //    for(int i = last_non_P+1; i < slength; ++i){
    //      for(int j = first_non_P+1; j < i; ++j) {
    //        disallowed_constituents[j][i] = true;
    //      }
    //    }
    //
    //    // Block out rows to top of punctuation at the beggining of the sentence
    //    for(int i = 0; i < first_non_P; ++i){
    //      for(int j = i+1; j < slength-1; ++j) {
    //        disallowed_constituents[i][j] = true;
    //      }
    //    }

    // Disallow cells on first non-lexical level that are both punc
    for(int i =0; i < slength-1; ++i){
      if(punc[i] && punc[i+1]) {
        disallowed_constituents[i][i+1] = true;
      }
    }
  }

  // TODO(bisk1): KILL
  /**
   * Determines whether the punctuation in the span [l,r] allows for a
   * constituent
   * 
   * @param l Left
   * @param r Right
   * @return Is a constituent allowed
   */
  final boolean punctuationBracketing(int l, int r) {
    return  Configuration.ignorePunctuation
        || !disallowed_constituents[l][r];
  }

  /**
   * Build boolean[][] of bracketings
   * @param entities Set of entities to use for construction
   */
  final void crossingBrackets(ArrayList<IntPair> entities){
    if (entities.size() == 0) {
      return;
    }
    int x,y;
    for (int s = 0; s < length; s++) {
      for (int i = 0; i < length - s; i++) {
        if (s == 0) {
          x = i; y = i;
        } else {
          x = i; y = i+s;
        }
        for (IntPair ip : entities) {
          //  i first j  second
          if (y > ip.first() && x < ip.first() && y < ip.second()) {
            this.crossingEntity[x][y] = true;
          }
          // first  i  second  j
          else if (x > ip.first() && x <= ip.second() && y > ip.second()) {
            this.crossingEntity[x][y] = true;
          }
          // Overlap on the tail
          else if (x == ip.second()) {
            this.crossingEntity[x][y] = true;
          }
          // Overlap on the start
          else if (y == ip.first()) {
            this.crossingEntity[x][y] = true;
          }
        }
      }
    }
  }

  /**
   * Mark full entity spans
   * @param entities List of entity spans
   */
  final void entitySpan(ArrayList<IntPair> entities) {
    if (entities.size() == 0) {
      return;
    }
    for (int s = 0; s < length; s++) {
      for (int i = 0; i < length - s; i++) {
        int x,y;
        if (s == 0) {
          x = i; y = i;
        } else {
          x = i; y = i+s;
        }
        entities.stream()
            .filter(ip -> ip.first() == x && ip.second() == y)
            .forEach(ip -> {
              this.Entity[x][y] = true;
              if (x < length - 1 && this.Entity[x + 1][y]) {
                this.Entity[x + 1][y] = false;
              }
              if (y > 0 && this.Entity[x][y - 1]) {
                this.Entity[x][y - 1] = false;
              }
            });
      }
    }
  }

  /**
   * Check if [x,y] crosses any bracketings
   * @param x Start index
   * @param y End index
   * @return if [x,y] crosses brackets
   */
  public final boolean crossingBrackets(int x, int y) {
    return this.crossingEntity[x][y];
  }

  /**
   * Check if [x, y] is the full entity
   * @param x  Start index
   * @param y  End index
   * @return if [x,y] is a full entity
   */
  public final boolean fullEntity(int x, int y) {
    return this.Entity[x][y];
  }

  /**
   * The length of the underlying sentence/dimensions of the chart
   */
  public int getLength() {
    return length;
  }

  void setLength(int length) {
    this.length = length;
  }

  public void fromAUTO(InductionParser parser) {
    ArrayList<String> AUTOparses = new ArrayList<>();
    if (sentence.AUTOparse != null)
      AUTOparses.add(sentence.AUTOparse);
    if (sentence.JSON != null && sentence.JSON.synPars != null){
      for (SynParObj obj : sentence.JSON.synPars){
        AUTOparses.add(obj.synPar);
      }
    }
    chart = new Cell[tags.length][tags.length];

    for (String parse : AUTOparses) {
      // Build a tree and create the appropriate grammar rules
      Tree<G> parse_tree = new Tree<>(parse, model, parser);
      // Create all the corresponding ChartItems in the Chart itself
      if (parse_tree.leftChild == null) {
        System.err.println("we have a problem");
        parse_tree = new Tree<>(parse, model, parser);
      }
      try {
        createChartItem(parse_tree);
      } catch (Exception e) {
        System.err.println("Skipping: " + parse);
        return;
      }
    }


    // Connect Children
    for (ChartItem<G> child : chart[0][chart.length-1].values()) {
      // Create TOP rule
      Unary u = parser.createInductionRule(model, new InducedCAT(InducedCAT.TOP),
          model.grammar.Categories.get(child.Category));

      // Create TOP
      if (TOP == null) {
        TOP = new ChartItem<>(u.A, u.Type, 0, chart[0][chart.length - 1]);
      }

      TOP.addChild(u, child, null);
    }
    removeRedundantBPs(TOP);
    chart[0][chart.length-1].addCat(TOP);
    parses = AUTOparses.size();
  }

  void createChartItem(Tree<G> Tree) {
    // Lexical base case
    if (Tree.X == Tree.Y && Tree.leftChild.leftChild == null) {
      if (chart[Tree.X][Tree.Y] == null)
        chart[Tree.X][Tree.Y] = new Cell<>(this, Tree.X);
      // put lexical chart item here
      chart[Tree.X][Tree.Y].addCat(Grammar.LexChartItem((Unary) Tree.rule, chart[Tree.X][Tree.Y]));
    } else {
      createChartItem(Tree.leftChild);
      if (Tree.rightChild != null)
        createChartItem(Tree.rightChild);

      if (chart[Tree.X][Tree.Y] == null)
        chart[Tree.X][Tree.Y] = new Cell<>(this, Tree.X, Tree.Y);

      Cell<G> leftCell = chart[Tree.leftChild.X][Tree.leftChild.Y];
      ChartItem<G> left = fromTree(Tree.leftChild, leftCell);

      if (Tree.rightChild != null) {
        Cell<G> rightCell = chart[Tree.rightChild.X][Tree.rightChild.Y];
        ChartItem<G> right = fromTree(Tree.rightChild, rightCell);
        if (right == null)
          right = fromTree(Tree.rightChild, rightCell);

        ChartItem<G> parent = chart[Tree.X][Tree.Y].addCat((Binary) Tree.rule, left, right);
        removeRedundantBPs(parent);
      } else {
        ChartItem<G> parent = chart[Tree.X][Tree.Y].addCat(
            new ChartItem<>(Tree.rule.A, Tree.rule.Type, 0, chart[Tree.X][Tree.Y]));
        parent.addChild(Tree.rule, left, null);
        removeRedundantBPs(parent);
        //chart[Tree.X][Tree.Y].addCat(parent);
      }
    }
  }

  ChartItem<G> fromTree(Tree<G> tree, Cell<G> cell) {
    if (tree.rule instanceof Unary) {
      return cell.getCat(new ChartItem<>(tree.parentCategory, tree.rule.Type, 0, cell));
    }

    Binary rule = (Binary)tree.rule;
    if (rule.Type == Rule_Type.FW_PUNCT) {
      return cell.getCat(new ChartItem<>(tree.parentCategory, tree.rightChild.type, tree.rightChild.arity, Punctuation.FW, cell));
    } else if (rule.Type == Rule_Type.BW_PUNCT) {
      return cell.getCat(new ChartItem<>(tree.parentCategory, tree.leftChild.type, tree.leftChild.arity, Punctuation.BW, cell));
    } else if (rule.Type == Rule_Type.FW_CONJOIN) {
      return cell.getCat(new ChartItem<>(tree.parentCategory, tree.rightChild.type, tree.rightChild.arity, Punctuation.None, cell));
    } else if (rule.Type == Rule_Type.BW_CONJOIN && Rule_Type.TR(tree.rightChild.type)) {
      return cell.getCat(new ChartItem<>(tree.parentCategory, tree.leftChild.type, tree.leftChild.arity, Punctuation.None, cell));
    } else {
      return cell.getCat(new ChartItem<>(tree.parentCategory, rule.Type, rule.arity, cell));
    }
  }

  void removeRedundantBPs(ChartItem<G> parent) {
    if (parent.children.size() == 1)
      return;
    HashSet<BackPointer<G>> clean = new HashSet<>();
    clean.addAll(parent.children);
    parent.children.clear();
    parent.children.addAll(clean);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    sentence = (Sentence) in.readObject();
    tags = (long[]) in.readObject();
    words = (long[]) in.readObject();
    parses = in.readDouble();

    //chart = (Cell[][]) in.readObject();
    chart = new Cell[tags.length][tags.length];
    if (chart != null) {
      for (int s = 0; s < chart.length; s++) {
        for (int i = 0; i < chart.length - s; i++) {
          chart[i][i + s] = new Cell<>(this, i, i+s);
        }
      }
    }
    TOP = (ChartItem<G>) in.readObject();
    if (chart != null) {
      TOP.cell = chart[0][chart.length - 1];
    }
    rePopulateCellsFromForest(TOP);
    id = in.readInt();
    if (!Configuration.ignorePunctuation) {
      disallowed_constituents = (boolean[][]) in.readObject();
    }
    crossingEntity = (boolean[][]) in.readObject();
    Entity = (boolean[][]) in.readObject();
  }

  void rePopulateCellsFromForest(ChartItem<G> chartItem) {
    if (chartItem == null || chartItem.used)
      return;

    Cell<G> cell = chart[chartItem.X][chartItem.Y];
    cell.addCat(chartItem);
    chartItem.cell = cell;
    if (!chartItem.children.isEmpty()) {
      for (BackPointer<G> bp : chartItem.children) {
        rePopulateCellsFromForest(bp.leftChild());
        rePopulateCellsFromForest(bp.rightChild());
      }
    }
    chartItem.used = true;
  }

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeObject(sentence);
    out.writeObject(tags);
    out.writeObject(words);
    out.writeDouble(parses);
    //out.writeObject(chart);
    out.writeObject(TOP);
    out.writeInt(id);
    if (!Configuration.ignorePunctuation) {
      out.writeObject(disallowed_constituents);
    }
    out.writeObject(crossingEntity);
    out.writeObject(Entity);
  }
}

