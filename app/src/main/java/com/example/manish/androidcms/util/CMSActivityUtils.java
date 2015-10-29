package com.example.manish.androidcms.util;

import android.content.Context;
import android.content.Intent;
import android.graphics.Point;

import com.example.manish.androidcms.CMS;
import com.example.manish.androidcms.ui.ActivityId;
import com.example.manish.androidcms.ui.comments.CommentsActivity;
import com.example.manish.androidcms.ui.notifications.NotificationsActivity;
import com.example.manish.androidcms.ui.posts.PostsActivity;
import org.wordpress.android.util.DisplayUtils;
/**
 * Created by Manish on 4/1/2015.
 */
public class CMSActivityUtils {


    public static Intent getIntentForActivityId
            (Context context, ActivityId id)
    {
        final Intent intent;

        switch (id)
        {
            case COMMENTS:
                if (CMS.getCurrentBlog() == null)
                {
                    return null;
                }
                intent = new Intent(context, CommentsActivity.class);
                intent.putExtra("id", CMS.getCurrentBlog().getLocalTableBlogId());
                break;
            case POSTS :
                intent = new Intent(context, PostsActivity.class);
                break;
            case NOTIFICATIONS:
                intent = new Intent(context, NotificationsActivity.class);
                break;
            default:
                intent = null;
                break;
        }
        return intent;
    }

    /*
     * returns the optimal pixel width to use for the menu drawer based on:
     * http://www.google.com/design/spec/layout/structure.html#structure-side-nav
     * http://www.google.com/design/spec/patterns/navigation-drawer.html
     * http://android-developers.blogspot.co.uk/2014/10/material-design-on-android-checklist.html
     * https://medium.com/sebs-top-tips/material-navigation-drawer-sizing-558aea1ad266
     */
    public static int getOptimalDrawerWidth(Context context) {
        Point displaySize = DisplayUtils.getDisplayPixelSize(context);
        int appBarHeight = DisplayUtils.getActionBarHeight(context);
        int drawerWidth = Math.min(displaySize.x, displaySize.y) - appBarHeight;
        int maxDp = (DisplayUtils.isXLarge(context) ? 400 : 320);
        int maxPx = DisplayUtils.dpToPx(context, maxDp);
        return Math.min(drawerWidth, maxPx);
    }
}
