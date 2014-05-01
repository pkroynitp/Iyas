package qa.qcri.qf.tools.questionfocus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.codehaus.plexus.util.StringUtils;

import qa.qcri.qf.fileutil.ReadFile;
import qa.qcri.qf.fileutil.WriteFile;
import qa.qcri.qf.pipeline.Analyzer;
import qa.qcri.qf.pipeline.retrieval.SimpleContent;
import qa.qcri.qf.trees.RichTree;
import qa.qcri.qf.trees.TokenTree;
import qa.qcri.qf.trees.TreeSerializer;
import qa.qcri.qf.trees.nodes.RichNode;
import qa.qcri.qf.trees.nodes.RichTokenNode;
import util.Pair;

import com.google.common.collect.Lists;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public class FocusClassifier {
	
	public static final String DATA = Commons.QUESTION_FOCUS_DATA + "qf-it_train10.txt";
	
	public static final String CASES_DIRECTORY = "CASes/question-focus/train.it";
	
	private static Logger logger = Logger.getLogger(FocusClassifier.class);
		
	//private final String trainQuestionFocusPath;
	
	//private final String trainOutputPath;
		
	//private final String trainCasesDir;
	
	//private final String lang;
	
	/*
	public static Set<String> allowedTags =
			// TODO: To fill with high-freq ita postags
			new HashSet<>(Arrays.asList(new String[0]));
	*/
	
	private final Set<String> allowedTags;
	
	private static String[] emptyTagset = { };
	
	private static String[] englishTagset = { 
		//"NNPS", "VBN", "JJ", "JJS", // These postags are seen just once as focus
		"NNS", "NNP",  "NN" // These are high frequency postags
	};
	
	private static String[] italianTagset = { 
		"SN", "SP", "SPN", "SS" 
	};
	
	//private final UIMAPersistence persistence;
	
	private JCas cas;

	private Analyzer analyzer;
	
	private TreeSerializer treeSerializer =
			new TreeSerializer().enableAdditionalLabels();
	
	private Map<Integer, List<String>> examples;
	
	private Map<String, Integer> postoFreq = new HashMap<>();
	
	public FocusClassifier(Analyzer analyzer, Set<String> allowedTags) throws UIMAException {
		if (analyzer == null)
			throw new NullPointerException("analyzer is null");
		if (allowedTags == null)
			throw new NullPointerException("allowedTags is null");
		 
		this.analyzer = analyzer;
		this.allowedTags = allowedTags;
		this.cas = JCasFactory.createJCas();
		this.examples = new HashMap<>();		
	}
	/*
	public FocusClassifier(String lang) throws UIMAException {
		this(lang, new UIMANoPersistence());
	}
	*/
	
	/*
	FocusClassifier(
			String lang,
			UIMAPersistence persistence			
			//String trainQuestionFocusPath,
			//String trainOutputDir,
			//String trainCasesDir)
			) throws UIMAException {
		if (lang == null)
			throw new NullPointerException("lang is null");
		if (persistence == null)
			throw new NullPointerException("persistence is null");
		//if (trainQuestionFocusPath == null) 
		//	throw new NullPointerException("trainQuestionsPath is null");
		//if (trainOutputDir == null)
		//	throw new NullPointerException("trainOutputDir is null");

		//this.lang = lang;
		//this.trainQuestionFocusPath = trainQuestionFocusPath;
		///this.trainOutputDir = trainOutputDir;
		this.allowedTags = getTagsetByLang(lang);
		this.ae = Commons.instantiateQuestionFocusAnalyzer(lang);
		
		this.ae.setPersistence(persistence);
		this.cas = JCasFactory.createJCas();
		this.ts = new TreeSerializer().enableAdditionalLabels();
		this.examples = new HashMap<>();
	}
	*/
	
	
	
	
	
	/*
	private FocusClassifier() throws UIMAException {
		//this.ae = Commons.instantiateQuestionFocusAnalyzer("it");
		this.ae = Commons.instantiateQuestionFocusAnalyzer("it");		
		System.out.println("CASES_DIRECTORY: " + CASES_DIRECTORY);
		this.ae.setPersistence(new UIMAFilePersistence(	CASES_DIRECTORY));
		this.cas = JCasFactory.createJCas();
		this.ts = new TreeSerializer().enableAdditionalLabels();
		this.examples = new HashMap<>();
	}
	*/
	
	/**
	 * Produces the examples from a training data
	 * @param dataPath the path of the training data
	 * @return this class instance for chaining
	 */
	public FocusClassifier generateExamples(String dataPath) {
	//public FocusClassifier generateExamples() { 
		//String dataPath = trainQuestionFocusPath;
		ReadFile in = new ReadFile(dataPath);
		
		int qid = 0;
		
		while (in.hasNextLine()) {
			String line = this.filterText(in.nextLine());
		
			/**
			 * Input check
			 */
			if (line.startsWith("IMPL")) continue;
			if (StringUtils.countMatches(line, "#") != 1) continue;
			assert line.contains("#");
			
			int beginPos = line.indexOf("#");
			int endPos = line.indexOf(" ", beginPos) - 1;
			if (endPos < 0) {
				endPos = line.length() - 1;
			}
			
			String text = line.replaceAll("#", "");
			
			this.analyzer.analyze(this.cas, new SimpleContent("q-" + qid, text));
			
			TokenTree tree = RichTree.getConstituencyTree(this.cas);
			
			this.addFocusMetadata(tree, beginPos, endPos);
			
			this.examples.put(qid, produceExamples(qid, tree));
			
			qid++;			
		}
		
		in.close();
		
		return this;
	}
	
	/**
	 * Generates a list of examples from a tree with tagged focus.
	 * @param tree Tree tree from which examples are generated
	 * @param ts The serializer used to output the trees
	 * @return A list of pair of examples, and the rich token tagged in that example
	 */
	public List<Pair<String, RichTokenNode>> generateExamples(TokenTree tree,
			TreeSerializer ts) {
		List<Pair<String, RichTokenNode>> examples = new ArrayList<>();
		
		for (RichTokenNode node : tree.getTokens()) {
			
			RichNode posTag = node.getParent();
			
			if (!allowedTags.contains(posTag.getValue())) continue;
			
			posTag.addAdditionalLabel(Commons.QUESTION_FOCUS_KEY);
			
			boolean isFocus = node.getMetadata().containsKey(Commons.QUESTION_FOCUS_KEY);
			
			String label = isFocus ? "+1" : "-1";
			
			String taggedTree = ts.serializeTree(tree, Commons.getParameterList());
			
			String example = "";
			example += label;
			example += "|BT| ";
			example += taggedTree;
			example += " |ET|";		
			
			examples.add(new Pair<>(example, node));
			
			posTag.removeAdditionalLabel(Commons.QUESTION_FOCUS_KEY);
		}
		
		return examples;
				
	}
	
	/**
	 * Generates examples for a given question
	 * @param qid the id of the question
	 * @param tree the tree representation of the question
	 * @return a list of examples
	 */
	private List<String> produceExamples(int qid, TokenTree tree) {
		List<String> examples = new ArrayList<>();
		
		for (RichTokenNode node : tree.getTokens())  {
			RichNode posTag = node.getParent();
			
			if (!allowedTags.contains(posTag.getValue())) continue;
			
			posTag.addAdditionalLabel(Commons.QUESTION_FOCUS_KEY);
			
			boolean isFocus = node.getMetadata().containsKey(Commons.QUESTION_FOCUS_KEY);
			
			String label = isFocus ? "+1" : "-1";
			
			String taggedTree = treeSerializer.serializeTree(tree, Commons.getParameterList());
			
			String example = "";
			example += label;
			example += "|BT| ";
			example += taggedTree;
			example += " |ET|";
			
			examples.add(example);
			
			posTag.removeAdditionalLabel(Commons.QUESTION_FOCUS_KEY);
			
			if (isFocus) { 
				System.out.println(taggedTree);
				String pos = node.getParent().getValue();
				
				Integer freq = this.postoFreq.get(pos);
				
				if (freq == null) { 
					this.postoFreq.put(pos, 1);
				} else {
					this.postoFreq.put(pos, freq + 1);
				}						
			}
		}
		
		return examples;
	}

	/**
	 * Add a FOCUS metadata to the leaf node of the tree delimited by the given indexes
	 * @param tree The tree to augment with metadata
	 * @param beginPos The starting index of the token
	 * @param endPos The ending index of the token
	 */
	private void addFocusMetadata(TokenTree tree, int beginPos, int endPos) {
		for (RichTokenNode node : tree.getTokens()) { 
			Token token = node.getToken();
			if (token.getBegin() == beginPos && token.getEnd() == endPos) {
				node.getMetadata().put(Commons.QUESTION_FOCUS_KEY, Commons.QUESTION_FOCUS_KEY);
			}				
		}
	}

	/**
	 * Performs basic whitespace filtering on a string.
	 * @param text
	 * @return the filtered text
	 */
	private String filterText(String text) {
		String filteredText = text.trim();
		filteredText = filteredText.replaceAll(" +", " ");
		return filteredText;
	}
	
	/**
	 * Print useful statistics on the generated data
	 * @return this class instance for chaining
	 */
	public FocusClassifier printStatistics() { 
		System.out.println("F-POS\tFREQUENCY");
		for (String pos : this.postoFreq.keySet()) { 
			System.out.println(pos + "\t" + this.postoFreq.get(pos));
		}
		
		int totalNumberOfExamples = 0;
		for (List<String> exampleList : this.examples.values()) {
			totalNumberOfExamples += exampleList.size();
		}
		
		System.out.println("\nTotal number of examples: " + totalNumberOfExamples);
		
		return this;
	}
	
	/**
	 * Writes the examples on disk
	 * @param outputPath A string holding the output file
	 * @param examples The data structure containing the examples mapped to questions
	 * @return This class instance for chaining
	 */
	public FocusClassifier writeExamplesToDisk(String outputPath, 
			Map<Integer, List<String>> examples) {
		
		WriteFile out = new WriteFile(outputPath);
		
		for (Integer qid : examples.keySet()) { 
			List<String> examplesList = examples.get(qid);
			
			for (String example : examplesList) {
				out.writeLn(example);
			}
		}
		
		out.close();
		
		return this;
	}
	
	public FocusClassifier writeExamplesToDisk(String outputPath) {
		return this.writeExamplesToDisk(outputPath, this.examples);
	}
	
	/**
	 * Writes all the examples on disk, but producing several folds
	 * @param outputDir The directory which will contain the output files
	 * @param folds The number of folds to create
	 * @return This class instance for chaining
	 */
	public FocusClassifier writeExamplesInFolds(String outputDir, int foldsNum) { 
		List<Integer> questionIds = Lists.newArrayList(this.examples.keySet());
		List<List<Integer>> foldsOfIds = Lists.partition(questionIds, questionIds.size() / foldsNum);
		
		System.out.println("Total number of questions: " + questionIds.size());
		System.out.println("Number of folds: " + foldsOfIds.size());
		for (List<Integer> foldOfIds : foldsOfIds) { 
			System.out.println("Total questions in single fold: " + foldOfIds.size());
		}
		
		for (int i = 0; i < foldsNum; i++) { 
			List<Integer> trainIds = new ArrayList<>();
			List<Integer> testIds = foldsOfIds.get(i);
			
			for (int j = 0; j < foldsNum; i++) {
				if (i != j) {
					trainIds.addAll(foldsOfIds.get(j));
				}
			}
			
			this.writeExamples(outputDir + "/" + "fold-" + i + "/svm.train", trainIds);
			this.writeExamples(outputDir + "/" + "fold-" + i + "/svm.test", testIds);
		}
		
		return this;
	}

	/**
	 * Writes the examples related to the questions with the specified
	 *  question id
	 * @param outputFile The output file which will contain the examples
	 * @param qids The ids of questions to consider
	 */
	private void writeExamples(String outputFile, List<Integer> qids) {
		WriteFile out = new WriteFile(outputFile);
		for (Integer qid : qids) { 
			for (String example : this.examples.get(qid)) { 
				out.writeLn(example);
			}
		}
		out.close();
	}
	
	/*
	public static void main(String[] args) throws UIMAException { 
		new FocusClassifier()
			.generateExamples(DATA)
			.printStatistics()
			.writeExamplesToDisk(Commons.QUESTION_FOCUS_DATA + "svm.train");
	}
	*/
	
	private Set<String> getTagsetByLang(String lang) { 
		assert lang != null;
		
		String[] tagset = null;
		if (lang.equals("en")) { 
			tagset = englishTagset;
		} else if (lang.equals("it")) {
			tagset = italianTagset;		
		} else {
			tagset = emptyTagset;
		}
		
		return new TreeSet<>(Arrays.asList(tagset));
	}
	
	/**
	 * Return the Focus classifier for the specified language
	 * 
	 * @param language A string holding the document language (e.g. en, it)
	 * @return The FocusClassifier for the specified language
	 */
	public static FocusClassifier byLanguage(String language) throws UIMAException {
		if (language == null)
			throw new NullPointerException("language is null");
		
		FocusClassifier focusClassifier = null;
		Analyzer analyzer = Commons.instantiateQuestionFocusAnalyzer(language);
		switch(language) {
			case "it":
				focusClassifier = new FocusClassifierIt(analyzer);
				break;
			case "en":
				focusClassifier = new FocusClassifierEn(analyzer);
				break;
			default:
				focusClassifier = new FocusClassifierEn(analyzer);
				logger.warn("No FocusClassifier found for lang: \"" + language + "\"" + 
						"Returning FocusClassifier for the english lang.");
				break;
		}
		
		return focusClassifier;
	}

}
