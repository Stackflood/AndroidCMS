package com.example.manish.androidcms.ui.media;

import android.app.Fragment;

/**
 * Created by Manish on 4/13/2015.
 */
public class MediaGridFragment extends Fragment {

    public enum Filter {
        ALL, IMAGES, UNATTACHED, CUSTOM_DATE;

        public static Filter getFilter(int filterPos) {
            if (filterPos > Filter.values().length)
                return ALL;
            else
                return Filter.values()[filterPos];
        }
    }
}
