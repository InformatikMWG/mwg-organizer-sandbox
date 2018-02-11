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
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;


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
        updatedFiles = 0;
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
                setupButtons();
                setLastUpdateTimeLabel();
                deleteFiles();
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
         * @return 13, when update failed; otherwise, any other value
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
                if((timeSinceLastUpdate < 20 * 60 * 1000) && !forceUpdate) { return res; }

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
                    StringBuffer response = new StringBuffer();

                    while ((inputLine = in.readLine()) != null) {
                        if (inputLine.contains("REQUEST_TOKEN")) {
                            REQUEST_TOKEN = inputLine.substring(49, 81);
                        }
                    }
                    in.close();
                } else { Log.e("test", "GET request failed"); }

                CookieStore cookieJar = manager.getCookieStore();
                List<HttpCookie> cookies = cookieJar.getCookies();
                String phpsessid = "";
                for (HttpCookie cookie : cookies) { phpsessid = cookie.toString(); }

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

                    // Array passed to FetchPDF
                    String[][] foundFiles = new String[20][3];

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

                            // Fetch the old file size from the SharedPref
                            String oldSize = sharedPref.getString(SharedPrefKeys.vplanButtonFilesize + i, "");

                            boolean noSizeGiven = false;
                            boolean fetchFile   = false;

                            // no file size given: check file name
                            if(size == null) {
                                noSizeGiven = true;

                                String oldFilename = sharedPref.getString(SharedPrefKeys.vplanButtonFilename + i, "dadadidada blubblub");
                                if(!oldFilename.equals(filename)) { fetchFile = true; }
                            } else if (!oldSize.equals(size)) { fetchFile = true;}

                            // old size =/= current size: add PDF file to "FETCH!!"-list
                            if(fetchFile) {
                                foundfilescount += 1;

                                String filesfoundstr = root.getApplicationContext()
                                                           .getResources().getString(R.string.progress_filesfound);
                                dialog.setProgressNumberFormat(foundfilescount + " " + filesfoundstr);

                                foundFiles[foundfilescount-1][0] = path;
                                foundFiles[foundfilescount-1][1] = filename;
                                foundFiles[foundfilescount-1][2] = label;

                                speditor.putBoolean(SharedPrefKeys.vplanButtonFileUpdated + i, true);
                            }


                            // Write information to SharedPrefs
                            // TODO: Support short labels
                            speditor.putString(SharedPrefKeys.vplanButtonLabel + i, label);
                            speditor.putString(SharedPrefKeys.vplanButtonFilename + i, filename);

                            if(!noSizeGiven) { speditor.putString(SharedPrefKeys.vplanButtonFilesize + i, size); }

                            i++;
                        }
                    }
                    in.close();

                    if(res != 13) {
                        // Update sucessful (?): download files, save button nr & update time

                        if(foundfilescount != 0) {
                            String fdlprepstr = root.getApplicationContext()
                                                    .getResources().getString(R.string.progress_filedlprep);
                            publishProgress(fdlprepstr);
                            fetchPDF(foundFiles, foundfilescount, this);
                        }
                        
                        speditor.putInt(SharedPrefKeys.vplanButtonNr, i);
                        speditor.putLong(SharedPrefKeys.vplanLastUpdate, now);
                        speditor.commit();
                    } else {
                        // Wrong credentials: avoid consequential errors
                        speditor.putInt(SharedPrefKeys.vplanButtonNr, 0);
                        speditor.commit();
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }

            finally {
                dialog.setMax(1);
                dialog.setProgress(1);
                return new Integer(res);
            }
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
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    if (inputLine.contains("REQUEST_TOKEN")) {
                        Log.e("test", inputLine);
                        REQUEST_TOKEN = inputLine.substring(49, 81);
                    }
                }
                in.close();
            } else { Log.e("test", "GET request not worked"); }

            CookieStore cookieJar = manager.getCookieStore();
            List<HttpCookie> cookies = cookieJar.getCookies();
            String phpsessid = "";
            for (HttpCookie cookie : cookies) { phpsessid = cookie.toString(); }

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
                    InputStream in   = con.getInputStream();
                    int lengthOfFile = con.getContentLength();

                    // TODO: Find a way of changing the displayed file name
                    // Directly accessing dialog.setMessage is not possible
                    root.updateFilename(filelabel);
                    dialog.setProgress(0);
                    dialog.setMax(lengthOfFile);
                    dialog.setProgressNumberFormat(foundfilescount + " / " + foundfilescount);


                    FileOutputStream fos = new FileOutputStream(new File(filedir + "/" + filename));

                    int length = -1;
                    byte[] buffer = new byte[1024];

                    int total = 0;

                    while ((length = in.read(buffer)) > -1) {
                        total += length;
                        dialog.setProgress(total);
                        fos.write(buffer, 0, length);
                    }
                    fos.close();
                    in.close();

                    updatedFiles++;

                    Log.e("test", "File downloaded");
                } else { Log.e("test", "GET PDF request not worked"); }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }


    /**
     *
     */
    int getUpdatedFiles() { return updatedFiles; }
}



