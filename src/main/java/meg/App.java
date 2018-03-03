package meg;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.ethereum.core.Transaction;
import org.ethereum.crypto.ECKey;
import org.spongycastle.util.BigIntegers;
import org.spongycastle.util.encoders.Hex;

import com.bitsofproof.supernode.wallet.BIP39;

import meg.KeystoreManager.KeystoreContent;
import meg.crypto.AES128;
import meg.crypto.AES256;
import meg.crypto.CryptoException;
import meg.f2a.F2aInfo;
import meg.menu.MenuManager;
import meg.wallet.WalletInfo;
import meg.wallet.WalletType;
import meg.wallet.WalletUtils;

public class App {

	private static final double VERSION = 0.1;

	private static final String FILE_IMG_EXT = "dll";
	private static final String FILE_WALLET_EXT = "ocx";
	public static final String FILE_2FA_EXT = "2fa";

	private static final int MAX_IDIE_TIME_SEC = 120;

	private static boolean holdon = false;

	private static boolean debug;

	private static AES256 aes256Cipher = null;

	public static void main(String[] args) throws IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException {
		initialize(args);
		checkUSB();
		ImageIO.setUseCache(false);
		try {
			start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void initialize(String[] args) {
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

		List<String> larg = Arrays.asList(args).stream().map(a -> a.trim()).filter(a -> a.length() > 0)
				.collect(Collectors.toList());
		if (larg.size() < 1)
			return;
		debug = larg.contains("debug");
		if (debug)
			o("on DEBUG mode");
	}

	private static void start() throws Exception {
		while (holdon) {
			Thread.sleep(50);
		}
		checkTimedOut();
		MenuManager mm = new MenuManager();
		if (!KeystoreManager.isKeystoreFileExists()) {
			mm.add("RESTORE keystore", "restoreKeyStore");
			mm.add("NEW keystore", "generateKeyStore");
		} else {
			mm.add("Sign transaction (currently supports ERC20 only)", "signTransaction", true);
			mm.add("Save wallet private key", "saveWalletPrivateKey", true);
			mm.add("Get wallet private key", "getWalletPrivateKey", true);
			mm.add("Save 2FA key", "save2fa", true);
			mm.add("Get 2FA key", "get2fa", true);
			// TODO mm.add("Save screen shot", "saveScreenShot", true);
			// mm.add("Show screen shot", "showScreenShot", true);
		}
		mm.add("Exit", "exit");
		mm.showOptionList("\n\n==========\n\nChoose an action:");
		int selection = getMenuSelection();
		if (selection < 1) {
			o("Invalid option");
			start();
			return;
		}

		try {
			mm.getOptionBySelection(selection).processMethod();
		} catch (RuntimeException e) {
			Throwable caused = e.getCause();
			if (caused instanceof InvocationTargetException) {
				throw (Exception) caused.getCause();
			} else {
				throw e;
			}
		}

		start();
	}

	@SuppressWarnings("unused")
	private static void loadKeystore() throws Exception {
		if (aes256Cipher != null) {
			return;
		}
		if (!KeystoreManager.isKeystoreFileExists()) {
			o("Keystore file named '%s' does not exists", KeystoreManager.getKeystoreFile().getName());
			System.exit(1);
		}
		o("Loading keystore");
		byte[] keyWithBIP39Encode = KeystoreManager.getEncryptedKey();
		String pwd = InputUtils.getPassword("Passphrase: ");
		if (pwd == null) {
			o("Cancelled");
			return;
		}

		String mnemonic = BIP39.getMnemonic(keyWithBIP39Encode);
		byte[] keyWithAES128 = BIP39.decode(mnemonic, pwd);
		byte[] key;
		try {
			key = AES128.decrypt(keyWithAES128, pwd);
		} catch (CryptoException e) {
			o("Incorrect passphrase");
			System.exit(1);
			return;
		}
		aes256Cipher = new AES256(key, pwd);
	}

	private static void askContinueOrExit(String question) throws Exception {
		if (!InputUtils.confirm(question == null ? "Continue?" : question)) {
			exit();
		}
	}

	private static void exit() throws Exception {
		try {
			if (SystemUtils.IS_OS_WINDOWS) {
				String[] cls = new String[] {"cmd.exe", "/c", "cls"};
				Runtime.getRuntime().exec(cls); 
			} else if (SystemUtils.IS_OS_LINUX) {
				Runtime.getRuntime().exec("clear");
			} else {
				throw new NotImplementedException("Clear screen for " + SystemUtils.OS_NAME);
			}
		} catch (Exception e) {
			if (e instanceof NotImplementedException) {
				throw e;
			}
			if (debug) {
				e.printStackTrace();
			}
		}
		o("Thanks for using our production");
		System.exit(0);
	}

	private static void o(String pattern, Object... params) {
		System.out.println(String.format(pattern, params));
	}

	private static long lastAction = Calendar.getInstance().getTimeInMillis();

	private static void checkTimedOut() {
		long now = Calendar.getInstance().getTimeInMillis();
		if (TimeUnit.SECONDS.convert(now - lastAction, TimeUnit.MILLISECONDS) > MAX_IDIE_TIME_SEC) {
			o("Session timed out (%d seconds)", MAX_IDIE_TIME_SEC);
			System.exit(0);
		} else {
			lastAction = now;
		}
	}

	// Menu
	@SuppressWarnings("unused")
	private static void generateKeyStore() throws Exception {
		USB usb = getUSB();
		File fUsbId = usb.getUsbIdFile();
		if (!debug && fUsbId.exists() && FileUtils.readFileToByteArray(fUsbId).length > 0) {
			o("WARNING! Your USB '%s' were used by another keystore before, restoring keystore may results losting encrypted data FOREVER",
					usb.getAbsolutePath());
			o("In order to generate new keystore you need to perform following actions:");
			o(" 1. Delete '%s' file located in USB", fUsbId.getName());
			o(" 2. Create a new '%s' file in your USB, but leave it empty", fUsbId.getName());
			return;
		}

		String pwd = InputUtils.getPassword_required("Passphrase (up to 16 chars): ", debug ? 1 : 8);

		String cfpwd = InputUtils.getPassword("One more time:");
		if (cfpwd == null) {
			o("Cancelled");
			return;
		}

		if (!pwd.equals(cfpwd)) {
			o("Mismatch confirmation passphrase");
			return;
		}

		// Gen key
		byte[] key = randomBytes(32);
		byte[] keyWithAES128 = AES128.encrypt(key, pwd);

		// Mnemonic
		byte[] keyWithBIP39Encode = BIP39.encode(keyWithAES128, pwd);
		String mnemonic = BIP39.getMnemonic(keyWithBIP39Encode).trim();
		o("Following is %d seeds word,\n you HAVE TO write it down and keep it safe.\n Losing these words, you can not restore your private key",
				mnemonic.split("\\s").length);
		o("\n%s\n", mnemonic);
		ClipboardUtils.setText(mnemonic, "Mnemonic");
		// Verify BIP39
		byte[] keyToVerify = AES128.decrypt(BIP39.decode(mnemonic, pwd), pwd);
		for (int i = 0; i < key.length; i++) {
			if (key[i] != keyToVerify[i]) {
				throw new RuntimeException("Mismatch BIP39, contact author");
			}
		}

		// Verify mnemonic
		o("For sure you already saved these seed words, you have to typing these words again:");
		String cfmnemonic;
		while (true) {
			cfmnemonic = InputUtils.getRawInput(null);
			if (cfmnemonic == null || !mnemonic.equals(cfmnemonic)) {
				o("Mismatch! Again:");
				continue;
			}
			o("Good job! Keep these seed words safe");
			break;
		}

		// Write file
		KeystoreContent keystore = new KeystoreContent();
		keystore.setEncryptedKey(keyWithBIP39Encode);
		KeystoreManager.save(keystore);

		o("Keystore created successfully");

		// Write MEMORIZE
		saveChecksum(mnemonic, key, pwd);
	}

	@SuppressWarnings("unused")
	private static void restoreKeyStore() throws Exception {
		USB usb = getUSB();
		o("Enter seed words:");
		String mnemonic;
		boolean first = true;
		do {
			if (first) {
				first = false;
			} else {
				o("Incorrect, input again or type 'cancel':");
			}
			mnemonic = InputUtils.getRawInput(null);
			if ("cancel".equalsIgnoreCase(mnemonic)) {
				return;
			}
		} while (!isValidSeedWords(mnemonic));

		String pwd = InputUtils.getPassword("Passphrase: ");
		if (pwd == null) {
			o("Cancelled");
			return;
		}

		byte[] keyWithAES128, key, keyWithBIP39Encode, usbIdContentBuffer, usbIdContent;
		int cacheSeedWordsLength = 0;

		try {
			usbIdContentBuffer = FileUtils.readFileToByteArray(usb.getUsbIdFile());
			if (usbIdContentBuffer.length > 1) {
				cacheSeedWordsLength = usbIdContentBuffer[0];
				usbIdContent = new byte[usbIdContentBuffer.length - 1];
				System.arraycopy(usbIdContentBuffer, 1, usbIdContent, 0, usbIdContent.length);
			} else {
				usbIdContent = new byte[0];
			}

			keyWithAES128 = BIP39.decode(mnemonic, pwd);
			key = AES128.decrypt(keyWithAES128, pwd);
			keyWithBIP39Encode = BIP39.encode(keyWithAES128, pwd);

			if (cacheSeedWordsLength > 0) {
				String mnemonicFromUsbID = new String(AES256.decrypt(usbIdContent, key, pwd), StandardCharsets.UTF_8);
				if (!mnemonic.trim().equals(mnemonicFromUsbID.trim())) {
					o("Incorrect! You have to check your seed words and your passphrase then try again!");
					o("Hint: seed contains %d words", cacheSeedWordsLength);
					return;
				}
			}
		} catch (Exception e) {
			o("Look like something wrong, you have to check your seed words and your passphrase then try again!");
			o("Hint: seed contains %d words", cacheSeedWordsLength);
			return;
		}

		KeystoreContent keystore = new KeystoreContent();
		keystore.setEncryptedKey(keyWithBIP39Encode);
		KeystoreManager.save(keystore);
		o("Keystore restored successfully");

		// Write MEMORIZE
		saveChecksum(mnemonic, key, pwd);
	}

	private static void saveChecksum(String mnemonic, byte[] key, String pwd) throws Exception {
		try {
			byte[] content = AES256.encrypt(StringUtil.getBytes(mnemonic, 256), key, pwd);
			byte[] buffer = new byte[content.length + 1];
			System.arraycopy(content, 0, buffer, 1, content.length);
			buffer[0] = (byte) mnemonic.split("\\s").length;
			FileUtils.writeByteArrayToFile(getUSB().getUsbIdFile(), buffer);
		} catch (Exception e) {
		}
	}

	private static boolean isValidSeedWords(String text) {
		if (text == null)
			return false;
		text = text.trim();
		String[] words = text.split("\\s");
		return words.length % 2 == 0;
	}

	@SuppressWarnings("unused")
	private static void saveWalletPrivateKey() throws Exception {
		MenuManager mm = new MenuManager();
		for (WalletType wt : WalletType.values()) {
			mm.add(wt.getDisplayText(), null);
		}
		mm.showOptionList("Select wallet type:");
		int selection = getMenuSelection();
		if (selection < 1 || selection > WalletType.values().length) {
			o("Invalid option");
			start();
			return;
		}

		WalletType wt = WalletType.values()[selection - 1];

		o("Enter your private key (will be encrypted):");
		o("(press Enter to skip)");
		String privateKey = InputUtils.getPassword(null);
		byte[] bprivateKey = StringUtil.getBytesNullable(privateKey);
		byte[] privateKeyWithAES256Encrypted = aes256Cipher.encryptNullable(bprivateKey);

		o("Enter your mnemonic (will be encrypted):");
		o("(press Enter to skip)");
		String mnemonic = StringUtils.trimToNull(InputUtils.getRawInput(null));
		if (mnemonic != null) {
			if (mnemonic.split("\\s").length % 12 != 0) {
				o("Mnemonic incorrect (can not devided by 12)");
				o("Aborted");
				return;
			}
		}
		byte[] bmnemonic = StringUtil.getBytesNullable(mnemonic);
		byte[] mnemonicWithAES256Encrypted = aes256Cipher.encryptNullable(bmnemonic);

		if (privateKey == null && mnemonic == null) {
			o("You must provide at least one information, Private Key or Mnemonic seed words");
			return;
		}

		String address;
		if (wt == WalletType.ERC20 && privateKey != null) {
			address = WalletUtils.getFriendlyEthAddressFromPrivateKey(privateKey);
		} else {
			o("Address (required, will not be encrypted):");
			while (true) {
				address = InputUtils.getInput(null, 68);
				if (InputUtils.confirm("You sure? Please confirm this address again!")) {
					break;
				}
				o("Address:");
			}
		}

		o("Note - this content can NOT be changed later (optional, will be encrypted):");
		o("(press Enter to skip)");
		String note = StringUtils.trimToNull(InputUtils.getRawInput(null));
		byte[] bnote = StringUtil.getBytesNullable(note);
		byte[] noteWithAES256Encrypted = aes256Cipher.encryptNullable(bnote);

		// Clear clip-board
		ClipboardUtils.clear();

		WalletInfo wi = new WalletInfo(wt.getDisplayText(), address, privateKeyWithAES256Encrypted,
				mnemonicWithAES256Encrypted, noteWithAES256Encrypted);
		File file = getUSB().getFileInUsb(String.format("%s.%s.%s", address, wt.name(), FILE_WALLET_EXT));
		try {
			UniqueFileUtils.write(file, wi.toRaw());
		} catch (FileExistsException e) {
			o("** This wallet is already exists in your device, named '%s'", file.getName());
			o(">> Aborted");
			return;
		}

		o("Saved %s", address);

		askContinueOrExit(null);
	}

	@SuppressWarnings("unused")
	private static void getWalletPrivateKey() throws Exception {
		USB usb = getUSB();
		File[] files = usb.getFilesInUsb();
		if (files == null || files.length == 0) {
			o("No wallet existing in device");
			return;
		}
		MenuManager mm = new MenuManager();
		List<File> wallets = new ArrayList<>();
		for (File file : files) {
			if (!file.isFile())
				continue;
			String name = file.getName();
			if (name.toLowerCase().endsWith("." + FILE_WALLET_EXT.toLowerCase())) {
				mm.add(name.split("\\.")[0], null);
				wallets.add(file);
			}
		}
		if (wallets.isEmpty()) {
			o("No wallet existing in device");
			return;
		}
		mm.showOptionList("Select a wallet:");
		int selection = getMenuSelection();
		if (selection < 1 || selection > wallets.size()) {
			o("Invalid selection");
			return;
		}
		File walletFile = wallets.get(selection - 1);
		WalletInfo wi = WalletInfo.fromUncompletedFile(UncompletedFile.fromFile(walletFile));
		o("Address:");
		o("\t%s", wi.getAddress());

		String privateKey = null, mnemonic = null;

		if (wi.containsPrivateKey()) {
			byte[] keyBytes = aes256Cipher.decrypt(wi.getPrivateKeyEncrypted());
			privateKey = new String(keyBytes, StandardCharsets.UTF_8);
			o("Private key:");
			o("\t%s", privateKey);
		}

		if (wi.containsMnemonic()) {
			byte[] mnemonicBytes = aes256Cipher.decrypt(wi.getMnemonicEncrypted());
			mnemonic = new String(mnemonicBytes, StandardCharsets.UTF_8);
			o("Mnemonic:");
			o("\t%s", mnemonic);
		}

		boolean copy = false;
		if (wi.containsMnemonic()) {
			if (copy = InputUtils.confirm("Copy mnemonic to clipboard?")) {
				ClipboardUtils.setText(mnemonic, "Mnemonic");
			}
		} else if (wi.containsPrivateKey()) {
			if (copy = InputUtils.confirm("Copy private key to clipboard?")) {
				ClipboardUtils.setText(privateKey, "Private key");
			}
		}

		if (wi.containsNote()) {
			byte[] noteBytes = aes256Cipher.decrypt(wi.getNoteEncrypted());
			String note = new String(noteBytes, StandardCharsets.UTF_8);
			o("Note:");
			o("\t%s", note);
		}

		if (copy) {
			o("ALERT: your clipboard current contains important data (private key, mnemonic)");
			o("It should be cleared !!!");
			o("Once you've done your job, tell me, I will clear these data from clipboard");
			o("Have you done your job?");
			while (!InputUtils.confirm("I'm done")) {
				o("Just continue your job, I will WAITING for you");
			}
			ClipboardUtils.clear();
		} else {
			o("ALERT: when you copy to clipboard, it contains important data (private key, mnemonic)");
			o("That should be cleared !!!");
			o("Once you've done your job, tell me, I will clear these data from clipboard");
			if (InputUtils.confirm("Clear it?")) {
				ClipboardUtils.clear();
				exit();
			}
			o("No, you have to clear it! I will clear your clip-board without permission! HAHA");
			ClipboardUtils.clear();
		}
	}

	@SuppressWarnings("unused")
	private static void signTransaction() throws Exception {
		MenuManager mm = new MenuManager();
		mm.add("Sign an Ethereum transaction", "signEthereumTransaction");
		mm.showOptionList("Target network:");
		int selection = getMenuSelection();
		if (selection < 1) {
			o("Cancelled");
			return;
		}
		mm.getOptionBySelection(selection).processMethod();
	}

	@SuppressWarnings("unused")
	private static void signEthereumTransaction() throws Exception {
		File walletFile = selectWallet(WalletType.ERC20);
		if (walletFile == null) {
			return;
		}

		WalletInfo wi = WalletInfo.fromUncompletedFile(UncompletedFile.fromFile(walletFile));
		if (!wi.containsPrivateKey()) {
			o("This Wallet does not contains private key !!!");
			o("Only wallets with private key are allowed to sign transaction");
			return;
		}

		String key = new String(aes256Cipher.decrypt(wi.getPrivateKeyEncrypted()), StandardCharsets.UTF_8);
		signEthereumTransaction(key);
	}

	@SuppressWarnings("unused")
	private static void signEthereumTransaction(String privateKey) throws Exception {
		BigInteger nonce, gasPrice, gasPriceGwei, gasLimit, amount;
		String receiveAddress;
		byte[] bNonce, bGasPrice, bGasLimit, bReceiveAddress, bValue, bData;
		Integer chainId;
		
		o("\n");

		String tmp;
		while (true) {
			tmp = InputUtils.getRawInput("Nonce: ");
			if (!NumberUtils.isValidExpandedInt(tmp)) {
				o("Invalid number! Digits only");
				o("Try again:");
				continue;
			}
			break;
		}
		nonce = new BigInteger(tmp, 10);
		bNonce = Hex.decode(NumberUtils.convertBigIntegerToHex(nonce));

		o("\nReceiver address:");
		while (true) {
			receiveAddress = StringUtils.trimToEmpty(InputUtils.getRawInput(null)).toLowerCase();
			if (debug && receiveAddress.length() == 0) {
				receiveAddress = "0x4164B04ad8A00f53E81e781d0d96eF6E4Bcf355f";
				o("Default: %s", receiveAddress);
			}
			if (receiveAddress.startsWith("0x")) {
				receiveAddress = receiveAddress.substring(2);
			}
			if (receiveAddress.length() != 40) {
				o("Invalid ERC20 address!");
				o("Try again:");
				continue;
			}
			bReceiveAddress = Hex.decode(receiveAddress);
			break;
		}

		o("\nGas price (Should be more than 9 Gwei)");
		String gwei;
		while (true) {
			gwei = InputUtils.getRawInput("Gwei: ");
			if (gwei.length() == 0) {
				gwei = "9";
				o("Default: %s", gwei);
			}
			if (!NumberUtils.isValidExpandedInt(gwei)) {
				o("Invalid number! Only digits are accepted");
				continue;
			}
			break;
		}
		gasPrice = new BigInteger(gwei + "000000000" /* Gwei to Wei */, 10);
		gasPriceGwei = new BigInteger(gwei, 10);
		bGasPrice = Hex.decode(NumberUtils.convertBigIntegerToHex(gasPrice));

		o("\n");
		tmp = InputUtils.getRawInput("Gas limit (Should be more than 21000): ");
		while (true) {
			if (tmp.length() == 0) {
				tmp = "21000";
				o("Default: %s", tmp);
			}
			if (!NumberUtils.isValidExpandedInt(tmp)) {
				o("Invalid number! Digits only");
				tmp = InputUtils.getRawInput("Gas limit: ");
				continue;
			}
			break;
		}
		gasLimit = new BigInteger(tmp, 10);
		bGasLimit = Hex.decode(NumberUtils.convertBigIntegerToHex(gasLimit));

		o("\n");
		while (true) {
			tmp = InputUtils.getRawInput("Amount ETH to send: ");
			if (!NumberUtils.isValidExpandedDouble(tmp)) {
				o("Invalid double value! Accepted format is # or #.##");
				continue;
			}
			break;
		}
		amount = new BigInteger(NumberUtils.toBigValue(tmp, 18), 10);
		bValue = Hex.decode(NumberUtils.convertBigIntegerToHex(amount));

		bData = null; // fixed to null for ETH

		chainId = new Integer(1); // ETH chain fixed 1

		o("\nPlease CAREFULLY check the following informations:");
		o("Nonce: %d", nonce);
		o("Gas price:");
		o("\t%s Wei", StringUtil.beautiNumber(gasPrice.toString(10)));
		o("\t~ %s Gwei", StringUtil.beautiNumber(gwei));
		String sGasLimit = StringUtil.beautiNumber(gasLimit.toString(10));
		o("Gas limit: %s", sGasLimit);
		o("Receive address: 0x%s", receiveAddress);
		o("Amount to transfer:");
		o("\t%s (raw)", StringUtil.beautiNumber(amount.toString(10)));
		String sETHAmt = StringUtil.beautiNumber(NumberUtils.fromBigValue(amount.toString(10), 18));
		o("\t%s ETH", sETHAmt);
		o("Data: (no data)");

		if (!InputUtils.confirm("Please CAREFULLY confirm the above informations")) {
			o("Abort!!!");
			return;
		}

		o("-- please wait --");
		ECKey ecSender = WalletUtils.getECKey(privateKey);
		Transaction tx = new Transaction(bNonce, bGasPrice, bGasLimit, bReceiveAddress, bValue, bData, chainId);
		tx.sign(ecSender);

		o("v:\t %s", Hex.toHexString(new byte[] { tx.getSignature().v }));
		o("r:\t %s", Hex.toHexString(BigIntegers.asUnsignedByteArray(tx.getSignature().r)));
		o("s:\t %s", Hex.toHexString(BigIntegers.asUnsignedByteArray(tx.getSignature().s)));

		ECKey key = ECKey.signatureToKey(tx.getHash(), tx.getSignature().toBase64());

		o("Un-signed transaction:\t %s", Hex.toHexString(tx.getEncodedRaw()));
		String signedTransaction = Hex.toHexString(tx.getEncoded());
		o("*** Signed transaction:\t %s", signedTransaction);

		o("Signature public key:\t %s", Hex.toHexString(key.getPubKey()));
		o("Sender is:\t %s", Hex.toHexString(key.getAddress()));
		o("********\n");
		o("You are about to\n\tsend %s ETH", sETHAmt);
		o("from\n\t%s", WalletUtils.getFriendlyEthAddressFromPrivateKey(privateKey));
		o("to\n\t0x%s", receiveAddress);
		o("with maximum fee is %s Gwei * %s Gas limit\n\tMAX %s ETH", StringUtil.beautiNumber(gwei), sGasLimit,
				StringUtil.beautiNumber(NumberUtils.fromBigValue(gasPrice.multiply(gasLimit).toString(10), 18)));
		o(tx.toString());
		ClipboardUtils.setText(signedTransaction, "Tx signature");

		askContinueOrExit(null);
	}

	@SuppressWarnings("unused")
	private static void save2fa() throws Exception {
		ClipboardUtils.clear();
		o("NOTICE: Please DO NOT copy and paste 2fa private key, just type it manually !!!");
		o("2FA private key (will be encrypted):");
		String _2fa = InputUtils.getInput2faPrivateKey();
		if (StringUtils.isBlank(_2fa)) {
			o("Cancelled");
			return;
		}

		ClipboardUtils.clear();
		o("Type it again:");
		String _cf2fa = InputUtils.getInput2faPrivateKey();
		if (!_2fa.equals(_cf2fa)) {
			o("Mismatch, action aborted!");
			return;
		}

		o("Account name (required, will not be encrypted so should not contains private information):");
		String account = InputUtils.getInput("account", false, "^[^\\s]+$",
				"Could not be empty or contains any Blank space", null);
		String csAccount = StringUtil.getSimpleCheckSum(account);

		o("Website or production name (required, will not be encrypted so should not contains private information):");
		String productionName = InputUtils.getInput("website or production name", false, null, null, null);
		String csProductionName = StringUtil.getSimpleCheckSum(productionName);

		o("Remarks (optional, will be encrypted):");
		String remark = InputUtils.getRawInput(null);

		byte[] encrypted2fa = aes256Cipher.encrypt(_2fa.getBytes(StandardCharsets.UTF_8));
		byte[] encryptedRemarks = aes256Cipher
				.encryptNullable(StringUtils.isBlank(remark) ? null : remark.getBytes(StandardCharsets.UTF_8));

		// Clear clip-board
		ClipboardUtils.clear();

		F2aInfo f2aInfo = new F2aInfo(encrypted2fa, account, productionName, encryptedRemarks);
		File file = getUSB().getFileInUsb(f2aInfo.getFileName());
		try {
			UniqueFileUtils.write(file, f2aInfo.toRaw());
		} catch (FileExistsException e) {
			o("** This account/website or production is already exists in your device, named '%s'", file.getName());
			o(">> if you want to override, the old data will be lost forever ! Please becareful");
			if (!InputUtils.confirm("Do you understand?")) {
				o(">> Aborted");
				return;
			}
			o("If you want to override, type 'I AGREE':");
			if (!"i agree".equalsIgnoreCase(InputUtils.getRawInput(null))) {
				o(">> You did not agree! Action aborted");
				return;
			}
			o("Now type 'OVERRIDE'");
			String input = InputUtils.getRawInput(null);
			if (input.equalsIgnoreCase("override") || input.equalsIgnoreCase("overide")) {
				FileUtils.deleteQuietly(file);
				UniqueFileUtils.write(file, f2aInfo.toRaw());
			} else {
				o(">> You did not type 'OVERRIDE' ! Action aborted");
				return;
			}
		}

		o("Saved 2fa private key for account '%s' of '%s'", account, productionName);
		askContinueOrExit(null);
	}

	@SuppressWarnings("unused")
	private static void get2fa() throws Exception {
		List<File> files = Arrays.asList(getUSB().getFilesInUsb()).stream()
				.filter(f -> f.getName().endsWith("." + FILE_2FA_EXT)).collect(Collectors.toList());
		if (files.isEmpty()) {
			o("Empty !!!");
			return;
		}
		final List<F2aInfo> f2aFiles = new ArrayList<>();
		files.stream().forEach(f -> {
			try {
				f2aFiles.add(F2aInfo.fromUncompletedFile(UncompletedFile.fromFile(f)));
			} catch (Exception e) {
				o("%s error: %s", f.getName(), e.getMessage());
			}
		});

		o("Select an account:");
		for (int i = 0; i < f2aFiles.size(); i++) {
			F2aInfo f2aInfo = f2aFiles.get(i);
			o(" %d. account '%s' of '%s'", i + 1, f2aInfo.getAccount(), f2aInfo.getProductionName());
		}

		o("Take one or 0 to cancel:");
		int selection;

		while (true) {
			selection = getMenuSelection();
			if (selection < 1) {
				o("Cancelled");
				return;
			}
			if (selection > f2aFiles.size()) {
				o("Invalid selection! Choose again:");
				continue;
			}
			break;
		}

		F2aInfo target = f2aFiles.get(selection - 1);

		String _2faPrivateKey = new String(aes256Cipher.decrypt(target.getEncrypted2faPrivateKey()),
				StandardCharsets.UTF_8);
		o("2FA private key: %s", _2faPrivateKey);

		if (target.containsEncryptedRemark()) {
			String remark = new String(aes256Cipher.decrypt(target.getEncryptedRemark()), StandardCharsets.UTF_8);
			o("Remark:\n%s", remark);
		}

		ClipboardUtils.setText(_2faPrivateKey, "2fa private key");

		askContinueOrExit(null);
	}

	private static int getMenuSelection() {
		String input = InputUtils.getInput("Action: ", 1);
		if (input == null) {
			return 0;
		}
		try {
			return Integer.parseInt(input);
		} catch (Exception e) {
			return 0;
		}
	}

	// Utils
	private static byte[] randomBytes(int size) throws NoSuchAlgorithmException {
		byte[] bytes = new byte[size];
		SecureRandom.getInstanceStrong().nextBytes(bytes);
		return bytes;
	}

	// OS and USB
	private static void checkUSB() {
		USB usb = getUSB();
		if (!usb.isValid()) {
			o("USB not found !!!");
			o("You may need a USB plugged in with an empty file named '%s' inside it", USB.USB_ID_FILE_NAME);
			System.exit(1);
		}
	}

	private static USB _usb = new USB(null);

	private static USB getUSB() {
		if (!SystemUtils.IS_OS_WINDOWS) {
			throw new RuntimeException(
					"Method of detecting USB device in OS " + System.getProperty("os.name") + " was not implemented");
		}
		if (debug) {
			_usb = new USB(new File("C:\\USB"));
		}
		if (_usb.isValid()) {
			return _usb;
		}
		File[] roots = File.listRoots();
		Device: for (File root : roots) {
			USB usb = new USB(root);
			if (!usb.isValid()) {
				continue Device;
			}
			try {
				File[] listOfFiles = root.listFiles();
				FileOnDevice: for (File file : listOfFiles) {
					if (file.isDirectory()) {
						o("Usb %s should not contains any directory. Skip this device", root.getAbsolutePath());
						continue Device;
					} else { // File
						if (file.getName().equalsIgnoreCase(usb.getUsbIdFile().getName())) {
							continue FileOnDevice;
						} else if (file.getName().toLowerCase().endsWith(FILE_IMG_EXT.toLowerCase())) {
							continue FileOnDevice;
						} else if (file.getName().toLowerCase().endsWith(FILE_WALLET_EXT.toLowerCase())) {
							continue FileOnDevice;
						} else if (file.getName().toLowerCase().endsWith(FILE_2FA_EXT.toLowerCase())) {
							continue FileOnDevice;
						} else {
							o("Usb %s should not contains any file except *.%s and *.%s files. Skip this device",
									root.getAbsolutePath(), FILE_IMG_EXT, FILE_WALLET_EXT);
							continue Device;
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				continue Device;
			}
			return _usb = new USB(root);
		}
		return _usb = new USB(null);
	}

	private static File selectWallet(WalletType wt) {
		USB usb = getUSB();
		List<File> walletFiles = Arrays.asList(usb.getFilesInUsb()).stream()//
				.filter(f -> f.getName().endsWith("." + FILE_WALLET_EXT)).collect(Collectors.toList());
		if (walletFiles.isEmpty()) {
			o("No wallet exists in device");
			return null;
		}

		walletFiles = walletFiles.stream()
				.filter(f -> f.getName().toLowerCase().contains("." + wt.name().toLowerCase() + "."))
				.collect(Collectors.toList());
		if (walletFiles.isEmpty()) {
			o("No wallet of type %s exists in device", wt.getDisplayText());
			return null;
		}

		o("Existing wallets:");
		for (int select = 1; select <= walletFiles.size(); select++) {
			String name = walletFiles.get(select - 1).getName();
			String[] spl = name.split("\\.");
			String address = spl[0];
			String type = spl.length < 3 ? "other" : spl[1];
			o(" %d. %s (%s)", select, address, type);
		}
		o("Take one or 0 to cancel:");
		int selection;

		while (true) {
			selection = getMenuSelection();
			if (selection < 1) {
				o("Cancelled");
				return null;
			}
			if (selection > walletFiles.size()) {
				o("Invalid selection! Choose again:");
				continue;
			}
			break;
		}
		return walletFiles.get(selection - 1);
	}

	private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss dd/MM/yy");

	public static String getAdditionalDetailInformation() {
		return String.format("JVM %s, version %f, at %s", Runtime.class.getPackage().getImplementationVersion(),
				VERSION, sdf.format(new Date()));
	}
}
