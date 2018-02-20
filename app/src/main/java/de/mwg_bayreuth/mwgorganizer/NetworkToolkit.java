package de.mwg_bayreuth.mwgorganizer;


import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AppCompatActivity;


class NetworkToolkit {
    private AppCompatActivity root;

    NetworkToolkit(AppCompatActivity root) { this.root = root; }

    boolean networkConnectionAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) root.getSystemService(root.getApplicationContext().CONNECTIVITY_SERVICE);
        if(connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED) {
            // Device connected to network

            // TODO: Further checks?
            return true;
        } else {
            // Device disconnected from any network
            return false;
        }
    }
}
