package framework.telegram.message.audio

import android.content.Context
import android.media.AudioManager

object AudioUtil {

    fun requestFocus(context: Context) {
        //        Intent i = new Intent("com.android.music.musicservicecommand");
        //        i.putExtra("command", "pause");
        //        sendBroadcast(i);

        (context.getSystemService(Context.AUDIO_SERVICE) as AudioManager).requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        //        ((AudioManager) context.getSystemService(Context.AUDIO_SERVICE)).requestAudioFocus(null, AudioManager.STREAM_SYSTEM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        //        ((AudioManager) context.getSystemService(Context.AUDIO_SERVICE)).requestAudioFocus(null, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        //        ((AudioManager) context.getSystemService(Context.AUDIO_SERVICE)).requestAudioFocus(null, AudioManager.STREAM_RING, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        //        ((AudioManager) context.getSystemService(Context.AUDIO_SERVICE)).requestAudioFocus(null, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        //        ((AudioManager) context.getSystemService(Context.AUDIO_SERVICE)).requestAudioFocus(null, AudioManager.STREAM_NOTIFICATION, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        //        ((AudioManager) context.getSystemService(Context.AUDIO_SERVICE)).requestAudioFocus(null, AudioManager.STREAM_DTMF, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
    }

    fun abandonAudioFocus(context: Context) {
        (context.getSystemService(Context.AUDIO_SERVICE) as AudioManager).abandonAudioFocus(null)
    }
}
