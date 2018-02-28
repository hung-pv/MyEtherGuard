package meg;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.Security;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.io.FileUtils;

import meg.crypto.RSA;
import meg.crypto.RSAKeyPair;

public class MyEtherGuard {

	private static final int MAX_SIZE = 80;
	private static final String FILE_IMG_EXT = "dll";
	private static final String FILE_WALLET_EXT = "ocx";

	private static final int MAX_IDIE_TIME_SEC = 60;

	private static final File FILE_RSA_KVP = new File("KEEP_THIS_SAFE.sys");
	private static final String USB_ID_FILE_NAME = "usb.4uth";
	private static boolean holdon = false;
	
	private static boolean debug;

	public static void main(String[] args) throws Exception {
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
		
		List<String> larg = Arrays.asList(args).stream().map(a -> a.trim()).filter(a -> a.length() > 0).collect(Collectors.toList());
		if (larg.size() < 1) return;
		debug = larg.contains("debug");
		if (debug) o("on DEBUG mode");
	}

	private static void start() throws Exception {
		while (holdon) {
			Thread.sleep(50);
		}
		loadKeyStore();
		checkTimedOut();
		StringBuilder sb = new StringBuilder();
		sb.append("\n\n ===== Choose an action:");
		if (!FILE_RSA_KVP.exists()) {
			sb.append("\n 0. Generate keystore file");
		} else {
			sb.append("\n 1. View Image");
			sb.append("\n 2. Make Image");
			sb.append("\n 3. View Wallet");
			sb.append("\n 4. Make Wallet");
		}
		o(sb.toString());
		String input = IOUtils.getInput(1);
		if (input == null) {
			e("Invalid option");
			start();
			return;
		}

		byte option = Byte.parseByte(input);
		if (option < 0 || option > 4) {
			e("Invalid option");
			start();
			return;
		}
		checkTimedOut();
		if (option == 0) {
			op0();
		} else if (option == 1) {
			op1();
		} else if (option == 2) {
			op2();
		} else if (option == 3) {
			op3();
		} else if (option == 4) {
			op4();
		}
		start();
	}

	private static void op0() throws Exception {
		if (FILE_RSA_KVP.exists()) {
			e("'%s' already exists!", FILE_RSA_KVP.getName());
			return;
		}

		e("Input password:");
		String pwd = getPassword();
		e("Confirm your password:");
		String cpwd = getPassword();
		if (!cpwd.equals(pwd)) {
			e("Incorrect confirmation password");
			return;
		}

		RSAKeyPair kvp = RSA.createKeyPair();
		String aesProtectedPublicKey = Base64.getEncoder().encodeToString(AES.encrypt(kvp.getPublicKeyRaw(), pwd));
		String aesProtectedPrivateKey = Base64.getEncoder().encodeToString(AES.encrypt(kvp.getPrivateKeyRaw(), pwd));
		FileUtils.writeLines(FILE_RSA_KVP, "UTF-8", Arrays.asList(aesProtectedPublicKey, aesProtectedPrivateKey));
		e("'%s' file was generated, please keep this file in a safe place", FILE_RSA_KVP.getName());
	}

	private static String getPassword() {
		String input = null;
		do {
			input = IOUtils.getRawInput();
			if (input == null || input.length() == 0) {
				e("ERR: Could not be empty");
				input = null;
				continue;
			}
			if (input.length() > 16) {
				e("Your password is longer than 16 characters, will be cut off to the first 16 chars only");
			}
		} while (input == null);
		return input;
	}

	private static void op1() throws Exception {
		checkUSB();
		File target = IOUtils.open(getUSB(), "Encrypted Image file", FILE_IMG_EXT);
		if (target == null) {
			return;
		}
		display(SomeUtils.convertToImage(decrypt(target)));
	}

	private static void op2() throws Exception {
		Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
		BufferedImage capture = new Robot().createScreenCapture(screenRect);
		display(capture);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(capture, "png", baos);
		baos.flush();
		byte[] imageBytes = baos.toByteArray();
		checkUSB();
		File target = IOUtils.save(imageBytes, getUSB(), "Encrypted Image file", FILE_IMG_EXT);
		if (target != null && confirm("Verify ?")) {
			display(SomeUtils.convertToImage(decrypt(target)));
		}
	}

	private static void op3() throws Exception {
		checkUSB();
		File target = IOUtils.open(getUSB(), "Encrypted Wallet file", FILE_WALLET_EXT);
		if (target == null) {
			return;
		}
		byte[] decrypt = RSA.decryptRSA(FileUtils.readFileToByteArray(target));
		IOUtils.displayTextBox(new String(decrypt, StandardCharsets.UTF_8), "(readonly) " + target.getName());
	}

	private static void op4() throws Exception {
		holdon = true;
		JFrame fr;
		JSplitPane panelMain;
		JScrollPane leftPanel;
		JPanel rightPanel;
		JTextArea txtData;
		JButton btnSave;

		fr = new JFrame();
		fr.setLayout(new GridLayout(1, 1));

		txtData = new JTextArea(80, 20);
		txtData.setVisible(true);

		btnSave = new JButton("Save");
		btnSave.setVisible(true);
		btnSave.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					saveContent(txtData.getText());
				} catch (Exception e1) {
					holdon = false;
				}
				fr.dispose();
			}
		});

		leftPanel = new JScrollPane(txtData);
		leftPanel.setVisible(true);
		leftPanel.setMinimumSize(new Dimension(500, 400));

		rightPanel = new JPanel(new GridLayout(1, 1));
		rightPanel.add(btnSave);
		rightPanel.setVisible(true);

		panelMain = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
		panelMain.setVisible(true);

		fr.setTitle("Input data");
		fr.setLocationRelativeTo(null);
		fr.add(panelMain);
		fr.setSize(600, 400);
		fr.setVisible(true);
	}

	private static void saveContent(String data) throws Exception {
		try {
			if (data == null || data.length() == 0) {
				e("Cancelled");
				return;
			}
			byte[] raw = data.getBytes(StandardCharsets.UTF_8);

			checkUSB();
			File target = IOUtils.save(raw, getUSB(), "Encrypted Wallet file", FILE_WALLET_EXT);
			if (target != null && confirm("Verify ?")) {
				byte[] decrypt = RSA.decryptRSA(FileUtils.readFileToByteArray(target));
				IOUtils.displayTextBox(new String(decrypt, StandardCharsets.UTF_8), "(readonly) " + target.getName());
			}

		} finally {
			holdon = false;
		}
	}

	private static File getUSB() {
		if (debug) {
			File usb = new File("C:\\USB");
			return usb;
		}
		String os = System.getProperty("os.name").toLowerCase();
		if (os.indexOf("win") < 0) {
			throw new RuntimeException("Method of detecting USB device in OS " + os + " was not implemented");
		}
		File[] roots = File.listRoots();
		Device: for (File root : roots) {
			if (!Paths.get(root.getAbsolutePath(), USB_ID_FILE_NAME).toFile().exists()) {
				continue Device;
			}
			try {
				File[] listOfFiles = root.listFiles();
				FileOnDevice: for (File file : listOfFiles) {
					if (file.isDirectory()) {
						e("Usb %s should not contains any directory. Skip this device", root.getAbsolutePath());
						continue Device;
					} else { // File
						if (file.getName().equalsIgnoreCase(USB_ID_FILE_NAME)) {
							continue FileOnDevice;
						} else if (file.getName().toLowerCase().endsWith(FILE_IMG_EXT.toLowerCase())) {
							continue FileOnDevice;
						} else if (file.getName().toLowerCase().endsWith(FILE_WALLET_EXT.toLowerCase())) {
							continue FileOnDevice;
						} else {
							e("Usb %s should not contains any file except *.%s and *.%s files. Skip this device",
									root.getAbsolutePath(), FILE_IMG_EXT, FILE_WALLET_EXT);
							continue Device;
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				continue Device;
			}
			return root;
		}
		return null;
	}

	private static void checkUSB() {
		File usb = getUSB();
		if (usb == null || !usb.exists() || !usb.isDirectory()) {
			e("USB not found");
			System.exit(1);
		}
		File auth = Paths.get(usb.getAbsolutePath(), USB_ID_FILE_NAME).toFile();
		if (!auth.exists()) {
			e("'%s' file does not exists in USB", USB_ID_FILE_NAME);
			System.exit(1);
		}
	}

	private static byte[] decrypt(File file) throws Exception {
		byte[] content = FileUtils.readFileToByteArray(file);
		return RSA.decryptRSA(content);
	}

	private static void loadKeyStore() throws Exception {
		if (!FILE_RSA_KVP.exists()) {
			e("'%s' does not exists", FILE_RSA_KVP.getName());
			return;
		}
		if (RSA.isKeysSet()) {
			return;
		}

		List<String> lines = FileUtils.readLines(FILE_RSA_KVP, StandardCharsets.UTF_8);
		if (lines.size() != 2) {
			throw new Exception("'" + FILE_RSA_KVP.getName() + "' is damaged or incorrect");
		}

		try {
			o("Decryption password:");
			String pwd = getPassword();

			String aesProtectedPublicKey = lines.get(0);
			String aesProtectedPrivateKey = lines.get(1);

			byte[] rsaPublicKey = AES.decrypt(Base64.getDecoder().decode(aesProtectedPublicKey), pwd);
			byte[] rsaPrivateKey = AES.decrypt(Base64.getDecoder().decode(aesProtectedPrivateKey), pwd);

			RSA.setKey(RSA.convertPublicKey(rsaPublicKey), RSA.convertPrivateKey(rsaPrivateKey));
		} catch (BadPaddingException e) {
			e("Incorrect password");
			loadKeyStore();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private static void display(BufferedImage img) {
		Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
		double maxW = screenRect.getWidth() / 100 * MAX_SIZE;
		double maxH = screenRect.getHeight() / 100 * MAX_SIZE;
		if (img.getWidth() > maxW || img.getHeight() > maxH) {
			double scale = Math.max(img.getWidth() / maxW, img.getHeight() / maxH);
			img = resize(img, img.getWidth() / scale, img.getHeight() / scale);
		}
		ImageIcon icon = new ImageIcon(img);
		JLabel label = new JLabel(icon);
		JOptionPane.showMessageDialog(null, label);
	}

	private static BufferedImage resize(BufferedImage img, double dnewW, double dnewH) {
		int newW = (int) dnewW;
		int newH = (int) dnewH;
		Image tmp = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
		BufferedImage dimg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);

		Graphics2D g2d = dimg.createGraphics();
		g2d.drawImage(tmp, 0, 0, null);
		g2d.dispose();

		return dimg;
	}

	private static boolean confirm(String msg) {
		o(msg);
		o("Y/N");
		return "y".equalsIgnoreCase(IOUtils.getInput(1));
	}

	private static long lastAction = Calendar.getInstance().getTimeInMillis();

	private static void checkTimedOut() {
		long now = Calendar.getInstance().getTimeInMillis();
		if (TimeUnit.SECONDS.convert(now - lastAction, TimeUnit.MILLISECONDS) > MAX_IDIE_TIME_SEC) {
			e("Session timed out (%d seconds)", MAX_IDIE_TIME_SEC);
			System.exit(0);
		} else {
			lastAction = now;
		}
	}

	private static void o(String pattern, Object... params) {
		System.out.println(String.format(pattern, params));
	}

	private static void e(String pattern, Object... params) {
		System.err.println(String.format(pattern, params));
	}

	//TODO utilities from here
	static class AES {
		private static final String AES_SPEC = "AES/CBC/PKCS5Padding";
		private static final String AES = "AES";

		public static byte[] encrypt(byte[] data, String salt) throws Exception {
			byte[] key = getKey(salt);
			SecretKeySpec secretKey = new SecretKeySpec(key, AES);
			Cipher cipher = Cipher.getInstance(AES_SPEC);
			cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(key));
			return cipher.doFinal(data);
		}

		public static byte[] decrypt(byte[] cipherText, String salt) throws Exception {
			byte[] key = getKey(salt);
			SecretKeySpec secretKey = new SecretKeySpec(key, AES);
			Cipher cipher = Cipher.getInstance(AES_SPEC);
			cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(key));
			return cipher.doFinal(cipherText);
		}

		private static byte[] getKey(String text) throws Exception {
			if (text == null || text.length() == 0) {
				throw new Exception("Password must not empty");
			}
			byte[] barr = text.getBytes(StandardCharsets.UTF_8);
			byte[] key = new byte[16];
			if (barr.length > 16) {
				System.arraycopy(barr, 0, key, 0, 16);
			} else if (barr.length < 16) {
				System.arraycopy(barr, 0, key, 0, barr.length);
			} else {
				key = barr;
			}
			return key;
		}
	}

	static class SomeUtils {
		public static void print(byte[] arr) {
			for (byte b : arr) {
				System.out.print(b + ", ");
			}
			System.out.println();
		}

		public static BufferedImage convertToImage(byte[] bytes) throws IOException {
			return ImageIO.read(new ByteArrayInputStream(bytes));
		}
	}

	static class IOUtils {

		private static final Scanner in = new Scanner(System.in);

		public static synchronized String getInput(int maxLength) {
			String input = in.nextLine();
			if (input != null) {
				input = input.trim();
				if (input.length() > maxLength) {
					input = null;
				}
			}
			return input;
		}

		public static synchronized String getRawInput() {
			return in.nextLine();
		}

		public static File save(byte[] data, File currentDir, String fileNameExtFilterDesc, String fileExt)
				throws Exception {
			JFileChooser chooser = new JFileChooser();
			FileNameExtensionFilter filter = new FileNameExtensionFilter(fileNameExtFilterDesc, fileExt);
			chooser.setFileFilter(filter);
			if (currentDir != null)
				chooser.setCurrentDirectory(currentDir);
			int retrival = chooser.showSaveDialog(null);
			if (retrival != JFileChooser.APPROVE_OPTION) {
				e("Cancelled");
				return null;
			}

			File target = chooser.getSelectedFile();
			if (target.exists()) {
				e("Override existing file is not allowed!");
				return null;
			}

			String fileName = target.getAbsolutePath();
			if (!fileName.endsWith("." + fileExt)) {
				fileName += ("." + fileExt);
			}

			FileUtils.writeByteArrayToFile(target = new File(fileName), RSA.encryptRSA(data));
			System.out.println("'" + target.getName() + "' saved");
			System.out.println("Full Path: " + fileName);
			return target;
		}

		public static File open(File currentDir, String fileNameExtFilterDesc, String fileExt) {
			JFileChooser chooser = new JFileChooser();
			FileNameExtensionFilter filter = new FileNameExtensionFilter(fileNameExtFilterDesc, fileExt);
			chooser.setFileFilter(filter);
			if (currentDir != null)
				chooser.setCurrentDirectory(currentDir);
			int returnVal = chooser.showOpenDialog(null);
			if (returnVal != JFileChooser.APPROVE_OPTION) {
				e("Cancelled");
				return null;
			}
			return chooser.getSelectedFile();
		}

		public static void displayTextBox(String text, String title) {
			JFrame fr;
			JScrollPane panelMain;
			JTextArea txtData;

			fr = new JFrame();
			fr.setLayout(new GridLayout(1, 1));

			txtData = new JTextArea(80, 20);
			txtData.setVisible(true);

			panelMain = new JScrollPane(txtData);
			panelMain.setVisible(true);

			txtData.setText(text);

			fr.setTitle(title);
			fr.setLocationRelativeTo(null);
			fr.add(panelMain);
			fr.setVisible(true);
			fr.setSize(600, 400);
		}
	}
}
