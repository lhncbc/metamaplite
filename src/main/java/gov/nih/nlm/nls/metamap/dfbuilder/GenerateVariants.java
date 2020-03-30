package gov.nih.nlm.nls.metamap.dfbuilder;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;
import java.sql.Connection;
import java.sql.SQLException;

import gov.nih.nlm.nls.lvg.Api.LvgApi;
import gov.nih.nlm.nls.lvg.Trie.RamTrie;
import gov.nih.nlm.nls.lvg.Flows.ToFruitfulVariants;
import gov.nih.nlm.nls.lvg.Lib.LexItem;

/**
 * Describe class GenerateVariants here.
 *
 *
 * Created: Sun Dec  1 21:22:24 2019
 *
 * @author <a href="mailto:wjrogers@mail.nih.gov">Willie Rogers</a>
 * @version 1.0
 */
public class GenerateVariants {

  String configFilename =
    System.getProperty("gv.lvg.config.file", "config/lvg.properties");
  LvgApi lvgApi;
  Connection conn;
  RamTrie trieI;
  RamTrie trieD;

  /**
   * Creates a new <code>GenerateVariants</code> instance.
   */
  public GenerateVariants() {
    // System.out.println("this.configFilename: " + this.configFilename);
    this.lvgApi = new LvgApi(this.configFilename);
    this.conn = this.lvgApi.GetConnection();
    this.trieI = this.lvgApi.GetInflectionTrie();
    this.trieD = this.lvgApi.GetDerivationTrie();
  }

  /** 
   * Get word component of word line.
   *
   * @param line line from words.txt
   * @return contents of words column.
   */
  public String getWord(String line) {
    String[] fields = line.split("\\|");
    return fields[2];
  }

  /***
   * Remove any items that are the product of 'a', 'A', or "0"
   * operations and any string with leading digits.
   *
   * @param lexItemList list of lexical items
   * @return list of lexical items with numbers and abbreviations removed.
   */
  public List<LexItem> filterNumbersAndAbbreviations(List<LexItem> lexItemList) {
    List<LexItem> newLexItemList = new ArrayList<LexItem>();
    for (LexItem item: lexItemList) {
      String[] fields = item.GetMutateInformation().split("\\|");
      // System.out.println("fields[2]: " + fields[2]);
      // System.out.println("item string[0]: " +
      // item.GetOriginalTerm().charAt(0));
      if ((fields[2].indexOf("A") < 0) &&
	  (fields[2].indexOf("0") < 0) &&
	  (! Character.isDigit(item.GetOriginalTerm().charAt(0)))) {
	newLexItemList.add(item);
      }
    }
    return newLexItemList;
  }

  /**
   * List variants for term
   * @param term input term
   * @return list of variants instances for term
   */
  public List<LexItem> generateVariantListForTerm(String term) {
    LexItem inLexItem = new LexItem(term);
    try {
      return filterNumbersAndAbbreviations
	(ToFruitfulVariants.Mutate
	 (inLexItem, this.conn, this.trieI, this.trieD, true, true));
    } catch (SQLException se) {
      throw new RuntimeException(se);
    }
  }

  /**
   * Generate piped representation of variant information for lexical
   * item.
   * @param item lexical item
   * @return string containing lexical item piped representation
   */
  public String generatePipedRepresentation(LexItem item) {
    // field order [:term :tcat :word :wcat :varlevel :history]
    return item.GetOriginalTerm() + "|" +
      item.GetSourceCategory().GetName() + "|" +
      item.GetTargetTerm() + "|" +
      item.GetTargetCategory().GetName() + "|" +
      item.GetFlowHistory() + "|" +
      item.GetMutateInformation();
  }

  /** 
   * Process words file (usually words.txt), write output to variants
   * file.
   *
   * @param wordsFilename filename of input words file
   * @param varsFilename filename of output variants file
   */
  public void processWords(String wordsFilename, String varsFilename) {
    Set<String> wordSet = new TreeSet<String>();
    Charset charset = Charset.forName("utf-8");
    try {
      BufferedReader br =
	new BufferedReader(new InputStreamReader
			   (new FileInputStream(wordsFilename),
			    charset));
      PrintWriter pw =
	new PrintWriter(new OutputStreamWriter
			(new FileOutputStream(varsFilename),
			 charset));
      String line;
      while ((line = br.readLine()) != null) {
	String word = getWord(line);
	if (! wordSet.contains(word)) {
	  wordSet.add(word);
	}
      }
      Set<String> variantSet = new TreeSet<String>();
      for (String word: wordSet) {
	for (LexItem item: generateVariantListForTerm(word)) {
	  variantSet.add(generatePipedRepresentation(item));
	}
      }
      for (String variant: variantSet) {
	pw.write(variant + "\n");
      }
      br.close();
      pw.close();
    } catch (FileNotFoundException fnfe) {
      throw new RuntimeException(fnfe);
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  /**
   * Read MRCONSO files and write generated Term/Treecode file.
   * @param mrconsoFilename UMLS MRCONSO table filename
   * @param varsFilename name of variant output file
   * lines of form:
   *   term|term-categories|term-variant|variant-categories|varlevel|history|
   */
  public void process(String mrconsoFilename, String varsFilename) {
    
    String wordsFilename = System.getProperty("gv.words.temp.filename",
					      "/tmp/words.txt.tmp");

    System.out.println("Processing " + mrconsoFilename + " --> " +
		       varsFilename + ".");

    GleanMrconso gleanMrconsoInst = new GleanMrconso();
    gleanMrconsoInst.process(mrconsoFilename, wordsFilename);
    this.processWords(wordsFilename, varsFilename);
  }

  /**
   * Describe <code>main</code> method here.
   *
   * @param args a <code>String</code> value
   */
  public static final void main(final String[] args) {
    if (args.length > 1) {
      String mrconsofn = args[0];
      String varsfn = args[1];
      GenerateVariants inst = new GenerateVariants();
      inst.process(mrconsofn, varsfn);
    }
  }
}
