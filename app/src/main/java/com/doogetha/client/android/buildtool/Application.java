package com.doogetha.client.android.buildtool;

import android.content.SharedPreferences;

import com.wincor.bcon.framework.android.util.VolleyUtil;

/**
 * Main application class for the android application. Note that this class 
 * is registered in the AndroidManifext.xml and instantiated by the Android system.
 * 
 * It can be used to hold application-wide properties.
 */
public class Application extends android.app.Application {
	
	public final static String URL_JOBS   = "http://www.doogetha.com/buildtool/res/jobs/";
    public final static String URL_PARAMS = "http://www.doogetha.com/buildtool/res/params/";

    private SharedPreferences preferences = null;

    @Override
	public void onCreate() {
        super.onCreate();
        VolleyUtil.initializeContext(this);
	}

    public SharedPreferences getPreferences() {
        if (preferences == null) {
            preferences = getSharedPreferences("buildtoolprefs", MODE_PRIVATE);
        }
        return preferences;
    }

    public String getUnitId() {
        return getPreferences().getString("unitId", null);
    }

    public void setUnitId(String unitId) {
        getPreferences().edit().putString("unitId", unitId).commit();
    }
}
