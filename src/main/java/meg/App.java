package meg;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
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
import meg.menu.MenuManager;
import meg.wallet.WalletInfo;
import meg.wallet.WalletType;
import meg.wallet.WalletUtils;

public class App {

	private static final String FILE_IMG_EXT = "dll";
	private static final String FILE_WALLET_EXT = "ocx";

	private static final int MAX_IDIE_TIME_SEC = 60;

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
			mm.add("Save wallet private key", "saveWalletPrivateKey", true);
			mm.add("Get wallet private key", "getWalletPrivateKey", true);
			mm.add("Sign transaction", "signTransaction", true);
			mm.add("Save screen shot", "saveScreenShot", true);
			mm.add("Show screen shot", "showScreenShot", true);
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
		String pwd = InputUtils.getPassword("Pass pharse:");
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
			o("Incorrect pass pharse");
			System.exit(1);
			return;
		}
		aes256Cipher = new AES256(key, pwd);
	}

	@SuppressWarnings("unused")
	private static void exit() {
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

		String pwd = InputUtils.getPassword("Pass pharse (up to 16 chars):");
		if (pwd == null) {
			o("Cancelled");
			return;
		}

		String cfpwd = InputUtils.getPassword("One more time:");
		if (cfpwd == null) {
			o("Cancelled");
			return;
		}

		if (!pwd.equals(cfpwd)) {
			o("Mismatch confirmation pass pharse");
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
		ClipboardUtils.setClipboard(mnemonic, "Mnemonic");
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
			cfmnemonic = InputUtils.getRawInput();
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
				o("Incorrect, input again or type 'cancel'");
			}
			mnemonic = InputUtils.getRawInput();
			if ("cancel".equalsIgnoreCase(mnemonic)) {
				return;
			}
		} while (!isValidSeedWords(mnemonic));

		String pwd = InputUtils.getPassword("Pass pharse (up to 16 chars):");
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
					o("Incorrect! You have to check your seed words and your pass pharse then try again!");
					o("Hint: seed contains %d words", cacheSeedWordsLength);
					return;
				}
			}
		} catch (Exception e) {
			o("Look like something wrong, you have to check your seed words and your pass pharse then try again!");
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
			byte[] content = AES256.encrypt(meg.StringUtils.getBytes(mnemonic, 256), key, pwd);
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

		o("Enter your private key:");
		String privateKey = InputUtils.getInput(64);
		if (privateKey == null) {
			o("Cancelled");
			return;
		}
		byte[] bprivateKey = privateKey.getBytes(StandardCharsets.UTF_8);
		byte[] privateKeyWithAES256Encrypted = aes256Cipher.encrypt(bprivateKey);
		
		o("Enter your mnemotic:");
		o("(press Enter to skip)");
		String mnemotic = org.apache.commons.lang3.StringUtils.trimToNull(InputUtils.getRawInput());
		if (mnemotic != null) {
			if (mnemotic.split("\\s").length % 12 != 0) {
				o("Mnemotic incorrect (can not devided by 12)");
				o("Aborted");
				return;
			}
		}
		byte[] bmnemotic = mnemotic == null ? null : mnemotic.getBytes(StandardCharsets.UTF_8);
		byte[] mnemoticWithAES256Encrypted = bmnemotic == null ? null : aes256Cipher.encrypt(bmnemotic);

		String address;
		if (wt == WalletType.ERC20) {
			address = WalletUtils.getFriendlyEthAddressFromPrivateKey(privateKey);
		} else {
			o("Address:");
			while (true) {
				address = InputUtils.getInput(68);
				if (InputUtils.confirm("You sure? Please confirm this address again!")) {
					break;
				}
				o("Address:");
			}
		}
		
		o("Note - this content can NOT be changed later:");
		o("(press Enter to skip)");
		String note = org.apache.commons.lang3.StringUtils.trimToNull(InputUtils.getRawInput());
		byte[] bnote = note == null ? null : note.getBytes(StandardCharsets.UTF_8);
		byte[] noteWithAES256Encrypted = bnote == null ? null : aes256Cipher.encrypt(bnote);

		WalletInfo wi = new WalletInfo(wt.getDisplayText(), address, privateKeyWithAES256Encrypted, mnemoticWithAES256Encrypted, noteWithAES256Encrypted);
		File file = getUSB().getFileInUsb(String.format("%s.%s.%s", address, wt.name(), FILE_WALLET_EXT));
		try {
			UniqueFileUtils.write(file, wi.toString());
		} catch (FileExistsException e) {
			o("** This wallet is already exists in your device, named '%s'", file.getName());
			o(">> Aborted");
			return;
		}

		o("Saved %s", address);
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
		File wallet = wallets.get(selection - 1);
		WalletInfo wi = WalletUtils.readWalletInfo(wallet);
		byte[] keyBytes = aes256Cipher.decrypt(wi.getPrivateKeyEncrypted());
		String privateKey = new String(keyBytes, StandardCharsets.UTF_8);
		o("Address:");
		o("\t%s", wi.getAddress());
		o("Private key:");
		o("\t%s", privateKey);
		
		if (wi.getMnemoticEncrypted() != null) {
			byte[] mnemoticBytes = aes256Cipher.decrypt(wi.getMnemoticEncrypted());
			String mnemotic = new String(mnemoticBytes, StandardCharsets.UTF_8);
			o("Mnemotic:");
			o("\t%s", mnemotic);
			ClipboardUtils.setClipboard(mnemotic, "Mnemotic");
		} else {
			ClipboardUtils.setClipboard(privateKey, "Private key");
		}

		if (wi.getNoteEncrypted() != null) {
			byte[] noteBytes = aes256Cipher.decrypt(wi.getNoteEncrypted());
			String note = new String(noteBytes, StandardCharsets.UTF_8);
			o("Note:");
			o("\t%s", note);
		}
	}

	@SuppressWarnings("unused")
	private static void signTransaction() throws Exception {
		byte[] nonce, gasPrice, gasLimit, receiveAddress, value, data;
		nonce = Hex.decode("");
		gasPrice = Hex.decode("");
		gasLimit = Hex.decode("");
		receiveAddress = Hex.decode("");
		value = Hex.decode("");
		data = null;
		Integer chainId = new Integer(1);

		Transaction tx = new Transaction(nonce, gasPrice, gasLimit, receiveAddress, value, data, chainId);
		String privateKey = "";
		tx.sign(WalletUtils.getECKey(privateKey));

		System.out.println("v\t\t\t: " + Hex.toHexString(new byte[] { tx.getSignature().v }));
		System.out.println("r\t\t\t: " + Hex.toHexString(BigIntegers.asUnsignedByteArray(tx.getSignature().r)));
		System.out.println("s\t\t\t: " + Hex.toHexString(BigIntegers.asUnsignedByteArray(tx.getSignature().s)));

		ECKey key = ECKey.signatureToKey(tx.getHash(), tx.getSignature().toBase64());

		System.out.println("Tx unsigned RLP\t\t: " + Hex.toHexString(tx.getEncodedRaw()));
		System.out.println("**Tx signed RLP\t\t: " + Hex.toHexString(tx.getEncoded()));

		System.out.println("Signature public key\t: " + Hex.toHexString(key.getPubKey()));
		System.out.println("Sender is\t\t: " + Hex.toHexString(key.getAddress()));

		System.out.println(tx.toString());
	}

	private static int getMenuSelection() {
		o("Action:");
		String input = InputUtils.getInput(1);
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
}
