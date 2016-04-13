package dk.lemonmelon.pooetry;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class IntroActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        Button noButton = (Button) findViewById(R.id.intro_no_button);
        noButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        Button yesButton = (Button) findViewById(R.id.intro_yes_button);
        yesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent showToiletWall = new Intent(getApplicationContext(), ToiletWallActivity.class);
                startActivity(showToiletWall);
            }
        });

        LocationLoader locationLoader = new LocationLoader(this);
        locationLoader.triggerLoadingOfLocation();
    }
}
