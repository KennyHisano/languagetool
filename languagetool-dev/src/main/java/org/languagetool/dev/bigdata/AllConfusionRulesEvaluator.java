/* LanguageTool, a natural language style checker 
 * Copyright (C) 2015 Daniel Naber (http://www.danielnaber.de)
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301
 * USA
 */
package org.languagetool.dev.bigdata;

import org.apache.commons.lang.StringUtils;
import org.languagetool.JLanguageTool;
import org.languagetool.Language;
import org.languagetool.Languages;
import org.languagetool.languagemodel.LanguageModel;
import org.languagetool.languagemodel.LuceneLanguageModel;
import org.languagetool.rules.ConfusionSet;
import org.languagetool.rules.ConfusionSetLoader;
import org.languagetool.rules.ConfusionString;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

final class AllConfusionRulesEvaluator {

  private static final int MAX_SENTENCES = 1000;
  private static final int MAX_NGRAM = 3;

  private AllConfusionRulesEvaluator() {
  }

  public static void main(String[] args) throws IOException {
    if (args.length < 3 || args.length > 4) {
      System.err.println("Usage: " + ConfusionRuleEvaluator.class.getSimpleName()
              + " <langCode> <languageModelTopDir> <wikipediaXml|tatoebaFile|dir>...");
      System.err.println("   <languageModelTopDir> is a directory with sub-directories '1grams', '2grams', and '3grams' with Lucene indexes");
      System.err.println("   <wikipediaXml|tatoebaFile|dir> either a Wikipedia XML dump, or a Tatoeba file or");
      System.err.println("                      a directory with example sentences (where <word>.txt contains only the sentences for <word>).");
      System.err.println("                      You can specify both a Wikipedia file and a Tatoeba file.");
      System.exit(1);
    }
    Language lang;
    if ("en".equals(args[0])) {
      lang = new ConfusionRuleEvaluator.EnglishLight();
    } else {
      lang = Languages.getLanguageForShortName(args[0]);
    }
    LanguageModel languageModel = new LuceneLanguageModel(new File(args[1]));
    List<String> inputsFiles = new ArrayList<>();
    inputsFiles.add(args[2]);
    if (args.length >= 4) {
      inputsFiles.add(args[3]);
    }
    ConfusionRuleEvaluator eval = new ConfusionRuleEvaluator(lang, languageModel, MAX_NGRAM);
    eval.setVerboseMode(false);
    ConfusionSetLoader confusionSetLoader = new ConfusionSetLoader();
    InputStream inputStream = JLanguageTool.getDataBroker().getFromResourceDirAsStream("/en/confusion_sets.txt");
    Map<String,List<ConfusionSet>> confusionSetMap = confusionSetLoader.loadConfusionSet(inputStream);
    Set<String> done = new HashSet<>();
    for (List<ConfusionSet> entry : confusionSetMap.values()) {
      for (ConfusionSet confusionSet : entry) {
        Set<ConfusionString> set = confusionSet.getSet();
        if (set.size() != 2) {
          System.out.println("Skipping confusion set with size != 2: " + confusionSet);
        } else {
          Iterator<ConfusionString> iterator = set.iterator();
          ConfusionString set1 = iterator.next();
          ConfusionString set2 = iterator.next();
          String word1 = set1.getString();
          String word2 = set2.getString();
          String key = word1 + " " + word2;
          if (!done.contains(key)) {
            String summary = eval.run(inputsFiles, word1, word2, confusionSet.getFactor(), MAX_SENTENCES);
            String summary1 = set1.getDescription() != null ? word1 + "|" + set1.getDescription() : word1;
            String summary2 = set2.getDescription() != null ? word2 + "|" + set2.getDescription() : word2;
            String start;
            if (summary1.compareTo(summary2) < 0) {
              start = summary1 + "; " + summary2 + "; " + confusionSet.getFactor();
            } else {
              start = summary2 + "; " + summary1 + "; " + confusionSet.getFactor();
            }
            String spaces = StringUtils.repeat(" ", 82-start.length());
            System.out.println(start + spaces + "# " + summary);
          }
          done.add(key);
        }
      }
    }
  }
  
}
