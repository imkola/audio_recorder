package com.jordanalcaraz.audiorecorder.audiorecorder;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.HashMap;

import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/**
 * AudioRecorderPlugin
 */
public class AudioRecorderPlugin implements MethodCallHandler,
    PluginRegistry.RequestPermissionsResultListener {
  private static final String LOG_TAG = "AudioRecorder";

  // I think request codes need to be unique across all Android plugins
  // in a Flutter app, but I'm not sure of the best way to pick one.
  private static final int PERMISSIONS_REQUEST_CODE = 201;

  private final Registrar registrar;
  private boolean isRecording = false;
  private MediaRecorder mRecorder = null;
  private String mFilePath = null;
  private long startTime;
  private String mExtension = "";
  private Result pendingResult;
  private WavRecorder wavRecorder;

  @Override
  public boolean onRequestPermissionsResult(int requestCode,
      String[] permissions, int[] grantResults) {
    if (requestCode != PERMISSIONS_REQUEST_CODE) {
      return false;
    }

    if (pendingResult != null) {
      boolean hasPermissions = grantResults.length == 1
          && grantResults[0] == PackageManager.PERMISSION_GRANTED;
      pendingResult.success(hasPermissions);
      pendingResult = null;
    }

    return true;
  }

  /**
   * Plugin registration.
   */
  public static void registerWith(Registrar registrar) {
    final MethodChannel channel = new MethodChannel(registrar.messenger(), "audio_recorder");
    channel.setMethodCallHandler(new AudioRecorderPlugin(registrar));
  }

  private AudioRecorderPlugin(Registrar registrar) {
    this.registrar = registrar;
    this.registrar.addRequestPermissionsResultListener(this);
  }

  @Override
  public void onMethodCall(MethodCall call, Result result) {
    switch (call.method) {
      case "start":
        Log.d(LOG_TAG, "Start");
        String path = call.argument("path");
        mExtension = call.argument("extension");
        startTime = SystemClock.elapsedRealtime();
        if (path != null) {
          mFilePath = path;
        } else {
          String fileName = String.valueOf(System.currentTimeMillis());
          mFilePath = registrar.context().getExternalFilesDir(null)
              + "/" + fileName + mExtension;
        }
        Log.d(LOG_TAG, mFilePath);
        startRecording();
        isRecording = true;
        result.success(null);
        break;
      case "stop":
        Log.d(LOG_TAG, "Stop");
        stopRecording();
        long duration = SystemClock.elapsedRealtime() - startTime;
        Log.d(LOG_TAG, "Duration : " + String.valueOf(duration));
        isRecording = false;
        HashMap<String, Object> recordingResult = new HashMap<>();
        recordingResult.put("duration", duration);
        recordingResult.put("path", mFilePath);
        recordingResult.put("audioOutputFormat", mExtension);
        result.success(recordingResult);
        break;
      case "isRecording":
        Log.d(LOG_TAG, "Get isRecording");
        result.success(isRecording);
        break;
      case "hasPermissions":
        Log.d(LOG_TAG, "Get hasPermissions");
        if (hasPermission(Manifest.permission.RECORD_AUDIO)) {
          result.success(hasPermission(Manifest.permission.RECORD_AUDIO));
        } else {
          pendingResult = result;
          ActivityCompat.requestPermissions(
              registrar.activity(),
              new String[] { Manifest.permission.RECORD_AUDIO },
              PERMISSIONS_REQUEST_CODE);
        }
        break;
      default:
        result.notImplemented();
        break;
    }
  }

  private boolean hasPermission(String permission) {
    Context context = registrar.context();
    return ContextCompat.checkSelfPermission(context, permission)
        == PackageManager.PERMISSION_GRANTED;
  }

  private void startRecording() {
    if (isOutputFormatWav()) {
      startWavRecording();
    } else {
      startNormalRecording();
    }
  }

  private void startNormalRecording() {
    mRecorder = new MediaRecorder();
    mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
    mRecorder.setOutputFormat(getOutputFormatFromString(mExtension));
    mRecorder.setOutputFile(mFilePath);
    mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

    try {
      mRecorder.prepare();
    } catch (IOException e) {
      Log.e(LOG_TAG, "prepare() failed");
    }

    mRecorder.start();
  }

  private void startWavRecording() {
    wavRecorder = new WavRecorder(registrar.context(), mFilePath);
    wavRecorder.startRecording();
  }

  private void stopRecording() {
    if (isOutputFormatWav()) {
      stopWavRecording();
    } else {
      stopNormalRecording();
    }
  }

  private void stopNormalRecording() {
    if (mRecorder != null){
      mRecorder.stop();
      mRecorder.reset();
      mRecorder.release();
      mRecorder = null;
    }
  }

  private void stopWavRecording() {
    wavRecorder.stopRecording();
  }

  private int getOutputFormatFromString(String outputFormat) {
    switch (outputFormat) {
      case ".mp4":
      case ".aac":
      case ".m4a":
        return MediaRecorder.OutputFormat.MPEG_4;
      default:
        return MediaRecorder.OutputFormat.MPEG_4;
    }
  }

  private boolean isOutputFormatWav() {
    return mExtension.equals(".wav");
  }
}
