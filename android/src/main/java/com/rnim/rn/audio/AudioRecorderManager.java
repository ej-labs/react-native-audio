package com.rnim.rn.audio;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

// import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
// import java.io.FileNotFoundException;
// import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import 	java.lang.ArrayIndexOutOfBoundsException;

// import android.util.Base64;
// import java.io.FileInputStream;

// import java.lang.reflect.Method;

class AudioRecorderManager extends ReactContextBaseJavaModule {

  private static final String TAG = "ReactNativeAudio";

  private static final String DocumentDirectoryPath = "DocumentDirectoryPath";
  private static final String PicturesDirectoryPath = "PicturesDirectoryPath";
  private static final String MainBundlePath = "MainBundlePath";
  private static final String CachesDirectoryPath = "CachesDirectoryPath";
  private static final String LibraryDirectoryPath = "LibraryDirectoryPath";
  private static final String MusicDirectoryPath = "MusicDirectoryPath";
  private static final String DownloadsDirectoryPath = "DownloadsDirectoryPath";
  private static final float MAX_REPORTABLE_AMP = 32767f;
	private static final float MAX_REPORTABLE_DB = 90.3087f;

  private Context context;
  private String currentOutputFile;
  private boolean isRecording = false;
  private boolean meteringEnabled = false;


  private static final int PERMISSION_RECORD_AUDIO = 0;

  private RecordWaveTask recordTask = null;
  // private boolean includeBase64 = false;
  

  public AudioRecorderManager(ReactApplicationContext reactContext) {
    super(reactContext);
    this.context = reactContext;
  }

  @Override
  public Map<String, Object> getConstants() {
    Map<String, Object> constants = new HashMap<>();
    constants.put(DocumentDirectoryPath, this.getReactApplicationContext().getFilesDir().getAbsolutePath());
    constants.put(PicturesDirectoryPath, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath());
    constants.put(MainBundlePath, "");
    constants.put(CachesDirectoryPath, this.getReactApplicationContext().getCacheDir().getAbsolutePath());
    constants.put(LibraryDirectoryPath, "");
    constants.put(MusicDirectoryPath, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath());
    constants.put(DownloadsDirectoryPath, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath());
    return constants;
  }

  @Override
  public String getName() {
    return "AudioRecorderManager";
  }

  @ReactMethod
  public void checkAuthorizationStatus(Promise promise) {
    int permissionCheck = ContextCompat.checkSelfPermission(getCurrentActivity(),
            Manifest.permission.RECORD_AUDIO);
    boolean permissionGranted = permissionCheck == PackageManager.PERMISSION_GRANTED;
    promise.resolve(permissionGranted);
  }

  @ReactMethod
  public void prepareRecordingAtPath(String recordingPath, ReadableMap recordingSettings, Promise promise) {
    if (isRecording){
      logAndRejectPromise(promise, "INVALID_STATE", "Please call stopRecording before starting recording");
    }

    try {
      meteringEnabled = recordingSettings.getBoolean("MeteringEnabled");
      // recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
      // int outputFormat = getOutputFormatFromString(recordingSettings.getString("OutputFormat"));
      // recorder.setOutputFormat(outputFormat);
      // int audioEncoder = getAudioEncoderFromString(recordingSettings.getString("AudioEncoding"));
      // recorder.setAudioEncoder(audioEncoder);
      // recorder.setAudioSamplingRate(recordingSettings.getInt("SampleRate"));
      // recorder.setAudioChannels(recordingSettings.getInt("Channels"));
      // recorder.setAudioEncodingBitRate(recordingSettings.getInt("AudioEncodingBitRate"));
      // recorder.setOutputFile(recordingPath);
      // includeBase64 = recordingSettings.getBoolean("IncludeBase64");
    }
    catch(final Exception e) {
      logAndRejectPromise(promise, "COULDNT_CONFIGURE_MEDIA_RECORDER" , "Make sure you've added RECORD_AUDIO permission to your AndroidManifest.xml file "+e.getMessage());
      return;
    }

    currentOutputFile = recordingPath;
    try {

      if (recordTask == null) {
        recordTask = new RecordWaveTask(context, this);
      } else {
        recordTask.setContext(context);
      }
      promise.resolve(currentOutputFile);
    } catch (final Exception e) {
      logAndRejectPromise(promise, "COULDNT_PREPARE_RECORDING_AT_PATH "+recordingPath, e.getMessage());
    }

  }


  private int getAudioEncoderFromString(String audioEncoder) {
   switch (audioEncoder) {
     case "aac":
       return MediaRecorder.AudioEncoder.AAC;
     case "aac_eld":
       return MediaRecorder.AudioEncoder.AAC_ELD;
     case "amr_nb":
       return MediaRecorder.AudioEncoder.AMR_NB;
     case "amr_wb":
       return MediaRecorder.AudioEncoder.AMR_WB;
     case "he_aac":
       return MediaRecorder.AudioEncoder.HE_AAC;
     case "vorbis":
      return MediaRecorder.AudioEncoder.VORBIS;
     default:
       Log.d("INVALID_AUDIO_ENCODER", "USING MediaRecorder.AudioEncoder.DEFAULT instead of "+audioEncoder+": "+MediaRecorder.AudioEncoder.DEFAULT);
       return MediaRecorder.AudioEncoder.DEFAULT;
   }
  }

  private int getOutputFormatFromString(String outputFormat) {
    switch (outputFormat) {
      case "mpeg_4":
        return MediaRecorder.OutputFormat.MPEG_4;
      case "aac_adts":
        return MediaRecorder.OutputFormat.AAC_ADTS;
      case "amr_nb":
        return MediaRecorder.OutputFormat.AMR_NB;
      case "amr_wb":
        return MediaRecorder.OutputFormat.AMR_WB;
      case "three_gpp":
        return MediaRecorder.OutputFormat.THREE_GPP;
      case "webm":
        return MediaRecorder.OutputFormat.WEBM;
      default:
        Log.d("INVALID_OUPUT_FORMAT", "USING MediaRecorder.OutputFormat.DEFAULT : "+MediaRecorder.OutputFormat.DEFAULT);
        return MediaRecorder.OutputFormat.DEFAULT;

    }
  }

  @ReactMethod
  public void startRecording(Promise promise){

    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
      // Request permission
      ActivityCompat.requestPermissions(getCurrentActivity(),
              new String[] { Manifest.permission.RECORD_AUDIO },
              PERMISSION_RECORD_AUDIO);
      return;
    }
    // Permission already available
    launchTask();
    isRecording = true;
    promise.resolve(currentOutputFile);

  }

  private void launchTask() {
    switch (recordTask.getStatus()) {
      case RUNNING:
//        Toast.makeText(context, "Task already running...", Toast.LENGTH_SHORT).show();
        return;
      case FINISHED:
        recordTask = new RecordWaveTask(context, this);
        break;
      case PENDING:
        if (recordTask != null && recordTask.isCancelled()) {
          recordTask = new RecordWaveTask(context, this);
        }
    }
    File wavFile = new File(currentOutputFile);
//    Toast.makeText(context, wavFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
    recordTask.execute(wavFile);
  }


  @ReactMethod
  public void stopRecording(Promise promise){

    if (recordTask != null && !recordTask.isCancelled() && recordTask.getStatus() == AsyncTask.Status.RUNNING) {
      recordTask.cancel(false);
    }

    isRecording = false;
    promise.resolve(currentOutputFile);

    WritableMap result = Arguments.createMap();
    result.putString("status", "OK");
    result.putString("audioFileURL", "file://" + currentOutputFile);

    // String base64 = "";
    // if (includeBase64) {
    //   try {
    //     InputStream inputStream = new FileInputStream(currentOutputFile);
    //     byte[] bytes;
    //     byte[] buffer = new byte[8192];
    //     int bytesRead;
    //     ByteArrayOutputStream output = new ByteArrayOutputStream();
    //     try {
    //       while ((bytesRead = inputStream.read(buffer)) != -1) {
    //         output.write(buffer, 0, bytesRead);
    //       }
    //     } catch (IOException e) {
    //       Log.e(TAG, "FAILED TO PARSE FILE");
    //     }
    //     bytes = output.toByteArray();
    //     base64 = Base64.encodeToString(bytes, Base64.DEFAULT);
    //   } catch(FileNotFoundException e) {
    //     Log.e(TAG, "FAILED TO FIND FILE");
    //   }
    // }
    // result.putString("base64", base64);

    sendEvent("recordingFinished", result);
  }



  @ReactMethod
  public void pauseRecording(Promise promise){
    // Added this function to have the same api for android and iOS, stops recording now
    stopRecording(promise);
  }

  public void sendMeter(double amplitude, int recorderSecondsElapsed){
        WritableMap body = Arguments.createMap();
        body.putInt("currentTime", recorderSecondsElapsed);
        if(meteringEnabled){
          if (amplitude == 0) {
            body.putInt("currentMetering", -160);//The first call - absolutely silence
          } else {
            //db = 20 * log10(peaks/ 32767); where 32767 - max value of amplitude in Android, peaks - current value
            Double dMetter = 20 * Math.log(amplitude / MAX_REPORTABLE_AMP);
            body.putInt("currentMetering", dMetter.intValue());
          }
        }
        sendEvent("recordingProgress", body);
  }



  private void sendEvent(String eventName, Object params) {
    getReactApplicationContext()
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit(eventName, params);
  }

  private void logAndRejectPromise(Promise promise, String errorCode, String errorMessage) {
    Log.e(TAG, errorMessage);
    promise.reject(errorCode, errorMessage);
  }

  private static class RecordWaveTask extends AsyncTask<File, Void, Object[]> {

    // Configure me!
    private static final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    private static final int SAMPLE_RATE = 16000; // Hz
    private static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int CHANNEL_MASK = AudioFormat.CHANNEL_IN_MONO;
    //

    private static final int BUFFER_SIZE = 2 * AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_MASK, ENCODING);

    private Context ctx;
    private AudioRecorderManager audioRecorderManager;

    int recorderSecondsElapsed = 0;
    Timer timer;
    double metering = 0f;

    private RecordWaveTask(Context ctx,  AudioRecorderManager audioRecorderManager) {
      setContext(ctx);
      setAudioRecorderManager(audioRecorderManager);
    }

    private void stopTimer(){
      recorderSecondsElapsed = 0;
      if (timer != null) {
        timer.cancel();
        timer.purge();
        timer = null;
      }
    }

    private void setContext(Context ctx) {
      stopTimer();
      this.ctx = ctx;
    }

    private void setAudioRecorderManager(AudioRecorderManager audioRecorderManager){
      this.audioRecorderManager = audioRecorderManager;
    }

    private int handleBuffer(byte[] buffer, int index) {
      try {
        return Math.abs(buffer[index]);
      } catch (ArrayIndexOutOfBoundsException e) {
        return 0;
      }
    }


    /**
     * Opens up the given file, writes the header, and keeps filling it with raw PCM bytes from
     * AudioRecord until it reaches 4GB or is stopped by the user. It then goes back and updates
     * the WAV header to include the proper final chunk sizes.
     *
     * @param files Index 0 should be the file to write to
     * @return Either an Exception (error) or two longs, the filesize, elapsed time in ms (success)
     */
    @Override
    protected Object[] doInBackground(File... files) {
      AudioRecord audioRecord = null;
      FileOutputStream wavOut = null;
      long startTime = 0;
      long endTime = 0;

      try {
        // Open our two resources
        audioRecord = new AudioRecord(AUDIO_SOURCE, SAMPLE_RATE, CHANNEL_MASK, ENCODING, BUFFER_SIZE);
        wavOut = new FileOutputStream(files[0]);

        // Write out the wav file header
        writeWavHeader(wavOut, CHANNEL_MASK, SAMPLE_RATE, ENCODING);

        // Avoiding loop allocations
        byte[] buffer = new byte[BUFFER_SIZE];
        boolean run = true;
        int read;
        long total = 0;

        // Let's go
        startTime = SystemClock.elapsedRealtime();

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
          @Override
          public void run() {
            audioRecorderManager.sendMeter(metering, recorderSecondsElapsed);
            recorderSecondsElapsed++;
          }
        }, 0, 1000);

        audioRecord.startRecording();
        while (run && !isCancelled()) {
          read = audioRecord.read(buffer, 0, buffer.length);

          // Log.d("record", recorderSecondsElapsed + "");

          if (read < 0) {
//            audioRecorderManager.sendMeter(0, recorderSecondsElapsed);
            metering = 0f;
          }

          double sum = 0;
          for (int i = 0; i < read; i++) {
            sum += handleBuffer(buffer, i);
          }

          if (read > 0) {
//            audioRecorderManager.sendMeter(sum/read, recorderSecondsElapsed);
            metering = sum/read;
          }

          // WAVs cannot be > 4 GB due to the use of 32 bit unsigned integers.
          if (total + read > 4294967295L) {
            // Write as many bytes as we can before hitting the max size
            for (int i = 0; i < read && total <= 4294967295L; i++, total++) {
              wavOut.write(handleBuffer(buffer, i));
            }
            run = false;
          } else {
            // Write out the entire read buffer
            wavOut.write(buffer, 0, read);
            total += read;
          }
        }
      } catch (IOException ex) {
        return new Object[]{ex};
      } finally {
        if (audioRecord != null) {
          try {
            if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
              audioRecord.stop();
              endTime = SystemClock.elapsedRealtime();
            }
          } catch (IllegalStateException ex) {
            //
          }
          if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
            audioRecord.release();
          }

          stopTimer();

        }
        if (wavOut != null) {
          try {
            wavOut.close();
          } catch (IOException ex) {
            //
          }
        }
      }

      try {
        // This is not put in the try/catch/finally above since it needs to run
        // after we close the FileOutputStream
        updateWavHeader(files[0]);
      } catch (IOException ex) {
        return new Object[] { ex };
      }

      return new Object[] { files[0].length(), endTime - startTime };
    }

    /**
     * Writes the proper 44-byte RIFF/WAVE header to/for the given stream
     * Two size fields are left empty/null since we do not yet know the final stream size
     *
     * @param out         The stream to write the header to
     * @param channelMask An AudioFormat.CHANNEL_* mask
     * @param sampleRate  The sample rate in hertz
     * @param encoding    An AudioFormat.ENCODING_PCM_* value
     * @throws IOException
     */
    private static void writeWavHeader(OutputStream out, int channelMask, int sampleRate, int encoding) throws IOException {
      short channels;
      switch (channelMask) {
        case AudioFormat.CHANNEL_IN_MONO:
          channels = 1;
          break;
        case AudioFormat.CHANNEL_IN_STEREO:
          channels = 2;
          break;
        default:
          throw new IllegalArgumentException("Unacceptable channel mask");
      }

      short bitDepth;
      switch (encoding) {
        case AudioFormat.ENCODING_PCM_8BIT:
          bitDepth = 8;
          break;
        case AudioFormat.ENCODING_PCM_16BIT:
          bitDepth = 16;
          break;
        case AudioFormat.ENCODING_PCM_FLOAT:
          bitDepth = 32;
          break;
        default:
          throw new IllegalArgumentException("Unacceptable encoding");
      }

      writeWavHeader(out, channels, sampleRate, bitDepth);
    }

    /**
     * Writes the proper 44-byte RIFF/WAVE header to/for the given stream
     * Two size fields are left empty/null since we do not yet know the final stream size
     *
     * @param out        The stream to write the header to
     * @param channels   The number of channels
     * @param sampleRate The sample rate in hertz
     * @param bitDepth   The bit depth
     * @throws IOException
     */
    private static void writeWavHeader(OutputStream out, short channels, int sampleRate, short bitDepth) throws IOException {
      // Convert the multi-byte integers to raw bytes in little endian format as required by the spec
      byte[] littleBytes = ByteBuffer
              .allocate(14)
              .order(ByteOrder.LITTLE_ENDIAN)
              .putShort(channels)
              .putInt(sampleRate)
              .putInt(sampleRate * channels * (bitDepth / 8))
              .putShort((short) (channels * (bitDepth / 8)))
              .putShort(bitDepth)
              .array();

      // Not necessarily the best, but it's very easy to visualize this way
      out.write(new byte[]{
              // RIFF header
              'R', 'I', 'F', 'F', // ChunkID
              0, 0, 0, 0, // ChunkSize (must be updated later)
              'W', 'A', 'V', 'E', // Format
              // fmt subchunk
              'f', 'm', 't', ' ', // Subchunk1ID
              16, 0, 0, 0, // Subchunk1Size
              1, 0, // AudioFormat
              littleBytes[0], littleBytes[1], // NumChannels
              littleBytes[2], littleBytes[3], littleBytes[4], littleBytes[5], // SampleRate
              littleBytes[6], littleBytes[7], littleBytes[8], littleBytes[9], // ByteRate
              littleBytes[10], littleBytes[11], // BlockAlign
              littleBytes[12], littleBytes[13], // BitsPerSample
              // data subchunk
              'd', 'a', 't', 'a', // Subchunk2ID
              0, 0, 0, 0, // Subchunk2Size (must be updated later)
      });
    }

    /**
     * Updates the given wav file's header to include the final chunk sizes
     *
     * @param wav The wav file to update
     * @throws IOException
     */
    private static void updateWavHeader(File wav) throws IOException {
      byte[] sizes = ByteBuffer
              .allocate(8)
              .order(ByteOrder.LITTLE_ENDIAN)
              // There are probably a bunch of different/better ways to calculate
              // these two given your circumstances. Cast should be safe since if the WAV is
              // > 4 GB we've already made a terrible mistake.
              .putInt((int) (wav.length() - 8)) // ChunkSize
              .putInt((int) (wav.length() - 44)) // Subchunk2Size
              .array();

      RandomAccessFile accessWave = null;
      //noinspection CaughtExceptionImmediatelyRethrown
      try {
        accessWave = new RandomAccessFile(wav, "rw");
        // ChunkSize
        accessWave.seek(4);
        accessWave.write(sizes, 0, 4);

        // Subchunk2Size
        accessWave.seek(40);
        accessWave.write(sizes, 4, 4);
      } catch (IOException ex) {
        // Rethrow but we still close accessWave in our finally
        throw ex;
      } finally {
        if (accessWave != null) {
          try {
            accessWave.close();
          } catch (IOException ex) {
            //
          }
        }
      }
    }

    @Override
    protected void onCancelled(Object[] results) {
      // Handling cancellations and successful runs in the same way
      onPostExecute(results);
    }

    @Override
    protected void onPostExecute(Object[] results) {
      Throwable throwable = null;
      if (results[0] instanceof Throwable) {
        // Error
        throwable = (Throwable) results[0];
        Log.e(RecordWaveTask.class.getSimpleName(), throwable.getMessage(), throwable);
      }

    }
  }
}


