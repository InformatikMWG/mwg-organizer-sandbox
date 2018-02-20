package de.mwg_bayreuth.mwgorganizer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;

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

import java.io.File;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;



public class DisplayPDF extends AppCompatActivity {
    String[] filelabels;
    private SharedPreferences.Editor speditor;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_pdf);

        Intent intent = getIntent();
        int numberOfFiles =  intent.getIntExtra("NUMBEROFFILES",0);
        String[] filenames = intent.getStringArrayExtra("PDFFILENAMES");
        String[] labels = intent.getStringArrayExtra("PDFFILELABELS");
        this.filelabels = labels;
        int currentFile = intent.getIntExtra("CURRENTFILE", 0);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        PDFViewPagerAdapter pagerAdapt = new PDFViewPagerAdapter(getSupportFragmentManager(), numberOfFiles, filenames, labels);

        setTitle(filelabels[currentFile]);

        // Set up the ViewPager with the sections adapter.
        ViewPager mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(pagerAdapt);
        mViewPager.setCurrentItem(currentFile);
        SharedPreferences sharedPref = getSharedPreferences(SharedPrefKeys.spPrefix, Context.MODE_PRIVATE);
        speditor = sharedPref.edit();

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageSelected(int position) {
                speditor.putBoolean(SharedPrefKeys.vplanButtonFileUpdated+position, false);
                speditor.commit();
                setTitle(filelabels[position]);
            }

            @Override
            public void onPageScrollStateChanged(int state) {}
        });

        speditor.putBoolean(SharedPrefKeys.vplanButtonFileUpdated+currentFile, false);
        speditor.commit();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_display_pd, menu);
        return true;
    }

    
    /**
     * Handle action bar item clicks here. The action bar handles clicks 
     * on the Home/Up button the same as clicks on the physical "back" button
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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
     * A fragment containing a PDFView with a document
     */
    public static class PDFFileFragment extends Fragment {
        String filename;
        String filelabel;

        public PDFFileFragment() { }

        /**
         * Returns a new instance of this fragment for the given filename and -label
         */
        public static PDFFileFragment newInstance(String filename, String filelabel) {
            PDFFileFragment fragment = new PDFFileFragment();
            Bundle args = new Bundle();
            args.putString("LABEL", filelabel);
            fragment.setArguments(args);
            fragment.filename = filename;
            fragment.filelabel = filelabel;
            return fragment;
        }

        
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_pdfdisplay, container, false);

            PDFView pdfview = (PDFView) rootView.findViewById(R.id.pdfView);

            File file = new File(filename);
            try { pdfview.fromFile(file).scrollHandle(new DefaultScrollHandle(getContext())).load(); }
            catch(Exception e) {
                // File not found or f***ed up
                // TODO: File should be downloaded again
                e.printStackTrace();
            }

            return rootView;
        }
    }

    
    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class PDFViewPagerAdapter extends FragmentPagerAdapter {
        private int numberOfFiles;
        private String[] filenames;
        private String[] filelabels;

        PDFViewPagerAdapter(FragmentManager fm, int numberOfFiles, String[] filenames, String[] filelabels) {
            super(fm);
            this.numberOfFiles = numberOfFiles;
            this.filenames = filenames;
            this.filelabels = filelabels;
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PDFFileFragment (defined as a static inner class below).
            String filelabel = filelabels[position];
            String filename = getExternalFilesDir(null) +"/"+ filenames[position];
            return PDFFileFragment.newInstance(filename, filelabel);
        }

        @Override
        public int getCount() {
            return numberOfFiles;
        }
    }
}
