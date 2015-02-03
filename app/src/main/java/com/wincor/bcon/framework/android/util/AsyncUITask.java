package com.wincor.bcon.framework.android.util;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface.OnCancelListener;
import android.os.Handler;

public abstract class AsyncUITask<T> implements Runnable,OnCancelListener
{
	private final Handler HANDLER = new Handler();
	private Context context = null;
	private ProgressDialog dialog = null;
	private boolean cancelled = false;

	public AsyncUITask(Context context)
	{
		this.context = context;
	}
	
	public Context getContext()
	{
		return context;
	}
	
	public void go(String msg)
	{
		go(msg, true);
	}
	
	public void go(String msg, boolean showDialog)
	{
		cancelled = false;
		if (showDialog) {
			dialog = ProgressDialog.show(context, "", msg, true, true);
			dialog.setCanceledOnTouchOutside(false);
			dialog.setOnCancelListener(this);
		}
    	new Thread(this).start();
	}
	
	public void onCancel(android.content.DialogInterface dialog)
	{
		cancelled = true;
	}
	  
	public void run()
	{
		try {
			T result = doTask();
			HANDLER.post(new CallBackNotifier(result, null));
		} catch (Throwable throwable) {
			HANDLER.post(new CallBackNotifier(null, throwable));
		}
	}
	
	public abstract T doTask() throws Throwable;
	
	public abstract void doneOk(T result);
	
	public abstract void doneFail(Throwable throwable);
	
	protected class CallBackNotifier implements Runnable
	{
		private T result = null;
		private Throwable throwable = null;
		public CallBackNotifier(T result, Throwable throwable) {
			this.result = result;
			this.throwable = throwable;
		}
		public void run() {
			if (cancelled) return; // do nothing if cancelled
			if (throwable != null)
				doneFail(throwable);
			else
				doneOk(result);
			try {
				if (dialog!=null) dialog.cancel();
			} catch (Exception ex) {
				// ignore
			}
		}
	}
}
