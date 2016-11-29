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
    private ArrayList<CoRefObject> failedCorefs;
    public CandidateEvaluator(ArrayList<CoRefObject> coRefObjects, HashMap<Tree, CoRefObject> coRefTreeTocoRefObjectMap, HashMap<Tree, ArrayList<Tree>> sentenceToNPTerminalsMap, ArrayList<Tree> parsedSentencesInFile) {
        this.coRefObjects = coRefObjects;
        this.coRefTreeTocoRefObjectMap = coRefTreeTocoRefObjectMap;
        this.coRefObjectToSuccessCandidatesMap = new HashMap<CoRefObject, ArrayList<CandidateNP>>();
        this.sentenceToNPTerminalsMap = sentenceToNPTerminalsMap;
        this.parsedSentencesInFile = parsedSentencesInFile;
        failedCorefs = new ArrayList<CoRefObject>();
    }


    public String evaluateCandidateNPsForCoRefs() throws IOException{
        for (CoRefObject coRef: coRefObjects) {
            boolean atleastOneCandidateFound = false;
            String markedNodeText = TreeHelper.getInstance().getTextValueForTree(coRef.tree(), true);
            System.out.println("Marked Node : "+ markedNodeText);
            ArrayList<CandidateNP> candidateNPs = coRef.getCandidates();
            for (CandidateNP candidate: candidateNPs) {
                boolean featureMatched = FeatureMatcher.doesFeatureMatch(coRef.tree(), coRef.getSentenceTree(), candidate, candidate.getSentenceRoot());
                if(TreeHelper.getInstance().getTextValueForTree(candidate.getNounPhrase(), true).length() == 0){
                    continue;
                }
                if(featureMatched){
                    String candidateNodeText = TreeHelper.getInstance().getTextValueForTree(candidate.getNounPhrase(), true);
                    System.out.println("Candidate : "+ candidateNodeText);
                    addToSuccessCandidates(coRef, candidate);
                    atleastOneCandidateFound = true;
                }
            }
            if(!atleastOneCandidateFound){
                failedCorefs.add(coRef);
            }
        }
        evaluatePronounsForCoref();

        System.out.println("Failed Coref Size :" + failedCorefs.size());
        System.out.println("Coref objects:" + coRefObjects.size());
        System.out.println("Processed Coref Objects :" + coRefObjectToSuccessCandidatesMap.keySet().size());
        for (CoRefObject coref: failedCorefs) {
            System.out.println("Failed : " + coref.getValue());
            System.out.println("Candidates Size : " + coref.getCandidates().size());
        }

       return getOutputToPrint();
       

    }

    private void evaluatePronounsForCoref(){
        for (CoRefObject coref: coRefObjects) {
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
                failedCorefs.remove(coref);
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
        for (CoRefObject coRef:coRefObjectToSuccessCandidatesMap.keySet()) {
            System.out.println("COREF : " + coRef.getValue());
            ArrayList<CandidateNP> candidates = coRefObjectToSuccessCandidatesMap.get(coRef);
            int i = 0;
            for (CandidateNP np: candidates) {
                System.out.println("Candidate " + i + " : " + np.getNounPhrase());
                i++;
            }
        }

        if(true){
            return "";
        }





        StringBuilder builder = new StringBuilder();
        builder.append("<TXT>");
        builder.append("\n");
        int xmlTagIDCounter = 1;

        for (CoRefObject coRef:coRefObjectToSuccessCandidatesMap.keySet()) {
            Node coRefXMLNode = coRef.node();
            String ref = null;
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
