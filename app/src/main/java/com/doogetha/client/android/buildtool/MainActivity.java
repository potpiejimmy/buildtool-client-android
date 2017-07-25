package com.doogetha.client.android.buildtool;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonRequest;
import com.wincor.bcon.framework.android.util.VolleyUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, AdapterView.OnItemClickListener{

    private final static DateFormat LIST_DATE_FORMATTER_LONG  = new SimpleDateFormat("MMM dd, HH:mm");
    private final static DateFormat LIST_DATE_FORMATTER_SHORT = new SimpleDateFormat("HH:mm");

    protected DrawerLayout mDrawer;
    protected NavigationView mStartMenu;

    /** Holds the list's data */
    private ArrayAdapter<JSONObject> data = null;
    /** Holds the list view UI element */
    private ListView mMainList = null;

    long mLastWaitForChangeRequest = 0L;
    boolean mWaitForChangesRunning = false;
    boolean mWaitForChangesInterrupted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setTitle("PC/E Build Tool");

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDrawer.openDrawer(Gravity.LEFT);
            }
        });

        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, mDrawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawer.addDrawerListener(toggle);
        toggle.syncState();

        mStartMenu = (NavigationView) findViewById(R.id.nav_view);
        mStartMenu.setNavigationItemSelectedListener(this);

        setupMainList();
    }

    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first
        
        // if waiting for changes process was running earlier and has been interrupted,
        // use the onResume callback to re-activate it
        if (mWaitForChangesInterrupted) waitForChangesElapsed();
    }

    @Override
    protected void onStop () {
        super.onStop();

        // cancel all pending requests
        VolleyUtil.getRequestQueue().cancelAll(this);
        if (mWaitForChangesRunning) mWaitForChangesInterrupted = true;
    }

    protected void setupMainList() {

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
                TextView datetime = (TextView) convertView.findViewById(R.id.item_datetime);
                ProgressBar progbar = (ProgressBar) convertView.findViewById(R.id.item_progressbar);

                String state = item.optString("state");
                title.setText(item.optString("name"));
                subtitle.setText(state);
                datetime.setText(formatListItemDateTime(item.optLong("lastmodified")));

                if (Character.isDigit(state.charAt(0))) {
                    progbar.setVisibility(View.VISIBLE);
                    progbar.setIndeterminate(false);
                    subtitle.setText(state + " %");
                    try {
                        progbar.setProgress(Integer.parseInt(state));
                    } catch (NumberFormatException e) {
                        progbar.setProgress(0);
                    }
                } else if (isJobDone(item)) {
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
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        startupInit();
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
            enterUnitId();
            return true;
        } else if (id == R.id.action_update) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.doogetha.com/download/buildtool.apk")));
            return true;
        } else if (id == R.id.action_clearlist) {
            clearList();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        startTask(item.getTitle().toString());
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
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
            case R.id.action_restart:
                startTask(data.getItem(info.position).optString("name"));
                return true;
            case R.id.action_delete:
                deleteTask(data.getItem(info.position).optString("name"));
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    protected void startupInit() {
        String unitId = ((Application)getApplication()).getUnitId();
        if (unitId == null || unitId.length() == 0) {
            enterUnitId();
        } else {
            mWaitForChangesRunning = true;
            waitForChanges();
            reload();
        }
    }

    protected void enterUnitId() {
        final EditText input = new EditText(this);
        String unitId = ((Application)getApplication()).getUnitId();
        if (unitId != null) input.setText(unitId);
        new AlertDialog.Builder(this)
                .setMessage("Enter your host ID:")
                .setView(input)
                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        getApp().setUnitId(input.getText().toString().trim());
                        startupInit();
                    }
                }).setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        }).show();
    }

    protected Application getApp() {
        return (Application)getApplication();
    }

    protected void updateItemsMainList(JSONArray result) {
        // ----- populate main list -----
        data.clear();
        for (int i=0; i<result.length(); i++) {
            JSONObject job = result.optJSONObject(i);
            if (!job.optString("name","").startsWith("BN "))
                data.add(job);
        }
    }

    protected void updateItemsStartMenu(JSONObject result) {
        // ----- start menu list -----

        mStartMenu.getMenu().clear();

        Menu menu = mStartMenu.getMenu();
        try {
            JSONArray items = new JSONArray(result.optString("value"));
            for (int i=0; i<items.length(); i++) {
                String item = items.optString(i);
                if (item.startsWith("-")) {
                    menu = mStartMenu.getMenu(); // one up
                    menu = menu.addSubMenu(item.substring(1)); // sub menu
                } else {
                    menu.add(item);
                }
            }
        } catch (Exception ex) {
            Snackbar.make(MainActivity.this.mMainList, ex.toString(), Snackbar.LENGTH_LONG).show();
        }
    }

    protected void sendRequest(JsonRequest<?> req) {
        req.setTag(this);
        VolleyUtil.getRequestQueue().add(req);
    }

    protected void waitForChangesElapsed() {
        Log.d(MainActivity.class.getName(), "XXX Change notify");
        waitForChanges();
        reload();
    }

    protected void waitForChanges() {
        // job list
        JsonArrayRequest req = new JsonArrayRequest(Request.Method.GET,
                Application.URL_JOBS + getApp().getUnitId() + "?waitForChange=true",
                null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        waitForChangesElapsed();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (System.currentTimeMillis() - mLastWaitForChangeRequest > 1000)
                            waitForChangesElapsed();
                        else
                        	mWaitForChangesInterrupted = true;
                    }
                });
        req.setRetryPolicy(new DefaultRetryPolicy(60000, 0, 0.0f));
        mLastWaitForChangeRequest = System.currentTimeMillis();
        sendRequest(req);
    }

    protected void reload() {
        // job list
        sendRequest(new JsonArrayRequest(Request.Method.GET,
                Application.URL_JOBS + getApp().getUnitId(),
                null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        updateItemsMainList(response);
                    }
                },
                new DefaultErrorListener()
        ));

        // start menu
        sendRequest(new JsonObjectRequest(Request.Method.GET,
                Application.URL_PARAMS + getApp().getUnitId() + "/jobs",
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        updateItemsStartMenu(response);
                    }
                },
                new DefaultErrorListener()
        ));
    }

    protected void startTask(String task) {
        sendRequest(new JsonObjectRequest(Request.Method.GET,
                Application.URL_JOBS + getApp().getUnitId() + "/" + task.replace(" ", "%20") + "?set=pending",
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        mDrawer.closeDrawer(GravityCompat.START);
                    }
                },
                new DefaultErrorListener()
        ));
    }

    protected void deleteTask(String task) {
        sendRequest(new JsonObjectRequest(Request.Method.DELETE,
                Application.URL_JOBS + getApp().getUnitId() + "/" + task.replace(" ", "%20"),
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                    }
                },
                new DefaultErrorListener()
        ));
    }

    protected void clearList() {
        sendRequest(new JsonObjectRequest(Request.Method.DELETE,
                Application.URL_JOBS + getApp().getUnitId(),
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                    }
                },
                new DefaultErrorListener()
        ));
    }

    protected static boolean isJobDone(JSONObject job) {
        String state = job.optString("state");
        return (state.toLowerCase().startsWith("done") || state.toLowerCase().startsWith("error"));
    }

    protected static String formatListItemDateTime(long time) {
        Calendar today = Calendar.getInstance();
        Calendar itemTime = Calendar.getInstance();
        itemTime.setTimeInMillis(time);
        if (today.get(Calendar.DAY_OF_YEAR) == itemTime.get(Calendar.DAY_OF_YEAR) &&
                today.get(Calendar.YEAR) == itemTime.get(Calendar.YEAR))
            /* use short format if today */
            return LIST_DATE_FORMATTER_SHORT.format(itemTime.getTime());
        else
            return LIST_DATE_FORMATTER_LONG.format(itemTime.getTime());
    }

    public class DefaultErrorListener implements Response.ErrorListener {
        @Override
        public void onErrorResponse(VolleyError error) {
            Snackbar.make(MainActivity.this.mMainList, error.toString(), Snackbar.LENGTH_LONG).show();
        }
    }
}
