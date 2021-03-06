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

    public static boolean doesFeatureMatch(Tree markedNode, Tree sentenceForMarkedNode, CandidateNP candidateNode, Tree sentenceForCandidateNode, boolean stringMatch){
	    boolean completeFeatureMatch = false;
		String markedNodeText = TreeHelper.getInstance().getTextValueForTree(markedNode, true);
		String candidateNodeText = TreeHelper.getInstance().getTextValueForTree(candidateNode.getNounPhrase(), true);
		if(candidateNodeText.contains("COREF")){
			return false;
		}

        if(stringMatch){
			if(doesStringMatch(markedNodeText, candidateNodeText)){
				return true;
			}
			else return false;
        }
        else{
        	if(doesNERMatch(markedNode, sentenceForMarkedNode, candidateNode)){
        		return true;
        	}
        	else return false;
        }

    	//NER match
    	//NERMatch = doesNERMatch(markedNode, sentenceForMarkedNode, candidateNode);
    	
    	//Number agreement
        //numberMatch = doesNumberMatch(markedNode, sentenceForMarkedNode, candidateNode);

        //Gender match

       
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
	    markedNodeText = markedNodeText.toLowerCase();
        candidateNodeText = candidateNodeText.toLowerCase();
        if(POSUtility.isArticle(markedNodeText) || POSUtility.isPronoun(markedNodeText)){
            return false;
        }
        if(POSUtility.isArticle(candidateNodeText) || POSUtility.isPronoun(candidateNodeText)){
            return false;
        }
        if(candidateNodeText.length() <= 1){
            return false;
        }
        String markedHeadNoun = getHeadNounFromString(markedNodeText);
    	if(markedNodeText.equalsIgnoreCase(candidateNodeText)) {
    	    return true;
        }
        else if(candidateNodeText.matches(markedNodeText) || StringUtils.find(markedNodeText, candidateNodeText)){
    		return true;
    	}
    	else if(candidateNodeText.matches(markedHeadNoun) || StringUtils.find(markedHeadNoun, candidateNodeText)){
    	    return true;
        }



//        else if(candidateNodeText.contains(markedNodeText + " ") || candidateNodeText.contains(" " + markedNodeText)
//                || markedNodeText.contains(candidateNodeText + " ") || markedNodeText.contains(" " + candidateNodeText)){
//            return  true;
//        }
//        else if(candidateNodeText.contains(headNoun + " ") || candidateNodeText.contains(" " + headNoun)
//                || headNoun.contains(candidateNodeText + " ") || headNoun.contains(" " + candidateNodeText)){
//            return  true;
//        }


//    	else if(candidateNodeText.matches("(.*)" + markedNodeText + "(.*)") || StringUtils.find(markedNodeText, "(.*)" + candidateNodeText + "(.*)")){
//    		return true;
//    	}
//    	else if(candidateNodeText.matches("(.*)" + headNoun + "(.*)" ) || StringUtils.find(headNoun, "(.*)" + candidateNodeText  + "(.*)")){
//    	    return true;
//        }
        return false;

	}
	
	public static boolean doesNERMatch(Tree markedNode, Tree markedNodeSentence, CandidateNP candidateNode){
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

	public static boolean doesNumberMatch(Tree markedNode, Tree sentenceForMarkedNode, CandidateNP candidateNP){
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

    public static String getHeadNounFromString(String nodeText){
        String[] textArray = nodeText.split(" ");
        return textArray[textArray.length - 1];
    }
}
