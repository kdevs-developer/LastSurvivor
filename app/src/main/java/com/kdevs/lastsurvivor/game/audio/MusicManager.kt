package com.kdevs.lastsurvivor.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import com.kdevs.lastsurvivor.R
import com.kdevs.lastsurvivor.game.engine.GameArea
import com.kdevs.lastsurvivor.game.engine.GameConfig
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** One looping piece of music per scene: the menus, and each of the five areas. */
enum class MusicTrack {
    MENU, CITY, SWAMP, DESERT, TUNDRA, GRAVEYARD;

    companion object {
        fun forArea(area: GameArea): MusicTrack = when (area) {
            GameArea.CITY -> CITY
            GameArea.SWAMP -> SWAMP
            GameArea.DESERT -> DESERT
            GameArea.TUNDRA -> TUNDRA
            GameArea.GRAVEYARD -> GRAVEYARD
        }
    }
}

/**
 * Background music. Each track is a short 16-bit mono WAV that is loaded into memory and looped by an
 * [AudioTrack] in static mode with loop points over the whole clip. Unlike MediaPlayer, that loops with no
 * gap and no click. Ask for a track with [play]; asking for the one already playing does nothing, so screens
 * can call it freely. Music follows the Sound setting ([setEnabled]) and pauses in the background
 * ([setForeground]). One instance serves the app; all calls are on whatever thread calls them but serialised.
 */
class MusicManager private constructor(private val context: Context) {

    companion object {
        @Volatile private var instance: MusicManager? = null

        fun get(context: Context): MusicManager =
            instance ?: synchronized(this) {
                instance ?: MusicManager(context.applicationContext).also { instance = it }
            }

        private fun rawId(track: MusicTrack): Int = when (track) {
            MusicTrack.MENU -> R.raw.music_menu
            MusicTrack.CITY -> R.raw.music_city
            MusicTrack.SWAMP -> R.raw.music_swamp
            MusicTrack.DESERT -> R.raw.music_desert
            MusicTrack.TUNDRA -> R.raw.music_tundra
            MusicTrack.GRAVEYARD -> R.raw.music_graveyard
        }

        private const val WAV_HEADER_BYTES = 44      // canonical header written by tools/generate_music.py
        private const val FADE_STEP_MS = 60L
        private const val FADE_STEPS = 25            // ~1.5 s fade-in
    }

    private val main = Handler(Looper.getMainLooper())
    private var track: AudioTrack? = null
    private var playing: MusicTrack? = null
    private var wanted: MusicTrack? = null
    private var enabled = true
    private var foreground = true

    @Synchronized fun play(music: MusicTrack) { wanted = music; sync() }

    @Synchronized fun setEnabled(on: Boolean) { enabled = on; sync() }

    @Synchronized fun setForeground(on: Boolean) { foreground = on; sync() }

    private fun sync() {
        try {
            val want = wanted
            if (!enabled || !foreground || want == null) {
                track?.takeIf { it.playState == AudioTrack.PLAYSTATE_PLAYING }?.pause()
                return
            }
            if (playing != want) {
                stop()
                val t = load(want) ?: return
                track = t
                playing = want
                t.setVolume(0f)
                t.play()
                fadeIn(t, 1)
            } else {
                track?.takeIf { it.playState != AudioTrack.PLAYSTATE_PLAYING }?.play()
            }
        } catch (e: RuntimeException) {
            // music is a nicety: never let an audio error crash the game
            stop()
        }
    }

    private fun stop() {
        track?.let { try { it.stop() } catch (_: IllegalStateException) {} ; it.release() }
        track = null
        playing = null
    }

    /** Reads the clip, hands it to a static AudioTrack and sets the loop points over the whole clip. */
    private fun load(music: MusicTrack): AudioTrack? {
        val bytes = context.resources.openRawResource(rawId(music)).use { it.readBytes() }
        if (bytes.size <= WAV_HEADER_BYTES) return null
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val sampleRate = buf.getInt(24)
        val frames = (bytes.size - WAV_HEADER_BYTES) / 2
        val pcm = ShortArray(frames)
        buf.position(WAV_HEADER_BYTES)
        buf.asShortBuffer().get(pcm)

        val t = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(frames * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        if (t.write(pcm, 0, frames) != frames) { t.release(); return null }
        t.setLoopPoints(0, frames, -1)
        return t
    }

    private fun fadeIn(t: AudioTrack, step: Int) {
        main.postDelayed({
            synchronized(this) {
                if (track !== t) return@postDelayed // another track took over meanwhile
                try {
                    t.setVolume(GameConfig.MUSIC_VOLUME * step / FADE_STEPS)
                } catch (_: IllegalStateException) {
                    return@postDelayed
                }
                if (step < FADE_STEPS) fadeIn(t, step + 1)
            }
        }, FADE_STEP_MS)
    }
}
