package de.mwg_bayreuth.mwgorganizer;


import android.content.Context;
import android.support.v4.app.Fragment;


/**
 * Forces developers to implement methods required by the GetFileToolkits for every
 * FileSelectionFragment
 */
abstract class FileSelectionFragment extends Fragment {
    abstract void setupButtons(Context context);
    abstract void setLastUpdateTimeLabel();
}
