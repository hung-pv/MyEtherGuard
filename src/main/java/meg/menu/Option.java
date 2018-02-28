package meg.menu;

import java.lang.reflect.Method;

import meg.App;

public class Option {

	private String displayText;
	private String processMethod;
	private boolean requiredKeystore;

	public Option(String displayText, String processMethod) {
		this.displayText = displayText;
		this.processMethod = processMethod;
	}
	
	public Option(String displayText, String processMethod, boolean requiredKeystore) {
		this.displayText = displayText;
		this.processMethod = processMethod;
		this.requiredKeystore = requiredKeystore;
	}

	public String getDisplayText() {
		return displayText;
	}
	
	public boolean isRequireKeystore() {
		return this.requiredKeystore;
	}

	public void processMethod() {
		try {
			if (this.requiredKeystore) {
				Method medLoadKeystore = App.class.getDeclaredMethod("loadKeystore");
				medLoadKeystore.setAccessible(true);
				medLoadKeystore.invoke(null);
			}
			
			Method med = App.class.getDeclaredMethod(this.processMethod);
			med.setAccessible(true);
			med.invoke(null);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
