package meg.wallet;

public enum WalletType {
	ERC20("Ethereum (and ERC20 tokens)"), BIT("Bitcoin based"), Other("Other");
	
	private String displayText;
	
	private WalletType(String displayText) {
		this.displayText = displayText;
	}
	
	public String getDisplayText() {
		return displayText;
	}
}
