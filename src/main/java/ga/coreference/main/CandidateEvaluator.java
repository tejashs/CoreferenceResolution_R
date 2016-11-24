package ga.coreference.main;

import edu.stanford.nlp.trees.Tree;
import org.apache.log4j.Logger;
import org.w3c.dom.Node;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 * Created by tejas on 06/11/16.
 */
public class CandidateEvaluator {
    private ArrayList<CoRefObject> coRefObjects;
    private HashMap<Tree, CoRefObject> coRefTreeTocoRefObjectMap;
    private HashMap<CoRefObject, ArrayList<CandidateNP>> coRefObjectToSuccessCandidatesMap;
    public CandidateEvaluator(ArrayList<CoRefObject> coRefObjects, HashMap<Tree, CoRefObject> coRefTreeTocoRefObjectMap) {
        this.coRefObjects = coRefObjects;
        this.coRefTreeTocoRefObjectMap = coRefTreeTocoRefObjectMap;
        this.coRefObjectToSuccessCandidatesMap = new HashMap<CoRefObject, ArrayList<CandidateNP>>();
    }


    public String evaluateCandidateNPsForCoRefs() throws IOException{
        for (CoRefObject coRef: coRefObjects) {
            ArrayList<CandidateNP> candidateNPs = coRef.getCandidates();
            for (CandidateNP candidate: candidateNPs) {
                boolean featureMatched = FeatureMatcher.doesFeatureMatch(coRef.tree(), coRef.getSentenceTree(), candidate, candidate.getSentenceRoot());
                if(TreeHelper.getInstance().getTextValueForTree(candidate.getNounPhrase(), true).length() == 0){
                    continue;
                }
                if(featureMatched){
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
            }
        }
       return getOutputToPrint();
       

    }
    
    public String getOutputToPrint() throws IOException{
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
