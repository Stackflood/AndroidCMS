package com.example.manish.androidcms.ui.notifications;


import com.example.manish.androidcms.models.Note;

public interface NotificationFragment {
    public static interface OnPostClickListener {
        public void onPostClicked(Note note, int remoteBlogId, int postId);
    }

    public Note getNote();
    public void setNote(Note note);
}
