package ga.coreference.main;

import edu.stanford.nlp.simple.Sentence;
import edu.stanford.nlp.trees.Tree;
import org.apache.log4j.Logger;
import org.w3c.dom.Node;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by tejas on 06/11/16.
 */
public class CandidateEvaluator {
    public static final String NER_0 = "0";
    public static final String NER_PERSON = "PERSON";
    public static final String NER_LOCATION = "LOCATION";
    public static final String NER_ORGANIZATION = "ORGANIZATION";
    private ArrayList<CoRefObject> coRefObjects;
    private HashMap<Tree, CoRefObject> coRefTreeTocoRefObjectMap;
    private HashMap<CoRefObject, ArrayList<CandidateNP>> coRefObjectToSuccessCandidatesMap;
    private HashMap<Tree, ArrayList<Tree>> sentenceToNPTerminalsMap;
    private ArrayList<Tree> parsedSentencesInFile;
    private HashMap<String, String> corefToIDMap;
    private boolean flag;
    StringBuilder builder;
    private HashSet<String> IDs;
    ArrayList<String> sentencesInFile;
    private int xmlTagIDCounter;
    
    public CandidateEvaluator(ArrayList<String> sentencesInFile, HashMap<String, String> corefToIDMap, ArrayList<CoRefObject> coRefObjects, HashMap<Tree, CoRefObject> coRefTreeTocoRefObjectMap, HashMap<Tree, ArrayList<Tree>> sentenceToNPTerminalsMap, ArrayList<Tree> parsedSentencesInFile) {
        this.coRefObjects = coRefObjects;
        this.coRefTreeTocoRefObjectMap = coRefTreeTocoRefObjectMap;
        this.coRefObjectToSuccessCandidatesMap = new HashMap<CoRefObject, ArrayList<CandidateNP>>();
        this.sentenceToNPTerminalsMap = sentenceToNPTerminalsMap;
        this.parsedSentencesInFile = parsedSentencesInFile;
        this.corefToIDMap = corefToIDMap;
        flag = false;
        builder = new StringBuilder();
        IDs = new HashSet<String>();
        this.sentencesInFile = sentencesInFile;
        xmlTagIDCounter = 1;
    }


    public String evaluateCandidateNPsForCoRefs() throws IOException{
        for (CoRefObject coRef: coRefObjects) {

            boolean atleastOneCandidateFound = false;

            String markedNodeText = TreeHelper.getInstance().getTextValueForTree(coRef.tree(), true);
           // System.out.println("Marked Node : "+ markedNodeText);
            ArrayList<CandidateNP> candidateNPs = coRef.getCandidates();
            String corefText = coRef.getValue().toLowerCase();
            if(corefToIDMap.containsKey(corefText)){
            	String id = corefToIDMap.get(corefText);
            	String idtoMatch = coRef.getID();
            	if(!id.equals(idtoMatch)){
            		 //StringBuilder builder = new StringBuilder();
            		if(!flag){
	            		builder.append("<TXT>");
	            	    builder.append("\n"); 
            		}
            	    builder.append("<COREF ID=\""+ idtoMatch + "\" REF=\""+ id + "\">"+coRef.getValue()+"</COREF>");
            	    builder.append("\n");
            	    flag = true;
            	    IDs.add(idtoMatch);
            	     
            	}
            }
            
            //do string match
            //String corefText = coRef.getValue();
            String corefID = coRef.getID();
            if(IDs.contains(corefID)) continue;
	            int sentenceID = coRef.getSentenceIndexInFile();
	            if(sentenceID > this.sentencesInFile.size()-1) continue;
	            for(int i = sentenceID; i >= 0; i--){
	            	
	            	String sentence = this.sentencesInFile.get(i);
	            	if(i == sentenceID){
	            		if(sentence.indexOf(corefID) > 0)
	            			sentence = sentence.substring(0, sentence.indexOf(corefID));
	            	}
	            	String corefHeadNoun = FeatureMatcher.getHeadNounFromString(corefText);
	            	if(sentence.contains(corefHeadNoun)){
	            		if(!flag){
		            		builder.append("<TXT>");
		            	    builder.append("\n"); 
	            		}
	            		builder.append("<COREF ID=\""+ "GA"+ xmlTagIDCounter + "\">"+corefHeadNoun+"</COREF>");
	            		builder.append("\n");
	            		builder.append("<COREF ID=\""+ corefID + "\" REF=\""+ "GA"+ xmlTagIDCounter + "\">"+coRef.getValue()+"</COREF>");
	             	    builder.append("\n");
	             	    xmlTagIDCounter++;
	             	    flag = true;
	             	    IDs.add(corefID);
	             	    break;
	            	}
	            }
            
	        if(IDs.contains(corefID)) continue;
            for (CandidateNP candidate: candidateNPs) {
                boolean featureMatched = FeatureMatcher.doesFeatureMatch(coRef.tree(), coRef.getSentenceTree(), candidate, candidate.getSentenceRoot(), true);
                if(TreeHelper.getInstance().getTextValueForTree(candidate.getNounPhrase(), true).length() == 0){
                    continue;
                }
                if(featureMatched){
                    String candidateNodeText = TreeHelper.getInstance().getTextValueForTree(candidate.getNounPhrase(), true);
                   // System.out.println("Candidate : "+ candidateNodeText);
                    addToSuccessCandidates(coRef, candidate);
                    IDs.add(corefID);

                    atleastOneCandidateFound = true;
                }
            }
          //  System.out.println("");
            if(IDs.contains(corefID)) continue;
            if(!coRefObjectToSuccessCandidatesMap.containsKey(coRef)){
                //check for NER match
                for (CandidateNP candidate: candidateNPs) {
                	 boolean featureMatched = FeatureMatcher.doesFeatureMatch(coRef.tree(), coRef.getSentenceTree(), candidate, candidate.getSentenceRoot(), false);
                	 if(TreeHelper.getInstance().getTextValueForTree(candidate.getNounPhrase(), true).length() == 0){
                         continue;
                     }
                	 if(featureMatched){
                		 String candidateNodeText = TreeHelper.getInstance().getTextValueForTree(candidate.getNounPhrase(), true);
                         // System.out.println("Candidate : "+ candidateNodeText);
                          if(coRefObjectToSuccessCandidatesMap.containsKey(coRef)){
                              ArrayList<CandidateNP> successCandidates = coRefObjectToSuccessCandidatesMap.get(coRef);
                              successCandidates.add(candidate);
                              coRefObjectToSuccessCandidatesMap.put(coRef, successCandidates);
                          }
                          else {
                              ArrayList<CandidateNP> successCandidates = new ArrayList<CandidateNP>();
                              successCandidates.add(candidate);
                              coRefObjectToSuccessCandidatesMap.put(coRef, successCandidates);
                          }
                          IDs.add(corefID);
                          break;
                	 }
                }
                
              //  System.out.println("");
            }
        }

        evaluatePronounsForCoref();

       
        
        
       return getOutputToPrint();
       

    }

    private void evaluatePronounsForCoref(){
        for (CoRefObject coref: coRefObjects) {
        	if(IDs.contains(coref.getID())) continue;
            if(!POSUtility.isPronoun(coref.getValue())){
                continue;
            }
            evaluatePronounForSentence(coref, null, false);

            int sentenceIndex = coref.getSentenceIndexInFile();
            int counter = 0;
            while(counter < 1){
                sentenceIndex--;
                if(sentenceIndex >= 0){
                    Tree sentenceNode = parsedSentencesInFile.get(sentenceIndex);
                    evaluatePronounForSentence(coref, sentenceNode, true);
                    counter++;
                }
                else {
                    break;
                }
            }
        }
    }

    private void evaluatePronounForSentence(CoRefObject coref, Tree sentence, boolean differentSentence){
        ArrayList<Tree> terminalNodes;
        ArrayList<Tree> npNodesToEvaluate;
        if(!differentSentence){
            sentence = coref.getSentenceTree();
            terminalNodes = sentenceToNPTerminalsMap.get(sentence);
            npNodesToEvaluate = TreeHelper.getInstance().getNpNodesUptilNodeInSentence(coref.tree(), terminalNodes, sentence);
        }
        else {
            npNodesToEvaluate = sentenceToNPTerminalsMap.get(sentence);
        }
        ArrayList<CandidateNP> candidatePronounNPs = new ArrayList<CandidateNP>();
        if(coref.getValue().equalsIgnoreCase("it")){
            for (Tree npNode: npNodesToEvaluate) {
                List<Sentence> sentences = TreeHelper.getInstance().getSentenceForTree(sentence, true);
                String ner = TreeHelper.getInstance().findNERTagForNP(sentences.get(0), npNode);
                if(ner.equals(NER_ORGANIZATION)){
                    CandidateNP candidateNP = new CandidateNP(npNode, sentence);
                    candidatePronounNPs.add(candidateNP);
                }
            }
        }
        else {
            for (Tree npNode: npNodesToEvaluate) {
                List<Sentence> sentences = TreeHelper.getInstance().getSentenceForTree(sentence, true);
                if(sentences == null || sentences.size() == 0){
                    continue;
                }
                String ner = TreeHelper.getInstance().findNERTagForNP(sentences.get(0), npNode);
                if(ner.equals(NER_PERSON)){
                    //Do gender check
                    CandidateNP candidateNP = new CandidateNP(npNode, sentence);
                    candidatePronounNPs.add(candidateNP);
                }
            }
        }
        if(candidatePronounNPs.size() > 0){
            for (CandidateNP candidate: candidatePronounNPs) {
                addToSuccessCandidates(coref, candidate);
            }
        }
    }

    private void addToSuccessCandidates(CoRefObject coRef, CandidateNP candidate){
        if(coRefObjectToSuccessCandidatesMap.containsKey(coRef)){
            ArrayList<CandidateNP> successCandidates = coRefObjectToSuccessCandidatesMap.get(coRef);
            successCandidates.add(candidate);
            coRefObjectToSuccessCandidatesMap.put(coRef, successCandidates);
        }
        else {
            ArrayList<CandidateNP> successCandidates = new ArrayList<CandidateNP>();
            successCandidates.add(candidate);
            coRefObjectToSuccessCandidatesMap.put(coRef, successCandidates);
        }
    }
    
    public String getOutputToPrint() throws IOException{
//        StringBuilder builder = new StringBuilder();
        if(!flag) {
        	builder.append("<TXT>");
        	builder.append("\n");
        }
       
       // int xmlTagIDCounter = 1;

        for (CoRefObject coRef:coRefObjectToSuccessCandidatesMap.keySet()) {
        	
            Node coRefXMLNode = coRef.node();
            String ref = null;
//            if(IDs.contains(coRefXMLNode.getAttributes().item(0).getNodeValue())){
//            	continue;
//            }
            ArrayList<CandidateNP> successCandidates = coRefObjectToSuccessCandidatesMap.get(coRef);
            HashMap<CandidateNP, String> candidateNPToXMLTextMap = new HashMap<CandidateNP, String>();
            for(int i = successCandidates.size()-1; i >= 0; i--){
                //check if its a coref
                Tree cNP = successCandidates.get(i).getNounPhrase();
                String xmlNodeTextToPrint = null;
                if(coRefObjectToSuccessCandidatesMap.containsKey(cNP)){
                    //TODO
                    Node xmlNode = coRefTreeTocoRefObjectMap.get(cNP).node();
                    if(coRefXMLNode.getAttributes().item(0).getNodeValue().equals(xmlNode.getAttributes().item(0).getNodeValue())){
                    	continue;
                    }
                    
                    ref  = xmlNode.getAttributes().item(0).getNodeValue();
                }
                else {
                    String ID = "GA" + xmlTagIDCounter;
                    xmlTagIDCounter++;
                    if(ref == null){
                        ref = ID;
                        builder.append(constructXMLNode(ID, null, cNP));
                        builder.append("\n");
                    }
                }
            }
            String x = constructXMLNode(coRefXMLNode.getAttributes().item(0).getNodeValue(), ref, coRefXMLNode.getTextContent());
            builder.append(x);
            builder.append("\n");
            //IDs.add(coRef.getID());
        }
        builder.append("</TXT>");
        builder.append("\n");
       
        return builder.toString();
    }

    private String constructXMLNode(String IDtoAdd, String referenceTag, Tree tree){
        return  constructXMLNode(IDtoAdd, referenceTag, TreeHelper.getInstance().getTextValueForTree(tree, true));
    }

    private String constructXMLNode(String IDtoAdd, String referenceTag, String textContent){
        String xmlTextToSend = null;
        if(referenceTag == null){
            xmlTextToSend =  "<COREF ID=\""+ IDtoAdd + "\">"+textContent+"</COREF>";
        }
        else {
            xmlTextToSend =  "<COREF ID=\""+ IDtoAdd + "\" REF=\""+ referenceTag + "\">"+textContent+"</COREF>";
        }
        return xmlTextToSend;
    }


    private Logger getLogger() {
        return Logger.getLogger(CandidateEvaluator.class);
    }
}
