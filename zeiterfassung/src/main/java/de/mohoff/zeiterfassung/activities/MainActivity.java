package de.mohoff.zeiterfassung.activities;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import de.mohoff.zeiterfassung.LocationUpdateHandler;
import de.mohoff.zeiterfassung.R;
import de.mohoff.zeiterfassung.database.DatabaseHelper;


public class MainActivity extends ActionBarActivity {

    private Button goToMap;
    private Button addNewTLA;
    private LocationUpdateHandler luh;
    private DatabaseHelper dbHelper = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getDbHelper();



        //luh = LocationUpdateHandler.getInstance(this);

        goToMap = (Button) findViewById(R.id.button);
        addNewTLA = (Button) findViewById(R.id.buttonTLA);
        goToMap.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                Intent intent = new Intent(MainActivity.this, Map.class);
                startActivity(intent);
                finish();
            }
        });
        addNewTLA.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                Intent intent = new Intent(MainActivity.this, AddTargetLocationArea.class);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            OpenHelperManager.releaseHelper();
            dbHelper = null;
        }
    }

    private DatabaseHelper getDbHelper() {
        if (dbHelper == null) {
            dbHelper =
                    OpenHelperManager.getHelper(this, DatabaseHelper.class);
        }
        return dbHelper;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
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


}
