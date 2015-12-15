package com.example.manish.androidcms.networking;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.example.manish.androidcms.CMS;
import com.example.manish.androidcms.models.Blog;

import NetWorking.Authenticator;
import NetWorking.AuthenticatorRequest;
import org.wordpress.android.util.StringUtils;

/**
 * Created by Manish on 4/13/2015.
 */
public class OAuthAuthenticator implements Authenticator {
    @Override
    public void authenticate(final AuthenticatorRequest request) {
        String siteId = request.getSiteId();

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(CMS.getContext());
        String token = settings.getString(CMS.ACCESS_TOKEN_PREFERENCE, null);

        if (siteId != null) {
            // Get the token for a Jetpack site if needed
            Blog blog = CMS.cmsDB.getBlogForDotComBlogId(siteId);

            if (blog != null) {
                String jetpackToken = blog.getApi_key();

                // valid OAuth tokens are 64 chars
                if (jetpackToken != null && jetpackToken.length() == 64 && !blog.isDotcomFlag()) {
                    token = jetpackToken;
                }
            }
        }

        request.sendWithAccessToken(StringUtils.notNullStr(token));
    }
}
