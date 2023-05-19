package framework.telegram.support.tools;

import android.net.Uri;

import framework.telegram.support.tools.framework.telegram.support.UriUtils;

/**
 * Created by hyf on 2016/10/25.
 */

public class ResourceUtils {
    private ResourceUtils(){

    }

    public static boolean isFileScheme(Uri uri) {
        if (uri != null && "file".equals(uri.getScheme())) {
            return true;
        }

        return false;
    }

    public static boolean isHttpScheme(Uri uri) {
        if (uri != null && ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))) {
            return true;
        }

        return false;
    }

    public static boolean isFileScheme(String uriString) {
        if (uriString != null) {
            Uri uri = UriUtils.parseUri(uriString);
            if (uri != null && "file".equals(uri.getScheme())) {
                return true;
            }
        }

        return false;
    }

    public static boolean isHttpScheme(String uriString) {
        if (uriString != null) {
            Uri uri = UriUtils.parseUri(uriString);
            if (uri != null && ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))) {
                return true;
            }
        }

        return false;
    }
}
