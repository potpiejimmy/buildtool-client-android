package com.wincor.bcon.framework.android.util;

import java.io.IOException;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class CustomSSLSocketFactory extends org.apache.http.conn.ssl.SSLSocketFactory
{
	public static class DummyX509TrustManager implements X509TrustManager {

	    public void checkClientTrusted(
	            X509Certificate[] chain,
	            String authType) throws CertificateException {
	    }

	    public void checkServerTrusted(
	            X509Certificate[] chain,
	            String authType) throws CertificateException {
	    }

	    public X509Certificate[] getAcceptedIssuers() {
	        return null;
	    }
	    
	};

	
	private SSLSocketFactory factory = null;

	public CustomSSLSocketFactory () throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException
    {
		super(null);
        SSLContext context = SSLContext.getInstance ("TLS");
        TrustManager[] tm = new TrustManager[] { new DummyX509TrustManager () };
        context.init (null, tm, new SecureRandom ());

        factory = context.getSocketFactory ();
    }

	public Socket createSocket() throws IOException
	{
	    return factory.createSocket();
	}

	public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException
	{
	    return factory.createSocket(socket, host, port, autoClose);
	}
}
