package com.example.manish.androidcms.models;

import java.util.ArrayList;

/**
 * Created by Manish on 12/30/2015.
 */
public class ReaderTagList extends ArrayList<ReaderTag> {

    @Override
    public Object clone() {
        return super.clone();
    }

    public boolean isSameList(ReaderTagList tagList) {
        if (tagList == null || tagList.size() != this.size()) {
            return false;
        }

        for (ReaderTag thisTag: tagList) {
            if (!hasSameTag(thisTag)) {
                return false;
            }
        }

        return true;
    }



    private boolean hasSameTag(ReaderTag tag) {
        if (tag == null || isEmpty()) {
            return false;
        }

        for (ReaderTag thisTag : this) {
            if (ReaderTag.isSameTag(thisTag, tag)) {
                return true;
            }
        }

        return false;
    }
}
