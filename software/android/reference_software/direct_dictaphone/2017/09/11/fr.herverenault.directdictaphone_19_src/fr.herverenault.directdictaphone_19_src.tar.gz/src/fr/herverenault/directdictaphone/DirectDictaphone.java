package fr.herverenault.directdictaphone;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder.AudioSource;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class DirectDictaphone extends Activity implements OnClickListener {

    private static final int ACTIVITY_LISTEN = 0;
    private static final int ACTIVITY_PREFS = 1;

    // 44100Hz is the only rate that is guaranteed to work on all devices
    private static final int SAMPLE_RATE = 44100;
    private static final int REFERENCE_BUFFER_SIZE = 4096;
    private static final int BUFFER_CLUSTER = 100; // around 5 sec
    private static final int THRESHOLD_INCREMENT = 100;
    private static final int MIN_OVER_THRESHOLD = 2;
    private static final int MIN_BEFORE_STABLE = 3;
    private static final int MAX_SILENCES = 60;
    private static final int START_SILENCE_THRESHOLD = 200;
    private static final int MAX_SILENCE_THRESHOLD = 2500;

    // to be fetched from prefs
    private boolean endSoundEnabled;
    private boolean stopOnSilence;
    private int maxRecordingTime;

    private MediaPlayer endSound, cancelSound;

    // flags
    private boolean readyToRecord = true;
    private boolean stopRecording = false;
    private boolean cancelRecording = false;
    private boolean isRecording = false;
    private boolean returnFromActivity = false;
    private boolean runningAnimation = false;

    private int mNumBufferRead;
    private int maxBufferRead;

    LinearLayout rootLayout;
    TextView textYouHave,
            textNumberNotes,
            textNotes,
            textRecording,
            textInstructions1,
            textInstructions2,
            textInstructions3;
    ArrayList<Bitmap> scaledBitmaps;
    ImageView pulsatingImage;
    ProgressBar progressBar;

    private static Context CONTEXT; // for toast and the sound in the asyncTask

    public static String FILE_PREFIX = "DirectDictaphone";

    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(getClass().getName(), "in onCreate Tid " + android.os.Process.myTid());

        setContentView(R.layout.main);

        // for toast and the sound in the asyncTask
        CONTEXT = this;

        // for modifying textViews
        textYouHave = (TextView) findViewById(R.id.text_you_have);
        textNumberNotes = (TextView) findViewById(R.id.text_number_of_notes);
        textNotes = (TextView) findViewById(R.id.text_notes);
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        textRecording = (TextView) findViewById(R.id.text_recording);
        textInstructions1 = (TextView) findViewById(R.id.text_instructions1);
        textInstructions2 = (TextView) findViewById(R.id.text_instructions2);
        textInstructions3 = (TextView) findViewById(R.id.text_instructions3);
        rootLayout = (LinearLayout) findViewById(R.id.root_layout); // for keepScreenOn true/false

        // for the pulsating red dot
        scaledBitmaps = new ArrayList<Bitmap>();
        pulsatingImage = (ImageView) findViewById(R.id.pulsating_image);
        Bitmap pulsatingBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.red_dot);
        int w = pulsatingBitmap.getWidth();
        int h = pulsatingBitmap.getHeight();

        for (double i = 0; i < Math.PI / 2; i += 0.15) {
            Bitmap scaledBMP = Bitmap.createScaledBitmap(pulsatingBitmap,
                    (int) (w * (0.4 + Math.abs(Math.sin(i)) * 0.6)),
                    (int) (h * (0.4 + Math.abs(Math.sin(i)) * 0.6)),
                    true);
            scaledBitmaps.add(scaledBMP);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(getClass().getName(), "in onStart Tid " + android.os.Process.myTid());

        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            Toast.makeText(DirectDictaphone.CONTEXT, getString(R.string.no_sd_card), Toast.LENGTH_LONG).show();
            finish();
        }

        // for restarts (after a long time in NotesList activity for example)
        pulsatingImage.setImageBitmap(scaledBitmaps.get(0));
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(getClass().getName(), "in onStop Tid " + android.os.Process.myTid());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(getClass().getName(), "in onDestroy Tid " + android.os.Process.myTid());
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(getClass().getName(), "in onRestart Tid " + android.os.Process.myTid());
    }

    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    protected void onResume() {
        super.onResume();
        Log.d(getClass().getName(), "in onResume isRecording " + isRecording + " returnFromActivity " + returnFromActivity +  " Tid " + android.os.Process.myTid());
        Log.d(getClass().getName(), "in onResume readyToRecord " + readyToRecord);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        endSoundEnabled = preferences.getBoolean("prefs_end_sound", true);
        if (endSoundEnabled) {
            endSound = MediaPlayer.create(CONTEXT, R.raw.end_sound);
            cancelSound = MediaPlayer.create(CONTEXT, R.raw.cancel_sound);
        }

        stopOnSilence = preferences.getBoolean("stop_on_silence", true);

        maxRecordingTime = Integer.parseInt(preferences.getString("max_recording_time", "45"));

        textNumberNotes.setText(String.valueOf(NotesFolder.numberNotes()));
        // in case we delete notes in NotesList
        if (NotesFolder.numberNotes() > 1) {
            textNotes.setText(getString(R.string.notes));
        } else {
            textNotes.setText(getString(R.string.note));
        }

        if (readyToRecord && !isRecording && !returnFromActivity) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                new RecordingTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                new AnimationImageTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            } else {
                new RecordingTask().execute();
                new AnimationImageTask().execute();
            }
        }
        readyToRecord = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(getClass().getName(), "in onPause Tid " + android.os.Process.myTid());

        if (isRecording) {
            Toast.makeText(this, getString(R.string.cancelled), Toast.LENGTH_SHORT).show();
        }
        isRecording = false; // stop animation thread
        cancelRecording = true; // stop recording without saving to file
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onClick(View v) {
        // configured in layout/main.xml
        Log.d(getClass().getName(), "in onClick, clicked view " + v.getId());

        switch (v.getId()) {
            case R.id.recorded_notes:
                Intent i = new Intent(this, NotesList.class);
                startActivityForResult(i, ACTIVITY_LISTEN);
                break;
            case R.id.root_layout:
                if (stopRecording) {
                    if (!runningAnimation) {

                        returnFromActivity = false;

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                            new RecordingTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                            new AnimationImageTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        } else {
                            new RecordingTask().execute();
                            new AnimationImageTask().execute();
                        }

                    }
                } else {
                    stopRecording = true;
                }
                break;
            case R.id.layout_text:
                if (isRecording) {
                    Toast.makeText(DirectDictaphone.CONTEXT, getString(R.string.cancelled), Toast.LENGTH_SHORT).show();
                }
                cancelRecording = true;
                stopRecording = true;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        Intent i;
        switch (item.getItemId()) {
            case R.id.menu_listen:
                i = new Intent(this, NotesList.class);
                startActivityForResult(i, ACTIVITY_LISTEN);
                break;
            case R.id.menu_prefs:
                i = new Intent(this, Preferences.class);
                startActivityForResult(i, ACTIVITY_PREFS);
                break;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        returnFromActivity = true;
    }

    private class RecordingTask extends AsyncTask<Void, Integer, Void> {

        @Override
        protected void onPreExecute() {
            isRecording = true;
            cancelRecording = false;
            stopRecording = false;
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... arg0) {

            int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                    AudioFormat.CHANNEL_CONFIGURATION_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            Log.d(getClass().getName(), "in doInBackground, bufferSize " + bufferSize);



            maxBufferRead = (int) Math.floor((SAMPLE_RATE * 2 * maxRecordingTime / bufferSize ) + 0.5);
            progressBar.setMax(maxBufferRead);

            byte[] mReadBuffer = new byte[bufferSize];
            byte[] someBuffers = new byte[BUFFER_CLUSTER * bufferSize];

            int silenceThreshold = START_SILENCE_THRESHOLD;
            int minOverThreshold = (int) Math.floor((MIN_OVER_THRESHOLD * REFERENCE_BUFFER_SIZE / bufferSize) + 0.5);
            int minBeforeStable = (int) Math.floor((MIN_BEFORE_STABLE * REFERENCE_BUFFER_SIZE / bufferSize) + 0.5);
            int maxSilences = (int) Math.floor((MAX_SILENCES * REFERENCE_BUFFER_SIZE / bufferSize) + 0.5);
            int thresholdIncrement = (int) Math.floor((THRESHOLD_INCREMENT * REFERENCE_BUFFER_SIZE / bufferSize) + 0.5);

            int bufferSum, bufferAverage;
            int nOverThreshold = 0;
            int nUnderThreshold = 0;
            boolean stabilizedThreshold = false;
            String dateTime = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());

            try {
                File sdCard = Environment.getExternalStorageDirectory();
                File folder = new File(sdCard.getAbsolutePath() + "/" + NotesFolder.PATH);
                if (!folder.exists() && !folder.mkdirs()) {
                    throw new Exception("Unable to create directory");
                }

                AudioRecord mAudioRecord = new AudioRecord(AudioSource.MIC, SAMPLE_RATE,
                        AudioFormat.CHANNEL_CONFIGURATION_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize);

                if (mAudioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                    throw new Exception("AudioRecord initialization failed");
                }

                RandomAccessFile mOutFile = new RandomAccessFile(folder + "/" + FILE_PREFIX + "_" + dateTime + ".part", "rw");

                // thanks http://i-liger.com/article/android-wav-audio-recording for the WAVE code
                mOutFile.setLength(0);
                mOutFile.writeBytes("RIFF");
                mOutFile.writeInt(0);
                mOutFile.writeBytes("WAVE");
                mOutFile.writeBytes("fmt ");
                mOutFile.writeInt(Integer.reverseBytes(16));
                mOutFile.writeShort(Short.reverseBytes((short) 1));
                mOutFile.writeShort(Short.reverseBytes((short) 1));
                mOutFile.writeInt(Integer.reverseBytes(SAMPLE_RATE));
                mOutFile.writeInt(Integer.reverseBytes(SAMPLE_RATE * 2));
                mOutFile.writeShort(Short.reverseBytes((short) 2));
                mOutFile.writeShort(Short.reverseBytes((short) 16));
                mOutFile.writeBytes("data");
                mOutFile.writeInt(0);

                mAudioRecord.startRecording();

                for (mNumBufferRead = 0; mNumBufferRead < maxBufferRead && nUnderThreshold < maxSilences && !stopRecording && !cancelRecording; mNumBufferRead++) {

                    mAudioRecord.read(mReadBuffer, 0, bufferSize);

                    if (mNumBufferRead > 0 && mNumBufferRead % BUFFER_CLUSTER == 0) {
                        mOutFile.write(someBuffers);
                    }

                    System.arraycopy(mReadBuffer, 0, someBuffers, (mNumBufferRead % BUFFER_CLUSTER) * bufferSize, bufferSize);

                    bufferSum = 0;

                    // average for current buffer
                    for (int j = 0; j < mReadBuffer.length; j += 2) {
                        // get 16 bit value
                        int k = mReadBuffer[j] + (mReadBuffer[j + 1] << 8);
                        bufferSum += Math.abs(k);
                    }
                    // buffer is 8 bit, sum is 16 bit => * 2
                    bufferAverage = Math.round(bufferSum / mReadBuffer.length * 2);

                    if (stopOnSilence) {
                        if (stabilizedThreshold) {
                            if (bufferAverage > silenceThreshold) {
                                nOverThreshold++;
                                if (nOverThreshold > minOverThreshold) {
                                    nUnderThreshold = 0;
                                }
                            } else {
                                nUnderThreshold++;
                                nOverThreshold = 0;
                            }
                        } else {
                            if (mNumBufferRead > 0) {
                                if (bufferAverage < silenceThreshold) {
                                    nUnderThreshold++;
                                    if (nUnderThreshold > 0) {
                                        stabilizedThreshold = true;
                                        nOverThreshold = 0;
                                        nUnderThreshold = 0;
                                    }
                                } else {
                                    nOverThreshold++;
                                    if (nOverThreshold > minBeforeStable) {
                                        silenceThreshold += thresholdIncrement;
                                        nOverThreshold = 0;
                                    }
                                    if (silenceThreshold >= MAX_SILENCE_THRESHOLD) {
                                        stabilizedThreshold = true;
                                        nOverThreshold = 0;
                                    }
                                }
                            }
                        }
                    }

                    publishProgress(mNumBufferRead, bufferAverage, nUnderThreshold, nOverThreshold);
                }

                mAudioRecord.stop();
                mAudioRecord.release();
                mAudioRecord = null;

                Log.d(getClass().getName(), "end of recording, cancelRecording " + cancelRecording + " Tid " + android.os.Process.myTid());

                if (!cancelRecording) {
                    mOutFile.write(someBuffers);
                    mOutFile.setLength(40 + mNumBufferRead * bufferSize);
                    mOutFile.seek(4);
                    mOutFile.writeInt(Integer.reverseBytes(36 + mNumBufferRead * bufferSize));
                    mOutFile.seek(40);
                    mOutFile.writeInt(Integer.reverseBytes(mNumBufferRead * bufferSize));
                    mOutFile.close();
                    new File(folder + "/" + FILE_PREFIX + "_" + dateTime + ".part")
                            .renameTo(new File(folder + "/" + FILE_PREFIX + "_" + dateTime + ".wav"));
                }
                else {
                    mOutFile.setLength(0);
                    mOutFile.close();
                    new File(folder + "/" + FILE_PREFIX + "_" + dateTime + ".part").delete();
                }


                isRecording = false;
                stopRecording = true;
            } catch (Exception e) {
                Log.e(getClass().getName(), "Exception " + e.getMessage());
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            Log.d(getClass().getName(), "in onPostExecute Tid " + android.os.Process.myTid());
            if (endSoundEnabled) {
                if (cancelRecording) {
                    cancelSound.start();
                } else {
                    endSound.start();
                }
            }
        }
    }

    private class AnimationImageTask extends AsyncTask<Void, Integer, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            Log.d(getClass().getName(), "in doInBackground, isRecording " + isRecording + "  Tid " + android.os.Process.myTid());
            while (isRecording) {
                for (int i = 1 - scaledBitmaps.size(); i < scaledBitmaps.size(); i++) {
                    try {
                        Thread.sleep(100, 0);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    publishProgress(scaledBitmaps.size() - 1 - Math.abs(i));
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            pulsatingImage.setImageBitmap(scaledBitmaps.get(values[0]));
            progressBar.setProgress(mNumBufferRead);
        }

        @Override
        protected void onPreExecute() {
            runningAnimation = true;
            mNumBufferRead = 0;

            rootLayout.setKeepScreenOn(true);

            textNumberNotes.setText(String.valueOf(NotesFolder.numberNotes()));
            if (NotesFolder.numberNotes() > 1) {
                textNotes.setText(getString(R.string.notes));
            } else {
                textNotes.setText(getString(R.string.note));
            }
            textRecording.setText(getString(R.string.recording));
            textInstructions1.setText(getString(R.string.instructions1_recording));
            textInstructions2.setText(getString(R.string.instructions2_recording));
            textInstructions3.setText(getString(R.string.instructions3_recording));
        }

        @Override
        protected void onPostExecute(Void result) {
            rootLayout.setKeepScreenOn(false);

            textNumberNotes.setText(String.valueOf(NotesFolder.numberNotes()));
            if (NotesFolder.numberNotes() > 1) {
                textNotes.setText(getString(R.string.notes));
            } else {
                textNotes.setText(getString(R.string.note));
            }
            textRecording.setText(getString(R.string.ready_to_record));
            textInstructions1.setText(getString(R.string.instructions1_stopped));
            textInstructions2.setText(getString(R.string.instructions2_stopped));
            textInstructions3.setText(getString(R.string.instructions3_stopped));

            runningAnimation = false;
            progressBar.setProgress(0);
        }
    }
}
