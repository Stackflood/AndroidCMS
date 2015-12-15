package com.example.manish.androidcms.ui.accounts.helpers;

import com.android.volley.VolleyError;
import com.example.manish.androidcms.CMS;
import com.example.manish.androidcms.networking.OAuthAuthenticator;
import com.example.manish.androidcms.util.VolleyUtils;
import Rest.RestRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import NetWorking.RestClientUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.JSONUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Manish on 4/9/2015.
 */
public class FetchBlogListWPCom extends FetchBlogListAbstract {
    @Override
    protected void fetchBlogList(Callback callback) {
        getUsersBlogsRequestREST(callback);
    }

    private List<Map<String, Object>> convertJSONObjectToSiteList(JSONObject jsonObject, boolean keepJetpackSites) {
        List<Map<String, Object>> sites = new ArrayList<Map<String, Object>>();
        JSONArray jsonSites = jsonObject.optJSONArray("sites");
        if (jsonSites != null) {
            for (int i = 0; i < jsonSites.length(); i++) {
                JSONObject jsonSite = jsonSites.optJSONObject(i);
                Map<String, Object> site = new HashMap<String, Object>();
                try {
                    // skip if it's a jetpack site and we don't keep them
                    if (jsonSite.getBoolean("jetpack") && !keepJetpackSites) {
                        continue;
                    }
                    site.put("blogName", jsonSite.get("name"));
                    site.put("url", jsonSite.get("URL"));
                    site.put("blogid", jsonSite.get("ID"));
                    site.put("isAdmin", jsonSite.get("user_can_manage"));
                    site.put("isVisible", jsonSite.get("visible"));
                    JSONObject jsonLinks = JSONUtils.getJSONChild(jsonSite, "meta/links");
                    if (jsonLinks != null) {
                        site.put("xmlrpc", jsonLinks.getString("xmlrpc"));
                        sites.add(site);
                    } else {
                        AppLog.e(AppLog.T.NUX, "xmlrpc links missing from the me/sites REST response");
                    }
                } catch (JSONException e) {
                    AppLog.e(AppLog.T.NUX, e);
                }
            }
        }
        return sites;
    }

    private void getUsersBlogsRequestREST(final FetchBlogListAbstract.Callback callback) {
        CMS.getRestClientUtils().get("me/sites", new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject response) {
                if (response != null) {
                    List<Map<String, Object>> userBlogListReceiver = convertJSONObjectToSiteList(response, false);
                    callback.onSuccess(userBlogListReceiver);
                } else {
                    callback.onSuccess(null);
                }
            }
        }, new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                JSONObject errorObject = VolleyUtils.volleyErrorToJSON(volleyError);
                callback.onError(LoginWPCom.restLoginErrorToMsgId(errorObject), false, false, false, "");
            }
        });
    }



    public FetchBlogListWPCom() {
        super(null, null);
    }
}
