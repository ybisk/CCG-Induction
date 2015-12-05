package CCGInduction.grammar;

import CCGInduction.Configuration;
import CCGInduction.ccg.CCGCategoryUtilities;
import CCGInduction.ccg.CCGcat;
import CCGInduction.ccg.DepRel;
import CCGInduction.ccg.InducedCAT;
import CCGInduction.data.Sentence;
import CCGInduction.data.Tagset;
import CCGInduction.experiments.Training;
import CCGInduction.models.Model;
import CCGInduction.parser.BackPointer;
import CCGInduction.parser.ChartItem;
import CCGInduction.parser.InductionParser;
import CCGInduction.parser.Parser;
import CCGInduction.utils.Logger;
import CCGInduction.utils.Math.Log;
import CCGInduction.data.POS;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Stack;
import java.util.regex.Pattern;

/**
 * Tree data-structure (e.g. ONE sampled from a parse chart)
 * 
 * @author bisk1
 * @param <G> Grammar type
 */
public class Tree<G extends Grammar> implements Comparable<Tree<G>>, Serializable {
  private static final Pattern split_auto_parse = Pattern.compile("((?<=\\(<)|(?=\\(<))|((?<=>\\))|(?=>\\)))");
  private static final Pattern close_parens = Pattern.compile("[\\)\\s*]+");

  /**
   * Left Child
   */
  public Tree<G> leftChild = null;
  /**
   * Right Child
   */
  public Tree<G> rightChild = null;
  /**
   * Rule used
   */
  public Rule rule;

  /**
   * Parent category
   */
  public long parentCategory = -1;
  private transient InducedCAT inducedCAT;
  /**
   * Rule Type
   */
  public Rule_Type type = null;
  public int arity = 0;

  /**
   * Head Index of subtree
   */
  public int headIndex = -1;

  /**
   * Other variables used in chart item representation for eq class. e.g. head
   * information
   */
  private final ChartItem<G> chartItem;
  /**
   * Reference to child chart items
   */
  private final BackPointer<G> backPointer;
  /**
   * Inside probability
   */
  public double prob;
  /**
   * Chart location X
   */
  public int X = -1;
  /**
   * Chart location Y
   */
  public int Y = -1;

  /**
   * CCG Dependency Relation
   */
  public transient DepRel[][] depRel;
  /**
   * CCG Cat, used to compute dependencies
   */
  public transient CCGcat ccgcat;

  /**
   * Unary Tree branch
   * 
   * @param rule_used Rule
   * @param parentChartItem ChartItem of Parent
   * @param backPointer Backpointer
   * @param probability Probability
   * @param childTree Child
   */
  public Tree(Unary rule_used, ChartItem<G> parentChartItem, BackPointer<G> backPointer,
              double probability, Tree<G> childTree) {
    rule = rule_used;
    parentCategory = rule.A;
    type = rule.Type;
    headIndex = childTree.headIndex;
    chartItem = parentChartItem;
    this.backPointer = backPointer;
    Set(probability, childTree, null);
  }

  /**
   * Binary Tree branch
   * 
   * @param rule_used Rule
   * @param parentChartItem ChartItem of Parent
   * @param backPointer Backpointer
   * @param probability Probability
   * @param leftChild Left Child
   * @param rightChild Right Child
   */
  public Tree(Binary rule_used, ChartItem<G> parentChartItem, BackPointer<G> backPointer,
              double probability, Tree<G> leftChild, Tree<G> rightChild) {
    rule = rule_used;
    parentCategory = rule.A;
    switch (rule.Type) {
      case FW_PUNCT:
      case FW_CONJOIN:
        type = rightChild.type;
        break;
      case BW_PUNCT:
        type = leftChild.type;
        break;
      case BW_CONJOIN:
        if (Rule_Type.TR(leftChild.type)) {
          type = leftChild.type;
          break;
        }
      default:
        type = rule.Type;
        break;
    }
    arity = rule_used.arity;
    chartItem = parentChartItem;
    this.backPointer = backPointer;
    this.headIndex = headIndex(rule_used, leftChild, rightChild);
    Set(probability, leftChild, rightChild);
  }

  private static int headIndex(Binary rule, Tree leftChild, Tree rightChild) {
    int headIndex;
    if (rule.head.equals(Rule_Direction.Right)) {
      headIndex = rightChild.headIndex;
    } else {
      headIndex = leftChild.headIndex;
    }

    if (rule.head.equals(Rule_Direction.Left) && Rule_Type.TR(leftChild.type))
      headIndex = rightChild.headIndex;
    if (rule.head.equals(Rule_Direction.Right) && Rule_Type.TR(rightChild.type))
      headIndex = leftChild.headIndex;

    if (rule.Type.equals(Rule_Type.BW_CONJOIN)) {
      headIndex = leftChild.headIndex;
    } else if (rule.Type.equals(Rule_Type.FW_CONJOIN)) {
      headIndex = rightChild.headIndex;
    }
    return headIndex;
  }

  /**
   * Recomputes the head index for a conll tree given Conj information
   */
  public void computeCoNLLHeads() {
    // If lexical, return
    if (this.leftChild == null) {
      return;
    }
    // If unary, inherit left child
    if (this.rightChild == null) {
      this.headIndex = this.leftChild.headIndex;
      return;
    }
    // If binary, check conjunction
    leftChild.computeCoNLLHeads();
    rightChild.computeCoNLLHeads();
    switch (rule.Type){
      case BW_CONJOIN:
        switch(Configuration.CONLL_DEPENDENCIES) {
          case X1_CC___CC_X2:
          case X1_CC___X1_X2:
          case X1_X2___X2_CC:
            headIndex = leftChild.headIndex;
            return;
          case X2_X1___X2_CC:
          case CC_X1___CC_X2:
            headIndex = rightChild.headIndex;
            return;
        }
        break;
      case FW_CONJOIN:
        switch(Configuration.CONLL_DEPENDENCIES) {
          case X1_CC___CC_X2:
          case X1_CC___X1_X2:
          case X1_X2___X2_CC:
          case X2_X1___X2_CC:
            headIndex = rightChild.headIndex;
            return;
          case CC_X1___CC_X2:
            headIndex = leftChild.headIndex;
            return;
        }
        break;
      case FW_APPLY:
      case FW_COMPOSE:
      case FW_XCOMPOSE:
      case FW_2_COMPOSE:
      case FW_3_COMPOSE:
      case FW_4_COMPOSE:
        if (Rule_Type.TR(leftChild.type)) {
          headIndex = rightChild.headIndex;
          return;
        }
        break;
      case BW_APPLY:
      case BW_COMPOSE:
      case BW_XCOMPOSE:
      case BW_2_COMPOSE:
      case BW_3_COMPOSE:
      case BW_4_COMPOSE:
        if (Rule_Type.TR(rightChild.type)) {
          headIndex = leftChild.headIndex;
          return;
        }
        break;
      default:
        break;
    }
    if (((Binary)rule).head.equals(Rule_Direction.Right)) {
      headIndex = rightChild.headIndex;
    } else {
      headIndex = leftChild.headIndex;
    }
  }
  private Tree(InducedCAT category) {
    inducedCAT = category;
    chartItem = null;
    backPointer = null;
  }

  /**
   * Creates a Tree object along with necessary grammar rules from an AUTO formatted parse
   * @param AUTO    Input parse
   * @param model   model instance for hashing/rule creating
   * @param parser  Induction
   */
  public Tree(String AUTO, Model<G> model, InductionParser parser) {
    // Split up the string with (<, tokens, )  as delimiters
    String[] tags = CCGCategoryUtilities.AUTOtoTags(AUTO);
    String[] words = CCGCategoryUtilities.AUTOtoWords(AUTO);
    Sentence sentence = new Sentence(AUTO, model.grammar);
    String[] regex_split = split_auto_parse.split(AUTO);
    ArrayList<String> split = split_parse(regex_split);
    // Stack to push subtrees on
    Stack<Tree<G>> stack = new Stack<>();
    int word_ind = 0;

    String[] node;
    for(int i = 0; i < split.size(); ++i) {
      // Open
      if(split.get(i).equals("(<")) {
        ++i;        // Increment to actual Non-Terminal Information
        node = Logger.whitespace_pattern.split(split.get(i));

        InducedCAT category = InducedCAT.valueOf(node[1]);
        if (category == null)
          throw new Parser.FailedParsingAssertion("Cannot parse:" + category);
        model.grammar.NTRecursively(category);
        if (category == null) {
          chartItem = null;
          backPointer = null;
          return;
        }
        // Non-terminal node
        if (node[0].equals("T")) {
          Tree<G> T = new Tree<>(category);
          T.X = -1;
          T.Y = -1;
          stack.push(T);
        }
        // Lexical node
        else {
          Tree<G> T = new Tree<>(category);
          T.X = word_ind;
          T.Y = word_ind;

          // If the mapping defines this tag as punctuation but not the category
          // and the category is not already defined as something else, make it punc
          if (Tagset.Punct(new POS(tags[word_ind]))  && category.atom != null
              && !InducedCAT.PUNC(category.atom) && !InducedCAT.atomic(category)) {
            InducedCAT.punc = InducedCAT.add(InducedCAT.punc, category.atom);
          }


          //T.rule = grammar.createRule(grammar.NTRecursively(category), grammar.Lex(tags[word_ind]), Rule_Type.PRODUCTION);
          if (Configuration.source.equals(Training.supervised)) {
            if (tags[word_ind].equals("CD") || tags[word_ind].equals("NUM")) {
              T.rule = model.grammar.createRule(model.grammar.NTRecursively(category), model.grammar.Lex(Tagset.convertNumber(words[word_ind])), Rule_Type.PRODUCTION);
              T.rule = model.grammar.createRule(model.grammar.NTRecursively(category), model.grammar.Lex(tags[word_ind]), Rule_Type.PRODUCTION);    // Add it to --> CD for coverage
            } else
              T.rule = model.grammar.createRule(model.grammar.NTRecursively(category), sentence.get(word_ind).wordOrTag(model.grammar.learnedWords, model.grammar), Rule_Type.PRODUCTION);
          } else {
            T.rule = model.grammar.createRule(model.grammar.NTRecursively(category), model.grammar.Lex(tags[word_ind]), Rule_Type.PRODUCTION);
          }
          T.parentCategory = T.rule.A;
          T.type = T.rule.Type;
          T.headIndex = T.X;
          T.leftChild = new Tree<>(T.rule.B, T.X, Rule_Type.LEX, null);
          stack.push(T);
          ++word_ind;
        }
      }
      // Close
      else if (split.get(i).equals(")") || split.get(i).equals(">)")) {
        // If not the last element
        if (i < split.size()-1) {
          Tree<G> T = stack.pop();
          Tree<G> Parent = stack.peek();
          if (Parent.leftChild == null) {
            Parent.leftChild = T;
          } else {
            Parent.rightChild = T;
          }
        }
      }
    }

    // Assign chart spans to every node.
    assignIndexAndRule(model, stack.peek(), parser);

    // Return Tree.  Requires copying values since this is a constructor
    Tree<G> this_tree = stack.pop();
    leftChild      = this_tree.leftChild;
    rightChild     = this_tree.rightChild;
    parentCategory = this_tree.parentCategory;
    type           = this_tree.type;
    arity          = this_tree.arity;
    headIndex      = this_tree.headIndex;
    rule           = this_tree.rule;
    X              = this_tree.X;
    Y              = this_tree.Y;
    chartItem      = null;
    backPointer    = null;
  }

  /**
   * Recurse through the constructed tree creating grammar rules and updating
   * span indices
   * @param model
   * @param tree newly constructed parse tree
   * @param parser Induction parser for creating rules
   */
  private static <G extends Grammar> void assignIndexAndRule(Model<G> model, Tree<?> tree, InductionParser parser) {
    if (tree.X != -1) {
      return;
    }

    // Assign left span as left child's span start
    assignIndexAndRule(model, tree.leftChild, parser);
    tree.X = tree.leftChild.X;
    // If Binary: grab right span from Right child
    // Else:  Grab Left/Unary Child
    if (tree.rightChild != null) {
      assignIndexAndRule(model, tree.rightChild, parser);
      tree.Y = tree.rightChild.Y;
      tree.rule = parser.createSupervisedRule(model, tree.inducedCAT, tree.leftChild.inducedCAT, tree.rightChild.inducedCAT);
      tree.headIndex = headIndex((Binary)tree.rule, tree.leftChild, tree.rightChild);
    } else {
      tree.Y = tree.leftChild.Y;
      tree.rule = parser.createInductionRule(model, tree.inducedCAT, tree.leftChild.inducedCAT);
      tree.headIndex = tree.leftChild.headIndex;
    }
    tree.parentCategory = tree.rule.A;

    switch(tree.rule.Type){
      case FW_PUNCT:
      case FW_CONJOIN:
        tree.type = tree.rightChild.type;
        tree.arity = tree.rightChild.arity;
        break;
      case BW_PUNCT:
        tree.type = tree.leftChild.type;
        tree.arity = tree.leftChild.arity;
        break;
      case BW_CONJOIN:
        if (Rule_Type.TR(tree.leftChild.type)) {
          tree.type = tree.leftChild.type;
          tree.arity = tree.leftChild.arity;
          break;
        }
      default:
        tree.type = tree.rule.Type;
        if (tree.rule instanceof Binary)
          tree.arity = ((Binary)tree.rule).arity;
        break;
    }
  }

  /**
   * Takes in a regex split-up string and further trims trailing > and splits closing )))
   * @param Parse AUTO parse
   * @return Cleaned parse
   */
  private static ArrayList<String> split_parse(String[] Parse) {
    ArrayList<String> toRet = new ArrayList<>();
    for (String str : Parse) {
      str = str.trim();
      if (str.length() != 0) {
        if (close_parens.matcher(str).matches()) {
          // Kill whitespace between ) ) )
          str = Logger.whitespace_pattern.matcher(str).replaceAll("");
          for (char c : str.toCharArray()) {
            toRet.add(String.valueOf(c));
          }
        } else {
          if (str.charAt(str.length()-1) == '>') {
            str = str.substring(0, str.length()-1);
          }
          toRet.add(str);
        }
      }
    }
    return toRet;
  }

  private void Set(double p, Tree<G> b, Tree<G> c) {
    prob = p;
    leftChild = b;
    rightChild = c;
  }

  /**
   * Create Lexical Tree
   * 
   * @param category  Category Hash
   * @param headIndex Head Index
   * @param ruleType  Rule Type
   * @param chardItem ChartItem reference
   */
  public Tree(long category, int headIndex, Rule_Type ruleType, ChartItem<G> chardItem) {
    parentCategory = category;
    type = ruleType;
    this.headIndex = headIndex;
    rule = null;
    prob = Log.ONE;
    chartItem = chardItem;
    backPointer = null;
  }

  @Override
  public String toString() {
    String catstring;
    if (ccgcat != null) {
      catstring = ccgcat.catString();
    } else {
      catstring = "(null CCGcat so far)";
    }
    return prob + "\t" + catstring;
  }

  /**
   * Return String representation based on Model and Grammar
   * 
   * @param model Model whose grammar has category hashes
   * @param depth Recursive depth
   * @return String representation of the tree
   */
  public String toString(Model<G> model, int depth) {
    String s = " ( " + model.grammar.prettyCat(parentCategory) + "|" + type + "|" + arity;
    if (leftChild != null) {
      s += leftChild.toString(model, depth + 1);
    }
    if (rightChild != null) {
      s += rightChild.toString(model, depth + 1);
    }
    s += " ) ";
    return s;
  }

  /**
   * Return String representation based on CCGCat values
   * 
   * @return String
   */
  public String ccgString() {
    if (leftChild.ccgcat == null) {
      return "{ " + ccgcat.catString() + "__" + ccgcat.heads().index() + " }";
    }
    String s = "{ " + ccgcat.catString() + "__" + ccgcat.heads().index();
    if (leftChild != null) {
      s += " " + leftChild.ccgString() + " ";
    }
    if (rightChild != null) {
      s += " " + rightChild.ccgString() + " ";
    }
    return s + " }";
  }

  @Override
  public int compareTo(Tree<G> obj) {
    if (Log.equal(obj.prob, prob)) { // Tied, choose head right
      return obj.headIndex - headIndex; // Still has ties (btw)
    }
    return (int) Math.signum(obj.prob - prob);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!Tree.class.isInstance(o)) {
      return false;
    }
    Tree<?> O = (Tree<?>) o;
    return O.parentCategory == parentCategory && O.type == type
            && O.arity == arity && O.rule == rule
            && ((O.chartItem == null && chartItem == null) || O.chartItem.equals(chartItem))
            && ((O.backPointer == null && backPointer == null) || O.backPointer.equals(backPointer))
            && O.X == X && O.Y == Y && ((leftChild == null && O.leftChild == null) || O.leftChild.equals(leftChild))
            && ((rightChild == null && O.rightChild == null) || O.rightChild.equals(rightChild));
  }

  @Override
  public int hashCode() {
    Long i = parentCategory + rule.hashCode();
    if (leftChild != null) {
      i += leftChild.hashCode();
    }
    if (rightChild != null) {
      i += rightChild.hashCode();
    }
    return i.hashCode();
  }
}
