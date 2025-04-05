package group13.depchain.blockchain;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import group13.depchain.blockchain.account.BlockchainAccount;
import group13.depchain.blockchain.account.ContractAccount;
import group13.depchain.blockchain.account.EOAAccount;
import group13.depchain.crypto.Util;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;

public class BlockchainState {
    private Dictionary<String, BlockchainAccount> accounts = new Hashtable<>();
    private long blockId = 0;
    private byte[] previousBlockHash = new byte[0];

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
        block.setPreviousBlockHash(previousBlockHash);
        block.hash();

        for (Transaction tx : block.getTransactions()) {
            boolean status;
            if (tx.isTransfer()) {
                BlockchainAccount from = getAccount(tx.getFrom());
                BlockchainAccount to = getAccount(tx.getTo());

                if (from == null) {
                    from = new EOAAccount(tx.getFrom(), 0);
                    insertAccount(from);
                } else if (!(from instanceof EOAAccount)) {
                    tx.setReturnData("From account is not an EOA.");
                    continue;
                }

                if (!((EOAAccount) from).validateNonce(tx.getNonce())) {
                    tx.setReturnData("Invalid nonce.");
                    continue;
                }

                if (to == null) {
                    to = new EOAAccount(tx.getTo(), 0);
                    insertAccount(to);
                }

                if (from.executeTransaction(this, tx))
                    status = to.executeTransaction(this, tx);
                else
                    status = false;
            } else {
                BlockchainAccount from = getAccount(tx.getFrom());
                BlockchainAccount contract = getAccount(tx.getTo());

                if (from == null) {
                    from = new EOAAccount(tx.getFrom(), 0);
                    insertAccount(from);
                } else if (!(from instanceof EOAAccount)) {
                    tx.setReturnData("From account is not an EOA.");
                    continue;
                }

                if (!((EOAAccount) from).validateNonce(tx.getNonce())) {
                    tx.setReturnData("Invalid nonce.");
                    continue;
                }

                if (contract == null || !(contract instanceof ContractAccount)) {
                    tx.setReturnData("Destination account is not a contract.");
                    continue;
                }

                status = contract.executeTransaction(this, tx);
            }

            String saved_return = tx.getReturnData();
            if (status) {
                tx.setReturnData("Done. " + saved_return);
            } else {
                tx.setReturnData("Failed. " + saved_return);
            }
        }

        save(block);

        // update state
        blockId++;
        previousBlockHash = block.getBlockHash();

        // debug
        System.out.println(this.toString());
    }

    public void setBlockId(long blockId) {
        this.blockId = blockId;
    }

    public void setPreviousBlockHash(byte[] previousBlockHash) {
        this.previousBlockHash = previousBlockHash;
    }

    @JsonIgnore
    public byte[] getPreviousBlockHash() {
        return this.previousBlockHash;
    }

    @JsonProperty("previousBlockHash")
    public String getPreviousBlockHashHex() {
        StringBuilder sb = new StringBuilder();
        for (byte b : this.previousBlockHash) {
            sb.append(String.format("%02x", b));
        }
        return "0x" + sb.toString();
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

    public long getAccountBalance(String address) {
        BlockchainAccount account = accounts.get(address);
        if (account != null) {
            return account.getBalance();
        }
        return 0;
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
        blockMap.put("previous_block_hash", previousBlockHash);

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
        state.setPreviousBlockHash(Util.hexStringToByteArray(root.get("block_hash").asText()));

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
        String istCoinAddress = "0x4321dcbA4321daed1234dcba1234000000001ced";
        String counterAddress = "0x4321dcbA4321daed1234dcba123400000000abcd";

        // Deployer EOA
        EOAAccount deployer = new EOAAccount(deployerAddress, 0);
        deployer.setBalance(1000_000_000_000L);
        genesisState.insertAccount(deployer);

        // ISTCoin Contract
        long totalSupply = 100000000;
        String istCoinBytecode = "0x"
                + "608060405234801561000f575f80fd5b506040516110af3803806110af83398101604081905261002e91610245565b6040518060400160405280600781526020016624a9aa21b7b4b760c91b815250604051806040016040528060038152602001621254d560ea1b815250816003908161007991906102f4565b50600461008682826102f4565b5050600780546001600160a01b031916339081179091555f818152600660205260409020805460ff191660011790556100dc91506100c2600290565b6100cd90600a6104a7565b6100d790846104bc565b6100e2565b506104e6565b6001600160a01b0382166101105760405163ec442f0560e01b81525f60048201526024015b60405180910390fd5b61011b5f838361011f565b5050565b6001600160a01b038316610149578060025f82825461013e91906104d3565b909155506101b99050565b6001600160a01b0383165f908152602081905260409020548181101561019b5760405163391434e360e21b81526001600160a01b03851660048201526024810182905260448101839052606401610107565b6001600160a01b0384165f9081526020819052604090209082900390555b6001600160a01b0382166101d5576002805482900390556101f3565b6001600160a01b0382165f9081526020819052604090208054820190555b816001600160a01b0316836001600160a01b03167fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef8360405161023891815260200190565b60405180910390a3505050565b5f60208284031215610255575f80fd5b5051919050565b634e487b7160e01b5f52604160045260245ffd5b600181811c9082168061028457607f821691505b6020821081036102a257634e487b7160e01b5f52602260045260245ffd5b50919050565b601f8211156102ef57805f5260205f20601f840160051c810160208510156102cd5750805b601f840160051c820191505b818110156102ec575f81556001016102d9565b50505b505050565b81516001600160401b0381111561030d5761030d61025c565b6103218161031b8454610270565b846102a8565b6020601f821160018114610353575f831561033c5750848201515b5f19600385901b1c1916600184901b1784556102ec565b5f84815260208120601f198516915b828110156103825787850151825560209485019460019092019101610362565b508482101561039f57868401515f19600387901b60f8161c191681555b50505050600190811b01905550565b634e487b7160e01b5f52601160045260245ffd5b6001815b60018411156103fd578085048111156103e1576103e16103ae565b60018416156103ef57908102905b60019390931c9280026103c6565b935093915050565b5f82610413575060016104a1565b8161041f57505f6104a1565b8160018114610435576002811461043f5761045b565b60019150506104a1565b60ff841115610450576104506103ae565b50506001821b6104a1565b5060208310610133831016604e8410600b841016171561047e575081810a6104a1565b61048a5f1984846103c2565b805f190482111561049d5761049d6103ae565b0290505b92915050565b5f6104b560ff841683610405565b9392505050565b80820281158282048414176104a1576104a16103ae565b808201808211156104a1576104a16103ae565b610bbc806104f35f395ff3fe608060405234801561000f575f80fd5b50600436106100fb575f3560e01c8063537df3b61161009357806395d89b411161006357806395d89b4114610240578063a9059cbb14610248578063dd62ed3e1461025b578063fe575a8714610293575f80fd5b8063537df3b6146101c757806370480275146101da57806370a08231146101ed5780638da5cb5b14610215575f80fd5b806323b872dd116100ce57806323b872dd1461016757806324d7806c1461017a578063313ce567146101a557806344337ea1146101b4575f80fd5b806306fdde03146100ff578063095ea7b31461011d5780631785f53c1461014057806318160ddd14610155575b5f80fd5b6101076102a6565b6040516101149190610a33565b60405180910390f35b61013061012b366004610a83565b610336565b6040519015158152602001610114565b61015361014e366004610aab565b61034f565b005b6002545b604051908152602001610114565b610130610175366004610ac4565b6103e6565b610130610188366004610aab565b6001600160a01b03165f9081526006602052604090205460ff1690565b60405160028152602001610114565b6101536101c2366004610aab565b610498565b6101536101d5366004610aab565b610530565b6101536101e8366004610aab565b6105c5565b6101596101fb366004610aab565b6001600160a01b03165f9081526020819052604090205490565b600754610228906001600160a01b031681565b6040516001600160a01b039091168152602001610114565b61010761065a565b610130610256366004610a83565b610669565b610159610269366004610afe565b6001600160a01b039182165f90815260016020908152604080832093909416825291909152205490565b6101306102a1366004610aab565b61071a565b6060600380546102b590610b2f565b80601f01602080910402602001604051908101604052809291908181526020018280546102e190610b2f565b801561032c5780601f106103035761010080835404028352916020019161032c565b820191905f5260205f20905b81548152906001019060200180831161030f57829003601f168201915b5050505050905090565b5f33610343818585610737565b60019150505b92915050565b6007546001600160a01b0316331461039e5760405162461bcd60e51b815260206004820152600d60248201526c2737ba103a34329037bbb732b960991b60448201526064015b60405180910390fd5b6001600160a01b0381165f81815260066020526040808220805460ff19169055517fa3b62bc36326052d97ea62d63c3d60308ed4c3ea8ac079dd8499f1e9c4f80c0f9190a250565b5f6103f08461071a565b156104355760405162461bcd60e51b815260206004820152601560248201527414d95b99195c881a5cc8189b1858dadb1a5cdd1959605a1b6044820152606401610395565b61043e8361071a565b156104855760405162461bcd60e51b8152602060048201526017602482015276149958d95a5d995c881a5cc8189b1858dadb1a5cdd1959604a1b6044820152606401610395565b610490848484610749565b949350505050565b335f9081526006602052604090205460ff166104e55760405162461bcd60e51b815260206004820152600c60248201526b2737ba1030b71030b236b4b760a11b6044820152606401610395565b6001600160a01b0381165f81815260056020526040808220805460ff19166001179055517ff9b68063b051b82957fa193585681240904fed808db8b30fc5a2d2202c6ed6279190a250565b335f9081526006602052604090205460ff1661057d5760405162461bcd60e51b815260206004820152600c60248201526b2737ba1030b71030b236b4b760a11b6044820152606401610395565b6001600160a01b0381165f81815260056020526040808220805460ff19169055517f2b6bf71b58b3583add364b3d9060ebf8019650f65f5be35f5464b9cb3e4ba2d49190a250565b6007546001600160a01b0316331461060f5760405162461bcd60e51b815260206004820152600d60248201526c2737ba103a34329037bbb732b960991b6044820152606401610395565b6001600160a01b0381165f81815260066020526040808220805460ff19166001179055517f44d6d25963f097ad14f29f06854a01f575648a1ef82f30e562ccd3889717e3399190a250565b6060600480546102b590610b2f565b5f6106733361071a565b156106b85760405162461bcd60e51b815260206004820152601560248201527414d95b99195c881a5cc8189b1858dadb1a5cdd1959605a1b6044820152606401610395565b6106c18361071a565b156107085760405162461bcd60e51b8152602060048201526017602482015276149958d95a5d995c881a5cc8189b1858dadb1a5cdd1959604a1b6044820152606401610395565b610713338484610749565b9392505050565b6001600160a01b03165f9081526005602052604090205460ff1690565b610744838383600161076c565b505050565b5f3361075685828561083f565b6107618585856108b5565b506001949350505050565b6001600160a01b0384166107955760405163e602df0560e01b81525f6004820152602401610395565b6001600160a01b0383166107be57604051634a1406b160e11b81525f6004820152602401610395565b6001600160a01b038085165f908152600160209081526040808320938716835292905220829055801561083957826001600160a01b0316846001600160a01b03167f8c5be1e5ebec7d5bd14f71427d1e84f3dd0314c0f7b2291e5b200ac8c7c3b9258460405161083091815260200190565b60405180910390a35b50505050565b6001600160a01b038381165f908152600160209081526040808320938616835292905220545f1981101561083957818110156108a757604051637dc7a0d960e11b81526001600160a01b03841660048201526024810182905260448101839052606401610395565b61083984848484035f61076c565b6001600160a01b0383166108de57604051634b637e8f60e11b81525f6004820152602401610395565b6001600160a01b0382166109075760405163ec442f0560e01b81525f6004820152602401610395565b6107448383836001600160a01b038316610937578060025f82825461092c9190610b67565b909155506109a79050565b6001600160a01b0383165f90815260208190526040902054818110156109895760405163391434e360e21b81526001600160a01b03851660048201526024810182905260448101839052606401610395565b6001600160a01b0384165f9081526020819052604090209082900390555b6001600160a01b0382166109c3576002805482900390556109e1565b6001600160a01b0382165f9081526020819052604090208054820190555b816001600160a01b0316836001600160a01b03167fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef83604051610a2691815260200190565b60405180910390a3505050565b602081525f82518060208401528060208501604085015e5f604082850101526040601f19601f83011684010191505092915050565b80356001600160a01b0381168114610a7e575f80fd5b919050565b5f8060408385031215610a94575f80fd5b610a9d83610a68565b946020939093013593505050565b5f60208284031215610abb575f80fd5b61071382610a68565b5f805f60608486031215610ad6575f80fd5b610adf84610a68565b9250610aed60208501610a68565b929592945050506040919091013590565b5f8060408385031215610b0f575f80fd5b610b1883610a68565b9150610b2660208401610a68565b90509250929050565b600181811c90821680610b4357607f821691505b602082108103610b6157634e487b7160e01b5f52602260045260245ffd5b50919050565b8082018082111561034957634e487b7160e01b5f52601160045260245ffdfea264697066735822122049e87549a51e08d9e249181c5e73321ca8c676ef14ff9ee1309193cdd21dd7b864736f6c634300081a0033"
                + String.format("%064x", totalSupply);
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
        genesisBlock.setPreviousBlockHash(new byte[0]);
        genesisBlock.hash();
        genesisState.save(genesisBlock);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("BlockchainState:\n");
        for (Enumeration<String> keys = accounts.keys(); keys.hasMoreElements();) {
            String key = keys.nextElement();
            BlockchainAccount account = accounts.get(key);
            sb.append(account.toString()).append("\n");
        }
        return sb.toString();
    }

}
