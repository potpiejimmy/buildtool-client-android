package com.wincor.bcon.framework.android.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

public class WebRequest implements ResponseHandler<String>
{
	private final static String CHAR_ENCODING = "UTF-8";
	
	protected final static int SOCKET_TIMEOUT_MS = 30000;
	
    private Map<String,String> params;
    private Map<String,String> headers;
    private String contentType = null;
 
    private int responseCode;
    private String message;
 
    private String response;
    
    private HttpClient client = null;
 
    public String getResponse()
    {
        return response;
    }
 
    public String getErrorMessage()
    {
        return message;
    }
 
    public int getResponseCode()
    {
        return responseCode;
    }
 
    public WebRequest()
    {
        params = new HashMap<String,String>();
        headers = new HashMap<String,String>();

        HttpParams httpParams = new BasicHttpParams();
    	HttpConnectionParams.setSoTimeout(httpParams, SOCKET_TIMEOUT_MS);
    	
    	SchemeRegistry schemeRegistry = new SchemeRegistry ();

    	schemeRegistry.register (new Scheme ("http",
    	    PlainSocketFactory.getSocketFactory (), 80));
    	try {
	    	schemeRegistry.register (new Scheme ("https",
	    	    new CustomSSLSocketFactory (), 443));
    	} catch (Exception ex) {
    		throw new RuntimeException("Could not register CustomSSLSocketFactory", ex);
    	}
    	
        client = new DefaultHttpClient(new ThreadSafeClientConnManager (
        		httpParams, schemeRegistry), httpParams);
    }
 
    public void setParam(String name, String value)
    {
        params.put(name, value);
    }
 
    public void setHeader(String name, String value)
    {
        headers.put(name, value);
    }
    
    public String getParam(String name)
    {
    	return params.get(name);
    }
 
    public String getHeader(String name)
    {
    	return headers.get(name);
    }
    
    public void removeParam(String name)
    {
    	params.remove(name);
    }
    
    public void removeHeader(String name)
    {
    	headers.remove(name);
    }
 
    public String getContentType()
    {
		return contentType;
	}

	public void setContentType(String contentType)
	{
		this.contentType = contentType;
	}

	public String get(String url) throws Exception
    {
        //add parameters
        StringBuffer combinedParams = new StringBuffer();
        if (!params.isEmpty())
        {
            combinedParams.append('?');
            for (String param : params.keySet())
            {
                String paramString = param + "=" + URLEncoder.encode(params.get(param), CHAR_ENCODING);
                if (combinedParams.length() > 1) combinedParams.append('&');
                combinedParams.append(paramString);
            }
        }
 
       HttpGet request = new HttpGet(url + combinedParams);
 
       setRequestHeaders(request);
       
       return executeRequest(request, 3);
    }

	public String put(String url, String msg) throws Exception
	{
		return putImpl(new HttpPut(url), msg);
	}
	
	public String post(String url, String msg) throws Exception
	{
		return putImpl(new HttpPost(url), msg);
	}
	
	protected String putImpl(HttpEntityEnclosingRequestBase request, String msg) throws Exception
	{
		setRequestHeaders(request);

        StringEntity entity = new StringEntity(msg, CHAR_ENCODING);
    	if (contentType != null)
    		entity.setContentType(contentType);
    	request.setEntity(entity);
 
        return executeRequest(request);
    }
 
	public String delete(String url) throws Exception
	{
		HttpDelete request = new HttpDelete(url);
 
		setRequestHeaders(request);
		
        return executeRequest(request);
    }
	
	protected void setRequestHeaders(HttpRequestBase request)
	{
       //add headers
       for(String header : headers.keySet()) {
            request.addHeader(header, headers.get(header));
//            if (header.equals("Authorization")) {
//            	String creds = headers.get(header).substring(6);
//            	String credentials = new String(Base64.decode(creds, Base64.DEFAULT));
//            	Log.v("XXXXXX MYWEBREQUEST", "LOGGING IN USING " + credentials);
//            }
       }
	}
 
    protected String executeRequestImpl(HttpUriRequest request, int tries) throws Exception
    {
    	for (int i=0; i<tries; i++) {
    		try {
    	    	return client.execute(request, this);
    		} catch (Exception e) {
    			if (i==tries-1) throw e;
    		}
    	}
    	return null;
    }
    
    protected String executeRequest(HttpUriRequest request) throws Exception
    {
    	return executeRequest(request, 1);
    }
    
    protected String executeRequest(HttpUriRequest request, int tries) throws Exception
    {
    	String result = executeRequestImpl(request, tries);
    	assertResponseCode();
    	return result;
    }
 
    public static String streamToString(InputStream is) throws IOException, UnsupportedEncodingException
    {
    	byte[] buf = new byte[1024];
    	int read = 0;
    	ByteArrayOutputStream data = new ByteArrayOutputStream();
    	
        try 
        {
	    	while ((read = is.read(buf)) > 0)
	    		data.write(buf, 0, read);
        }
        finally
        {
            try {is.close();} catch (IOException e) {}
        }
        String result = new String(data.toByteArray(), CHAR_ENCODING);
        return result;
    }

    public String handleResponse(HttpResponse response) throws IOException, UnsupportedEncodingException
    {
        responseCode = response.getStatusLine().getStatusCode();
        message = response.getStatusLine().getReasonPhrase();

        HttpEntity entity = response.getEntity();
 
        if (entity != null)
            return streamToString(entity.getContent());
        return null;
    }
    
    protected void assertResponseCode() throws IOException
    {
        if (responseCode >= 300)
        	throw new IOException(responseCode + " " + message);
    }
}