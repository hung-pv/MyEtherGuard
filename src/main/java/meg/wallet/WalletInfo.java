package meg.wallet;

import java.util.Base64;

import meg.UncompletedFile;

public class WalletInfo {

	private String type;
	private String address;
	private byte[] privateKeyEncrypted;
	private byte[] mnemonicEncrypted;
	private byte[] noteEncrypted;
	
	private WalletInfo() {
		
	}

	public WalletInfo(String type, String address, byte[] privateKeyEncrypted, byte[] mnemonicEncrypted,
			byte[] noteEncrypted) {
		this.type = type;
		this.address = address;
		this.privateKeyEncrypted = privateKeyEncrypted;
		this.mnemonicEncrypted = mnemonicEncrypted;
		this.noteEncrypted = noteEncrypted;
	}

	public String getType() {
		return type;
	}

	public String getAddress() {
		return address;
	}

	public byte[] getPrivateKeyEncrypted() {
		return privateKeyEncrypted;
	}

	public byte[] getMnemonicEncrypted() {
		return mnemonicEncrypted;
	}

	public byte[] getNoteEncrypted() {
		return noteEncrypted;
	}
	
	public boolean containsPrivateKey() {
		return this.privateKeyEncrypted != null;
	}
	
	public boolean containsMnemonic() {
		return this.mnemonicEncrypted != null;
	}
	
	public boolean containsNote() {
		return this.noteEncrypted != null;
	}
	
	public byte[] toRaw() {
		UncompletedFile uf = new UncompletedFile();
		uf.append(this.type);
		uf.append(this.address);
		uf.append(this.privateKeyEncrypted == null ? null : Base64.getEncoder().encodeToString(this.privateKeyEncrypted));
		uf.append(this.mnemonicEncrypted == null ? null : Base64.getEncoder().encodeToString(this.mnemonicEncrypted));
		uf.append(this.noteEncrypted == null ? null : Base64.getEncoder().encodeToString(this.noteEncrypted));
		return uf.toRaw();
	}
	
	public static WalletInfo fromUncompletedFile(UncompletedFile file) {
		WalletInfo result = new WalletInfo();
		result.type = file.getLine(0);
		result.address = file.getLine(1);
		result.privateKeyEncrypted = file.getLine(2) == null ? null : Base64.getDecoder().decode(file.getLine(2));
		result.mnemonicEncrypted = file.getLine(3) == null ? null : Base64.getDecoder().decode(file.getLine(3));
		result.noteEncrypted = file.getLine(4) == null ? null : Base64.getDecoder().decode(file.getLine(4));
		return result;
	}
	
	public static WalletInfo empty() {
		return new WalletInfo();
	}
}
