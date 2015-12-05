package CCGInduction.parser;

import CCGInduction.Configuration;
import CCGInduction.ccg.CCGcat;
import CCGInduction.ccg.CoNLLDependency;
import CCGInduction.data.JSON.JSONFormat;
import CCGInduction.data.Sentence;
import CCGInduction.data.Tagset;
import CCGInduction.experiments.Action;
import CCGInduction.grammar.Grammar;
import CCGInduction.models.HDPArgumentModel;
import CCGInduction.grammar.Tree;
import junit.framework.TestCase;

public class ChartTest extends TestCase {
  private Grammar grammar;
  private HDPArgumentModel model;
  private InductionParser parser;

  @Override
  public void setUp() throws Exception {
    Tagset.readTagMapping("src/main/resources/english.pos.map");
    new Configuration("config/sample-config.properties");
    grammar = new Grammar();
    model   = new HDPArgumentModel(grammar);
    parser  = new InductionParser(Action.Supervised);
  }

  /**
   * Tests the output CCG dependencies of a Tree
   * @throws Exception
   */
  public void testCCGdependencies() throws Exception {

    // Verb clusters
    testCCG(I_was_writing_code);

    // Relative Clauses
    testCCG(code1_that_works_rocks);
    testCCG(code2_that_works_rocks);
    testCCG(code3_works_that_rocks);
    testCCG(code4_works_that_rocks);
    testCCG(words_that_code5_rocks);
    testCCG(words_that_code6_rocks);
    testCCG(that_works_code7_rocks);
    testCCG(that_works_code8_rocks);
    testCCG(code9_that_I_write_sucks);
    testCCG(code10_that_write_I_sucks);
    testCCG(code11_I_write_that_sucks);
    testCCG(code12_write_I_that_sucks);
    testCCG(I_write_that_code13_sucks);
    testCCG(write_I_that_code14_sucks);
    testCCG(that_I_write_code15_sucks);
    testCCG(that_write_I_code16_sucks);

    // Auxiliaries
    testCCG(code17_should_just_work);
    testCCG(code18_just_work_should);
    testCCG(should_just_work_code19);
    testCCG(just_work_should_code20);
    testCCG(should_just_work_code21);
    testCCG(just_work_should_code22);
    testCCG(code23_should_just_work);
    testCCG(code24_just_work_should);
    testCCG(code25_that_I_write_should_also_just_work);

    // Conjunction / Coordination
    testCCG(john_and_mary);
    testCCG(john_and_mary_saw_the_explosion);
    testCCG(john_saw_and_mary_heard_the_explosion);
  }

  private void testCCG(String[] AUTO_DEP) throws Exception {
    Sentence sentence = new Sentence(AUTO_DEP[0], grammar);
    JSONFormat.createFromSentence(sentence, grammar);
    Tree<Grammar> T = new Tree<>(AUTO_DEP[0], model, parser);
    Chart.featureStructure(T, CCGcat.DepType.CCG, model); // CCGCat issue
    String dependencies = JSONFormat.pargString(Chart.CCGdependencies(T, sentence), sentence.JSON);
    assertEquals("Dependency Mismatch", AUTO_DEP[1], dependencies);
  }

  /**
   * Tests the output CoNLL dependencies of a Tree
   * @throws Exception
   */
  public void testCoNLLdependencies() throws Exception {
    testCoNLL(john_and_mary);
    testCoNLL(john_and_mary_saw_the_explosion);
    testCoNLL(john_saw_and_mary_heard_the_explosion);
  }

  private void testCoNLL(String[] AUTO_DEP) throws Exception {
    Sentence sentence = new Sentence(AUTO_DEP[0], grammar);
    JSONFormat.createFromSentence(sentence, grammar);
    Tree<Grammar> T = new Tree<>(AUTO_DEP[0], model, parser);
    String dependencies;

    Configuration.CONLL_DEPENDENCIES = CoNLLDependency.X1_CC___X1_X2;
    Chart.featureStructure(T, CCGcat.DepType.CoNLL, model); // CCGCat issue
    dependencies = JSONFormat.conllString(Chart.CoNLLdependencies(T, sentence), sentence.JSON);
    assertEquals("Dependency Mismatch", AUTO_DEP[2], dependencies);

    Configuration.CONLL_DEPENDENCIES = CoNLLDependency.X1_CC___CC_X2;
    Chart.featureStructure(T, CCGcat.DepType.CoNLL, model); // CCGCat issue
    dependencies = JSONFormat.conllString(Chart.CoNLLdependencies(T, sentence), sentence.JSON);
    assertEquals("Dependency Mismatch", AUTO_DEP[3], dependencies);

    Configuration.CONLL_DEPENDENCIES = CoNLLDependency.X1_X2___X2_CC;
    Chart.featureStructure(T, CCGcat.DepType.CoNLL, model); // CCGCat issue
    dependencies = JSONFormat.conllString(Chart.CoNLLdependencies(T, sentence), sentence.JSON);
    assertEquals("Dependency Mismatch", AUTO_DEP[4], dependencies);

    Configuration.CONLL_DEPENDENCIES = CoNLLDependency.X2_X1___X2_CC;
    Chart.featureStructure(T, CCGcat.DepType.CoNLL, model); // CCGCat issue
    dependencies = JSONFormat.conllString(Chart.CoNLLdependencies(T, sentence), sentence.JSON);
    assertEquals("Dependency Mismatch", AUTO_DEP[5], dependencies);

    Configuration.CONLL_DEPENDENCIES = CoNLLDependency.CC_X1___CC_X2;
    Chart.featureStructure(T, CCGcat.DepType.CoNLL, model); // CCGCat issue
    dependencies = JSONFormat.conllString(Chart.CoNLLdependencies(T, sentence), sentence.JSON);
    assertEquals("Dependency Mismatch", AUTO_DEP[6], dependencies);
  }

  public static void assertEquals(String message, String gold, String produced) {
    if (!gold.replaceAll("\\s+","").equalsIgnoreCase(produced.replaceAll("\\s+","")))
      assertFalse(message, false);
  }



  // Unary              (<T cat 0 1>
  // Binary Head Left   (<T cat 0 2>
  // Binary Head Right  (<T cat 1 2>

    private final String[] I_was_writing_code = new String[] {
        "(<T S 1 2> " +
        "(<L N PRP PRP I ##>) (<T S\\N 0 1> " +
        "(<L (S\\N)/(S\\N) VBD VBD was ##>) (<T S\\N 0 1> " +
        "(<L (S\\N)/N VBG VBG writing ##>) " +
        "(<L N NN NN code #>))))",
        "<s> 4\n" +
        "0 \t 1 \t (S\\N)/(S\\N) \t 1 \t I was\n" +
        "0 \t 2 \t (S\\N)/N \t 1 \t I writing\n" +
        "2 \t 1 \t (S\\N)/(S\\N) \t 2 \t writing was\n" +
        "3 \t 2 \t (S\\N)/N \t 2 \t code writing\n" +
        "<\\s>"
    };

    // Relative pronouns (subject extract, type-raising required)
    // code that works rocks
    //code1_NN_N that_IN_(N\.N)/(S\N) works_VB_S\N rocks_VB_S\N
    private final String[] code1_that_works_rocks = new String[] {
        "(<T S 1 2> (<T N 0 2> " +
        "(<L N NN NN code ##>) (<T N\\N 0 2> " +
        "(<L (N\\N)/(S\\N) IN IN that ##>) " +
        "(<L S\\N VB VB works ##>))) " +
        "(<L S\\N VB VB rocks ##>))",
        "<s> 4\n" +
        "0 \t 1 \t (N\\N)/(S\\N) \t 1 \t code that\n" +
        "0 \t 2 \t S\\N \t 1 \t code works <XB>\n" +
        "0 \t 3 \t S\\N \t 1 \t code rocks <XB>\n" +
        "2 \t 1 \t (N\\N)/(S\\N) \t 2 \t works that\n" +
        "<\\s>"
    };
    //code2_NN_N that_IN_(N\.N)/(S/N) works_VB_S/N rocks_VB_S\N
    private final String[] code2_that_works_rocks = new String[] {
        "(<T S 1 2> (<T N 0 2> " +
        "(<L N NN NN code ##>) (<T N\\N 0 2> " +
        "(<L (N\\N)/(S/N) IN IN that ##>) " +
        "(<L S/N VB VB works ##>))) " +
        "(<L S\\N VB VB rocks ##>))",
        "<s> 4\n" +
        "0 \t 1 \t (N\\N)/(S/N) \t 1 \t code that\n" +
        "0 \t 2 \t S/N \t 1 \t code works <XB>\n" +
        "0 \t 3 \t S\\N \t 1 \t code rocks <XB>\n" +
        "2 \t 1 \t (N\\N)/(S/N) \t 2 \t works that\n" +
        "<\\s>"
    };
    //code3_NN_N works_VB_S\N that_IN_(N\.N)\(S\N)  rocks_VB_S\N
    private final String[] code3_works_that_rocks = new String[] {
        "(<T S 1 2> (<T N 0 2> " +
        "(<L N NN NN code ##>) (<T N\\N 0 2> " +
        "(<L S\\N VB VB works ##>)" +
        "(<L (N\\N)\\(S\\N) IN IN that ##>))) " +
        "(<L S\\N VB VB rocks ##>))",
        "<s> 4\n" +
        "0 \t 1 \t S\\N \t 1 \t code works <XB>\n" +
        "0 \t 2 \t (N\\N)\\(S\\N) \t 1 \t code that\n" +
        "0 \t 3 \t S\\N \t 1 \t code rocks <XB>\n" +
        "1 \t 2 \t (N\\N)\\(S\\N) \t 2 \t works that\n" +
        "<\\s>"
    };
    //code4_NN_N works_VB_S/N that_IN_(N\.N)\(S/N)  rocks_VB_S\N
    private final String[] code4_works_that_rocks = new String[] {
        "(<T S 1 2> (<T N 0 2> " +
        "(<L N NN NN code ##>) (<T N\\N 0 2> " +
        "(<L S/N VB VB works ##>)" +
        "(<L (N\\N)\\(S/N) IN IN that ##>))) " +
        "(<L S\\N VB VB rocks ##>))",
        "<s> 4\n" +
        "0 \t 1 \t S/N \t 1 \t code works <XB>\n" +
        "0 \t 2 \t (N\\N)\\(S/N) \t 1 \t code that\n" +
        "0 \t 3 \t S\\N \t 1 \t code rocks <XB>\n" +
        "1 \t 2 \t (N\\N)\\(S/N) \t 2 \t works that\n" +
        "<\\s>"
    };
    //works_VB_S\N that_IN_(N/.N)\(S\N) code5_NN_N  rocks_VB_S\N
    private final String[] words_that_code5_rocks = new String[] {
        "(<T S 1 2> (<T N 1 2> (<T N/N 1 2> "
        + "(<L S\\N VB VB works ##>) "
        + "(<L (N/N)\\(S\\N) IN IN that ##>)) "
        + "(<L N NN NN code ##>)) "
        + "(<L S\\N VB VB rocks ##>))",
        "<s> 4\n" +
        "0 \t 1 \t (N/N)\\(S\\N) \t 2 \t works that\n" +
        "2 \t 0 \t S\\N \t 1 \t code works <XB>\n" +
        "2 \t 1 \t (N/N)\\(S\\N) \t 1 \t code that\n" +
        "2 \t 3 \t S\\N \t 1 \t code rocks <XB>\n" +
        "<\\s>"
    };
    //works_VB_S/N that_IN_(N/.N)\(S/N) code6_NN_N  rocks_VB_S\N
    private final String[] words_that_code6_rocks = new String[] {
        "(<T S 1 2> (<T N 1 2> (<T N/N 1 2> "
        + "(<L S/N VB VB works ##>) "
        + "(<L (N/N)\\(S/N) IN IN that ##>)) "
        + "(<L N NN NN code ##>)) "
        + "(<L S\\N VB VB rocks ##>))",
        "<s> 4\n" +
        "0 \t 1 \t (N/N)\\(S/N) \t 2 \t works that\n" +
        "2 \t 0 \t S/N \t 1 \t code works <XB>\n" +
        "2 \t 1 \t (N/N)\\(S/N) \t 1 \t code that\n" +
        "2 \t 3 \t S\\N \t 1 \t code rocks <XB>\n" +
        "<\\s>"
    };
    //that_IN_(N/.N)/(S\N) works_VB_S\N  code7_NN_N  rocks_VB_S\N
    private final String[] that_works_code7_rocks = new String[] {
        "(<T S 1 2> (<T N 1 2> (<T N/N 1 2> "
        + "(<L (N/N)/(S\\N) IN IN that ##>) "
        + "(<L S\\N VB VB works ##>))"
        + "(<L N NN NN code ##>)) "
        + "(<L S\\N VB VB rocks ##>))",
        "<s> 4\n" +
        "1 \t 0 \t (N/N)/(S\\N) \t 2 \t works that\n" +
        "2 \t 0 \t (N/N)/(S\\N) \t 1 \t code that\n" +
        "2 \t 1 \t S\\N \t 1 \t code works <XB>\n" +
        "2 \t 3 \t S\\N \t 1 \t code rocks <XB>\n" +
        "<\\s>"
    };
    //that_IN_(N/.N)/(S/N) works_VB_S/N  code8_NN_N  rocks_VB_S\N
    private final String[] that_works_code8_rocks = new String[] {
        "(<T S 1 2> (<T N 1 2> (<T N/N 1 2> "
        + "(<L (N/N)/(S/N) IN IN that ##>) "
        + "(<L S/N VB VB works ##>))"
        + "(<L N NN NN code ##>)) "
        + "(<L S\\N VB VB rocks ##>))",
        "<s> 4\n" +
        "1 \t 0 \t (N/N)/(S/N) \t 2 \t works that\n" +
        "2 \t 0 \t (N/N)/(S/N) \t 1 \t code that\n" +
        "2 \t 1 \t S/N \t 1 \t code works <XB>\n" +
        "2 \t 3 \t S\\N \t 1 \t code rocks <XB>\n" +
        "<\\s>"
    };
    // Relative pronouns (object extraction, subject type-raising required)
    // code that I write sucks
    //code9_NN_N that_IN_(N\.N)/(S/N) I_NN_N write_VB_(S\N)/N sucks_VB_S\N
    private final String[] code9_that_I_write_sucks = new String[] {
        "(<T S 1 2> (<T N 0 2> " +
        "(<L N NN NN code ##>) (<T N\\N 0 2> " +
        "(<L (N\\N)/(S/N) IN IN that ##>) (<T S/N 1 2> (<T S/(S\\N) 0 1> " +
        "(<L N PRP PRP I ##>)) " +
        "(<L (S\\N)/N VB VB write ##>)))) " +
        "(<L S\\N VB VB sucks ##>))",
        "<s> 5\n" +
        "0 \t 1 \t (N\\N)/(S/N) \t 1 \t code that\n" +
        "0 \t 3 \t (S\\N)/N \t 2 \t code write <XB>\n" +
        "0 \t 4 \t S\\N \t 1 \t code sucks <XB>\n" +
        "2 \t 3 \t (S\\N)/N \t 1 \t I write\n" +
        "3 \t 1 \t (N\\N)/(S/N) \t 2 \t write that\n" +
        "<\\s>"
    };
    //code10_NN_N that_IN_(N\.N)/(S\N) write_VB_(S/N)\N I_NN_N sucks_VB_S\N
    private final String[] code10_that_write_I_sucks = new String[] {
        "(<T S 1 2> (<T N 0 2> " +
        "(<L N NN NN code #>) (<T N\\N 0 2> " +
        "(<L (N\\N)/(S\\N) IN IN that ##>) (<T S\\N 0 2> " +
        "(<L (S/N)\\N VB VB write ##>) (<T S\\(S/N) 0 1> " +
        "(<L N PRP PRP I #>)))))" +
        "(<L S\\N VB VB sucks ##>))",
        "<s> 5\n" +
        "0 \t 1 \t (N\\N)/(S\\N) \t 1 \t code that\n" +
        "0 \t 2 \t (S/N)\\N \t 2 \t code write <XB>\n" +
        "0 \t 4 \t S\\N \t 1 \t code sucks <XB>\n" +
        "2 \t 1 \t (N\\N)/(S\\N) \t 2 \t write that\n" +
        "3 \t 2 \t (S/N)\\N \t 1 \t I write\n" +
        "<\\s>"
    };
    //code11_NN_N I_NN_N write_VB_(S\N)/N that_IN_(N\.N)\(S/N)  sucks_VB_S\N
    private final String[] code11_I_write_that_sucks = new String[] {
        "(<T S 1 2> (<T N 0 2> " +
        "(<L N NN NN code ##>) (<T N\\N 1 2> (<T S/N 1 2> (<T S/(S\\N) 0 1> " +
        "(<L N PRP PRP I ##>))" +
        "(<L (S\\N)/N VB VB write ##>)) " +
        "(<L (N\\N)\\(S/N) IN IN that ##>))) " +
        "(<L S\\N VB VB sucks ##>))",
        "<s> 5\n" +
        "0 \t 2 \t (S\\N)/N \t 2 \t code write <XB>\n" +
        "0 \t 3 \t (N\\N)\\(S/N) \t 1 \t code that\n" +
        "0 \t 4 \t S\\N \t 1 \t code sucks <XB>\n" +
        "1 \t 2 \t (S\\N)/N \t 1 \t I write\n" +
        "2 \t 3 \t (N\\N)\\(S/N) \t 2 \t write that\n" +
        "<\\s>"
    };
    //code12_NN_N write_VB_(S/N)\N I_NN_N that_IN_(N\.N)\(S\N)  sucks_VB_S\N
    private final String[] code12_write_I_that_sucks = new String[] {
        "(<T S 1 2> (<T N 0 2> " +
        "(<L N NN NN code ##>) (<T N\\N 1 2> (<T S\\N 0 2> " +
        "(<L (S/N)\\N VB VB write ##>) (<T S\\(S/N) 0 1>" +
        "(<L N PRP PRP I ##>))) " +
        "(<L (N\\N)\\(S\\N) IN IN that ##>)))" +
        "(<L S\\N VB VB sucks ##>))",
        "<s> 5\n" +
        "0 \t 1 \t (S/N)\\N \t 2 \t code write <XB>\n" +
        "0 \t 3 \t (N\\N)\\(S\\N) \t 1 \t code that\n" +
        "0 \t 4 \t S\\N \t 1 \t code sucks <XB>\n" +
        "1 \t 3 \t (N\\N)\\(S\\N) \t 2 \t write that\n" +
        "2 \t 1 \t (S/N)\\N \t 1 \t I write\n" +
        "<\\s>"
    };
    //I_NN_N write_VB_(S\N)/N that_IN_(N/.N)\(S/N) code13_NN_N  sucks_VB_S\N
    private final String[] I_write_that_code13_sucks = new String[] {
        "(<T S 1 2> (<T N 0 2> " +
        "(<T N/N 1 2> (<T S/N 1 2> (<T S/(S\\N) 0 1> " +
        "(<L N PRP PRP I ##>)) " +
        "(<L (S\\N)/N VB VB write ##>)) " +
        "(<L (N/N)\\(S/N) IN IN that ##>)) " +
        "(<L N NN NN code ##>))" +
        "(<L S\\N VB VB sucks ##>))",
        "<s> 5\n" +
        "0 \t 1 \t (S\\N)/N \t 1 \t I write\n" +
        "1 \t 2 \t (N/N)\\(S/N) \t 2 \t write that\n" +
        "3 \t 1 \t (S\\N)/N \t 2 \t code write <XB>\n" +
        "3 \t 2 \t (N/N)\\(S/N) \t 1 \t code that\n" +
        "3 \t 4 \t S\\N \t 1 \t code sucks <XB>\n" +
        "<\\s>"
    };
    //write_VB_(S/N)\N I_NN_N that_IN_(N/.N)\(S\N) code14_NN_N  sucks_VB_S\N
    private final String[] write_I_that_code14_sucks = new String[] {
        "(<T S 1 2> (<T N 0 2> " +
        "(<T N/N 1 2> (<T S\\N 0 2> " +
        "(<L (S/N)\\N VB VB write ##>) (<T S\\(S/N) 0 1> " +
        "(<L N PRP PRP I ##>))) " +
        "(<L (N/N)\\(S\\N) IN IN that ##>)) " +
        "(<L N NN NN code ##>))" +
        "(<L S\\N VB VB sucks ##>))",
        "<s> 5\n" +
        "0 \t 2 \t (N/N)\\(S\\N) \t 2 \t write that\n" +
        "1 \t 0 \t (S/N)\\N \t 1 \t I write\n" +
        "3 \t 0 \t (S/N)\\N \t 2 \t code write <XB>\n" +
        "3 \t 2 \t (N/N)\\(S\\N) \t 1 \t code that\n" +
        "3 \t 4 \t S\\N \t 1 \t code sucks <XB>\n" +
        "<\\s>"
    };
    //that_IN_(N/.N)/(S/N) I_NN_N write_VB_(S\N)/N  code15_NN_N  sucks_VB_S\N
    private final String[] that_I_write_code15_sucks = new String[] {
        "(<T S 1 2> (<T N 0 2> (<T N/N 0 2> " +
        "(<L (N/N)/(S/N) IN IN that ##>) (<T S/N 1 2> (<T S/(S\\N) 0 1> " +
        "(<L N PRP PRP I ##>))" +
        "(<L (S\\N)/N VB VB write ##>)))" +
        "(<L N NN NN code ##>))" +
        "(<L S\\N VB VB sucks ##>))",
        "<s> 5\n" +
        "1 \t 2 \t (S\\N)/N \t 1 \t I write\n" +
        "2 \t 0 \t (N/N)/(S/N) \t 2 \t write that\n" +
        "3 \t 0 \t (N/N)/(S/N) \t 1 \t code that\n" +
        "3 \t 2 \t (S\\N)/N \t 2 \t code write <XB>\n" +
        "3 \t 4 \t S\\N \t 1 \t code sucks <XB>\n" +
        "<\\s>"
    };
    //that_IN_(N/.N)/(S\N) write_VB_(S/N)\N I_NN_N  code16_NN_N  sucks_VB_S\N
    private final String[] that_write_I_code16_sucks = new String[] {
        "(<T S 1 2> (<T N 0 2> (<T N/N> " +
        "(<L (N/N)/(S\\N) IN IN that ##>) (<T S\\N 0 2> " +
        "(<L (S/N)\\N VB VB write ##>) (<T S\\(S/N) 0 1> " +
        "(<L N PRP PRP I ##>)))) " +
        "(<L N NN NN code ##>))" +
        "(<L S\\N VB VB sucks ##>))",
        "<s> 5\n" +
        "1 \t 0 \t (N/N)/(S\\N) \t 2 \t write that\n" +
        "2 \t 1 \t (S/N)\\N \t 1 \t I write\n" +
        "3 \t 0 \t (N/N)/(S\\N) \t 1 \t code that\n" +
        "3 \t 1 \t (S/N)\\N \t 2 \t code write <XB>\n" +
        "3 \t 4 \t S\\N \t 1 \t code sucks <XB>\n" +
        "<\\s>"
    };

    // Auxiliaries (some might be ambiguous - 'just' can sometimes attach to
    // 'should' as well as just to 'work' : code should just work
    //code17_NN_N should_MD_(S\N)/(S\N) just_RB_S/.S work_VB_S\N
    private final String[] code17_should_just_work = new String[] {
        "(<T S 1 2> " +
        "(<L N NN NN code ##>) (<T S\\N 0 2> " +
        "(<L (S\\N)/(S\\N) MD MD should ##>) (<T S\\N 1 2> " +
        "(<L S/S RB RB just ##>) " +
        "(<L S\\N VB VB work ##>))))",
        "<s> 4\n" +
        "0 \t 1 \t (S\\N)/(S\\N) \t 1 \t code should\n" +
        "0 \t 3 \t S\\N \t 1 \t code work\n" +
        "3 \t 1 \t (S\\N)/(S\\N) \t 2 \t work should\n" +
        "3 \t 2 \t S/S \t 1 \t work just\n" +
        "<\\s>"
    };
    //code18_NN_N just_RB_S/S work_VB_S\N should_MD_(S\N)\(S\N)
    private final String[] code18_just_work_should = new String[] {
        "(<T S 1 2> " +
        "(<L N NN NN code ##>)(<T S\\N 1 2> (<T S\\N 1 2> " +
        "(<L S/S RB RB just ##>)" +
        "(<L S\\N VB VB work ##>))" +
        "(<L (S\\N)\\(S\\N) MD MD should ##>)))",
        "<s> 4\n" +
        "0 \t 2 \t S\\N \t 1 \t code work\n" +
        "0 \t 3 \t (S\\N)\\(S\\N) \t 1 \t code should\n" +
        "2 \t 1 \t S/S \t 1 \t work just\n" +
        "2 \t 3 \t (S\\N)\\(S\\N) \t 2 \t work should\n" +
        "<\\s>"
    };
    //should_MD_(S/N)/(S\N) just_RB_S/.S work_VB_S\N code19_NN_N
    private final String[] should_just_work_code19 = new String[] {
        "(<T S 0 2> (<T S/N 0 2> " +
        "(<L (S/N)/(S\\N) MD MD should ##>)(<T S\\N 1 2>" +
        "(<L S/S RB RB just ##>)" +
        "(<L S\\N VB VB work ##>))) " +
        "(<L N NN NN code ##>))",
        "<s> 4\n" +
        "2 \t 0 \t (S/N)/(S\\N) \t 2 \t work should\n" +
        "2 \t 1 \t S/S \t 1 \t work just\n" +
        "3 \t 0 \t (S/N)/(S\\N) \t 1 \t code should\n" +
        "3 \t 2 \t S\\N \t 1 \t code work\n" +
        "<\\s>"
    };
    //just_RB_S/.S work_VB_S\N should_MD_(S/N)\(S\N) code20_NN_N
    private final String[] just_work_should_code20 = new String[] {
        "(<T S 0 2> (<T S/N 1 2> (<T S\\N 1 2> " +
        "(<L S/S RB RB just ##>)" +
        "(<L S\\N VB VB work ##>))" +
        "(<L (S/N)\\(S\\N) MD MD should ##>))" +
        "(<L N NN NN code ##>))",
        "<s> 4\n" +
        "1 \t 0 \t S/S \t 1 \t work just\n" +
        "1 \t 2 \t (S/N)\\(S\\N) \t 2 \t work should\n" +
        "3 \t 1 \t S\\N \t 1 \t code work\n" +
        "3 \t 2 \t (S/N)\\(S\\N) \t 1 \t code should\n" +
        "<\\s>"
    };
    //should_MD_(S/N)/(S/N) just_RB_S/.S work_VB_S/N code21_NN_N
    private final String[] should_just_work_code21 = new String[] {
        "(<T S 0 2> (<T S/N 0 2> " +
        "(<L (S/N)/(S/N) MD MD should ##>) (<T S/N 1 2> " +
        "(<L S/S RB RB just ##>)" +
        "(<L S/N VB VB work ##>))) " +
        "(<L N NN NN code ##>))",
        "<s> 4\n" +
        "2 \t 0 \t (S/N)/(S/N) \t 2 \t work should\n" +
        "2 \t 1 \t S/S \t 1 \t work just\n" +
        "3 \t 0 \t (S/N)/(S/N) \t 1 \t code should\n" +
        "3 \t 2 \t S/N \t 1 \t code work\n" +
        "<\\s>"
    };
    //just_RB_S/.S work_VB_S/N should_MD_(S/N)\(S/N) code22_NN_N
    private final String[] just_work_should_code22 = new String[] {
        "(<T S 0 2> (<T S/N 1 2> (<T S/N 1 2> " +
        "(<L S/S RB RB just ##>) " +
        "(<L S/N VB VB work ##>)) " +
        "(<L (S/N)\\(S/N) MD MD should ##>)) " +
        "(<L N NN NN code ##>))",
        "<s> 4\n" +
        "1 \t 0 \t S/S \t 1 \t work just\n" +
        "1 \t 2 \t (S/N)\\(S/N) \t 2 \t work should\n" +
        "3 \t 1 \t S/N \t 1 \t code work\n" +
        "3 \t 2 \t (S/N)\\(S/N) \t 1 \t code should\n" +
        "<\\s>"
    };
    //code23_NN_N should_MD_(S\N)/(S/N) just_RB_S/.S work_VB_S/N
    private final String[] code23_should_just_work = new String[] {
        "(<T S 1 2> " +
        "(<L N NN NN code ##>) (<T S\\N 0 2> " +
        "(<L (S\\N)/(S/N) MD MD should ##>)(<T S/N 1 2> " +
        "(<L S/S RB RB just ##>)" +
        "(<L S/N VB B work ##>))))",
        "<s> 4\n" +
        "0 \t 1 \t (S\\N)/(S/N) \t 1 \t code should\n" +
        "0 \t 3 \t S/N \t 1 \t code work\n" +
        "3 \t 1 \t (S\\N)/(S/N) \t 2 \t work should\n" +
        "3 \t 2 \t S/S \t 1 \t work just\n" +
        "<\\s>"
    };
    //code24_NN_N just_RB_S/.S work_VB_S/N should_MD_(S\N)\(S/N)
    private final String[] code24_just_work_should = new String[] {
        "(<T S 1 2> " +
        "(<L N NN NN code ##>)(<T S\\N 1 2> (<T S/N 1 2> " +
        "(<L S/S RB RB just ##>)" +
        "(<L S/N VB VB work ##>))" +
        "(<L (S\\N)\\(S/N) MD MD should ##>)))",
        "<s> 4\n" +
        "0 \t 2 \t S/N \t 1 \t code work\n" +
        "0 \t 3 \t (S\\N)\\(S/N) \t 1 \t code should\n" +
        "2 \t 1 \t S/S \t 1 \t work just\n" +
        "2 \t 3 \t (S\\N)\\(S/N) \t 2 \t work should\n" +
        "<\\s>"
    };
    // Auxiliary and relative clause
    //code25_NN_N that_IN_(N\.N)/(S/N) I_NN_N write_VB_(S\N)/N should_MD_(S\N)/(S\N) also_RB_S/.S just_RB_S/.S work_VB_S\N
    private final String[] code25_that_I_write_should_also_just_work = new String[] {
        "(<T S 1 2> (<T N 1 2> " +
        "(<L N NN NN code ##>)(<T N\\N 0 2> " +
        "(<L (N\\N)/(S/N) IN IN that ##>)(<T S/N 1 2> (<T S/(S\\N) 0 1> " +
        "(<L N PRP PRP I ##>))" +
        "(<L (S\\N)/N VB VB write ##>))))(<T S\\N 0 2> " +
        "(<L (S\\N)/(S\\N) MD MD should ##>)(<T S\\N 1 2>" +
        "(<L S/S RB RB also ##>)(<T S\\N 1 2>" +
        "(<L S/S RB RB just ##>)" +
        "(<L S\\N VB VB work ##>)))))",
        "<s> 9\n" +
        "0 \t 1 \t (N\\N)/(S/N) \t 1 \t code that\n" +
        "0 \t 3 \t (S\\N)/N \t 2 \t code write <XB>\n" +
        "0 \t 4 \t (S\\N)/(S\\N) \t 1 \t code should <XB>\n" +
        "0 \t 7 \t S\\N \t 1 \t code work <XB>\n" +
        "2 \t 3 \t (S\\N)/N \t 1 \t I write\n" +
        "3 \t 1 \t (N\\N)/(S/N) \t 2 \t write that\n" +
        "7 \t 4 \t (S\\N)/(S\\N) \t 2 \t work should\n" +
        "7 \t 5 \t S/S \t 1 \t work also\n" +
        "7 \t 6 \t S/S \t 1 \t work just\n" +
        "<\\s>"
    };

    // Conjunctions.  Particularly an issue for CoNLL style types
    // John and Mary
    private final String[] john_and_mary = new String[] {
        "(<T N 0 2> "
        + "(<L N NNP NNP John N>) (<T N[conj] 1 2> "
        + "(<L conj CC CC and conj>) "
        + "(<L N NNP NNP Mary N>)))",
          "<s> 0\n" +
          "<\\s>",
        // X1_CC___X1_X2
            "1\tJohn\t_\t_\tNNP\t_\t_\t0\tCONLL_ROOT\t_\t_\n" +
            "2\tand\t_\t_\tCC\t_\t_\t1\tCONLL_ARG:N_1\t_\t_\tfalse\n" +
            "3\tMary\t_\t_\tNNP\t_\t_\t1\tCONLL_ARG:N_1\t_\t_\tfalse",
        // X1_CC___CC_X2
            "1\tJohn\t_\t_\tNNP\t_\t_\t0\tCONLL_ROOT\t_\t_\n" +
            "2\tand\t_\t_\tCC\t_\t_\t1\tCONLL_ARG:N_1\t_\t_\tfalse\n" +
            "3\tMary\t_\t_\tNNP\t_\t_\t2\tCONLL_ARG:conj_2\t_\t_\tfalse",
        // X1_X2___X2_CC
            "1\tJohn\t_\t_\tNNP\t_\t_\t0\tCONLL_ROOT\t_\t_\n" +
            "2\tand\t_\t_\tCC\t_\t_\t3\tCONLL_ARG:N_2\t_\t_\tfalse\n" +
            "3\tMary\t_\t_\tNNP\t_\t_\t1\tCONLL_ARG:N_1\t_\t_\tfalse",
        // X2_X1___X2_CC
            "1\tJohn\t_\t_\tNNP\t_\t_\t3\tCONLL_ARG:N_1\t_\t_\tfalse\n" +
            "2\tand\t_\t_\tCC\t_\t_\t3\tCONLL_ARG:N_2\t_\t_\tfalse\n" +
            "3\tMary\t_\t_\tNNP\t_\t_\t0\tCONLL_ROOT\t_\t_",
        // CC_X1___CC_X2
            "1\tJohn\t_\t_\tNNP\t_\t_\t2\tCONLL_ARG:conj_1\t_\t_\tfalse\n" +
            "2\tand\t_\t_\tCC\t_\t_\t0\tCONLL_ROOT\t_\t_\n" +
            "3\tMary\t_\t_\tNNP\t_\t_\t2\tCONLL_ARG:conj_2\t_\t_\tfalse"
    };
    // John and Mary saw the explosion
    private final String[] john_and_mary_saw_the_explosion = new String[] {
        "(<T S 1 2> (<T N 0 2> "
        + "(<L N NNP NNP John N>) (<T N[conj] 1 2> "
        + "(<L conj CC CC and conj>) "
        + "(<L N NNP NNP Mary N>))) (<T S\\N 0 2> "
        + "(<L (S\\N)/N VBD VBD saw (S\\N)/N>) (<T N 1 2> "
        + "(<L N/N DT DT the N/N>) "
        + "(<L N NN NN explosion N>))))",
          "<s> 4\n" +
          "0 \t 3 \t (S\\N)/N \t 1 \t John saw\n" +
          "2 \t 3 \t (S\\N)/N \t 1 \t Mary saw\n" +
          "5 \t 3 \t (S\\N)/N \t 2 \t explosion saw\n" +
          "5 \t 4 \t N/N \t 1 \t explosion the\n" +
          "<\\s>",
        // X1_CC___X1_X2
            "1\tJohn\t_\t_\tNNP\t_\t_\t4\tCONLL_ARG:(S\\N)/N_1\t_\t_\tfalse\n" +
            "2\tand\t_\t_\tCC\t_\t_\t1\tCONLL_ARG:N_1\t_\t_\tfalse\n" +
            "3\tMary\t_\t_\tNNP\t_\t_\t1\tCONLL_ARG:N_1\t_\t_\tfalse\n" +
            "4\tsaw\t_\t_\tVBD\t_\t_\t0\tCONLL_ROOT\t_\t_\n" +
            "5\tthe\t_\t_\tDT\t_\t_\t6\tCONLL_MOD:N/N_1\t_\t_\ttrue\n" +
            "6\texplosion\t_\t_\tNN\t_\t_\t4\tCONLL_ARG:(S\\N)/N_2\t_\t_\tfalse",
        // X1_CC___CC_X2
            "1\tJohn\t_\t_\tNNP\t_\t_\t4\tCONLL_ARG:(S\\N)/N_1\t_\t_\tfalse\n" +
            "2\tand\t_\t_\tCC\t_\t_\t1\tCONLL_ARG:N_1\t_\t_\tfalse\n" +
            "3\tMary\t_\t_\tNNP\t_\t_\t2\tCONLL_ARG:conj_2\t_\t_\tfalse\n" +
            "4\tsaw\t_\t_\tVBD\t_\t_\t0\tCONLL_ROOT\t_\t_\n" +
            "5\tthe\t_\t_\tDT\t_\t_\t6\tCONLL_MOD:N/N_1\t_\t_\ttrue\n" +
            "6\texplosion\t_\t_\tNN\t_\t_\t4\tCONLL_ARG:(S\\N)/N_2\t_\t_\tfalse",
        // X1_X2___X2_CC
            "1\tJohn\t_\t_\tNNP\t_\t_\t4\tCONLL_ARG:(S\\N)/N_1\t_\t_\tfalse\n" +
            "2\tand\t_\t_\tCC\t_\t_\t3\tCONLL_ARG:N_2\t_\t_\tfalse\n" +
            "3\tMary\t_\t_\tNNP\t_\t_\t1\tCONLL_ARG:N_1\t_\t_\tfalse\n" +
            "4\tsaw\t_\t_\tVBD\t_\t_\t0\tCONLL_ROOT\t_\t_\n" +
            "5\tthe\t_\t_\tDT\t_\t_\t6\tCONLL_MOD:N/N_1\t_\t_\ttrue\n" +
            "6\texplosion\t_\t_\tNN\t_\t_\t4\tCONLL_ARG:(S\\N)/N_2\t_\t_\tfalse",
        // X2_X1___X2_CC
            "1\tJohn\t_\t_\tNNP\t_\t_\t3\tCONLL_ARG:N_1\t_\t_\tfalse\n" +
            "2\tand\t_\t_\tCC\t_\t_\t3\tCONLL_ARG:N_2\t_\t_\tfalse\n" +
            "3\tMary\t_\t_\tNNP\t_\t_\t4\tCONLL_ARG:(S\\N)/N_1\t_\t_\tfalse\n" +
            "4\tsaw\t_\t_\tVBD\t_\t_\t0\tCONLL_ROOT\t_\t_\n" +
            "5\tthe\t_\t_\tDT\t_\t_\t6\tCONLL_MOD:N/N_1\t_\t_\ttrue\n" +
            "6\texplosion\t_\t_\tNN\t_\t_\t4\tCONLL_ARG:(S\\N)/N_2\t_\t_\tfalse",
        // CC_X1___CC_X2
            "1\tJohn\t_\t_\tNNP\t_\t_\t2\tCONLL_ARG:conj_1\t_\t_\tfalse\n" +
            "2\tand\t_\t_\tCC\t_\t_\t4\tCONLL_ARG:(S\\N)/N_1\t_\t_\tfalse\n" +
            "3\tMary\t_\t_\tNNP\t_\t_\t2\tCONLL_ARG:conj_2\t_\t_\tfalse\n" +
            "4\tsaw\t_\t_\tVBD\t_\t_\t0\tCONLL_ROOT\t_\t_\n" +
            "5\tthe\t_\t_\tDT\t_\t_\t6\tCONLL_MOD:N/N_1\t_\t_\ttrue\n" +
            "6\texplosion\t_\t_\tNN\t_\t_\t4\tCONLL_ARG:(S\\N)/N_2\t_\t_\tfalse"
    };
    // John saw and Mary heard the explosion
    private final String[] john_saw_and_mary_heard_the_explosion = new String[]{
        "(<T S 0 2> (<T S/N 0 2> (<T S/N 1 2> (<T S/(S\\N) 0 1> " +
        "(<L N NNP NNP John N>))" +
        "(<L (S\\N)/N VBD VBD saw (S\\N)/N>)) (<T S/N[conj] 1 2>" +
        "(<L conj CC CC and conj>) (<T S/N 1 2> (<T S/(S\\N) 0 1> " +
        "(<L N NNP NNP Mary N>))" +
        "(<L (S\\N)/N VBD VBD heard (S\\N)/N>)))) (<T N 1 2> " +
        "(<L N/N DT DT the N/N>)" +
        "(<L N NN NN explosion N>)))",
        "<s> 5\n" +
        "0 \t 1 \t (S\\N)/N \t 1 \t John saw\n" +
        "3 \t 4 \t (S\\N)/N \t 1 \t Mary heard\n" +
        "6 \t 1 \t (S\\N)/N \t 2 \t explosion saw <XU>\n" +
        "6 \t 4 \t (S\\N)/N \t 2 \t explosion heard\n" +
        "6 \t 5 \t N/N \t 1 \t explosion the\n" +
        "<\\s>",
        // X1_CC___X1_X2
        "1\tJohn\t_\t_\tNNP\t_\t_\t2\tCONLL_ARG:(S\\N)/N_1\t_\t_\tfalse\n" +
        "2\tsaw\t_\t_\tVBD\t_\t_\t0\tCONLL_ROOT\t_\t_\n" +
        "3\tand\t_\t_\tCC\t_\t_\t2\tCONLL_ARG:(S\\N)/N_1\t_\t_\tfalse\n" +
        "4\tMary\t_\t_\tNNP\t_\t_\t5\tCONLL_ARG:(S\\N)/N_1\t_\t_\tfalse\n" +
        "5\theard\t_\t_\tVBD\t_\t_\t2\tCONLL_ARG:(S\\N)/N_1\t_\t_\tfalse\n" +
        "6\tthe\t_\t_\tDT\t_\t_\t7\tCONLL_MOD:N/N_1\t_\t_\ttrue\n" +
        "7\texplosion\t_\t_\tNN\t_\t_\t2\tCONLL_ARG:(S\\N)/N_2\t_\t_\tfalse",
        // X1_CC___CC_X2
        "1\tJohn\t_\t_\tNNP\t_\t_\t2\tCONLL_ARG:(S\\N)/N_1\t_\t_\tfalse\n" +
        "2\tsaw\t_\t_\tVBD\t_\t_\t0\tCONLL_ROOT\t_\t_\n" +
        "3\tand\t_\t_\tCC\t_\t_\t2\tCONLL_ARG:(S\\N)/N_1\t_\t_\tfalse\n" +
        "4\tMary\t_\t_\tNNP\t_\t_\t5\tCONLL_ARG:(S\\N)/N_1\t_\t_\tfalse\n" +
        "5\theard\t_\t_\tVBD\t_\t_\t3\tCONLL_ARG:conj_2\t_\t_\tfalse\n" +
        "6\tthe\t_\t_\tDT\t_\t_\t7\tCONLL_MOD:N/N_1\t_\t_\ttrue\n" +
        "7\texplosion\t_\t_\tNN\t_\t_\t2\tCONLL_ARG:(S\\N)/N_2\t_\t_\tfalse",
        // X1_X2___X2_CC
        "1\tJohn\t_\t_\tNNP\t_\t_\t2\tCONLL_ARG:(S\\N)/N_1\t_\t_\tfalse\n" +
        "2\tsaw\t_\t_\tVBD\t_\t_\t0\tCONLL_ROOT\t_\t_\n" +
        "3\tand\t_\t_\tCC\t_\t_\t5\tCONLL_ARG:(S\\N)/N_2\t_\t_\tfalse\n" +
        "4\tMary\t_\t_\tNNP\t_\t_\t5\tCONLL_ARG:(S\\N)/N_1\t_\t_\tfalse\n" +
        "5\theard\t_\t_\tVBD\t_\t_\t2\tCONLL_ARG:(S\\N)/N_1\t_\t_\tfalse\n" +
        "6\tthe\t_\t_\tDT\t_\t_\t7\tCONLL_MOD:N/N_1\t_\t_\ttrue\n" +
        "7\texplosion\t_\t_\tNN\t_\t_\t2\tCONLL_ARG:(S\\N)/N_2\t_\t_\tfalse",
        // X2_X1___X2_CC
        "1\tJohn\t_\t_\tNNP\t_\t_\t2\tCONLL_ARG:(S\\N)/N_1\t_\t_\tfalse\n" +
        "2\tsaw\t_\t_\tVBD\t_\t_\t5\tCONLL_ARG:(S\\N)/N_1\t_\t_\tfalse\n" +
        "3\tand\t_\t_\tCC\t_\t_\t5\tCONLL_ARG:(S\\N)/N_2\t_\t_\tfalse\n" +
        "4\tMary\t_\t_\tNNP\t_\t_\t5\tCONLL_ARG:(S\\N)/N_1\t_\t_\tfalse\n" +
        "5\theard\t_\t_\tVBD\t_\t_\t0\tCONLL_ROOT\t_\t_\n" +
        "6\tthe\t_\t_\tDT\t_\t_\t7\tCONLL_MOD:N/N_1\t_\t_\ttrue\n" +
        "7\texplosion\t_\t_\tNN\t_\t_\t5\tCONLL_ARG:(S\\N)/N_2\t_\t_\tfalse",
        // CC_X1___CC_X2
        "1\tJohn\t_\t_\tNNP\t_\t_\t2\tCONLL_ARG:(S\\N)/N_1\t_\t_\tfalse\n" +
        "2\tsaw\t_\t_\tVBD\t_\t_\t3\tCONLL_ARG:conj_1\t_\t_\tfalse\n" +
        "3\tand\t_\t_\tCC\t_\t_\t0\tCONLL_ROOT\t_\t_\n" +
        "4\tMary\t_\t_\tNNP\t_\t_\t5\tCONLL_ARG:(S\\N)/N_1\t_\t_\tfalse\n" +
        "5\theard\t_\t_\tVBD\t_\t_\t3\tCONLL_ARG:conj_2\t_\t_\tfalse\n" +
        "6\tthe\t_\t_\tDT\t_\t_\t7\tCONLL_MOD:N/N_1\t_\t_\ttrue\n" +
        "7\texplosion\t_\t_\tNN\t_\t_\t3\tCONLL_ARG:(S\\N)/N_2\t_\t_\tfalse"
    };
}