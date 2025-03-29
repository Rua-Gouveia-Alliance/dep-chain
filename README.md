# dep-chain

## TODO

1. Utilizador faz um pedido, através do client no terminal, de ou (1) transferir moedas ou (2) executar smartContract (ou visualizar estado da blockchain?)
2. Pedido do utilizador é traduzido por uma biblioteca (stage 1) num service request que os blockchain members recebem, ou seja uma transação da blockchain
3. Blockchain members adicionam a transação (por exemplo, transferir 5 ISTcoins de acc1 para acc2. Apenas o cliente associado à acc1 poderá realizar essa transação) 
    ao próximo bloco (B1), se esta for válida.
4. O bloco B1 terá já o estado do mundo em que as 5 ISTCoins saíram do balanço de acc1 e entraram no balanço de acc2.
5. O processo líder, através do algoritmo Byzantine Epoch R/W, decidirá quando se deve "escrever" este bloco e transmitirá para todos os outros processos

## Compilation

To generate the necessary sources, compile and package the code, run:

```
$ mvn clean install -pl blockchain -am
$ cd blockchain
$ mvn web3j:generate-sources
```

## Demo

We have prepared a demo on the Blockchain Members deciding on a value with Byzantine Consensus.

For executing this you just need to run 6 different instances of the members with argument from 0 to 5, for example for the leader (0):

```
$ mvn -pl blockchain exec:java -Dexec.mainClass="group13.depchain.BlockchainMember" -Dexec.args="0"
```

The processes will successfully decide on the string "ABAB". This will be printed in the terminal.
