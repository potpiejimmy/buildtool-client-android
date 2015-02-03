package com.wincor.bcon.framework.android.util;

import org.json.JSONArray;
import org.json.JSONObject;


/**
 * Utility class to access resources of a RESTful web service.
 * <p/>
 * A RESTful web service is usually accessed using a base-URL:
 * 
 * <code>http://myhost/service/resources/<entityName>s/
 * 
 * which will fetch a collection of available <entityName> entities,
 * whereas the URL extended by an entity's ID:
 * 
 * <code>http://myhost/service/resources/<entityName>s/<id>/
 * 
 * is used to access a single specific entity.
 * 
 * On the base URL, you perform HTTP GET to retrieve a collection of items
 * and HTTP POST to insert a new item.
 * 
 * On the extended URL using the entity's ID, you perform HTTP GET,
 * PUT or DELETE to read, update or delete a specific item, respectively.
 * <p/>
 * This class is designed to simplify the access for all of the above.
 * The JSONArray class is used to reflect the list of entities returned
 * from the base URL. The JSONObject is used to represent a single entity.
 */
public class RestResourceAccessor
{
	private String baseUrl = null;
	private JsonWebRequest requester = null;
	
	public RestResourceAccessor(String baseUrl)
	{
		if (baseUrl == null) throw new IllegalArgumentException("baseUrl is null");

		this.setBaseUrl(baseUrl);
		this.requester = new JsonWebRequest();
	}
	
	/**
	 * Returns the WebRequest object used to handle all HTTP
	 * requests - can be used to set additional properties for
	 * the web requests.
	 * @return WebRequest
	 */
	public WebRequest getWebRequest()
	{
		return this.requester;
	}
	
	public void setBaseUrl(String baseUrl)
	{
		this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
	}

	public String getBaseUrl()
	{
		return baseUrl;
	}

	public JSONArray getItems() throws Exception
	{
		return requester.getObjectArray(baseUrl);
	}
	
	public void insertItem(JSONObject item) throws Exception
	{
		requester.postObject(baseUrl, item);
	}
	
	public JSONObject insertItemWithResult(JSONObject item) throws Exception
	{
		return requester.postObjectWithResult(baseUrl, item);
	}
	
	public JSONObject getItem(String id) throws Exception
	{
		return requester.getObject(baseUrl+id);
	}
	
	public void updateItem(String id, JSONObject item) throws Exception
	{
		requester.putObject(baseUrl + id, item);
	}
	
	public JSONObject updateItemWithResult(String id, JSONObject item) throws Exception
	{
		return requester.putObjectWithResult(baseUrl + id, item);
	}
	
	public void deleteItem(String id) throws Exception
	{
		requester.deleteObject(baseUrl + id);
	}
	
	public JSONObject getItem(long id) throws Exception
	{
		return getItem(""+id);
	}
	
	public void updateItem(long id, JSONObject item) throws Exception
	{
		updateItem(""+id, item);
	}
	
	public void deleteItem(long id) throws Exception
	{
		deleteItem(""+id);
	}
}
