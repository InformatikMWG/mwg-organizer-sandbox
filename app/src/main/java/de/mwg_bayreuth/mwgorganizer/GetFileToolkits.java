package de.mwg_bayreuth.mwgorganizer;


import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import java.io.File;

/**
 * Implements a basic set of methods required by the GetFileToolkits (Vertreungsplan & Mensa)
 */
abstract class GetFileToolkits {
    SharedPreferences sharedPref;
    SharedPreferences.Editor speditor;
    ProgressDialog dialog;
    File filedir;
    MWGOrganizer root;



    /**
     * Set up reference attributes for callbacks to the creator classes
     * @param sharedPref - an instance of SharedPreferences required to get sharedPref values
     * @param speditor - an instance of an SharedPreferences.Editor to edit sharedPref values
     * @param dialog - an instance of a progress dialog used to display the progress
     * @param filedir - the path to the directory containing all application files
     * @param root - the fragment which requires the toolkit
     */
    void setUpReferences(SharedPreferences sharedPref, SharedPreferences.Editor speditor,
                         ProgressDialog dialog, File filedir, MWGOrganizer root) {
        this.sharedPref = sharedPref;
        this.speditor = speditor;
        this.dialog = dialog;
        this.filedir = filedir;
        this.root = root;
    }



    /**
     * Callbacks for executing fragment-specific behavior
     */
    void setupButtons()           { root.setupButtons(); }
    void setLastUpdateTimeLabel() { root.setLastUpdateTimeLabel(); }



    /**
     * Delete local copies of files being outdated or missing on the homepage
     */
    void deleteFiles() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            /**
             * List all files in the directory and delete them if they are old
             * Exception: The .news file won't be deleted when present
             */
            protected Void doInBackground(Void... params) {
                File[] files = filedir.listFiles();
                for (File f : files) {
                    String filename = f.getName();
                    if (isOldFile(filename) && !(filename.equals(".news"))) {
                        f.delete();
                    }
                }
                return null;
            }


            /**
             * Determines, whether a file is outdated or missing on the homepage
             * @param f - the name of the file to be looked at
             * @return true, when the file may be deleted, false, if the file should be kept
             */
            private boolean isOldFile(String f) {
                boolean isOld = true;

                // Is this an outdated / missing Vertretungsplan?
                int nrVertplanButtons = sharedPref.getInt(SharedPrefKeys.vplanButtonNr, 0);
                for (int i = 0; i < nrVertplanButtons; i++) {
                    if (f.equals(sharedPref.getString(SharedPrefKeys.vplanButtonFilename + i, ""))) {
                        isOld = false;
                        break;
                    }
                }

                // Is this an outdated / missing Mensa lunch schedule?
                int nrMensabuttons = sharedPref.getInt(SharedPrefKeys.mensaButtonNr, 0);
                for (int i = 0; i < nrMensabuttons; i++) {
                    if (f.equals(sharedPref.getString(SharedPrefKeys.mensaButtonFilename + i, ""))) {
                        isOld = false;
                        break;
                    }
                }

                return isOld;
            }
        }.execute();
    }
}
