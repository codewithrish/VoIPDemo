package strokeapp.pgimer.com.voipdemo;

import android.app.ProgressDialog;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.firebase.client.Firebase;
import com.firebase.client.FirebaseApp;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.sinch.android.rtc.PushPair;
import com.sinch.android.rtc.Sinch;
import com.sinch.android.rtc.SinchClient;
import com.sinch.android.rtc.calling.Call;
import com.sinch.android.rtc.calling.CallClient;
import com.sinch.android.rtc.calling.CallClientListener;
import com.sinch.android.rtc.calling.CallListener;

import org.w3c.dom.Text;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class CallActivity extends AppCompatActivity {

    private Button callButton;
    private EditText receiver;
    private TextView callState;

    private Call call;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

        MainActivity.sinchClient.setSupportCalling(true);
        MainActivity.sinchClient.startListeningOnActiveConnection();
        MainActivity.sinchClient.start();

        callButton = (Button) findViewById(R.id.callbutton);
        receiver = (EditText) findViewById(R.id.receiverId);
        callState = (TextView) findViewById(R.id.callState);

        callButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // make a call!
                String receiverName = receiver.getText().toString();

                if(TextUtils.isEmpty(receiverName)) {
                    Toast.makeText(CallActivity.this, "Enter Receivers ID", Toast.LENGTH_SHORT).show();
                }
                else {
                    if (call == null) {
                        callState.setText("Connecting ....");
                        call = MainActivity.sinchClient.getCallClient().callUser(receiverName);
                        call.addCallListener(new SinchCallListener());

                        callButton.setText("Hang Up");

                        callButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                call.hangup();
                                call = null;
                                startActivity(new Intent(CallActivity.this, MainActivity.class));
                            }
                        });
                    }
                }
            }
        });

        MainActivity.sinchClient.getCallClient().addCallClientListener(new SinchCallClientListener());

    }
    private class SinchCallListener implements CallListener {
        @Override
        public void onCallEnded(Call endedCall) {
            //call ended by either party
            setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);
            //onCallEnded
            call = null;
            callState.setText("Call Ended Reconnect");
            startActivity(new Intent(CallActivity.this, MainActivity.class));

        }
        @Override
        public void onCallEstablished(Call establishedCall) {
            //incoming call was picked up
            setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
            //onCallEstablished
            callState.setText("Connected");
        }
        @Override
        public void onCallProgressing(Call progressingCall) {
            //call is ringing
            //onCallProgressing
            callState.setText("Ringing");
        }
        @Override
        public void onShouldSendPushNotification(Call call, List<PushPair> pushPairs) {
            //don't worry about this right now
        }
    }

    private class SinchCallClientListener implements CallClientListener {
        @Override
        public void onIncomingCall(CallClient callClient, Call incomingCall) {
            //Pick up the call!
            call = incomingCall;
            call.answer();
            call.addCallListener(new SinchCallListener());
            callButton.setText("Hang Up");

            callButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(CallActivity.this, MainActivity.class));
                }
            });


        }
    }
}