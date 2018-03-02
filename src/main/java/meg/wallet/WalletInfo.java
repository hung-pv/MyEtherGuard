package meg.wallet;

import java.util.Base64;

public class WalletInfo {

	private String type;
	private String address;
	private byte[] privateKeyEncrypted;
	private byte[] mnemoticEncrypted;
	private byte[] noteEncrypted;

	public WalletInfo(String type, String address, byte[] privateKeyEncrypted, byte[] mnemoticEncrypted,
			byte[] noteEncrypted) {
		this.type = type;
		this.address = address;
		this.privateKeyEncrypted = privateKeyEncrypted;
		this.mnemoticEncrypted = mnemoticEncrypted;
		this.noteEncrypted = noteEncrypted;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public byte[] getPrivateKeyEncrypted() {
		return privateKeyEncrypted;
	}

	public void setPrivateKeyEncrypted(byte[] privateKeyEncrypted) {
		this.privateKeyEncrypted = privateKeyEncrypted;
	}

	public byte[] getMnemoticEncrypted() {
		return mnemoticEncrypted;
	}

	public void setMnemoticEncrypted(byte[] mnemoticEncrypted) {
		this.mnemoticEncrypted = mnemoticEncrypted;
	}

	public byte[] getNoteEncrypted() {
		return noteEncrypted;
	}

	public void setNoteEncrypted(byte[] noteEncrypted) {
		this.noteEncrypted = noteEncrypted;
	}

	@Override
	public String toString() {
		return (this.type == null ? "other" : this.type) + "\n" + //
				this.address + "\n" + //
				Base64.getEncoder().encodeToString(this.privateKeyEncrypted) + "\n" + //
				(this.mnemoticEncrypted == null ? "(no mnemotic)"
						: Base64.getEncoder().encodeToString(this.mnemoticEncrypted))
				+ "\n" + //
				(this.noteEncrypted == null ? "(no note)"
						: Base64.getEncoder().encodeToString(this.noteEncrypted));
	}
}
