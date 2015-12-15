package com.example.manish.androidcms.ui.prefs;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import com.example.manish.androidcms.CMS;
import com.example.manish.androidcms.R;
import com.example.manish.androidcms.widgets.WPEditTextPreference;

import org.wordpress.android.util.ActivityUtils;
import org.wordpress.passcodelock.AppLockManager;

import java.util.List;
import java.util.Map;

/**
 * Created by Manish on 4/1/2015.
 */
@SuppressWarnings("deprecation")
public class SettingsFragment extends PreferenceFragment {

    public static final String SETTINGS_PREFERENCES = "settings-pref";

    private AlertDialog mDialog;
    private SharedPreferences mSettings;

    private WPEditTextPreference mTaglineTextPreference;

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(R.string.settings);

        // the set of blogs may have changed while we were away
        //updateSelfHostedBlogsPreferenceCategory();
        //refreshWPComAuthCategory();

        //update Passcode lock row if available
        if (AppLockManager.getInstance().isAppLockFeatureEnabled()) {
            CheckBoxPreference passcodeEnabledCheckBoxPreference = (CheckBoxPreference) findPreference(getResources().getString(R.string.pref_key_passlock));
            if (AppLockManager.getInstance().getCurrentAppLock().isPasswordLocked()) {
                passcodeEnabledCheckBoxPreference.setChecked(true);
            } else {
                passcodeEnabledCheckBoxPreference.setChecked(false);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Resources resources = getResources();

        if (savedInstanceState == null) {
           // AnalyticsTracker.track(AnalyticsTracker.Stat.OPENED_SETTINGS);
        }

        addPreferencesFromResource(R.xml.settings);
        Preference.OnPreferenceChangeListener preferenceChangeListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue != null) { // cancelled dismiss keyboard
                    preference.setSummary(newValue.toString());
                }
                ActivityUtils.hideKeyboard(getActivity());
                return true;
            }
        };

        mTaglineTextPreference = (WPEditTextPreference) findPreference(resources.getString(R.string.pref_key_post_sig));
        if (mTaglineTextPreference != null) {
            mTaglineTextPreference.setOnPreferenceChangeListener(preferenceChangeListener);
        }
       /* findPreference(resources.getString(R.string.pref_key_notifications)).setOnPreferenceClickListener(notificationPreferenceClickListener);
        findPreference(resources.getString(R.string.pref_key_language)).setOnPreferenceClickListener(languagePreferenceClickListener);
        findPreference(resources.getString(R.string.pref_key_app_about)).setOnPreferenceClickListener(launchActivitiyClickListener);
        findPreference(resources.getString(R.string.pref_key_oss_licenses)).setOnPreferenceClickListener(launchActivitiyClickListener);
        findPreference(resources.getString(R.string.pref_key_help_and_support)).setOnPreferenceClickListener(launchActivitiyClickListener);
        findPreference(resources.getString(R.string.pref_key_passlock)).setOnPreferenceChangeListener(passcodeCheckboxChangeListener);*/
        findPreference(resources.getString(R.string.pref_key_signout)).setOnPreferenceClickListener(signOutPreferenceClickListener);
        //findPreference(resources.getString(R.string.pref_key_reset_shared_pref)).setOnPreferenceClickListener(resetAUtoSharePreferenceClickListener);

        mSettings = PreferenceManager.getDefaultSharedPreferences(getActivity());

       // initNotifications();

        // Passcode Lock not supported
        if (AppLockManager.getInstance().isAppLockFeatureEnabled()) {
            final CheckBoxPreference passcodeEnabledCheckBoxPreference = (CheckBoxPreference) findPreference(
                    resources.getString(R.string.pref_key_passlock));
            // disable on-click changes on the property
            passcodeEnabledCheckBoxPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    passcodeEnabledCheckBoxPreference.setChecked(
                            AppLockManager.getInstance().getCurrentAppLock().isPasswordLocked());
                    return false;
                }
            });
        } else {
            PreferenceScreen rootScreen = (PreferenceScreen) findPreference(resources.getString(R.string.pref_key_settings_root));
            PreferenceGroup passcodeGroup = (PreferenceGroup) findPreference(resources.getString(R.string.pref_key_passlock_section));
            rootScreen.removePreference(passcodeGroup);
        }
        //displayPreferences();
    }

   /* public void refreshWPComAuthCategory() {
        PreferenceCategory wpComCategory = (PreferenceCategory) findPreference(getActivity().getString(R.string.pref_key_wpcom));
        wpComCategory.removeAll();
        addWpComSignIn(wpComCategory, 0);
        addWpComShowHideButton(wpComCategory, 5);
        List<Map<String, Object>> accounts = WordPress.wpDB.getAccountsBy("dotcomFlag = 1 AND isHidden = 0", null);
        addAccounts(wpComCategory, accounts, 10);
    }*/

    private final Preference.OnPreferenceClickListener signOutPreferenceClickListener = new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
            dialogBuilder.setMessage(getString(R.string.sign_out_confirm));
            dialogBuilder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // set the result code so caller knows the user signed out
                    getActivity().setResult(SettingsActivity.RESULT_SIGNED_OUT);
                    CMS.signOutAsyncWithProgressBar(getActivity(), new CMS.SignOutAsync.SignOutCallback() {
                        @Override
                        public void onSignOut() {
                            getActivity().finish();
                        }
                    });
                }
            });
            dialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // Just close the window.
                }
            });
            dialogBuilder.setCancelable(true);
            dialogBuilder.create().show();
            return true;
        }
    };
}
