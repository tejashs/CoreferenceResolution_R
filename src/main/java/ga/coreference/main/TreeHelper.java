package ga.coreference.main;

import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.simple.Document;
import edu.stanford.nlp.simple.Sentence;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.StringUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Node;

import java.util.*;

/**
 * Created by tejas on 03/11/16.
 */
public class TreeHelper {

    private static TreeHelper INSTANCE = new TreeHelper();
    private static HashMap<Sentence, List<String>> sentenceToNerTagsMap = new HashMap<Sentence, List<String>>();

    private TreeHelper() {

    }

    public static TreeHelper getInstance() {
        return INSTANCE;
    }

    public Tree getTreeForNode(CoRefObject coref, Tree sentence) {
        for (Tree child : sentence.getChildrenAsList()) {
            if (POSUtility.checkIfTagIsNounRelated(child.label().value())) {
                //String textValue = child.toString();
                //TODO IGNORE CASE????
                String textValueOfChild;
                String nodeValueToCompare = "";
                textValueOfChild = getTextValueForTree(child, false);
                nodeValueToCompare = "ID=" + coref.getID();

                if (textValueOfChild.contains(nodeValueToCompare)) {
                    Tree nodeToReturn = narrowDownOnLeafNode(coref);
                    return nodeToReturn;
                }
            } else if (!POSUtility.isTerminalTag(child.label().value())) {
                Tree tree = getTreeForNode(coref, child);
                if(tree != null){
                    return tree;
                }
            }
        }
        return null;
    }

    public Tree narrowDownOnLeafNode(CoRefObject coref){
        String nodeTextValue = coref.getValue();
        String[] textArray = nodeTextValue.split("\\s|\\n");
        String textToFind = textArray[0];
        boolean isNodeTextMultipleWords = false;
        if(textArray.length > 1){
            //Node text contains more than one word
            isNodeTextMultipleWords = true;
        }
        Tree root = coref.getSentenceTree();
        List<Tree> leaves = root.getLeaves();
        int ballParkIndexToStart = getBallParkIndexToFindLeaf(coref, root);
        if(ballParkIndexToStart == -1){
            ballParkIndexToStart = 0;
        }
        int foundIndex = -1;
        for (int i = ballParkIndexToStart; i < leaves.size(); i++) {
            Tree leaf = leaves.get(i);
            String[] leafValues = leaf.label().value().split("\\s");
            String leafValueToCompare = null;
            if(leafValues.length == 1){
                leafValueToCompare = StringUtils.stripNonAlphaNumerics(leafValues[0]);
            }
            else {
                StringBuilder builder = new StringBuilder();
                for (int j = 0; j < leafValues.length ; j++) {
                    builder.append(StringUtils.stripNonAlphaNumerics(leafValues[i]));
                    builder.append(" ");
                }
                leafValueToCompare = builder.toString().trim();
            }
            String tempTextToFind = StringUtils.stripNonAlphaNumerics(textToFind);
            if(tempTextToFind.length() > leafValueToCompare.length()){
                //This is to handle Apostrophes inside coref tags if it gets split into different sentences
                if(tempTextToFind.contains(leafValueToCompare)){
                    foundIndex = i;
                }
            }
            if(leafValueToCompare.equals(tempTextToFind)){
                foundIndex = i;
                break;
            }
        }
        if(foundIndex == -1){
            getLogger().info("**********************");
            getLogger().info("INDEX NOT FOUND FOR :");
            getLogger().info(nodeTextValue);
            getLogger().info("**********************");
            return null;
        }

        if(!isNodeTextMultipleWords){
            return getParentForNode(leaves.get(foundIndex), coref);
        }
        else {
            ArrayList<Tree> leavesToSend = new ArrayList<Tree>();
            //leavesToSend.add(0, leaves.get(foundIndex));
            int arrayIndex = 0;
            int tempIndex = 0;
            for (Tree leaf : leaves) {
                if(tempIndex != foundIndex){
                    tempIndex++;
                    //So that we can start comparison directly from found index
                    continue;
                }
                if(arrayIndex == textArray.length){
                    break;
                }
                String leafText = leaf.label().value();
                leafText = StringUtils.stripNonAlphaNumerics(leafText);
                if((leafText == null) || (leafText.length() == 0)){
                    continue;
                }
                if(StringUtils.stripNonAlphaNumerics(textArray[arrayIndex]).equals(leafText)){
                    leavesToSend.add(arrayIndex, leaf);
                    arrayIndex++;
                }
            }
            if(leavesToSend.size() == 0){
                return null;
            }
            return getCommonParentForLeaves(leavesToSend, coref);
        }
    }

    private int getBallParkIndexToFindLeaf(CoRefObject coref, Tree root) {
        List<Tree> leaves = root.getLeaves();
        int indexToSend = -1;
        String nodeValueToCompare = "ID=\"" + coref.getID() + "\"";
        for(int i=0; i<leaves.size(); i++){
            Tree leaf = leaves.get(i);
            if(leaf.label().value().contains(nodeValueToCompare)){
                indexToSend = i;
                break;
            }
        }
        return indexToSend;
    }

    private Tree getCommonParentForLeaves(ArrayList<Tree> leaves, CoRefObject coref){
        String textToCompare = coref.getValue();
        Tree firstLeaf = leaves.get(0);
        Tree parent = getParentForNode(firstLeaf, coref);
        return getParentForText(parent, textToCompare, coref);
    }

    private Tree getParentForText(Tree parent, String textToCompare, CoRefObject coref){
        String parentText = getTextValueForTree(parent, true);
        if(parentText == null){
            return null;
        }
        if(parentText.contains(textToCompare)){
            return parent;
        }
        else {
            parent = getParentForNode(parent, coref);
            return getParentForText(parent, textToCompare, coref);
        }
    }

    public String getTextValueForTree(Tree child, boolean skipCoRefTag) {
        if(child == null){
            return null;
        }
        List<Word> words = child.yieldWords();
        StringBuilder builder = new StringBuilder();
        if(!skipCoRefTag){
            //This is for finding valid CoRef ID. So anything would work as long as it has ID.
            for (Word w : words) {
                builder.append(w.value().trim());
                builder.append(" ");
            }
            return builder.toString().trim();
        }
        else {
            for (Word w : words) {
                if(w.value().contains("COREF")){
                    continue;
                }
                String word = StringUtils.stripNonAlphaNumerics(w.value());
                builder.append(word);
                if(word != null && word.length() > 0){
                    builder.append(" ");
                }
            }
            return builder.toString().trim();
        }
    }

    private String getNodeValueToCompare(Node node, boolean shouldCompareID) {
        StringBuilder sb = new StringBuilder();
        if(shouldCompareID) {
            sb.append(node.getAttributes().item(0).getNodeName());
            sb.append("=");
            sb.append("\"");
            sb.append(node.getAttributes().item(0).getNodeValue());
            sb.append("\"");
        }
        else {
            String nodeTextValue = node.getTextContent();
            String[] textArray = nodeTextValue.split("\\s|\\n");
            for (int i = 0; i < textArray.length ; i++) {
                sb.append(StringUtils.stripNonAlphaNumerics(textArray[i]));
                sb.append(" ");
            }
        }
        return sb.toString().trim();
    }

    private String cleanupSentence(String sentence) {
        sentence = sentence.replaceAll("\\.", " ");
        sentence = sentence.replaceAll("\\n", " ");
        return sentence.trim();
    }
    private Tree getParentForNode(Tree node, CoRefObject coref){
        return node.parent(coref.getSentenceTree());
    }

    private Logger getLogger(){
        return Logger.getLogger(TreeHelper.class);
    }
    
    public List<Sentence> getSentenceForTree(Tree tree, boolean skipCoRefTag){
    	List<Word> words = tree.yieldWords();
        StringBuilder builder = new StringBuilder();
        if(!skipCoRefTag){
            for (Word w : words) {
                builder.append(w.value().trim());
                builder.append(" ");
            }
            Document doc = new Document(builder.toString().trim());
            return doc.sentences();
        }
        else {
            for (Word w : words) {
                if(w.value().contains("COREF")){
                    continue;
                }
                String word = w.value();
                builder.append(word);
                builder.append(" ");
            }
            Document doc = new Document(builder.toString().trim());
            return doc.sentences();
        }
    }
    
    public String findNERTagForNP(Sentence sentence, Tree NP){
        List<String> NERtags;
        if(!sentenceToNerTagsMap.containsKey(sentence)){
             NERtags = sentence.nerTags();
        }
        else {
             NERtags = sentenceToNerTagsMap.get(sentence);
        }

    	List<String> words = sentence.words();
    	List<Word> NPwords = NP.yieldWords();

        for (String w: words) {

        }


        StringBuilder builder = new StringBuilder();
        for (Word w : NPwords) {
            if(w.value().contains("COREF") || w.value().length() == 0){
                continue;
            }
            String word = w.value();
            builder.append(word);
            builder.append(" ");
        }
        String NPText = builder.toString().trim();
        if(NPText.length() <= 0 ){
            return "O";
        }
        String[] NPWords = NPText.split("\\s|\\n");

    	Hashtable<String, Integer> NERTagsForNP = new Hashtable<String, Integer>();
    	
    	for(String NPWord: NPWords){
    	    int indexOfNPWord = words.indexOf(NPWord);
            if(indexOfNPWord == -1){
                System.out.println();
            }
    		String NERTagForNPWord = NERtags.get(indexOfNPWord);
    		if(NERTagsForNP.containsKey(NERTagForNPWord)){
    			return NERTagForNPWord;
    			
    		}
    		else{
    			NERTagsForNP.put(NERTagForNPWord, 1);
    		}
    	}
    	
    	String w = NPWords[NPWords.length-1];
    	int idx = words.indexOf(w);
    	String tag = NERtags.get(idx);
    	return tag;	
    }

    public ArrayList<Tree> getNpNodesUptilNodeInSentence(Tree node, ArrayList<Tree> npNpdesList, Tree sentence){
        ArrayList<Tree> listToReturn = new ArrayList<Tree>();
        int nodeNumber = node.nodeNumber(sentence);
        for (int i = 0; i < npNpdesList.size(); i++) {
            Tree n = npNpdesList.get(i);
            int nNodeNumber = n.nodeNumber(sentence);
            if(nNodeNumber < nodeNumber){
                listToReturn.add(n);
            }
        }
        return listToReturn;
    }
}
