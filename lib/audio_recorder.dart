import 'dart:async';
import 'dart:io';

import 'package:file/local.dart';
import 'package:flutter/services.dart';
import 'package:path/path.dart' as p;

class AudioRecorder {
  static const MethodChannel _channel = const MethodChannel('audio_recorder');

  /// use [LocalFileSystem] to permit widget testing
  static LocalFileSystem fs = LocalFileSystem();

  static Future start(
      {String path, AudioOutputFormat audioOutputFormat}) async {
    String extension;
    if (path != null) {
      if (audioOutputFormat != null) {
        if (_convertStringInAudioOutputFormat(p.extension(path)) !=
            audioOutputFormat) {
          extension = _convertAudioOutputFormatInString(audioOutputFormat);
          path += extension;
        } else {
          extension = p.extension(path);
        }
      } else {
        if (_isAudioOutputFormat(p.extension(path))) {
          extension = p.extension(path);
        } else {
          extension = ".m4a"; // default value
          path += extension;
        }
      }
      File file = fs.file(path);
      if (await file.exists()) {
        throw new Exception("A file already exists at the path :" + path);
      } else if (!await file.parent.exists()) {
        throw new Exception("The specified parent directory does not exist");
      }
    } else {
      extension = ".m4a"; // default value
    }
    return _channel
        .invokeMethod('start', {"path": path, "extension": extension});
  }

  static Future<Recording> stop() async {
    Map<String, Object> response =
        Map.from(await _channel.invokeMethod('stop'));
    Recording recording = new Recording(
        duration: new Duration(milliseconds: response['duration']),
        path: response['path'],
        audioOutputFormat:
            _convertStringInAudioOutputFormat(response['audioOutputFormat']),
        extension: response['audioOutputFormat']);
    return recording;
  }

  static Future<bool> get isRecording async {
    bool isRecording = await _channel.invokeMethod('isRecording');
    return isRecording;
  }

  /// The current recording status.
  static Future<RecordingStatus> get recordingStatus async {
    Map<String, dynamic> result =
        await _channel.invokeMapMethod('recordingStatus');
    return RecordingStatus(
      isRecording: result['isRecording'],
      duration: Duration(milliseconds: result['duration']),
    );
  }

  static Future<bool> get hasPermissions async {
    bool hasPermission = await _channel.invokeMethod('hasPermissions');
    return hasPermission;
  }

  static AudioOutputFormat _convertStringInAudioOutputFormat(String extension) {
    switch (extension) {
      case ".wav":
        return AudioOutputFormat.WAV;
      case ".mp4":
      case ".aac":
      case ".m4a":
        return AudioOutputFormat.AAC;
      default:
        return null;
    }
  }

  static bool _isAudioOutputFormat(String extension) {
    switch (extension) {
      case ".wav":
      case ".mp4":
      case ".aac":
      case ".m4a":
        return true;
      default:
        return false;
    }
  }

  static String _convertAudioOutputFormatInString(
      AudioOutputFormat outputFormat) {
    switch (outputFormat) {
      case AudioOutputFormat.WAV:
        return ".wav";
      case AudioOutputFormat.AAC:
        return ".m4a";
      default:
        return ".m4a";
    }
  }
}

enum AudioOutputFormat { AAC, WAV }

class Recording {
  // File path
  String path;
  // File extension
  String extension;
  // Audio duration in milliseconds
  Duration duration;
  // Audio output format
  AudioOutputFormat audioOutputFormat;

  Recording({this.duration, this.path, this.audioOutputFormat, this.extension});
}

/// Represents the current recording status.
class RecordingStatus {
  bool isRecording;

  /// The duration of the current recording, if actively recording.
  ///
  /// This value is currently more accurate on iOS than on Android, because on
  /// Android there doesn't appear to be a way to query the native recorder
  /// directly for this value. Instead, we take a timestamp when starting the
  /// recording, so if there is any lag before the recording actually starts,
  /// the reported duration will be longer than the actual audio file.
  Duration duration;

  RecordingStatus({this.isRecording, this.duration});
}
