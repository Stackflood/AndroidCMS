package com.example.manish.androidcms.ui.comments;

import android.graphics.drawable.Drawable;
import android.text.Spanned;
import android.text.util.Linkify;
import android.widget.TextView;

import com.example.manish.androidcms.CMS;
import com.example.manish.androidcms.R;
import com.example.manish.androidcms.util.Emoticons;

import org.wordpress.android.util.HtmlUtils;
import org.wordpress.android.util.helpers.WPImageGetter;

/**
 * Created by Manish on 10/16/2015.
 */
public class CommentUtils {

    /*
     * displays comment text as html, including retrieving images
     */
    public static void displayHtmlComment(TextView textView, String content, int maxImageSize) {
        if (textView == null) {
            return;
        }

        if (content == null) {
            textView.setText(null);
            return;
        }

        // skip performance hit of html conversion if content doesn't contain html
        if (!content.contains("<") && !content.contains("&")) {
            content = content.trim();
            textView.setText(content);
            // make sure unnamed links are clickable
            if (content.contains("://")) {
                Linkify.addLinks(textView, Linkify.WEB_URLS);
            }
            return;
        }

        // convert emoticons first (otherwise they'll be downloaded)
        content = Emoticons.replaceEmoticonsWithEmoji(content);

        // now convert to HTML with an image getter that enforces a max image size
        final Spanned html;
        if (maxImageSize > 0 && content.contains("<img")) {
            Drawable loading = textView.getContext().getResources().getDrawable(
                    R.drawable.dashicon_format_image_big_grey);
            Drawable failed = textView.getContext().getResources().getDrawable(R.drawable.noticon_warning_big_grey);
            html = HtmlUtils.fromHtml(content, new WPImageGetter(textView, maxImageSize, CMS.imageLoader, loading,
                    failed));
        } else {
            html = HtmlUtils.fromHtml(content);
        }

        // remove extra \n\n added by Html.convert()
        CharSequence source = html;
        int start = 0;
        int end = source.length();
        while (start < end && Character.isWhitespace(source.charAt(start))) {
            start++;
        }
        while (end > start && Character.isWhitespace(source.charAt(end - 1))) {
            end--;
        }

        textView.setText(source.subSequence(start, end));
    }
}
