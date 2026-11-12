#!/usr/bin/env python3
"""
Synthesises the background music into app/src/main/res/raw/ as 16 kHz mono WAV loops (numpy only):

    python tools/generate_music.py

  music_menu.wav       calm minor pad with a few soft bells
  music_city.wav       Ruined City: low, melancholy, with faint rain
  music_swamp.wav      Toxic Swamp: murky, slow, with deep bubbles
  music_desert.wav     Dust Wasteland: Phrygian colour, plucked notes and wind
  music_tundra.wav     Frozen Tundra: airy, glassy bells, cold wind
  music_graveyard.wav  Haunted Graveyard: dark, with a far-off bell toll

All of them are quiet on purpose. They are WAV, not Ogg, because the game plays them from memory with
AudioTrack's loop points, which is the only way to loop with no gap or click. Each loop is built in a
circular buffer (notes wrap round the end, reverb and wind are circular), so its end flows into its start.
The output is deterministic. To use real music instead, drop 16-bit mono WAV loops with the same names into res/raw/.
"""
import os
import wave

import numpy as np

SR = 16000
OUT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res", "raw")


def hz(midi):
    return 440.0 * 2.0 ** ((midi - 69) / 12.0)


class Loop:
    def __init__(self, seconds, seed):
        self.n = int(SR * seconds)
        self.buf = np.zeros(self.n)
        self.rng = np.random.default_rng(seed)

    def add(self, start, sig):
        """Adds [sig] at [start] seconds, wrapping round the end so the loop is seamless."""
        idx = (int(start * SR) + np.arange(len(sig))) % self.n
        np.add.at(self.buf, idx, sig)

    def texture(self, lo, hi, amp, swell_cycles=2):
        """Band-limited noise (wind, rain) whose loudness drifts slowly; circular, so it loops cleanly."""
        spec = np.fft.rfft(self.rng.uniform(-1, 1, self.n))
        f = np.fft.rfftfreq(self.n, 1 / SR)
        spec *= np.clip((f - lo) / (lo * 0.5 + 1), 0, 1) * np.clip((hi - f) / (hi * 0.5 + 1), 0, 1)
        x = np.fft.irfft(spec, self.n)
        x /= np.max(np.abs(x)) or 1.0
        t = np.arange(self.n) / self.n
        drift = 0.6 + 0.4 * np.sin(2 * np.pi * swell_cycles * t + 1.0)
        self.buf += x * drift * amp

    def reverb(self, wet=0.8, seconds=2.6, tau=0.85):
        t = np.arange(int(SR * seconds)) / SR
        ir = self.rng.uniform(-1, 1, len(t)) * np.exp(-t / tau)
        ir = np.convolve(ir, np.ones(20) / 20, mode="same")  # dark, soft tail
        ir /= np.sqrt(np.sum(ir ** 2))
        wetsig = np.fft.irfft(np.fft.rfft(self.buf) * np.fft.rfft(ir, self.n), self.n)
        self.buf = self.buf * (1 - wet * 0.5) + wetsig * wet

    def render(self, peak=0.45):
        x = self.buf - np.mean(self.buf)
        x = np.tanh(x * 0.9)
        x = x / np.max(np.abs(x)) * peak
        k = int(SR * 0.03)  # a 30 ms dip at the loop point: inaudible in a pad, and it rules out any click
        ramp = 0.5 - 0.5 * np.cos(np.pi * np.arange(k) / k)
        x[:k] *= ramp
        x[-k:] *= ramp[::-1]
        return x


# ------------------------------------------------------------------ voices
def pad_note(freq, dur, attack=1.6, release=2.2, detune=0.004, shimmer=0.06):
    total = dur + release
    t = np.arange(int(SR * total)) / SR
    env = np.ones_like(t)
    a = t < attack
    env[a] = 0.5 - 0.5 * np.cos(np.pi * t[a] / attack)
    r = t > dur
    env[r] = 0.5 + 0.5 * np.cos(np.pi * np.clip((t[r] - dur) / release, 0, 1))
    sig = np.zeros_like(t)
    for d, amp in ((1.0, 1.0), (1 + detune, 0.7), (1 - detune, 0.7)):
        for k, h in enumerate((1.0, 0.30, 0.12, 0.05)):
            sig += amp * h * np.sin(2 * np.pi * freq * d * (k + 1) * t)
    sig *= 1 + shimmer * np.sin(2 * np.pi * 0.23 * t)
    return sig * env


def bell(freq, tau=0.9):
    t = np.arange(int(SR * tau * 5)) / SR
    sig = (np.sin(2 * np.pi * freq * t) + 0.35 * np.sin(2 * np.pi * freq * 2 * t)
           + 0.10 * np.sin(2 * np.pi * freq * 2.76 * t) * np.exp(-t / 0.15))
    return sig * np.exp(-t / tau) * np.minimum(t / 0.006, 1.0)


def pluck(freq, tau=0.5):
    t = np.arange(int(SR * tau * 5)) / SR
    sig = np.sin(2 * np.pi * freq * t) + 0.5 * np.sin(2 * np.pi * freq * 2 * t) * np.exp(-t / 0.2) + 0.25 * np.sin(2 * np.pi * freq * 3 * t) * np.exp(-t / 0.1)
    return sig * np.exp(-t / tau) * np.minimum(t / 0.004, 1.0)


def bubble(freq):
    t = np.arange(int(SR * 0.5)) / SR
    return np.sin(2 * np.pi * np.cumsum(freq * (1.6 - 1.2 * np.minimum(t / 0.12, 1.0))) / SR) * np.exp(-t / 0.09) * np.minimum(t / 0.004, 1.0)


def thump(freq=52.0, tau=0.22):
    t = np.arange(int(SR * 1.0)) / SR
    return np.sin(2 * np.pi * np.cumsum(freq * (1 + 1.2 * np.exp(-t / 0.05))) / SR) * np.exp(-t / tau) * np.minimum(t / 0.01, 1.0)


def toll(freq):
    t = np.arange(int(SR * 6.0)) / SR
    sig = sum(a * np.sin(2 * np.pi * freq * m * t) * np.exp(-t / tau)
              for m, a, tau in ((1.0, 1.0, 2.2), (2.0, 0.5, 1.5), (2.4, 0.35, 1.0), (3.0, 0.2, 0.7), (4.1, 0.1, 0.5)))
    return sig * np.minimum(t / 0.01, 1.0)


# ------------------------------------------------------------------ the tracks
def build(seed, bpm, chords, pad=0.15, bass=0.22, attack=1.6, detune=0.004, shimmer=0.06):
    beat = 60.0 / bpm
    chord_len = 8 * beat
    loop = Loop(chord_len * len(chords), seed)
    for i, (root, tones) in enumerate(chords):
        start = i * chord_len
        for semi in tones:
            loop.add(start, pad_note(hz(root + semi), chord_len, attack, detune=detune, shimmer=shimmer) * pad)
        loop.add(start, pad_note(hz(root - 12), chord_len, attack * 0.7, detune=detune) * bass)
    return loop, beat, chord_len


def sprinkle(loop, chords, beat, chord_len, voice, steps, prob, octave, amp):
    for i, (root, tones) in enumerate(chords):
        for s in steps:
            if loop.rng.random() < prob:
                semi = int(loop.rng.choice(tones)) + octave
                loop.add(i * chord_len + s * beat * 0.5, voice(hz(root + semi)) * amp)


def menu():
    chords = [(45, (0, 7, 12, 15)), (41, (0, 7, 12, 16)), (48, (0, 7, 12, 16)), (43, (0, 7, 12, 14))]  # Am F C G
    loop, beat, cl = build(11, 62, chords)
    sprinkle(loop, chords, beat, cl, bell, (0, 3, 5, 6, 9, 12, 14), 0.5, 12, 0.05)
    loop.reverb(0.9, tau=0.9)
    return loop.render()


def city():
    chords = [(38, (0, 7, 12, 15)), (34, (0, 7, 12, 16)), (41, (0, 7, 12, 16)), (36, (0, 7, 12, 16))]  # Dm Bb F C
    loop, beat, cl = build(21, 66, chords, pad=0.14)
    loop.texture(2500, 6500, 0.035, swell_cycles=3)                       # rain
    for i in range(len(chords)):                                          # a very soft low pulse
        for bar in range(2):
            loop.add(i * cl + bar * 4 * beat, thump(50.0) * 0.09)
    sprinkle(loop, chords, beat, cl, lambda f: bell(f, 0.6), (2, 7, 11, 13), 0.5, 12, 0.045)
    loop.reverb(0.8, tau=0.8)
    return loop.render()


def swamp():
    chords = [(40, (0, 7, 12, 15)), (36, (0, 7, 11, 16)), (40, (0, 5, 12, 15)), (35, (0, 7, 12, 14))]  # Em Cmaj7 Esus4 Bsus2
    loop, beat, cl = build(31, 52, chords, pad=0.15, bass=0.26, attack=2.0, detune=0.009, shimmer=0.12)
    for _ in range(14):                                                    # deep bubbles
        loop.add(loop.rng.random() * loop.n / SR, bubble(float(loop.rng.choice((110, 146, 98, 130)))) * 0.06)
    loop.texture(120, 500, 0.05, swell_cycles=2)                          # low, damp air
    loop.reverb(0.9, tau=1.1)
    return loop.render()


def desert():
    chords = [(40, (0, 7, 12, 16)), (41, (0, 7, 12, 16)), (40, (0, 7, 12, 16)), (38, (0, 7, 12, 15))]  # E F E Dm
    loop, beat, cl = build(41, 60, chords, pad=0.13, bass=0.2, attack=1.8)
    scale = (0, 1, 4, 7, 8, 10)                                            # E Phrygian
    for i, (root, _) in enumerate(chords):
        for s in (0, 3, 6, 8, 11, 14):
            if loop.rng.random() < 0.6:
                semi = int(loop.rng.choice(scale)) + 24
                loop.add(i * cl + s * beat * 0.5, pluck(hz(40 + semi), 0.55) * 0.05)
    loop.texture(300, 1100, 0.06, swell_cycles=2)                          # wind
    loop.reverb(0.75, tau=0.9)
    return loop.render()


def tundra():
    chords = [(38, (0, 7, 11, 14)), (35, (0, 7, 10, 15)), (43, (0, 7, 11, 14)), (45, (0, 7, 12, 14))]  # Dmaj7 Bm7 Gmaj7 Aadd9
    loop, beat, cl = build(51, 56, chords, pad=0.12, bass=0.16, attack=2.2, detune=0.003, shimmer=0.04)
    sprinkle(loop, chords, beat, cl, lambda f: bell(f, 1.5), (0, 4, 7, 10, 13), 0.7, 24, 0.045)
    loop.texture(500, 2500, 0.05, swell_cycles=2)                          # cold wind
    loop.reverb(0.95, seconds=3.2, tau=1.3)
    return loop.render()


def graveyard():
    chords = [(36, (0, 7, 12, 15)), (32, (0, 7, 12, 16)), (41, (0, 7, 12, 15)), (31, (0, 7, 12, 15))]  # Cm Ab Fm Gm
    loop, beat, cl = build(61, 50, chords, pad=0.13, bass=0.28, attack=2.0, detune=0.007, shimmer=0.1)
    for i in (0, 2):                                                       # a far-off bell, twice per loop
        loop.add(i * cl + 1.0, toll(hz(60)) * 0.07)
    sprinkle(loop, chords, beat, cl, lambda f: bell(f, 1.2), (4, 10), 0.5, 24, 0.035)
    loop.texture(150, 600, 0.04, swell_cycles=1)
    loop.reverb(0.95, seconds=3.4, tau=1.4)
    return loop.render()


TRACKS = {"menu": menu, "city": city, "swamp": swamp, "desert": desert, "tundra": tundra, "graveyard": graveyard}


def write_wav(name, x):
    path = os.path.join(OUT, f"music_{name}.wav")
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes((np.clip(x, -1, 1) * 32767).astype("<i2").tobytes())
    print(f"music_{name}.wav  {len(x) / SR:5.1f}s  {os.path.getsize(path) / 1024:5.0f} KB")


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    for name, fn in TRACKS.items():
        write_wav(name, fn())
