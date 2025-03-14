package group13.depchain.consensus;

import java.util.List;
import java.util.ArrayList;
import group13.depchain.Messages.*;

public class EpochState {
    private int valts;
    private String val;
    private List<WSEntry> writeset;

    public EpochState() {
        this.valts = -1;
        this.val = "";
        this.writeset = new ArrayList<WSEntry>();
    }

    public EpochState(StateMessage message) {
        this.valts = message.getValts();
        this.val = message.getVal();
        this.writeset = message.getWritesetList();

    }

    public int getValts() {
        return this.valts;
    }

    public void setValts(int valts) {
        this.valts = valts;
    }

    public String getVal() {
        return this.val;
    }

    public void setVal(String val) {
        this.val = val;
    }

    public List<WSEntry> getWriteset() {
        return this.writeset;
    }

    public void tryRemoveVal(String val) {
        for (WSEntry e : this.writeset) {
            if (e.getVal() == val) {
                this.writeset.remove(e);
            }
        }
    }

    public void addVal(String val) {
        WSEntry entry = WSEntry.newBuilder().setValts(this.valts).setVal(val).build();
        this.writeset.add(entry);
    }

}
