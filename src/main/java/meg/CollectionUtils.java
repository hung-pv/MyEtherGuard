package meg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class CollectionUtils {
	public static byte[] merge(Collection<byte[]> collection, int size) {
		byte[] arr = new byte[size];
		int pointer = 0;
		for (byte[] ele : collection) {
			for (byte b : ele) {
				arr[pointer++] = b;
			}
		}
		return arr;
	}

	public static List<byte[]> split(byte[] arr, int size) {
		List<byte[]> result = new ArrayList<byte[]>();
		int remain = arr.length;
		int pointer = 0;
		while (remain > 0) {
			int spl = Math.min(remain, size);
			int endIndex = pointer + spl;
			byte[] barr = Arrays.copyOfRange(arr, pointer, endIndex);
			result.add(barr);
			pointer = endIndex;
			remain = arr.length - pointer;
		}
		return result;
	}
}
