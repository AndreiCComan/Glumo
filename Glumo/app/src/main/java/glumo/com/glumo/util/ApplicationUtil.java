package glumo.com.glumo.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import glumo.com.glumo.application.GlumoApplication;

/**
 * This class simply handles the internet connection
 */
public class ApplicationUtil {
    /** Checks whether the device currently has a network connection.
     * @return true if the device has a network connection, false otherwise.
     */
    public static boolean isDeviceOnline() {
        // Get a reference to the ConnectivityManager to check state of network connectivity
        ConnectivityManager connectivityManager = (ConnectivityManager) GlumoApplication.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        // Get details on the currently active default data network
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        // If there is a network connection
        return (networkInfo != null && networkInfo.isConnected());
    }
}
