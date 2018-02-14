package de.mwg_bayreuth.mwgorganizer;

import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;

import java.io.File;

import com.github.barteksc.pdfviewer.PDFView;

public class DisplayPDF extends AppCompatActivity {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_pdf);

        Intent intent = getIntent();
        int numberOfFiles =  intent.getIntExtra("NUMBEROFFILES",0);
        String[] filenames = intent.getStringArrayExtra("PDFFILENAMES");
        String[] labels = intent.getStringArrayExtra("PDFFILELABELS");
        int currentFile = intent.getIntExtra("CURRENTFILE", 0);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(), numberOfFiles, filenames, labels);

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_display_pd, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar handles clicks
        // on the Home/Up button the same as clicks on the physical "back" button
        switch(item.getItemId()) {
            case android.R.id.home:
                onBackPressed(); return true;
            case R.id.action_about:
                startActivity(new Intent(this, About.class)); return true;
            case R.id.action_settings:
                startActivity(new Intent(this, Settings.class)); return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        String filename;
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_LABEL = "LABEL";

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber, String filename, String filelabel) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putString(ARG_LABEL, filelabel);
            fragment.setArguments(args);
            fragment.filename = filename;
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_pdfdisplay, container, false);
            TextView textView = (TextView) rootView.findViewById(R.id.section_label);
            textView.setText(getArguments().getString(ARG_LABEL));

            PDFView pdfview = (PDFView) rootView.findViewById(R.id.pdfView);

            File file = new File(filename);
            try { pdfview.fromFile(file).load(); }
            catch(Exception e) { e.printStackTrace(); }

            return rootView;
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        private int numberOfFiles;
        private String[] filenames;
        private String[] filelabels;

        public SectionsPagerAdapter(FragmentManager fm, int numberOfFiles, String[] filenames, String[] filelabels) {
            super(fm);
            this.numberOfFiles = numberOfFiles;
            this.filenames = filenames;
            this.filelabels = filelabels;
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            String filelabel = filelabels[position];
            String filename = getExternalFilesDir(null) +"/"+ filenames[position];
            return PlaceholderFragment.newInstance(position + 1, filename,filelabel);
        }

        @Override
        public int getCount() {
            return numberOfFiles;
        }
    }
}
