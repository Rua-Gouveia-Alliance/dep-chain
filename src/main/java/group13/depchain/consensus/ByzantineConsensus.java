package group13.depchain.consensus;


import java.io.IOException;
import java.lang.Thread.State;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.net.SocketException;
import java.security.PrivateKey;

import group13.depchain.client.Client;
import group13.depchain.crypto.Util;
import group13.depchain.network.AuthenticatedPerfectLink;
import group13.depchain.util.Message;
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

    private ArrayList<Client> processes;
    private Client leader;
    private AuthenticatedPerfectLink al;
    private ConditionalCollect cc;
    private ArrayList<Message> written;
    private ArrayList<Message> accepted;
    private Epochstate epochstate;
    private String currVal;
    // Read Phase
        
    // epochstate => (valts, val, writeset)
    public ByzantineConsensus(ArrayList<Client> procs, Client leader, Epochstate epochstate, ConditionalCollect cc) {
        this.cc = cc;
        this.processes = procs;
        this.leader = leader;
        this.written = new ArrayList<>();
        this.accepted = new ArrayList<>();
        this.epochstate = epochstate;
    }

    public void propose(String val) {
        if (this.currVal == null) {this.currVal = val;}

        // Leader broadcasts READ message
        /*
        for (p in this.processes) {
            if (p == this.leader) {continue;}

            this.leader.send(READ, this.currVal)
            OR could be done
            this.al.send(origin, destiny, (READ, this.currVal))

        }
        
        */
    }

    // When a process receives the READ message from the Leader, it calls upon the Conditional Collect Input, inputting its current Epochstate
    // Conditional Collect collects all these messages and outputs it - this is the list states[]
    // In states[], there is either states[p] = Epochstate e OR states[p] = UNDEFINED
    public void chooseTmpVal() {
        String tmpVal = null;
        /*  
            for s in states[]
            if (s.ts > 0 && s.val != null && cc.binds(ts, val, states)) {tmpval = s.val}
            elif unbound(states[]) && (states[L].val != null) {tmpval = states[L].val}

            if (tmpval != null) {
                if there's a ts associated with tmpval, switch that ts with current epoch ts (ets)
                then send WRITE message with tmpval to everyone
                if ()
            }
        */
    }

    /*  TODO: Conditional Collect must use sound(.) predicate on an N-vector S of states (this being states[])
        sound predicate has 2 conditions: (1) quorumhighest & (2) certifiedvalue. 
        S means states[]. #(S) means number of DEFINED entries in states[].
        For a pair (ts, val) in S, we say that S binds ts to val if (0) #(S) >= N-f && (1) quorumhighest(ts,val,S) == True && (2) certifiedvalue(ts,val,S) == True. binds(ts,val,S)
        When the ts of a quorum of entries in S is 0 (initial time), we say S is unbound, unbound(S)
        sound(S) is True if (1) S is unbound or (2) there exists a pair (ts,val) such that binds(ts,val,S)
        Every correct process initializes the cc primitive with the sound(.) predicate
    */    

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
