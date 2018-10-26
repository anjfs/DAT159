package no.hvl.dat159;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Wallet {

	private String id;
	private KeyPair keyPair;

	// A refererence to the "global" complete utxo-set
	private Map<Input, Output> utxoMap;

	public Wallet(String id, UTXO utxo) {
		this.id = id;
		this.keyPair = DSAUtil.generateRandomDSAKeyPair();
		this.utxoMap = utxo.getUTXOMap();
	}

	public String getAddress() {
		return HashUtil.addressFromPublicKey(keyPair.getPublic());
	}

	public PublicKey getPublicKey() {
		return keyPair.getPublic();
	}

	public Transaction createTransaction(long value, String address) throws Exception {

		// 1. Collect all UTXO for this wallet and calculate balance
		Map<Input, Output> walletOutputs = collectMyUtxo();
		
		long balance = calculateBalance(walletOutputs.values());

		// 2. Check if there are sufficient funds --- Exception?
		if (balance < value)
			throw new Exception("Insufficient funds.");

		// 3. Choose a number of UTXO to be spent --- Strategy?
		Map<Input, Output> UTXOToSpend = new HashMap<>();
		long collected = 0;

		for (Map.Entry<Input, Output> walletEntry : walletOutputs.entrySet()) {
			UTXOToSpend.put(walletEntry.getKey(), walletEntry.getValue());
			collected += walletEntry.getValue().getValue();
			if (collected > value)
				break;
		}

		// 4. Calculate change
		long change = collected - value;

		// 5. Create an "empty" transaction
		Transaction transaction = new Transaction(getPublicKey());

		// 6. Add chosen inputs
		for (Input input : UTXOToSpend.keySet())
			transaction.addInput(input);

		// 7. Add 1 or 2 outputs, depending on change
		Output mainOutput = new Output(value, address);
		transaction.addOutput(mainOutput);

		if (change > 0) {
			Output changeOutput = new Output(change, getAddress());
			transaction.addOutput(changeOutput);
		}

		// 8. Sign the transaction
		transaction.signTxUsing(keyPair.getPrivate());

		// 9. Calculate the hash for the transaction
		transaction.calculateTxHash();

		// 10. return
		return transaction;

		// PS! We have not updated the UTXO yet. That is normally done
		// when appending the block to the blockchain, and not here!
		// Do that manually from the Application-main.
	}

	@Override
	public String toString() {
		return "Id: " + id + ", Address: " + getAddress() + ", Balance: " + getBalance();
	}

	public long getBalance() {
		String walletAddress = getAddress();
		List<Output> walletOutputs = new ArrayList<>();

		for (Output utxoOuput : utxoMap.values()) {
			String outputAddress = utxoOuput.getAddress();
			if (walletAddress.equals(outputAddress))
				walletOutputs.add(utxoOuput);
		}
		return calculateBalance(walletOutputs);
	}

	// TODO Getters?
	public String getId() {
		return id;
	}

	public KeyPair getKeyPair() {
		return keyPair;
	}

	public Map<Input, Output> getUtxoMap() {
		return utxoMap;
	}

	private long calculateBalance(Collection<Output> outputs) {
		long sum = 0;
		for (Output output : outputs)
			sum += output.getValue();
		return sum;
	}

	private Map<Input, Output> collectMyUtxo() {
		Map<Input, Output> outputs = new HashMap<>();

		for (Map.Entry<Input, Output> utxoEntry : utxoMap.entrySet()) {
			String outputAddress = utxoEntry.getValue().getAddress();
			if (getAddress().equals(outputAddress))
				outputs.put(utxoEntry.getKey(), utxoEntry.getValue());
		}
		return outputs;
	}

}
