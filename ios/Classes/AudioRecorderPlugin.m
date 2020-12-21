#import "AudioRecorderPlugin.h"
#import "audio_recorder-Swift.h"

@implementation AudioRecorderPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftAudioRecorderPlugin registerWithRegistrar:registrar];
}
@end
