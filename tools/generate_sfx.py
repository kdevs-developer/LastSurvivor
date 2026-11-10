#!/usr/bin/env python3
"""
Synthesises every Last Survivor sound effect into app/src/main/res/raw/ as 22.05 kHz mono WAV.

    python tools/generate_sfx.py

Needs numpy only. The output is deterministic (fixed seeds), so re-running it gives the same
files. To use real recordings instead, drop files with the same names (sfx_<effect>.wav or .ogg)
into res/raw/ and delete the matching generator below. Each name maps to a SoundEffect constant
in game/audio/SoundEffect.kt (sfx_shot_pistol <-> SHOT_PISTOL).
"""
import os
import wave

import numpy as np

SR = 22050
OUT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res", "raw")
rng = np.random.default_rng(1234)


# ------------------------------------------------------------------ building blocks
def t_axis(seconds):
    return np.arange(int(SR * seconds)) / SR


def noise(seconds):
    return rng.uniform(-1.0, 1.0, int(SR * seconds))


def decay(seconds, tau, attack=0.002):
    t = t_axis(seconds)
    env = np.exp(-t / tau)
    a = max(1, int(SR * attack))
    env[:a] *= np.linspace(0.0, 1.0, a)
    return env


def sweep(seconds, f0, f1, curve=1.0):
    """Sine whose frequency glides from f0 to f1 (curve > 1 = fast at first)."""
    t = t_axis(seconds)
    x = (t / seconds) ** (1.0 / curve)
    freq = f0 + (f1 - f0) * x
    return np.sin(2 * np.pi * np.cumsum(freq) / SR)


def saw(freq_curve):
    phase = np.cumsum(freq_curve) / SR
    return 2.0 * (phase - np.floor(phase + 0.5))


def lowpass(x, cutoff):
    """One-pole low-pass; cutoff may be a scalar or an array (Hz)."""
    cutoff = np.broadcast_to(np.asarray(cutoff, dtype=float), x.shape)
    a = 1.0 - np.exp(-2 * np.pi * cutoff / SR)
    y = np.empty_like(x)
    acc = 0.0
    for i in range(len(x)):
        acc += a[i] * (x[i] - acc)
        y[i] = acc
    return y


def highpass(x, cutoff):
    return x - lowpass(x, cutoff)


def bandpass(x, lo, hi):
    return lowpass(highpass(x, lo), hi)


def mix(length, *parts):
    """Sum (offset_seconds, signal) pairs into one buffer of [length] seconds."""
    out = np.zeros(int(SR * length))
    for offset, sig in parts:
        start = int(SR * offset)
        end = min(len(out), start + len(sig))
        if start < len(out):
            out[start:end] += sig[: end - start]
    return out


def stack(*sigs):
    """Sum signals of different lengths (shorter ones are zero-padded)."""
    out = np.zeros(max(len(x) for x in sigs))
    for x in sigs:
        out[: len(x)] += x
    return out


def click(seconds=0.03, lo=1500, hi=6000, tau=0.006):
    return bandpass(noise(seconds), lo, hi) * decay(seconds, tau, 0.0005)


def tone(seconds, freq, tau, harmonics=(1.0,)):
    t = t_axis(seconds)
    sig = sum(a * np.sin(2 * np.pi * freq * (i + 1) * t) for i, a in enumerate(harmonics))
    return sig * decay(seconds, tau)


def finish(x, peak=0.92, fade=0.01):
    x = np.tanh(x * 1.2) if np.max(np.abs(x)) > 1.0 else x
    m = np.max(np.abs(x)) or 1.0
    x = x / m * peak
    n = max(1, int(SR * fade))
    x[-n:] *= np.linspace(1.0, 0.0, n)  # no click at the end of the sample
    return x


# ------------------------------------------------------------------ the sounds
def shot_pistol():
    d = 0.28
    crack = highpass(noise(d), 700) * decay(d, 0.035)
    thump = sweep(d, 190, 55, 2.0) * decay(d, 0.07)
    tail = lowpass(noise(d), 1500) * decay(d, 0.09) * 0.25
    return crack * 0.8 + thump * 0.9 + tail


def shot_smg():
    d = 0.16
    crack = highpass(noise(d), 1000) * decay(d, 0.022)
    thump = sweep(d, 240, 90, 2.0) * decay(d, 0.04)
    return crack * 0.9 + thump * 0.7


def shot_shotgun():
    d = 0.7
    blast = lowpass(noise(d), 3500) * decay(d, 0.09)
    boom = sweep(d, 130, 38, 2.0) * decay(d, 0.18)
    crack = highpass(noise(d), 900) * decay(d, 0.03)
    tail = lowpass(noise(d), 900) * decay(d, 0.25) * 0.35
    return blast * 0.8 + boom * 1.1 + crack * 0.6 + tail


def shot_rifle():
    d = 0.6
    crack = highpass(noise(d), 1300) * decay(d, 0.04)
    thump = sweep(d, 160, 45, 2.0) * decay(d, 0.12)
    echo = lowpass(noise(d), 1800) * decay(d, 0.16) * 0.3
    echo = np.concatenate([np.zeros(int(SR * 0.09)), echo])[: len(crack)]
    return crack * 0.9 + thump * 0.9 + echo


def reload_start():
    d = 0.35
    return mix(d,
               (0.0, stack(click(0.05, 1200, 5000, 0.012) * 0.9, tone(0.05, 900, 0.02) * 0.4)),
               (0.13, click(0.04, 2000, 7000, 0.008) * 0.7),
               (0.17, tone(0.15, 420, 0.05, (1.0, 0.4)) * 0.3))


def reload_done():
    d = 0.35
    return mix(d,
               (0.0, stack(click(0.05, 800, 3500, 0.012) * 0.8, tone(0.1, 250, 0.04) * 0.5)),
               (0.1, stack(click(0.06, 1500, 6000, 0.01) * 1.0, tone(0.12, 160, 0.05, (1.0, 0.5)) * 0.7)))


def hit_flesh():
    d = 0.16
    thud = sweep(d, 150, 70, 2.0) * decay(d, 0.045)
    wet = lowpass(noise(d), 700) * decay(d, 0.04)
    return thud * 0.8 + wet * 0.8


def zombie_death():
    d = 0.9
    t = t_axis(d)
    f = 150 - 90 * (t / d) ** 0.7
    f = f * (1 + 0.05 * np.sin(2 * np.pi * 6 * t))
    voice = lowpass(saw(f), 900) * np.sin(np.pi * np.minimum(t / 0.12, 1.0) * 0.5) ** 2
    voice *= np.exp(-t / 0.45)
    breath = lowpass(noise(d), 1500) * decay(d, 0.3) * 0.35
    thud = mix(d, (0.55, sweep(0.25, 90, 40, 2.0) * decay(0.25, 0.08) * 0.8))
    return voice + breath + thud


def _groan(d, f0, f1, vib, cutoff):
    t = t_axis(d)
    f = f0 + (f1 - f0) * np.sin(np.pi * t / d)
    f = f * (1 + 0.04 * np.sin(2 * np.pi * vib * t))
    env = np.sin(np.pi * np.clip(t / d, 0, 1)) ** 1.5
    voice = lowpass(saw(f), cutoff) * env
    rasp = lowpass(noise(d), 1200) * env * 0.25
    return voice + rasp


def zombie_groan_1():
    return _groan(1.1, 85, 115, 5.0, 650)


def zombie_groan_2():
    return _groan(1.4, 100, 72, 4.0, 800)


def zombie_swing():
    d = 0.32
    t = t_axis(d)
    air = bandpass(noise(d), 500, 3000) * np.sin(np.pi * np.clip(t / d, 0, 1)) ** 2
    scratch = highpass(noise(d), 2500) * decay(d, 0.05) * 0.3
    return air * 0.8 + scratch


def player_hurt():
    d = 0.35
    thump = sweep(d, 230, 100, 1.5) * decay(d, 0.08)
    body = lowpass(noise(d), 1100) * decay(d, 0.07)
    grunt = lowpass(saw(180 - 60 * t_axis(d) / d), 700) * decay(d, 0.09) * 0.5
    return thump + body * 0.6 + grunt


def poisoned():
    d = 0.6
    hiss = highpass(noise(d), 3000) * decay(d, 0.25) * 0.5
    out = hiss
    for off in (0.04, 0.17, 0.29, 0.43):
        f0 = rng.uniform(300, 500)
        blip = sweep(0.09, f0, f0 * 2.2, 1.0) * decay(0.09, 0.03)
        out = out + mix(d, (off, blip * 0.6))
    return out


def shield_block():
    d = 0.35
    zap = sweep(d, 1800, 700, 3.0) * decay(d, 0.06)
    ring = tone(d, 1320, 0.1) * 0.5 + tone(d, 1980, 0.07) * 0.3
    return stack(zap * 0.6, ring, click(0.03, 3000, 9000, 0.01) * 0.5)


def heartbeat():
    d = 0.5
    def beat(amp):
        return lowpass(sweep(0.18, 70, 42, 1.5) * decay(0.18, 0.06), 160) * amp
    return mix(d, (0.0, beat(1.0)), (0.2, beat(0.7)))


def pickup_powerup():
    d = 0.4
    notes = [(0.0, 660), (0.08, 880), (0.16, 1175)]
    parts = [(o, tone(0.2, f, 0.07, (1.0, 0.3, 0.1))) for o, f in notes]
    return mix(d, *parts)


def pickup_heal():
    d = 0.7
    return mix(d, (0.0, tone(d, 880, 0.18, (1.0, 0.25))), (0.07, tone(0.6, 1320, 0.16, (1.0, 0.2))),
               (0.14, tone(0.55, 1760, 0.12)))


def pickup_grenade():
    d = 0.3
    return mix(d, (0.0, stack(click(0.06, 600, 4000, 0.015) * 0.9, tone(0.2, 380, 0.05, (1.0, 0.7, 0.4)) * 0.6)),
               (0.09, click(0.05, 1000, 5000, 0.01) * 0.6))


def grenade_throw():
    d = 0.35
    t = t_axis(d)
    air = bandpass(noise(d), 400, 2500) * np.sin(np.pi * np.clip(t / d, 0, 1)) ** 2 * 0.6
    return mix(d, (0.0, click(0.04, 1000, 4000, 0.01) * 0.8), (0.03, air))


def explosion():
    d = 1.5
    t = t_axis(d)
    rumble = lowpass(noise(d), 160 + 2400 * np.exp(-t / 0.18)) * decay(d, 0.5)
    boom = sweep(d, 80, 24, 2.0) * decay(d, 0.45)
    crack = highpass(noise(d), 800) * decay(d, 0.04) * 0.7
    debris = highpass(noise(d), 2500) * decay(d, 0.3) * 0.15
    debris = np.concatenate([np.zeros(int(SR * 0.1)), debris])[: len(t)]
    return rumble * 1.2 + boom * 1.2 + crack + debris


def _roar(d, f0, f1, drive, trem):
    t = t_axis(d)
    f = f0 + (f1 - f0) * np.sin(np.pi * np.clip(t / d, 0, 1) * 0.9)
    f = f * (1 + 0.03 * np.sin(2 * np.pi * 7 * t))
    voice = saw(f) + 0.5 * saw(f * 1.01)
    voice = np.tanh(voice * drive)
    voice = lowpass(voice, 1100)
    amp = np.sin(np.pi * np.clip(t / d, 0, 1)) ** 0.8
    amp *= 1 - trem + trem * (0.5 + 0.5 * np.sin(2 * np.pi * 26 * t))
    growl = lowpass(noise(d), 900) * amp * 0.3
    return voice * amp + growl


def boss_roar():
    return _roar(1.7, 58, 80, 2.5, 0.35)


def boss_windup():
    d = 0.8
    t = t_axis(d)
    f = 70 + 90 * (t / d) ** 1.5
    voice = lowpass(np.tanh(saw(f) * 2.0), 900) * (0.3 + 0.7 * t / d)
    return voice * np.minimum(t / 0.05, 1.0)


def boss_charge():
    d = 0.75
    t = t_axis(d)
    air = bandpass(noise(d), 300, 2200) * np.sin(np.pi * np.clip(t / d, 0, 1)) ** 1.5
    parts = [(0.0, air * 0.7)]
    for off in (0.05, 0.2, 0.34, 0.47, 0.58):
        parts.append((off, sweep(0.14, 85, 35, 2.0) * decay(0.14, 0.05) * 0.9))
    return mix(d, *parts)


def boss_summon():
    d = 1.1
    t = t_axis(d)
    base = 190 + 60 * (t / d)
    vib = 1 + 0.03 * np.sin(2 * np.pi * 6 * t)
    a = np.sin(2 * np.pi * np.cumsum(base * vib) / SR)
    b = np.sin(2 * np.pi * np.cumsum(base * vib * 1.034) / SR)
    env = np.sin(np.pi * np.clip(t / d, 0, 1)) ** 1.2
    breath = highpass(noise(d), 1800) * env * 0.12
    return (a + b) * 0.5 * env + breath


def boss_death():
    d = 2.2
    t = t_axis(d)
    roar = _roar(d, 90, 28, 2.8, 0.4)
    boom = mix(d, (0.5, sweep(1.6, 70, 22, 2.0) * decay(1.6, 0.5)))
    rumble = mix(d, (0.5, lowpass(noise(1.6), 200 + 1800 * np.exp(-t[:int(SR * 1.6)] / 0.2)) * decay(1.6, 0.5)))
    return roar * 0.9 + boom * 1.1 + rumble


def wave_start():
    d = 1.1
    parts = []
    for off, f in ((0.0, 196), (0.32, 196), (0.64, 294)):
        n = int(SR * 0.4)
        tt = np.arange(n) / SR
        horn = lowpass(saw(np.full(n, f) * (1 + 0.004 * np.sin(2 * np.pi * 5 * tt))), 1400)
        horn += 0.5 * lowpass(saw(np.full(n, f * 1.5)), 1400)
        env = np.minimum(tt / 0.04, 1.0) * np.exp(-tt / 0.28)
        parts.append((off, horn * env))
    return mix(d, *parts)


def wave_clear():
    d = 1.0
    parts = []
    for off, f in ((0.0, 523.25), (0.1, 659.25), (0.2, 783.99), (0.32, 1046.5)):
        parts.append((off, tone(0.6, f, 0.16, (1.0, 0.35, 0.12)) * (0.8 if off < 0.3 else 1.0)))
    return mix(d, *parts)


def game_over():
    d = 2.0
    t = t_axis(d)
    f = 220 * np.exp(-t / 1.2)
    a = lowpass(saw(f), 900)
    b = lowpass(saw(f * 1.5 * (1 + 0.01 * np.sin(2 * np.pi * 4 * t))), 700) * 0.6
    env = np.minimum(t / 0.05, 1.0) * np.exp(-t / 0.9)
    boom = mix(d, (0.0, sweep(1.0, 90, 30, 2.0) * decay(1.0, 0.4) * 0.9))
    return (a + b) * env + boom


def ui_click():
    d = 0.06
    return tone(d, 1500, 0.012) * 0.6 + click(d, 2500, 8000, 0.006) * 0.4


def ui_purchase():
    d = 0.5
    return mix(d, (0.0, tone(0.4, 1318.5, 0.07, (1.0, 0.3))), (0.07, tone(0.45, 1760, 0.12, (1.0, 0.3))))


def ui_denied():
    d = 0.28
    t = t_axis(d)
    sq = np.sign(np.sin(2 * np.pi * 110 * t))
    return lowpass(sq, 900) * np.minimum(t / 0.01, 1.0) * np.exp(-t / 0.12)


SOUNDS = {
    "shot_pistol": shot_pistol, "shot_smg": shot_smg, "shot_shotgun": shot_shotgun, "shot_rifle": shot_rifle,
    "reload_start": reload_start, "reload_done": reload_done,
    "hit_flesh": hit_flesh, "zombie_death": zombie_death,
    "zombie_groan_1": zombie_groan_1, "zombie_groan_2": zombie_groan_2, "zombie_swing": zombie_swing,
    "player_hurt": player_hurt, "poisoned": poisoned, "shield_block": shield_block, "heartbeat": heartbeat,
    "pickup_powerup": pickup_powerup, "pickup_heal": pickup_heal, "pickup_grenade": pickup_grenade,
    "grenade_throw": grenade_throw, "explosion": explosion,
    "boss_roar": boss_roar, "boss_windup": boss_windup, "boss_charge": boss_charge,
    "boss_summon": boss_summon, "boss_death": boss_death,
    "wave_start": wave_start, "wave_clear": wave_clear, "game_over": game_over,
    "ui_click": ui_click, "ui_purchase": ui_purchase, "ui_denied": ui_denied,
}


def write_wav(path, x):
    pcm = np.clip(x, -1.0, 1.0)
    pcm = (pcm * 32767).astype("<i2")
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(pcm.tobytes())


def main():
    os.makedirs(OUT, exist_ok=True)
    total = 0
    for name, fn in SOUNDS.items():
        sig = finish(fn())
        path = os.path.join(OUT, f"sfx_{name}.wav")
        write_wav(path, sig)
        size = os.path.getsize(path)
        total += size
        print(f"sfx_{name}.wav  {len(sig) / SR:4.2f}s  {size / 1024:5.1f} KB")
    print(f"{len(SOUNDS)} files, {total / 1024:.0f} KB total")


if __name__ == "__main__":
    main()
