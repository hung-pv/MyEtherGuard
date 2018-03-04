package meg;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Base64;

public class MegDevice {

	public static final String ID_FILE_NAME = new String(Base64.getDecoder().decode("dXNiLjR1dGg="), StandardCharsets.UTF_8);
	
	private File drive;

	public MegDevice(File drive) {
		this.drive = drive;
	}
	
	public File getFile(String...path) {
		return this.drive == null ? null : Paths.get(this.drive.getAbsolutePath(), path).toFile();
	}
	
	public File[] getFiles() {
		return this.drive == null ? null : this.drive.listFiles();
	}
	
	public boolean isValid() {
		return this.drive != null && this.drive.exists() && this.drive.isDirectory() && this.getIdFile().exists();
	}
	
	public String getAbsolutePath() {
		return this.drive == null ? null : this.drive.getAbsolutePath();
	}
	
	public File getIdFile() {
		return getFile(ID_FILE_NAME);
	}
}
