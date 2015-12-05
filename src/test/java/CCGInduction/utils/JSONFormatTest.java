package CCGInduction.utils;

import CCGInduction.data.JSON.JSONFormat;
import com.google.gson.Gson;
import junit.framework.TestCase;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

public class JSONFormatTest extends TestCase {
  @Rule
  public final TemporaryFolder testFolder = new TemporaryFolder();
  private static ArrayList<JSONFormat> AUTOJSON, NAACLJSON;

  public void setUp() throws Exception {
    testFolder.create();
    File autoFile = testFolder.newFile("file.auto");
    OutputStreamWriter writer =  new OutputStreamWriter(new FileOutputStream(autoFile), "UTF-8");
    // Write a successful CCGbank parse
    String parse1 = "(<T S[dcl] 0 2> (<T S[dcl] 1 2> " +
        "(<L S/S RB RB No S_42/S_42>) (<T S[dcl] 1 2> " +
        "(<L , , , , ,>) (<T S[dcl] 1 2> " +
        "(<L NP PRP PRP it NP>) (<T S[dcl]\\NP 0 2> (<T (S[dcl]\\NP)/NP 0 2> " +
        "(<L (S[dcl]\\NP)/NP VBD VBD was (S[dcl]\\NP_8)/NP_9>) " +
        "(<L (S\\NP)\\(S\\NP) RB RB n't (S_21\\NP_16)_21\\(S_21\\NP_16)_21>) ) (<T NP 0 1> (<T N 1 2> " +
        "(<L N/N NNP NNP Black N_30/N_30>) " +
        "(<L N NNP NNP Monday N>) ) ) ) ) ) ) " +
        "(<L . . . . .>) )";
    writer.write("ID=Test.1 PARSER=TEST NUMPARSE=1\n" + parse1 + '\n');
    // Write a parse failure
    writer.write("ID=Test.2 PARSER=TEST NUMPARSE=0\n");
    // Write a successful C&C parse
    String parse2 = "(<T S[dcl] rp 0 2> (<T S[dcl] ba 1 2> " +
        "(<L NP It it PRP O I-NP NP>) (<T S[dcl]\\NP fa 0 2> " +
        "(<L (S[dcl]\\NP)/(S[pss]\\NP) was be VBD O I-VP (S[dcl]\\NP)/(S[pss]\\NP)>) (<T S[pss]\\NP ba 0 2> " +
        "(<L S[pss]\\NP planned plan VBN O I-VP S[pss]\\\\NP>) (<T (S\\NP)\\(S\\NP) fa 0 2> " +
        "(<L ((S\\NP)\\(S\\NP))/NP by by IN O I-PP ((S[X]\\NP)\\(S[X]\\NP))/NP>) (<T NP ba 0 2> (<T NP lex 0 1> " +
        "(<L N Thomas_Hughes Thomas_Hughes NNP I-ORG I-NP N>)) (<T NP[conj] conj 0 2> " +
        "(<L , , , , O O ,>) (<T NP ba 0 2> (<T NP[nb] fa 1 2> " +
        "(<L NP[nb]/N a a DT O I-NP NP[nb]/N>) (<T N ba 0 2> (<T N fa 1 2> " +
        "(<L N/N British British NNP I-ORG I-NP N/N>) (<T N fa 1 2> " +
        "(<L N/N social social JJ O I-NP N/N>) " +
        "(<L N reformer reformer NN O I-NP N>))) (<T N[conj] conj 0 2> " +
        "(<L conj and and CC O O conj>) " +
        "(<L N author author NN O I-NP N>)))) (<T NP\\NP fa 0 2> " +
        "(<L (NP\\NP)/NP of of IN O I-PP (NP\\NP)/NP>) (<T NP lex 0 1> " +
        "(<L N Tom_Brown_Schooldays Tom_Brown_Schooldays NNP I-ORG I-NP N>)))))))))) " +
        "(<L . . . . O O .>))";
    writer.write("ID=Test.3 PARSER=TEST NUMPARSE=1\n" + parse2 + '\n');
    writer.close();

    File naaclFile = testFolder.newFile("file.naacl");
    writer =  new OutputStreamWriter(new FileOutputStream(naaclFile), "UTF-8");
    // English without dependencies
    writer.write(
    "1       There   _       EX      EX      DET     _       _       _\n" +
    "2       is      _       VB      VBZ     VERB    _       _       _\n" +
    "3       no      _       DT      DT      DET     _       _       _\n" +
    "4       asbestos        _       NN      NN      NOUN    _       _       _\n" +
    "5       in      _       IN      IN      ADP     _       _       _\n" +
    "6       our     _       PR      PRP$    PRON    _       _       _\n" +
    "7       products        _       NN      NNS     NOUN    _       _       _\n" +
    "8       now     _       RB      RB      ADV     _       _       _\n" +
    "9       .       _       .       .       .       _       _       _\n" +
    "10      ''      _       ''      ''      .       _       _       _\n");
    writer.write("\n");
    // German with dependencies
    writer.write(
    "1       Statt   _       KOUI    KOUI    CONJ    _       4       CP      4       CP\n" +
    "2       Details _       NN      NN      NOUN    _       4       OA      4       OA\n" +
    "3       zu      _       PTKZU   PTKZU   PRT     _       4       PM      4       PM\n" +
    "4       nennen  _       VVINF   VVINF   VERB    _       6       MO      6       MO\n" +
    "5       ,       _       $,      $,      .       _       6       PUNC    6       PUNC\n" +
    "6       wiederholt      _       VVFIN   VVFIN   VERB    _       0       ROOT    0       ROOT\n" +
    "7       er      _       PPER    PPER    PRON    _       6       SB      6       SB\n" +
    "8       unverdrossen    _       ADJD    ADJD    ADJ     _       6       MO      6       MO\n" +
    "9       die     _       ART     ART     DET     _       11      NK      11      NK\n" +
    "10      ``      _       $(      $(      .       _       11      PUNC    11      PUNC\n" +
    "11      Erfolgsformel   _       NN      NN      NOUN    _       6       OA      6       OA\n" +
    "12      ''      _       $(      $(      .       _       6       PUNC    6       PUNC\n" +
    "13      :       _       $.      $.      .       _       6       PUNC    6       PUNC\n");
    writer.close();


    // Build JSON objects
    String AUTOserialized1 =
        "{\"words\": [" +
            "{\"cat\": \"S/S\", \"pos\": \"RB\", \"word\": \"No\"}," +
            "{\"cat\": \",\", \"pos\": \",\", \"word\": \",\"}," +
            "{\"cat\": \"NP\", \"pos\": \"PRP\", \"word\": \"it\"}," +
            "{\"cat\": \"(S[dcl]\\\\NP)/NP\", \"pos\": \"VBD\", \"word\": \"was\"}," +
            "{\"cat\": \"(S\\\\NP)\\\\(S\\\\NP)\", \"pos\": \"RB\", \"word\": \"n't\"}," +
            "{\"cat\": \"N/N\", \"pos\": \"NNP\", \"word\": \"Black\"}," +
            "{\"cat\": \"N\", \"pos\": \"NNP\", \"word\": \"Monday\"}," +
            "{\"cat\": \".\", \"pos\": \".\", \"word\": \".\"}" +
            "], \"synPars\": [{\"synPar\": \"" + parse1.replace("\\", "\\\\") + "\", \"score\":1.0}]}";
    String AUTOserialized2 =
        "{\"words\": [" +
            "{\"cat\": \"NP\", \"word\": \"It\", \"pos\":\"PRP\"}," +
            "{\"cat\": \"(S[dcl]\\\\NP)/(S[pss]\\\\NP)\" , \"word\": \"was\", \"pos\": \"VBD\"}," +
            "{\"cat\": \"S[pss]\\\\NP\" , \"word\": \"planned\", \"pos\": \"VBN\"}," +
            "{\"cat\": \"((S\\\\NP)\\\\(S\\\\NP))/NP\" , \"word\": \"by\", \"pos\": \"IN\"}," +
            "{\"cat\": \"N\" , \"word\": \"Thomas_Hughes\", \"pos\": \"NNP\"}," +
            "{\"cat\": \",\" , \"word\": \",\", \"pos\": \",\"}," +
            "{\"cat\": \"NP[nb]/N\" , \"word\": \"a\", \"pos\": \"DT\"}," +
            "{\"cat\": \"N/N\" , \"word\": \"British\", \"pos\": \"NNP\"}," +
            "{\"cat\": \"N/N\" , \"word\": \"social\", \"pos\": \"JJ\"}," +
            "{\"cat\": \"N\" , \"word\": \"reformer\", \"pos\": \"NN\"}," +
            "{\"cat\": \"conj\" , \"word\": \"and\", \"pos\": \"CC\"}," +
            "{\"cat\": \"N\" , \"word\": \"author\", \"pos\": \"NN\"}," +
            "{\"cat\": \"(NP\\\\NP)/NP\" , \"word\": \"of\", \"pos\": \"IN\"}," +
            "{\"cat\": \"N\" , \"word\": \"Tom_Brown_Schooldays\", \"pos\": \"NNP\"}," +
            "{\"cat\": \".\" , \"word\": \".\", \"pos\": \".\"}"+
            "], \"synPars\": [{\"synPar\": \"" + parse2.replace("\\", "\\\\") + "\", \"score\":1.0}]}";
    String NAACLserialized1 =
        "{\"words\": [" +
            "{\"word\": \"There\", \"lemma\": \"_\", \"cpos\": \"EX\", \"pos\": \"EX\", \"upos\": \"DET\"}," +
            "{\"word\": \"is\", \"lemma\": \"_\", \"cpos\": \"VB\", \"pos\": \"VBZ\", \"upos\": \"VERB\"}," +
            "{\"word\": \"no\", \"lemma\": \"_\", \"cpos\": \"DT\", \"pos\": \"DT\", \"upos\": \"DET\"}," +
            "{\"word\": \"asbestos\", \"lemma\": \"_\", \"cpos\": \"NN\", \"pos\": \"NN\", \"upos\": \"NOUN\"}," +
            "{\"word\": \"in\", \"lemma\": \"_\", \"cpos\": \"IN\", \"pos\": \"IN\", \"upos\": \"ADP\"}," +
            "{\"word\": \"our\", \"lemma\": \"_\", \"cpos\": \"PR\", \"pos\": \"PRP$\", \"upos\": \"PRON\"}," +
            "{\"word\": \"products\", \"lemma\": \"_\", \"cpos\": \"NN\", \"pos\": \"NNS\", \"upos\": \"NOUN\"}," +
            "{\"word\": \"now\", \"lemma\": \"_\", \"cpos\": \"RB\", \"pos\": \"RB\", \"upos\": \"ADV\"}," +
            "{\"word\": \".\", \"lemma\": \"_\", \"cpos\": \".\", \"pos\": \".\", \"upos\": \".\"}," +
            "{\"word\": \"''\", \"lemma\": \"_\", \"cpos\": \"''\", \"pos\": \"''\", \"upos\": \".\"}" +
            "]}";
    String NAACLserialized2 =
        "{\"words\": [" +
            "{\"word\": \"Statt\", \"lemma\": \"_\", \"cpos\": \"KOUI\", \"pos\": \"KOUI\", \"upos\": \"CONJ\"}," +
            "{\"word\": \"Details\", \"lemma\": \"_\", \"cpos\": \"NN\", \"pos\": \"NN\", \"upos\": \"NOUN\"}," +
            "{\"word\": \"zu\", \"lemma\": \"_\", \"cpos\": \"PTKZU\", \"pos\": \"PTKZU\", \"upos\": \"PRT\"}," +
            "{\"word\": \"nennen\", \"lemma\": \"_\", \"cpos\": \"VVINF\", \"pos\": \"VVINF\", \"upos\": \"VERB\"}," +
            "{\"word\": \",\", \"lemma\": \"_\", \"cpos\": \"$,\", \"pos\": \"$,\", \"upos\":\".\"}," +
            "{\"word\": \"wiederholt\", \"lemma\": \"_\", \"cpos\": \"VVFIN\", \"pos\": \"VVFIN\", \"upos\": \"VERB\"}," +
            "{\"word\": \"er\", \"lemma\": \"_\", \"cpos\": \"PPER\", \"pos\": \"PPER\", \"upos\": \"PRON\"}," +
            "{\"word\": \"unverdrossen\", \"lemma\": \"_\", \"cpos\": \"ADJD\", \"pos\": \"ADJD\", \"upos\": \"ADJ\"}," +
            "{\"word\": \"die\", \"lemma\": \"_\", \"cpos\": \"ART\", \"pos\": \"ART\", \"upos\": \"DET\"}," +
            "{\"word\": \"``\", \"lemma\": \"_\", \"cpos\": \"$(\", \"pos\": \"$(\", \"upos\": \".\"}," +
            "{\"word\": \"Erfolgsformel\", \"lemma\": \"_\", \"cpos\": \"NN\", \"pos\": \"NN\", \"upos\": \"NOUN\"}," +
            "{\"word\": \"''\", \"lemma\": \"_\", \"cpos\": \"$(\", \"pos\": \"$(\", \"upos\": \".\"}," +
            "{\"word\": \":\", \"lemma\": \"_\", \"cpos\": \"$.\", \"pos\": \"$.\", \"upos\": \".\"}" +
            "],\"synPars\":[{\"conllParse\":[" +
            "{\"index\":1,\"head\":4,\"label\":\"CP\"}," +
            "{\"index\":2,\"head\":4,\"label\":\"OA\"}," +
            "{\"index\":3,\"head\":4,\"label\":\"PM\"}," +
            "{\"index\":4,\"head\":6,\"label\":\"MO\"}," +
            "{\"index\":5,\"head\":6,\"label\":\"PUNC\"}," +
            "{\"index\":6,\"head\":0,\"label\":\"ROOT\"}," +
            "{\"index\":7,\"head\":6,\"label\":\"SB\"}," +
            "{\"index\":8,\"head\":6,\"label\":\"MO\"}," +
            "{\"index\":9,\"head\":11,\"label\":\"NK\"}," +
            "{\"index\":10,\"head\":11,\"label\":\"PUNC\"}," +
            "{\"index\":11,\"head\":6,\"label\":\"OA\"}," +
            "{\"index\":12,\"head\":6,\"label\":\"PUNC\"}," +
            "{\"index\":13,\"head\":6,\"label\":\"PUNC\"}],\"score\":0.0}],\"parses\":0.0}";
    Gson gson = new Gson();
    AUTOJSON = new ArrayList<>(2);
    AUTOJSON.add(gson.fromJson(AUTOserialized1, JSONFormat.class));
    AUTOJSON.add(gson.fromJson(AUTOserialized2, JSONFormat.class));
    NAACLJSON = new ArrayList<>(2);
    NAACLJSON.add(gson.fromJson(NAACLserialized1, JSONFormat.class));
    NAACLJSON.add(gson.fromJson(NAACLserialized2, JSONFormat.class));
  }

  public void testReadAUTO() throws Exception {
    ArrayList<JSONFormat> read = JSONFormat.readAUTO(testFolder.getRoot() + "/file.auto");
    assertEquals(read.size(), AUTOJSON.size());
    // CCG
    assertTrue(read.get(0).equals(AUTOJSON.get(0)));
    // C&C
    assertTrue(read.get(1).equals(AUTOJSON.get(1)));
  }

  public void testReadCoNLL() throws Exception {
    ArrayList<JSONFormat> read = JSONFormat.readCoNLL(testFolder.getRoot() + "/file.naacl", true);
    assertEquals(read.size(), NAACLJSON.size());
    assertTrue(read.get(0).equals(NAACLJSON.get(0)));
    assertTrue(read.get(1).equals(NAACLJSON.get(1)));
  }
}