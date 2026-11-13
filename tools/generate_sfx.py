#!/usr/bin/env python3
"""
Synthesises every Last Survivor sound effect into app/src/main/res/raw/ as 44.1 kHz mono WAV.

    python tools/generate_sfx.py                 # everything
    python tools/generate_sfx.py reload_pistol_a # just these

Needs numpy; scipy is optional (it only makes the filters much faster). The output is deterministic
(fixed seeds), so re-running it gives the same files. To use real recordings instead, drop files with the
same names (sfx_<effect>.wav or .ogg) into res/raw/ and delete the matching generator below. Each name maps
to a SoundEffect constant in game/audio/SoundEffect.kt (sfx_shot_pistol <-> SHOT_PISTOL).

How the sounds are built (this is what makes them sound "full" rather than like beeps):
  * guns are layers: a 1 ms snap, a bright crack, a mid-range blast, a sub-bass thump, soft saturation,
    then a room tail (reverb) and, where it fits, a shell casing dropping on the floor;
  * reloads are three short sounds per weapon type (A at the start, B in the middle, C at the end), made
    of real parts: plastic clicks, metal ticks, sliding scrapes, solid clacks. C is built so its final
    "chunk" lands exactly LEAD seconds after it starts; the game starts C that long before the reload
    finishes (GameConfig.RELOAD_FINISH_LEAD), so the weapon is ready on the very beat of the sound;
  * voices (zombies, bosses) use formant filters so they have a vowel, not just a buzz;
  * bells, chimes and UI sounds use inharmonic partials and a gentle room tail.
"""
import os
import sys
import wave

import numpy as np

try:
    from scipy import signal as _sig
except ImportError:  # slower pure-python filters are used instead
    _sig = None

SR = 44100
LEAD = 0.18  # seconds from the start of a reload "C" sound to its final chunk (see the module docstring)
OUT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res", "raw")
rng = np.random.default_rng(1234)
_rr = np.random.default_rng(777)  # the reload parts use their own stream


# ------------------------------------------------------------------ building blocks
def t_axis(seconds):
    return np.arange(int(SR * seconds)) / SR


def noise(seconds):
    return rng.uniform(-1.0, 1.0, int(SR * seconds))


def _rnoise(seconds):
    return _rr.uniform(-1.0, 1.0, int(SR * seconds))


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


def _clamp(f):
    return min(float(f), SR * 0.45)


def _biquad_loop(x, b, a):
    y = np.zeros_like(x)
    x1 = x2 = y1 = y2 = 0.0
    for i in range(len(x)):
        v = b[0] * x[i] + b[1] * x1 + b[2] * x2 - a[1] * y1 - a[2] * y2
        x2, x1 = x1, x[i]
        y2, y1 = y1, v
        y[i] = v
    return y


def _rbj(kind, f):
    w = 2 * np.pi * _clamp(f) / SR
    alpha = np.sin(w) / (2 * 0.7071)
    cw = np.cos(w)
    if kind == "low":
        b = np.array([(1 - cw) / 2, 1 - cw, (1 - cw) / 2])
    else:
        b = np.array([(1 + cw) / 2, -(1 + cw), (1 + cw) / 2])
    a = np.array([1 + alpha, -2 * cw, 1 - alpha])
    return b / a[0], a / a[0]


def lowpass(x, cutoff):
    """Low-pass; cutoff is a number (2-pole) or an array in Hz (gliding one-pole, slower)."""
    if np.ndim(cutoff) == 0:
        if _sig is not None:
            return _sig.sosfilt(_sig.butter(2, _clamp(cutoff), "low", fs=SR, output="sos"), x)
        b, a = _rbj("low", cutoff)
        return _biquad_loop(x, b, a)
    cutoff = np.broadcast_to(np.asarray(cutoff, dtype=float), x.shape)
    k = 1.0 - np.exp(-2 * np.pi * np.minimum(cutoff, SR * 0.45) / SR)
    y = np.empty_like(x)
    acc = 0.0
    for i in range(len(x)):
        acc += k[i] * (x[i] - acc)
        y[i] = acc
    return y


def highpass(x, cutoff):
    if _sig is not None:
        return _sig.sosfilt(_sig.butter(2, _clamp(cutoff), "high", fs=SR, output="sos"), x)
    b, a = _rbj("high", cutoff)
    return _biquad_loop(x, b, a)


def bandpass(x, lo, hi):
    if _sig is not None:
        return _sig.sosfilt(_sig.butter(2, [_clamp(lo), _clamp(hi)], "band", fs=SR, output="sos"), x)
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


def reverb(x, rt=0.4, wet=0.25, damp=4000, predelay=0.010, seed=99):
    """A small room: the sound convolved with a dark, decaying noise burst, added under the dry sound."""
    r = np.random.default_rng(seed)
    n = int(SR * rt)
    t = np.arange(n) / SR
    ir = r.uniform(-1, 1, n) * np.exp(-6.9 * t / rt)
    ir = lowpass(ir, damp)
    ir[: int(SR * predelay)] = 0.0
    size = len(x) + n
    tail = np.fft.irfft(np.fft.rfft(x, size) * np.fft.rfft(ir, size), size)
    dry = np.zeros(size)
    dry[: len(x)] = x
    e_dry = np.sum(x ** 2) + 1e-12
    e_tail = np.sum(tail ** 2) + 1e-12
    return dry + tail * wet * np.sqrt(e_dry / e_tail)


def _metal(seconds, freqs, tau, amp=1.0):
    """A struck metal part: a few damped, slightly inharmonic partials (the 'tink' of steel on steel)."""
    t = t_axis(seconds)
    out = np.zeros_like(t)
    for i, f in enumerate(freqs):
        out += np.sin(2 * np.pi * f * t + i) * np.exp(-t / (tau / (1 + 0.6 * i))) / (1 + i)
    out *= np.minimum(t / 0.0004, 1.0)
    return out * amp


def _tick(seconds=0.02, lo=2500, hi=9000, tau=0.003):
    return bandpass(_rnoise(seconds), lo, hi) * decay(seconds, tau, 0.0003)


def _thud(seconds, freq, tau, amp=1.0):
    """Muffled knock: a falling low sine plus a soft noise burst (plastic / hand on metal)."""
    t = t_axis(seconds)
    body = np.sin(2 * np.pi * np.cumsum(freq * (1.0 - 0.35 * t / seconds)) / SR) * np.exp(-t / tau)
    soft = lowpass(_rnoise(seconds), 900) * np.exp(-t / (tau * 0.5)) * 0.8
    return (body + soft) * np.minimum(t / 0.0008, 1.0) * amp


def _scrape(seconds, f0, f1, amp=1.0, grit=60.0, bright=1500):
    """Metal or plastic sliding: band-limited noise whose pitch glides, with a fast rough flutter."""
    t = t_axis(seconds)
    n = _rnoise(seconds)
    centre = np.linspace(f0, f1, len(t))
    carrier = np.sin(2 * np.pi * np.cumsum(centre) / SR)
    sig = highpass(n, bright) * (0.6 + 0.4 * carrier)
    flutter = 0.65 + 0.35 * np.sign(np.sin(2 * np.pi * grit * t + 6 * np.sin(2 * np.pi * 7 * t)))
    env = np.sin(np.pi * np.clip(t / seconds, 0, 1)) ** 0.6
    return sig * flutter * env * amp


def _clack(amp=1.0, body=170, ring=(2600, 4100, 6200), ring_tau=0.025):
    """A solid part seating: low knock + steel ring + a bright tick."""
    return stack(_thud(0.07, body, 0.02, 1.0 * amp),
                 _metal(0.14, ring, ring_tau, 0.5 * amp),
                 _tick(0.02, 3000, 9000, 0.003) * 0.5 * amp)


def _plastic_click(amp=1.0):
    return stack(bandpass(_rnoise(0.025), 1800, 6500) * decay(0.025, 0.0025, 0.0002) * 0.9 * amp,
                 _thud(0.04, 520, 0.008, 0.45 * amp))


def _casing(amp=1.0, f=(4300, 6100, 8200)):
    """A brass case hitting the floor: a ring, a smaller bounce, then another."""
    return mix(0.35,
               (0.00, _metal(0.12, f, 0.030, 0.55 * amp)),
               (0.07, _metal(0.09, tuple(x * 1.04 for x in f), 0.022, 0.30 * amp)),
               (0.12, _metal(0.07, tuple(x * 0.96 for x in f), 0.016, 0.17 * amp)))


def _bell(freq, dur, tau, amp=1.0):
    """A soft bell: harmonic body plus a few inharmonic partials that die away faster."""
    t = t_axis(dur)
    parts = ((1.0, 1.0, 1.0), (2.0, 0.42, 0.7), (2.76, 0.22, 0.45), (5.4, 0.08, 0.25))
    sig = sum(a * np.sin(2 * np.pi * freq * r * t) * np.exp(-t / (tau * k)) for r, a, k in parts)
    return sig * np.minimum(t / 0.003, 1.0) * amp


def _tone(seconds, freq, tau, harmonics=(1.0,)):
    t = t_axis(seconds)
    sig = sum(a * np.sin(2 * np.pi * freq * (i + 1) * t) for i, a in enumerate(harmonics))
    return sig * decay(seconds, tau)


def _formants(x, f1, f2, mixlow=0.35):
    """Gives a buzzy source a vowel: two resonant bands plus a little of the low body."""
    return bandpass(x, f1 * 0.78, f1 * 1.28) + 0.65 * bandpass(x, f2 * 0.8, f2 * 1.25) + mixlow * lowpass(x, 400)


def trim(x, floor=0.003):
    """Cuts the near-silent end off a sound (reverb tails are long and quiet)."""
    m = np.max(np.abs(x)) or 1.0
    idx = np.where(np.abs(x) > floor * m)[0]
    return x[: idx[-1] + 1] if len(idx) else x


def finish(x, peak=0.92, fade=0.02):
    x = trim(x)
    x = np.tanh(x * 1.2) if np.max(np.abs(x)) > 1.0 else x
    m = np.max(np.abs(x)) or 1.0
    x = x / m * peak
    n = max(1, int(SR * fade))
    x[-n:] *= np.linspace(1.0, 0.0, n)  # no click at the end of the sample
    return x


# ------------------------------------------------------------------ guns
def _gunshot(d, thump, crack, blast, snap=0.6, rt=0.35, wet=0.3, drive=1.3, tail_at=None):
    """Layers: snap + crack + blast + sub thump, saturated, then a room tail."""
    t = t_axis(d)
    f0, f1, tau, amp = thump
    snap_s = noise(d) * np.exp(-t / 0.0008) * snap
    crack_s = highpass(noise(d), crack[0]) * decay(d, crack[1], 0.0003) * crack[2]
    blast_s = lowpass(noise(d), blast[0]) * decay(d, blast[1], 0.0008) * blast[2]
    thump_s = sweep(d, f0, f1, 2.0) * decay(d, tau, 0.001) * amp
    dry = np.tanh((snap_s + crack_s + blast_s + thump_s) * drive)
    return reverb(dry, rt, wet, seed=11 if tail_at is None else tail_at)


def shot_pistol():
    dry = _gunshot(0.30, (210, 60, 0.06, 0.95), (900, 0.026, 0.85), (3000, 0.04, 0.55), rt=0.32, wet=0.28)
    return mix(0.8, (0.0, dry), (0.30, _casing(0.35)))


def shot_revolver():
    dry = _gunshot(0.55, (150, 38, 0.12, 1.15), (700, 0.04, 0.85), (2200, 0.075, 0.75), snap=0.7, rt=0.5, wet=0.36, drive=1.5)
    return mix(1.0, (0.0, dry))


def shot_smg():
    dry = _gunshot(0.16, (260, 90, 0.035, 0.75), (1200, 0.02, 0.9), (3500, 0.03, 0.45), rt=0.18, wet=0.2)
    return mix(0.4, (0.0, dry), (0.17, _casing(0.25)))


def shot_assault():
    dry = _gunshot(0.24, (200, 68, 0.05, 0.95), (1000, 0.026, 0.9), (3000, 0.045, 0.55), rt=0.3, wet=0.26, drive=1.4)
    return mix(0.6, (0.0, dry), (0.22, _casing(0.3)))


def shot_shotgun():
    return _gunshot(0.8, (120, 34, 0.2, 1.25), (800, 0.035, 0.75), (3500, 0.1, 0.95), snap=0.8, rt=0.6, wet=0.36, drive=1.5)


def shot_rifle():
    dry = _gunshot(0.65, (170, 45, 0.12, 0.95), (1300, 0.04, 0.9), (2500, 0.06, 0.55), snap=0.7, rt=0.6, wet=0.38)
    return mix(1.2, (0.0, dry), (0.40, _casing(0.3)))


def shot_sniper():
    d = 1.1
    t = t_axis(d)
    whip = bandpass(noise(d), 2500, 9000) * decay(d, 0.02, 0.0004) * 0.8
    dry = _gunshot(d, (140, 32, 0.17, 1.15), (1500, 0.05, 1.0), (2000, 0.1, 0.6), snap=0.9, rt=1.0, wet=0.5, drive=1.6)
    out = mix(2.1, (0.0, dry), (0.0, whip))
    return out


def shot_minigun():
    return _gunshot(0.13, (320, 110, 0.025, 0.65), (1500, 0.015, 0.9), (4000, 0.02, 0.4), rt=0.12, wet=0.12)


# ------------------------------------------------------------------ reloads (A at the start, B in the middle, C at the end)
# C sounds all put their final chunk at LEAD = 0.18 s, so the weapon is ready exactly on the beat.

# --- pistol: magazine out, magazine in, slide
def reload_pistol_a():
    return mix(0.5,
               (0.00, stack(_plastic_click(0.9), _metal(0.12, (3100, 4700, 6900), 0.02, 0.35))),     # release button
               (0.04, _scrape(0.13, 2500, 1500, 0.24, bright=1800)),                                  # magazine slides out
               (0.17, _thud(0.12, 210, 0.035, 0.75)),                                                 # into the palm
               (0.26, _metal(0.09, (2300, 3900), 0.012, 0.18)),                                       # loose rounds
               (0.32, _metal(0.08, (2700, 4400), 0.010, 0.12)))


def reload_pistol_b():
    return mix(0.4,
               (0.00, _scrape(0.10, 1400, 2800, 0.22, bright=1800)),                                  # new magazine in the well
               (0.09, _clack(1.0, 150, (2600, 4100), 0.016)))                                         # seated


def reload_pistol_c():
    return mix(0.45,
               (0.00, _scrape(0.09, 1900, 3200, 0.40, grit=90.0)),                                    # slide pulled back
               (0.085, _metal(0.05, (3300, 5200), 0.008, 0.25)),
               (LEAD, _clack(1.0, 260, (1700, 2900, 4800), 0.030)),                                   # slide snaps forward
               (LEAD + 0.05, _tick(0.02, 3000, 8500, 0.004) * 0.5))


# --- revolver: cylinder out + empties, rounds in, cylinder shut
def reload_revolver_a():
    parts = [(0.00, _plastic_click(0.8)),
             (0.04, _scrape(0.12, 1800, 2600, 0.18, bright=1200)),                                    # cylinder swings out
             (0.17, _clack(0.8, 190, (1900, 3100), 0.022))]                                           # stops
    for i, off in enumerate((0.27, 0.31, 0.37, 0.40, 0.46, 0.50)):                                    # six empties fall out
        parts.append((off, _casing(0.30 + 0.05 * (i % 3), f=(4000 + 250 * i, 5900, 7800))))
    return mix(0.9, *parts)


def reload_revolver_b():
    parts = []
    for i, off in enumerate((0.00, 0.075, 0.15, 0.23, 0.31, 0.385)):                                  # rounds dropped in one by one
        parts.append((off, stack(_metal(0.07, (3500 + 150 * i, 5400), 0.012, 0.28), _thud(0.05, 300, 0.01, 0.35))))
    return mix(0.55, *parts)


def reload_revolver_c():
    return mix(0.45,
               (0.00, _scrape(0.10, 1600, 2400, 0.22, bright=1200)),                                 # cylinder swings shut
               (0.10, _tick(0.012, 3000, 7000, 0.002) * 0.7),                                        # ratchet
               (0.13, _tick(0.012, 3000, 7000, 0.002) * 0.7),
               (0.155, _tick(0.012, 3000, 7000, 0.002) * 0.7),
               (LEAD, _clack(1.0, 210, (2200, 3600, 5800), 0.028)))                                  # locks


# --- SMG / assault rifle: heavy magazine, charging handle
def reload_smg_a():
    return mix(0.55,
               (0.00, _clack(0.8, 180, (3000, 4600), 0.014)),                                        # release
               (0.03, _scrape(0.16, 2300, 1300, 0.26, bright=1600)),                                 # long magazine slides out
               (0.20, _thud(0.12, 190, 0.04, 0.75)),
               (0.30, _metal(0.08, (2400, 4100), 0.011, 0.14)))


def reload_smg_b():
    return mix(0.45,
               (0.00, _scrape(0.10, 1300, 2600, 0.24, bright=1600)),
               (0.10, stack(_clack(1.05, 130, (2400, 3800, 5600), 0.02), _thud(0.1, 110, 0.04, 0.5))),  # slapped in
               (0.24, _tick(0.014, 2500, 6500, 0.003) * 0.4))                                        # tug to check it


def reload_smg_c():
    return mix(0.5,
               (0.00, _scrape(0.10, 1700, 3000, 0.38, grit=80.0)),                                   # charging handle back
               (0.10, _metal(0.05, (3000, 4800), 0.008, 0.22)),
               (LEAD, stack(_clack(1.05, 240, (1500, 2700, 4400), 0.03), _thud(0.08, 90, 0.03, 0.4))),  # bolt closes
               (LEAD + 0.05, _tick(0.02, 3000, 8000, 0.004) * 0.4))


# --- shotgun: action opens, shells are loaded, pump
def reload_shotgun_a():
    return mix(0.45,
               (0.00, _plastic_click(0.9)),
               (0.03, _thud(0.09, 160, 0.03, 0.8)),
               (0.06, _scrape(0.12, 1500, 2200, 0.22, bright=1300)),
               (0.20, _clack(0.8, 200, (2100, 3500), 0.02)))


def reload_shotgun_b():
    parts = []
    for off in (0.00, 0.17, 0.34):                                                                   # three shells into the tube
        parts.append((off, stack(_thud(0.08, 230, 0.022, 0.8),
                                 _metal(0.09, (3600, 5600), 0.014, 0.28),
                                 _tick(0.014, 2000, 6500, 0.003) * 0.5)))
    return mix(0.55, *parts)


def reload_shotgun_c():
    return mix(0.5,
               (0.00, _scrape(0.10, 1400, 2300, 0.32, grit=50.0)),                                   # forend slides back
               (0.07, _clack(0.55, 210, (1900, 3200), 0.02)),
               (LEAD, stack(_clack(1.15, 130, (1400, 2500, 4100), 0.034), _thud(0.1, 85, 0.04, 0.55))),  # chunk: pumped shut
               (LEAD + 0.04, _tick(0.02, 2500, 7000, 0.004) * 0.4))


# --- rifle: magazine out / in, bolt charge
def reload_rifle_a():
    return mix(0.55,
               (0.00, _clack(0.85, 170, (2800, 4400), 0.015)),
               (0.03, _scrape(0.15, 2200, 1300, 0.25, bright=1500)),
               (0.19, _thud(0.12, 200, 0.04, 0.8)),
               (0.29, _metal(0.08, (2500, 4200), 0.011, 0.14)))


def reload_rifle_b():
    return mix(0.45,
               (0.00, _scrape(0.10, 1300, 2500, 0.24, bright=1500)),
               (0.10, stack(_clack(1.1, 125, (2300, 3700, 5400), 0.022), _thud(0.1, 105, 0.045, 0.55))))


def reload_rifle_c():
    return mix(0.5,
               (0.00, _scrape(0.11, 1500, 2600, 0.38, grit=70.0)),
               (0.10, _clack(0.45, 230, (2100, 3600), 0.015)),                                       # bolt back
               (LEAD, stack(_clack(1.2, 200, (1300, 2400, 3900), 0.036), _thud(0.1, 100, 0.04, 0.5))))  # bolt forward


# --- sniper: bolt-action: lift and pull, rounds, push and lock
def reload_sniper_a():
    return mix(0.6,
               (0.00, _clack(0.7, 260, (3200, 5000), 0.012)),                                        # bolt handle up
               (0.05, _scrape(0.14, 1700, 2500, 0.34, grit=60.0)),                                   # bolt pulled back
               (0.19, _clack(0.9, 190, (1900, 3200), 0.022)),                                        # hits the stop
               (0.28, _casing(0.55)))                                                                # empty case rings on the floor


def reload_sniper_b():
    return mix(0.4,
               (0.00, stack(_thud(0.08, 250, 0.02, 0.8), _metal(0.09, (3300, 5200), 0.012, 0.3))),   # round pressed in
               (0.12, stack(_thud(0.08, 240, 0.02, 0.8), _metal(0.09, (3500, 5400), 0.012, 0.3))))


def reload_sniper_c():
    return mix(0.5,
               (0.00, _scrape(0.12, 2400, 1600, 0.32, grit=60.0)),                                   # bolt pushed forward
               (0.12, _tick(0.012, 3000, 8000, 0.002) * 0.6),
               (LEAD, stack(_clack(1.2, 160, (1700, 2900, 4700), 0.04), _thud(0.1, 95, 0.04, 0.5))))  # handle locks down


# --- minigun: ammo box, belt, cover
def reload_minigun_a():
    return mix(0.6,
               (0.00, _clack(1.0, 140, (700, 1500, 3100), 0.05)),                                    # latch
               (0.05, _scrape(0.2, 900, 1700, 0.22, grit=30.0, bright=900)),                         # cover swings open
               (0.26, _clack(0.8, 120, (800, 1700), 0.04)))


def reload_minigun_b():
    parts = [(0.0, _thud(0.12, 120, 0.05, 0.6))]
    for i in range(11):                                                                              # links of the belt rattle in
        parts.append((0.03 + i * 0.035, _metal(0.05, (2800 + 120 * (i % 4), 4400), 0.008, 0.30 * (1.0 - i * 0.05))))
    return mix(0.55, *parts)


def reload_minigun_c():
    return mix(0.5,
               (0.00, _scrape(0.12, 1200, 800, 0.22, grit=30.0, bright=900)),                       # cover swings shut
               (LEAD, stack(_clack(1.25, 110, (650, 1400, 2900, 4700), 0.06), _thud(0.12, 80, 0.05, 0.6))),  # slams
               (LEAD + 0.07, _metal(0.06, (2500, 4100), 0.01, 0.2)))                                 # latch


# ------------------------------------------------------------------ zombies and the player
def hit_flesh():
    d = 0.2
    thud = sweep(d, 150, 62, 2.0) * decay(d, 0.05)
    wet = lowpass(noise(d), 900) * decay(d, 0.035)
    slap = bandpass(noise(d), 600, 3200) * decay(d, 0.012, 0.0004) * 0.5
    return reverb(thud * 0.8 + wet * 0.8 + slap, 0.12, 0.12)


def _voice(d, f_curve, f1, f2, rasp=0.25, env_pow=1.5, jitter=0.03):
    t = t_axis(d)
    f = f_curve * (1 + jitter * np.sin(2 * np.pi * 5.3 * t) + 0.012 * np.sin(2 * np.pi * 11 * t))
    src = saw(f) + 0.4 * saw(f * 2.01)
    voice = _formants(src, f1, f2)
    env = np.sin(np.pi * np.clip(t / d, 0, 1)) ** env_pow
    breath = bandpass(noise(d), 500, 2200) * env * rasp
    return (voice * 0.7 + breath) * env


def zombie_death():
    d = 1.0
    t = t_axis(d)
    f = 150 - 90 * (t / d) ** 0.7
    voice = _voice(d, f, 520, 1050, rasp=0.35, env_pow=0.7)
    voice *= np.exp(-t / 0.5)
    thud = mix(d, (0.58, sweep(0.28, 95, 38, 2.0) * decay(0.28, 0.09) * 0.85),
               (0.58, lowpass(noise(0.2), 600) * decay(0.2, 0.04) * 0.5))
    return reverb(voice + thud, 0.3, 0.2)


def zombie_groan_1():
    d = 1.2
    t = t_axis(d)
    return reverb(_voice(d, 85 + 30 * np.sin(np.pi * t / d), 480, 980), 0.3, 0.18)


def zombie_groan_2():
    d = 1.5
    t = t_axis(d)
    return reverb(_voice(d, 100 - 28 * t / d, 560, 1250, rasp=0.3), 0.3, 0.18)


def zombie_swing():
    d = 0.34
    t = t_axis(d)
    air = bandpass(noise(d), 500, 3500) * np.sin(np.pi * np.clip(t / d, 0, 1)) ** 2
    scratch = highpass(noise(d), 2800) * decay(d, 0.05) * 0.25
    return air * 0.8 + scratch


def player_hurt():
    d = 0.42
    t = t_axis(d)
    thump = sweep(d, 240, 95, 1.5) * decay(d, 0.08)
    body = lowpass(noise(d), 1100) * decay(d, 0.07)
    grunt = _formants(saw(190 - 70 * t / d) + 0.3 * saw((190 - 70 * t / d) * 2.0), 600, 1200) * decay(d, 0.1) * 0.6
    return reverb(thump + body * 0.6 + grunt, 0.2, 0.15)


def poisoned():
    d = 0.7
    hiss = highpass(noise(d), 3200) * decay(d, 0.25) * 0.45
    out = hiss
    for off in (0.04, 0.17, 0.29, 0.43):
        f0 = rng.uniform(300, 500)
        out = out + mix(d, (off, sweep(0.1, f0, f0 * 2.2, 1.0) * decay(0.1, 0.03) * 0.55))
    return reverb(out, 0.25, 0.15)


def shield_block():
    d = 0.4
    zap = sweep(d, 1900, 700, 3.0) * decay(d, 0.06)
    ring = _bell(1320, d, 0.1, 0.6) + _bell(1980, d, 0.07, 0.3)
    return reverb(stack(zap * 0.55, ring, bandpass(noise(0.04), 3000, 9000) * decay(0.04, 0.01) * 0.5), 0.35, 0.25)


def heartbeat():
    d = 0.55
    def beat(amp):
        return lowpass(sweep(0.2, 72, 42, 1.5) * decay(0.2, 0.065), 170) * amp
    return mix(d, (0.0, beat(1.0)), (0.21, beat(0.7)))


# ------------------------------------------------------------------ pickups and grenades
def pickup_powerup():
    d = 0.7
    parts = [(o, _bell(f, 0.5, 0.11, 0.7)) for o, f in ((0.0, 659.3), (0.075, 830.6), (0.15, 987.8), (0.225, 1318.5))]
    return reverb(mix(d, *parts), 0.5, 0.3)


def pickup_heal():
    d = 0.9
    parts = [(0.0, _bell(523.3, 0.8, 0.22, 0.6)), (0.08, _bell(659.3, 0.75, 0.2, 0.55)), (0.16, _bell(784.0, 0.7, 0.2, 0.5)),
             (0.24, _bell(1046.5, 0.6, 0.18, 0.45))]
    swell = lowpass(noise(d), 1200) * np.sin(np.pi * np.clip(t_axis(d) / d, 0, 1)) ** 2 * 0.03
    return reverb(mix(d, *parts) + swell, 0.7, 0.32)


def pickup_grenade():
    d = 0.4
    return reverb(mix(d,
                      (0.0, stack(_clack(0.9, 220, (2400, 3800), 0.02), _plastic_click(0.5))),
                      (0.1, _metal(0.12, (3300, 5100, 7400), 0.03, 0.45))), 0.2, 0.15)


def grenade_throw():
    d = 0.45
    t = t_axis(d)
    air = bandpass(noise(d), 400, 2800) * np.sin(np.pi * np.clip(t / d, 0, 1)) ** 2 * 0.55
    return mix(d, (0.0, stack(_metal(0.14, (2900, 4700, 7100), 0.04, 0.5), _plastic_click(0.6))),   # pin pings out
               (0.06, _clack(0.6, 260, (1800, 3000), 0.02)),                                       # spoon flies off
               (0.09, air))


def explosion():
    d = 2.2
    t = t_axis(d)
    rumble = lowpass(noise(d), 150 + 2600 * np.exp(-t / 0.2)) * decay(d, 0.6)
    boom = sweep(d, 85, 22, 2.0) * decay(d, 0.55)
    crack = highpass(noise(d), 900) * decay(d, 0.05, 0.0004) * 0.8
    punch = lowpass(noise(d), 700) * decay(d, 0.09, 0.0006)
    crackle = np.zeros_like(t)
    spots = np.random.default_rng(5).uniform(0.1, 1.6, 90)                                           # debris raining down
    for s in spots:
        i = int(s * SR)
        n = min(len(t) - i, int(0.012 * SR))
        crackle[i:i + n] += noise(0.012)[:n] * np.exp(-s / 0.7) * 0.5 * np.exp(-np.arange(n) / (0.003 * SR))
    dry = np.tanh((rumble * 1.2 + boom * 1.25 + crack + punch * 0.7 + crackle * 0.6) * 1.2)
    return reverb(dry, 1.0, 0.4, damp=2500)


# ------------------------------------------------------------------ bosses
def _roar(d, f0, f1, drive, trem):
    t = t_axis(d)
    f = f0 + (f1 - f0) * np.sin(np.pi * np.clip(t / d, 0, 1) * 0.9)
    f = f * (1 + 0.03 * np.sin(2 * np.pi * 7 * t))
    src = saw(f) + 0.5 * saw(f * 1.01)
    src = np.tanh(src * drive)
    voice = _formants(src, 420, 900, 0.5)
    amp = np.sin(np.pi * np.clip(t / d, 0, 1)) ** 0.8
    amp *= 1 - trem + trem * (0.5 + 0.5 * np.sin(2 * np.pi * 26 * t))
    growl = lowpass(noise(d), 900) * amp * 0.3
    return voice * amp + growl


def boss_roar():
    return reverb(_roar(1.7, 58, 80, 2.5, 0.35), 0.8, 0.28)


def boss_windup():
    d = 0.8
    t = t_axis(d)
    f = 70 + 90 * (t / d) ** 1.5
    voice = _formants(np.tanh(saw(f) * 2.0), 400, 850, 0.5) * (0.3 + 0.7 * t / d)
    return reverb(voice * np.minimum(t / 0.05, 1.0), 0.4, 0.2)


def boss_charge():
    d = 0.8
    t = t_axis(d)
    air = bandpass(noise(d), 300, 2400) * np.sin(np.pi * np.clip(t / d, 0, 1)) ** 1.5
    parts = [(0.0, air * 0.7)]
    for off in (0.05, 0.2, 0.34, 0.47, 0.58):
        parts.append((off, sweep(0.16, 85, 33, 2.0) * decay(0.16, 0.055) * 0.9))
    return reverb(mix(d, *parts), 0.4, 0.2)


def boss_summon():
    d = 1.2
    t = t_axis(d)
    base = 190 + 60 * (t / d)
    vib = 1 + 0.03 * np.sin(2 * np.pi * 6 * t)
    a = np.sin(2 * np.pi * np.cumsum(base * vib) / SR)
    b = np.sin(2 * np.pi * np.cumsum(base * vib * 1.034) / SR)
    env = np.sin(np.pi * np.clip(t / d, 0, 1)) ** 1.2
    breath = highpass(noise(d), 1800) * env * 0.12
    return reverb((a + b) * 0.5 * env + breath, 0.9, 0.35)


def boss_death():
    d = 2.4
    t = t_axis(d)
    roar = _roar(d, 90, 28, 2.8, 0.4)
    boom = mix(d, (0.55, sweep(1.7, 70, 20, 2.0) * decay(1.7, 0.55)))
    rumble = mix(d, (0.55, lowpass(noise(1.7), 200 + 1800 * np.exp(-t[:int(SR * 1.7)] / 0.2)) * decay(1.7, 0.55)))
    return reverb(roar * 0.9 + boom * 1.1 + rumble, 1.2, 0.35, damp=2200)


# ------------------------------------------------------------------ waves and game flow
def wave_start():
    d = 1.4
    parts = []
    for off, f in ((0.0, 196), (0.32, 196), (0.64, 294)):
        n = int(SR * 0.45)
        tt = np.arange(n) / SR
        horn = lowpass(saw(np.full(n, f) * (1 + 0.004 * np.sin(2 * np.pi * 5 * tt))), 1500)
        horn += 0.5 * lowpass(saw(np.full(n, f * 1.5)), 1500)
        env = np.minimum(tt / 0.04, 1.0) * np.exp(-tt / 0.3)
        parts.append((off, horn * env))
    return reverb(mix(d, *parts), 0.7, 0.3)


def wave_start_2():
    """A soft low gong: inharmonic partials ringing out under a short swell."""
    d = 1.6
    t = t_axis(d)
    gong = sum(a * np.sin(2 * np.pi * f * t) * np.exp(-t / tau)
               for f, a, tau in ((110, 1.0, 0.7), (165.5, 0.6, 0.55), (233, 0.45, 0.4), (312, 0.3, 0.3), (448, 0.15, 0.2)))
    swell = lowpass(noise(d), 700) * np.minimum(t / 0.25, 1.0) * np.exp(-t / 0.35) * 0.6
    return reverb((gong + swell) * np.minimum(t / 0.01, 1.0), 0.9, 0.3)


def wave_start_3():
    """Two falling tones, like a quiet alert."""
    d = 1.2
    parts = []
    for off in (0.0, 0.42):
        n = int(SR * 0.45)
        tt = np.arange(n) / SR
        f = 392 - 110 * tt / 0.45
        sig = np.sin(2 * np.pi * np.cumsum(f) / SR) + 0.3 * np.sin(2 * np.pi * np.cumsum(f * 2) / SR)
        parts.append((off, sig * np.minimum(tt / 0.02, 1.0) * np.exp(-tt / 0.24)))
    return reverb(mix(d, *parts), 0.7, 0.3)


def game_start():
    d = 2.0
    t = t_axis(d)
    swell = np.sin(2 * np.pi * np.cumsum(np.linspace(80, 150, len(t))) / SR) * np.minimum(t / 0.5, 1.0) * np.exp(-t / 0.9) * 0.7
    parts = [(0.0, swell)]
    for i, f in enumerate((220.0, 261.6, 329.6, 440.0)):
        parts.append((0.25 + i * 0.2, _bell(f, 1.0, 0.4, 0.55)))
    return reverb(mix(d, *parts), 1.0, 0.35)


def thunder():
    d = 3.6
    t = t_axis(d)
    crack = highpass(noise(d), 800) * np.exp(-t / 0.05) * np.minimum(t / 0.001, 1.0) * 0.8
    rumble = lowpass(noise(d), 160 + 120 * np.exp(-t / 0.6))
    roll = 0.55 + 0.45 * np.sin(2 * np.pi * 1.3 * t + 2.0 * np.sin(2 * np.pi * 0.37 * t))
    env = np.minimum(t / 0.12, 1.0) * np.exp(-t / 1.2) * roll
    boom = np.sin(2 * np.pi * np.cumsum(55 * np.exp(-t / 2.0) + 28) / SR) * np.exp(-t / 0.6) * 0.5
    return crack + rumble * env * 5.0 + boom


def wave_clear():
    d = 1.6
    parts = []
    for off, f in ((0.0, 523.25), (0.1, 659.25), (0.2, 783.99), (0.32, 1046.5)):
        parts.append((off, _bell(f, 1.0, 0.3, 0.7 if off < 0.3 else 0.95)))
    return reverb(mix(d, *parts), 1.0, 0.38)


def game_over():
    d = 2.6
    t = t_axis(d)
    f = 220 * np.exp(-t / 1.3)
    a = lowpass(saw(f), 900)
    b = lowpass(saw(f * 1.5 * (1 + 0.01 * np.sin(2 * np.pi * 4 * t))), 700) * 0.6
    c = lowpass(saw(f * 1.19), 800) * 0.4                                                          # minor third
    env = np.minimum(t / 0.05, 1.0) * np.exp(-t / 1.0)
    boom = mix(d, (0.0, sweep(1.2, 90, 28, 2.0) * decay(1.2, 0.45) * 0.9))
    return reverb((a + b + c) * env + boom, 1.2, 0.35, damp=2500)


# ------------------------------------------------------------------ menus
def ui_click():
    d = 0.09
    tap = lowpass(_bell(1900, d, 0.012, 0.6) + _bell(2850, d, 0.008, 0.2), 7000)
    tick = bandpass(noise(0.012), 2500, 7000) * decay(0.012, 0.003, 0.0003) * 0.25
    return stack(tap, tick)


def ui_purchase():
    d = 0.9
    return reverb(mix(d, (0.0, _bell(1568.0, 0.7, 0.18, 0.7)), (0.07, _bell(2093.0, 0.7, 0.2, 0.8)),
                      (0.14, _bell(3136.0, 0.6, 0.12, 0.4))), 0.5, 0.3)


def ui_denied():
    d = 0.34
    parts = []
    for off, f in ((0.0, 260.0), (0.13, 196.0)):
        n = int(SR * 0.2)
        tt = np.arange(n) / SR
        body = (np.sin(2 * np.pi * f * tt) + 0.25 * np.sin(2 * np.pi * f * 2 * tt)) * np.exp(-tt / 0.07)
        parts.append((off, body * np.minimum(tt / 0.004, 1.0) * 0.8))
    return lowpass(mix(d, *parts), 2500)


SOUNDS = {
    "shot_pistol": shot_pistol, "shot_revolver": shot_revolver, "shot_smg": shot_smg, "shot_assault": shot_assault,
    "shot_shotgun": shot_shotgun, "shot_rifle": shot_rifle, "shot_sniper": shot_sniper, "shot_minigun": shot_minigun,
    "reload_pistol_a": reload_pistol_a, "reload_pistol_b": reload_pistol_b, "reload_pistol_c": reload_pistol_c,
    "reload_revolver_a": reload_revolver_a, "reload_revolver_b": reload_revolver_b, "reload_revolver_c": reload_revolver_c,
    "reload_smg_a": reload_smg_a, "reload_smg_b": reload_smg_b, "reload_smg_c": reload_smg_c,
    "reload_shotgun_a": reload_shotgun_a, "reload_shotgun_b": reload_shotgun_b, "reload_shotgun_c": reload_shotgun_c,
    "reload_rifle_a": reload_rifle_a, "reload_rifle_b": reload_rifle_b, "reload_rifle_c": reload_rifle_c,
    "reload_sniper_a": reload_sniper_a, "reload_sniper_b": reload_sniper_b, "reload_sniper_c": reload_sniper_c,
    "reload_minigun_a": reload_minigun_a, "reload_minigun_b": reload_minigun_b, "reload_minigun_c": reload_minigun_c,
    "hit_flesh": hit_flesh, "zombie_death": zombie_death,
    "zombie_groan_1": zombie_groan_1, "zombie_groan_2": zombie_groan_2, "zombie_swing": zombie_swing,
    "player_hurt": player_hurt, "poisoned": poisoned, "shield_block": shield_block, "heartbeat": heartbeat,
    "pickup_powerup": pickup_powerup, "pickup_heal": pickup_heal, "pickup_grenade": pickup_grenade,
    "grenade_throw": grenade_throw, "explosion": explosion,
    "boss_roar": boss_roar, "boss_windup": boss_windup, "boss_charge": boss_charge,
    "boss_summon": boss_summon, "boss_death": boss_death,
    "wave_start": wave_start, "wave_start_2": wave_start_2, "wave_start_3": wave_start_3, "game_start": game_start,
    "thunder": thunder,
    "wave_clear": wave_clear, "game_over": game_over,
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
    only = sys.argv[1:]  # optional: regenerate just these sounds
    os.makedirs(OUT, exist_ok=True)
    total = 0
    for name, fn in SOUNDS.items():
        if only and name not in only:
            continue
        sig = finish(fn())
        path = os.path.join(OUT, f"sfx_{name}.wav")
        write_wav(path, sig)
        size = os.path.getsize(path)
        total += size
        print(f"sfx_{name}.wav  {len(sig) / SR:4.2f}s  {size / 1024:5.1f} KB")
    print(f"{len(only) or len(SOUNDS)} files, {total / 1024:.0f} KB total")


if __name__ == "__main__":
    main()
