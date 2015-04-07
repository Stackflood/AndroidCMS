package com.example.manish.androidcms.util;

import android.app.Application;

import com.example.manish.androidcms.BuildConfig;
import com.helpshift.Helpshift;

import java.util.HashMap;

/**
 * Created by Manish on 4/3/2015.
 */
public class HelpshiftHelper {
    public static void init(Application application) {
        HashMap<String, Boolean> config = new HashMap<String, Boolean>();
        config.put("enableInAppNotification", false);
        Helpshift.install(application, BuildConfig.HELPSHIFT_API_KEY, BuildConfig.HELPSHIFT_API_DOMAIN,
                BuildConfig.HELPSHIFT_API_ID, config);
    }

}
