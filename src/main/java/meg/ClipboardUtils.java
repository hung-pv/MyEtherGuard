package meg;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;

public class ClipboardUtils {

	public static void setClipboard(String text) {
		try {
			Toolkit.getDefaultToolkit()
	        .getSystemClipboard()
	        .setContents(
	                new StringSelection(text),
	                null
	        );
			System.out.println("(Copied to clipboard)");
		} catch (Exception e) {
		}
	}
}
