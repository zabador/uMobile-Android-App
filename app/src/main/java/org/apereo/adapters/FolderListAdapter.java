package org.apereo.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.apereo.R;
import org.apereo.models.Folder;

import java.util.List;

/**
 * Created by schneis on 8/20/14.
 */
public class FolderListAdapter extends ArrayAdapter<Folder> {

    Context context;
    int layoutResourceId;
    List<Folder> data = null;
    int selectedIndex;

    public FolderListAdapter(Context context, int layoutResourceId, List<Folder> data, int selectedIndex) {
        super(context, layoutResourceId, data);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.data = data;
        this.selectedIndex = selectedIndex;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        FolderHolder holder = null;

        if (row == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);

            holder = new FolderHolder();

            row.setTag(holder);
        } else {
            holder = (FolderHolder) row.getTag();
        }

        holder.txtName = (TextView) row.findViewById(R.id.name);

        if (position == selectedIndex) {
            holder.txtName.setTypeface(Typeface.DEFAULT_BOLD);
            holder.txtName.setTextColor(
                    context.getResources().getColor(android.R.color.primary_text_dark));
        } else  {
            holder.txtName.setTypeface(Typeface.DEFAULT);
            holder.txtName.setTextColor(
                    context.getResources().getColor(android.R.color.secondary_text_light));
        }

        Folder folder = data.get(position);
        holder.txtName.setText(folder.getName());

        return row;
    }

    public void setSelectedIndex(int selectedIndex) {
        this.selectedIndex = selectedIndex;
    }

    static class FolderHolder {
        TextView txtName;
    }
}
