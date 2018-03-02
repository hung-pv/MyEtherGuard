package meg;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Base64;

public class USB {

	public static final String USB_ID_FILE_NAME = new String(Base64.getDecoder().decode("dXNiLjR1dGg="), StandardCharsets.UTF_8);
	
	private File drive;

	public USB(File drive) {
		this.drive = drive;
	}
	
	public File getFileInUsb(String...path) {
		return this.drive == null ? null : Paths.get(this.drive.getAbsolutePath(), path).toFile();
	}
	
	public File[] getFilesInUsb() {
		return this.drive == null ? null : this.drive.listFiles();
	}
	
	public boolean isValid() {
		return this.drive != null && this.drive.exists() && this.drive.isDirectory() && this.getUsbIdFile().exists();
	}
	
	public String getAbsolutePath() {
		return this.drive == null ? null : this.drive.getAbsolutePath();
	}
	
	public File getUsbIdFile() {
		return getFileInUsb(USB_ID_FILE_NAME);
	}
}
