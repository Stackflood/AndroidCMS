package com.example.manish.androidcms.ui.stats;

import android.os.Bundle;
import android.widget.Toast;

import com.example.manish.androidcms.CMS;
import com.example.manish.androidcms.R;
import com.example.manish.androidcms.ui.CMSDrawerActivity;

/**
 * The native stats activity, accessible via the menu drawer.
 * <p>
 * By pressing a spinner on the action bar, the user can select which timeframe they wish to see.
 * </p>
 */
public class StatsActivity extends CMSDrawerActivity {

    public static final String ARG_NO_MENU_DRAWER = "no_menu_drawer";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (CMS.cmsDB == null) {
            Toast.makeText(this, R.string.fatal_db_error, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
    }
}
