package com.example.manish.androidcms.util;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpStack;
import com.example.manish.androidcms.models.Blog;
import com.example.manish.androidcms.networking.WPDelayedHurlStack;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

/**
 * Created by Manish on 4/9/2015.
 */
public class VolleyUtils {

    /*
     * attempts to return JSON from a volleyError - useful for WP REST API failures, which often
     * contain JSON in the response
     */
    public static JSONObject volleyErrorToJSON(VolleyError volleyError) {
        if (volleyError == null || volleyError.networkResponse == null ||
                volleyError.networkResponse.data == null
                ||
                volleyError.networkResponse.headers == null) {
            return null;
        }

        String contentType = volleyError.networkResponse.headers.get("Content-Type");
        if (contentType == null || !contentType.equals("application/json")) {
            return null;
        }

        try {
            String response = new String(volleyError.networkResponse.data, "UTF-8");
            JSONObject json = new JSONObject(response);
            return json;
        } catch (UnsupportedEncodingException e) {
            return null;
        } catch (JSONException e) {
            return null;
        }
    }

    /*
     * cancel all Volley requests
     */
    public static void cancelAllRequests(RequestQueue requestQueue) {
        if (requestQueue==null)
            return;
        RequestQueue.RequestFilter filter = new RequestQueue.RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return true;
            }
        };
        requestQueue.cancelAll(filter);
    }

    public static int statusCodeFromVolleyError(VolleyError volleyError) {
        if (volleyError == null || volleyError.networkResponse == null) {
            return 0;
        }
        return volleyError.networkResponse.statusCode;
    }

    /*
     * Return true if the blog is protected with HTTP Basic Auth
     */
    public static boolean isCustomHTTPClientStackNeeded(Blog currentBlog) {
        if (currentBlog.hasValidHTTPAuthCredentials())
            return true;

        return false;
    }

    public static HttpStack getHTTPClientStack(final Context ctx) {
        return getHTTPClientStack(ctx, null);
    }

    public static HttpStack getHTTPClientStack(final Context ctx, final Blog currentBlog) {
        return new WPDelayedHurlStack(ctx, currentBlog);
    }
}
