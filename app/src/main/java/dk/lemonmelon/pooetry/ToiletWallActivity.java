package dk.lemonmelon.pooetry;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


public class ToiletWallActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_toilet_wall);

        RelativeLayout container = (RelativeLayout) findViewById(R.id.content_container);

        ScrollView verticalScrollView = (ScrollView) findViewById(R.id.vertical_scroll_view);
        HorizontalScrollView horizontalScrollView = (HorizontalScrollView) findViewById(R.id.horizontal_scroll_view);

        //TODO: Make this scrolly shit work so we start in center
        //verticalScrollView.scrollTo(0, 15);
        //horizontalScrollView.scrollTo(5000, 0);
        //Log.d("pooetry", "scrolled");

        startContentRequestTask(this, container);
    }

    private void startContentRequestTask(Context ctx, RelativeLayout container) {
        new ContentRequestTask(ctx, container).execute();
    }

    private class ContentRequestTask extends AsyncTask<Void, Void, Void> {
        private RelativeLayout container;
        private Context ctx;

        public ContentRequestTask(Context ctx, RelativeLayout container) {
            this.ctx = ctx;
            this.container = container;
        }

        protected Void doInBackground(Void... nothings) {
            final RelativeLayout container = this.container;
            final Context ctx = this.ctx;

            InputStream bodyStream = null;
            try {
                HttpClient httpClient = new DefaultHttpClient();
                HttpResponse res = httpClient.execute(new HttpGet("http://api.pooetry.lemonmelon.dk:3009/content?long=x&lat=y"));
                if(res.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                    Log.e("pooetry", "Failed to get content. Instead, got " + res);
                    return null;
                }
                bodyStream = res.getEntity().getContent();
            }
            catch(IOException e) {
                Log.e("pooetry", "Got IOException while getting data from online " + e);
                return null;
            }

            BufferedReader r = new BufferedReader(new InputStreamReader(bodyStream));
            StringBuilder bodyStr = new StringBuilder();
            String line;
            try {
                while ((line = r.readLine()) != null) {
                    bodyStr.append(line);
                }
            }
            catch(IOException e) {
                Log.e("pooetry", "Got IOException while reading body stream " + e);
                return null;
            }

            try {
                JSONObject body = new JSONObject(bodyStr.toString());
                JSONArray notes = (JSONArray) body.get("notes");

                if(notes.length() < 1) {
                    Log.d("pooetry", "No notes");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            container.removeAllViews();

                            TextView failureText = new TextView(ctx);
                            failureText.setText("Failed to load notes");

                            container.addView(failureText);
                        }
                    });
                    return null;
                }

                final String[] result = new String[notes.length()];

                for(int i = 0; i < notes.length(); i++) {
                    String note = notes.getString(i);
                    result[i] = note;
                }

                Log.d("pooetry", "Got resulting strings: " + result);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        for(int i = 0; i < result.length; i++) {
                            container.removeAllViews();

                            TextView v = new TextView(ctx);
                            v.setText(result[i]);

                            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(400, ViewGroup.LayoutParams.WRAP_CONTENT);
                            params.leftMargin = i * 500;
                            params.topMargin = i * 25;

                            container.addView(v, params);
                        }
                    }
                });

                return null;
            }
            catch(JSONException e) {
                Log.e("pooetry", "Got JSONException while parsing body string " + e);
            }

            return null;
        }
    }
}
