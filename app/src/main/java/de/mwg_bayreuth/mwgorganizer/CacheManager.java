package de.mwg_bayreuth.mwgorganizer;


import android.content.SharedPreferences;

import java.io.File;



/**
 * This class does some management of the "file cache", i.e.
 *     1. deletes all files and remove their meta-data from the shared preferences
 *     2. clears the credentials
 */
class CacheManager {
    private SharedPreferences.Editor speditor;
    private File fileDirectory;



    /**
     * Constructor: requires:
     * @param speditor - a shared preferences editor created by an activity
     * @param extDirectory - the directory where the files are located
     */
    CacheManager(SharedPreferences.Editor speditor, File extDirectory) {
        this.speditor = speditor;
        this.fileDirectory = extDirectory;
    }



    /**
     * Delete all files and remove their meta data from the shared preferences
     */
    void clearFileCache() {
        // Remove Vertretungsplan file meta data from Shared Prefs
        speditor.putLong(SharedPrefKeys.vplanLastUpdate, 0);
        speditor.putInt(SharedPrefKeys.vplanButtonNr, 0);
        for(int i = 0; i < 20; i++) {
            speditor.putString(SharedPrefKeys.vplanButtonFilesize + i, "0,0 KiB");
            speditor.putString(SharedPrefKeys.vplanButtonLabel + i, "");
            speditor.putString(SharedPrefKeys.vplanButtonShortLabel + i, "");
            speditor.putString(SharedPrefKeys.vplanButtonFilename + i, "");
        }

        // Remove Mensa menu file meta data from Shared Prefs
        speditor.putLong(SharedPrefKeys.mensaLastUpdate, 0);
        speditor.putInt(SharedPrefKeys.mensaButtonNr, 0);
        for(int i = 0; i < 20; i++) {
            speditor.putString(SharedPrefKeys.mensaButtonFilesize+ i, "0,0 KiB");
            speditor.putString(SharedPrefKeys.mensaButtonLabel + i, "");
            speditor.putString(SharedPrefKeys.mensaButtonShortLabel + i, "");
            speditor.putString(SharedPrefKeys.mensaButtonFilename + i, "");
        }

        // Delete the time of the last News update from the Shared Prefs
        speditor.putString(SharedPrefKeys.newsLastUpdate, "");

        // Apply the changes to the shared preferences
        speditor.commit();

        // Remove *all* files from the data directory
        File[] files = fileDirectory.listFiles();
        for(File f : files) { f.delete(); }
    }



    /**
     * Replace the "real" credentials by invented, non-existing ones
     */
    void clearCredentials() {
        speditor.putLong(SharedPrefKeys.vplanLastUpdate, 0);
        speditor.putString(SharedPrefKeys.credUsername, "dadadidada");
        speditor.putString(SharedPrefKeys.credPassword, "blubblub");
        speditor.putString(SharedPrefKeys.vplanPath, "");
        speditor.commit();
    }
}
