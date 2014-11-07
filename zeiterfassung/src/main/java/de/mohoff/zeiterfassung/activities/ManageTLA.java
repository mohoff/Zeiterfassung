package de.mohoff.zeiterfassung.activities;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import de.mohoff.zeiterfassung.ListAdapterTLA;
import de.mohoff.zeiterfassung.R;
import de.mohoff.zeiterfassung.database.DatabaseHelper;
import de.mohoff.zeiterfassung.datamodel.TargetLocationArea;

import java.util.List;

public class ManageTLA extends ActionBarActivity {

    private DatabaseHelper dbHelper = null;
    ListAdapterTLA adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_tla);
        getDbHelper();

        getActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#025167")));

        adapter = new ListAdapterTLA(this);
        final ListView lv = (ListView) findViewById(R.id.listView);
        lv.setItemsCanFocus(true);
        lv.setClickable(false);
        lv.setAdapter(adapter);

        //setListAdapter(adapter);

        /*lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(ManageTLA.this, ManageTLADetail.class);
                intent.putExtra("selected", (int) i);
                startActivity(intent);
                //overridePendingTransition(R.anim.enter_from_right, R.anim.exit_out_left);
            }
        });*/

    }

    public List<TargetLocationArea> getAllTLAs(){
        return dbHelper.getTLAs();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.manage_tla, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private DatabaseHelper getDbHelper() {
        if (dbHelper == null) {
            dbHelper =
                    OpenHelperManager.getHelper(this, DatabaseHelper.class);
        }
        return dbHelper;
    }
}
