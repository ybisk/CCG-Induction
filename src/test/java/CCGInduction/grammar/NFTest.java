package CCGInduction.grammar;

import CCGInduction.experiments.Action;
import CCGInduction.Configuration;
import CCGInduction.ccg.InducedCAT;
import CCGInduction.data.JSON.JSONFormat;
import CCGInduction.data.Sentence;
import CCGInduction.data.Sentences;
import CCGInduction.data.Tagset;
import CCGInduction.models.HDPArgumentModel;
import CCGInduction.parser.InductionChart;
import CCGInduction.parser.InductionParser;
import junit.framework.TestCase;

public class NFTest extends TestCase {
  private Grammar grammar;
  private HDPArgumentModel model;
  private InductionParser<Grammar> parser;

  private final InducedCAT N = new InducedCAT(InducedCAT.N);
  private final InducedCAT S = new InducedCAT(InducedCAT.S);

  @Override
  public void setUp() throws Exception {
    Tagset.readTagMapping("src/main/resources/english.pos.map");
    new Configuration("config/sample-config.properties");
    grammar = new Grammar();
    model   = new HDPArgumentModel(grammar);
    parser  = new InductionParser(Action.B3Mod_B2TR_B0Else);

    grammar.TOP = grammar.NT(new InducedCAT(InducedCAT.TOP));
    grammar.createRule(grammar.TOP, grammar.NT(N), Rule_Type.TYPE_TOP);
    grammar.createRule(grammar.TOP, grammar.NT(S), Rule_Type.TYPE_TOP);
    grammar.createRule(grammar.TOP, grammar.NT(InducedCAT.valueOf("S/N")), Rule_Type.TYPE_TOP);
    grammar.createRule(grammar.TOP, grammar.NT(InducedCAT.valueOf("((S/S)/N)/N")), Rule_Type.TYPE_TOP);
    Configuration.complexTOP = true;
  }

  public void testBinaryNF() throws Exception {
    // NF Constraint 1 (B0 and Bn>=1):
    // The output of >B^n (n >= 1) cannot be the primary functor for >B^n (n <= 1)
    JSONFormat JSON = new JSONFormat();
    JSON.addWord("NN","NN","NN","NN","NOUN","N/N",null);
    JSON.addWord("NN","NN","NN","NN","NOUN","N/N",null);
    JSON.addWord("NN","NN","NN","NN","NOUN","N/N",null);
    JSON.addWord("NN","NN","NN","NN","NOUN","N",null);
    Sentence sentence = new Sentence();
    Sentences.readJSONSentence(JSON.toString(), sentence, grammar);

    InductionChart<Grammar> chart = new InductionChart<>(sentence, model);
    parser.parse(model, chart);
    assertEquals(1.0, chart.parses);

    //   N   N\N    N\N
    JSON = new JSONFormat();
    JSON.addWord("NN","NN","NN","NN","NOUN","N",null);
    JSON.addWord("NN","NN","NN","NN","NOUN","N\\N",null);
    JSON.addWord("NN","NN","NN","NN","NOUN","N\\N",null);
    JSON.addWord("NN","NN","NN","NN","NOUN","N\\N",null);
    sentence = new Sentence();
    Sentences.readJSONSentence(JSON.toString(), sentence, grammar);

    chart = new InductionChart<>(sentence, model);
    parser.parse(model, chart);
    assertEquals(1.0, chart.parses);

    // N/N & N/N  N
    JSON = new JSONFormat();
    JSON.addWord("NN","NN","NN","NN","NOUN","N/N",null);
    JSON.addWord("NN","NN","NN","NN","NOUN","N/N",null);
    JSON.addWord("CC","CC","CC","CC","CONJ","conj",null);
    JSON.addWord("NN","NN","NN","NN","NOUN","N/N",null);
    JSON.addWord("NN","NN","NN","NN","NOUN","N",null);
    sentence = new Sentence();
    Sentences.readJSONSentence(JSON.toString(), sentence, grammar);

    chart = new InductionChart<>(sentence, model);
    parser.parse(model, chart);
    assertEquals(1.0, chart.parses);

    // N/N , N/N  N
    JSON = new JSONFormat();
    JSON.addWord("NN","NN","NN","NN","NOUN","N/N",null);
    JSON.addWord("NN","NN","NN","NN","NOUN","N/N",null);
    JSON.addWord(",", "," ,"," ,"," ,"."   ,","  ,null);
    JSON.addWord("NN","NN","NN","NN","NOUN","N/N",null);
    JSON.addWord("NN","NN","NN","NN","NOUN","N"  ,null);
    sentence = new Sentence();
    Sentences.readJSONSentence(JSON.toString(), sentence, grammar);

    chart = new InductionChart<>(sentence, model);
    parser.parse(model, chart);
    // N/N ,  <-- Max projection
    // , N/N  <-- Max projection
    // , N/N  N  <-- Not allowed
    assertEquals(2.0, chart.parses);

    // NF Constraint 2: (B1 and Bn>=1)
    // The output of >B1 cannot be the primary functor for >B^n>=1
    // N S/S S/S & S/S  (S\N)/N N
    JSON = new JSONFormat();
    JSON.addWord("NN","NN","NN","NN","NOUN","N"  ,null);
    JSON.addWord("VB","VB","VB","VB","VERB","S/.S"    ,null);
    JSON.addWord("VB","VB","VB","VB","VERB","S/.S"    ,null);
    JSON.addWord("CC","CC","CC","CC","CONJ","conj"    ,null);
    JSON.addWord("VB","VB","VB","VB","VERB","S/.S"    ,null);
    JSON.addWord("VB","VB","VB","VB","VERB","(S\\N)/N",null);
    JSON.addWord("NN","NN","NN","NN","NOUN","N"  ,null);
    sentence = new Sentence();
    Sentences.readJSONSentence(JSON.toString(), sentence, grammar);

    chart = new InductionChart<>(sentence, model);
    parser.parse(model, chart);
    assertEquals(1.0, chart.parses);

    // N S/S S/S , S/S  (S\N)/N N
    JSON = new JSONFormat();
    JSON.addWord("NN","NN","NN","NN","NOUN","N"  ,null);
    JSON.addWord("VB","VB","VB","VB","VERB","S/.S"    ,null);
    JSON.addWord("VB","VB","VB","VB","VERB","S/.S"    ,null);
    JSON.addWord(",", "," ,"," ,"," ,"."   ,","       ,null);
    JSON.addWord("VB","VB","VB","VB","VERB","S/.S"    ,null);
    JSON.addWord("VB","VB","VB","VB","VERB","(S\\N)/N",null);
    JSON.addWord("NN","NN","NN","NN","NOUN","N"  ,null);
    sentence = new Sentence();
    Sentences.readJSONSentence(JSON.toString(), sentence, grammar);

    chart = new InductionChart<>(sentence, model);
    parser.parse(model, chart);
    assertEquals(2.0, chart.parses);

    // NF Constraint 3: (Bn>=1 and Bm>1)
    // The output of >Bm cannot be the secondary functor for >B^n>m
    // TODO:  Not accessible with current restrictions
//    JSON = new JSONWordsParsesAndEntities();
//    JSON.addWord("NN","NN","NN","NN","NOUN","S/N"           ,null);
//    JSON.addWord("VB","VB","VB","VB","VERB","(S/N)/(S/N)"   ,null);
//    JSON.addWord("NN","NN","NN","NN","NOUN","((S/N)/N)/N"   ,null);
//    sentence = new Sentence();
//    Sentences.readJSONSentence(JSON.toString(), sentence, grammar);
//
//    chart = new InductionChart(sentence, model, false);
//    parser.parse(chart);
//    assertEquals(1.0, chart.parses);


    grammar.createRule(
        grammar.NT(new InducedCAT(InducedCAT.S).forward(new InducedCAT(InducedCAT.S).backward(new InducedCAT(InducedCAT.N)))),
        grammar.NT(new InducedCAT(InducedCAT.N)), Rule_Type.FW_TYPERAISE);
    Configuration.typeRaising = true;
    // NF Constraint 4: (T and Bn>0)
    // The output of >T cannot be the primary input to >B^n>0 if the secondary input is the output of <B^m>n
    JSON = new JSONFormat();
    JSON.addWord("NN","NN","NN","NN","NOUN","N"  ,null);
    JSON.addWord("VB","VB","VB","VB","VERB","(S\\N)/N",null);
    JSON.addWord("NN","NN","NN","NN","NOUN","N"  ,null);
    sentence = new Sentence();
    Sentences.readJSONSentence(JSON.toString(), sentence, grammar);

    chart = new InductionChart<>(sentence, model);
    parser.parse(model, chart);
    assertEquals(1.0, chart.parses);

    // NF Constraint 5: (T and B0)
    // The output of forward type-raising >T cannot be the functor in application >B0
    // N (S\N)/N N
    JSON = new JSONFormat();
    JSON.addWord("NN","NN","NN","NN","NOUN","N"         ,null);
    JSON.addWord("VB","VB","VB","VB","VERB","(S\\N)/N"  ,null);
    JSON.addWord("VB","VB","VB","VB","VERB","S\\.S"     ,null);
    sentence = new Sentence();
    Sentences.readJSONSentence(JSON.toString(), sentence, grammar);

    chart = new InductionChart<>(sentence, model);
    parser.parse(model, chart);
    assertEquals(1.0, chart.parses);

    // N , (S\N)/N N    --- Punctuation should not fuck up the type-raising information/constraint
    JSON = new JSONFormat();
    JSON.addWord("NN","NN","NN","NN","NOUN","N"       ,null);
    JSON.addWord(",", "," ,"," ,"," ,"."   ,","       ,null);
    JSON.addWord("VB","VB","VB","VB","VERB","(S\\N)/N",null);
    JSON.addWord("NN","NN","NN","NN","NOUN","N"       ,null);
    sentence = new Sentence();
    Sentences.readJSONSentence(JSON.toString(), sentence, grammar);

    chart = new InductionChart<>(sentence, model);
    parser.parse(model, chart);
    assertEquals(1.0, chart.parses);

    // NF Constraint 6: (T and coord)
    // The result of coordination cannot be typ
    //code9_NN_N that_IN_(N\.N)/(S/N) I_NN_N write_VB_(S\N)/N sucks_VB_S\N
    JSON = new JSONFormat();
    JSON.addWord("NN","NN","NN","NN","NOUN","N"               ,null);
    JSON.addWord("IN","IN","IN","IN","ADP" ,"(N\\.N)/(S/N)"   ,null);
    JSON.addWord("NN","NN","NN","NN","NOUN","N"               ,null);
    JSON.addWord("CC","CC","CC","CC","CONJ","conj"            ,null);
    JSON.addWord("NN","NN","NN","NN","NOUN","N"               ,null);
    JSON.addWord("VB","VB","VB","VB","VERB","(S\\N)/N"        ,null);
    JSON.addWord("VB","VB","VB","VB","VERB","S\\N"            ,null);
    sentence = new Sentence();
    Sentences.readJSONSentence(JSON.toString(), sentence, grammar);

    chart = new InductionChart<>(sentence, model);
    parser.parse(model, chart);
    assertEquals(1.0, chart.parses);

    // New Constraint ( no double BW_CONJ )
    JSON = new JSONFormat();
    JSON.addWord("NN","NN","NN","NN","NOUN","N"       ,null);
    JSON.addWord("CC","CC","CC","CC","CONJ","conj"    ,null);
    JSON.addWord("NN","NN","NN","NN","NOUN","N"       ,null);
    JSON.addWord(",", "," ,"," ,"," ,"."   ,"conj"    ,null);
    JSON.addWord("CC","CC","CC","CC","CONJ","conj"    ,null);
    JSON.addWord("NN","NN","NN","NN","NOUN","N"       ,null);
    sentence = new Sentence();
    Sentences.readJSONSentence(JSON.toString(), sentence, grammar);

    chart = new InductionChart<>(sentence, model);
    parser.parse(model, chart);
    assertEquals(1.0, chart.parses);
  }

}