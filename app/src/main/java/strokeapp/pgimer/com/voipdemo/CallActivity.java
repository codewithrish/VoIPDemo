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

public class CallActivity extends AppCompatActivity implements View.OnClickListener {

    private Button callButton;
    private EditText receiver;
    private TextView callState;

    private Call call;

    private Button mRecordBtn, sendFileButton, discardFileButton;

    private static String mFileName = null;
    private static final String LOG_TAG = "Record_Log";

    private MediaRecorder mRecorder = null;
    private MediaPlayer mPlayer = null;

    private StorageReference mStorage;
    private DatabaseReference mDatabase;

    private ProgressDialog mProgressDialog;

    private File localFile = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

        MainActivity.sinchClient.setSupportCalling(true);
        MainActivity.sinchClient.startListeningOnActiveConnection();
        MainActivity.sinchClient.start();

        callButton = (Button) findViewById(R.id.callbutton);
        mRecordBtn = (Button) findViewById(R.id.recordBtn);
        sendFileButton = (Button) findViewById(R.id.sendFileButton);
        discardFileButton = (Button) findViewById(R.id.discardFileButton);

        receiver = (EditText) findViewById(R.id.receiverId);
        callState = (TextView) findViewById(R.id.callState);

        mProgressDialog = new ProgressDialog(this);

        mStorage = FirebaseStorage.getInstance().getReference();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        mFileName+="/recorded_audio.3gp";

        callButton.setOnClickListener(this);
        mRecordBtn.setOnClickListener(this);
        sendFileButton.setOnClickListener(this);
        discardFileButton.setOnClickListener(this);

        MainActivity.sinchClient.getCallClient().addCallClientListener(new SinchCallClientListener());



        mRecordBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    startRecording();
                }
                else if(event.getAction() == MotionEvent.ACTION_UP) {
                    stopRecording();
                    startPlaying(new File(mFileName));

                    sendFileButton.setVisibility(View.VISIBLE);
                    discardFileButton.setVisibility(View.VISIBLE);
                }
                return false;
            }
        });

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.callbutton:
                String receiverName = receiver.getText().toString();

                if(TextUtils.isEmpty(receiverName)) {
                    Toast.makeText(CallActivity.this, "Enter Receiver's ID", Toast.LENGTH_SHORT).show();
                } else {
                    if (call == null) {
                        try {
                            callState.setText("Connecting ....");
                            call = MainActivity.sinchClient.getCallClient().callUser(receiverName);
                            call.addCallListener(new SinchCallListener());
                            callButton.setText("Hang Up");
                            mRecordBtn.setVisibility(View.VISIBLE);
                        }catch(IllegalStateException e)
                        {
                            e.printStackTrace();
                        }
                    } else {
                        call.hangup();
                        call = null;
                        startActivity(new Intent(CallActivity.this, MainActivity.class));
                    }
                }
                break;
            case R.id.sendFileButton:
                stopPlaying();
                uploadAudio();
                sendFileButton.setVisibility(View.GONE);
                discardFileButton.setVisibility(View.GONE);
                break;
            case R.id.discardFileButton:
                stopPlaying();
                sendFileButton.setVisibility(View.GONE);
                discardFileButton.setVisibility(View.GONE);
                break;

        }
    }

    class SinchCallListener implements CallListener {
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

            mRecordBtn.setVisibility(View.VISIBLE);

            mRecordBtn.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if(event.getAction() == MotionEvent.ACTION_DOWN) {
                        startRecording();
                    }
                    else if(event.getAction() == MotionEvent.ACTION_UP) {
                        stopRecording();
                        startPlaying(new File(mFileName));

                        sendFileButton.setVisibility(View.VISIBLE);
                        discardFileButton.setVisibility(View.VISIBLE);

                        sendFileButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                stopPlaying();
                                uploadAudio();
                                sendFileButton.setVisibility(View.GONE);
                                discardFileButton.setVisibility(View.GONE);
                            }
                        });

                        discardFileButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                stopPlaying();
                                sendFileButton.setVisibility(View.GONE);
                                discardFileButton.setVisibility(View.GONE);
                            }
                        });

                    }
                    return false;
                }
            });

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

    class SinchCallClientListener implements CallClientListener {
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


    private void startRecording() {


        if( mRecorder == null ) {
            mRecorder = new MediaRecorder();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mRecorder.setOutputFile(mFileName);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        }

        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }

        mRecorder.start();
    }

    private void stopRecording() {
        mRecorder.stop();
        mRecorder.reset();
        mRecorder.release();
        mRecorder = null;
    }

    private void uploadAudio() {

        mProgressDialog.setMessage("Uploading Audio .... ");
        mProgressDialog.show();
        StorageReference filepath = mStorage.child("Audio").child("new_audio.3gp");
        Uri uri = Uri.fromFile(new File(mFileName));
        filepath.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                mProgressDialog.dismiss();
                updateDatabase();
            }
        });
    }

    private void downloadAudio() {
        mProgressDialog.setMessage("Downloading Audio .... ");
        mProgressDialog.show();

        StorageReference filepath = mStorage.child("Audio").child("new_audio.3gp");

        try {
            localFile = File.createTempFile("audio", "3gp");
        } catch (IOException e) {
            e.printStackTrace();
        }

        filepath.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                Toast.makeText(CallActivity.this, "File Downloaded", Toast.LENGTH_SHORT).show();
                mProgressDialog.dismiss();


                startPlaying(localFile);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(CallActivity.this, "File can't be Downloaded", Toast.LENGTH_SHORT).show();
                mProgressDialog.dismiss();
            }
        });
    }

    private void updateDatabase() {
        int randomNum = 0 + (int)(Math.random() * 2000);
        mDatabase.child("hey").setValue(randomNum+"");

        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                downloadAudio();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        //
    }

    private void startPlaying(File fMusic) {
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(fMusic.toString());
            Log.i("lkjhgfd", "1");
            mPlayer.prepare();
            Log.i("lkjhgfd", "2");
            mPlayer.start();
            Log.i("lkjhgfd", "3");
            mPlayer.setLooping(true);
            Log.i("lkjhgfd", "4");
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }
    }

    private void stopPlaying() {
        mPlayer.release();
        mPlayer = null;
    }
}