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
    FileSelectionFragment fileSelectionFragment;
    Menu drawerMenu;



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

        // Configure the floating button
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            if(currentFSF != null) { MWGOrganizer.this.updateFiles(true); }
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

        // Handle navigation view item clicks here.
        switch(item.getItemId()) {
            default:
                fragmentClass = HomeFragment.class;
                currentFSF = null;
                fab.setVisibility(View.GONE);
                exchangeFragment(fragmentClass);
                setTitle(getApplicationContext().getResources().getString(R.string.app_name));
                break;
            case R.id.nav_vplan:
                fragmentClass = VertretungsplanFragment.class;
                currentFSF = FSFEnum.VplanFrag;
                fab.setVisibility(View.VISIBLE);
                exchangeFragment(fragmentClass);
                updateFiles(false);
                setTitle(item.getTitle());
                break;
            //case R.id.nav_mensa:
                //setTitle(item.getTitle());

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
            fragment = (android.support.v4.app.Fragment) fragmentClass.newInstance();
            if(currentFSF == FSFEnum.VplanFrag) {
                fileSelectionFragment = (FileSelectionFragment) fragment;
            }
        } catch (Exception e) { e.printStackTrace(); }

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment).commit();
        initButtons();
    }








    public void updateFiles(boolean forceUpdate) {
        // Check for internet connection
        NetworkToolkit nettools = new NetworkToolkit(MWGOrganizer.this);
        if(!nettools.networkConnectionAvailable()) {
            // No internet connection: Complain
            Snackbar.make(getWindow().getDecorView().getRootView(), R.string.general_nointernetconnection, Snackbar.LENGTH_SHORT).show();
        } else {
            if (currentFSF == FSFEnum.VplanFrag) {
                if (forceUpdate) {
                    speditor.putBoolean(SharedPrefKeys.vplanForceUpdate, true);
                    speditor.commit();
                }
                GetVertretungsplanToolkit gvt = new GetVertretungsplanToolkit(
                        sharedPref, speditor, cachemanager, progDialog, extDirectory, this);
            } else if (currentFSF == FSFEnum.MplanFrag) {

            } else if (currentFSF == FSFEnum.NewsFrag) {

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
                } else { snackbarString = getApplicationContext().getResources().getString(R.string.progress_allfilesuptodate); }
            } else {
                // News update: no further information
                snackbarString = getApplicationContext().getResources().getString(R.string.progress_newsupdated);
            }
        } else {
            // Display error string
            snackbarString = getApplicationContext().getResources().getString(R.string.progress_updatefail);
        }

        Snackbar.make(view, snackbarString, Snackbar.LENGTH_SHORT).show();
    }


    public void initButtons() {
        if(currentFSF == FSFEnum.VplanFrag)
            { fileSelectionFragment.setupButtons(getApplicationContext()); }
    }

    public void setupButtons() {
        // Uses a dirty, but working hack :D
        if(currentFSF == FSFEnum.VplanFrag) { exchangeFragment(VertretungsplanFragment.class); }
    }



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

            default:
                return super.onOptionsItemSelected(item);
        }
    }



    // Has to be overridden, otherwise nasty crashes occurr
    @Override
    public void onFragmentInteraction(Uri uri) {}

    @Override
    public void onListFragmentInteraction(FileSelectionListContent.Item item) {}


    private enum FSFEnum { VplanFrag, MplanFrag, NewsFrag }
}
