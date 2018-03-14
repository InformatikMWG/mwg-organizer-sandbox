package de.mwg_bayreuth.mwgorganizer;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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
import android.widget.TextView;

import java.io.File;
import java.text.SimpleDateFormat;

public class MWGOrganizer extends AppCompatActivity
implements NavigationView.OnNavigationItemSelectedListener,
        // Also implement the interaction listeners of the single fragments
        HomeFragment.OnFragmentInteractionListener,
        FileListFragment.OnListFragmentInteractionListener {

    CacheManager cachemanager;
    File extDirectory;
    ProgressDialog updateFileListDialog;
    ProgressDialog updateNewsDialog;
    SharedPreferences sharedPref;
    SharedPreferences.Editor speditor;
    MainFrags currentFrag;
    FileSelectionFragment fileSelectionFragment;
    Menu drawerMenu;
    boolean doubleBackToExitPressedOnce = false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mwgorganizer);


        sharedPref = getSharedPreferences(SharedPrefKeys.spRoot, Context.MODE_PRIVATE);
        speditor = sharedPref.edit();

        extDirectory = getExternalFilesDir(null);
        cachemanager = new CacheManager(speditor, extDirectory);

        updateFileListDialog = new ProgressDialog(this);
        updateFileListDialog.setCanceledOnTouchOutside(false);
        updateFileListDialog.setCancelable(false);

        String cancelstr = getApplicationContext().getResources().getString(R.string.general_cancel);

        /** TODO: Kill the update progress when pressing this button
        updateFileListDialog.setButton(DialogInterface.BUTTON_NEGATIVE, cancelstr, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO: Cancle file list update progress
            }
        });*/

        updateFileListDialog.setTitle(getApplicationContext()
                .getResources().getString(R.string.general_refreshFileList));
        updateFileListDialog.setMessage(" ");


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Configure the floating button
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            if(currentFrag != null) { MWGOrganizer.this.updateFiles(true); }
            }
        });


        // Initialize the drawer
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        drawerMenu = navigationView.getMenu();
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


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        Class fragmentClass = null;

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        TextView lastUpdateLabel = (TextView) findViewById(R.id.lastUpdateLabel);

        // Handle navigation view item clicks here.
        switch(item.getItemId()) {
            default:
                fragmentClass = HomeFragment.class;
                currentFrag = null;
                fab.setVisibility(View.GONE);
                lastUpdateLabel.setVisibility(View.GONE);
                exchangeFragment(fragmentClass);
                setTitle(getApplicationContext().getResources().getString(R.string.app_name));
                break;
            case R.id.nav_vplan:
                fragmentClass = FileListFragment.class;
                currentFrag = MainFrags.VplanFrag;
                fab.setVisibility(View.VISIBLE);
                lastUpdateLabel.setVisibility(View.VISIBLE);
                exchangeFragment(fragmentClass);
                updateFiles(false);
                setTitle(item.getTitle());
                break;
            case R.id.nav_mensa:
                fragmentClass = FileListFragment.class;
                currentFrag = MainFrags.MplanFrag;
                fab.setVisibility(View.VISIBLE);
                lastUpdateLabel.setVisibility(View.VISIBLE);
                exchangeFragment(fragmentClass);
                //updateFiles(false);
                setTitle(item.getTitle());
                break;

            //case R.id.nav_news:
                //setTitle(item.getTitle());

            case R.id.nav_about:
                startActivity(new Intent(this, About.class)); break;
            case R.id.nav_settings:
                startActivity(new Intent(this, Settings.class)); break;
        }

        // Close drawer
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }



    private void exchangeFragment(Class fragmentClass) {
        android.support.v4.app.Fragment fragment = null;
        try {
            if(fragmentClass == FileListFragment.class) {
                switch(currentFrag) {
                    case VplanFrag: fragment = FileListFragment.newInstance(".vertplan"); break;
                    case MplanFrag: fragment = FileListFragment.newInstance(".mensa");    break;
                    default:        break;
                }

                fileSelectionFragment = (FileSelectionFragment) fragment;
            } else fragment = (android.support.v4.app.Fragment) fragmentClass.newInstance();
        } catch(Exception e) { e.printStackTrace(); }

        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();
        initButtons();
    }


    public void updateFiles(boolean forceUpdate) {
        // Check for internet connection
        NetworkToolkit nettools = new NetworkToolkit(MWGOrganizer.this);
        if(!nettools.networkConnectionAvailable()) {
            // No internet connection: Complain
            Snackbar.make(getWindow().getDecorView().getRootView(), R.string.general_nointernetconnection, Snackbar.LENGTH_SHORT).show();
        } else {
            // set the forceUpdateFlag if necessary
            if(forceUpdate) {
                switch (currentFrag) {
                    case VplanFrag: speditor.putBoolean(SharedPrefKeys.vplanForceUpdate, true); break;
                    case MplanFrag: speditor.putBoolean(SharedPrefKeys.mensaForceUpdate, true); break;
                    case NewsFrag:  speditor.putBoolean(SharedPrefKeys.newsForceUpdate,  true); break;
                    default: break;
                }
                speditor.commit();
            }

            // execute the required getFilesToolkit
            switch(currentFrag) {
                case VplanFrag:
                    new GetVertretungsplanToolkit(sharedPref, speditor, cachemanager, updateFileListDialog, extDirectory, this);
                    break;

                case MplanFrag:
                    // TODO: Create and execute a getMensaplanToolkit

                case NewsFrag:
                    // TODO: Create and execute a getNewsToolkit

                default: break;
            }
        }
    }



    public void showUpdateSummary(boolean isNewsUpdate, int updatedFiles) {
        View view = getWindow().getDecorView().getRootView();
        String snackbarString;

        if(updatedFiles != -1) {
            if (!isNewsUpdate) {
                if(updatedFiles > 0) {
                    String filesupdatedstr = getApplicationContext().getResources().getString(R.string.progress_filesupdated);
                    snackbarString = updatedFiles + " " + filesupdatedstr;
                } else snackbarString = getApplicationContext().getResources().getString(R.string.progress_allfilesuptodate);
            } else {
                // News update: no further information
                snackbarString = getApplicationContext().getResources().getString(R.string.progress_newsupdated);
            }
        } else {
            // Display error string
            snackbarString = getApplicationContext().getResources().getString(R.string.progress_updatefail);
        }

        // Don't show snackbar if update has been performed too recently & has not been forced
        if(updatedFiles != 237) Snackbar.make(view, snackbarString, Snackbar.LENGTH_SHORT).show();
    }


    public void initButtons() {
        if(currentFrag == MainFrags.VplanFrag)
            { fileSelectionFragment.setupButtons(getApplicationContext()); }
    }

    public void setupButtons() {
        // Uses a dirty, but working »hack« :D
        if(currentFrag == MainFrags.VplanFrag) { exchangeFragment(FileListFragment.class); }
    }


    /**
     * Methods for exchanging fragments from buttons in the »Home« fragment
     */
    public void openVertplaene(View view) {
        onNavigationItemSelected(drawerMenu.findItem(R.id.nav_vplan));
        drawerMenu.findItem(R.id.nav_vplan).setChecked(true);
    }
    public void openMensa(View view) {
        onNavigationItemSelected(drawerMenu.findItem(R.id.nav_mensa));
        drawerMenu.findItem(R.id.nav_mensa).setChecked(true);
    }
    public void openNews(View view) {
        onNavigationItemSelected(drawerMenu.findItem(R.id.nav_news));
        drawerMenu.findItem(R.id.nav_news).setChecked(true);
    }


    /**
     * Open the Login Activity when a GetFileToolkit asks to do so
     */
    public void openLogin() { startActivity(new Intent(this, LoginActivity.class)); }


    /**
     * Update the time displayed in the "Last Update: ..."-label
     */
    @SuppressLint({"SimpleDateFormat", "SetTextI18n"})
    public void setLastUpdateTimeLabel() {
        TextView lastUpdateLabel = (TextView) findViewById(R.id.lastUpdateLabel);
        Long lastUpdate;

        switch(currentFrag) {
            case VplanFrag: lastUpdate = sharedPref.getLong(SharedPrefKeys.vplanLastUpdate, 0); break;
            case MplanFrag: lastUpdate = sharedPref.getLong(SharedPrefKeys.mensaLastUpdate, 0); break;
            case NewsFrag:  lastUpdate = sharedPref.getLong(SharedPrefKeys.newsLastUpdate,  0); break;
            default:        lastUpdate = 0L; break;
        }

        String lastupdatecaption = getApplicationContext().getResources().getString(R.string.general_lastUpdate);
        String nointconncaption  = getApplicationContext().getResources().getString(R.string.general_nointernetconnection);

        String timeStamp = new SimpleDateFormat().format(new java.util.Date(lastUpdate));
        if(lastUpdate != 0) { lastUpdateLabel.setText(lastupdatecaption + timeStamp); }
        else                { lastUpdateLabel.setText(nointconncaption); }
    }



    /**
     * Custom onBackPressed-Method
     *   1. When pressing back while the drawer is open, the drawer gets closed
     *   2. If the drawer is closed, require two clicks on the back key for closing the app
     */
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        // Drawer opened -> Close drawer; Drawer closed -> Close application
        if (drawer.isDrawerOpen(GravityCompat.START)) { drawer.closeDrawer(GravityCompat.START); }
        else {
            // Drawer closed -> Double click for closing the app
            if (doubleBackToExitPressedOnce) {
                // BACK pressed two times during the time interval -> close app
                super.onBackPressed();
                return;
            }

            int clickInterval = 2000; // max time interval between both clicks in milliseconds

            this.doubleBackToExitPressedOnce = true;
            String snackbarmssg = getApplicationContext().getResources().getString(R.string.general_clicktwicetoexit);
            Snackbar.make(getWindow().getDecorView().getRootView(), snackbarmssg, clickInterval).show();

            // set doubleBackToExitPressedOnes to false when BACK was not pressed a second time during the clickInterval
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() { doubleBackToExitPressedOnce=false; }
            }, clickInterval);
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

            default:
                return super.onOptionsItemSelected(item);
        }
    }



    // Has to be overridden, otherwise nasty crashes occur
    @Override
    public void onFragmentInteraction(Uri uri) {}

    @Override
    public void onListFragmentInteraction(FileSelectionListContent.Item item) {}


    private enum MainFrags { VplanFrag, MplanFrag, NewsFrag }
}
