package ga.coreference.main;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

import java.util.List;
import java.util.Properties;

/**
 * Created by tejas on 13/11/16.
 */
public class ParseUtility {
    static StanfordCoreNLP pipeline;
    static {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse");
        pipeline = new StanfordCoreNLP(props);
    }

    public static List<CoreMap> getParsedSentences(String textToParse) {
        Annotation annotatedDoc = new Annotation(textToParse);
        pipeline.annotate(annotatedDoc);
        List<CoreMap> sentences = annotatedDoc.get(CoreAnnotations.SentencesAnnotation.class);
        return sentences;
    }
}
