package glumo.com.glumo.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import glumo.com.glumo.R;

public class FileListAdapter extends ArrayAdapter<String>  {

    public FileListAdapter(Context context, int textViewResourceId,
                           List<String> files) {
        super(context, textViewResourceId, files);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getViewOptimize(position, convertView, parent);
    }

    public View getViewOptimize(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder = null;
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.file_item_row, null);
            viewHolder = new ViewHolder();
            viewHolder.fileName = (TextView)convertView.findViewById(R.id.file_name);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        String fileName = getItem(position);
        viewHolder.fileName.setText(fileName);
        return convertView;
    }

    private class ViewHolder {
        public TextView fileName;
    }
}
