package com.example.manish.androidcms.models;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Reader;
import java.util.ArrayList;

/**
 * Created by Manish on 12/30/2015.
 */
public class ReaderPostList extends ArrayList<ReaderPost> {

    public static ReaderPostList fromJson(JSONObject json) {
        if (json == null) {
            throw new IllegalArgumentException("null json post list");
        }

        ReaderPostList posts = new ReaderPostList();
        JSONArray jsonPosts = json.optJSONArray("posts");
        if (jsonPosts != null) {
            for (int i = 0; i < jsonPosts.length(); i++) {
                posts.add(ReaderPost.fromJson(jsonPosts.optJSONObject(i)));
            }
        }

        return posts;
    }

    /*
     * does passed list contain the same posts as this list?
     */
    public boolean isSameList(ReaderPostList posts) {
        if (posts == null || posts.size() != this.size()) {
            return false;
        }

        for (ReaderPost post: posts) {
            int index = indexOfPost(post);
            if (index == -1 || !post.isSamePost(this.get(index))) {
                return false;
            }
        }

        return true;
    }

    public int indexOfPost(ReaderPost post) {
        if (post == null) {
            return -1;
        }
        for (int i = 0; i < size(); i++) {
            if (this.get(i).postId == post.postId) {
                if (post.isExternal && post.feedId == this.get(i).feedId) {
                    return i;
                } else if (!post.isExternal && post.blogId == this.get(i).blogId) {
                    return i;
                }
            }
        }
        return -1;
    }
}
