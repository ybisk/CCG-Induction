package CCGInduction;

import CCGInduction.data.JSON.JSONFormat;
import CCGInduction.utils.TextFile;
import junit.framework.TestCase;

import java.io.BufferedReader;
import java.io.File;

public class IllinoisParserTest extends TestCase {

  public void testMain() throws Exception {

    // Unsupervised from Tags (NAACL Shared Task Files)
    String[] args = new String[] {
        "config/sample-config.properties", "threshold=0.01",
        "source=induction",
        "trainFile=src/main/resources/english.example",
        "testFile=src/main/resources/english.example",
        "trainingRegimen=readTrainingFiles,HDPArgumentModel,I,I,B2Mod,IO,Test,lexicalize,IO,Test"
    };
    runExperiment(args);

    // separate train and test.
    args = new String[] {
        "config/sample-config.properties", "threshold=0.01",
        "source=induction", "longestSentence=200",
        "trainFile=src/main/resources/english.example",
        "folder=ExperimentOutput2/",
        "trainingRegimen=readTrainingFiles,HDPArgumentModel,I,I,B2Mod,IO,Save"
    };
    Main.main(args);
    args = new String[] {
        "config/sample-config.properties", "threshold=0.01",
        "source=induction", "longestTestSentence=200",
        "testFile=src/main/resources/english.example",
        "trainingRegimen=Load,Test",
        "folder=ExperimentOutput/",
        "loadModelFile=ExperimentOutput2/Model0"
    };
    Main.main(args);
    checkForParseFailures();

    File dir = new File("ExperimentOutput");
    for (File c : dir.listFiles())
      c.delete();
    dir.delete();

    dir = new File("ExperimentOutput2");
    for (File c : dir.listFiles())
      c.delete();
    dir.delete();
  }

  public void testMainSupervised() throws Exception {
    // Supervised from AUTO
    String[] args = new String[] {
        "config/sample-config.properties", "threshold=0.01",
        "source=supervised", "longestSentence=200",
        "trainFile=src/main/resources/english.AUTO.example",
        "testFile=src/main/resources/english.AUTO.example",
        "trainingRegimen=readTrainingFiles,HDPArgumentModel,lexicalize,Supervised,IO,Test"
    };
    runExperiment(args);
  }

  public void testMainSuperTags() throws Exception {
    // Semi-Supervised from JSON
    // One sentence has two parses
    // One sentence has some supertags
    String[] args = new String[] {
        "config/sample-config.properties", "threshold=0.01",
        "source=induction", "longestSentence=200",
        "trainFile=src/main/resources/english.JSON.example",
        "testFile=src/main/resources/english.JSON.example",
        "trainingRegimen=readTrainingFiles,HDPArgumentModel,I,I,B2Mod,IO,Test,lexicalize,IO,Test"
    };
    runExperiment(args);
  }

  private void runExperiment(String[] args) throws Exception {
    Main.main(args);

    File dir = new File("ExperimentOutput");
    assertTrue(dir.exists());
    assertTrue(dir.isDirectory());

    File out = new File("ExperimentOutput/Output.log");
    assertTrue(out.exists());

    File test = new File("ExperimentOutput/Test.0.1.JSON.gz");
    assertTrue(test.exists());
    checkForParseFailures();

    // cleanup
    for (File c : dir.listFiles())
      c.delete();
    dir.delete();
  }

  void checkForParseFailures() throws Exception {
    // Check # of successfully parsed sentences
    BufferedReader reader = TextFile.Reader("ExperimentOutput/Test.0.1.JSON.gz");
    String line;
    int count = 0;
    while ((line = reader.readLine()) != null) {
      JSONFormat json = JSONFormat.deSerialize(line);
      if (json.parses > 0)
        ++count;
    }
    assertTrue("No successful parses", 0 != count);
  }
}