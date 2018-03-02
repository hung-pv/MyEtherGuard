package meg;

import java.io.IOException;

public class FileExistsException extends IOException {

	private static final long serialVersionUID = -4456000906696489312L;

	public FileExistsException(String message) {
		super(message);
	}
}
