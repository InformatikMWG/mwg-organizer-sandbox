package de.mwg_bayreuth.mwgorganizer;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.view.MenuItem;
import android.widget.TextView;

public class About extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        TextView authorList = (TextView) findViewById(R.id.authorList);
        authorList.setText(Html.fromHtml(getString(R.string.about_authors_text)));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // F*** up the Back-Arrow in the title bar so that it behaves like the physical back button
        switch(item.getItemId()) {
            case android.R.id.home:
                onBackPressed(); return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
