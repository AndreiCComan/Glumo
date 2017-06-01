package glumo.com.glumo.adapter;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import glumo.com.glumo.R;

/**
 * This class handles the recycling of the view for the list of bluetooth devices
 */
public class BluetoothListAdapter extends ArrayAdapter<BluetoothDevice>  {

    /**
     * Constructor method, just calls super
     * @param context context
     * @param textViewResourceId text view resource id
     * @param devices devices
     */
    public BluetoothListAdapter(Context context, int textViewResourceId,
                                List<BluetoothDevice> devices) {
        super(context, textViewResourceId, devices);
    }

    /**
     * This method just calls getViewOptimize on the given parameters
     * @param position position
     * @param convertView convertView
     * @param parent parent
     * @return getViewOptimize
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getViewOptimize(position, convertView, parent);
    }

    /**
     * This method handles the layout for the view of the activity
     * @param position position
     * @param convertView convertView
     * @param parent parent
     * @return processed view
     */
    public View getViewOptimize(int position, View convertView, ViewGroup parent) {

        // view holder
        ViewHolder viewHolder = null;

        // applying recycled view
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.bluetooth_device_row, null);
            viewHolder = new ViewHolder();
            viewHolder.deviceName = (TextView)convertView.findViewById(R.id.deviceName);
            viewHolder.deviceMacAddress = (TextView)convertView.findViewById(R.id.deviceMacAddress);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        BluetoothDevice bluetoothDevice = getItem(position);
        viewHolder.deviceName.setText(bluetoothDevice.getName());
        viewHolder.deviceMacAddress.setText(bluetoothDevice.getAddress());
        return convertView;
    }

    /**
     * This method just declares textviews for device name and device mac address
     */
    private class ViewHolder {
        public TextView deviceName;
        public TextView deviceMacAddress;
    }
}
