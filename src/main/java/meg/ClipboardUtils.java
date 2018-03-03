package meg;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;

public class ClipboardUtils {

	public static void setText(String text, String name) {
		if (text == null) {
			text = "";
		}
		try {
			Toolkit.getDefaultToolkit()
	        .getSystemClipboard()
	        .setContents(
	                new StringSelection(text),
	                null
	        );
			if (text.equals("")) {
				System.out.println("Clipboard cleared");
			} else if (name == null)
				System.out.println("(Copied to clipboard)");
			else
				System.out.println(String.format("(%s was copied to clipboard)", name));
		} catch (Exception e) {
		}
	}
	
	public static void clear() {
		setText(null, null);
	}
}
