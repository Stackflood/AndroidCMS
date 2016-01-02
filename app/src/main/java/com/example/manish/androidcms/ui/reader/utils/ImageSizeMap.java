package com.example.manish.androidcms.ui.reader.utils;

import android.text.TextUtils;

import com.example.manish.androidcms.util.JSONUtil;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.UrlUtils;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by Manish on 12/30/2015.
 */
public class ImageSizeMap extends HashMap<String, ImageSizeMap.ImageSize> {
    private static final String EMPTY_JSON = "{}";
    public ImageSizeMap(String jsonString) {
        if (TextUtils.isEmpty(jsonString) || jsonString.equals(EMPTY_JSON)) {
            return;
        }

        try {
            JSONObject json = new JSONObject(jsonString);
            Iterator<String> it = json.keys();
            if (!it.hasNext()) {
                return;
            }

            while (it.hasNext()) {
                JSONObject jsonAttach = json.optJSONObject(it.next());
                if (jsonAttach != null && JSONUtil.getString(jsonAttach, "mime_type").startsWith("image")) {
                    String normUrl = UrlUtils.normalizeUrl(UrlUtils.removeQuery(JSONUtil.getString(jsonAttach, "URL")));
                    int width = jsonAttach.optInt("width");
                    int height = jsonAttach.optInt("height");
                    this.put(normUrl, new ImageSize(width, height));
                }
            }
        } catch (JSONException e) {
            AppLog.e(AppLog.T.READER, e);
        }
    }

    public ImageSize getImageSize(final String imageUrl) {
        if (imageUrl == null) {
            return null;
        } else {
            return super.get(UrlUtils.normalizeUrl(UrlUtils.removeQuery(imageUrl)));
        }
    }
    public String getLargestImageUrl(int minImageWidth) {
        String currentImageUrl = null;
        int currentMaxWidth = minImageWidth;
        for (Entry<String, ImageSize> attach : this.entrySet()) {
            if (attach.getValue().width > currentMaxWidth) {
                currentImageUrl = attach.getKey();
                currentMaxWidth = attach.getValue().width;
            }
        }

        return currentImageUrl;
    }



        public static class ImageSize {
        public final int width;
        public final int height;
        public ImageSize(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }
}
