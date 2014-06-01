package hu.zoltan.varadi.rccar;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import hu.varadi.zoltan.rccar.R;

/**
 * Created by zoltan on 2014.06.01..
 */
public class FtdiDeviceInputAdapter extends BaseAdapter {

    List<String> items = new ArrayList<String>();
    Activity context;

    public FtdiDeviceInputAdapter(Activity context) {

        this.context = context;

    }


    public void addItem(String item) {

        this.items.add(item);
    }


    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int i) {
        return items.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = convertView;
        // reuse views
        if (rowView == null) {
            LayoutInflater inflater = context.getLayoutInflater();
            rowView = inflater.inflate(R.layout.modem_input_list_item, null);
            // configure view holder


        }

        // fill data
        TextView text = (TextView) rowView.findViewById(R.id.dataTextView);

        text.setText(items.get(position));

        return rowView;
    }


}
