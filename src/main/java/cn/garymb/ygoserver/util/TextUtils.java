package cn.garymb.ygoserver.util;

public final class TextUtils {
	
	public static boolean isEmpty(CharSequence str) {
        if (str == null || str.length() == 0)
            return true;
        else
            return false;
	}

}
