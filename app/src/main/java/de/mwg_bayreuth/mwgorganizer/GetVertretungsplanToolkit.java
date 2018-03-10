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


/**
 * An extension class to GetFileToolkits
 * This toolkit provides features to download Vertretungsplan files
 */
class GetVertretungsplanToolkit extends GetFileToolkits {
    private CacheManager cachemanager;
    private int foundfilescount;
    private int updatedFiles;



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
        updatedFiles = -1;
        new FetchHtmlTask().execute();
    }


    /**
     * Redirect to the root fragment
     */
    private void openLogin() { root.openLogin(); }



    /**
     * Download the HTML source of the Vertretungsplan page, search for downloadable files
     * and trigger FetchPDF
     */
    private class FetchHtmlTask  extends AsyncTask<Void, String, Integer> {
        String get_url = "https://www.mwg-bayreuth.de/Login.html";
        String post_url = "https://www.mwg-bayreuth.de/Login.html";

        String[][] localFiles; // x/0: path, x/1: filename, x/2: label, x/3: downloaded?
        int localFilesNumber;


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
                root.showUpdateSummary(false, updatedFiles);
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
                if((timeSinceLastUpdate < 20 * 60 * 1000) && !forceUpdate) { updatedFiles = 237; return 42; }

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
                } else { updatedFiles = -1; Log.e("test", "GET request failed"); }



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
                    int i = 0;


                    boolean newVersionAvailable = false;

                    String searchfilesstr = root.getApplicationContext()
                                                .getResources().getString(R.string.progress_searchforfiles);
                    publishProgress(searchfilesstr);

                    // Prepare array passed to FetchPDF
                    String[][] foundFiles = new String[20][3]; // x/0: path, x/1: filename, x/2: label

                    // Copy list of currently downloaded files to internal array
                    localFiles = new String[20][3]; // x/0: filename, x/1: size, x/2: downloaded?
                    localFilesNumber = sharedPref.getInt(SharedPrefKeys.vplanFileNr, 0);
                    for(int j = 0; j < localFilesNumber; j++) {
                        localFiles[j][0] = sharedPref.getString(SharedPrefKeys.vplanFileFilename + j, "dadadidada");
                        localFiles[j][1] = sharedPref.getString(SharedPrefKeys.vplanFileFilesize + j, "moinmoin");
                        if(sharedPref.getBoolean(SharedPrefKeys.vplanFileDownloaded + j, false)) { localFiles[j][2] = "true"; }
                        else { localFiles[j][2] = "false"; }
                    }



                    while ((inputLine = in.readLine()) != null) {
                        // Login verkackt, falls der String doom auftaucht
                        if (inputLine.matches(doom)) { res = 13; }


                        // TODO: Re-Activate version control stuff
                        // Found hyperlink containing an APK-file
                        if (inputLine.matches(uaim)) {
                            HTMLParser p = new HTMLParser(inputLine);
                            String label = p.getLabel();

                            /**
                            if(UpdateTrigger.updateAvailable(label)) {
                                // Update available
                                editor.putBoolean(SharedPrefKeys.UpdateFlag, true);
                                editor.commit();
                                newVersionAvailable = true;
                            }
                            */
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
                            String path = p.getPath();
                            String filename = p.getFilename();
                            String label = p.getLabel();
                            String size = p.getSize();
                            boolean isVplan = p.getIsVplan();


                            if(downloadFile(filename, size)) {
                                foundfilescount += 1;

                                String filesfoundstr = root.getApplicationContext()
                                        .getResources().getString(R.string.progress_filesfound);
                                dialog.setProgressNumberFormat(foundfilescount + " " + filesfoundstr);

                                foundFiles[foundfilescount-1][0] = path;
                                foundFiles[foundfilescount-1][1] = filename;
                                foundFiles[foundfilescount-1][2] = label;

                                // TODO: Find better position for these lines
                                speditor.putBoolean(SharedPrefKeys.vplanFileUpdated + i, true);
                                speditor.putBoolean(SharedPrefKeys.vplanFileDownloaded + i, true);
                            }

                            // Write information to SharedPrefs
                            // TODO: Move this, support short labels
                            speditor.putString(SharedPrefKeys.vplanFileLabel + i, label);
                            speditor.putString(SharedPrefKeys.vplanFileFilename + i, filename);
                            speditor.putBoolean(SharedPrefKeys.vplanFileIsVplan + i, isVplan);

                            if(size != null) { speditor.putString(SharedPrefKeys.vplanFileFilesize + i, size); }

                            i++;
                        }
                    }
                    in.close();

                    if(res != 13) {
                        // Update sucessful (?): download files, save button nr & update time

                        updatedFiles = 0;
                        if(foundfilescount != 0) {
                            String fdlprepstr = root.getApplicationContext()
                                                    .getResources().getString(R.string.progress_filedlprep);
                            publishProgress(fdlprepstr);
                            fetchPDF(foundFiles, foundfilescount, this);
                        }
                        
                        speditor.putInt(SharedPrefKeys.vplanFileNr, i);
                        speditor.putLong(SharedPrefKeys.vplanLastUpdate, now);
                        speditor.commit();
                    } else {
                        // Wrong credentials: avoid consequential errors
                        speditor.putInt(SharedPrefKeys.vplanFileNr, 0);
                        speditor.commit();
                    }
                } else { updatedFiles = -1; }
            } catch (Exception e) { updatedFiles = -1; e.printStackTrace(); }

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
         * @param filename - the name of the PDF file
         * @param size - the size representation from the hyperlink
         * @return whether to download this file or not
         */
        private boolean downloadFile(String filename, String size) {
            for (int i = 0; i < localFilesNumber; i++) {
                if(localFiles[0] != null) {
                    if (localFiles[i][0].equals(filename)) {
                        return size != null && !localFiles[i][1].equals(size) && localFiles[i][2].equals("true");
                    }
                }
            }

            return true;
        }
    }



    /**
     *
     * @param foundFiles - an array containing all found files to be updated
     * @param foundfilescount - the number of found files
     */
    private void fetchPDF(String[][] foundFiles, int foundfilescount,
                          de.mwg_bayreuth.mwgorganizer.GetVertretungsplanToolkit.FetchHtmlTask root) {
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
            } else { updatedFiles = -1; Log.e("test", "GET request not worked"); }



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
            for(int i = 0; i < foundfilescount; i++) {
                String path = foundFiles[i][0];
                String filename = foundFiles[i][1];
                String filelabel = foundFiles[i][2];

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
                    dialog.setProgressNumberFormat((updatedFiles + 1) + "/" + foundfilescount);


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

                    updatedFiles++;

                    Log.e("test", "File downloaded");
                } else { Log.e("test", "GET PDF request not worked"); }
            }
        } catch (Exception e) { updatedFiles = -1; e.printStackTrace(); }
    }
}