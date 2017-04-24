package strokeapp.pgimer.com.voipdemo;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.sinch.android.rtc.Sinch;
import com.sinch.android.rtc.SinchClient;

public class MainActivity extends AppCompatActivity {

    private EditText caller;
    private Button setUsername;

    public static SinchClient sinchClient;

    private static final String APP_KEY = "4dc40dc6-897a-4bf4-9bf7-19112a5f031b";
    private static final String APP_SECRET = "nSUGEgh99km3eicN4tIpKw==";
    private static final String ENVIRONMENT = "sandbox.sinch.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setUsername = (Button) findViewById(R.id.setUsername);

        caller = (EditText) findViewById(R.id.callerId);

        setUsername.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String userId = caller.getText().toString();

                if(TextUtils.isEmpty(userId)) {
                    Toast.makeText(MainActivity.this, "Enter Your User ID", Toast.LENGTH_SHORT).show();
                }
                else {
                    sinchClient = Sinch.getSinchClientBuilder()
                            .context(MainActivity.this)
                            .userId(caller.getText().toString())
                            .applicationKey(APP_KEY)
                            .applicationSecret(APP_SECRET)
                            .environmentHost(ENVIRONMENT)
                            .build();
                    startActivity(new Intent(MainActivity.this, CallActivity.class));
                }
            }
        });

    }
}
