package meg;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;

public class UncompletedFile {

	private List<String> lines = new ArrayList<String>();

	public void append(String...lines) {
		for (String line : lines) {
			this.lines.add(line);
		}
	}

	public void append(Collection<String> lines) {
		this.lines.addAll(lines);
	}
	
	public String getLine(int index) {
		return this.lines.get(index);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("DO NOT change content of this file, any modification will corrupt this file and LOSING data FOREVER\n");
		this.lines.forEach(line -> {
			if (line == null) {
				sb.append('0');
			} else {
				sb.append('\t');
				sb.append(line);
			}
			sb.append('\n');
		});
		return sb.toString();
	}
	
	public byte[] toRaw() {
		return this.toString().getBytes(StandardCharsets.UTF_8);
	}
	
	public static UncompletedFile fromFile(File file) throws IOException {
		byte[] raw = FileUtils.readFileToByteArray(file);
		String[] spl = new String(raw, StandardCharsets.UTF_8).split("\n");
		List<String> lines = new ArrayList<>();
		for (int i = 1; i < spl.length; i++) {
			String line = spl[i];
			if (line.startsWith("0")) {
				lines.add(null);
			} else if (line.startsWith("\t")) {
				lines.add(line.substring(1));
			} else {
				throw new IOException("Invalid format of type UncompletedFile");
			}
		}
		
		UncompletedFile result = new UncompletedFile();
		result.append(lines);
		return result;
	}
}
