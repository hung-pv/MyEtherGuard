package meg.wallet;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class WalletListManager {

	private List<WalletInfo> wallets = new ArrayList<>();
	
	public void add(File file) {
		WalletInfo wi = WalletInfo.empty();
		
		this.wallets.add(wi);
	}
}
