package com.doogetha.client.android.buildtool;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.wincor.bcon.framework.android.util.AsyncUITask;
import com.wincor.bcon.framework.android.util.RestResourceAccessor;

import org.json.JSONArray;
import org.json.JSONObject;


public class MainActivity extends ActionBarActivity implements AdapterView.OnItemClickListener {

    /** Holds the list's data */
    private ArrayAdapter<JSONObject> data = null;

    /** Holds the list view UI element */
    private ListView mMainList = null;
    private ListView mDrawerList = null;
    private DrawerLayout mDrawerLayout = null;

    /** Inner class used for asynchronous loading of data */
    private DataLoader dataLoader = null;

    private CharSequence mTitle;
    private ActionBarDrawerToggle mDrawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // MAIN LIST VIEW

        this.mMainList = (ListView) findViewById(R.id.jobList);
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
                ProgressBar progbar = (ProgressBar) convertView.findViewById(R.id.item_progressbar);

                String state = item.optString("state");
                title.setText(item.optString("name"));
                subtitle.setText(state);

                if (Character.isDigit(state.charAt(0))) {
                    progbar.setVisibility(View.VISIBLE);
                    progbar.setIndeterminate(false);
                    try {
                        progbar.setProgress(Integer.parseInt(state));
                    } catch (NumberFormatException e) {
                        progbar.setProgress(0);
                    }
                } else if (state.toLowerCase().startsWith("done") || state.toLowerCase().startsWith("error")) {
                    progbar.setVisibility(View.GONE);
                } else {
                    progbar.setVisibility(View.VISIBLE);
                    progbar.setIndeterminate(true);
                }

                return convertView;
            }
        };

        this.mMainList.setAdapter(this.data);
        this.mMainList.setOnItemClickListener(this);
        registerForContextMenu(this.mMainList);

        this.dataLoader = new DataLoader();

        // APPLICATION DRAWER

        mTitle = getTitle();
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                getSupportActionBar().setTitle(mTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getSupportActionBar().setTitle(mTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        // activate the drawer toggle icon
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        // APPLICATION DRAWER ITEMS

        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        String[] items = getResources().getStringArray(R.array.drawer_items);
        // Set the adapter for the list view
        mDrawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_list_item, items));
        // Set the list's click listener
        mDrawerList.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                new TaskStarter(getResources().getStringArray(R.array.drawer_items)[position], position).go(getString(R.string.loading));
            }
        });

        refresh();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // handle drawer toggle icon event:
        if (mDrawerToggle.onOptionsItemSelected(item)) return true;

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

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.context_menu_main, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.action_delete:
                new TaskDeleter(data.getItem(info.position).optString("name")).go(getString(R.string.loading));
                return true;
            default:
                return super.onContextItemSelected(item);
        }
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

    /**
     * Settings of pending tasks
     */
    protected class TaskStarter extends AsyncUITask<JSONObject>
    {
        private int position;
        private String task;

        public TaskStarter(String task, int position) {
            super(MainActivity.this);
            this.task = task;
            this.position = position;
        }

        @Override
        public JSONObject doTask() throws Throwable
        {
            RestResourceAccessor acc = ((Application)getApplication()).getRestAccessor();
            acc.getWebRequest().setParam("set", "pending");
            return acc.getItem(task.replace(" ", "%20"));
        }

        @Override
        public void doneOk(JSONObject result) {
            mDrawerList.setItemChecked(position, false);
            mDrawerLayout.closeDrawer(mDrawerList);
            refresh();
        }

        @Override
        public void doneFail(Throwable throwable) {
            throwable.printStackTrace();
            Toast.makeText(getApplicationContext(), throwable.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Settings of pending tasks
     */
    protected class TaskDeleter extends AsyncUITask<String>
    {
        private String task;

        public TaskDeleter(String task) {
            super(MainActivity.this);
            this.task = task;
        }

        @Override
        public String doTask() throws Throwable
        {
            RestResourceAccessor acc = ((Application)getApplication()).getRestAccessor();
            acc.deleteItem(task.replace(" ", "%20"));
            return "Deleted";
        }

        @Override
        public void doneOk(String result) {
            refresh();
        }

        @Override
        public void doneFail(Throwable throwable) {
            throwable.printStackTrace();
            Toast.makeText(getApplicationContext(), throwable.toString(), Toast.LENGTH_SHORT).show();
        }
    }
}
