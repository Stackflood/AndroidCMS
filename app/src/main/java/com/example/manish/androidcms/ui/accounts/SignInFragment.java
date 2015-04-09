package com.example.manish.androidcms.ui.accounts;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.example.manish.androidcms.R;
import com.example.manish.androidcms.models.Blog;
import com.example.manish.androidcms.widgets.CMSTextView;

import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.NetworkUtils;

/**
 * Created by Manish on 4/8/2015.
 */
public class SignInFragment extends AbstractFragment implements TextWatcher {
    private static final String DOT_COM_BASE_URL = "https://wordpress.com";
    private static final String FORGOT_PASSWORD_RELATIVE_URL = "/wp-login.php?action=lostpassword";
    private static final int WPCOM_ERRONEOUS_LOGIN_THRESHOLD = 3;
    private static final String FROM_LOGIN_SCREEN_KEY = "FROM_LOGIN_SCREEN_KEY";

    public static final String ENTERED_URL_KEY = "ENTERED_URL_KEY";
    public static final String ENTERED_USERNAME_KEY = "ENTERED_USERNAME_KEY";

    private EditText mUsernameEditText;
    private EditText mPasswordEditText;
    private EditText mUrlEditText;
    private EditText mTwoStepEditText;

    private CMSTextView mSignInButton;
    private CMSTextView mCreateAccountButton;
    private CMSTextView mAddSelfHostedButton;
    private CMSTextView mProgressTextSignIn;
    private CMSTextView mForgotPassword;
    private CMSTextView mJetpackAuthLabel;

    private LinearLayout mBottomButtonsLayout;
    private RelativeLayout mUsernameLayout;
    private RelativeLayout mPasswordLayout;
    private RelativeLayout mProgressBarSignIn;
    private RelativeLayout mUrlButtonLayout;
    private RelativeLayout mTwoStepLayout;
    private LinearLayout mTwoStepFooter;

    private ImageView mInfoButton;
    private ImageView mInfoButtonSecondary;

    //private final EmailChecker mEmailChecker;

    private boolean mSelfHosted;
    private boolean mEmailAutoCorrected;
    private boolean mShouldSendTwoStepSMS;
    private int mErroneousLogInCount;
    private String mUsername;
    private String mPassword;
    private String mTwoStepCode;
    private String mHttpUsername;
    private String mHttpPassword;
    private Blog mJetpackBlog;

    public SignInFragment() {
        //mEmailChecker = new EmailChecker();
    }

    @Override
    protected void onDoneAction() {

    }

    @Override
    protected boolean isUserDataValid() {

        final String username = EditTextUtils.getText(mUsernameEditText).trim();
        final String password = EditTextUtils.getText(mPasswordEditText).trim();
        boolean retValue = true;

        if (username.equals("")) {
            mUsernameEditText.setError(getString(R.string.required_field));
            mUsernameEditText.requestFocus();
            retValue = false;
        }

        if (password.equals("")) {
            mPasswordEditText.setError(getString(R.string.required_field));
            mPasswordEditText.requestFocus();
            retValue = false;
        }
        return retValue;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        ViewGroup rootView = (ViewGroup)inflater.inflate(R.layout.signin_fragment, container, false);
        mUrlButtonLayout = (RelativeLayout) rootView.findViewById(R.id.url_button_layout);
        mTwoStepLayout = (RelativeLayout) rootView.findViewById(R.id.two_factor_layout);
        mTwoStepFooter = (LinearLayout) rootView.findViewById(R.id.two_step_footer);
        mUsernameLayout = (RelativeLayout) rootView.findViewById(R.id.nux_username_layout);
        mUsernameLayout.setOnClickListener(mOnLoginFormClickListener);
        mPasswordLayout = (RelativeLayout) rootView.findViewById(R.id.nux_password_layout);
        mPasswordLayout.setOnClickListener(mOnLoginFormClickListener);

        mUsernameEditText = (EditText) rootView.findViewById(R.id.nux_username);
        mUsernameEditText.addTextChangedListener(this);
        mUsernameEditText.setOnClickListener(mOnLoginFormClickListener);
        mPasswordEditText = (EditText) rootView.findViewById(R.id.nux_password);
        mPasswordEditText.addTextChangedListener(this);
        mPasswordEditText.setOnClickListener(mOnLoginFormClickListener);
        mJetpackAuthLabel = (CMSTextView) rootView.findViewById(R.id.nux_jetpack_auth_label);
        mUrlEditText = (EditText) rootView.findViewById(R.id.nux_url);
        mSignInButton = (CMSTextView) rootView.findViewById(R.id.nux_sign_in_button);
        mSignInButton.setOnClickListener(mSignInClickListener);
        mProgressBarSignIn = (RelativeLayout) rootView.findViewById(R.id.nux_sign_in_progress_bar);
        mProgressTextSignIn = (CMSTextView) rootView.findViewById(R.id.nux_sign_in_progress_text);
        mCreateAccountButton = (CMSTextView) rootView.findViewById(R.id.nux_create_account_button);
        mCreateAccountButton.setOnClickListener(mCreateAccountListener);
        mAddSelfHostedButton = (CMSTextView) rootView.findViewById(R.id.nux_add_selfhosted_button);

        mAddSelfHostedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mUrlButtonLayout.getVisibility() == View.VISIBLE) {
                    mUrlButtonLayout.setVisibility(View.GONE);
                    mAddSelfHostedButton.setText(getString(R.string.nux_add_selfhosted_blog));
                    mSelfHosted = false;
                } else {
                    mUrlButtonLayout.setVisibility(View.VISIBLE);
                    mAddSelfHostedButton.setText(getString(R.string.nux_oops_not_selfhosted_blog));
                    mSelfHosted = true;
                }
            }
        });



       /* mForgotPassword = (WPTextView) rootView.findViewById(R.id.forgot_password);
        mForgotPassword.setOnClickListener(mForgotPasswordListener);
        mUsernameEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    autocorrectUsername();
                }
            }
        });

        mPasswordEditText.setOnEditorActionListener(mEditorAction);
        mUrlEditText.setOnEditorActionListener(mEditorAction);

        mTwoStepEditText = (EditText) rootView.findViewById(R.id.nux_two_step);
        mTwoStepEditText.addTextChangedListener(this);
        mTwoStepEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (keyCode == EditorInfo.IME_ACTION_DONE)) {
                    if (fieldsFilled()) {
                        signIn();
                    }
                }

                return false;
            }
        });*/

       /* WPTextView twoStepFooterButton = (WPTextView) rootView.findViewById(R.id.two_step_footer_button);
        twoStepFooterButton.setText(Html.fromHtml("<u>" + getString(R.string.two_step_footer_button) + "</u>"));
        twoStepFooterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestSMSTwoStepCode();
            }
        });*/

        mBottomButtonsLayout = (LinearLayout) rootView.findViewById(R.id.nux_bottom_buttons);
        /*initPasswordVisibilityButton(rootView, mPasswordEditText);
        initInfoButtons(rootView);
        moveBottomButtons();*/

        return rootView;
    }

    public void askForSslTrust()
    {
        /*SelfSignedSSLCertsManager.askForSslTrust(getActivity(),
                new GenericCallback<Void>() {
            @Override
            public void callback(Void aVoid) {
                // Try to signin again
                signIn();
            }
        });
        endProgress();*/
    }

    public void signInDotComUser(String username, String password) {
        if (username != null && password != null) {
            mUsernameEditText.setText(username);
            mPasswordEditText.setText(password);
            signIn();
        }
    }

    private final View.OnClickListener mOnLoginFormClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // Don't change layout if we are performing a network operation
            if (mProgressBarSignIn.getVisibility() == View.VISIBLE) return;

            if (mTwoStepLayout.getVisibility() == View.VISIBLE) {
                setTwoStepAuthVisibility(false);
            }
        }
    };

    private final View.OnClickListener mSignInClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            signIn();
        }
    };

    private final View.OnClickListener mCreateAccountListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent newAccountIntent = new Intent(getActivity(), NewAccountActivity.class);
            Activity activity = getActivity();
            if (activity != null) {
                activity.startActivityForResult(newAccountIntent, SignInActivity.CREATE_ACCOUNT_REQUEST);
            }
        }
    };

    private void signIn() {
        if (!isUserDataValid()) {
            return;
        }

        if (!checkNetworkConnectivity()) {
            return;
        }

        mUsername = EditTextUtils.getText(mUsernameEditText).trim();
        mPassword = EditTextUtils.getText(mPasswordEditText).trim();
        mTwoStepCode = EditTextUtils.getText(mTwoStepEditText).trim();
        if (isWPComLogin()) {
            startProgress(getString(R.string.connecting_wpcom));
            //signInAndFetchBlogListWPCom();
        } else {
            startProgress(getString(R.string.signing_in));
            //signInAndFetchBlogListWPOrg();
        }
    }

    protected void startProgress(String message) {
        mProgressBarSignIn.setVisibility(View.VISIBLE);
        mProgressTextSignIn.setVisibility(View.VISIBLE);
        mSignInButton.setVisibility(View.GONE);
        mProgressBarSignIn.setEnabled(false);
        mProgressTextSignIn.setText(message);
        mUsernameEditText.setEnabled(false);
        mPasswordEditText.setEnabled(false);
        mTwoStepEditText.setEnabled(false);
        mUrlEditText.setEnabled(false);
        mAddSelfHostedButton.setEnabled(false);
        mCreateAccountButton.setEnabled(false);
        mForgotPassword.setEnabled(false);
    }

    private boolean isWPComLogin() {
        String selfHostedUrl = EditTextUtils.getText(mUrlEditText).trim();
        return !mSelfHosted ||
                TextUtils.isEmpty(selfHostedUrl) ||
                selfHostedUrl.contains("wordpress.com");
    }

    private boolean checkNetworkConnectivity() {
        if (!NetworkUtils.isNetworkAvailable(getActivity())) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            SignInDialogFragment nuxAlert;
            nuxAlert = SignInDialogFragment.newInstance
                    (getString(R.string.no_network_title),
                    getString(R.string.no_network_message),
                    R.drawable.noticon_alert_big,
                    getString(R.string.cancel));
            ft.add(nuxAlert, "alert");
            ft.commitAllowingStateLoss();
            return false;
        }
        return true;
    }
    private void setTwoStepAuthVisibility(boolean isVisible) {
        mTwoStepLayout.setVisibility(isVisible ? View.VISIBLE : View.GONE);
        mTwoStepFooter.setVisibility(isVisible ? View.VISIBLE : View.GONE);
        mSignInButton.setText(isVisible ? getString(R.string.verify) : getString(R.string.sign_in));
        mForgotPassword.setVisibility(isVisible ? View.GONE : View.VISIBLE);
        mBottomButtonsLayout.setVisibility(isVisible ? View.GONE : View.VISIBLE);
        mUsernameEditText.setFocusableInTouchMode(!isVisible);
        mUsernameLayout.setAlpha(isVisible ? 0.6f : 1.0f);
        mPasswordEditText.setFocusableInTouchMode(!isVisible);
        mPasswordLayout.setAlpha(isVisible ? 0.6f : 1.0f);

        if (isVisible) {
            mTwoStepEditText.requestFocus();
            mTwoStepEditText.setText("");
        } else {
            mTwoStepEditText.setText("");
            mTwoStepEditText.clearFocus();
        }
    }
}
