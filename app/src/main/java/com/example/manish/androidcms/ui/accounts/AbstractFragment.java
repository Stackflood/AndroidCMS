package com.example.manish.androidcms.ui.accounts;

import android.app.Fragment;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Bundle;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import org.wordpress.android.util.AppLog;

/**
 * A fragment representing a single step in a wizard. The fragment shows a dummy title indicating
 * the page number, along with some dummy text.
 */
public abstract class AbstractFragment extends Fragment {

    protected static RequestQueue requestQueue;
    protected ConnectivityManager mSystemService;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppLog.v(AppLog.T.NUX, "NewAccountAbstractOage.onCreate()");
        mSystemService = (ConnectivityManager) getActivity()
                .getApplicationContext().
                getSystemService(Context.CONNECTIVITY_SERVICE);
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(getActivity());
        }
    }


    protected void startProgress(String message) {
    }

    protected void updateProgress(String message) {
    }

    protected void endProgress() {
    }

    protected abstract void onDoneAction();

    protected abstract boolean isUserDataValid();

    protected enum ErrorType {USERNAME, PASSWORD, SITE_URL, EMAIL, TITLE, UNDEFINED}
}
