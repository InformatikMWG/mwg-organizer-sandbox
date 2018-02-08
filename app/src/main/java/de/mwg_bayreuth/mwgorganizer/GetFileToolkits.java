package de.mwg_bayreuth.mwgorganizer;


import android.app.ProgressDialog;
import android.content.SharedPreferences;

import java.io.File;

abstract class GetFileToolkits {
    SharedPreferences sharedPref;
    SharedPreferences.Editor speditor;
    CacheManager cachemanager;
    ProgressDialog dialog;
    File filedir;
    FileSelectFragment root;

    void setUpReferences(SharedPreferences sharedPref, SharedPreferences.Editor speditor,
                         CacheManager cachemanager, ProgressDialog dialog, File filedir, FileSelectFragment root) {
        this.sharedPref = sharedPref;
        this.speditor = speditor;
        this.cachemanager = cachemanager;
        this.dialog = dialog;
        this.filedir = filedir;
        this.root = root;
    }

    // void setupButtons() = { root.setupButtons; }

    // void setLastUpdateTimeLabel() { root.setLastUpdateTimeLabel(); }

    void deleteFiles() {
        // TODO: Implement mechanism to delete all files
    }

}
