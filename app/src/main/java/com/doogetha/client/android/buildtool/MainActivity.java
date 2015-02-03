package com.doogetha.client.android.buildtool;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.wincor.bcon.framework.android.util.AsyncUITask;

import org.json.JSONArray;
import org.json.JSONObject;


public class MainActivity extends ActionBarActivity implements AdapterView.OnItemClickListener {

    /** Holds the list's data */
    private ArrayAdapter<JSONObject> data = null;

    /** Holds the list view UI element */
    private ListView listView = null;

    /** Inner class used for asynchronous loading of data */
    private DataLoader dataLoader = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.listView = (ListView) findViewById(R.id.jobList);
        this.data = new ArrayAdapter<JSONObject>(this, R.layout.main_list_item) {

            @Override
            public View getView(int position, View convertView, ViewGroup viewGroup) {
                if (convertView == null) {
                    LayoutInflater inflater = (LayoutInflater) getContext()
                            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    convertView = inflater.inflate(R.layout.main_list_item, null);
                }

                JSONObject item = getItem(position);

                TextView title = (TextView) convertView.findViewById(R.id.item_title);
                TextView subtitle = (TextView) convertView.findViewById(R.id.item_subtitle);
                title.setText(item.optString("name"));
                subtitle.setText("State: " + item.optString("state"));

                return convertView;
            }
        };

        this.listView.setAdapter(this.data);
        this.listView.setOnItemClickListener(this);

        this.dataLoader = new DataLoader();

        refresh();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        } else if (id == R.id.action_refresh) {
            refresh();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
        //editItem(data.getItem(position));
    }

    /**
     * Asynchronously refreshes the list data
     */
    protected void refresh()
    {
        dataLoader.go(getString(R.string.loading), true);
    }

    /**
     * Inner class used for asynchronous loading of data from the server.
     */
    protected class DataLoader extends AsyncUITask<JSONArray>
    {
        public DataLoader() { super(MainActivity.this); }

        @Override
        public JSONArray doTask() throws Throwable
        {
            return ((Application)getApplication()).getRestAccessor().getItems();
        }

        @Override
        public void doneOk(JSONArray result) {
            data.clear();
            for (int i=0; i<result.length(); i++) data.add(result.optJSONObject(i));
        }

        @Override
        public void doneFail(Throwable throwable) {
            throwable.printStackTrace();
            Toast.makeText(getApplicationContext(), throwable.toString(), Toast.LENGTH_SHORT).show();
        }
    }
}
