package group13.depchain.blockchain;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import group13.depchain.blockchain.account.BlockchainAccount;
import group13.depchain.blockchain.account.ContractAccount;
import group13.depchain.blockchain.account.EOAAccount;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;

public class BlockchainState {
    private Dictionary<String, BlockchainAccount> accounts = new Hashtable<>();
    private long blockId = 0;

    public BlockchainState() {
    }

    public BlockchainState(BlockchainAccount acc) {
        accounts.put(acc.getAddress(), acc);
    }

    public BlockchainState(BlockchainAccount[] accs) {
        for (BlockchainAccount a : accs) {
            accounts.put(a.getAddress(), a);
        }
    }

    public void executeBlock(Block block) {
        for (Transaction tx : block.getTransactions()) {
            if (tx.isTransfer()) {
                BlockchainAccount from = getAccount(tx.getFrom());
                BlockchainAccount to = getAccount(tx.getTo());

                if (from == null) {
                    from = new EOAAccount(tx.getFrom(), 0);
                    insertAccount(from);
                }

                if (to == null) {
                    to = new EOAAccount(tx.getTo(), 0);
                    insertAccount(to);
                }

                from.executeTransaction(this, tx);
                to.executeTransaction(this, tx);
            } else {
                BlockchainAccount contract = getAccount(tx.getTo());

                if (contract == null) {
                    // TODO: support contract deployment?
                    contract = new ContractAccount(tx.getTo(), "");
                    insertAccount(contract);
                }

                contract.executeTransaction(this, tx);
            }
        }

        save(block);
        blockId++;

        // debug
        System.out.println(this.toString());
    }

    public void setBlockId(long blockId) {
        this.blockId = blockId;
    }

    public void insertAccount(BlockchainAccount account) {
        accounts.put(account.getAddress(), account);
    }

    public Dictionary<String, BlockchainAccount> getAccounts() {
        return accounts;
    }

    public BlockchainAccount getAccount(String address) {
        return accounts.get(address);
    }

    public void save(Block block) {
        ObjectMapper objectMapper = new ObjectMapper();
        File statesDirectory = new File("./blockchain/states");
        if (!statesDirectory.exists()) {
            statesDirectory.mkdir();
        }

        File blockJSON = new File(statesDirectory, "block" + Long.toString(blockId) + ".json");
        HashMap<String, Object> blockMap = new HashMap<>();

        blockMap.put("block_hash", block.getBlockHash());
        blockMap.put("previous_block_hash", block.getPreviousBlockHash());
        blockMap.put("transactions", block.getTransactions());

        HashMap<String, Object> stateMap = new HashMap<>();
        for (Enumeration<String> keys = accounts.keys(); keys.hasMoreElements();) {
            String key = keys.nextElement();
            BlockchainAccount account = accounts.get(key);
            HashMap<String, Object> accountMap = new HashMap<>();

            accountMap.put("balance", account.getBalance());
            if (account instanceof ContractAccount) {
                accountMap.put("code", ((ContractAccount) account).getContractCode());
                accountMap.put("storage", ((ContractAccount) account).getStorage());
            } else if (account instanceof EOAAccount) {
                accountMap.put("nonce", ((EOAAccount) account).getNonce());
            }

            stateMap.put(key, accountMap);
        }

        blockMap.put("state", stateMap);

        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(blockJSON, blockMap);
        } catch (IOException e) {
            System.out.println("Error writing block to file: " + e.getMessage());
        }
    }

    public static BlockchainState load(String dir) throws Exception {
        long id = getLatestBlockNum(dir);
        if (id == -1) {
            throw new Exception("No block files found in directory: " + dir);
        }
        File file = BlockchainState.getBlockFile(dir, id);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(file);

        JsonNode stateNode = root.get("state");
        if (stateNode == null || !stateNode.isObject()) {
            throw new Exception("Invalid state file format");
        }

        Dictionary<String, BlockchainAccount> accounts = new Hashtable<>();
        Iterator<Map.Entry<String, JsonNode>> fields = stateNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String address = entry.getKey();
            JsonNode accountNode = entry.getValue();
            long balance = accountNode.get("balance").asLong();

            if (accountNode.has("code")) {
                String code = accountNode.get("code").asText();
                ContractAccount contractAccount = new ContractAccount(address, code);
                contractAccount.setBalance(balance);

                if (accountNode.has("storage")) {
                    JsonNode storageNode = accountNode.get("storage");
                    Dictionary<String, String> storage = new Hashtable<>();
                    Iterator<Map.Entry<String, JsonNode>> storageFields = storageNode.fields();
                    while (storageFields.hasNext()) {
                        Map.Entry<String, JsonNode> storageEntry = storageFields.next();
                        String key = storageEntry.getKey();
                        String value = storageEntry.getValue().asText();
                        storage.put(key, value);
                    }
                    contractAccount.setStorage(storage);
                }

                accounts.put(address, contractAccount);
            } else {
                EOAAccount eoaAccount = new EOAAccount(address, accountNode.get("nonce").asLong());
                eoaAccount.setBalance(balance);
                accounts.put(address, eoaAccount);
            }
        }

        BlockchainState state = new BlockchainState();
        for (Enumeration<String> keys = accounts.keys(); keys.hasMoreElements();) {
            String key = keys.nextElement();
            BlockchainAccount account = accounts.get(key);
            state.insertAccount(account);
        }
        state.setBlockId(id + 1);

        // debug
        System.out.println(state.toString());
        return state;
    }

    public static long getLatestBlockNum(String dir) {
        File statesDirectory = new File(dir);
        if (!statesDirectory.exists() || !statesDirectory.isDirectory()) {
            throw new IllegalArgumentException("Invalid directory: " + dir);
        }

        File[] files = statesDirectory.listFiles((d, name) -> name.startsWith("block") && name.endsWith(".json"));
        if (files == null || files.length == 0) {
            throw new IllegalStateException("No block files found in directory: " + dir);
        }

        Pattern pattern = Pattern.compile("^block(\\d+)\\.json$");
        long maxId = -1;

        for (File file : files) {
            Matcher m = pattern.matcher(file.getName());
            if (m.matches()) {
                long id = Long.parseLong(m.group(1));
                if (id > maxId) {
                    maxId = id;
                }
            }
        }
        return maxId;
    }

    private static File getBlockFile(String dir, long id) {
        return new File(dir, "block" + Long.toString(id) + ".json");
    }

    public static void createGenesisBlock() throws IOException {
        BlockchainState genesisState = new BlockchainState();

        String deployerAddress = "0x3a89a0f8dfaef2cad42e124049ee152bb01edc85";
        String blacklistAddress = "0x1234abcd1234dead4321abcd4321000000000000";
        String istCoinAddress = "0x4321dcbA4321daed1234dcba1234000000001ced";

        String counterAddress = "0x4321dcbA4321daed1234dcba123400000000abcd";

        // Deployer EOA
        EOAAccount deployer = new EOAAccount(deployerAddress, 0);
        deployer.setBalance(1000_000_000_000L);
        genesisState.insertAccount(deployer);

        // Blacklist Contract
        String blacklistBytecode = "0x"
                + "6080604052348015600e575f80fd5b50600280546001600160a01b031916339081179091555f908152600160208190526040909120805460ff191690911790556104218061004c5f395ff3fe608060405234801561000f575f80fd5b506004361061007a575f3560e01c8063537df3b611610058578063537df3b6146100e657806370480275146100f95780638da5cb5b1461010c578063fe575a8714610137575f80fd5b80631785f53c1461007e57806324d7806c1461009357806344337ea1146100d3575b5f80fd5b61009161008c3660046103be565b610162565b005b6100be6100a13660046103be565b6001600160a01b03165f9081526001602052604090205460ff1690565b60405190151581526020015b60405180910390f35b6100916100e13660046103be565b6101f9565b6100916100f43660046103be565b610291565b6100916101073660046103be565b610326565b60025461011f906001600160a01b031681565b6040516001600160a01b0390911681526020016100ca565b6100be6101453660046103be565b6001600160a01b03165f9081526020819052604090205460ff1690565b6002546001600160a01b031633146101b15760405162461bcd60e51b815260206004820152600d60248201526c2737ba103a34329037bbb732b960991b60448201526064015b60405180910390fd5b6001600160a01b0381165f81815260016020526040808220805460ff19169055517fa3b62bc36326052d97ea62d63c3d60308ed4c3ea8ac079dd8499f1e9c4f80c0f9190a250565b335f9081526001602052604090205460ff166102465760405162461bcd60e51b815260206004820152600c60248201526b2737ba1030b71030b236b4b760a11b60448201526064016101a8565b6001600160a01b0381165f81815260208190526040808220805460ff19166001179055517ff9b68063b051b82957fa193585681240904fed808db8b30fc5a2d2202c6ed6279190a250565b335f9081526001602052604090205460ff166102de5760405162461bcd60e51b815260206004820152600c60248201526b2737ba1030b71030b236b4b760a11b60448201526064016101a8565b6001600160a01b0381165f81815260208190526040808220805460ff19169055517f2b6bf71b58b3583add364b3d9060ebf8019650f65f5be35f5464b9cb3e4ba2d49190a250565b6002546001600160a01b031633146103705760405162461bcd60e51b815260206004820152600d60248201526c2737ba103a34329037bbb732b960991b60448201526064016101a8565b6001600160a01b0381165f818152600160208190526040808320805460ff1916909217909155517f44d6d25963f097ad14f29f06854a01f575648a1ef82f30e562ccd3889717e3399190a250565b5f602082840312156103ce575f80fd5b81356001600160a01b03811681146103e4575f80fd5b939250505056fea264697066735822122014b61bb4fd11e85cd63a47830b42e3f3aeae5cb2dd86b69127b2236903380a6264736f6c634300081a0033";
        ContractAccount blacklistAccount = new ContractAccount(blacklistAddress, blacklistBytecode, deployerAddress);
        blacklistAccount.setBalance(0);
        genesisState.insertAccount(blacklistAccount);

        // ISTCoin Contract
        long totalSupply = 100000000;
        String baddr = "1234abcd1234dead4321abcd4321000000000000";
        String istCoinBytecode = "0x"
                + "608060405234801561000f575f80fd5b50604051610ed5380380610ed583398101604081905261002e91610234565b6040518060400160405280600781526020016624a9aa21b7b4b760c91b815250604051806040016040528060038152602001621254d560ea1b81525081600390816100799190610306565b5060046100868282610306565b5050600580546001600160a01b0319166001600160a01b038416179055506100ca336100b0600290565b6100bb90600a6104b9565b6100c590856104ce565b6100d1565b50506104f8565b6001600160a01b0382166100ff5760405163ec442f0560e01b81525f60048201526024015b60405180910390fd5b61010a5f838361010e565b5050565b6001600160a01b038316610138578060025f82825461012d91906104e5565b909155506101a89050565b6001600160a01b0383165f908152602081905260409020548181101561018a5760405163391434e360e21b81526001600160a01b038516600482015260248101829052604481018390526064016100f6565b6001600160a01b0384165f9081526020819052604090209082900390555b6001600160a01b0382166101c4576002805482900390556101e2565b6001600160a01b0382165f9081526020819052604090208054820190555b816001600160a01b0316836001600160a01b03167fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef8360405161022791815260200190565b60405180910390a3505050565b5f8060408385031215610245575f80fd5b825160208401519092506001600160a01b0381168114610263575f80fd5b809150509250929050565b634e487b7160e01b5f52604160045260245ffd5b600181811c9082168061029657607f821691505b6020821081036102b457634e487b7160e01b5f52602260045260245ffd5b50919050565b601f82111561030157805f5260205f20601f840160051c810160208510156102df5750805b601f840160051c820191505b818110156102fe575f81556001016102eb565b50505b505050565b81516001600160401b0381111561031f5761031f61026e565b6103338161032d8454610282565b846102ba565b6020601f821160018114610365575f831561034e5750848201515b5f19600385901b1c1916600184901b1784556102fe565b5f84815260208120601f198516915b828110156103945787850151825560209485019460019092019101610374565b50848210156103b157868401515f19600387901b60f8161c191681555b50505050600190811b01905550565b634e487b7160e01b5f52601160045260245ffd5b6001815b600184111561040f578085048111156103f3576103f36103c0565b600184161561040157908102905b60019390931c9280026103d8565b935093915050565b5f82610425575060016104b3565b8161043157505f6104b3565b816001811461044757600281146104515761046d565b60019150506104b3565b60ff841115610462576104626103c0565b50506001821b6104b3565b5060208310610133831016604e8410600b8410161715610490575081810a6104b3565b61049c5f1984846103d4565b805f19048211156104af576104af6103c0565b0290505b92915050565b5f6104c760ff841683610417565b9392505050565b80820281158282048414176104b3576104b36103c0565b808201808211156104b3576104b36103c0565b6109d0806105055f395ff3fe608060405234801561000f575f80fd5b5060043610610090575f3560e01c8063313ce56711610063578063313ce567146100fa57806370a082311461010957806395d89b4114610131578063a9059cbb14610139578063dd62ed3e1461014c575f80fd5b806306fdde0314610094578063095ea7b3146100b257806318160ddd146100d557806323b872dd146100e7575b5f80fd5b61009c610184565b6040516100a99190610828565b60405180910390f35b6100c56100c0366004610878565b610214565b60405190151581526020016100a9565b6002545b6040519081526020016100a9565b6100c56100f53660046108a0565b61022d565b604051600281526020016100a9565b6100d96101173660046108da565b6001600160a01b03165f9081526020819052604090205490565b61009c6103a9565b6100c5610147366004610878565b6103b8565b6100d961015a3660046108f3565b6001600160a01b039182165f90815260016020908152604080832093909416825291909152205490565b60606003805461019390610924565b80601f01602080910402602001604051908101604052809291908181526020018280546101bf90610924565b801561020a5780601f106101e15761010080835404028352916020019161020a565b820191905f5260205f20905b8154815290600101906020018083116101ed57829003601f168201915b5050505050905090565b5f3361022181858561052c565b60019150505b92915050565b60055460405163fe575a8760e01b81526001600160a01b0385811660048301525f92169063fe575a8790602401602060405180830381865afa158015610275573d5f803e3d5ffd5b505050506040513d601f19601f82011682018060405250810190610299919061095c565b156102e35760405162461bcd60e51b815260206004820152601560248201527414d95b99195c881a5cc8189b1858dadb1a5cdd1959605a1b60448201526064015b60405180910390fd5b60055460405163fe575a8760e01b81526001600160a01b0385811660048301529091169063fe575a8790602401602060405180830381865afa15801561032b573d5f803e3d5ffd5b505050506040513d601f19601f8201168201806040525081019061034f919061095c565b156103965760405162461bcd60e51b8152602060048201526017602482015276149958d95a5d995c881a5cc8189b1858dadb1a5cdd1959604a1b60448201526064016102da565b6103a184848461053e565b949350505050565b60606004805461019390610924565b60055460405163fe575a8760e01b81523360048201525f916001600160a01b03169063fe575a8790602401602060405180830381865afa1580156103fe573d5f803e3d5ffd5b505050506040513d601f19601f82011682018060405250810190610422919061095c565b156104675760405162461bcd60e51b815260206004820152601560248201527414d95b99195c881a5cc8189b1858dadb1a5cdd1959605a1b60448201526064016102da565b60055460405163fe575a8760e01b81526001600160a01b0385811660048301529091169063fe575a8790602401602060405180830381865afa1580156104af573d5f803e3d5ffd5b505050506040513d601f19601f820116820180604052508101906104d3919061095c565b1561051a5760405162461bcd60e51b8152602060048201526017602482015276149958d95a5d995c881a5cc8189b1858dadb1a5cdd1959604a1b60448201526064016102da565b61052533848461053e565b9392505050565b6105398383836001610561565b505050565b5f3361054b858285610634565b6105568585856106aa565b506001949350505050565b6001600160a01b03841661058a5760405163e602df0560e01b81525f60048201526024016102da565b6001600160a01b0383166105b357604051634a1406b160e11b81525f60048201526024016102da565b6001600160a01b038085165f908152600160209081526040808320938716835292905220829055801561062e57826001600160a01b0316846001600160a01b03167f8c5be1e5ebec7d5bd14f71427d1e84f3dd0314c0f7b2291e5b200ac8c7c3b9258460405161062591815260200190565b60405180910390a35b50505050565b6001600160a01b038381165f908152600160209081526040808320938616835292905220545f1981101561062e578181101561069c57604051637dc7a0d960e11b81526001600160a01b038416600482015260248101829052604481018390526064016102da565b61062e84848484035f610561565b6001600160a01b0383166106d357604051634b637e8f60e11b81525f60048201526024016102da565b6001600160a01b0382166106fc5760405163ec442f0560e01b81525f60048201526024016102da565b6105398383836001600160a01b03831661072c578060025f828254610721919061097b565b9091555061079c9050565b6001600160a01b0383165f908152602081905260409020548181101561077e5760405163391434e360e21b81526001600160a01b038516600482015260248101829052604481018390526064016102da565b6001600160a01b0384165f9081526020819052604090209082900390555b6001600160a01b0382166107b8576002805482900390556107d6565b6001600160a01b0382165f9081526020819052604090208054820190555b816001600160a01b0316836001600160a01b03167fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef8360405161081b91815260200190565b60405180910390a3505050565b602081525f82518060208401528060208501604085015e5f604082850101526040601f19601f83011684010191505092915050565b80356001600160a01b0381168114610873575f80fd5b919050565b5f8060408385031215610889575f80fd5b6108928361085d565b946020939093013593505050565b5f805f606084860312156108b2575f80fd5b6108bb8461085d565b92506108c96020850161085d565b929592945050506040919091013590565b5f602082840312156108ea575f80fd5b6105258261085d565b5f8060408385031215610904575f80fd5b61090d8361085d565b915061091b6020840161085d565b90509250929050565b600181811c9082168061093857607f821691505b60208210810361095657634e487b7160e01b5f52602260045260245ffd5b50919050565b5f6020828403121561096c575f80fd5b81518015158114610525575f80fd5b8082018082111561022757634e487b7160e01b5f52601160045260245ffdfea264697066735822122048f18d1d23c0c5c4f76d9b3122afbd7317de4b9e41364f7827d27041faab674564736f6c634300081a0033"
                + String.format("%064x", totalSupply)
                + String.format("%064x", new BigInteger(baddr, 16));
        ContractAccount istAccount = new ContractAccount(istCoinAddress, istCoinBytecode, deployerAddress);
        istAccount.setBalance(0);
        genesisState.insertAccount(istAccount);

        // Counter Contract
        String counterBytecode = "0x"
                + "6080604052348015600e575f80fd5b5060025f5561011d806100205f395ff3fe6080604052348015600e575f80fd5b5060043610603a575f3560e01c80637527836214603e57806387fa4bc214604f578063d09de08a146063575b5f80fd5b604d60493660046092565b6069565b005b5f5460405190815260200160405180910390f35b604d607f565b805f808282546077919060bc565b909155505050565b5f80549080608b8360d2565b9190505550565b5f6020828403121560a1575f80fd5b5035919050565b634e487b7160e01b5f52601160045260245ffd5b8082018082111560cc5760cc60a8565b92915050565b5f6001820160e05760e060a8565b506001019056fea26469706673582212207a1750d0e49faa8c77f2408bc19a0ab52b848f5dd0dbe759bb55304204abfa2664736f6c634300081a0033";
        ContractAccount counterAccount = new ContractAccount(counterAddress, counterBytecode, deployerAddress);
        counterAccount.setBalance(0);
        genesisState.insertAccount(counterAccount);

        // Create and Save Genesis Block
        Block genesisBlock = new Block();
        genesisState.save(genesisBlock);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("BlockchainState:\n");
        sb.append("Block ID: ").append(blockId).append("\n");
        sb.append("Accounts:\n");
        for (Enumeration<String> keys = accounts.keys(); keys.hasMoreElements();) {
            String key = keys.nextElement();
            BlockchainAccount account = accounts.get(key);
            sb.append(account.toString()).append("\n");
        }
        return sb.toString();
    }

}
