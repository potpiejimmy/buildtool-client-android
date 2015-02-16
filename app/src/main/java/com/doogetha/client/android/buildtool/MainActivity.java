package com.doogetha.client.android.buildtool;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.wincor.bcon.framework.android.util.AsyncUITask;
import com.wincor.bcon.framework.android.util.RestResourceAccessor;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends ActionBarActivity implements AdapterView.OnItemClickListener {

    private final static DateFormat LIST_DATE_FORMATTER_LONG  = new SimpleDateFormat("MMM dd, HH:mm");
    private final static DateFormat LIST_DATE_FORMATTER_SHORT = new SimpleDateFormat("HH:mm");

    /** Holds the list's data */
    private ArrayAdapter<JSONObject> data = null;

    /** Holds the list view UI element */
    private ListView mMainList = null;
    private View mDrawer = null;
    private ListView mDrawerList = null;
    private DrawerLayout mDrawerLayout = null;

    /* refresh animation */
    private MenuItem refreshMenuItem = null;
    private ImageView spinningRefreshView = null;
    private Animation spinningRefreshAnim = null;
    private boolean isAutoRefreshTimerRunning = false;

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

        this.dataLoader = new DataLoader();

        // APPLICATION DRAWER

        mTitle = getTitle();
        mDrawer = findViewById(R.id.left_drawer);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                //invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                //invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        // activate the drawer toggle icon and app icon
        getSupportActionBar().setTitle(R.string.actionbar_title);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setLogo(R.drawable.buildtool_appicon);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayUseLogoEnabled(true);

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        // APPLICATION DRAWER ITEMS

        mDrawerList = (ListView) findViewById(R.id.left_drawer_list);

        String[] items = getResources().getStringArray(R.array.drawer_items);
        // Set the adapter for the list view
        mDrawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_list_item, items) {
            @Override
            public View getView(int position, View convertView, ViewGroup viewGroup) {
                if (convertView == null) {
                    LayoutInflater inflater = (LayoutInflater) getContext()
                            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    if (getItemViewType(position) == 0) {
                        convertView = inflater.inflate(R.layout.drawer_list_item, null);
                    } else {
                        convertView = inflater.inflate(R.layout.drawer_list_separator, null);
                    }
                }

                TextView title = null;
                String label = getResources().getStringArray(R.array.drawer_items)[position];
                if (getItemViewType(position) == 0) {
                    title = (TextView) convertView;
                } else {
                    title = (TextView) convertView.findViewById(R.id.sepTitle);
                    label = label.substring(1);
                }
                title.setText(label);

                return convertView;
            }

            @Override
            public int getViewTypeCount(){
                return 2; // normal item and separator
            }

            @Override
            public int getItemViewType(int position) {
                return getResources().getStringArray(R.array.drawer_items)[position].startsWith("-") ? 1 : 0;
            }

            @Override
            public boolean isEnabled(int position) {
                return getItemViewType(position) == 0;
            }
        });
        // Set the list's click listener
        mDrawerList.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                new TaskStarter(getResources().getStringArray(R.array.drawer_items)[position], position).go(getString(R.string.loading));
            }
        });

        // -------- REFRESH ANIM ----------------

        /* Attach a rotating ImageView to the refresh item as an ActionView */
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        spinningRefreshView = (ImageView) inflater.inflate(R.layout.refresh_action_view, null);

        spinningRefreshAnim = AnimationUtils.loadAnimation(this, R.anim.clockwise_refresh);
        spinningRefreshAnim.setRepeatCount(Animation.INFINITE);

        // --------------------------------------
    }

    protected void startupInit() {
        String unitId = ((Application)getApplication()).getUnitId();
        if (unitId == null || unitId.length() == 0) {
            enterUnitId();
        } else {
            refresh();
        }
    }

    protected static boolean isJobDone(JSONObject job) {
        String state = job.optString("state");
        return (state.toLowerCase().startsWith("done") || state.toLowerCase().startsWith("error"));
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
        refreshMenuItem = menu.findItem(R.id.action_refresh);
        startupInit();
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
            enterUnitId();
            return true;
        } else if (id == R.id.action_update) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.doogetha.com/download/buildtool.apk")));
            return true;
        } else if (id == R.id.action_refresh) {
            refresh(false);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
        //editItem(data.getItem(position));
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
                        ((Application)getApplication()).setUnitId(input.getText().toString().trim());
                        refresh();
                    }
                }).setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        }).show();
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
    protected void refresh() {
        refresh(true);
    }

    protected void refresh(boolean showDialog) {
        spinningRefreshView.clearAnimation();
        spinningRefreshView.startAnimation(spinningRefreshAnim);
        refreshMenuItem.setActionView(spinningRefreshView);
        dataLoader.go(getString(R.string.loading), showDialog);
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

    /**
     * Inner class used for asynchronous loading of data from the server.
     */
    protected class DataLoader extends AsyncUITask<JSONArray>
    {
        private final Handler handler = new Handler();

        public DataLoader() { super(MainActivity.this); }

        @Override
        public JSONArray doTask() throws Throwable
        {
            return ((Application)getApplication()).getRestAccessor().getItems();
        }

        @Override
        public void doneOk(JSONArray result) {
            data.clear();
            boolean allDone = true;
            for (int i=0; i<result.length(); i++) {
                JSONObject job = result.optJSONObject(i);
                data.add(job);
                if (!isJobDone(job)) allDone = false;
            }
            if (!allDone) {
                if (!isAutoRefreshTimerRunning) {
                    isAutoRefreshTimerRunning = true;
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    isAutoRefreshTimerRunning = false;
                                    refresh(false);
                                }
                            });
                        }
                    }, 10000);
                }
            } else {
                spinningRefreshView.clearAnimation();
                refreshMenuItem.setActionView(null);
            }
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
            mDrawerLayout.closeDrawer(mDrawer);
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
