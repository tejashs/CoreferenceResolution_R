package ga.coreference.main;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.StringUtils;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Created by tejas on 13/11/16.
 */
public class CoRefObject {
    private String ID;
    private String value;
    private int sentenceIndexInFile;
    private Tree sentenceTree;
    private Tree coRefTree;
    private Node coRefNode;
    private ArrayList<CandidateNP> candidates;

    public Tree tree() {
        return coRefTree;
    }

    public void setCoRefTree(Tree coRefTree) {
        this.coRefTree = coRefTree;
    }

    public CoRefObject(String ID, String value, Tree sentence, int sentenceIndexInFile, Node node) {
        this.ID = ID;
        setValue(value);
        this.sentenceTree = sentence;
        this.sentenceIndexInFile = sentenceIndexInFile;
        this.coRefNode = node;
        candidates = new ArrayList<CandidateNP>();
    }

    public ArrayList<CandidateNP> getCandidates() {
        return candidates;
    }

    public void setCandidates(ArrayList<CandidateNP> candidates) {
        this.candidates.clear();
        this.candidates.addAll(candidates);
    }

    public String getID() {
        return ID;
    }

    public String getValue() {
        return value;
    }

    public int getSentenceIndexInFile() {
        return sentenceIndexInFile;
    }

    public Tree getSentenceTree() {
        return sentenceTree;
    }

    private void setValue(String value){
        value = value.replaceAll("\\n", " ");
        String[] values = value.split("\\s");
        StringBuilder builder = new StringBuilder();
        for (String val: values) {
            builder.append(StringUtils.stripNonAlphaNumerics(val));
            builder.append(" ");
        }
        this.value = builder.toString().trim();

    }

    public Node node() {
        return coRefNode;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof CoRefObject)){
            return false;
        }
        CoRefObject coref2 = (CoRefObject) obj;
        return (this.getID().equals(coref2.getID())) && (this.getValue().equals(coref2.getValue()));
    }

    @Override
    public int hashCode() {
        return getValue().hashCode();
    }
}
