package de.mwg_bayreuth.mwgorganizer;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.URL;

public class LoginActivity extends AppCompatActivity {
    private ProgressDialog progDialog;
    private String path_vertretungsplan;
    private String username;
    private String password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        progDialog = new ProgressDialog(this);
        progDialog.setTitle(getApplicationContext().getResources().getString(R.string.login_checkCred));
    }

    public boolean connectedToInternet() {
        // TODO: Check for internet connection
//        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
//        NetworkTools nettools = new NetworkTools(cm);
//        return nettools.homepageReachable();
        return true;
    }

    public void login(View view) {
        if(!connectedToInternet()) {
            String noconnectionmssg = getApplicationContext().getResources().getString(R.string.general_nointernetconnection);

            // Build a "no connection" dialog
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setTitle(noconnectionmssg);
            alertDialogBuilder
                .setCancelable(true)
                // close dialog when clicking this button
                .setPositiveButton("OK",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) { dialog.cancel(); }
                });

            // create alert dialog
            AlertDialog alertDialog = alertDialogBuilder.create();

            // show it
            alertDialog.show();

            return;
        }

        EditText un = (EditText) findViewById(R.id.InputUsername);
        EditText pw = (EditText) findViewById(R.id.InputPW);
        username = un.getText().toString();
        password = pw.getText().toString();

        CheckCredentialsTask checkCredTask = new CheckCredentialsTask(this);
        checkCredTask.execute(username, password);
    }

    public void saveLoginData(String username, String password) {
        SharedPreferences sharedPref = getSharedPreferences(SharedPrefKeys.spPrefix, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(SharedPrefKeys.credUsername, username);
        editor.putString(SharedPrefKeys.credPassword, password);
        editor.putString(SharedPrefKeys.vplanPath, path_vertretungsplan);
        editor.commit();
        finish(); // Close the activity
    }

    public void showLetters(View view) {
        boolean checked = ((CheckBox) view).isChecked();
        EditText pw = (EditText) findViewById(R.id.InputPW);
        if(checked) {
            pw.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        } else {
            pw.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
    }

    private class CheckCredentialsTask extends AsyncTask<String, Void, Boolean> {
        private static final String get_url = "https://www.mwg-bayreuth.de/Login.html";
        private static final String post_url = "https://www.mwg-bayreuth.de/Login.html";

        private LoginActivity root;
        
        CheckCredentialsTask(LoginActivity root) { this.root = root; }
        
        @Override
        protected void onPreExecute() {
            progDialog.show();
            return;
        }

        @Override
        protected void onPostExecute(Boolean isValid) {
            progDialog.dismiss();
            if(isValid) { saveLoginData(username, password); }
            else {
                String invalidcredmssg = getApplicationContext().getResources().getString(R.string.login_invalidcred);

                // Build a "invalid credentials" dialog
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(root);
                alertDialogBuilder.setTitle(invalidcredmssg);
                alertDialogBuilder
                        .setCancelable(true)
                        // close dialog when clicking this button
                        .setPositiveButton("OK",new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) { dialog.cancel(); }
                        });

                // create alert dialog
                AlertDialog alertDialog = alertDialogBuilder.create();

                // show it
                alertDialog.show();
            }
            return;
        }


        @Override
        protected Boolean doInBackground(String... params) {
            String username = params[0];
            String password = params[1];
            try {
                CookieManager manager = new CookieManager();
                CookieHandler.setDefault(manager);

                // extract the individual request token from the first page
                String REQUEST_TOKEN = "";
                URL url = new URL(get_url);

                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");

                int responseCode = con.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) { // success
                    REQUEST_TOKEN = getRequestToken(con);
                } else { Log.e("test", "GET request not worked"); }

                // Post the login data
                URL obj = new URL(post_url);
                con = (HttpURLConnection) obj.openConnection();
                con.setRequestMethod("POST");

                String urlParameters = "FORM_SUBMIT=tl_login&REQUEST_TOKEN=" + REQUEST_TOKEN + "&username=" + username + "&password=" + password;

                con.setDoOutput(true);

                OutputStream os = con.getOutputStream();
                os.write(urlParameters.getBytes());
                os.flush();
                os.close();

                // If the output stream contains this line, the login has failed
                String aim = "Bitte Loggen Sie sich ein";

                // Don't remove the following line:
                con.getResponseCode();

                obj = new URL("https://www.mwg-bayreuth.de/vertretungsplan.html");
                con = (HttpURLConnection) obj.openConnection();
                con.setRequestMethod("GET");

                responseCode = con.getResponseCode();

                boolean isValid = true;


                if (responseCode == HttpURLConnection.HTTP_OK) { // success
                    Log.d("onlineHelper", "check");
                    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    String inputLine;


                    while ((inputLine = in.readLine()) != null) {
                        if (inputLine.contains(aim)) { isValid = false; }

                        String vertretungsplanHref = ".*menu_Elemente.*ertretungsplan.html.*";

                        // correct credentials: save the path of the Vertreungsplan page
                        // Log.e("dynamic", inputLine);

                        if(inputLine.matches(vertretungsplanHref)) {
                            // inputLine has the following format:
                            // menu_Elemente['xy']['href'] = 'Vertretungsplan.html';

                            path_vertretungsplan = inputLine.split("'")[5];

                            Log.e("dynamic", path_vertretungsplan);
                        }
                    }
                    in.close();

                    return isValid;
                } else { return false; }
            } catch (Exception e) { return false; }
        }

        private String getRequestToken(HttpURLConnection con) throws Exception {
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            String requestToken = "";
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.contains("REQUEST_TOKEN")) {
                    requestToken = inputLine.substring(49, 81);
                }
            }
            in.close();
            return requestToken;
        }
    }

}
