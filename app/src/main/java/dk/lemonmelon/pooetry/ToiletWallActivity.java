package dk.lemonmelon.pooetry;

import android.app.Activity;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
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

public class ToiletWallActivity extends Activity {
    private Location _location = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_toilet_wall);

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        _location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        if(_location == null) {
            Toast.makeText(this, "Failed to get location. Turn on GPS, then try to start the app again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

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
                EditText textField = (EditText) inputDialog.findViewById(R.id.dialog_input_text_field);
                textField.setText("");
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

            BufferedReader r = null;
            try {
                HttpClient httpClient = new DefaultHttpClient();
                HttpResponse res = httpClient.execute(new HttpGet("http://api.pooetry.lemonmelon.dk:3009/content?long=" + _location.getLongitude() + "&lat=" + _location.getLatitude()));
                if(res.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                    Log.e("pooetry", "Failed to get content. Instead, got " + res);
                    return null;
                }
                InputStream bodyStream = res.getEntity().getContent();
                r = new BufferedReader(new InputStreamReader(bodyStream, "UTF8"));
            }
            catch(IOException e) {
                Log.e("pooetry", "Got IOException while getting data from online " + e);
                reportError();
                return null;
            }

            StringBuilder bodyStr = new StringBuilder();
            String line;
            try {
                while ((line = r.readLine()) != null) {
                    bodyStr.append(line);
                }
            }
            catch(IOException e) {
                Log.e("pooetry", "Got IOException while reading body stream " + e);
                reportError();
                return null;
            }

            try {
                JSONObject body = new JSONObject(bodyStr.toString());
                JSONArray notes = (JSONArray) body.get("notes");

                if(notes.length() < 1) {
                    Log.d("pooetry", "No notes");
                    reportError();
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
                reportError();
            }

            return null;
        }

        private void reportError() {
            Log.e("pooetry", "Something went wrong/failed to load notes/showing dialog and closing");

            final RelativeLayout container = this.container;

            final AlertDialog.Builder errorAlertBuilder = new AlertDialog.Builder(this.ctx);
            errorAlertBuilder.setTitle("Failed to load notes");
            errorAlertBuilder.setMessage("Something went wrong while requesting notes from the internet. Maybe your connection is bad? Start app again to retry.");
            errorAlertBuilder.setNegativeButton("Shutdown", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AlertDialog errorAlert = errorAlertBuilder.create();
                    errorAlert.setCanceledOnTouchOutside(false);
                    container.removeAllViews();
                    errorAlert.show();
                }
            });
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
                o.put("long", "" + _location.getLongitude());
                o.put("lat", "" + _location.getLatitude());
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
                req.setHeader("Content-Type", "application/json; charset=utf-8");
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
