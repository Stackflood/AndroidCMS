package com.example.manish.androidcms.ui.accounts.helpers;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.example.manish.androidcms.BuildConfig;
import com.example.manish.androidcms.CMS;
import com.example.manish.androidcms.R;
import com.example.manish.androidcms.models.Blog;
import com.example.manish.androidcms.util.VolleyUtils;
import Rest.Oauth;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.AppLog;

/**
 * Created by Manish on 4/9/2015.
 */
public class LoginWPCom extends LoginAbstract {

    private String mTwoStepCode;
    private boolean mShouldSendTwoStepSMS;
    private Blog mJetpackBlog;

    public LoginWPCom(String username, String password, String twoStepCode,
                      boolean shouldSendTwoStepSMS,
                      Blog blog) {
        super(username, password);
        mTwoStepCode = twoStepCode;
        mShouldSendTwoStepSMS = shouldSendTwoStepSMS;
        mJetpackBlog = blog;
    }


    private Request makeOAuthRequest(final String username,
                                     final String password,
                                     final Oauth.Listener listener,
                                     final Oauth.ErrorListener errorListener)
    {
        Oauth oauth = new Oauth(BuildConfig.OAUTH_APP_ID,
                                BuildConfig.OAUTH_APP_SECRET,
                                BuildConfig.OAUTH_REDIRECT_URI );

        Request oauthRequest;

        oauthRequest = oauth.makeRequest(username, password, mTwoStepCode, mShouldSendTwoStepSMS,
                listener, errorListener);

        return oauthRequest;
    }


    public static int restLoginErrorToMsgId(JSONObject errorObject) {
        // Default to generic error message
        int errorMsgId = R.string.nux_cannot_log_in;

        // Map REST errors to local error codes
        if (errorObject != null) {
            try {
                String error = errorObject.optString("error", "");
                String errorDescription = errorObject.getString("error_description");
                if (error.equals("invalid_request")) {
                    if (errorDescription.contains("Incorrect username or password.")) {
                        errorMsgId = R.string.username_or_password_incorrect;
                    }
                } else if (error.equals("needs_2fa")) {
                    errorMsgId = R.string.account_two_step_auth_enabled;
                } else if (error.equals("invalid_otp")) {
                    errorMsgId = R.string.invalid_verification_code;
                }
            } catch (JSONException e) {
                AppLog.e(AppLog.T.NUX, e);
            }
        }
        return errorMsgId;
    }


    protected void login() {

       /* SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(CMS.getContext());

        SharedPreferences.Editor editor = settings.edit();

        String existingUsername = settings.getString(CMS.WPCOM_USERNAME_PREFERENCE, null);

        editor.remove(CMS.IS_SIGNED_OUT_PREFERENCE);
        editor.apply();

        mCallback.onSuccess();*/
        //Bypass this s** for now

        // Get OAuth token for the first time and check for errors
       CMS.requestQueue.add(makeOAuthRequest(mUsername,
                mPassword,
                new Oauth.Listener() {
            @SuppressLint("CommitPrefEdits")
            @Override
            public void onResponse(final Oauth.Token token) {
                // Once we have a token, start up Simperium
                //SimperiumUtils.configureSimperium(WordPress.getContext(), token.toString());

                if (mJetpackBlog != null) {
                    // Store token in blog object for Jetpack sites
                    //mJetpackBlog.setApi_key(token.toString());
                   // CMS.cmsDB.saveBlog(mJetpackBlog);
                }

                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(CMS.getContext());

                SharedPreferences.Editor editor = settings.edit();

                String existingUsername = settings.getString(CMS.WPCOM_USERNAME_PREFERENCE, null);
              /*if (settings.contains(CMS.IS_SIGNED_OUT_PREFERENCE) && existingUsername != null &&
                        !existingUsername.equals(mUsername)) {
                    // If user has signed out and back in with a different username, we must clear the old data
                    CMS.wpDB.dangerouslyDeleteAllContent();
                    CMS.removeWpComUserRelatedData(WordPress.getContext());
                    existingUsername = null;
                    CMS.currentBlog = null;
                }*/
                if (mJetpackBlog == null ||
                        TextUtils.isEmpty(settings.getString(CMS.WPCOM_USERNAME_PREFERENCE, null))) {
                    // Store token in global account
                    //SharedPreferences.Editor editor = settings.edit();
                    editor.putString(CMS.WPCOM_USERNAME_PREFERENCE, mUsername);
                    editor.putString(CMS.ACCESS_TOKEN_PREFERENCE, token.toString());
                    //editor.commit();
                }

                editor.remove(CMS.IS_SIGNED_OUT_PREFERENCE);
                editor.apply();

                mCallback.onSuccess();
            }
        }, new Oauth.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                JSONObject errorObject = VolleyUtils.volleyErrorToJSON(volleyError);
                int errorMsgId = restLoginErrorToMsgId(errorObject);
                mCallback.onError(errorMsgId, errorMsgId == R.string.account_two_step_auth_enabled, false, false);
            }
        }));
    }
}
