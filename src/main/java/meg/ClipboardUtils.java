package meg;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;

public class ClipboardUtils {

	public static void setClipboard(String text, String name) {
		try {
			Toolkit.getDefaultToolkit()
	        .getSystemClipboard()
	        .setContents(
	                new StringSelection(text),
	                null
	        );
			if (name == null)
				System.out.println("(Copied to clipboard)");
			else
				System.out.println(String.format("(%s was copied to clipboard)", name));
		} catch (Exception e) {
		}
	}
}
