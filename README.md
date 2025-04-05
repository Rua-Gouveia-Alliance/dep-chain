# dep-chain

## Compilation

To generate the necessary sources, compile and package the code, run:

```
$ mvn clean install -pl blockchain -am
$ mvn -pl blockchain web3j:generate-sources
```

## Demo

We have prepared a demo on the Blockchain Members deciding on a value with Byzantine Consensus.

For executing this you just need to run 6 different instances of the members with argument from 0 to 5, for example for the leader (0):

```
$ mvn -pl blockchain exec:java -Dexec.args="0 <nClients>"
```

The processes will successfully decide on the string "ABAB". This will be printed in the terminal.
