package meg.wallet;

public enum WalletType {
	ERC20("Ethereum (ERC20 tokens)"), Bitcoin("Bitcoin based");
	
	private String displayText;
	
	private WalletType(String displayText) {
		this.displayText = displayText;
	}
	
	public String getDisplayText() {
		return displayText;
	}
}
