package com.wincor.bcon.framework.android.util;

import org.json.JSONArray;
import org.json.JSONObject;

public class JsonWebRequest extends WebRequest
{
	public JsonWebRequest()
	{
    	setContentType("application/json; charset=utf-8");
    	setHeader("Accept", "application/json");
	}

	public JSONObject getObject(String url) throws Exception
	{
		return new JSONObject(super.get(url));
	}
	
	public JSONArray getObjectArray(String url) throws Exception
	{
		return new JSONArray(super.get(url));
	}
	
	public void postObject(String url, JSONObject object) throws Exception
	{
		super.post(url, object.toString());
	}

	public JSONObject postObjectWithResult(String url, JSONObject object) throws Exception
	{
		return new JSONObject(super.post(url, object.toString()));
	}

	public void putObject(String url, JSONObject object) throws Exception
	{
		super.put(url, object.toString());
	}

	public JSONObject putObjectWithResult(String url, Object object) throws Exception
	{
		return new JSONObject(super.put(url, object.toString()));
	}

	public void deleteObject(String url) throws Exception
	{
		super.delete(url);
	}
}
