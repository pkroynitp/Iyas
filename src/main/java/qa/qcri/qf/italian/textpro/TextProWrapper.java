package qa.qcri.qf.italian.textpro;

import it.unitn.limosine.util.SharedModel;
import it.unitn.limosine.util.StreamGobbler;

import java.io.BufferedOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceAccessException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

import java.nio.charset.*;
import java.nio.CharBuffer;
import java.nio.ByteBuffer;

public class TextProWrapper extends JCasAnnotator_ImplBase {

 	private String txpcommand="textpro.pl";
//ToDo: change with a proper textpro command (using resources and environment variables)
 	private String[] txpparams={"-l", "ita", "-c", "token+tokenstart+tokenend+sentence+pos+lemma+entity"};
 	private String txppath;
    String txpencoding = "ISO-8859-1"; // "Cp1252" = Windows Latin 1
	
 	
 	private static String txp2ner(String txpner) {
 		if (txpner.equals("PER")) return "PERSON";
 		if (txpner.equals("ORG")) return "ORGANIZATION";
		if (txpner.equals("LOC")) return "LOCATION";
		if (txpner.equals("GPE")) return "LOCATION";
		return "ENTITY";
 	}
 	
	  private static String forXML(String aText){
		    final StringBuilder result = new StringBuilder();
		    final StringCharacterIterator iterator = new StringCharacterIterator(aText);
		    char character =  iterator.current();
		    while (character != CharacterIterator.DONE ){
		      if (character == '<') {
		        result.append("&lt;");
		      }
		      else if (character == '>') {
		        result.append("&gt;");
		      }
		      else if (character == '\"') {
		        result.append("&quot;");
		      }
		      else if (character == '\'') {
		        result.append("&#039;");
		      }
		      else if (character == '&') {
			         result.append("&amp;");
			      }
		      
		      else if (character == '(') {
			         result.append("-LRB-");
			      }
		      else if (character == ')') {
			         result.append("-RRB-");
			      }
			      
		      else {
		        //the char is not a special one
		        //add it to the result as is
		        result.append(character);
		      }
		      character = iterator.next();
		    }
		    return result.toString();
		  }

	  @Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);

		try {

			
			SharedModel sharedModel = (SharedModel) getContext().getResourceObject("RunTextPro");

			getContext().getLogger().log(Level.INFO,
					"Launching TextPro...");

			txppath = sharedModel.getPath();

			getContext().getLogger().log(Level.INFO, txppath);

			
			
		} catch (ResourceAccessException e) {
			e.printStackTrace();
		}
		
	}

 	
 	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		
 		//prepare textpro input	
 		String doctxt_utf=jcas.getDocumentText();

 		//run textpro

 		if (doctxt_utf.length()==0) return;
 		
		try {

			
			
			String fulltxpcommand = txppath + "/" + txpcommand; 
			List<String> fulltxpcmdline=new ArrayList<String>();
			fulltxpcmdline.add(fulltxpcommand);
			fulltxpcmdline.addAll(Arrays.asList(txpparams));
			ProcessBuilder processBuilder = new ProcessBuilder(fulltxpcmdline);
			
			Map<String, String> env = processBuilder.environment();
			env.put("TEXTPRO", txppath);

			Process process = processBuilder.start();
		
			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new BufferedOutputStream(process.getOutputStream()), txpencoding);
			PrintWriter writer = new PrintWriter(outputStreamWriter, true);
			
			writer.println(doctxt_utf);
		
			writer.println();
			writer.flush();
			writer.close();
        
        
			StreamGobbler outGobbler = new StreamGobbler(process.getInputStream());
			StreamGobbler errGobbler = new StreamGobbler(process.getErrorStream());
			Thread outThread = new Thread(outGobbler);
			Thread errThread = new Thread(errGobbler);
			outThread.start();
			errThread.start();

			outThread.join();
			errThread.join();

			process.waitFor();

        
			for(String out : outGobbler.getOuput()) 
				System.err.println(out.trim());
        
			List<String> output = outGobbler.getOuput();
			List<String> outputErr = errGobbler.getOuput();
        
			for(String out : outputErr) 
				System.err.println(out.trim());
        
			System.err.println("RETURNED:");
			for(String out : output) 
				System.err.println(out.trim());
        
			//parse textpro output
        
			int tokenCount=0;
			int sentenceCount = 0;
			StringBuilder tokenizedSentence = new StringBuilder();
			int tokenSentCount = 0; //sentence-level counter for tokens
			Sentence sentence = new Sentence(jcas);
		
			boolean nestart=false;
			NamedEntity ne = new NamedEntity(jcas);
     
			for(String out : output) {
				System.out.println("TEXTPRO OUTPUT: " +out);
				String[] txpvals = out.trim().split("\t");

				// TOKEN
				if (txpvals.length<7) continue;
				Token token = new Token(jcas);
				token.setBegin(Integer.parseInt(txpvals[1]));
				token.setEnd(Integer.parseInt(txpvals[2]));
				//token.setTokenId(tokenCount);
				//token.setTokenSentId(tokenSentCount);
				//token.setAnnotatorId(getClass().getCanonicalName()+":token");   
				//token.setNormalizedText(forXML(token.getCoveredText()));
				//			token.setStanfordNormalizedText(coreLab.value());
				//token.setStanfordNormalizedText(forXML(token.getCoveredText())); //RE uses this as input!
				token.addToIndexes();

				tokenCount++;
			
				// SENTENCE
			
				if (tokenSentCount==0) {
					sentence = new Sentence(jcas);
					//sentence.setSentenceId(sentenceCount);
					//sentence.setStartToken(token);
					sentence.setBegin(token.getBegin());
					//sentence.setAnnotatorId(getClass().getCanonicalName()+":sentence");
					sentenceCount++;
					tokenizedSentence.setLength(0);
				}
				//tokenizedSentence.append(token.getNormalizedText() + " ");
				tokenSentCount++;
			
				if (txpvals[3].equals("<eos>")) {
			
					//sentence.setEndToken(token);
					sentence.setEnd(token.getEnd());
					//sentence.setTokenizedSentence(tokenizedSentence.toString().trim());
					sentence.addToIndexes();

					tokenSentCount = 0; //sentence-level counter for tokens
				}
			
				// LEMMA ? MORPH ?
			
				// NE
				// store finished/ongoing NE
				if (nestart) {
					if (txpvals[6].startsWith("I-")) {
						//ne.setEndToken(token);
						ne.setEnd(token.getEnd());					
					} else {
						ne.addToIndexes();
						nestart=false;
					}
				}
				// start new NE
				if (txpvals[6].startsWith("B-")) {
					nestart=true;
					ne= new NamedEntity(jcas);
					//ne.setStartToken(token);
					ne.setBegin(token.getBegin());
					//ne.setEndToken(token);
					ne.setEnd(token.getEnd());					
					//ne.setNetag(txp2ner(txpvals[6].substring(2,txpvals[6].length())));
					ne.setValue(txp2ner(txpvals[6].substring(2,txpvals[6].length())));
					//ne.setAnnotatorId(getClass().getCanonicalName()+":ner");				
				}
			
				// POS
				POS pos = new POS(jcas);
				pos.setBegin(token.getBegin());
				pos.setEnd(token.getEnd());
				pos.setPosValue(txpvals[4]);
				token.setPos(pos);				
				//pos.setAnnotatorId(getClass().getCanonicalName()+":pos");				
				pos.addToIndexes();

				// lemma
				Lemma lem = new Lemma(jcas);
				lem.setBegin(token.getBegin());
				lem.setEnd(token.getEnd());
				lem.setValue(txpvals[5]);
				//lem.setLemma(forXML(txpvals[5]));
				//lem.setToken(token);
				//lem.setAnnotatorId(getClass().getCanonicalName()+":lemma");				
				lem.addToIndexes();					
				token.setLemma(lem);
			}
			
			// store NE finishing at the last input line
			if (nestart) 
				ne.addToIndexes();
			       
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
		
}