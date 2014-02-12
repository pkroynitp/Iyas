package qa.qcri.qf.pipeline.trec;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.uima.UIMAException;

import qa.qcri.qf.datagen.rr.Reranking;
import qa.qcri.qf.datagen.rr.RerankingTest;
import qa.qcri.qf.datagen.rr.RerankingTrain;
import qa.qcri.qf.features.PairFeatureFactory;
import qa.qcri.qf.fileutil.FileManager;
import qa.qcri.qf.pipeline.Analyzer;
import qa.qcri.qf.pipeline.serialization.UIMAFilePersistence;
import qa.qcri.qf.pipeline.serialization.UIMANoPersistence;
import qa.qcri.qf.pipeline.serialization.UIMAPersistence;
import qa.qcri.qf.trees.TreeSerializer;
import qa.qcri.qf.trees.nodes.RichNode;
import qa.qcri.qf.trees.providers.PosChunkTreeProvider;
import cc.mallet.types.Alphabet;

import com.google.common.base.Joiner;
import com.google.common.io.Files;

public class TrecPipelineRunner {

	private static final String ARGUMENTS_FILE_OPT = "argumentsFilePath";
	private static final String TRAINING_QUESTIONS_PATH_OPT = "trainQuestionsPath";
	private static final String TRAINING_CANDIDATES_PATH_OPT = "trainCandidatesPath";
	private static final String TRAINING_CASES_DIR_OPT = "trainCasesDir";
	private static final String TRAINING_OUTPUT_DIR_OPT = "trainOutputDir";
	private static final String TEST_QUESTIONS_PATH_OPT = "testQuestionsPath";
	private static final String TEST_CANDIDATES_PATH_OPT = "testCandidatesPath";
	private static final String TEST_CASES_DIR_OPT = "testCasesDir";
	private static final String TEST_OUTPUT_DIR_OPT = "testOutputDir";

	public static void main(String[] args) throws UIMAException {
		
		System.out.println(Joiner.on(" ").join(args));

		Options options = new Options();

		options.addOption(ARGUMENTS_FILE_OPT, true,
				"The path of the file containing the command line arguments");
		options.addOption(TRAINING_QUESTIONS_PATH_OPT, true,
				"The path of the file containing the training questions");
		options.addOption(TRAINING_CANDIDATES_PATH_OPT, true,
				"The path of the file containing the training candidates passages");
		options.addOption(TRAINING_CASES_DIR_OPT, true,
				"The path where training CASes are stored (this enables file persistence)");
		options.addOption(TRAINING_OUTPUT_DIR_OPT, true,
				"The path where the training files will be stored");

		options.addOption(TEST_QUESTIONS_PATH_OPT, true,
				"The path of the file containing the test questions");
		options.addOption(TEST_CANDIDATES_PATH_OPT, true,
				"The path of the file containing the test candidates passages");
		options.addOption(TEST_CASES_DIR_OPT, true,
				"The path where test CASes are stored (this enables file persistence)");
		options.addOption(TEST_OUTPUT_DIR_OPT, true,
				"The path where the test files will be stored");

		CommandLineParser parser = new BasicParser();

		try {
			CommandLine cmd = parser.parse(options, args);

			String argumentsFilePath = getOptionalPathOption(cmd,
					ARGUMENTS_FILE_OPT, "Please specify a valid arguments file");
			
			if(argumentsFilePath != null) {
				String[] newArgs;
				try {
					newArgs = readArgs(argumentsFilePath);
					cmd = new BasicParser().parse(options, newArgs);
				} catch (IOException e) {
					System.err.println("Failed to load arguments file. Processing the given arguments...");
				}
			}

			String trainQuestionsPath = getFileOption(cmd,
					TRAINING_QUESTIONS_PATH_OPT,
					"Please specify the path of the training questions file.");

			String trainCandidatesPath = getFileOption(cmd,
					TRAINING_CANDIDATES_PATH_OPT,
					"Please specify the path of the training candidates file.");

			String trainOutputDir = getPathOption(cmd, TRAINING_OUTPUT_DIR_OPT,
					"Please specify a valid output directory for training data.");

			String trainCasesPath = getOptionalPathOption(cmd,
					TRAINING_CASES_DIR_OPT,
					"Please specify a valid directory for the training CASes.");

			String testQuestionsPath = getFileOption(cmd,
					TEST_QUESTIONS_PATH_OPT,
					"Please specify the path of the test questions file.");

			String testCandidatesPath = getFileOption(cmd,
					TEST_CANDIDATES_PATH_OPT,
					"Please specify the path of the test candidates file.");

			String testOutputDir = getPathOption(cmd, TEST_OUTPUT_DIR_OPT,
					"Please specify a valid output directory for test data.");

			String testCasesPath = getOptionalPathOption(cmd,
					TEST_CASES_DIR_OPT,
					"Please specify a valid directory for the test CASes.");

			UIMAPersistence trainPersistence = trainCasesPath == null ? new UIMANoPersistence()
					: new UIMAFilePersistence(trainCasesPath);

			UIMAPersistence testPersistence = testCasesPath == null ? new UIMANoPersistence()
					: new UIMAFilePersistence(testCasesPath);

			/**
			 * The parameter list used to establish matching between trees and
			 * output the content of the token nodes
			 */
			String parameterList = Joiner.on(",").join(
					new String[] { RichNode.OUTPUT_PAR_LEMMA,
							RichNode.OUTPUT_PAR_TOKEN_LOWERCASE });

			FileManager fm = new FileManager();

			PairFeatureFactory pf = new PairFeatureFactory(new Alphabet());

			TrecPipeline pipeline = new TrecPipeline(fm);

			/**
			 * Sets up the analyzer, initially with the persistence directory
			 * for train CASes
			 */
			Analyzer ae = pipeline.instantiateAnalyzer(trainPersistence);

			pipeline.performAnalysis(ae, trainQuestionsPath,
					trainCandidatesPath);

			Reranking dataGenerator = new RerankingTrain(fm, trainOutputDir,
					ae, new TreeSerializer().enableRelationalTags(), pf,
					new PosChunkTreeProvider()).setParameterList(parameterList);

			pipeline.performDataGeneration(dataGenerator);

			pipeline.closeFiles();

			/**
			 * Changes the persistence directory for test CASes
			 */
			ae.setPersistence(testPersistence);

			pipeline.performAnalysis(ae, testQuestionsPath, testCandidatesPath);

			/**
			 * Sets up the generation for test
			 */
			dataGenerator = new RerankingTest(fm, testOutputDir, ae,
					new TreeSerializer().enableRelationalTags(), pf,
					new PosChunkTreeProvider()).setParameterList(parameterList);

			pipeline.performDataGeneration(dataGenerator);

			pipeline.closeFiles();

		} catch (ParseException e) {
			System.out.println("Error in parsing the command line.");
			e.printStackTrace();
		}
	}

	private static String getFileOption(CommandLine cmd, String optionName,
			String errorMessage) throws ParseException {
		if (cmd.hasOption(optionName)) {
			String filePath = cmd.getOptionValue(optionName);
			if (new File(filePath).exists()) {
				return filePath;
			} else {
				throw new ParseException(errorMessage);
			}
		} else {
			throw new ParseException(errorMessage);
		}
	}

	private static String getPathOption(CommandLine cmd, String optionName,
			String errorMessage) throws ParseException {
		if (cmd.hasOption(optionName)) {
			String path = cmd.getOptionValue(optionName);
			try {
				Files.createParentDirs(Paths.get(path).toFile());
			} catch (IOException e) {
				throw new ParseException(errorMessage);
			}
			return path;
		} else {
			throw new ParseException(errorMessage);
		}
	}

	private static String getOptionalPathOption(CommandLine cmd,
			String optionName, String errorMessage) throws ParseException {
		if (cmd.hasOption(optionName)) {
			String path = cmd.getOptionValue(optionName);
			try {
				Files.createParentDirs(Paths.get(path).toFile());
			} catch (IOException e) {
				throw new ParseException(errorMessage);
			}
			return path;
		} else {
			return null;
		}
	}
	
	private static String[] readArgs(String argumentsFilePath) throws IOException {
		List<String> lines = Files.readLines(new File(argumentsFilePath), Charset.defaultCharset());
		return Joiner.on(" ").join(lines).split(" ");
	}
}