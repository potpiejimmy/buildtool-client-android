package com.doogetha.client.android.buildtool;

import com.wincor.bcon.framework.android.util.RestResourceAccessor;

/**
 * Main application class for the android application. Note that this class 
 * is registered in the AndroidManifext.xml and instantiated by the Android system.
 * 
 * It can be used to hold application-wide properties.
 */
public class Application extends android.app.Application {
	
	/** sample URL pointing to the server's REST resource of "SampleEntity" */
	public final static String URL = "http://www.doogetha.com/buildtool/res/jobs/w7-deffm0287/";

	/** REST resource accessor instance */
	private RestResourceAccessor req = null;
	
	@Override
	public void onCreate() {
        req = new RestResourceAccessor(URL);
        // set general HTTP request parameters (just an example)
    	req.getWebRequest().setParam("max", "255");
	}

	/**
	 * Returns the sample REST resource accessor that can be used
	 * to read, update and delete entities of the SampleEntity type
	 * from the server.
	 * @return the REST resource accessor
	 */
	public RestResourceAccessor getRestAccessor() {
		return req;
	}

}