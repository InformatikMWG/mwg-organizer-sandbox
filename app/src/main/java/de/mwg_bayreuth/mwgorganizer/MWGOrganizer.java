package de.mwg_bayreuth.mwgorganizer;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import java.io.File;

import de.mwg_bayreuth.mwgorganizer.dummy.ListContent;

public class MWGOrganizer extends AppCompatActivity
implements NavigationView.OnNavigationItemSelectedListener,
        // Also implement the interaction listeners of the single fragments
        HomeFragment.OnFragmentInteractionListener,
        VertretungsplanFragment.OnListFragmentInteractionListener {

    CacheManager cachemanager;
    File extDirectory;
    ProgressDialog progDialog;
    SharedPreferences sharedPref;
    SharedPreferences.Editor speditor;
    FSFEnum currentFSF;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mwgorganizer);

        sharedPref = getSharedPreferences(SharedPrefKeys.spPrefix, Context.MODE_PRIVATE);
        speditor = sharedPref.edit();
        extDirectory = getExternalFilesDir(null);
        cachemanager = new CacheManager(speditor, extDirectory);

        progDialog = new ProgressDialog(this);
        progDialog.setCanceledOnTouchOutside(false);
        boolean resistDialogs = true; // TODO: Load from SharedPrefs

        if(resistDialogs) { progDialog.setCancelable(false); }
        progDialog.setTitle(getApplicationContext()
                  .getResources().getString(R.string.general_update));
        progDialog.setMessage(" ");
        progDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progDialog.setIndeterminate(false);


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Configure the floating "Update" button
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(currentFSF != null) {
                    // TODO: Check whether Connection to Homepage is possible
                    if(false) {
                        String noconnectionstr = getApplicationContext().getResources()
                                                                        .getString(R.string.general_nointernetconnection);
                        Snackbar.make(view, noconnectionstr, Snackbar.LENGTH_SHORT)
                                .setAction("Action", null).show();
                    } else { MWGOrganizer.this.updateFiles(view); }
                }
            }
        });


        // Initialize the drawer
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.getMenu().getItem(0).setChecked(true);

        // Only add the "Home"-fragment if the container exists
        if(findViewById(R.id.fragment_container) != null) {
            // Don't do anything when other fragments already exist
            if (savedInstanceState != null) { return; }

            // Create a home fragment and place it in the container
            HomeFragment homefragment = new HomeFragment();
            getSupportFragmentManager().beginTransaction().
                    add(R.id.fragment_container, homefragment).commit();
        }
    }


    /**
     * Change the shown fragment when selecting an item in the menu drawer
     * @param item - the selected drawer item
     * @return true if the action succeded
     */
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        android.support.v4.app.Fragment fragment = null;
        Class fragmentClass = null;
        boolean exchangeFragment = false;

        // Handle navigation view item clicks here.
        switch (item.getItemId()) {
            default:
                fragmentClass = HomeFragment.class;
                exchangeFragment = true;
                currentFSF = null;
                break;
            case R.id.nav_vplan:
                fragmentClass = VertretungsplanFragment.class;
                exchangeFragment = true;
                currentFSF = FSFEnum.VplanFrag;
                break;
            //    case R.id.nav_mensa:

            //    case R.id.nav_news:

            case R.id.nav_about:
                startActivity(new Intent(this, About.class));
                break;
            case R.id.nav_settings:
                startActivity(new Intent(this, Settings.class));
                break;
        }

        // Do *not* execute this code when an Activity has been opened!!
        if(exchangeFragment) {
            // Replace the currently shown fragment
            try { fragment = (android.support.v4.app.Fragment) fragmentClass.newInstance(); }
            catch (Exception e) { e.printStackTrace(); }
            getSupportFragmentManager().beginTransaction()
                                       .replace(R.id.fragment_container, fragment).commit();
            // Change the activity title
            setTitle(item.getTitle());
        }

        // Close the drawer
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    /**
     * Mechanism for updating files
     * Executed when floating button is triggered and internet connection to homepage exists
     */
    public void updateFiles(View view) {
        int updatedFiles = -1;
        boolean showUpdateSummary = false;

        // Check for intenet connection performed in floating action button listener
        if(currentFSF == FSFEnum.VplanFrag) {
            speditor.putBoolean(SharedPrefKeys.vplanForceUpdate, true);
            speditor.commit();
            GetVertretungsplanToolkit gvt = new GetVertretungsplanToolkit(
                    sharedPref, speditor, cachemanager, progDialog, extDirectory, this);
            //updatedFiles = gvt.getUpdatedFiles();
            //showUpdateSummary = true;
        } else if (currentFSF == FSFEnum.MplanFrag) {
            speditor.putBoolean(SharedPrefKeys.mensaForceUpdate, true);
            speditor.commit();
            // TODO: Create an getFilesToolkit (has to be generated) and execute it
            // TODO: Show update summary
        } else if (currentFSF == FSFEnum.NewsFrag) {
            speditor.putBoolean(SharedPrefKeys.newsForceUpdate, true);
            speditor.commit();
            // TODO: Create an getFilesToolkit (has to be generated) and execute it
        }

        /**
        if(showUpdateSummary) {
            String updatedFilesStr;
            // TODO: Use translatable strings
            if(updatedFiles == -1) {
                // All files up to date
                updatedFilesStr = "Alle Dateien aktuell";
            } else {
                // Some files have been updated
                updatedFilesStr = updatedFiles + " Dateien aktualisiert";
            }
            Snackbar.make(view, updatedFilesStr, Snackbar.LENGTH_SHORT)
                    .setAction("Action", null).show();
        }
        */
    }


    public void setupButtons() {
        if(currentFSF == FSFEnum.VplanFrag) {
            // TODO: Set up buttons for the Vertretungsplan fragment (see #3)
        }
    }


    public void openLogin() { startActivity(new Intent(this, LoginActivity.class)); }

    public void setLastUpdateTimeLabel() {}

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        // Drawer opened -> Close drawer; Drawer closed -> Close application
        if (drawer.isDrawerOpen(GravityCompat.START)) { drawer.closeDrawer(GravityCompat.START); }
        else {
            // TODO: App erst schließen, wenn Zurücktaste 2x schnell hintereinander gedrückt wurde
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.mwgorganizer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch(item.getItemId()) {
            case R.id.action_about:
                startActivity(new Intent(this, About.class));
                return true;

            case R.id.action_settings:
                startActivity(new Intent(this, Settings.class));
                return true;

            case R.id.action_dev_shit:
                startActivity(new Intent(this, DisplayPDF.class));
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    // Has to be overridden, otherwise nasty crashes occurr
    @Override
    public void onFragmentInteraction(Uri uri) {}

    @Override
    public void onListFragmentInteraction(ListContent.Item item) {}


    private enum FSFEnum { VplanFrag, MplanFrag, NewsFrag }
}
