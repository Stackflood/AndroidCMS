package com.example.manish.androidcms.ui.accounts.helpers;

/**
 * Created by Manish on 4/9/2015.
 */
public abstract class LoginAbstract {
    protected String mUsername;
    protected String mPassword;
    protected Callback mCallback;

    public interface Callback {
        void onSuccess();
        void onError(int errorMessageId,
                     boolean twoStepCodeRequired,
                     boolean httpAuthRequired,
                     boolean erroneousSslCertificate);
    }

    public LoginAbstract(String username, String password) {
        mUsername = username;
        mPassword = password;
    }

    public void execute(Callback callback) {
        mCallback = callback;
        new Thread() {
            @Override
            public void run() {
                login();
            }
        }.start();
    }

    //Different classes using this abstract class will use the method differently
    protected abstract void login();
}
