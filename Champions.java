import bt.*;
import bt.compiler.CompilerVersion;
import bt.compiler.TargetCompilerVersion;
import bt.ui.EmulatorWindow;

/**
 * A champion versus challenger game.
 * 
 * This contract has a weight, this weight (in Burst) is constant.
 * This contract also has a current champion, and challengers can defy the
 * champion by sending a challenging amount in Burst.
 * 
 * Based on block hash and transaction id a random number will be generated between
 * 0 and weight+challenge. Any number between 0-weight will make the current
 * champion the winner and any number higher than that will make the challenger
 * winner.
 * 
 * If the current champion wins, he takes the challenger amount as prize. If the
 * challenger is the winner, he gets back his challenge and becomes the new
 * champion.
 * 
 * There is a 1% fee over the challenges and the activation fee is 30 Burst.
 * 
 * @author jjos
 */
@TargetCompilerVersion(CompilerVersion.v0_0_0)
public class Champions extends Contract {

	long weight;
	Address champion;
	long totalWeight;
	long random;
	long challenge;
	long fee;
	long rest;

	public static final long ACTIVATION_FEE = 30 * ONE_BURST;

	public void txReceived() {
		challenge = getCurrentTxAmount() + ACTIVATION_FEE;
		totalWeight = weight + challenge;
		fee = challenge / 100;

		// We could add a sleep here to be extra cautious but this delay
		// would make the game less dynamic, so leave it.
		// sleepOneBlock();
		//random = performSHA256_64(getPrevBlockHash1(), getCurrentTxTimestamp().getValue());
		random = getPrevBlockHash1();
		random &= 0x0FFFFFFFFFFFFFFFL; // avoid negative values
		random %= totalWeight;

		if (random > weight) {
			// challenger won
			sendMessage("A challenger took your title.", champion);
			// we have a new champion
			champion = getCurrentTxSender();
			sendMessage("You are the new champion!", champion);
		} else {
			// challenger lost
			sendMessage("Champion resisted, try again.", getCurrentTxSender());
		}
		// pay the winner (current champion)
		sendAmount(getCurrentTxAmount() - fee, champion);
	}

	@Override
	protected void blockFinished() {
		// if there is some balance, round up with the creator
		rest = getCurrentBalance() - 5*ONE_BURST;
		if(rest > 0)
			sendAmount(rest, getCreator());
	}

	/**
	 * Main function, for debbuging purposes only, not exported to bytecode.
	 */
	public static void main(String[] args) throws Exception {
		new EmulatorWindow(Champions.class);
	}
}
