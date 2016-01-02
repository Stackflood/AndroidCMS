package com.example.manish.androidcms.ui.reader.utils;

import android.text.TextUtils;

import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.UrlUtils;

/**
 * Created by Manish on 12/30/2015.
 */
public class ReaderUtils {

    /*
   * returns the passed string formatted for use with our API - see sanitize_title_with_dashes
   * https://github.com/WordPress/WordPress/blob/master/wp-includes/formatting.php#L1258
   */
    public static String sanitizeWithDashes(final String title) {
        if (title == null) {
            return "";
        }
        return title.trim()
                .replaceAll("&[^\\s]*;", "")        // remove html entities
                .replaceAll("[\\.\\s]+", "-")       // replace periods and whitespace with a dash
                .replaceAll("[^A-Za-z0-9\\-]", "")  // remove remaining non-alphanum/non-dash chars
                .replaceAll("--", "-");             // reduce double dashes potentially added above
    }

    public static String getResizedImageUrl(final String imageUrl, int width, int height, boolean isPrivate) {
        return getResizedImageUrl(imageUrl, width, height, isPrivate, PhotonUtils.Quality.MEDIUM);
    }
    public static String getResizedImageUrl(final String imageUrl,
                                            int width,
                                            int height,
                                            boolean isPrivate,
                                            PhotonUtils.Quality quality) {
        if (isPrivate) {
            return getPrivateImageForDisplay(imageUrl, width, height);
        } else {
            return PhotonUtils.getPhotonImageUrl(imageUrl, width, height, quality);
        }
    }

    /*
     * use this to request a reduced size image from a private post - images in private posts can't
     * use photon but these are usually wp images so they support the h= and w= query params
     */
    private static String getPrivateImageForDisplay(final String imageUrl, int width, int height) {
        if (TextUtils.isEmpty(imageUrl)) {
            return "";
        }

        final String query;
        if (width > 0 && height > 0) {
            query = "?w=" + width + "&h=" + height;
        } else if (width > 0) {
            query = "?w=" + width;
        } else if (height > 0) {
            query = "?h=" + height;
        } else {
            query = "";
        }
        // remove the existing query string, add the new one, and make sure the url is https:
        return UrlUtils.removeQuery(UrlUtils.makeHttps(imageUrl)) + query;
    }
}
