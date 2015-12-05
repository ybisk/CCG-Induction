package CCGInduction.parser;

import CCGInduction.Configuration;
import CCGInduction.ccg.CCGCategoryUtilities;
import CCGInduction.ccg.InducedCAT;
import CCGInduction.experiments.Action;
import CCGInduction.grammar.Binary;
import CCGInduction.grammar.Grammar;
import CCGInduction.grammar.Rule_Type;
import CCGInduction.grammar.Unary;
import CCGInduction.models.HDPArgumentModel;
import CCGInduction.utils.Logger;
import CCGInduction.utils.TextFile;
import junit.framework.TestCase;

import java.io.BufferedReader;

public class InductionParserTest extends TestCase {

  public void testCreateInductionRule() throws Exception {
    new Configuration("config/sample-config.properties");
    Grammar grammar = new Grammar();
    HDPArgumentModel model = new HDPArgumentModel(grammar);
    InductionParser parser = new InductionParser(Action.Supervised);
    InducedCAT.punc = InducedCAT.add(InducedCAT.punc, InducedCAT.COLON);
    InducedCAT.punc = InducedCAT.add(InducedCAT.punc, InducedCAT.SEMICOLON);
    InducedCAT.punc = InducedCAT.add(InducedCAT.punc, InducedCAT.COMMA);
    InducedCAT.punc = InducedCAT.add(InducedCAT.punc, InducedCAT.PERIOD);
    InducedCAT.punc = InducedCAT.add(InducedCAT.punc, InducedCAT.RRB);
    InducedCAT.punc = InducedCAT.add(InducedCAT.punc, InducedCAT.LRB);
    InducedCAT.conj_atoms = InducedCAT.add(InducedCAT.conj_atoms, InducedCAT.COLON);
    InducedCAT.conj_atoms = InducedCAT.add(InducedCAT.conj_atoms, InducedCAT.SEMICOLON);
    InducedCAT.conj_atoms = InducedCAT.add(InducedCAT.conj_atoms, InducedCAT.COMMA);
    InducedCAT.conj_atoms = InducedCAT.add(InducedCAT.conj_atoms, InducedCAT.PERIOD);
    InducedCAT.conj_atoms = InducedCAT.add(InducedCAT.conj_atoms, InducedCAT.LRB);


    BufferedReader reader = TextFile.Reader("src/main/resources/CCGbank02-21.grammar.txt");
    String line;
    String[] split;

    while ((line = reader.readLine()) != null) {
      split = Logger.whitespace_pattern.split(line);

      // Binary
      if (split.length == 6) {
        InducedCAT parent  = InducedCAT.valueOf(split[2]);
        InducedCAT leftC   = InducedCAT.valueOf(split[4]);
        InducedCAT rightC  = InducedCAT.valueOf(split[5]);
        Binary rule;
        Rule_Type type;
        try {
          type = Rule_Type.valueOf(split[0]);
          rule = parser.createSupervisedRule(model, parent, leftC, rightC);
          assertTrue(CCGCategoryUtilities.softEquality(parent, grammar.Categories.get(rule.A)));
          if (type != rule.Type) {
            System.err.println("Type Mismatch:" + rule.Type + '\t' + line);
          }
          //assertEquals(type, rule.Type);
        } catch (Exception e) {
          System.err.println(line);
        }
      } else {
        try {
          InducedCAT parent  = InducedCAT.valueOf(split[2]);
          Rule_Type type = Rule_Type.valueOf(split[0]);

          if (type == Rule_Type.PRODUCTION) {
            grammar.createRule(grammar.NT(parent), grammar.Lex(split[4]), Rule_Type.PRODUCTION);
          } else {
            InducedCAT child  = InducedCAT.valueOf(split[4]);
            Unary rule = parser.createInductionRule(model, parent, child);
            if (rule.Type != type){
              System.err.println("Type Mismatch:" + rule.Type + '\t' + line);
            }
          }
        } catch (Exception e) {
          System.err.println(line);
        }
      }
    }
  }
}