package ga.coreference.main;

import edu.stanford.nlp.ie.machinereading.structure.MachineReadingAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.simple.Sentence;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.StringUtils;

import java.util.List;
import java.util.Properties;

/**
 * Created by tejas on 06/11/16.
 */
public class FeatureMatcher {

    public static void main(String[] args) {
        doesGenderMatch(null,null,null);
    }

    public static boolean doesFeatureMatch(Tree markedNode, Tree sentenceForMarkedNode, CandidateNP candidateNode, Tree sentenceForCandidateNode){
	    boolean completeFeatureMatch = false;
		String markedNodeText = TreeHelper.getInstance().getTextValueForTree(markedNode, true);
		String candidateNodeText = TreeHelper.getInstance().getTextValueForTree(candidateNode.getNounPhrase(), true);
		if(candidateNodeText.contains("COREF")){
			return false;
		}

        boolean NERMatch = false;
        boolean stringMatch = false;
        boolean numberMatch = false;
		if(doesStringMatch(markedNodeText, candidateNodeText)){
			stringMatch = true;
		}


    	//NER match
    	//NERMatch = doesNERMatch(markedNode, sentenceForMarkedNode, candidateNode);
    	
    	//Number agreement
        //numberMatch = doesNumberMatch(markedNode, sentenceForMarkedNode, candidateNode);

        //Gender match

        return stringMatch;
    }
	
	public static boolean doesFeatureMatch(Tree markedNode, CandidateNP candidateNode){
		String markedNodeText = TreeHelper.getInstance().getTextValueForTree(markedNode, true);
    	String candidateNodeText = TreeHelper.getInstance().getTextValueForTree(candidateNode.getNounPhrase(), true);
        if(candidateNodeText.contains("COREF")){
            return false;
        }
    	if(doesStringMatch(markedNodeText, candidateNodeText)){
    		return true;
    	}
    	return false;
	}
	
	private static boolean doesStringMatch(String markedNodeText, String candidateNodeText){
	    //Working with 20% etc
        String headNoun = getHeadNounFromString(markedNodeText);
    	if(markedNodeText.equalsIgnoreCase(candidateNodeText)) {
    	    return true;
        }
    	else if(candidateNodeText.matches(markedNodeText) || StringUtils.find(markedNodeText, candidateNodeText)){
    		return true;
    	}
    	else if(candidateNodeText.matches(headNoun) || StringUtils.find(headNoun, candidateNodeText)){
    	    return true;
        }
        return false;




//        markedNodeText = markedNodeText.toLowerCase().trim();
//        candidateNodeText.toLowerCase().trim();
//
//        if(POSUtility.isArticle(markedNodeText) || POSUtility.isArticle(candidateNodeText) || candidateNodeText.length() == 1){
//            return false;
//        }
//        String[] markedNodeArray = markedNodeText.split("\\s");
//        String[] candidateNodeArray = candidateNodeText.split("\\s");
//
//        for (int i = 0; i < markedNodeArray.length ; i++) {
//            String marked = markedNodeArray[i];
//            if(marked.length() == 1 || POSUtility.isArticle(marked)){
//                continue;
//            }
//            for (int j = 0; j < candidateNodeArray.length ; j++) {
//                String candidate = candidateNodeArray[j];
//                if(candidate.length() == 1 || POSUtility.isArticle(candidate)){
//                    continue;
//                }
//                if(marked.equals(candidate) || candidate.contains(marked)){
//                    return true;
//                }
//            }
//        }
//        return false;





//        if(markedNodeArray.length == 1 && candidateNodeArray.length != 1){
//            for (int i = 0; i < candidateNodeArray.length ; i++) {
//                String str = candidateNodeArray[i];
//                return str.equalsIgnoreCase(markedNodeArray[0]) || str.toLowerCase().contains(markedNodeArray[0].toLowerCase());
//            }
//        }
//        else if(candidateNodeArray.length == 1 && markedNodeArray.length != 1){
//            for (int i = 0; i < markedNodeArray.length ; i++) {
//                String str = markedNodeArray[i];
//                return str.equalsIgnoreCase(candidateNodeArray[0]) || str.toLowerCase().contains(candidateNodeArray[0].toLowerCase());
//            }
//        }
//        else {
//            for (int i = 0; i < candidateNodeArray.length ; i++) {
//                for (int j = 0; j < markedNodeArray.length ; j++) {
//                    String str1 = candidateNodeArray[i];
//                    String str2 = markedNodeArray[j];
//                    return str1.equalsIgnoreCase(str2) || str1.toLowerCase().contains(str2.toLowerCase()) || str2.toLowerCase().contains(str1.toLowerCase());
//                }
//            }
//        }
//        return false;
	}
	
	private static boolean doesNERMatch(Tree markedNode, Tree markedNodeSentence, CandidateNP candidateNode){
        String markedNodeNERTag = "";
        String candidateNodeNERTag = "";
        List<Sentence> markedNodeSent = TreeHelper.getInstance().getSentenceForTree(markedNodeSentence, true);
        List<Sentence> candidateNodeSent = TreeHelper.getInstance().getSentenceForTree(candidateNode.getSentenceRoot(), true);
        for(Sentence sent1: markedNodeSent){
            markedNodeNERTag = TreeHelper.getInstance().findNERTagForNP(sent1, markedNode);
        }
        for(Sentence sent2: candidateNodeSent){
            candidateNodeNERTag = TreeHelper.getInstance().findNERTagForNP(sent2, candidateNode.getNounPhrase());
        }
        if(markedNodeNERTag.equals(candidateNodeNERTag)){
            return true;
        }
        return false;
	}

	private static boolean doesNumberMatch(Tree markedNode, Tree sentenceForMarkedNode, CandidateNP candidateNP){
	    boolean numberMatch = false;
        boolean isMarkedNodeSingular = false;
        boolean isCandidateNodeSingular = false;
        isMarkedNodeSingular = POSUtility.isSingular(sentenceForMarkedNode, markedNode);
        isCandidateNodeSingular = POSUtility.isSingular(candidateNP.getSentenceRoot(), candidateNP.getNounPhrase());
        if(isCandidateNodeSingular == isMarkedNodeSingular){
            numberMatch = true;
        }
        return numberMatch;
	}

	private static boolean doesGenderMatch(Tree markedNode, Tree sentenceForMarkedNode, CandidateNP candidateNP){
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,parse,gender");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        Annotation document = new Annotation("Ellen has expressed concern\n" +
                "that a plane crash into a plutonium storage bunker at Pantex could\n" +
                "spread radioactive smoke for miles. Plutonium, a highly radioactive\n" +
                "element, causes cancer if inhaled");
        pipeline.annotate(document);

        for (CoreMap sentence : document.get(CoreAnnotations.SentencesAnnotation.class)) {
            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                System.out.print(token.value());
                System.out.print(", Gender: ");
                System.out.println(token.get(MachineReadingAnnotations.GenderAnnotation.class));
            }
        }
        return false;
    }

    private static String getHeadNounFromString(String nodeText){
        String[] textArray = nodeText.split(" ");
        return textArray[textArray.length - 1];
    }
}
