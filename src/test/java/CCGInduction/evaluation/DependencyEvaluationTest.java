package CCGInduction.evaluation;

import junit.framework.TestCase;

public class DependencyEvaluationTest extends TestCase {

  public void testScore() throws Exception {
    // System Graph (PARG format here)
    Graph syst = new Graph(7);
    syst.addEdge("2 0 N/N        1 it     No",    EdgeType.PARG);
    syst.addEdge("2 3 (S\\N)/N   1 it     was",   EdgeType.PARG);
    syst.addEdge("3 4 S\\S       1 was    n't",   EdgeType.PARG);
    syst.addEdge("6 3 (S\\N)/N   2 Monday was",   EdgeType.PARG);
    syst.addEdge("6 5 N/N        1 Monday Black", EdgeType.PARG);

    // Gold Graph (PARG format)
    Graph gold = new Graph(7);
    gold.addEdge("2 3 (S[dcl]\\NP)/NP   1 it     was",    EdgeType.PARG);
    gold.addEdge("3 0 S/S               1 was    No",     EdgeType.PARG);
    gold.addEdge("3 4 (S\\NP)\\(S\\NP)  2 was    n't",    EdgeType.PARG);
    gold.addEdge("6 3 (S[dcl]\\NP)/NP   2 Monday was",    EdgeType.PARG);
    gold.addEdge("6 5 N/N               1 Monday Black",  EdgeType.PARG);

    // Evaluate PARG vs PARG
    assertEquals(4, DependencyEvaluation.score(gold, syst, EvalMode.Undirected,     EdgeType.PARG, false));
    assertEquals(4, DependencyEvaluation.score(gold, syst, EvalMode.Directed,       EdgeType.PARG, false));
    assertEquals(3, DependencyEvaluation.score(gold, syst, EvalMode.Argument,       EdgeType.PARG, false));
    assertEquals(1, DependencyEvaluation.score(gold, syst, EvalMode.Labeled,        EdgeType.PARG, false));
    assertEquals(3, DependencyEvaluation.score(gold, syst, EvalMode.NoFeatures,     EdgeType.PARG, false));
    assertEquals(4, DependencyEvaluation.score(gold, syst, EvalMode.Simplified,     EdgeType.PARG, false));
    assertEquals(4, DependencyEvaluation.score(gold, syst, EvalMode.SimplifiedDCL,  EdgeType.PARG, false));

    // Gold Graph (C&C format)
    Graph CCgold = new Graph(7);
    CCgold.addEdge("n't_5 ((S[X]{Y}\\NP{Z}){Y}\\(S[X]{Y}<1>\\NP{Z}){Y}){_} 1 was_4 0",  EdgeType.CANDC);
    CCgold.addEdge("Black_6 (N{Y}/N{Y}<1>){_} 1 Monday_7 0",                            EdgeType.CANDC);
    CCgold.addEdge("was_4 ((S[dcl]{_}\\NP{Y}<1>){_}/NP{Z}<2>){_} 2 Monday_7 0",         EdgeType.CANDC);
    CCgold.addEdge("was_4 ((S[dcl]{_}\\NP{Y}<1>){_}/NP{Z}<2>){_} 1 it_3 0",             EdgeType.CANDC);
    CCgold.addEdge("No_1 (S[X]{Y}/S[X]{Y}<1>){_} 1 was_4 0",                            EdgeType.CANDC);

    // Evaluate PARG vs C&C
    assertEquals(4, DependencyEvaluation.score(CCgold, syst, EvalMode.Undirected,     EdgeType.CANDC, false));
    assertEquals(4, DependencyEvaluation.score(CCgold, syst, EvalMode.Directed,       EdgeType.CANDC, false));
    // note!  One higher than in CCGbank
    assertEquals(3, DependencyEvaluation.score(CCgold, syst, EvalMode.Argument,       EdgeType.CANDC, false));
    assertEquals(1, DependencyEvaluation.score(CCgold, syst, EvalMode.Labeled,        EdgeType.CANDC, false));
    assertEquals(3, DependencyEvaluation.score(CCgold, syst, EvalMode.NoFeatures,     EdgeType.CANDC, false));
    assertEquals(4, DependencyEvaluation.score(CCgold, syst, EvalMode.Simplified,     EdgeType.CANDC, false));
    assertEquals(4, DependencyEvaluation.score(CCgold, syst, EvalMode.SimplifiedDCL,  EdgeType.CANDC, false));

    // Gold Graph CoNLL format
    Graph CoNLLGold = new Graph(7);
    CoNLLGold.addEdge("1   No              _ RB RB   ADV  _ 4   VMOD" , EdgeType.CONLL);
    CoNLLGold.addEdge("2   ,               _ ,  ,    .    _ 4   P"    , EdgeType.CONLL);
    CoNLLGold.addEdge("3   it              _ PR PRP  PRON _ 4   SUB"  , EdgeType.CONLL);
    CoNLLGold.addEdge("4   was             _ VB VBD  VERB _ 0   ROOT" , EdgeType.CONLL);
    CoNLLGold.addEdge("5   n't             _ RB RB   ADV  _ 4   VMOD" , EdgeType.CONLL);
    CoNLLGold.addEdge("6   Black           _ NN NNP  NOUN _ 7   NMOD" , EdgeType.CONLL);
    CoNLLGold.addEdge("7   Monday          _ NN NNP  NOUN _ 4   PRD"  , EdgeType.CONLL);
    CoNLLGold.addEdge("8   .               _ .  .    .    _ 4   P"    , EdgeType.CONLL);

    // System Graph CoNLL format
    Graph CoNLLSyst = new Graph(7);
    CoNLLSyst.addEdge("1 no  _ RB  RB  ADV _ 3 CONLL_MOD:N/N_1 _ _ true"            , EdgeType.CONLL);
    CoNLLSyst.addEdge("2 , _ , , . _ 6 X _ _"                                       , EdgeType.CONLL);
    CoNLLSyst.addEdge("3 it  _ PR  PRP PRON  _ 4 CONLL_ARG:S\\N_1 _ _ false"        , EdgeType.CONLL);
    CoNLLSyst.addEdge("4 was _ VB  VBD VERB  _ 0 CONLL_ROOT  _ _"                   , EdgeType.CONLL);
    CoNLLSyst.addEdge("5 n't _ RB  RB  ADV _ 4 CONLL_MOD:(S\\S)/N_1 _ _ true"       , EdgeType.CONLL);
    CoNLLSyst.addEdge("6 black _ NN  NNP NOUN  _ 7 CONLL_MOD:N/N_1 _ _ true"        , EdgeType.CONLL);
    CoNLLSyst.addEdge("7 monday  _ NN  NNP NOUN  _ 5 CONLL_ARG:(S\\S)/N_2 _ _ false", EdgeType.CONLL);
    CoNLLSyst.addEdge("8 . _ . . . _ 6 X _ _ "                                      , EdgeType.CONLL);

    assertEquals(4, DependencyEvaluation.score(CoNLLGold, CoNLLSyst, EvalMode.Directed,   EdgeType.CONLL, false));
    assertEquals(4, DependencyEvaluation.score(CoNLLGold, CoNLLSyst, EvalMode.Undirected, EdgeType.CONLL, false));
  }
}