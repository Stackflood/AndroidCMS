package com.example.manish.androidcms.models;

/**
 * Created by Manish on 4/3/2015.
 */
public class Blog {
    private int localTableBlogId;
    private String url;
    private String homeURL;
    private String blogName;
    private String username;
    private String password;
    private String imagePlacement;
    private boolean featuredImageCapable;
    private boolean fullSizeImage;
    private boolean scaledImage;
    private int scaledImageWidth;
    private String maxImageWidth;
    private int maxImageWidthId;
    private int remoteBlogId;
    private String dotcom_username;
    private String dotcom_password;
    private String api_key;
    private String api_blogid;
    private boolean dotcomFlag;
    private String wpVersion;
    private String httpuser = "";
    private String httppassword = "";
    private String postFormats;
    private String blogOptions = "{}";
    private boolean isAdmin;
    private boolean isHidden;

    public Blog() {
    }

    public Blog(int localTableBlogId, String url, String homeURL, String blogName, String username, String password, String imagePlacement, boolean featuredImageCapable, boolean fullSizeImage, boolean scaledImage, int scaledImageWidth, String maxImageWidth, int maxImageWidthId, int remoteBlogId, String dotcom_username, String dotcom_password, String api_key, String api_blogid, boolean dotcomFlag, String wpVersion, String httpuser, String httppassword, String postFormats, String blogOptions, boolean isAdmin, boolean isHidden) {
        this.localTableBlogId = localTableBlogId;
        this.url = url;
        this.homeURL = homeURL;
        this.blogName = blogName;
        this.username = username;
        this.password = password;
        this.imagePlacement = imagePlacement;
        this.featuredImageCapable = featuredImageCapable;
        this.fullSizeImage = fullSizeImage;
        this.scaledImage = scaledImage;
        this.scaledImageWidth = scaledImageWidth;
        this.maxImageWidth = maxImageWidth;
        this.maxImageWidthId = maxImageWidthId;
        this.remoteBlogId = remoteBlogId;
        this.dotcom_username = dotcom_username;
        this.dotcom_password = dotcom_password;
        this.api_key = api_key;
        this.api_blogid = api_blogid;
        this.dotcomFlag = dotcomFlag;
        this.wpVersion = wpVersion;
        this.httpuser = httpuser;
        this.httppassword = httppassword;
        this.postFormats = postFormats;
        this.blogOptions = blogOptions;
        this.isAdmin = isAdmin;
        this.isHidden = isHidden;
    }

    public Blog(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.localTableBlogId = -1;
    }

    public int getLocalTableBlogId() {
        return localTableBlogId;
    }

    public void setLocalTableBlogId(int id) {
        this.localTableBlogId = id;
    }

    public String getBlogName() {
        return blogName;
    }

    public void setBlogName(String blogName) {
        this.blogName = blogName;
    }


}
