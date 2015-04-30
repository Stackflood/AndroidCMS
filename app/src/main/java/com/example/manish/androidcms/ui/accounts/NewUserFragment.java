package com.example.manish.androidcms.ui.accounts;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.SyncStateContract;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.manish.androidcms.Constants;
import com.example.manish.androidcms.R;
import com.example.manish.androidcms.ui.accounts.helpers.CreateUserAndBlog;
import com.example.manish.androidcms.widgets.CMSTextView;

import org.json.JSONObject;
import org.wordpress.android.util.AlertUtils;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.UserEmailUtils;
import org.wordpress.persistentedittext.PersistentEditTextHelper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import NetWorking.RestClientUtils;

/**
 * Created by Manish on 4/8/2015.
 */
public class NewUserFragment extends AbstractFragment implements TextWatcher {

    private EditText mSiteUrlTextField;
    protected static RestClientUtils mRestClientUtils;
    private EditText mEmailTextField;
    private EditText mPasswordTextField;
    private EditText mUsernameTextField;
    private CMSTextView mSignupButton;
    private CMSTextView mProgressTextSignIn;
    private RelativeLayout mProgressBarSignIn;
    //private EmailChecker mEmailChecker;
    private boolean mEmailAutoCorrected;
    private boolean mAutoCompleteUrl;

    @Override
    protected void onDoneAction() {

    }

    @Override
    protected boolean isUserDataValid() {
        // try to create the user
        final String email = EditTextUtils.getText(mEmailTextField).trim();
        final String password = EditTextUtils.getText(mPasswordTextField).trim();
        final String username = EditTextUtils.getText(mUsernameTextField).trim();
        final String siteUrl = EditTextUtils.getText(mSiteUrlTextField).trim();
        boolean retValue = true;

        if (email.equals("")) {
            showEmailError(R.string.required_field);
            retValue = false;
        }

        final Pattern emailRegExPattern = Patterns.EMAIL_ADDRESS;
        Matcher matcher = emailRegExPattern.matcher(email);
        if (!matcher.find() || email.length() > 100) {
            showEmailError(R.string.invalid_email_message);
            retValue = false;
        }

        if (username.equals("")) {
            showUsernameError(R.string.required_field);
            retValue = false;
        }

        if (username.length() < 4) {
            showUsernameError(R.string.invalid_username_too_short);
            retValue = false;
        }

        if (username.length() > 60) {
            showUsernameError(R.string.invalid_username_too_long);
            retValue = false;
        }

        if (siteUrl.length() < 4) {
            showSiteUrlError(R.string.blog_name_must_be_at_least_four_characters);
            retValue = false;
        }

        if (password.equals("")) {
            showPasswordError(R.string.required_field);
            retValue = false;
        }

        if (password.length() < 4) {
            showPasswordError(R.string.invalid_password_message);
            retValue = false;
        }

        return retValue;
    }

    private void showPasswordError(int messageId) {
        mPasswordTextField.setError(getString(messageId));
        mPasswordTextField.requestFocus();
    }

    private void showEmailError(int messageId) {
        mEmailTextField.setError(getString(messageId));
        mEmailTextField.requestFocus();
    }

    private void showUsernameError(int messageId) {
        mUsernameTextField.setError(getString(messageId));
        mUsernameTextField.requestFocus();
    }

    private void showSiteUrlError(int messageId) {
        mSiteUrlTextField.setError(getString(messageId));
        mSiteUrlTextField.requestFocus();
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

   /* private final TextView.OnEditorActionListener mEditorAction = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            return onDoneEvent(actionId, event);
        }
    };*/
    private final View.OnKeyListener mSiteUrlKeyListener = new View.OnKeyListener() {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            mAutoCompleteUrl = EditTextUtils.isEmpty(mSiteUrlTextField);
            return false;
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout containing a title and body text.
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.new_account_user_fragment_screen,
                container, false);

        CMSTextView termsOfServiceTextView = (CMSTextView) rootView.findViewById(R.id.l_agree_terms_of_service);
        termsOfServiceTextView.setText(Html.fromHtml(String.format(getString(R.string.agree_terms_of_service), "<u>",
                "</u>")));
        termsOfServiceTextView.setOnClickListener(new View.OnClickListener() {
                                                      @Override
                                                      public void onClick(View v) {
                                                          Uri uri = Uri.parse(Constants.URL_TOS);
                                                          startActivity(new Intent(Intent.ACTION_VIEW, uri));
                                                      }
                                                  }
        );

        mSignupButton = (CMSTextView) rootView.findViewById(R.id.signup_button);
        mSignupButton.setOnClickListener(mSignupClickListener);
        mSignupButton.setEnabled(true);

        mProgressTextSignIn = (CMSTextView) rootView.findViewById(R.id.nux_sign_in_progress_text);
        mProgressBarSignIn = (RelativeLayout) rootView.findViewById(R.id.nux_sign_in_progress_bar);

        mEmailTextField = (EditText) rootView.findViewById(R.id.email_address);
        mEmailTextField.setText(UserEmailUtils.getPrimaryEmail(getActivity()));
        mEmailTextField.setSelection(EditTextUtils.getText(mEmailTextField).length());
        mPasswordTextField = (EditText) rootView.findViewById(R.id.password);
        mUsernameTextField = (EditText) rootView.findViewById(R.id.username);
        mSiteUrlTextField = (EditText) rootView.findViewById(R.id.site_url);

        mEmailTextField.addTextChangedListener(this);
        mPasswordTextField.addTextChangedListener(this);
        mUsernameTextField.addTextChangedListener(this);
        mSiteUrlTextField.addTextChangedListener(this);
        mSiteUrlTextField.setOnKeyListener(mSiteUrlKeyListener);
        //mSiteUrlTextField.setOnEditorActionListener(mEditorAction);

        mUsernameTextField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // auto fill blog address
                mSiteUrlTextField.setError(null);
                if (mAutoCompleteUrl) {
                    mSiteUrlTextField.setText(EditTextUtils.getText(mUsernameTextField));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        mUsernameTextField.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    mAutoCompleteUrl = EditTextUtils.getText(mUsernameTextField)
                            .equals(EditTextUtils.getText(mSiteUrlTextField))
                            || EditTextUtils.isEmpty(mSiteUrlTextField);
                }
            }
        });

        mEmailTextField.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    autocorrectEmail();
                }
            }
        });
        //initPasswordVisibilityButton(rootView, mPasswordTextField);
        //initInfoButton(rootView);
        return rootView;
    }


    private void autocorrectEmail() {
        if (mEmailAutoCorrected) {
            return;
        }
        final String email = EditTextUtils.getText(mEmailTextField).trim();
        //String suggest = mEmailChecker.suggestDomainCorrection(email);
        /*if (suggest.compareTo(email) != 0) {
            mEmailAutoCorrected = true;
            mEmailTextField.setText(suggest);
            mEmailTextField.setSelection(suggest.length());
        }*/
    }

    private final View.OnClickListener mSignupClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            validateAndCreateUserAndBlog();
        }
    };

    protected RestClientUtils getRestClientUtils() {
        if (mRestClientUtils == null) {
            mRestClientUtils = new RestClientUtils(requestQueue, null, null);
        }
        return mRestClientUtils;
    }

    private void validateAndCreateUserAndBlog() {
        if (mSystemService.getActiveNetworkInfo() == null) {
            AlertUtils.showAlert(getActivity(),
                    R.string.no_network_title, R.string.no_network_message);
            return;
        }
        if (!isUserDataValid()) {
            return;
        }

        // Prevent double tapping of the "done" btn in keyboard for those clients that don't dismiss the keyboard.
        // Samsung S4 for example
        if (View.VISIBLE == mProgressBarSignIn.getVisibility()) {
            return;
        }

        startProgress(getString(R.string.validating_user_data));

        final String siteUrl = EditTextUtils.getText(mSiteUrlTextField).trim();
        final String email = EditTextUtils.getText(mEmailTextField).trim();
        final String password = EditTextUtils.getText(mPasswordTextField).trim();
        final String username = EditTextUtils.getText(mUsernameTextField).trim();
        final String siteName = siteUrlToSiteName(siteUrl);
        final String language = CreateUserAndBlog.getDeviceLanguage(getActivity().getResources());


        CreateUserAndBlog createUserAndBlog = new CreateUserAndBlog(
                email,
                username,
                password,
                siteUrl,
                siteName,
                language,
                getRestClientUtils(),
                getActivity(),
                new ErrorListener(),
                new CreateUserAndBlog.Callback() {
                    @Override
                    public void onStepFinished(CreateUserAndBlog.Step step) {
                        if (!isAdded()) {
                            return;
                        }
                        switch (step) {
                            case VALIDATE_USER:
                                updateProgress(getString(R.string.validating_site_data));
                                break;
                            case VALIDATE_SITE:
                                updateProgress(getString(R.string.creating_your_account));
                                break;
                            case CREATE_USER:
                                updateProgress(getString(R.string.creating_your_site));
                                break;
                            case CREATE_SITE:
                                // no messages
                            case AUTHENTICATE_USER:
                            default:
                                break;
                        }
                    }

                    @Override
                    public void onSuccess(JSONObject createSiteResponse) {
                        //AnalyticsUtils.refreshMetadata(username, email);
                        //AnalyticsTracker.track(AnalyticsTracker.Stat.CREATED_ACCOUNT);
                        endProgress();
                        if (isAdded()) {
                            finishThisStuff(username, password);
                        }
                    }

                    @Override
                    public void onError(int messageId) {
                        endProgress();
                        if (isAdded()) {
                            showError(getString(messageId));
                        }
                    }
                }
        );
        createUserAndBlog.startCreateUserAndBlogProcess();
    }

    private String siteUrlToSiteName(String siteUrl) {
        return siteUrl;
    }

    private void finishThisStuff(String username, String password) {
        final Activity activity = getActivity();
        if (activity != null) {
            Intent intent = new Intent();
            intent.putExtra("username", username);
            intent.putExtra("password", password);
            activity.setResult(NewAccountActivity.RESULT_OK, intent);
            activity.finish();
            PersistentEditTextHelper persistentEditTextHelper = new PersistentEditTextHelper(getActivity());
            persistentEditTextHelper.clearSavedText(mEmailTextField, null);
            persistentEditTextHelper.clearSavedText(mUsernameTextField, null);
            persistentEditTextHelper.clearSavedText(mSiteUrlTextField, null);
        }
    }
}
