package com.example.manish.androidcms.ui.posts;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.example.manish.androidcms.R;
import com.example.manish.androidcms.models.CategoryNode;

import java.util.List;

/**
 * Created by Manish on 6/17/2015.
 */
public class ParentCategorySpinnerAdapter extends BaseAdapter implements SpinnerAdapter {
    int mResourceId;
    List<CategoryNode> mObjects;
    Context mContext;

    public int getCount() {
        return mObjects.size();
    }

    public CategoryNode getItem(int position) {
        return mObjects.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public ParentCategorySpinnerAdapter(Context context, int resource, List<CategoryNode> objects) {
        super();
        mContext = context;
        mObjects = objects;
        mResourceId = resource;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        LayoutInflater inflater = (LayoutInflater)
                mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(mResourceId, parent, false);
        TextView textView = (TextView) rowView.findViewById(R.id.categoryRowText);
        ImageView levelIndicatorView = (ImageView)
                rowView.findViewById(R.id.categoryRowLevelIndicator);
        textView.setText(Html.fromHtml(getItem(position).getName()));
        levelIndicatorView.setVisibility(View.GONE);
        return rowView;
    }
}
