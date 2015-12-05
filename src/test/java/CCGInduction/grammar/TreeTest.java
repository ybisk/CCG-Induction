package CCGInduction.grammar;

import CCGInduction.Configuration;
import CCGInduction.experiments.Action;
import CCGInduction.models.HDPArgumentModel;
import CCGInduction.parser.InductionParser;
import junit.framework.TestCase;

public class TreeTest extends TestCase {
  private HDPArgumentModel model;
  private InductionParser parser;

  @Override
  public void setUp() throws Exception {
    new Configuration("config/sample-config.properties");
    model   = new HDPArgumentModel(new Grammar());
    parser  = new InductionParser(Action.Supervised);
  }

  public void testInducedAUTOConstructor() throws Exception {
    String AUTO = "(<T S 1 2> " +
        "(<L N PRP PRP I NP>) (<T S\\N 1 2> " +
        "(<L S/.S VBP VBP yearn S/S>) (<T S\\N 1 2> " +
        "(<L S/.S TO TO to S/S_46>) (<T S\\N 1 2> " +
        "(<L (S\\N)/N VB VB learn (S\\N_69)/N_70>)" +
        "(<L N NNP NNP CCG N>) ) ) ) )";
    Tree<Grammar> T = new Tree<>(AUTO, model, parser);
    assertEquals(T.toString(model,0).trim(),
        "( S|BW_APPLY|0 " +
        "( N|PRODUCTION|0 ( PRP|LEX|0 )  )  ( S\\N|FW_XCOMPOSE|1 " +
        "( S/.S|PRODUCTION|0 ( VBP|LEX|0 )  )  ( S\\N|FW_XCOMPOSE|1 " +
        "( S/.S|PRODUCTION|0 ( TO|LEX|0 )  )  ( S\\N|FW_APPLY|0 " +
        "( (S\\N)/N|PRODUCTION|0 ( VB|LEX|0 )  )  " +
        "( N|PRODUCTION|0 ( NNP|LEX|0 )  )  )  )  )  )");
  }

  public void testCCGbankAUTOConstructor() throws Exception {
    String AUTO = "(<T S[dcl] 1 2> " +
        "(<L NP PRP PRP I NP>) (<T S[dcl]\\NP 1 2> "+
        "(<L (S[dcl]\\NP)/(S[to]\\NP) VBP VBP yearn (S[dcl]\\NP_1)/(S[to]_2\\NP_3:B)>) (<T S[to]\\NP 1 2> "+
        "(<L (S[to]\\NP)/(S[b]\\NP) TO TO to (S[to]\\NP_45)/(S[b]_46\\NP_45:B)_46>) (<T S[b]\\NP 1 2> "+
        "(<L (S[b]\\NP)/NP VB VB learn (S[b]\\NP_69)/NP_70>) (<T NP 0 1> "+
        "(<L N NNP NNP CCG N>) ) ) ) ) )";
    Tree<Grammar> T = new Tree<>(AUTO, model, parser);
    assertEquals(T.toString(model,0).trim(),
        "( S[dcl]|BW_APPLY|0 " +
        "( NP|PRODUCTION|0 ( PRP|LEX|0 )  )  ( S[dcl]\\NP|FW_APPLY|0 " +
        "( (S[dcl]\\NP)/(S[to]\\NP)|PRODUCTION|0 ( VBP|LEX|0 )  )  ( S[to]\\NP|FW_APPLY|0 " +
        "( (S[to]\\NP)/(S[b]\\NP)|PRODUCTION|0 ( TO|LEX|0 )  )  ( S[b]\\NP|FW_APPLY|0 " +
        "( (S[b]\\NP)/NP|PRODUCTION|0 ( VB|LEX|0 )  )  ( NP|TYPE_CHANGE|0 " +
        "( N|PRODUCTION|0 ( NNP|LEX|0 )  )  )  )  )  )  )");
  }
}