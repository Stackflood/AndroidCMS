package com.example.manish.androidcms.datasets;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import com.example.manish.androidcms.CMS;
import com.example.manish.androidcms.R;
import com.example.manish.androidcms.models.ReaderPost;
import com.example.manish.androidcms.models.ReaderPostList;
import com.example.manish.androidcms.models.ReaderTag;
import com.example.manish.androidcms.models.ReaderTagList;
import com.example.manish.androidcms.models.ReaderTagType;
import com.example.manish.androidcms.ui.reader.ReaderConstants;
import com.example.manish.androidcms.ui.reader.actions.ReaderActions;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.SqlUtils;

/**
 * tbl_posts contains all reader posts
 * tbl_post_tags stores the association between posts and tags (posts can exist in more than one tag)
 *
 */
public class ReaderPostTable {
    public static void addOrUpdatePosts(final ReaderTag tag, ReaderPostList posts) {
        if (posts == null || posts.size() == 0) {
            return;
        }

        SQLiteDatabase db = ReaderDatabase.getWritableDb();
        SQLiteStatement stmtPosts = db.compileStatement(
                "INSERT OR REPLACE INTO tbl_posts ("
                        + COLUMN_NAMES
                        + ") VALUES (?1,?2,?3,?4,?5,?6,?7,?8,?9,?10,?11,?12,?13,?14,?15,?16,?17,?18,?19,?20,?21,?22,?23,?24,?25,?26,?27,?28,?29,?30,?31,?32,?33)");
        SQLiteStatement stmtTags = db.compileStatement(
                "INSERT OR REPLACE INTO tbl_post_tags (post_id, blog_id, feed_id, pseudo_id, tag_name, tag_type) VALUES (?1,?2,?3,?4,?5,?6)");

        db.beginTransaction();
        try {
            // first insert into tbl_posts
            for (ReaderPost post: posts) {
                stmtPosts.bindLong  (1,  post.postId);
                stmtPosts.bindLong  (2,  post.blogId);
                stmtPosts.bindLong  (3,  post.feedId);
                stmtPosts.bindString(4,  post.getPseudoId());
                stmtPosts.bindString(5,  post.getAuthorName());
                stmtPosts.bindLong  (6,  post.authorId);
                stmtPosts.bindString(7,  post.getTitle());
                stmtPosts.bindString(8,  maxText(post));
                stmtPosts.bindString(9,  post.getExcerpt());
                stmtPosts.bindString(10, post.getUrl());
                stmtPosts.bindString(11, post.getShortUrl());
                stmtPosts.bindString(12, post.getBlogUrl());
                stmtPosts.bindString(13, post.getBlogName());
                stmtPosts.bindString(14, post.getFeaturedImage());
                stmtPosts.bindString(15, post.getFeaturedVideo());
                stmtPosts.bindString(16, post.getPostAvatar());
                stmtPosts.bindLong  (17, post.timestamp);
                stmtPosts.bindString(18, post.getPublished());
                stmtPosts.bindLong  (19, post.numReplies);
                stmtPosts.bindLong  (20, post.numLikes);
                stmtPosts.bindLong  (21, SqlUtils.boolToSql(post.isLikedByCurrentUser));
                stmtPosts.bindLong  (22, SqlUtils.boolToSql(post.isFollowedByCurrentUser));
                stmtPosts.bindLong  (23, SqlUtils.boolToSql(post.isCommentsOpen));
                stmtPosts.bindLong  (24, SqlUtils.boolToSql(post.isRebloggedByCurrentUser));
                stmtPosts.bindLong  (25, SqlUtils.boolToSql(post.isExternal));
                stmtPosts.bindLong  (26, SqlUtils.boolToSql(post.isPrivate));
                stmtPosts.bindLong  (27, SqlUtils.boolToSql(post.isVideoPress));
                stmtPosts.bindLong  (28, SqlUtils.boolToSql(post.isJetpack));
                stmtPosts.bindString(29, post.getPrimaryTag());
                stmtPosts.bindString(30, post.getSecondaryTag());
                stmtPosts.bindLong  (31, SqlUtils.boolToSql(post.isLikesEnabled));
                stmtPosts.bindLong  (32, SqlUtils.boolToSql(post.isSharingEnabled));
                stmtPosts.bindString(33, post.getAttachmentsJson());
                stmtPosts.execute();
            }

            // now add to tbl_post_tags if a tag was passed
            if (tag != null) {
                String tagName = tag.getTagName();
                int tagType = tag.tagType.toInt();
                for (ReaderPost post: posts) {
                    stmtTags.bindLong  (1, post.postId);
                    stmtTags.bindLong  (2, post.blogId);
                    stmtTags.bindLong  (3, post.feedId);
                    stmtTags.bindString(4, post.getPseudoId());
                    stmtTags.bindString(5, tagName);
                    stmtTags.bindLong  (6, tagType);
                    stmtTags.execute();
                }
            }

            db.setTransactionSuccessful();

        } finally {
            db.endTransaction();
            SqlUtils.closeStatement(stmtPosts);
            SqlUtils.closeStatement(stmtTags);
        }
    }


    /*
     * purge table of unattached/older posts - no need to wrap this in a transaction since it's
     * only called from ReaderDatabase.purge() which already creates a transaction
     */
    protected static int purge(SQLiteDatabase db) {
        // delete posts in tbl_post_tags attached to tags that no longer exist
        int numDeleted = db.delete("tbl_post_tags",
                "tag_name NOT IN (SELECT DISTINCT tag_name FROM tbl_tags)", null);

        // delete excess posts on a per-tag basis
        ReaderTagList tags = ReaderTagTable.getAllTags();
        for (ReaderTag tag: tags) {
            numDeleted += purgePostsForTag(db, tag);
        }

        // delete posts in tbl_posts that no longer exist in tbl_post_tags
        numDeleted += db.delete("tbl_posts", "pseudo_id NOT IN (SELECT DISTINCT pseudo_id FROM tbl_post_tags)", null);

        return numDeleted;
    }

    /*
     * purge excess posts in the passed tag - note we only keep as many posts as are returned
     * by a single request
     */
    private static final int MAX_POSTS_PER_TAG = ReaderConstants.READER_MAX_POSTS_TO_REQUEST;
    private static int purgePostsForTag(SQLiteDatabase db, ReaderTag tag) {
        int numPosts = getNumPostsWithTag(tag);
        if (numPosts <= MAX_POSTS_PER_TAG) {
            return 0;
        }

        int numToPurge = numPosts - MAX_POSTS_PER_TAG;
        String[] args = {tag.getTagName(), Integer.toString(tag.tagType.toInt()),
                Integer.toString(numToPurge)};
        String where = "pseudo_id IN ("
                + "  SELECT tbl_posts.pseudo_id FROM tbl_posts, tbl_post_tags"
                + "  WHERE tbl_posts.pseudo_id = tbl_post_tags.pseudo_id"
                + "  AND tbl_post_tags.tag_name=?1"
                + "  AND tbl_post_tags.tag_type=?2"
                + "  ORDER BY tbl_posts.timestamp"
                + "  LIMIT ?3"
                + ")";
        int numDeleted = db.delete("tbl_post_tags", where, args);
        AppLog.d(AppLog.T.READER, String.format("reader post table > purged %d posts in tag %s", numDeleted, tag.getTagNameForLog()));
        return numDeleted;
    }



    public static ReaderPostList getPostsWithTag(ReaderTag tag, int maxPosts,
                                                 boolean excludeTextColumn) {
        if (tag == null) {
            return new ReaderPostList();
        }

        String columns = (excludeTextColumn ? COLUMN_NAMES_NO_TEXT : "tbl_posts.*");
        String sql = "SELECT " + columns + " FROM tbl_posts, tbl_post_tags"
                + " WHERE tbl_posts.post_id = tbl_post_tags.post_id"
                + " AND tbl_posts.blog_id = tbl_post_tags.blog_id"
                + " AND tbl_post_tags.tag_name=?"
                + " AND tbl_post_tags.tag_type=?";

        if (tag.tagType == ReaderTagType.DEFAULT) {
            // skip posts that are no longer liked if this is "Posts I Like", skip posts that are no
            // longer followed if this is "Blogs I Follow"
            if (tag.isPostsILike()) {
                sql += " AND tbl_posts.is_liked != 0";
            } else if (tag.isBlogsIFollow()) {
                sql += " AND tbl_posts.is_followed != 0";
            }
        }

        sql += " ORDER BY tbl_posts.timestamp DESC";

        if (maxPosts > 0) {
            sql += " LIMIT " + Integer.toString(maxPosts);
        }

        String[] args = {tag.getTagName(), Integer.toString(tag.tagType.toInt())};
        Cursor cursor = ReaderDatabase.getReadableDb().rawQuery(sql, args);
        try {
            return getPostListFromCursor(cursor);
        } finally {
            SqlUtils.closeCursor(cursor);
        }
    }

    private static ReaderPostList getPostListFromCursor(Cursor cursor) {
        ReaderPostList posts = new ReaderPostList();
        try {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    posts.add(getPostFromCursor(cursor));
                } while (cursor.moveToNext());
            }
        } catch (IllegalStateException e) {
            //CrashlyticsUtils.logException(e, CrashlyticsUtils.ExceptionType.SPECIFIC);
            AppLog.e(AppLog.T.READER, e);
        }
        return posts;
    }

    public static int getNumPostsInFeed(long feedId)
    {
        if(feedId == 0)
        {
            return 0;
        }

        return SqlUtils.intForQuery(ReaderDatabase.getReadableDb(),
                "SELECT count(*) FROM tbl_posts WHERE feed_id=?",
                new String[]{Long.toString(feedId)});

    }

    public static int getNumPostsInBlog(long blogId) {
        if (blogId == 0) {
            return 0;
        }
        return SqlUtils.intForQuery(ReaderDatabase.getReadableDb(),
                "SELECT count(*) FROM tbl_posts WHERE blog_id=?",
                new String[]{Long.toString(blogId)});
    }

    public static int getNumPostsWithTag(ReaderTag tag) {
        if (tag == null) {
            return 0;
        }
        String[] args = {tag.getTagName(), Integer.toString(tag.tagType.toInt())};
        return SqlUtils.intForQuery(ReaderDatabase.getReadableDb(),
                "SELECT count(*) FROM tbl_post_tags WHERE tag_name=? AND tag_type=?",
                args);
    }

    /*
    * updates both the like count for a post and whether it's liked by the current user
    */
    public static void setLikesForPost(ReaderPost post, int numLikes, boolean isLikedByCurrentUser) {
        if (post == null) {
            return;
        }

        String[] args = {Long.toString(post.blogId), Long.toString(post.postId)};

        ContentValues values = new ContentValues();
        values.put("num_likes", numLikes);
        values.put("is_liked", SqlUtils.boolToSql(isLikedByCurrentUser));

        ReaderDatabase.getWritableDb().update(
                "tbl_posts",
                values,
                "blog_id=? AND post_id=?",
                args);
    }

    public static boolean isPostLikedByCurrentUser(ReaderPost post) {
        return post != null && isPostLikedByCurrentUser(post.blogId, post.postId);
    }
    public static boolean isPostLikedByCurrentUser(long blogId, long postId) {
        String[] args = new String[] {Long.toString(blogId), Long.toString(postId)};
        return SqlUtils.boolForQuery(ReaderDatabase.getReadableDb(),
                "SELECT is_liked FROM tbl_posts WHERE blog_id=? AND post_id=?",
                args);
    }

    public static void setFollowStatusForPostsInFeed(long feedId, boolean isFollowed) {
        setFollowStatusForPosts(0, feedId, isFollowed);
    }

    private static void setFollowStatusForPosts(long blogId, long feedId, boolean isFollowed) {
        if (blogId == 0 && feedId == 0) {
            return;
        }

        SQLiteDatabase db = ReaderDatabase.getWritableDb();
        db.beginTransaction();
        try {
            if (blogId != 0) {
                String sql = "UPDATE tbl_posts SET is_followed=" + SqlUtils.boolToSql(isFollowed)
                        + " WHERE blog_id=?";
                db.execSQL(sql, new String[]{Long.toString(blogId)});
            } else {
                String sql = "UPDATE tbl_posts SET is_followed=" + SqlUtils.boolToSql(isFollowed)
                        + " WHERE feed_id=?";
                db.execSQL(sql, new String[]{Long.toString(feedId)});
            }


            // if blog/feed is no longer followed, remove its posts tagged with "Blogs I Follow" in
            // tbl_post_tags
            if (!isFollowed) {
                if (blogId != 0) {
                    db.delete("tbl_post_tags", "blog_id=? AND tag_name=?",
                            new String[]{Long.toString(blogId), ReaderTag.TAG_NAME_FOLLOWING});
                } else {
                    db.delete("tbl_post_tags", "feed_id=? AND tag_name=?",
                            new String[]{Long.toString(feedId), ReaderTag.TAG_NAME_FOLLOWING});
                }
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /*
    * Android's CursorWindow has a max size of 2MB per row which can be exceeded
    * with a very large text column, causing an IllegalStateException when the
    * row is read - prevent this by limiting the amount of text that's stored in
    * the text column - note that this situation very rarely occurs
    * https://github.com/android/platform_frameworks_base/blob/master/core/res/res/values/config.xml#L946
    * https://github.com/android/platform_frameworks_base/blob/3bdbf644d61f46b531838558fabbd5b990fc4913/core/java/android/database/CursorWindow.java#L103
    */
    private static final int MAX_TEXT_LEN = (1024 * 1024) / 2;
    private static String maxText(final ReaderPost post) {
        if (post.getText().length() <= MAX_TEXT_LEN) {
            return post.getText();
        }
        // if the post has an excerpt (which should always be the case), store it as the full text
        // with a link to the full article
        if (post.hasExcerpt()) {
            AppLog.w(AppLog.T.READER, "reader post table > max text exceeded, storing excerpt");
            return "<p>" + post.getExcerpt() + "</p>"
                    + String.format("<p style='text-align:center'><a href='%s'>%s</a></p>",
                    post.getUrl(), CMS.getContext().getString(R.string.reader_label_view_original));
        } else {
            AppLog.w(AppLog.T.READER, "reader post table > max text exceeded, storing truncated text");
            return post.getText().substring(0, MAX_TEXT_LEN);
        }
    }


    private static final String COLUMN_NAMES =
            "post_id,"              // 1
                    + "blog_id,"              // 2
                    + "feed_id,"              // 3
                    + "pseudo_id,"            // 4
                    + "author_name,"          // 5
                    + "author_id,"            // 6
                    + "title,"                // 7
                    + "text,"                 // 8
                    + "excerpt,"              // 9
                    + "url,"                  // 10
                    + "short_url,"            // 11
                    + "blog_url,"             // 12
                    + "blog_name,"            // 13
                    + "featured_image,"       // 14
                    + "featured_video,"       // 15
                    + "post_avatar,"          // 16
                    + "timestamp,"            // 17
                    + "published,"            // 18
                    + "num_replies,"          // 19
                    + "num_likes,"            // 20
                    + "is_liked,"             // 21
                    + "is_followed,"          // 22
                    + "is_comments_open,"     // 23
                    + "is_reblogged,"         // 24
                    + "is_external,"          // 25
                    + "is_private,"           // 26
                    + "is_videopress,"        // 27
                    + "is_jetpack,"           // 28
                    + "primary_tag,"          // 29
                    + "secondary_tag,"        // 30
                    + "is_likes_enabled,"     // 31
                    + "is_sharing_enabled,"   // 32
                    + "attachments_json";     // 33

    // used when querying multiple rows and skipping tbl_posts.text
    private static final String COLUMN_NAMES_NO_TEXT =
            "tbl_posts.post_id,"              // 1
                    + "tbl_posts.blog_id,"              // 2
                    + "tbl_posts.feed_id,"              // 3
                    + "tbl_posts.author_id,"            // 4
                    + "tbl_posts.pseudo_id,"            // 5
                    + "tbl_posts.author_name,"          // 6
                    + "tbl_posts.blog_name,"            // 7
                    + "tbl_posts.blog_url,"             // 8
                    + "tbl_posts.excerpt,"              // 9
                    + "tbl_posts.featured_image,"       // 10
                    + "tbl_posts.featured_video,"       // 11
                    + "tbl_posts.title,"                // 12
                    + "tbl_posts.url,"                  // 13
                    + "tbl_posts.short_url,"            // 14
                    + "tbl_posts.post_avatar,"          // 15
                    + "tbl_posts.timestamp,"            // 16
                    + "tbl_posts.published,"            // 17
                    + "tbl_posts.num_replies,"          // 18
                    + "tbl_posts.num_likes,"            // 19
                    + "tbl_posts.is_liked,"             // 20
                    + "tbl_posts.is_followed,"          // 21
                    + "tbl_posts.is_comments_open,"     // 22
                    + "tbl_posts.is_reblogged,"         // 23
                    + "tbl_posts.is_external,"          // 24
                    + "tbl_posts.is_private,"           // 25
                    + "tbl_posts.is_videopress,"        // 26
                    + "tbl_posts.is_jetpack,"           // 27
                    + "tbl_posts.primary_tag,"          // 28
                    + "tbl_posts.secondary_tag,"        // 29
                    + "tbl_posts.is_likes_enabled,"     // 30
                    + "tbl_posts.is_sharing_enabled,"   // 31
                    + "tbl_posts.attachments_json";     // 32

    /*
     * returns whether any of the passed posts are new or changed - used after posts are retrieved
     */
    public static ReaderActions.UpdateResult comparePosts(ReaderPostList posts) {
        if (posts == null || posts.size() == 0) {
            return ReaderActions.UpdateResult.UNCHANGED;
        }

        boolean hasChanges = false;
        for (ReaderPost post: posts) {
            ReaderPost existingPost = getPost(post.blogId, post.postId, true);
            if (existingPost == null) {
                return ReaderActions.UpdateResult.HAS_NEW;
            } else if (!hasChanges && !post.isSamePost(existingPost)) {
                hasChanges = true;
            }
        }

        return (hasChanges ? ReaderActions.UpdateResult.CHANGED : ReaderActions.UpdateResult.UNCHANGED);
    }

    public static ReaderPost getPost(long blogId, long postId, boolean excludeTextColumn) {

        String columns = (excludeTextColumn ? COLUMN_NAMES_NO_TEXT : "*");
        String sql = "SELECT " + columns + " FROM tbl_posts WHERE blog_id=? AND post_id=? LIMIT 1";

        String[] args = new String[] {Long.toString(blogId), Long.toString(postId)};
        Cursor c = ReaderDatabase.getReadableDb().rawQuery(sql, args);
        try {
            if (!c.moveToFirst()) {
                return null;
            }
            return getPostFromCursor(c);
        } finally {
            SqlUtils.closeCursor(c);
        }
    }

    private static ReaderPost getPostFromCursor(Cursor c) {
        if (c == null) {
            throw new IllegalArgumentException("getPostFromCursor > null cursor");
        }

        ReaderPost post = new ReaderPost();

        // text column is skipped when retrieving multiple rows
        int idxText = c.getColumnIndex("text");
        if (idxText > -1) {
            post.setText(c.getString(idxText));
        }

        post.postId = c.getLong(c.getColumnIndex("post_id"));
        post.blogId = c.getLong(c.getColumnIndex("blog_id"));
        post.feedId = c.getLong(c.getColumnIndex("feed_id"));
        post.authorId = c.getLong(c.getColumnIndex("author_id"));
        post.setPseudoId(c.getString(c.getColumnIndex("pseudo_id")));

        post.setAuthorName(c.getString(c.getColumnIndex("author_name")));
        post.setBlogName(c.getString(c.getColumnIndex("blog_name")));
        post.setBlogUrl(c.getString(c.getColumnIndex("blog_url")));
        post.setExcerpt(c.getString(c.getColumnIndex("excerpt")));
        post.setFeaturedImage(c.getString(c.getColumnIndex("featured_image")));
        post.setFeaturedVideo(c.getString(c.getColumnIndex("featured_video")));

        post.setTitle(c.getString(c.getColumnIndex("title")));
        post.setUrl(c.getString(c.getColumnIndex("url")));
        post.setShortUrl(c.getString(c.getColumnIndex("short_url")));
        post.setPostAvatar(c.getString(c.getColumnIndex("post_avatar")));

        post.timestamp = c.getLong(c.getColumnIndex("timestamp"));
        post.setPublished(c.getString(c.getColumnIndex("published")));

        post.numReplies = c.getInt(c.getColumnIndex("num_replies"));
        post.numLikes = c.getInt(c.getColumnIndex("num_likes"));

        post.isLikedByCurrentUser = SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("is_liked")));
        post.isFollowedByCurrentUser = SqlUtils.sqlToBool(c.getInt( c.getColumnIndex("is_followed")));
        post.isCommentsOpen = SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("is_comments_open")));
        post.isRebloggedByCurrentUser = SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("is_reblogged")));
        post.isExternal = SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("is_external")));
        post.isPrivate = SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("is_private")));
        post.isVideoPress = SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("is_videopress")));
        post.isJetpack = SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("is_jetpack")));

        post.setPrimaryTag(c.getString(c.getColumnIndex("primary_tag")));
        post.setSecondaryTag(c.getString(c.getColumnIndex("secondary_tag")));

        post.isLikesEnabled = SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("is_likes_enabled")));
        post.isSharingEnabled = SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("is_sharing_enabled")));

        post.setAttachmentsJson(c.getString(c.getColumnIndex("attachments_json")));

        return post;
    }
    /*
     * returns the iso8601 published date of the oldest post with the passed tag
     */
    public static String getOldestPubDateWithTag(final ReaderTag tag) {
        if (tag == null) {
            return "";
        }

        String sql = "SELECT tbl_posts.published FROM tbl_posts, tbl_post_tags"
                + " WHERE tbl_posts.post_id = tbl_post_tags.post_id AND tbl_posts.blog_id = tbl_post_tags.blog_id"
                + " AND tbl_post_tags.tag_name=? AND tbl_post_tags.tag_type=?"
                + " ORDER BY published LIMIT 1";
        String[] args = {tag.getTagName(), Integer.toString(tag.tagType.toInt())};
        return SqlUtils.stringForQuery(ReaderDatabase.getReadableDb(), sql, args);
    }
}