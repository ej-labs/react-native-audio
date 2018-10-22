import React from 'react'; //eslint-disable-line
import {
  Platform,
  NativeModules,
  PermissionsAndroid,
  // DeviceEventEmitter,
  NativeAppEventEmitter,
} from 'react-native';

const AudioRecorderManager = NativeModules.AudioRecorderManager;


export class AudioRecorder {

  progressSubscription = null;
  finishedSubscription = null;

  static prepareRecordingAtPath(path, options) {
    if (this.progressSubscription) this.progressSubscription.remove();
    this.progressSubscription = NativeAppEventEmitter.addListener('recordingProgress',
      (data) => {
        if (this.onProgress) {
          this.onProgress(data);
        }
      }
    );

    if (this.finishedSubscription) this.finishedSubscription.remove();
    this.finishedSubscription = NativeAppEventEmitter.addListener('recordingFinished',
      (data) => {
        if (this.onFinished) {
          this.onFinished(data);
        }
      }
    );

    const defaultOptions = {
      SampleRate: 44100.0,
      Channels: 2,
      AudioQuality: 'High',
      AudioEncoding: 'ima4',
      OutputFormat: 'mpeg_4',
      MeteringEnabled: false,
      MeasurementMode: false,
      AudioEncodingBitRate: 32000,
      IncludeBase64: false,
    };

    const recordingOptions = { ...defaultOptions, ...options };

    if (Platform.OS === 'ios') {
      AudioRecorderManager.prepareRecordingAtPath(
        path,
        recordingOptions.SampleRate,
        recordingOptions.Channels,
        recordingOptions.AudioQuality,
        recordingOptions.AudioEncoding,
        recordingOptions.MeteringEnabled,
        recordingOptions.MeasurementMode,
        recordingOptions.IncludeBase64
      );
    } else {
      return AudioRecorderManager.prepareRecordingAtPath(path, recordingOptions);
    }
  }

  static startRecording() {
    return AudioRecorderManager.startRecording();
  }

  static pauseRecording() {
    return AudioRecorderManager.pauseRecording();
  }

  static resumeRecording() {
    return AudioRecorderManager.resumeRecording();
  }

  static stopRecording() {
    return AudioRecorderManager.stopRecording();
  }

  static checkAuthorizationStatus() {
    return AudioRecorderManager.checkAuthorizationStatus();
  }

  static requestAuthorization() {
    if (Platform.OS === 'ios') { return AudioRecorderManager.requestAuthorization(); }
    return new Promise((resolve, reject) => {
      PermissionsAndroid.request(
        PermissionsAndroid.PERMISSIONS.RECORD_AUDIO
      ).then(result => {
        if (result === PermissionsAndroid.RESULTS.GRANTED || result === true) { resolve(true); } else { resolve(false); }
      });
    });
  }

  static removeListeners() {
    if (this.progressSubscription) this.progressSubscription.remove();
    if (this.finishedSubscription) this.finishedSubscription.remove();
  }

}

interface AudioUtilsParams {
  MainBundlePath: string,
  CachesDirectoryPath: string,
  DocumentDirectoryPath: string,
  LibraryDirectoryPath: string,
  PicturesDirectoryPath?: string,
  MusicDirectoryPath?: string,
  DownloadsDirectoryPath?: string,
}

let _AudioUtils: AudioUtilsParams = {};

if (Platform.OS === 'ios') {
  _AudioUtils = {
    MainBundlePath: AudioRecorderManager.MainBundlePath,
    CachesDirectoryPath: AudioRecorderManager.NSCachesDirectoryPath,
    DocumentDirectoryPath: AudioRecorderManager.NSDocumentDirectoryPath,
    LibraryDirectoryPath: AudioRecorderManager.NSLibraryDirectoryPath,
  };
} else if (Platform.OS === 'android') {
  _AudioUtils = {
    MainBundlePath: AudioRecorderManager.MainBundlePath,
    CachesDirectoryPath: AudioRecorderManager.CachesDirectoryPath,
    DocumentDirectoryPath: AudioRecorderManager.DocumentDirectoryPath,
    LibraryDirectoryPath: AudioRecorderManager.LibraryDirectoryPath,
    PicturesDirectoryPath: AudioRecorderManager.PicturesDirectoryPath,
    MusicDirectoryPath: AudioRecorderManager.MusicDirectoryPath,
    DownloadsDirectoryPath: AudioRecorderManager.DownloadsDirectoryPath,
  };
}

export const AudioUtils = _AudioUtils;
