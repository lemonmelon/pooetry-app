package dk.lemonmelon.pooetry;

import android.app.Activity;
import android.support.v7.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;


public class ToiletWallActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_toilet_wall);

        RelativeLayout container = (RelativeLayout) findViewById(R.id.content_container);

        final ScrollView verticalScrollView = (ScrollView) findViewById(R.id.vertical_scroll_view);
        final HorizontalScrollView horizontalScrollView = (HorizontalScrollView) findViewById(R.id.horizontal_scroll_view);

        AlertDialog.Builder inputDialogBuilder = new AlertDialog.Builder(this);
        inputDialogBuilder.setTitle("Write a pooem");
        inputDialogBuilder.setView(R.layout.dialog_input);
        inputDialogBuilder.setPositiveButton("Submit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                AlertDialog thisDialog = (AlertDialog) dialog;
                EditText textField = (EditText) thisDialog.findViewById(R.id.dialog_input_text_field);
                String textInput = textField.getText().toString();
                Log.d("pooetry", "Got that sweet text input " + textInput);

                startNotePostingRequest(getApplicationContext(), textInput);
            }
        });
        final AlertDialog inputDialog = inputDialogBuilder.create();

        final GestureDetector gestureDetector = new GestureDetector(this, new OnDoubleTapListener(new Runnable() {
            @Override
            public void run() {
                inputDialog.show();
            }
        }));

        verticalScrollView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                boolean goOn = gestureDetector.onTouchEvent(event);
                if(!goOn) {
                    return false;
                }
                return verticalScrollView.onTouchEvent(event);
            }
        });

        horizontalScrollView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                boolean goOn = gestureDetector.onTouchEvent(event);
                if(!goOn) {
                    return false;
                }
                return horizontalScrollView.onTouchEvent(event);
            }
        });

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

                Log.d("pooetry", "Got resulting strings:");
                for(String s : result){
                    Log.d("pooetry", "- " + s);
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        container.removeAllViews();

                        int leftMarginCursor = 0;
                        int topMarginCursor = 0;
                        for(String s : result) {
                            TextView v = new TextView(ctx);
                            v.setText(s);
                            v.setTextSize(12f + (float)(Math.random() * 10));

                            RelativeLayout.LayoutParams params;

                            if(Math.random() * 100 < 50.) {
                                params = new RelativeLayout.LayoutParams(375, ViewGroup.LayoutParams.WRAP_CONTENT);
                                params.leftMargin = leftMarginCursor;
                                params.topMargin = topMarginCursor;
                                leftMarginCursor += 450;
                                topMarginCursor += 40;
                            }
                            else {
                                //TODO: Really annoying that we can't get text to fill the height, then expand width to fit :-(
                                params = new RelativeLayout.LayoutParams(800, 375);
                                params.leftMargin = leftMarginCursor;
                                params.topMargin = topMarginCursor;
                                leftMarginCursor += 40;
                                topMarginCursor += 450;
                            }

                            container.addView(v, params);

                            Log.d("pooetry", "Added string " + s + " at " + leftMarginCursor + "," + topMarginCursor);
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

    private void startNotePostingRequest(Context ctx, String note) {
        new NotePostingRequestTask(ctx, note).execute();
    }

    private class NotePostingRequestTask extends AsyncTask<Void, Void, Void> {
        private String input;
        private Context ctx;

        public NotePostingRequestTask(Context ctx, String note) {
            this.ctx = ctx;
            JSONObject o = new JSONObject();
            try {
                o.put("text", note);
                o.put("long", "nulled");
                o.put("lat", "nulled");
                this.input = o.toString();
            }
            catch(JSONException e) {
                Log.e("pooetry", "Failed to prepare data for NotePostingRequestTask: " + e);
            }
        }

        protected Void doInBackground(Void... nothings) {
            final Context ctx = this.ctx;

            try {
                HttpClient httpClient = new DefaultHttpClient();
                HttpPost req = new HttpPost("http://api.pooetry.lemonmelon.dk:3009/note");
                req.setEntity(new StringEntity(input));
                req.setHeader("Content-Type", "application/json");
                HttpResponse res = httpClient.execute(req);
                if (res.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                    Log.e("pooetry", "Failed to post note. Instead, got " + res.getStatusLine().getStatusCode());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Failed to post note", Toast.LENGTH_LONG).show();
                        }
                    });
                    return null;
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Note poosted", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (IOException e) {
                Log.e("pooetry", "Got IOException while getting data from online " + e);
                return null;
            }
            return null;
        }
    }

    private class OnDoubleTapListener extends GestureDetector.SimpleOnGestureListener {
        Runnable _toBeDone;

        public OnDoubleTapListener(Runnable toBeDone) {
            _toBeDone = toBeDone;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            _toBeDone.run();
            return true;
        }
    }
}
