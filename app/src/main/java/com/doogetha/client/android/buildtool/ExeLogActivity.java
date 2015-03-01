package com.doogetha.client.android.buildtool;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketHandler;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class ExeLogActivity extends Activity {

    private final Handler uiHandler = new Handler();

    private ScrollView scroller = null;
    private TextView logView = null;
    private WebSocketConnection mWebSocketClient = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_exe_log);

        String job = getIntent().getExtras().getString("job");

        setTitle(job);

        this.scroller = (ScrollView)findViewById(R.id.logScroller);
        this.logView = (TextView)findViewById(R.id.logView);

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        //findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);

        try {
            mWebSocketClient = new WebSocketConnection();
            mWebSocketClient.connect("ws://www.doogetha.com/buildtool/ws/exelog/out/test/test", new WebSocketHandler() {
                @Override
                public void onOpen() {
                    Log.i("Websocket", "Opened");
                }

                @Override
                public void onTextMessage(String s) {
                    final String message = s;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            logView.append(message);
                            logView.append("\n"); // XXX
                            scroller.fullScroll(View.FOCUS_DOWN);
                        }
                    });
                }

                @Override
                public void onClose(int i, String s) {
                    Log.i("Websocket", "Closed " + s);
                }
            });

        } catch (Exception e) {
            Log.e("Websocket", "Failure", e);
        }
    }

    @Override
    public void onDestroy() {
        mWebSocketClient.disconnect();
        super.onDestroy();
    }
}
