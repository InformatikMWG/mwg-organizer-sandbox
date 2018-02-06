package de.mwg_bayreuth.mwgorganizer;

import android.content.Intent;
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

import de.mwg_bayreuth.mwgorganizer.dummy.DummyContent;

public class MWGOrganizer extends AppCompatActivity
implements NavigationView.OnNavigationItemSelectedListener,
        // Also implement the interaction listeners of the single fragments
        HomeFragment.OnFragmentInteractionListener,
        VertretungsplanFragment.OnListFragmentInteractionListener {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mwgorganizer);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Configure the floating button
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: Aktualisierung auslösen
                // Snackbar statt Toast "Aktualisierung ..." / "Keine Internetverbindung"
                Snackbar.make(view, "TODO: Aktualisierung auslösen", Snackbar.LENGTH_SHORT)
                        .setAction("Action", null).show();
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

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        android.support.v4.app.Fragment fragment = null;
        Class fragmentClass = null;
        boolean exchangeFragment = false;

        // Handle navigation view item clicks here.
        switch(item.getItemId()) {
            default:
                fragmentClass = HomeFragment.class;
                exchangeFragment = true; break;
            case R.id.nav_vplan:
                fragmentClass = VertretungsplanFragment.class;
                exchangeFragment = true; break;
        //    case R.id.nav_mensa:

        //    case R.id.nav_news:

            case R.id.nav_about:
                startActivity(new Intent(this, About.class)); break;
            case R.id.nav_settings:
                startActivity(new Intent(this, Settings.class)); break;
        }

        // Do *not* execute this code when an Activity has been opened!!
        if(exchangeFragment) {
            try { fragment = (android.support.v4.app.Fragment) fragmentClass.newInstance(); }
            catch (Exception e) { e.printStackTrace(); }

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment).commit();

            setTitle(item.getTitle());
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    // Has to be overridden, otherwise nasty crashes occurr
    @Override
    public void onFragmentInteraction(Uri uri) {}

    @Override
    public void onListFragmentInteraction(DummyContent.DummyItem item) {}
}
