package meg.menu;

import java.util.ArrayList;
import java.util.List;

public class MenuManager {

	private List<Option> options = new ArrayList<>();
	
	public void add(Option option) {
		this.options.add(option);
	}
	
	public void add(String displayText, String processMethod) {
		this.add(displayText, processMethod, false);
	}
	
	public void add(String displayText, String processMethod, boolean requireKeystore) {
		this.options.add(new Option(displayText, processMethod, requireKeystore));
	}
	
	public void showOptionList(String header) {
		if (header != null)
			System.out.println(header);
		for(int i = 0; i < this.options.size(); i++) {
			System.out.println(String.format("%s%d. %s", header != null ? " " : "", i+1, this.options.get(i).getDisplayText()));
		}
	}
	
	public Option getOptionBySelection(int selected) {
		return this.options.get(selected - 1);
	}
}
