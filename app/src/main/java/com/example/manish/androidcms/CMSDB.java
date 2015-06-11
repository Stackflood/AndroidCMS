package com.example.manish.androidcms;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Base64;

import com.example.manish.androidcms.datasets.CommentTable;
import com.example.manish.androidcms.datasets.SuggestionTable;
import com.example.manish.androidcms.models.Blog;
import com.example.manish.androidcms.models.Post;
import com.example.manish.androidcms.models.PostLocation;
import com.example.manish.androidcms.models.PostsListPost;
import com.example.manish.androidcms.networking.OAuthAuthenticator;

import NetWorking.RestClientUtils;

import org.json.JSONArray;
import org.wordpress.android.util.MapUtils;
import org.wordpress.android.util.SqlUtils;

import org.apache.commons.lang.ArrayUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.helpers.MediaFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;


//import  commons

/**
 * Created by Manish on 4/1/2015.
 */
public class CMSDB {

    public static final String COLUMN_NAME_POST_ID               = "postID";
    public static final String COLUMN_NAME_FILE_PATH             = "filePath";
    public static final String COLUMN_NAME_FILE_NAME             = "fileName";
    public static final String COLUMN_NAME_TITLE                 = "title";
    public static final String COLUMN_NAME_DESCRIPTION           = "description";
    public static final String COLUMN_NAME_CAPTION               = "caption";
    public static final String COLUMN_NAME_HORIZONTAL_ALIGNMENT  = "horizontalAlignment";
    public static final String COLUMN_NAME_WIDTH                 = "width";
    public static final String COLUMN_NAME_HEIGHT                = "height";
    public static final String COLUMN_NAME_MIME_TYPE             = "mimeType";
    public static final String COLUMN_NAME_FEATURED              = "featured";
    public static final String COLUMN_NAME_IS_VIDEO              = "isVideo";
    public static final String COLUMN_NAME_IS_FEATURED_IN_POST   = "isFeaturedInPost";
    public static final String COLUMN_NAME_FILE_URL              = "fileURL";
    public static final String COLUMN_NAME_THUMBNAIL_URL         = "thumbnailURL";
    public static final String COLUMN_NAME_MEDIA_ID              = "mediaId";
    public static final String COLUMN_NAME_BLOG_ID               = "blogId";
    public static final String COLUMN_NAME_DATE_CREATED_GMT      = "date_created_gmt";
    public static final String COLUMN_NAME_VIDEO_PRESS_SHORTCODE = "videoPressShortcode";
    public static final String COLUMN_NAME_UPLOAD_STATE          = "uploadState";
    // add hidden flag to blog settings (accounts)
    private static final String ADD_ACCOUNTS_HIDDEN_FLAG = "alter table accounts add isHidden boolean default 0;";
    private SQLiteDatabase db;
    private Context context;
    /*public static RestClientUtils mRestClientUtils;
    public static RestClientUtils mRestClientUtilsVersion1_1;*/
    private static final String DATABASE_NAME = "CMS";
    private static final String CREATE_TABLE_SETTINGS = "create table if not exists accounts (id integer primary key autoincrement, "
            + "url text, blogName text, username text, password text, imagePlacement text, centerThumbnail boolean, fullSizeImage boolean, maxImageWidth text, maxImageWidthId integer);";

    public static final String SETTINGS_TABLE = "accounts";
    private static final String MEDIA_TABLE = "media";
    private static final String CREATE_TABLE_POSTS = "create table if not exists posts (id integer primary key autoincrement, blogID text, "
            + "postid text, title text default '', dateCreated date, date_created_gmt date, categories text default '', custom_fields text default '', "
            + "description text default '', link text default '', mt_allow_comments boolean, mt_allow_pings boolean, "
            + "mt_excerpt text default '', mt_keywords text default '', mt_text_more text default '', permaLink text default '', post_status text default '', userid integer default 0, "
            + "wp_author_display_name text default '', wp_author_id text default '', wp_password text default '', wp_post_format text default '', wp_slug text default '', mediaPaths text default '', "
            + "latitude real, longitude real, localDraft boolean default 0, uploaded boolean default 0, isPage boolean default 0, wp_page_parent_id text, wp_page_parent_title text);";

    private static final String CREATE_TABLE_THEMES = "create table if not exists themes (_id integer primary key autoincrement, "
            + "themeId text, name text, description text, screenshotURL text, trendingRank integer default 0, popularityRank integer default 0, launchDate date, previewURL text, blogId text, isCurrent boolean default false, isPremium boolean default false, features text);";

    private static final String POSTS_TABLE = "posts";

    // categories
    private static final String CREATE_TABLE_CATEGORIES = "create table if not exists cats (id integer primary key autoincrement, "
            + "blog_id text, wp_id integer, category_name text not null);";
    private static final String CATEGORIES_TABLE = "cats";
    protected static final String PASSWORD_SECRET = BuildConfig.DB_SECRET;

    // for capturing blogID
    private static final String ADD_BLOGID = "alter table accounts add blogId integer;";
    private static final String UPDATE_BLOGID = "update accounts set blogId = 1;";
    private static final String ADD_LOCATION_FLAG = "alter table accounts add location boolean default false;";
    private static final String ADD_DOTCOM_USERNAME = "alter table accounts add dotcom_username text;";
    private static final String ADD_DOTCOM_PASSWORD = "alter table accounts add dotcom_password text;";
    private static final String ADD_API_KEY = "alter table accounts add api_key text;";
    private static final String ADD_API_BLOGID = "alter table accounts add api_blogid text;";
    // add wordpress.com flag and version column
    private static final String ADD_DOTCOM_FLAG = "alter table accounts add dotcomFlag boolean default false;";
    private static final String ADD_WP_VERSION = "alter table accounts add wpVersion text;";


    // add httpuser and httppassword
    private static final String ADD_HTTPUSER = "alter table accounts add httpuser text;";
    private static final String ADD_HTTPPASSWORD = "alter table accounts add httppassword text;";

    // add new table for QuickPress homescreen shortcuts
    private static final String CREATE_TABLE_QUICKPRESS_SHORTCUTS = "create table if not exists quickpress_shortcuts (id integer primary key autoincrement, accountId text, name text);";
    private static final String QUICKPRESS_SHORTCUTS_TABLE = "quickpress_shortcuts";


    // add field to store last used blog
    private static final String ADD_POST_FORMATS = "alter table accounts add postFormats text default '';";


    //add scaled image settings
    private static final String ADD_SCALED_IMAGE = "alter table accounts add isScaledImage boolean default false;";
    private static final String ADD_SCALED_IMAGE_IMG_WIDTH = "alter table accounts add scaledImgWidth integer default 1024;";


    //add boolean to posts to check uploaded posts that have local changes
    private static final String ADD_LOCAL_POST_CHANGES = "alter table posts add isLocalChange boolean default 0";

    private static final String CREATE_TABLE_MEDIA = "create table if not exists media (id integer primary key autoincrement, "
            + "postID integer not null, filePath text default '', fileName text default '', title text default '', description text default '', caption text default '', horizontalAlignment integer default 0, width integer default 0, height integer default 0, mimeType text default '', featured boolean default false, isVideo boolean default false);";

    private static final int DATABASE_VERSION = 29;


    public boolean isBlogInDatabase(int blogId, String xmlRpcUrl) {
        Cursor c = db.query(SETTINGS_TABLE, new String[]{"id"}, "blogId=? AND url=?",
                new String[]{Integer.toString(blogId), xmlRpcUrl}, null, null, null, null);
        boolean result =  c.getCount() > 0;
        c.close();
        return result;
    }

    public int instantiateBlogByLocalId(int remoteBlogId, String xmlRpcUrl) {
        int localBlogID = SqlUtils.intForQuery(db, "SELECT id FROM accounts WHERE blogId=? AND url=?",
                new String[]{Integer.toString(remoteBlogId), xmlRpcUrl});
        if (localBlogID==0) {
            localBlogID = this.getLocalTableBlogIdForJetpackRemoteID(remoteBlogId, xmlRpcUrl);
        }
        return localBlogID;
    }

    public void deleteMediaFilesForPost(Post post) {
        db.delete(MEDIA_TABLE, "blogId='" + post.getLocalTableBlogId() +
                "' AND postID=" + post.getLocalTablePostId(), null);
    }

    /** For a given blogId, get the media file with the given media_id **/
    public Cursor getMediaFile(String blogId, String mediaId) {
        return db.rawQuery("SELECT * FROM " +
                MEDIA_TABLE + " WHERE blogId=? AND mediaId=?", new String[] { blogId, mediaId });
    }

    public boolean addBlog(Blog blog) {
        ContentValues values = new ContentValues();
        values.put("url", blog.getUrl());
        values.put("homeURL", blog.getHomeURL());
        values.put("blogName", blog.getBlogName());
        values.put("username", blog.getUsername());
        values.put("password", encryptPassword(blog.getPassword()));
        values.put("httpuser", blog.getHttpuser());
        values.put("httppassword", encryptPassword(blog.getHttppassword()));
        values.put("imagePlacement", blog.getImagePlacement());
        values.put("centerThumbnail", false);
        values.put("fullSizeImage", false);
        values.put("maxImageWidth", blog.getMaxImageWidth());
        values.put("maxImageWidthId", blog.getMaxImageWidthId());
        values.put("blogId", blog.getRemoteBlogId());
        values.put("dotcomFlag", blog.isDotcomFlag());
        if (blog.getWpVersion() != null) {
            values.put("wpVersion", blog.getWpVersion());
        } else {
            values.putNull("wpVersion");
        }
        values.put("isAdmin", blog.isAdmin());
        values.put("isHidden", blog.isHidden());
        return db.insert(SETTINGS_TABLE, null, values) > -1;
    }
    /**
     * Saves a list of posts to the db
     * @param postsList: list of post objects
     * @param localBlogId: the posts table blog id
     * @param isPage: boolean to save as pages
     */
    public void savePosts(List<?> postsList, int localBlogId, boolean isPage, boolean shouldOverwrite) {
        if (postsList != null && postsList.size() != 0) {
            db.beginTransaction();
            try {
                for (Object post : postsList) {
                    ContentValues values = new ContentValues();

                    // Sanity checks
                    if (!(post instanceof Map)) {
                        continue;
                    }
                    Map<?, ?> postMap = (Map<?, ?>) post;
                    String postID = MapUtils.getMapStr(postMap, (isPage) ? "page_id" : "postid");
                    if (TextUtils.isEmpty(postID)) {
                        // If we don't have a post or page ID, move on
                        continue;
                    }

                    values.put("blogID", localBlogId);
                    values.put("postid", postID);
                    values.put("title", MapUtils.getMapStr(postMap, "title"));
                    Date dateCreated = MapUtils.getMapDate(postMap, "dateCreated");
                    if (dateCreated != null) {
                        values.put("dateCreated", dateCreated.getTime());
                    } else {
                        Date now = new Date();
                        values.put("dateCreated", now.getTime());
                    }

                    Date dateCreatedGmt = MapUtils.getMapDate(postMap, "date_created_gmt");
                    if (dateCreatedGmt != null) {
                        values.put("date_created_gmt", dateCreatedGmt.getTime());
                    } else {
                        dateCreatedGmt = new Date((Long) values.get("dateCreated"));
                        values.put("date_created_gmt", dateCreatedGmt.getTime() + (dateCreatedGmt.getTimezoneOffset() * 60000));
                    }

                    values.put("description", MapUtils.getMapStr(postMap, "description"));
                    values.put("link", MapUtils.getMapStr(postMap, "link"));
                    values.put("permaLink", MapUtils.getMapStr(postMap, "permaLink"));

                    Object[] postCategories = (Object[]) postMap.get("categories");
                    JSONArray jsonCategoriesArray = new JSONArray();
                    if (postCategories != null) {
                        for (Object postCategory : postCategories) {
                            jsonCategoriesArray.put(postCategory.toString());
                        }
                    }
                    values.put("categories", jsonCategoriesArray.toString());

                    Object[] custom_fields = (Object[]) postMap.get("custom_fields");
                    JSONArray jsonCustomFieldsArray = new JSONArray();
                    if (custom_fields != null) {
                        for (Object custom_field : custom_fields) {
                            jsonCustomFieldsArray.put(custom_field.toString());
                            // Update geo_long and geo_lat from custom fields
                            if (!(custom_field instanceof Map))
                                continue;
                            Map<?, ?> customField = (Map<?, ?>) custom_field;
                            if (customField.get("key") != null && customField.get("value") != null) {
                                if (customField.get("key").equals("geo_longitude"))
                                    values.put("longitude", customField.get("value").toString());
                                if (customField.get("key").equals("geo_latitude"))
                                    values.put("latitude", customField.get("value").toString());
                            }
                        }
                    }
                    values.put("custom_fields", jsonCustomFieldsArray.toString());

                    values.put("mt_excerpt", MapUtils.getMapStr(postMap, (isPage) ? "excerpt" : "mt_excerpt"));
                    values.put("mt_text_more", MapUtils.getMapStr(postMap, (isPage) ? "text_more" : "mt_text_more"));
                    values.put("mt_allow_comments", MapUtils.getMapInt(postMap, "mt_allow_comments", 0));
                    values.put("mt_allow_pings", MapUtils.getMapInt(postMap, "mt_allow_pings", 0));
                    values.put("wp_slug", MapUtils.getMapStr(postMap, "wp_slug"));
                    values.put("wp_password", MapUtils.getMapStr(postMap, "wp_password"));
                    values.put("wp_author_id", MapUtils.getMapStr(postMap, "wp_author_id"));
                    values.put("wp_author_display_name", MapUtils.getMapStr(postMap, "wp_author_display_name"));
                    values.put("post_status", MapUtils.getMapStr(postMap, (isPage) ? "page_status" : "post_status"));
                    values.put("userid", MapUtils.getMapStr(postMap, "userid"));

                    if (isPage) {
                        values.put("isPage", true);
                        values.put("wp_page_parent_id", MapUtils.getMapStr(postMap, "wp_page_parent_id"));
                        values.put("wp_page_parent_title", MapUtils.getMapStr(postMap, "wp_page_parent_title"));
                    } else {
                        values.put("mt_keywords", MapUtils.getMapStr(postMap, "mt_keywords"));
                        values.put("wp_post_format", MapUtils.getMapStr(postMap, "wp_post_format"));
                    }

                    String whereClause = "blogID=? AND postID=? AND isPage=?";
                    if (!shouldOverwrite) {
                        whereClause += " AND NOT isLocalChange=1";
                    }

                    int result = db.update(POSTS_TABLE, values, whereClause,
                            new String[]{String.valueOf(localBlogId), postID, String.valueOf(SqlUtils.boolToSql(isPage))});
                    if (result == 0)
                        db.insert(POSTS_TABLE, null, values);
                }

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }

    public void updateMediaFile(String blogId, String mediaId,
                                String title, String description, String caption) {
        if (blogId == null || blogId.equals("")) {
            return;
        }

        ContentValues values = new ContentValues();

        if (title == null || title.equals("")) {
            values.put("title", "");
        } else {
            values.put("title", title);
        }

        if (title == null || title.equals("")) {
            values.put("description", "");
        } else {
            values.put("description", description);
        }

        if (caption == null || caption.equals("")) {
            values.put("caption", "");
        } else {
            values.put("caption", caption);
        }

        db.update(MEDIA_TABLE, values, "blogId = ? AND mediaId=?", new String[] { blogId, mediaId });
    }


    public List<PostsListPost> getPostsListPosts(int blogId, boolean loadPages) {
        List<PostsListPost> posts = new ArrayList<PostsListPost>();
        Cursor c;
        c = db.query(POSTS_TABLE,
                new String[] { "id", "blogID", "title",
                        "date_created_gmt", "post_status", "isUploading", "localDraft", "isLocalChange" },
                "blogID=? AND isPage=? AND NOT (localDraft=1 AND uploaded=1)",
                new String[] {String.valueOf(blogId), (loadPages) ? "1" : "0"}, null, null, "localDraft DESC, date_created_gmt DESC");
        int numRows = c.getCount();
        c.moveToFirst();

        for (int i = 0; i < numRows; ++i) {
            String postTitle = StringUtils.unescapeHTML(c.getString(c.getColumnIndex("title")));

            // Create the PostsListPost and add it to the Array
            PostsListPost post = new PostsListPost(
                    c.getInt(c.getColumnIndex("id")),
                    c.getInt(c.getColumnIndex("blogID")),
                    postTitle,
                    c.getLong(c.getColumnIndex("date_created_gmt")),
                    c.getString(c.getColumnIndex("post_status")),
                    SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("localDraft"))),
                    SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("isLocalChange"))),
                    SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("isUploading")))
            );
            posts.add(i, post);
            c.moveToNext();
        }
        c.close();

        return posts;
    }

    public boolean findLocalChanges(int blogId, boolean isPage) {
        Cursor c = db.query(POSTS_TABLE, null,
                "isLocalChange=? AND blogID=? AND isPage=?", new String[]{"1", String.valueOf(blogId), (isPage) ? "1" : "0"}, null, null, null);
        int numRows = c.getCount();
        c.close();
        if (numRows > 0) {
            return true;
        }

        return false;
    }

    /** For a given blogId, get all the media files **/
    public Cursor getMediaFilesForBlog(String blogId) {
        return db.rawQuery("SELECT id as _id, * FROM " + MEDIA_TABLE + " WHERE blogId=? AND mediaId <> '' AND "
                + "(uploadState IS NULL OR uploadState IN ('uploaded', 'queued', 'failed', 'uploading')) ORDER BY (uploadState=?) DESC, date_created_gmt DESC", new String[] { blogId, "uploading" });
    }

    public MediaFile getMediaFile(String src, Post post) {
        Cursor c = db.query(MEDIA_TABLE, null, "postID=? AND filePath=?",
                new String[]{String.valueOf(post.getLocalTablePostId()), src}, null, null, null);

        try {
            if (c.moveToFirst()) {
                MediaFile mf = new MediaFile();
                mf.setId(c.getInt(0));
                mf.setPostID(c.getInt(1));
                mf.setFilePath(c.getString(2));
                mf.setFileName(c.getString(3));
                mf.setTitle(c.getString(4));
                mf.setDescription(c.getString(5));
                mf.setCaption(c.getString(6));
                mf.setHorizontalAlignment(c.getInt(7));
                mf.setWidth(c.getInt(8));
                mf.setHeight(c.getInt(9));
                mf.setMimeType(c.getString(10));
                mf.setFeatured(c.getInt(11) > 0);
                mf.setVideo(c.getInt(12) > 0);
                mf.setFeaturedInPost(c.getInt(13) > 0);
                mf.setFileURL(c.getString(14));
                mf.setThumbnailURL(c.getString(15));
                mf.setMediaId(c.getString(16));
                mf.setBlogId(c.getString(17));
                mf.setDateCreatedGMT(c.getLong(18));
                mf.setUploadState(c.getString(19));
                mf.setVideoPressShortCode(c.getString(20));

                return mf;
            } else {
                return null;
            }
        } finally {
            c.close();
        }
    }

    public boolean deletePost(Post post) {
        int result = db.delete(POSTS_TABLE,
                "blogID=? AND id=?",
                new String[]{String.valueOf(post.getLocalTableBlogId()),
                        String.valueOf(post.getLocalTablePostId())});

        return (result == 1);
    }

    public void saveMediaFile(MediaFile mf)
    {
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME_POST_ID, mf.getPostID());
        values.put(COLUMN_NAME_FILE_PATH, mf.getFilePath());
        values.put(COLUMN_NAME_FILE_NAME, mf.getFileName());
        values.put(COLUMN_NAME_TITLE, mf.getTitle());
        values.put(COLUMN_NAME_DESCRIPTION, mf.getDescription());
        values.put(COLUMN_NAME_CAPTION, mf.getCaption());
        values.put(COLUMN_NAME_HORIZONTAL_ALIGNMENT, mf.getHorizontalAlignment());
        values.put(COLUMN_NAME_WIDTH, mf.getWidth());
        values.put(COLUMN_NAME_HEIGHT, mf.getHeight());
        values.put(COLUMN_NAME_MIME_TYPE, mf.getMimeType());
        values.put(COLUMN_NAME_FEATURED, mf.isFeatured());
        values.put(COLUMN_NAME_IS_VIDEO, mf.isVideo());
        values.put(COLUMN_NAME_IS_FEATURED_IN_POST, mf.isFeaturedInPost());
        values.put(COLUMN_NAME_FILE_URL, mf.getFileURL());
        values.put(COLUMN_NAME_THUMBNAIL_URL, mf.getThumbnailURL());
        values.put(COLUMN_NAME_MEDIA_ID, mf.getMediaId());
        values.put(COLUMN_NAME_BLOG_ID, mf.getBlogId());
        values.put(COLUMN_NAME_DATE_CREATED_GMT, mf.getDateCreatedGMT());
        values.put(COLUMN_NAME_VIDEO_PRESS_SHORTCODE, mf.getVideoPressShortCode());
        if (mf.getUploadState() != null)
            values.put(COLUMN_NAME_UPLOAD_STATE, mf.getUploadState());
        else
            values.putNull(COLUMN_NAME_UPLOAD_STATE);

        synchronized (this) {
            int result = 0;
            boolean isMarkedForDelete = false;
            if (mf.getMediaId() != null) {
                Cursor cursor = db.rawQuery("SELECT uploadState FROM " +
                                MEDIA_TABLE + " WHERE mediaId=?",
                        new String[]{StringUtils.notNullStr(mf.getMediaId())});
                if (cursor != null && cursor.moveToFirst()) {
                    isMarkedForDelete = "delete".equals(cursor.getString(0));
                    cursor.close();
                }

                if (!isMarkedForDelete)
                    result = db.update(MEDIA_TABLE, values, "blogId=? AND mediaId=?",
                            new String[]{StringUtils.notNullStr(mf.getBlogId()), StringUtils.notNullStr(mf.getMediaId())});
            }

            if (result == 0 && !isMarkedForDelete) {
                result = db.update(MEDIA_TABLE, values, "postID=? AND filePath=?",
                        new String[]{String.valueOf(mf.getPostID()), StringUtils.notNullStr(mf.getFilePath())});
                if (result == 0)
                    db.insert(MEDIA_TABLE,null, values);
            }
        }


    }

    public long savePost(Post post) {
        long result = -1;
        if (post != null) {
            ContentValues values = new ContentValues();
            values.put("blogID", post.getLocalTableBlogId());
            values.put("title", post.getTitle());
            values.put("date_created_gmt", post.getDate_created_gmt());
            values.put("description", post.getDescription());
            values.put("mt_text_more", post.getMoreText());

            JSONArray categoriesJsonArray = post.getJSONCategories();
            if (categoriesJsonArray != null) {
                values.put("categories", categoriesJsonArray.toString());
            }

            values.put("localDraft", post.isLocalDraft());
            values.put("mt_keywords", post.getKeywords());
            values.put("wp_password", post.getPassword());
            values.put("post_status", post.getPostStatus());
            values.put("isUploading", post.isUploading());
            values.put("uploaded", post.isUploaded());
            values.put("isPage", post.isPage());
            values.put("wp_post_format", post.getPostFormat());
            putPostLocation(post, values);
            values.put("isLocalChange", post.isLocalChange());
            values.put("mt_excerpt", post.getPostExcerpt());

            result = db.insert(POSTS_TABLE, null, values);

            if (result >= 0 && post.isLocalDraft() && !post.isUploaded()) {
                post.setLocalTablePostId(result);
            }
        }

        return (result);
    }

    public Cursor getMediaImagesForBlog(String blogId) {
        return db.rawQuery("SELECT id as _id, * FROM " +
                        MEDIA_TABLE + " WHERE blogId=? AND mediaId <> '' AND "
                        + "(uploadState IS NULL OR uploadState IN " +
                        "('uploaded', 'queued', 'failed', 'uploading')) " +
                        "AND mimeType LIKE ? ORDER BY " +
                        "(uploadState=?) DESC, date_created_gmt DESC",
                new String[] { blogId, "image%", "uploading" });
    }

    public int updatePost(Post post) {
        int result = 0;
        if (post != null) {
            ContentValues values = new ContentValues();
            values.put("title", post.getTitle());
            values.put("date_created_gmt", post.getDate_created_gmt());
            values.put("description", post.getDescription());
            values.put("mt_text_more", post.getMoreText());
            values.put("isUploading", post.isUploading());
            values.put("uploaded", post.isUploaded());

            JSONArray categoriesJsonArray = post.getJSONCategories();
            if (categoriesJsonArray != null) {
                values.put("categories", categoriesJsonArray.toString());
            }

            values.put("localDraft", post.isLocalDraft());
            values.put("mediaPaths", post.getMediaPaths());
            values.put("mt_keywords", post.getKeywords());
            values.put("wp_password", post.getPassword());
            values.put("post_status", post.getPostStatus());
            values.put("isPage", post.isPage());
            values.put("wp_post_format", post.getPostFormat());
            values.put("isLocalChange", post.isLocalChange());
            values.put("mt_excerpt", post.getPostExcerpt());
            putPostLocation(post, values);

            result = db.update(POSTS_TABLE, values,
                    "blogID=? AND id=? AND isPage=?",
                    new String[]{
                            String.valueOf(post.getLocalTableBlogId()),
                            String.valueOf(post.getLocalTablePostId()),
                            String.valueOf(SqlUtils.boolToSql(post.isPage()))
                    });
        }

        return (result);
    }

    private void putPostLocation(Post post, ContentValues values) {
        if (post.hasLocation()) {
            PostLocation location = post.getLocation();
            values.put("latitude", location.getLatitude());
            values.put("longitude", location.getLongitude());
        } else {
            values.putNull("latitude");
            values.putNull("longitude");
        }
    }

    public void deleteUploadedPosts(int blogID, boolean isPage) {
        if (isPage)
            db.delete(POSTS_TABLE, "blogID=" + blogID
                    + " AND localDraft != 1 AND isPage=1", null);
        else
            db.delete(POSTS_TABLE, "blogID=" + blogID
                    + " AND localDraft != 1 AND isPage=0", null);

    }

    public static String encryptPassword(String clearText) {
        try {
            DESKeySpec keySpec = new DESKeySpec(
                    PASSWORD_SECRET.getBytes("UTF-8"));
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey key = keyFactory.generateSecret(keySpec);

            Cipher cipher = Cipher.getInstance("DES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            String encrypedPwd = Base64.encodeToString(cipher.doFinal(clearText
                    .getBytes("UTF-8")), Base64.DEFAULT);
            return encrypedPwd;
        } catch (Exception e) {
        }
        return clearText;
    }

    public boolean saveBlog(Blog blog) {
        if (blog.getLocalTableBlogId() == -1) {
            return addBlog(blog);
        }

        ContentValues values = new ContentValues();
        values.put("url", blog.getUrl());
        values.put("homeURL", blog.getHomeURL());
        values.put("username", blog.getUsername());
        values.put("password", encryptPassword(blog.getPassword()));
        values.put("httpuser", blog.getHttpuser());
        values.put("httppassword", encryptPassword(blog.getHttppassword()));
        values.put("imagePlacement", blog.getImagePlacement());
        values.put("centerThumbnail", blog.isFeaturedImageCapable());
        values.put("fullSizeImage", blog.isFullSizeImage());
        values.put("maxImageWidth", blog.getMaxImageWidth());
        values.put("maxImageWidthId", blog.getMaxImageWidthId());
        values.put("postFormats", blog.getPostFormats());
        values.put("dotcom_username", blog.getDotcom_username());
        values.put("dotcom_password", encryptPassword(blog.getDotcom_password()));
        values.put("api_blogid", blog.getApi_blogid());
        values.put("api_key", blog.getApi_key());
        values.put("isScaledImage", blog.isScaledImage());
        values.put("scaledImgWidth", blog.getScaledImageWidth());
        values.put("blog_options", blog.getBlogOptions());
        values.put("isHidden", blog.isHidden());
        values.put("blogName", blog.getBlogName());
        values.put("isAdmin", blog.isAdmin());
        values.put("isHidden", blog.isHidden());
        if (blog.getWpVersion() != null) {
            values.put("wpVersion", blog.getWpVersion());
        } else {
            values.putNull("wpVersion");
        }
        boolean returnValue = db.update(SETTINGS_TABLE, values, "id=" + blog.getLocalTableBlogId(),
                null) > 0;
        if (blog.isDotcomFlag()) {
            returnValue = updateWPComCredentials(blog.getUsername(), blog.getPassword());
        }

        return (returnValue);
    }

    public boolean updateWPComCredentials(String username, String password) {
        // update the login for wordpress.com blogs
        ContentValues userPass = new ContentValues();
        userPass.put("username", username);
        userPass.put("password", encryptPassword(password));
        return db.update(SETTINGS_TABLE, userPass, "username=\""
                + username + "\" AND dotcomFlag=1", null) > 0;
    }
    /*
     * Jetpack blogs have the "wpcom" blog_id stored in options->api_blogid. This is because self-hosted blogs have both
     * a blogID (local to their network), and a unique blogID on wpcom.
     */
    private int getLocalTableBlogIdForJetpackRemoteID(int remoteBlogId, String xmlRpcUrl) {
        if (TextUtils.isEmpty(xmlRpcUrl)) {
            String sql = "SELECT id FROM " + SETTINGS_TABLE + " WHERE dotcomFlag=0 AND api_blogid=?";
            String[] args = {Integer.toString(remoteBlogId)};
            return SqlUtils.intForQuery(db, sql, args);
        } else {
            String sql = "SELECT id FROM " + SETTINGS_TABLE + " WHERE dotcomFlag=0 AND api_blogid=? AND url=?";
            String[] args = {Integer.toString(remoteBlogId), xmlRpcUrl};
            return SqlUtils.intForQuery(db, sql, args);
        }
    }
    public Blog getBlogForDotComBlogId(String dotComBlogId) {
        Cursor c = db.query(SETTINGS_TABLE, new String[]{"id"}, "api_blogid=? OR (blogId=? AND dotcomFlag=1)",
                new String[]{dotComBlogId, dotComBlogId}, null, null, null);
        Blog blog = null;
        if (c.moveToFirst()) {
            blog = instantiateBlogByLocalId(c.getInt(0));
        }
        c.close();
        return blog;
    }

    public void deleteAllAccounts() {
        List<Integer> ids = getAllAccountIDs();
        if (ids.size() == 0)
            return;

        db.beginTransaction();
        try {
            for (int id: ids) {
                deleteAccount(context, id);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public boolean deleteAccount(Context ctx, int id) {
        // TODO: should this also delete posts and other related info?
        int rowsAffected = db.delete(SETTINGS_TABLE, "id=?", new String[]{Integer.toString(id)});
        //deleteQuickPressShortcutsForAccount(ctx, id);
        return (rowsAffected > 0);
    }
    /**
     * Instantiate a new Blog object from it's local id
     *
     * @param localId local blog id
     * @return a new Blog instance or null if the localId was not found
     */
    public Blog instantiateBlogByLocalId(int localId) {
        String[] fields =
                new String[]{"url", "blogName", "username", "password", "httpuser", "httppassword", "imagePlacement",
                        "centerThumbnail", "fullSizeImage", "maxImageWidth", "maxImageWidthId",
                        "blogId", "dotcomFlag", "dotcom_username", "dotcom_password", "api_key",
                        "api_blogid", "wpVersion", "postFormats", "isScaledImage",
                        "scaledImgWidth", "homeURL", "blog_options", "isAdmin", "isHidden"};
        Cursor c = db.query(SETTINGS_TABLE, fields,
                "id=?", new String[]{Integer.toString(localId)}, null, null, null);

        Blog blog = null;
        if (c.moveToFirst()) {
            if (c.getString(0) != null) {
                blog = new Blog();
                blog.setLocalTableBlogId(localId);
                blog.setUrl(c.getString(c.getColumnIndex("url"))); // 0

                blog.setBlogName(c.getString(c.getColumnIndex("blogName"))); // 1
                blog.setUsername(c.getString(c.getColumnIndex("username"))); // 2
                blog.setPassword(decryptPassword(c.getString(c.getColumnIndex("password")))); // 3
                if (c.getString(c.getColumnIndex("httpuser")) == null) {
                    blog.setHttpuser("");
                } else {
                    blog.setHttpuser(c.getString(c.getColumnIndex("httpuser")));
                }
                if (c.getString(c.getColumnIndex("httppassword")) == null) {
                    blog.setHttppassword("");
                } else {
                    blog.setHttppassword(decryptPassword(c.getString(c.getColumnIndex("httppassword"))));
                }
                blog.setImagePlacement(c.getString(c.getColumnIndex("imagePlacement")));
                blog.setFeaturedImageCapable(c.getInt(c.getColumnIndex("centerThumbnail")) > 0);
                blog.setFullSizeImage(c.getInt(c.getColumnIndex("fullSizeImage")) > 0);
                blog.setMaxImageWidth(c.getString(c.getColumnIndex("maxImageWidth")));
                blog.setMaxImageWidthId(c.getInt(c.getColumnIndex("maxImageWidthId")));
                blog.setRemoteBlogId(c.getInt(c.getColumnIndex("blogId")));
                blog.setDotcomFlag(c.getInt(c.getColumnIndex("dotcomFlag")) > 0);
                if (c.getString(c.getColumnIndex("dotcom_username")) != null) {
                    blog.setDotcom_username(c.getString(c.getColumnIndex("dotcom_username")));
                }
                if (c.getString(c.getColumnIndex("dotcom_password")) != null) {
                    blog.setDotcom_password(decryptPassword(c.getString(c.getColumnIndex("dotcom_password"))));
                }
                if (c.getString(c.getColumnIndex("api_key")) != null) {
                    blog.setApi_key(c.getString(c.getColumnIndex("api_key")));
                }
                if (c.getString(c.getColumnIndex("api_blogid")) != null) {
                    blog.setApi_blogid(c.getString(c.getColumnIndex("api_blogid")));
                }
                if (c.getString(c.getColumnIndex("wpVersion")) != null) {
                    blog.setWpVersion(c.getString(c.getColumnIndex("wpVersion")));
                }
                blog.setPostFormats(c.getString(c.getColumnIndex("postFormats")));
                blog.setScaledImage(c.getInt(c.getColumnIndex("isScaledImage")) > 0);
                blog.setScaledImageWidth(c.getInt(c.getColumnIndex("scaledImgWidth")));
                blog.setHomeURL(c.getString(c.getColumnIndex("homeURL")));
                if (c.getString(c.getColumnIndex("blog_options")) == null) {
                    blog.setBlogOptions("{}");
                } else {
                    blog.setBlogOptions(c.getString(c.getColumnIndex("blog_options")));
                }
                blog.setAdmin(c.getInt(c.getColumnIndex("isAdmin")) > 0);
                blog.setHidden(c.getInt(c.getColumnIndex("isHidden")) > 0);
            }
        }
        c.close();
        return blog;
    }

    public static String decryptPassword(String encryptedPwd) {
        try {
            DESKeySpec keySpec = new DESKeySpec(
                    PASSWORD_SECRET.getBytes("UTF-8"));
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey key = keyFactory.generateSecret(keySpec);

            byte[] encryptedWithoutB64 = Base64.decode(encryptedPwd, Base64.DEFAULT);
            Cipher cipher = Cipher.getInstance("DES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] plainTextPwdBytes = cipher.doFinal(encryptedWithoutB64);
            return new String(plainTextPwdBytes);
        } catch (Exception e) {
        }
        return encryptedPwd;
    }

    public int getLocalTableBlogIdForRemoteBlogIdAndXmlRpcUrl(int remoteBlogId, String xmlRpcUrl) {
        int localBlogID = SqlUtils.intForQuery(db, "SELECT id FROM accounts WHERE blogId=? AND url=?",
                new String[]{Integer.toString(remoteBlogId), xmlRpcUrl});
        if (localBlogID==0) {
            localBlogID = this.getLocalTableBlogIdForJetpackRemoteID(remoteBlogId, xmlRpcUrl);
        }
        return localBlogID;
    }
    /**
     * Set the ID of the most recently active blog. This value will persist between application
     * launches.
     *
     * @param id ID of the most recently active blog.
     */
    public void updateLastBlogId(int id) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("last_blog_id", id);
        editor.commit();
    }

    public static void deleteDatabase(Context ctx) {
        ctx.deleteDatabase(DATABASE_NAME);
    }

    /*public static RestClientUtils getRestClientUtils() {
        if (mRestClientUtils == null) {
            OAuthAuthenticator authenticator = OAuthAuthenticatorFactory.instantiate();
            mRestClientUtils = new RestClientUtils(requestQueue, authenticator, mOnAuthFailedListener);
        }
        return mRestClientUtils;
    }*/

    //add boolean to track if featured image should be included in the post content
    private static final String ADD_FEATURED_IN_POST = "alter table media add isFeaturedInPost boolean default false;";

    // add home url to blog settings
    private static final String ADD_HOME_URL = "alter table accounts add homeURL text default '';";

    private static final String ADD_BLOG_OPTIONS = "alter table accounts add blog_options text default '';";


    // add admin flag to blog settings
    private static final String ADD_ACCOUNTS_ADMIN_FLAG = "alter table accounts add isAdmin boolean default false;";

    // add category parent id to keep track of category hierarchy
    private static final String ADD_PARENTID_IN_CATEGORIES = "alter table cats add parent_id integer default 0;";


    // Add boolean to POSTS to track posts currently being uploaded
    private static final String ADD_IS_UPLOADING = "alter table posts add isUploading boolean default 0";

    // add thumbnailURL, thumbnailPath and fileURL to media
    private static final String ADD_MEDIA_THUMBNAIL_URL = "alter table media add thumbnailURL text default '';";
    private static final String ADD_MEDIA_FILE_URL = "alter table media add fileURL text default '';";
    private static final String ADD_MEDIA_UNIQUE_ID = "alter table media add mediaId text default '';";
    private static final String ADD_MEDIA_BLOG_ID = "alter table media add blogId text default '';";
    private static final String ADD_MEDIA_DATE_GMT = "alter table media add date_created_gmt date;";
    private static final String ADD_MEDIA_UPLOAD_STATE = "alter table media add uploadState default '';";
    private static final String ADD_MEDIA_VIDEOPRESS_SHORTCODE = "alter table media add videoPressShortcode text default '';";

    //Add all the database tables to make app functional
    private void setupDB()
    {
        // Add columns that were added in very early releases, then move on to version 9
        db.execSQL(ADD_BLOGID);
        db.execSQL(UPDATE_BLOGID);
        db.execSQL(ADD_LOCATION_FLAG);
        db.execSQL(ADD_DOTCOM_USERNAME);
        db.execSQL(ADD_DOTCOM_PASSWORD);
        db.execSQL(ADD_API_KEY);
        db.execSQL(ADD_API_BLOGID);
        db.execSQL(ADD_DOTCOM_FLAG);
        db.execSQL(ADD_WP_VERSION);
        db.execSQL(ADD_HTTPUSER);
        db.execSQL(ADD_HTTPPASSWORD);
        migratePasswords();
        db.delete(POSTS_TABLE, null, null);
        db.execSQL(CREATE_TABLE_POSTS);
        db.execSQL(ADD_POST_FORMATS);
        db.execSQL(ADD_SCALED_IMAGE);
        db.execSQL(ADD_SCALED_IMAGE_IMG_WIDTH);
        db.execSQL(ADD_LOCAL_POST_CHANGES);
        db.execSQL(ADD_FEATURED_IN_POST);
        db.execSQL(ADD_HOME_URL);
        db.execSQL(ADD_BLOG_OPTIONS);
        migrateWPComAccount();
        db.execSQL(ADD_PARENTID_IN_CATEGORIES);
        db.execSQL(ADD_ACCOUNTS_ADMIN_FLAG);
        db.execSQL(ADD_MEDIA_FILE_URL);
        db.execSQL(ADD_MEDIA_THUMBNAIL_URL);
        db.execSQL(ADD_MEDIA_UNIQUE_ID);
        db.execSQL(ADD_MEDIA_BLOG_ID);
        db.execSQL(ADD_MEDIA_DATE_GMT);
        db.execSQL(ADD_MEDIA_UPLOAD_STATE);
        db.execSQL(ADD_ACCOUNTS_HIDDEN_FLAG);
        db.execSQL(ADD_MEDIA_VIDEOPRESS_SHORTCODE);
        CommentTable.reset(db);
        // Drop the notes table, no longer needed with Simperium.
        db.execSQL("DROP TABLE IF EXISTS notes;");
        // Add isUploading column to POSTS
        db.execSQL(ADD_IS_UPLOADING);
        // Remove WordPress.com credentials
        removeDotComCredentials();
    }

    public CMSDB(Context ctx) {
        this.context = ctx;
        db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);
        // Create tables if they don't exist
        db.execSQL(CREATE_TABLE_SETTINGS);
        db.execSQL(CREATE_TABLE_POSTS);
        db.execSQL(CREATE_TABLE_CATEGORIES);
        db.execSQL(CREATE_TABLE_QUICKPRESS_SHORTCUTS);
        db.execSQL(CREATE_TABLE_MEDIA);
        db.execSQL(CREATE_TABLE_THEMES);

        CommentTable.createTables(db);
        SuggestionTable.createTables(db);

        //setupDB();

        // Update tables for new installs and app updates
        int currentVersion = db.getVersion();
        switch (currentVersion) {
            case 0:
                // New install
                currentVersion++;
            case 1:
                // Add columns that were added in very early releases, then move on to version 9
                db.execSQL(ADD_BLOGID);
                db.execSQL(UPDATE_BLOGID);
                db.execSQL(ADD_LOCATION_FLAG);
                db.execSQL(ADD_DOTCOM_USERNAME);
                db.execSQL(ADD_DOTCOM_PASSWORD);
                db.execSQL(ADD_API_KEY);
                db.execSQL(ADD_API_BLOGID);
                db.execSQL(ADD_DOTCOM_FLAG);
                db.execSQL(ADD_WP_VERSION);
                currentVersion = 9;
            case 9:
                db.execSQL(ADD_HTTPUSER);
                db.execSQL(ADD_HTTPPASSWORD);
                migratePasswords();
                currentVersion++;
            case 10:
                db.delete(POSTS_TABLE, null, null);
                db.execSQL(CREATE_TABLE_POSTS);
                db.execSQL(ADD_POST_FORMATS);
                currentVersion++;
            case 11:
                db.execSQL(ADD_SCALED_IMAGE);
                db.execSQL(ADD_SCALED_IMAGE_IMG_WIDTH);
                db.execSQL(ADD_LOCAL_POST_CHANGES);
                currentVersion++;

            case 12:
                db.execSQL(ADD_FEATURED_IN_POST);
                currentVersion++;
            case 13:
                db.execSQL(ADD_HOME_URL);
                currentVersion++;
            case 14:
                db.execSQL(ADD_BLOG_OPTIONS);
                currentVersion++;
            case 15:
                // No longer used (preferences migration)
                currentVersion++;
            case 16:
                migrateWPComAccount();
                currentVersion++;
            case 17:
                db.execSQL(ADD_PARENTID_IN_CATEGORIES);
                currentVersion++;
            case 18:
                db.execSQL(ADD_ACCOUNTS_ADMIN_FLAG);
                db.execSQL(ADD_MEDIA_FILE_URL);
                db.execSQL(ADD_MEDIA_THUMBNAIL_URL);
                db.execSQL(ADD_MEDIA_UNIQUE_ID);
                db.execSQL(ADD_MEDIA_BLOG_ID);
                db.execSQL(ADD_MEDIA_DATE_GMT);
                db.execSQL(ADD_MEDIA_UPLOAD_STATE);
                currentVersion++;
            case 19:
                // revision 20: create table "notes"
                currentVersion++;
            case 20:
                db.execSQL(ADD_ACCOUNTS_HIDDEN_FLAG);
                currentVersion++;
            case 21:
                db.execSQL(ADD_MEDIA_VIDEOPRESS_SHORTCODE);
                currentVersion++;
                // version 23 added CommentTable.java, version 24 changed the comment table schema
            case 22:
                currentVersion++;
            case 23:
                CommentTable.reset(db);
                currentVersion++;
            case 24:
                currentVersion++;
            case 25:
                //ver 26 "virtually" remove columns 'lastCommentId' and 'runService' from the DB
                //SQLite supports a limited subset of ALTER TABLE.
                //The ALTER TABLE command in SQLite allows the user to rename a table or to add a new column to an existing table.
                //It is not possible to rename a column, remove a column, or add or remove constraints from a table.
                currentVersion++;
            case 26:
                // Drop the notes table, no longer needed with Simperium.
                db.execSQL("DROP TABLE IF EXISTS notes;");
                currentVersion++;
            case 27:
                // Add isUploading column to POSTS
                db.execSQL(ADD_IS_UPLOADING);
                currentVersion++;
            case 28:
                // Remove WordPress.com credentials
                removeDotComCredentials();
                currentVersion++;
        }


        db.setVersion(DATABASE_VERSION);

    }

    // Removes stored DotCom credentials. As of March 2015 only the OAuth token is used
    private void removeDotComCredentials() {
        // First clear out the password for all WP.com sites
        ContentValues dotComValues = new ContentValues();
        dotComValues.put("password", "");
        db.update(SETTINGS_TABLE, dotComValues, "dotcomFlag=1", null);

        // Next, we'll clear out the credentials stored for Jetpack sites
        ContentValues jetPackValues = new ContentValues();
        jetPackValues.put("dotcom_username", "");
        jetPackValues.put("dotcom_password", "");
        db.update(SETTINGS_TABLE, jetPackValues, null, null);

        // Lastly we'll remove the preference that previously stored the WP.com password
        if (this.context != null) {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this.context);
            SharedPreferences.Editor editor = settings.edit();
            editor.remove("wp_pref_wpcom_password");
            editor.apply();
        }
    }

    private void migrateWPComAccount() {
        Cursor c = db.query(SETTINGS_TABLE, new String[] { "username" }, "dotcomFlag=1", null, null,
                null, null);

        if (c.getCount() > 0) {
            c.moveToFirst();
            String username = c.getString(0);
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this.context);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(CMS.WPCOM_USERNAME_PREFERENCE, username);
            editor.commit();
        }

        c.close();
    }
    public List<Map<String, Object>> getAllAccounts() {
        return getAccountsBy(null, null);
    }

    public List<Map<String, Object>> getAccountsBy(String byString, String[] extraFields) {
        return getAccountsBy(byString, extraFields, 0);
    }

    public List<Integer> getAllAccountIDs() {
        Cursor c = db.rawQuery("SELECT DISTINCT id FROM " + SETTINGS_TABLE, null);
        try {
            List<Integer> ids = new ArrayList<Integer>();
            if (c.moveToFirst()) {
                do {
                    ids.add(c.getInt(0));
                } while (c.moveToNext());
            }
            return ids;
        } finally {
            SqlUtils.closeCursor(c);
        }
    }

    private void migratePasswords() {
        Cursor c = db.query(SETTINGS_TABLE, new String[] { "id", "password",
                        "httppassword", "dotcom_password" }, null, null, null, null,
                null);
        int numRows = c.getCount();
        c.moveToFirst();

        for (int i = 0; i < numRows; i++) {
            ContentValues values = new ContentValues();

            if (c.getString(1) != null) {
                values.put("password", encryptPassword(c.getString(1)));
            }
            if (c.getString(2) != null) {
                values.put("httppassword", encryptPassword(c.getString(2)));
            }
            if (c.getString(3) != null) {
                values.put("dotcom_password", encryptPassword(c.getString(3)));
            }

            db.update(SETTINGS_TABLE, values, "id=" + c.getInt(0), null);

            c.moveToNext();
        }
        c.close();
    }

    public SQLiteDatabase getDatabase() {
        return db;
    }


    public Post getPostForLocalTablePostId(long localTablePostId) {
        Cursor c = db.query(POSTS_TABLE, null, "id=?", new String[]{String.valueOf(localTablePostId)}, null, null, null);

        Post post = new Post();
        if (c.moveToFirst()) {
            post.setLocalTablePostId(c.getLong(c.getColumnIndex("id")));
            post.setLocalTableBlogId(Integer.valueOf(c.getString(c.getColumnIndex("blogID"))));
            post.setRemotePostId(c.getString(c.getColumnIndex("postid")));
            post.setTitle(c.getString(c.getColumnIndex("title")));
            post.setDateCreated(c.getLong(c.getColumnIndex("dateCreated")));
            post.setDate_created_gmt(c.getLong(c.getColumnIndex("date_created_gmt")));
            post.setCategories(c.getString(c.getColumnIndex("categories")));
            post.setCustomFields(c.getString(c.getColumnIndex("custom_fields")));
            post.setDescription(c.getString(c.getColumnIndex("description")));
            post.setLink(c.getString(c.getColumnIndex("link")));
            post.setAllowComments(SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("mt_allow_comments"))));
            post.setAllowPings(SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("mt_allow_pings"))));
            post.setPostExcerpt(c.getString(c.getColumnIndex("mt_excerpt")));
            post.setKeywords(c.getString(c.getColumnIndex("mt_keywords")));
            post.setMoreText(c.getString(c.getColumnIndex("mt_text_more")));
            post.setPermaLink(c.getString(c.getColumnIndex("permaLink")));
            post.setPostStatus(c.getString(c.getColumnIndex("post_status")));
            post.setUserId(c.getString(c.getColumnIndex("userid")));
            post.setAuthorDisplayName(c.getString(c.getColumnIndex("wp_author_display_name")));
            post.setAuthorId(c.getString(c.getColumnIndex("wp_author_id")));
            post.setPassword(c.getString(c.getColumnIndex("wp_password")));
            post.setPostFormat(c.getString(c.getColumnIndex("wp_post_format")));
            post.setSlug(c.getString(c.getColumnIndex("wp_slug")));
            post.setMediaPaths(c.getString(c.getColumnIndex("mediaPaths")));

            int latColumnIndex = c.getColumnIndex("latitude");
            int lngColumnIndex = c.getColumnIndex("longitude");
            if (!c.isNull(latColumnIndex) && !c.isNull(lngColumnIndex)) {
                //post.setLocation(c.getDouble(latColumnIndex), c.getDouble(lngColumnIndex));
            }

            post.setLocalDraft(SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("localDraft"))));
            post.setUploading(SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("isUploading"))));
            post.setUploaded(SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("uploaded"))));
            post.setIsPage(SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("isPage"))));
            post.setPageParentId(c.getString(c.getColumnIndex("wp_page_parent_id")));
            post.setPageParentTitle(c.getString(c.getColumnIndex("wp_page_parent_title")));
            post.setLocalChange(SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("isLocalChange"))));
        } else {
            post = null;
        }

        c.close();
        return post;
    }


    public List<Map<String, Object>> getAccountsBy(String byString, String[] extraFields, int limit) {
        if (db == null) {
            return new Vector<Map<String, Object>>();
        }
        String limitStr = null;
        if (limit != 0) {
            limitStr = String.valueOf(limit);
        }
        String[] baseFields = new String[]{"id", "blogName", "username", "blogId", "url"};
        String[] allFields = baseFields;
        if (extraFields != null) {

           allFields = (String[]) ArrayUtils.addAll(baseFields, extraFields);
        }
        Cursor c = db.query(SETTINGS_TABLE, allFields, byString, null, null, null, null, limitStr);
        int numRows = c.getCount();
        c.moveToFirst();
        List<Map<String, Object>> accounts = new Vector<Map<String, Object>>();
        for (int i = 0; i < numRows; i++) {
            int id = c.getInt(0);
            String blogName = c.getString(1);
            String username = c.getString(2);
            int blogId = c.getInt(3);
            String url = c.getString(4);
            if (id > 0) {
                Map<String, Object> thisHash = new HashMap<String, Object>();
                thisHash.put("id", id);
                thisHash.put("blogName", blogName);
                thisHash.put("username", username);
                thisHash.put("blogId", blogId);
                thisHash.put("url", url);
                int extraFieldsIndex = baseFields.length;
                if (extraFields != null) {
                    for (int j = 0; j < extraFields.length; ++j) {
                        thisHash.put(extraFields[j], c.getString(extraFieldsIndex + j));
                    }
                }
                accounts.add(thisHash);
            }
            c.moveToNext();
        }
        c.close();
        //Collections.sort(accounts, BlogUtils.BlogNameComparator);
        return accounts;
    }

    public int getNumDotComAccounts() {
        return SqlUtils.intForQuery(db, "SELECT COUNT(*) FROM " + SETTINGS_TABLE + " WHERE dotcomFlag = 1", null);
    }
    public boolean isDotComAccountVisible(int blogId) {
        String[] args = {Integer.toString(blogId)};
        return SqlUtils.boolForQuery(db, "SELECT 1 FROM " + SETTINGS_TABLE +
                " WHERE isHidden = 0 AND blogId=?", args);
    }

    public List<Map<String, Object>> getVisibleAccounts() {
        return getAccountsBy("isHidden = 0", null);
    }

    /**
     * Get the ID of the most recently active blog. -1 is returned if there is no recently active
     * blog.
     */
    public int getLastBlogId() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getInt("last_blog_id", -1);
    }

    public int getNumVisibleAccounts() {
        return SqlUtils.intForQuery(db, "SELECT COUNT(*) FROM " + SETTINGS_TABLE +
                " WHERE isHidden = 0", null);
    }
}
