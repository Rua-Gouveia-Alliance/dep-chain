package group13.depchain.consensus;

import java.io.IOException;
import java.lang.Thread.State;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;

import java.net.SocketException;
import java.security.PrivateKey;
import group13.depchain.client.Client;
import group13.depchain.crypto.Util;
import group13.depchain.network.AuthenticatedPerfectLink;
import group13.depchain.network.ConditionalCollect;
import group13.depchain.network.OutputPredicate;
import group13.depchain.Messages.*;
import group13.depchain.util.MessageId;

/*
 *  1. Inicializado com um value state, state esse que ficou do processo anterior.
 *  2. Este value contém
 *      (1) timestamp/value pair mais recente num quorum de WRITE messages durante o epoch com timestamp valts;
 *      (2) a writeset of timestamp/value pairs with one entry for every value that this process has ever written, timestamp being of the omst recent epoch
 *  3. Read Phase:
 *      (1) Obtém os estados de todos os processos para determinar se existe um valor que já tenha sido bep-decidido.
 *      (2) Aqui não basta o líder fazer esta computação, TODOS os processos têm que repetir a computação e escrever um valor.
 *
 *
 * 1. Líder L faz broadcast de mensagem READ a todos os processos, o que faz com que todos os processos invoquem o CC.
 * 2. TODOS os processos escrevem uma mensagem [Estado, valts, val, writeset].
 * 3. CC procura por um valor (de um Epoch anterior) que tenha que ser escrito na fase WRITE
 *      a. Para isto, usa-se o predicado sound(S) no vetor S de STATE messages.
 *      b. Uma entrada de S pode ser definida e conter uma mensagem de Estado ou ser UNDEFINED
 *      c. Para qq defined, há (1) timestamp ts, (2) valor v e (3) writeset ws
 *      d. Se L é correto, pelo menos N-f entradas de S são definidas.
 *      e. sound(.) tem 2 condições, em conjunto determinam se um processo já tinha um valor bep-decidded v num Epoch anterior. Se sim, v tem que ser escrito.
 *          1. Procura por um par de timestamp/value com o MAIOR timestamp dentro de um quorum de entradas definidas em S.
 *          2. Determina se um valor v ocorre em algum writeset de uma entrada de S originada por um processo correto.
 *              Quando o ws de >f contêm (ts,v) com timestamp ts ou superior, então v é CERTIFICADO e algum processo escreveu v no Epoch ts ou depois. Predicado certifiedvalues(.)
 *      f. Para um par (ts,v) em S, diz-se que S binds ts to v se #(S) >= N-f e [1] quorumhighest(ts,v,S) && [2] certifiedvalue(ts,v,S)
 *      g. Quando #(S) >= N-f e timestamps = 0 (estado inicial), diz-se que S é unbound
 *      h. Então, o predicado sound(S) é TRUE sse exist (ts,v) tq binds(ts,v,S) ou unbound(S)
 *
 */

public class ByzantineConsensus {

    private ArrayList<Integer> procIds;
    private AuthenticatedPerfectLink al;
    private ConditionalCollect cc;
    private ArrayList<Message> written;
    private ArrayList<Message> accepted;
    private Epochstate epochstate;
    private ArrayList<Epochstate> states;
    private String currVal;
    private String tmpVal = null;
    private final int leaderId = 0;
    // Read Phase

    public OutputPredicate soundPredicate = (N, f, msgs) -> { return checkSoundPredicate(N,f,msgs); };

    // epochstate => (valts, val, writeset)
    public ByzantineConsensus(ArrayList<Integer> procIds, Epochstate epochstate,
            ConditionalCollect cc, AuthenticatedPerfectLink al) {
        
        this.al = al;
        this.cc = cc;
        this.procIds = procIds;
        this.written = new ArrayList<>();
        this.accepted = new ArrayList<>();
        this.epochstate = epochstate;
    }

    public void leaderPropose(String val) {
        // should check if it is leader process proposing?


        // TODO: may not be just a string
        if (this.currVal == null) { this.currVal = val; }     

        // Leader broadcasts READ message
        MessageId id = new MessageId(procIds.get(0));
        for (Integer i : this.procIds) {
            id = new MessageId(i);
            Message readMessage = Message.newBuilder().setCode(MessageCode.READ)
            .setMessage(ByteString.copyFrom(new byte[0])).setSender(leaderId)
            .setSeq(id.getSeq()).build();

            this.al.send(i, readMessage);
            id.next();
        }
    }

    public void deliverReadMessage(int process, MessageId id) {
        // May need to update epochstate HERE

        // When a process receives the READ message from the leader, it sends its current state 
        // trough the Conditional Collect, which signs it with a DS, to the leader process.
        Message received = al.deliver();
        MessageCode code = received.getCode();
        int senderId = received.getSender();
        if (code == MessageCode.READ && senderId == leaderId) {
            cc.send(leaderId, this.epochstate);
        }

    }
    
    // quorumHighest(ts, v, states) = true quando o [número de Epochstates com timestamp inferior a ts]+1 (próprio ts,v) é superior a (N+f)/2

    // certifiedValue(ts,v,states) = true quando o [número de Epochstates cujo writeset inclui pares (timestamp,val) com 
    // timestamp >= ts e val = v] é superior a f

    public boolean checkSoundPredicate(int total, int byzantine, ArrayList<Message> states) {
        assert total == states.size();

        int byzantineSafeCnt = 0, unboundCnt = 0, highestCnt = 0, certifiedCnt = 0;
        int unboundQuorumSize = (total + 1) / 2, quorumHighestSize = (total+byzantine)/2;

        Epochstate quorumHighest = states[0];

        for (Epochstate s : states) {
            if (s.getValue() != "Undefined") { byzantineSafeCnt++; }
            else if (s.getTimeStamp() == 0) { unboundCnt++; }

            // Quorum Highest
            if (s.getTimeStamp() > quorumHighest.getTimeStamp() || (s.getTimeStamp() == quorumHighest.getTimeStamp() && s.getValue().equals(quorumHighest.getValue()))) {
                quorumHighest = s;
                highestCnt++;   // IS THIS CORRECT?
            }
        }

        if (!(byzantineSafeCnt >= total-byzantine)) { return false; }
        if (!(unboundCnt >= unboundQuorumSize) && !(highestCnt > quorumHighestSize)) { return false; }

        // (byzantineSafeCnt >= total-byzantine) && (unboundCnt >= unboundQuorumSize || ((highestCnt > quorumHighestSize) && certifiedCnt > byzantine));
        //!(unboundCnt >= unboundQuorumSize) || !(highestCnt > quorumHighestSize))

        // Certified Value. A different for loop is used to have quorumHighest defined
        for (Epochstate s : states) {
            for (Pair<Integer, String> p : s.getWriteset()) {
                if (p.getLeft() >= quorumHighest.getTimeStamp() && p.getRight().equals(quorumHighest.getValue())) { certifiedCnt++; }
            }
        }

        // SHOULD THERE BE A SEPARATED FINDTMPVAL FUNCTION?
        this.tmpVal = null;
        int ts = quorumHighest.getTimeStamp();
        String val = quorumHighest.getValue();
        
        if (ts >= 0 && val != null && ((highestCnt > quorumHighestSize) && certifiedCnt > byzantine)) {
            this.tmpVal = val;
        } else if (val != null && unboundCnt >= unboundQuorumSize) { this.tmpVal = val; }

        return (unboundCnt >= unboundQuorumSize || ((highestCnt > quorumHighestSize) && certifiedCnt > byzantine));
    }

    public String writeTmpVal() {
        if (this.tmpVal != null) {

            for (Pair<Integer, String> p : this.epochstate.getWriteset()) {
                if (p.getRight().equals(this.tmpVal)) {
                    this.epochstate.removePair(p);
                    this.epochstate.addPair(new Pair<>(this.epochstate.getTimeStamp(), tmpVal));
                }
            }

            MessageId id = new MessageId(procIds.get(0));
            for (Integer i : this.procIds) {
                id = new MessageId(i);
                // TODO: como construir esta mensagem bem?????
                Message readMessage = Message.newBuilder().setCode(MessageCode.WRITE)
                .setMessage(ByteString.copyFrom(tmpVal).setSender(leaderId)
                .setSeq(id.getSeq()).build();

                this.al.send(i, readMessage);
                id.next();
            }
        }
    }

    public ArrayList<Message> getWritten() {
        return this.written;
    }

    public ArrayList<Message> getAccepted() {
        return this.accepted;
    }

    public Epochstate getEpochstate() {
        return this.epochstate;
    }

}
