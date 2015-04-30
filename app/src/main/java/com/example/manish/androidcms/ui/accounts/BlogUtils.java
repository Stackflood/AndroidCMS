package com.example.manish.androidcms.ui.accounts;

import com.example.manish.androidcms.CMS;
import com.example.manish.androidcms.models.Blog;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.MapUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.UrlUtils;

import java.util.List;
import java.util.Map;

/**
 * Created by Manish on 4/13/2015.
 */
public class BlogUtils {


    private static final String DEFAULT_IMAGE_SIZE = "2000";
    /**
     * Add selected blog(s) to the database.
     *
     * @return true if a change has been made (new blog added or old blog updated).
     */
    public static boolean addBlogs(List<Map<String, Object>> blogList, String username, String password,
                                   String httpUsername, String httpPassword) {
        boolean retValue = false;
        for (Map<String, Object> blogMap : blogList) {
            String blogName = StringUtils.unescapeHTML(blogMap.get("blogName").toString());
            String xmlrpc = blogMap.get("xmlrpc").toString();
            String homeUrl = blogMap.get("url").toString();
            String blogId = blogMap.get("blogid").toString();
            boolean isVisible = true;
            if (blogMap.containsKey("isVisible")) {
                isVisible = MapUtils.getMapBool(blogMap, "isVisible");
            }
            boolean isAdmin = MapUtils.getMapBool(blogMap, "isAdmin");
            retValue |= addOrUpdateBlog(blogName, xmlrpc, homeUrl, blogId, username, password, httpUsername,
                    httpPassword, isAdmin, isVisible);
        }
        return retValue;
    }

    /**
     * Check xmlrpc urls validity
     *
     * @param blogList blog list
     * @return true if there is at least one invalid xmlrpc url
     */
    public static boolean isAnyInvalidXmlrpcUrl(List<Map<String, Object>> blogList) {
        for (Map<String, Object> blogMap : blogList) {
            String xmlrpc = blogMap.get("xmlrpc").toString();
            if (!UrlUtils.isValidUrlAndHostNotNull(xmlrpc)) {
                return true;
            }
        }
        return false;
    }


    /**
     * Add a new blog or update a blog name in local DB.
     *
     * @return true if a new blog has been added or an old blog has been updated.
     * Return false if no change has been made.
     */
    public static boolean addOrUpdateBlog(String blogName, String xmlRpcUrl, String homeUrl, String blogId,
                                          String username, String password, String httpUsername, String httpPassword,
                                          boolean isAdmin, boolean isVisible) {
        Blog blog;
        if (!CMS.cmsDB.isBlogInDatabase(Integer.parseInt(blogId), xmlRpcUrl)) {
            // The blog isn't in the app, so let's create it
            blog = new Blog(xmlRpcUrl, username, password);
            blog.setHomeURL(homeUrl);
            blog.setHttpuser(httpUsername);
            blog.setHttppassword(httpPassword);
            blog.setBlogName(blogName);
            // deprecated
            blog.setImagePlacement("");
            blog.setFullSizeImage(false);
            blog.setMaxImageWidth(DEFAULT_IMAGE_SIZE);
            // deprecated
            blog.setMaxImageWidthId(0);
            blog.setRemoteBlogId(Integer.parseInt(blogId));
            blog.setDotcomFlag(xmlRpcUrl.contains("wordpress.com"));
            // assigned later in getOptions call
            blog.setWpVersion("");
            blog.setAdmin(isAdmin);
            blog.setHidden(!isVisible);
            CMS.cmsDB.saveBlog(blog);
            return true;
        } else {
            // Update blog name
            int localTableBlogId = CMS.cmsDB.getLocalTableBlogIdForRemoteBlogIdAndXmlRpcUrl(
                    Integer.parseInt(blogId), xmlRpcUrl);
            try {
                blog = CMS.cmsDB.instantiateBlogByLocalId(localTableBlogId);
                if (!blogName.equals(blog.getBlogName())) {
                    blog.setBlogName(blogName);
                    CMS.cmsDB.saveBlog(blog);
                    return true;
                }
            } catch (Exception e) {
                AppLog.e(AppLog.T.NUX, "localTableBlogId: " + localTableBlogId + " not found");
            }
            return false;
        }
    }

    public static boolean addBlogs(List<Map<String, Object>> userBlogList, String username) {
        return addBlogs(userBlogList, username, null, null, null);
    }
}
