package de.mwg_bayreuth.mwgorganizer;


import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;


/**
 * An extension class to GetFileToolkits
 * This toolkit provides features to download Vertretungsplan files
 */
class GetVertretungsplanToolkit extends GetFileToolkits {
    private CacheManager cachemanager;
    private int foundfilescount;
    private int updatedFilesNr;



    /**
     *
     * @param sharedPref - an instance of SharedPreferences required to get sharedPref values
     * @param speditor - an instance of an SharedPreferences.Editor to edit sharedPref values
     * @param cachemanager - an instance of a CacheManager
     * @param dialog - an instance of a progress dialog used to display the progress
     * @param filedir - the path to the directory containing all application files
     * @param root - the activity which requires the toolkit
     */
    GetVertretungsplanToolkit(SharedPreferences sharedPref, SharedPreferences.Editor speditor,
                              CacheManager cachemanager, ProgressDialog dialog, File filedir,
                              MWGOrganizer root) {
        setUpReferences(sharedPref, speditor, dialog, filedir, root);
        this.cachemanager = cachemanager;
        updatedFilesNr = -1;
        new UpdateFilesTask().execute();
    }


    /**
     * Redirect to the root fragment
     */
    private void openLogin() { root.openLogin(); }



    /**
     * Download the HTML source of the Vertretungsplan page, search for downloadable files
     * and set up the list buttons
     */
    private class UpdateFilesTask  extends AsyncTask<Void, String, Integer> {
        String get_url  = "https://www.mwg-bayreuth.de/Login.html";
        String post_url = "https://www.mwg-bayreuth.de/Login.html";

        ArrayList<PDFFile> localFiles;


        /**
         * Preparation:
         * Reset the file counter & show the dialog
         * Cancel when no/wrong credentials have been given
         */
        @Override
        protected void onPreExecute() {
            String username = sharedPref.getString(SharedPrefKeys.credUsername, "dadadidada");
            String password = sharedPref.getString(SharedPrefKeys.credPassword, "blubblub");

            if((username.equals("dadadidada")) || (password.equals("blubblub"))) {
                openLogin();
                cancel(true);
            } else { dialog.show(); }

            foundfilescount = 0;
            return;
        }


        /**
         * After updating files:
         *     a) Wrong credentials: Open Login screen
         *     b) Update succeeded: Set up buttons & time label, delete old files
         * @param responseCode - 13 when update failed, succeeded with other value
         */
        @Override
        protected void onPostExecute(Integer responseCode) {
            dialog.dismiss();

            if(responseCode == 13) { // Wrong credentials: open Login
                cachemanager.clearCredentials();
                openLogin();
            } else { // Create buttons, set time label, delete old files
                root.setupButtons();
                setLastUpdateTimeLabel();
                deleteFiles();
                root.showUpdateSummary(false, updatedFilesNr);
            }
        }


        /**
         * Action for updating the progress
         * @param filename - The name of the currently downloaded file
         */
        @Override
        protected void onProgressUpdate(String... filename) {
            dialog.setMessage(filename[0]);
        }


        /**
         * Provides a callback for fetchPDF to update the name of the currently downloaded file
         * @param filename - the name of the currently downloaded file
         */
        void updateFilename(String filename) { publishProgress(filename); }


        /**
         * Download the HTML file, parse it and trigger fetchPDF
         * @param params - some parameters
         * @return 13, when update failed; 42, when update has been performed recently; otherwise, any other value
         */
        @Override
        protected Integer doInBackground(Void... params) {
            // Configure progress indicator
            dialog.setProgress(0);
            String connectstr = root.getApplicationContext()
                                    .getResources().getString(R.string.progress_connect);
            publishProgress(connectstr);
            dialog.setProgressNumberFormat("");

            // Initial value for return value
            int res = 1;


            ArrayList<PDFFile> localFiles = new ArrayList<PDFFile>();

            int totalFiles = sharedPref.getInt(SharedPrefKeys.vplanFileNr, 0);
            for(int i = 0; i < totalFiles; i++) {
                String  filename = sharedPref.getString(SharedPrefKeys.vplanFileFilename + i, "");
                String  label    = sharedPref.getString(SharedPrefKeys.vplanFileLabel + i, "");
                String  size     = sharedPref.getString(SharedPrefKeys.vplanFileFilesize + i, "");
                boolean isVplan  = sharedPref.getBoolean(SharedPrefKeys.vplanFileIsVplan + i, false);
                PDFFile f        = new PDFFile("", filename, label, "", size, isVplan);
                localFiles.add(f);
            }
            this.localFiles = localFiles;


            ArrayList<PDFFile> foundVplanFiles = new ArrayList<PDFFile>();
            ArrayList<PDFFile> foundInfoFiles  = new ArrayList<PDFFile>();
            ArrayList<PDFFile> updatedFiles    = new ArrayList<PDFFile>();
            
            try {
                String username = sharedPref.getString(SharedPrefKeys.credUsername, "dadadidada");
                String password = sharedPref.getString(SharedPrefKeys.credPassword, "blubblub");

                // Calculate the time passed since the last update
                Long lastUpdate = sharedPref.getLong(SharedPrefKeys.vplanLastUpdate, 0);
                Long now = System.currentTimeMillis();
                Long timeSinceLastUpdate = now - lastUpdate;

                // Check whether the update has been forced by triggering the update button
                boolean forceUpdate = sharedPref.getBoolean(SharedPrefKeys.vplanForceUpdate, false);
                speditor.putBoolean(SharedPrefKeys.vplanForceUpdate, false);
                speditor.commit();

                // Don't update when last update less than 20 min ago and update wasn't forced
                // Have fun finding the relation between 42 and 237 :D
                if((timeSinceLastUpdate < 20 * 60 * 1000) && !forceUpdate) { updatedFilesNr = 237; return 42; }

                // Preparation for login: Create a cookie manager
                CookieManager manager = new CookieManager();
                CookieHandler.setDefault(manager);

                // Create an URL connection
                URL url = new URL(get_url);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                int responseCode = con.getResponseCode();

                // Extract the individual REQUEST_TOKEN from the first page
                String REQUEST_TOKEN = "";
                if (responseCode == HttpURLConnection.HTTP_OK) { // success
                    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    String inputLine;

                    while ((inputLine = in.readLine()) != null) {
                        if (inputLine.contains("REQUEST_TOKEN")) {
                            REQUEST_TOKEN = inputLine.substring(49, 81);
                        }
                    }
                    in.close();
                } else { updatedFilesNr = -1; Log.e("test", "GET request failed"); }



                // Login
                URL obj = new URL(post_url);
                con = (HttpURLConnection) obj.openConnection();
                con.setRequestMethod("POST");
                String urlParameters = "FORM_SUBMIT=tl_login&REQUEST_TOKEN=" + REQUEST_TOKEN + "&username=" + username + "&password=" + password;
                con.setDoOutput(true); // For POST only - START

                OutputStream os = con.getOutputStream();
                os.write(urlParameters.getBytes());
                os.flush();
                os.close();
                con.getResponseCode();


                // Connect to the Vertretungsplan HTML-Page
                obj = new URL("https://www.mwg-bayreuth.de/" + sharedPref.getString(SharedPrefKeys.vplanPath, "vertretungsplan.html"));
                con = (HttpURLConnection) obj.openConnection();
                con.setRequestMethod("GET");


                responseCode = con.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) { // success
                    // Prepare parsing the HTML input
                    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    String inputLine;
                    String aim  = ".*tl_files.*pdf.*";                     // Pattern for PDF-Files
                    String uaim = ".*apk.*";                               // Pattern for APK-Files
                    String doom = "<h1>Bitte Loggen Sie sich ein</h1>";    // Straight out Login-Page



                    String searchfilesstr = root.getApplicationContext()
                                                .getResources().getString(R.string.progress_searchforfiles);
                    publishProgress(searchfilesstr);


                    boolean newVersionAvailable = false;

                    while ((inputLine = in.readLine()) != null) {
                        // Login verkackt, falls der String doom auftaucht
                        if (inputLine.matches(doom)) { res = 13; }


                        // TODO: Re-Activate version control stuff
                        // Found hyperlink containing an APK-file
                        if (inputLine.matches(uaim)) {
                            HTMLParser p = new HTMLParser(inputLine);
                            String label = p.getLabel();

                            // newVersionAvailable = true;
                        }

                        // Unset update flag when no newer version is available
                        /** if(!newVersionAvailable) {
                            speditor.putBoolean(SharedPrefKeys.UpdateFlag, false);
                            speditor.commit();
                        }*/

                        // Found hyperlink containing an PDF file
                        if (inputLine.matches(aim)) {
                            // Determine path, filename, -label and -size
                            HTMLParser p = new HTMLParser(inputLine);

                            PDFFile f = new PDFFile(p.getPath(), p.getFilename(), p.getLabel(), p.getLabel(), p.getSize(), p.getIsVplan());

                            if(p.getIsVplan()) { foundVplanFiles.add(f); }
                            else               { foundInfoFiles.add(f); }

                            if(downloadFile(f)) {
                                foundfilescount += 1;

                                String filesfoundstr = root.getApplicationContext()
                                        .getResources().getString(R.string.progress_filesfound);
                                dialog.setProgressNumberFormat(foundfilescount + " " + filesfoundstr);

                                updatedFiles.add(f);
                            }
                        }

                        int i = 0;

                        // Write information to SharedPrefs
                        for(PDFFile file: foundVplanFiles) {
                            speditor.putString(SharedPrefKeys.vplanFileLabel + i, file.label);
                            speditor.putString(SharedPrefKeys.vplanFileFilename + i, file.filename);
                            speditor.putBoolean(SharedPrefKeys.vplanFileIsVplan + i, file.isVplan);
                            if(file.size != null) { speditor.putString(SharedPrefKeys.vplanFileFilesize + i, file.size); }
                            i++;
                        }

                        for(PDFFile file: foundInfoFiles) {
                            speditor.putString(SharedPrefKeys.vplanFileLabel + i, file.label);
                            speditor.putString(SharedPrefKeys.vplanFileFilename + i, file.filename);
                            speditor.putBoolean(SharedPrefKeys.vplanFileIsVplan + i, file.isVplan);
                            if(file.size != null) { speditor.putString(SharedPrefKeys.vplanFileFilesize + i, file.size); }
                            i++;
                        }

                        speditor.putInt(SharedPrefKeys.vplanFileNr, i);
                        speditor.commit();
                    }
                    in.close();

                    if(res != 13) {
                        // Update sucessful (?): download files, save button nr & update time

                        updatedFilesNr = 0;
                        if(foundfilescount != 0) {
                            String fdlprepstr = root.getApplicationContext()
                                                    .getResources().getString(R.string.progress_filedlprep);
                            publishProgress(fdlprepstr);

                            int filesInSPrefs = sharedPref.getInt(SharedPrefKeys.vplanFileNr, 0);

                            for(PDFFile file: updatedFiles) {
                                for(int i = 0; i < filesInSPrefs; i++) {
                                    String filenameFromSPs = sharedPref.getString(SharedPrefKeys.vplanFileFilename + i, "");

                                    if (file.filename.equals(filenameFromSPs)) {
                                        speditor.putBoolean(SharedPrefKeys.vplanFileDownloaded + i, false);
                                        speditor.commit();
                                    }
                                }
                            }
                        }

                        speditor.putInt(SharedPrefKeys.vplanUpdatedFilesNr, foundfilescount);
                        speditor.putLong(SharedPrefKeys.vplanLastUpdate, now);
                        speditor.commit();
                    } else {
                        // Wrong credentials: avoid consequential errors
                        speditor.putInt(SharedPrefKeys.vplanUpdatedFilesNr, 0);
                        speditor.putInt(SharedPrefKeys.vplanFileNr, 0);
                        speditor.commit();
                    }
                } else { updatedFilesNr = -1; }
            } catch (Exception e) { updatedFilesNr = -1; e.printStackTrace(); }

            finally {
                dialog.setMax(1);
                dialog.setProgress(1);
                return res;
            }
        }


        /**
         * Checks whether a file should be downloaded:
         *   - download, if file is not present on device
         *   - download, if file size (if given) has changed
         *   - don't download, if file download skipped, broken, etc.
         * @param f - a PDFFile element
         * @return whether to download this file or not
         */
        private boolean downloadFile(PDFFile f) {
            for(PDFFile file: localFiles) {
                if(file.filename.equals(f.filename)) {
                    return !file.size.equals(f.size);
                }
            }

            return true;
        }
    }



    /**
     *
     * @param filesToDownload - a list containing all files that should be downloaded
     * @param root - the root FetchHtmlTask
     */
    private void fetchPDF(ArrayList<PDFFile> filesToDownload,
                          de.mwg_bayreuth.mwgorganizer.GetVertretungsplanToolkit.UpdateFilesTask root) {
        try {
            String get_url  = "https://www.mwg-bayreuth.de/Login.html";
            String post_url = "https://www.mwg-bayreuth.de/Login.html";
            String pdf_url  = "https://www.mwg-bayreuth.de/";

            String username = sharedPref.getString(SharedPrefKeys.credUsername, "dadadidada");
            String password = sharedPref.getString(SharedPrefKeys.credPassword, "blubblub");


            CookieManager manager = new CookieManager();
            CookieHandler.setDefault(manager);


            URL url = new URL(get_url);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();

            con.setRequestMethod("GET");

            int responseCode = con.getResponseCode();
            Log.e("test", "GET Response Code :: " + responseCode);

            // extract the individual request token from the first page
            String REQUEST_TOKEN = "";
            if (responseCode == HttpURLConnection.HTTP_OK) { // success
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;

                while ((inputLine = in.readLine()) != null) {
                    if (inputLine.contains("REQUEST_TOKEN")) {
                        Log.e("test", inputLine);
                        REQUEST_TOKEN = inputLine.substring(49, 81);
                    }
                }
                in.close();
            } else { updatedFilesNr = -1; Log.e("test", "GET request not worked"); }



            URL obj = new URL(post_url);
            con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("POST");

            // Login
            String urlParameters = "FORM_SUBMIT=tl_login&REQUEST_TOKEN=" + REQUEST_TOKEN + "&username=" + username + "&password=" + password;
            con.setDoOutput(true); // For POST only - START

            OutputStream os = con.getOutputStream();
            os.write(urlParameters.getBytes());
            os.flush();
            os.close();

            responseCode = con.getResponseCode();
            Log.e("test", "POST Response Code :: " + responseCode);


            // Download all files in one go
            for(PDFFile file: filesToDownload) {
                String path = file.path;
                String filename = file.filename;
                String filelabel = file.label;

                obj = new URL(pdf_url + path);
                con = (HttpURLConnection) obj.openConnection();
                con.setRequestMethod("GET");

                responseCode = con.getResponseCode();
                Log.e("test", "GET Response Code :: " + responseCode);


                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream in = con.getInputStream();
                    int fileSize   = con.getContentLength();

                    root.updateFilename(filelabel);
                    dialog.setProgress(0);
                    dialog.setMax(fileSize);
                    dialog.setProgressNumberFormat((updatedFilesNr + 1) + "/" + foundfilescount);


                    FileOutputStream fos = new FileOutputStream(new File(filedir + "/" + filename));


                    byte[] buffer = new byte[1024];

                    int fragmentLength;
                    int alreadyDownloaded = 0;

                    while ((fragmentLength = in.read(buffer)) > -1) {
                        alreadyDownloaded += fragmentLength;
                        dialog.setProgress(alreadyDownloaded);
                        fos.write(buffer, 0, fragmentLength);
                    }
                    fos.close();
                    in.close();


                    int filesInSPrefs = sharedPref.getInt(SharedPrefKeys.vplanFileNr, 0);

                    for(int i = 0; i < filesInSPrefs; i++) {
                        String filenameFromSPs = sharedPref.getString(SharedPrefKeys.vplanFileFilename + i, "");

                        if(filename.equals(filenameFromSPs)) {
                            speditor.putBoolean(SharedPrefKeys.vplanFileUpdated + i, true);
                            speditor.putBoolean(SharedPrefKeys.vplanFileDownloaded + i, true);
                            speditor.commit();
                        }
                    }


                    updatedFilesNr++;

                    Log.e("test", "File downloaded");
                } else { Log.e("test", "GET PDF request not worked"); }
            }
        } catch (Exception e) { updatedFilesNr = -1; e.printStackTrace(); }
    }
    
    
    
    private class PDFFile {
        String  path;
        String  filename;
        String  label;
        String  shortlabel;
        String  size;
        boolean isVplan;
        
        PDFFile(String path, String filename, String label, String shortlabel, String size, boolean isVplan) {
            this.path = path;
            this.filename = filename;
            this.label = label;
            this.shortlabel = label;
            this.size = size;
            this.isVplan = isVplan;
        }
    }
}