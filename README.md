# dep-chain

This project is divided in two parts: a byzantine consensus implementation, and a blockchain service.

There is also a client library that submits requests to the service for consensus and execution in the blockchain.
For testing purposes, we developed a small TUI application using the blockchain library with some functionalities.

This README explains how to compile, test, and execute, and presents a small demo of the project.

## Important locations

### Solidity contracts

The source of the solidity contracts are stored in `blockchain/src/solidity/contracts`. If you want to compile the contracts into bytecode you can run:

```sh
mvn -pl blockchain web3j:generate-sources
```

The resulting bytecode will be stored at `blockchain/target/generated-sources/solidity`.

### States

The states of the blockchain are stored at `blockchain/states`. It comes with the genesis block `block0.json`, but as each block is executed, more states will be added.

If the states directory doesn't exist, it will be created and the genesis block will be automatically generated.

### Keys

All public and private keys of the blockchain members are stored in `keys`.

The public and private keys of the clients are stored in `keys/clients`, and for convenience their addresses are also stored in the same place, although we never read from these files as we use the public keys to determine the addresses.

All the keys come pre-generated for convenience. If the `keys` directory doesn't exist, it will be created and all the keys will be automatically generated.

## Compilation and Unit Testing

In order to compile the whole project, including generating the necessary targets, and run the unit tests,
go to the root of the project and type the following:

```sh
$ mvn clean install
```

If you want faster compilation and don't desire to run the tests, you can type:

```sh
$ mvn clean install -DskipTests
```

## Executing

### Blockchain Members

First, to run the blockchain members, we recommend you open 5 different terminals and run the following from the root of the project (one command for each terminal):

```sh
$ mvn -pl blockchain exec:java -Dexec.args="0 5"
$ mvn -pl blockchain exec:java -Dexec.args="1 5"
$ mvn -pl blockchain exec:java -Dexec.args="2 5"
$ mvn -pl blockchain exec:java -Dexec.args="3 5"
$ mvn -pl blockchain exec:java -Dexec.args="4 5"
```

**Note 1**: The first argument represents the ID of the member to be executed, and the second is the total number of members.

**Note 2**: By default, member with ID 0 is the leader.

### Client

Next, to run the TUI app of the client, also from the root of the project:

```sh
$ mvn -pl client exec:java -Dexec.args="0"
```

**Note 3**: Here, the argument is the ID of the client.

**Note 4**: Client with ID 0 is the deployer of the IST Coin contract (owner of the blacklist) and starts with a big balance of Dep Coin and IST Coin, so it is particularly useful. You can run other clients with different IDs at the same time in other terminals.

## Demo

1. After the 5 blockchain members are running, open 2 clients in different terminals. A client with ID 0, and a client with ID 1.
2. We will now test the most basic transaction, sending Dep Coin between the two:
    1. Go to the client with ID 0 and check its balance (menu option 1).
    2. Still in the client with ID 0, use menu option 2 to execute a transaction.
    3. Use as receipient the address of client with ID 1. If you are using the default keys the address should be `0xb47dc6ee9aba1b31dbe92933fe5a38f4df15b8d8`.
    4. Type some amount to transfer.
    5. Check the balance of each client.
3. Now, we will test smart contract execution. To transfer some IST Coin between the two:
    1. In client with ID 0 go to menu option 3.
    2. Check your current balance by using menu option 2 and typing your address. If using the default: `0x3a89a0f8dfaef2cad42e124049ee152bb01edc85`.
    3. Go back into menu option 3 for contract execution and start a IST Coin transfer with menu option 1.
    4. Use as receipient the address of client with ID 1. Again, if you are using the default keys the address should be `0xb47dc6ee9aba1b31dbe92933fe5a38f4df15b8d8`.
    5. Type some amount to transfer.
    6. Check the balance of each client.
4. Finally, we will test the blacklist:
    1. With client 0 (owner of the blacklist), go into menu option 3.
    2. In the contract execution menu use option 3 to blacklist an account.
    3. Blacklist client 1. Again, the default address should be `0xb47dc6ee9aba1b31dbe92933fe5a38f4df15b8d8`.
    4. Try to make a transfer of IST Coin as done before, and look at the balance again to check that the transfer didn't go through.
    5. Remove client 1 from the blacklist by going into the contract execution menu and selecting option 4 (with client 0).
    6. Try to make the transfer again (which now should work again) and check the balances.

**Note 5**: After each action, a return value is displayed. In the case of the transfer after blacklisting the client, although the transfer returns `Done...`, what comes next is an error code that is returned by the EVM (because the client is blacklisted). In contrast, when it is successful, the return is `0x1` (true).