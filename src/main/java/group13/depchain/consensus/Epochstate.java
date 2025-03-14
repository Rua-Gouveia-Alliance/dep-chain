package group13.depchain.consensus;

import java.util.ArrayList;

import org.apache.commons.lang3.tuple.Pair;

public class Epochstate {
    private Integer valts;
    private String val;
    private ArrayList<Pair<Integer, String>> writeset;

    public Epochstate(Integer valts, String val, ArrayList<Pair<Integer, String>> writeset) {
        this.valts = valts;
        this.val = val;
        this.writeset = writeset;
    }

    public Integer getTimeStamp() {
        return this.valts;
    }

    public String getValue() {
        return this.val;
    }

    public ArrayList<Pair<Integer, String>> getWriteset() {
        return this.writeset;
    }

    public void removePair(Pair<Integer,String> toRemove) {
        this.writeset.remove(toRemove);
    }

    public void addPair(Pair<Integer,String> toAdd) {
        this.writeset.add(toAdd);
    }

}
