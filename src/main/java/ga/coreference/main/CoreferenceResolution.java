package ga.coreference.main;

import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.lang.reflect.Array;
import java.util.*;

public class CoreferenceResolution {
    private ArrayList<String> sentencesInFile;
    private ArrayList<Tree> parsedSentencesInFile;
    HashMap<Tree, ArrayList<Tree>> sentenceToNPTerminalMap;
    private ArrayList<CoRefObject> coRefObjects;
    private HashMap<Tree, CoRefObject> coRefTreeToCoRefObjectMap;
    private ArrayList<CoRefObject> failedCoRefs;
    private HashMap<String, String> corefToIDMap;

    public static void main(String[] args) {
//        String str = "/Users/tejas/Dropbox/MS Academics/Fall 2016/Natural Language Processing/Project/Initial Data set/dev/a9.crf";
        String str = "/Users/tejas/Dropbox/MS Academics/Fall 2016/Natural Language Processing/Project/Test Set 1/tst1/a13.crf";
        CoreferenceResolution res = new CoreferenceResolution();
        try {
            res.parseInputFile(str);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

public CoreferenceResolution(){
    initialize();
}
    private void initialize(){
        sentencesInFile = new ArrayList<String>();
        parsedSentencesInFile = new ArrayList<Tree>();
        sentenceToNPTerminalMap = new HashMap<Tree, ArrayList<Tree>>();
        coRefObjects = new ArrayList<CoRefObject>();
        coRefTreeToCoRefObjectMap = new HashMap<Tree, CoRefObject>();
        failedCoRefs = new ArrayList<CoRefObject>();
    }

    public String parseInputFile(String fileName) throws IOException {
        initialize();
        File file = new File(fileName);
        //Get All COREF Tags from file
        coRefObjects = preProcessFile(file);

        TreeHelper treeHelper = TreeHelper.getInstance();
        for (int i = 0; i < coRefObjects.size(); i++) {
            Tree nodeinTree = null;
            CoRefObject coref = coRefObjects.get(i);
            String val = coref.getValue();
            nodeinTree = treeHelper.narrowDownOnLeafNode(coref);
            if(nodeinTree == null){
                System.out.println("WTF - NOT WORKING FOR COREF ID : " + coref.getValue());
            }
            else {
                coref.setCoRefTree(nodeinTree);
                coRefTreeToCoRefObjectMap.put(nodeinTree, coref);
            }
        }
        return startResolution();
    }

    private String startResolution() throws IOException{
        for (Tree sentenceNode: parsedSentencesInFile) {
            ArrayList<Tree> terminalNPNodes = new ArrayList<Tree>();
            TreeTraversalUtility.getTerminalNPNodes(sentenceNode, terminalNPNodes);
            if(!sentenceToNPTerminalMap.containsKey(sentenceNode)){
                sentenceToNPTerminalMap.put(sentenceNode, terminalNPNodes);
            }
        }

        for (CoRefObject coRef: coRefObjects) {
            if(coRef.tree() == null){
                failedCoRefs.add(coRef);
                continue;
            }
            Tree sentence = coRef.getSentenceTree();
            ArrayList<Tree> npNodesList = sentenceToNPTerminalMap.get(sentence);
            ArrayList<Tree> npNodesUpUntilNode = TreeHelper.getInstance().getNpNodesUptilNodeInSentence(coRef.tree(), npNodesList, sentence);
            ArrayList<CandidateNP> candidateNPs = getCandidateNPs(npNodesUpUntilNode, sentence);
            getAllNPsFromPreviousSentencesInDecreasingOrder(sentence, candidateNPs);
            coRef.setCandidates(candidateNPs);
        }

        CandidateEvaluator evaluator = new CandidateEvaluator(corefToIDMap, coRefObjects, coRefTreeToCoRefObjectMap, sentenceToNPTerminalMap, parsedSentencesInFile);
        return evaluator.evaluateCandidateNPsForCoRefs();
    }

    private ArrayList<CandidateNP> getCandidateNPs(ArrayList<Tree> nodes, Tree sentence){
        ArrayList<CandidateNP> listToSend = new ArrayList<CandidateNP>();
        for (Tree n: nodes) {
            listToSend.add(new CandidateNP(n, sentence));
        }
        return listToSend;
    }




    private void getAllNPsFromPreviousSentencesInDecreasingOrder(Tree sentence, ArrayList<CandidateNP> candidateNPs){
        int sentenceIndex = parsedSentencesInFile.indexOf(sentence);
        for(int i=sentenceIndex-1; i>=0; i--){
            Tree sentenceNode = parsedSentencesInFile.get(i);
            ArrayList<Tree> terminalNPNodes = new ArrayList<Tree>();
            TreeTraversalUtility.getTerminalNPNodes(sentenceNode, terminalNPNodes);
            ArrayList<CandidateNP> candidates = CandidateNP.getCandidateNPFromTree(terminalNPNodes, sentenceNode);
            candidateNPs.addAll(candidates);
        }

    }

    private HashMap<String, Tree> getSentenceToParseTreeMap(String fileText) {
        List<CoreMap> sentences = ParseUtility.getParsedSentences(fileText);
        getLogger().debug("Parsed Sentences Count: " + sentences.size());

        int validSentenceCounter = 0;
        HashMap<String, Tree> sentenceToTreeMap = new HashMap<String, Tree>();
        for (CoreMap sentence : sentences) {
            if (sentence.toString().equals(".")) {
                continue;
            }
            validSentenceCounter++;
            //getLogger().debug(sentence.toString());
            // this is the parse tree of the current sentence
            Tree tree = sentence.get(TreeCoreAnnotations.TreeAnnotation.class);
            sentenceToTreeMap.put(sentence.toString(), tree);
        }
        getLogger().debug("Valid Sentences Count : " + validSentenceCounter);
        return sentenceToTreeMap;
    }


private String getTextFromFile(File file){
    String fileText = "";
    try {
        fileText = IOUtils.slurpFile(file);
    } catch (IOException e) {
        e.printStackTrace();
    }
    fileText = fileText.replaceAll("\\n\\n", ".");
    fileText = fileText.replaceAll("<TXT>", "");
    fileText = fileText.replaceAll("</TXT>", "");
    //fileText = fileText.replaceAll("\\n-", ".\n\n");
    return fileText;
}
    private ArrayList<CoRefObject> preProcessFile(File file) {
        String fileText = getTextFromFile(file);
        DocumentPreprocessor documentPreprocessor = new DocumentPreprocessor(new StringReader(fileText));
        ArrayList<CoRefObject> listOfCorefs = new ArrayList<CoRefObject>();
        int sentenceIndex = 0;
        for (List<HasWord> list: documentPreprocessor) {
            String sentence = Sentence.listToOriginalTextString(list);
            sentence = sentence.trim();
            sentencesInFile.add(sentenceIndex, sentence);
            sentenceIndex++;
        }
        NodeList allTags = getAllTagsInFile(file);
        //populate to a hashmap
        corefToIDMap = new HashMap<String, String>();
        for(int i = 0; i < allTags.getLength(); i++){
        	Node coref = allTags.item(i);
        	String corefText = coref.getTextContent();
        	if(!corefToIDMap.containsKey(corefText)){
        		corefToIDMap.put(corefText, coref.getAttributes().item(0).getNodeValue());
        	}
        }
        
        
        parsedSentencesInFile = getParsedSentences(file);
        int ballParkIndex = -1;
        int coRefIndex = 0;
        for (int i=0; i<allTags.getLength(); i++){
            Node node = allTags.item(i);
            CoRefObject coref = getCoRefObjectForTag(node, ballParkIndex);
            ballParkIndex = coref.getSentenceIndexInFile();
            listOfCorefs.add(coRefIndex, coref);
            coRefIndex++;
        }
        return listOfCorefs;
    }
    private ArrayList<Tree> getParsedSentences(File file) {
        String fileText = getTextFromFile(file);
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        Annotation annotatedDoc = null;
        annotatedDoc = new Annotation(fileText);
        pipeline.annotate(annotatedDoc);
        List<CoreMap> sentences = annotatedDoc.get(CoreAnnotations.SentencesAnnotation.class);
        ArrayList<Tree> validSentences = new ArrayList<Tree>();
        for (CoreMap line : sentences){
            String lineStr = line.toString();
            if(lineStr.length() == 1 || lineStr.equals("<TXT>") || lineStr.equals("</TXT>")){
                continue;
            }
            validSentences.add(line.get(TreeCoreAnnotations.TreeAnnotation.class));
        }
        return validSentences;
    }

    private CoRefObject getCoRefObjectForTag(Node node, int ballParkIndex){
        String id = node.getAttributes().item(0).getNodeValue();
        String idToMatch = "ID=\"" + id + "\"";
        int foundIndex = -1;
        if(ballParkIndex < 0){
            ballParkIndex = 0;
        }
        for(int i = ballParkIndex; i < parsedSentencesInFile.size(); i++){
            String sentence = parsedSentencesInFile.get(i).toString();
            if(sentence.contains(idToMatch)){
                foundIndex = i;
                break;
            }
        }
        CoRefObject coref = new CoRefObject(node.getAttributes().item(0).getNodeValue(), node.getTextContent(),parsedSentencesInFile.get(foundIndex), foundIndex, node);
        return coref;
    }

    private NodeList getAllTagsInFile(File f) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document document = null;
        try {
            document = factory.newDocumentBuilder().parse(f);
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        NodeList listOfTags = null;
        if (document != null) {
            listOfTags = document.getElementsByTagName("COREF");
            getLogger().debug("Number of CoRef Tags : " + listOfTags.getLength());
        }
        return listOfTags;
    }
    private Logger getLogger() {
        return Logger.getLogger(CoreferenceResolution.class);
    }

}
