package group13.depchain.consensus;

import java.util.List;
import java.util.ArrayList;
import group13.depchain.Messages.*;

public class EpochState {
    private int valts;
    private Block val;
    private List<WSEntry> writeset;

    public EpochState() {
        this.valts = 0;
        this.val = new Block();
        this.writeset = new ArrayList<WSEntry>();
    }

    public EpochState(int valts) {
        this.valts = valts;
        this.val = new Block();
        this.writeset = new ArrayList<WSEntry>();
    }

    public EpochState(StateMessage message) {
        this.valts = message.getValts();
        this.val = new Block(message.getVal());
        this.writeset = message.getWritesetList();
    }

    public int getValts() {
        return this.valts;
    }

    public void setValts(int valts) {
        this.valts = valts;
    }

    public Block getVal() {
        return this.val;
    }

    public void setVal(Block val) {
        this.val = val;
    }

    public List<WSEntry> getWriteset() {
        return this.writeset;
    }

    public void tryRemoveVal(Block val) {
        for (WSEntry e : this.writeset) {
            if (val.eq(e.getVal())) {
                this.writeset.remove(e);
            }
        }
    }

    public void addVal(Block val) {
        WSEntry entry =
                WSEntry.newBuilder().setValts(this.valts).setVal(val.toBlockMessage()).build();
        this.writeset.add(entry);
    }

    @Override
    public String toString() {
        String str = "{ valts: " + this.valts + ", val: " + this.val + "ws: [ ";
        for (WSEntry e : this.writeset)
            str += "( " + e.getValts() + ", " + e.getVal() + ") ";
        str += "] }";
        return str;
    }
}
