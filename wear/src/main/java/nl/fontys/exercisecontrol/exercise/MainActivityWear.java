package nl.fontys.exercisecontrol.exercise;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Sensor;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import nl.fontys.exercisecontrol.exercise.collector.ExerciseData;
import nl.fontys.exercisecontrol.exercise.collector.JsonMeasurementCollector;
import nl.fontys.exercisecontrol.exercise.recorder.MeasurementCollector;
import nl.fontys.exercisecontrol.exercise.recorder.MeasurementException;
import nl.fontys.exercisecontrol.exercise.recorder.MeasurementRecorder;

public class MainActivityWear extends Activity {

    private ConnectionHandler handler;
    private MeasurementRecorder recorder;
    private MeasurementCollector collector;
    private Chronometer chronometer;
    private TextView headerLbl;
    private String exerciseName="unknown exercise";
    private String exerciseGUID ="";
    private final static String TAG="LOG";
    private Button startBtn;
    private Button stopBtn;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        handler = ObjectHelper.getInstance(getApplicationContext()).getConnectionHandler();


        final WatchViewStub stub = (WatchViewStub)findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub watchViewStub) {
                TextView textView = (TextView) findViewById(R.id.headerLbl);
                Bundle bundle = getIntent().getExtras();
                String name = bundle.getString(SelectExerciseActivityWear.EXERCISE_NAME);
                textView.setText(name);

            }
        });

        collector = new JsonMeasurementCollectorImpl();
        recorder = new MeasurementRecorder(this,initSensors(), 10, collector);
        recorder.initialize();


    }

    private void toogleButtons() {
        startBtn = (Button) findViewById(R.id.startBtn);
        stopBtn = (Button) findViewById(R.id.stopBtn);
        if(startBtn.isEnabled()) {
            startBtn.setEnabled(false);
            stopBtn.setEnabled(true);
        }
        else {
            startBtn.setEnabled(true);
            stopBtn.setEnabled(false);
        }
    }
    private String getExerciseName() {
        Intent intent = getIntent();
        return intent.getStringExtra(SelectExerciseActivityWear.EXERCISE_NAME);
    }
    private String getExerciseGUID() {
        Intent intent = getIntent();
        return intent.getStringExtra(SelectExerciseActivityWear.EXERCISE_GUID);
    }

    /**
     * setting the name of the exerccise to the header
     */
    private void setExerciseNameToHeaderLbl() {
        headerLbl = (TextView ) findViewById(R.id.headerLbl);
        exerciseName = getExerciseName();
        headerLbl.setText(exerciseName);
    }
    /**
     * create sensor data
     */
    private int[] initSensors() {
        int[] sensors = new int[2];
        sensors[0]=Sensor.TYPE_GYROSCOPE;
        sensors[1]=Sensor.TYPE_LINEAR_ACCELERATION;
        return sensors;
    }
    @Override
    protected void onResume(){
        super.onResume();
      //  recorder.start();
    }
    @Override
    public void onDestroy() {
    super.onDestroy();
    //handler.disconnectGoogleClient();

    recorder.stop();
    recorder.terminate();
    }

    /**
     * start the measurement
     * start chronometer
     * set the Exercise nameS
     * @param view
     */
    public void start(View view) {
        if(isConnectedToPhone()) {
            exerciseGUID = getExerciseGUID();
            chronometer = (Chronometer) findViewById(R.id.chronometer);
            chronometer.setBase(SystemClock.elapsedRealtime()); //reset to 0
            recorder.start(exerciseGUID);
            chronometer.start();
            setExerciseNameToHeaderLbl();
            toogleButtons();
        }
        else {
            showToast(getString(R.string.errorConnectionToPhone), Toast.LENGTH_LONG);
        }

    }

    /**
     * TODO: enable, disabled during debug
     * check if phone and watch are connected
     * @return
     */
    private boolean isConnectedToPhone() {
     /*
        if(!handler.isConnected()) {
            handler.connectGogleClient();
        }
        return handler.isConnected();
        */
        return true;
    }
    /**
     * stop the measurement
     * stop chronometer
     * @param view
     */
    public void stop(View view) {
        chronometer = (Chronometer) findViewById(R.id.chronometer);
        chronometer.stop();
        recorder.stop();
        toogleButtons();
    }

    /**
     * display a toast message
     * @param message text to display
     * @param length time how long the display is show
     */
    private void showToast(String message, int length) {
        Toast toast = Toast.makeText(this, message, length);
        toast.show();
    }

    private class JsonMeasurementCollectorImpl extends JsonMeasurementCollector {

        @Override
        public void startCollecting(String exerciseName) throws MeasurementException {
            Log.d(TAG, "Started collecting measurements.");
            super.startCollecting(exerciseGUID);
        }

        @Override
        public void stopCollecting(double time) throws MeasurementException {
            super.stopCollecting(time);
            Log.d(TAG, "Stopped collecting measurements. Recorded length: " + time);
        }

        @Override
        public void collectionComplete(ExerciseData data) {
            Log.d("log","measurement completed");

            Gson gson = new Gson();
            Log.d(TAG,"guid is " +data.getGuid());
            String result = gson.toJson(data);
            Log.d(TAG, result);
            sendMessage(result);
        }

        /**
         * send message with text and show toast
         * @param text text of message
         **/
        private void sendMessage(String text) {
             if(isConnectedToPhone()) {
                handler.sendMessage(text);
                showToast(getString(R.string.messageSendSuccess), Toast.LENGTH_LONG);
            }
            else {
                 showToast(getString(R.string.messageSendFailed), Toast.LENGTH_LONG);
            }

        }
        @Override
        public void collectionFailed(MeasurementException ex) {
            Log.d(TAG, "Measurement failed: " + ex.getMessage());
            showToast(getString(R.string.measurementFailed), Toast.LENGTH_LONG);
        }
    }
}
