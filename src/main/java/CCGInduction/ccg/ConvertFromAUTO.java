package CCGInduction.ccg;

import CCGInduction.Configuration;
import CCGInduction.data.JSON.JSONFormat;
import CCGInduction.data.Sentence;
import CCGInduction.data.Tagset;
import CCGInduction.experiments.Action;
import CCGInduction.grammar.Grammar;
import CCGInduction.models.ArgumentModel;
import CCGInduction.utils.Logger;
import CCGInduction.grammar.Tree;
import CCGInduction.parser.Chart;
import CCGInduction.parser.InductionParser;
import CCGInduction.utils.TextFile;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * A Simple class for converting AUTO parses into dependencies or TeX
 */
public class ConvertFromAUTO {

  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      System.out.println("Dear user, please provide some additional information:");
      System.out.println("java -jar hdpccg.jar ConvertFromAUTO Config.txt");
      System.out.println("Params to set:");
      System.out.println("AUTOFileToConvert=<FileOfStrings>");
      System.out.println("ConvertAUTO=PARG/CONLL/TEX");
      return;
    }
    new Configuration(args);
    Tagset.readTagMapping(Configuration.TAGSET);

    Grammar grammar = new Grammar();
    ArgumentModel model = new ArgumentModel(grammar);
    InductionParser parser = new InductionParser(Action.B3Mod_B2TR_B0Else);

    String filename = Configuration.AUTOFileToConvert;
    BufferedReader reader = TextFile.Reader(filename);
    String name = filename;
    if (name.contains("gz"))
      name = name.replace(".gz", '.' + Configuration.ConvertAUTO.toString().toLowerCase() + ".gz");
    else
      name = name + '.' + Configuration.ConvertAUTO.toString().toLowerCase();
    BufferedWriter writer = TextFile.Writer(name);
    String line;
    int count = 0;
    while ((line = reader.readLine()) != null) {
      if (line.startsWith("ID")) // Skip IDs
        continue;
      Sentence sentence = new Sentence(line, grammar);
      Tree<Grammar> parse_tree = new Tree<>(line, model, parser);

      String output = "%FAIL: " + line;
      switch (Configuration.ConvertAUTO) {
        case AUTOConversion.PARG:
          Chart.featureStructure(parse_tree, CCGcat.DepType.CCG, model); // CCGCat issue
          JSONFormat.createFromSentence(sentence, grammar);
          output = JSONFormat.pargString(Chart.CCGdependencies(parse_tree, sentence), sentence.JSON);
          break;
        case AUTOConversion.CONLL:
          Chart.featureStructure(parse_tree, CCGcat.DepType.CoNLL, model); // CCGCat issue
          JSONFormat.createFromSentence(sentence, grammar);
          output = JSONFormat.conllString(Chart.CoNLLdependencies(parse_tree, sentence), sentence.JSON) + "\n";
          break;
        case AUTOConversion.TEX:
          if (count == 0) {
            createTeXHeader(writer);
          }
          ArrayList<ArrayList<ArrayList<String>>> viterbiParse = new ArrayList<>();
          for (int i = 0; i < sentence.length(); i++) {
            viterbiParse.add(new ArrayList<>());
            for (int j = 0; j < sentence.length(); j++) {
              viterbiParse.get(i).add(new ArrayList<>());
            }
          }
          Chart.buildTeXCells(viterbiParse, model, parse_tree);
          output = Chart.buildTeX(viterbiParse, sentence, grammar);
          break;
      }
      writer.write(output + "\n");
      Logger.stat(++count);
    }
    if (Configuration.ConvertAUTO == AUTOConversion.TEX){
      closeTeXFile(writer);
    }
    reader.close();
    writer.close();
    System.out.println("Converted " + filename + "\tto\t" + name);
  }

  static void createTeXHeader(BufferedWriter writer) throws IOException {
    writer.write("\\documentclass[11pt]{beamer}\n");
    writer.write("\\usetheme{default}\n");
    writer.write("\\usepackage{ccg}\n");
    writer.write("\\usepackage[utf8x]{inputenc}\n");
    writer.write("\\usepackage[T1]{fontenc}\n");
    if (Configuration.TEX_LANGUAGE.equals("chinese")) {
      writer.write("\\usepackage{CJK}\n");
      writer.write("\\newcommand{\\chinese}{\\begin{CJK}{UTF8}{gbsn}}\n");
      writer.write("\\newcommand{\\stopchinese}{\\end{CJK}}\n");
    }
    writer.write("\\geometry{top=1mm, bottom=1mm, left=1mm, right=1mm}\n");
    writer.write("\\usepackage{adjustbox}\n");
    writer.write("\\begin{document}\n");
  }

  static void closeTeXFile(BufferedWriter writer) throws IOException {
    writer.write("\\end{document}");
  }
}
