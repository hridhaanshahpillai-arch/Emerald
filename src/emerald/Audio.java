package emerald;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.util.Random;

/**
 * Emerald's sound chip: 4 channels, each playing one sfx at a time, mixed and sent to the speakers
 * on its own thread. Music plays a cart's patterns in order on the channels; sound effects take a
 * free channel (or the given one).
 *
 * Each note lasts speed x 183 samples (speed / 120 of a second). Pitch 0 is a low C (65 Hz), 12 to
 * an octave. Without a sound card it keeps time silently.
 */
final class Audio implements AutoCloseable {

    static final int RATE = 22050;
    static final int TICK = 183;          // samples in one speed unit
    private static final double BASE = 65.406; // pitch 0 (a C), in Hz

    /** One of the 4 channels. */
    private static final class Channel {
        int sfx = -1;                     // what it's playing (-1: nothing)
        long sample;                      // samples into the sfx (loops already applied)
        double phase;
        double amplitude;                 // glides to the note's volume so notes never click
        double noise;
        boolean music;                    // playing for the song (sound effects leave these alone)
        int note = -1;                    // the note playing now, for the editor
    }

    private final Channel[] channels = new Channel[Sound.CHANNELS];
    private final Random random = new Random();
    private volatile Cart cart = new Cart();
    // the song
    private int pattern = -1;
    private long patternSample, patternLength;

    private Thread thread;
    private volatile boolean running;
    private SourceDataLine line;

    Audio() {
        for (int i = 0; i < channels.length; i++) channels[i] = new Channel();
    }

    /** The cart whose sounds play (the editor's, so changes are heard straight away). */
    void setCart(Cart cart) {
        this.cart = cart;
    }

    Audio start() {
        running = true;
        try {
            if (Boolean.getBoolean("emerald.silent")) throw new IllegalStateException("silent");
            AudioFormat format = new AudioFormat(RATE, 16, 1, true, false);
            line = AudioSystem.getSourceDataLine(format);
            line.open(format, 2048 * 2); // ~90 ms queued
            line.start();
        } catch (Exception | LinkageError e) {
            line = null; // no sound card: keep time anyway
        }
        thread = new Thread(this::run, "Emerald sound");
        thread.setDaemon(true);
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
        return this;
    }

    private void run() {
        short[] block = new short[256];
        byte[] bytes = new byte[block.length * 2];
        long next = System.nanoTime();
        while (running) {
            mix(block);
            if (line != null) {
                for (int i = 0; i < block.length; i++) {
                    bytes[i * 2] = (byte) block[i];
                    bytes[i * 2 + 1] = (byte) (block[i] >> 8);
                }
                line.write(bytes, 0, bytes.length);
            } else {
                next += block.length * 1_000_000_000L / RATE;
                long wait = next - System.nanoTime();
                if (wait > 0) {
                    try {
                        Thread.sleep(wait / 1_000_000, (int) (wait % 1_000_000));
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
        }
    }

    @Override
    public void close() {
        running = false;
        if (thread != null) thread.interrupt();
        if (line != null) {
            line.stop();
            line.close();
        }
    }

    // ---------------------------------------------------------------- playing

    /** Plays sfx n on a channel (-1: a free one, not the music's if it can help it). n = -1 stops that channel. */
    synchronized void sfx(int n, int channel) {
        if (n < 0) {
            if (channel >= 0 && channel < channels.length) stopChannel(channels[channel]);
            else for (Channel c : channels) if (!c.music) stopChannel(c);
            return;
        }
        if (n >= Sound.SFX_COUNT) return;
        Channel c = channel >= 0 && channel < channels.length ? channels[channel] : freeChannel();
        if (c.music && channel >= 0) c.music = false; // asked for this channel: the song gives it up
        c.sfx = n;
        c.sample = 0;
        c.note = 0;
    }

    private Channel freeChannel() {
        for (Channel c : channels) if (c.sfx < 0) return c;
        for (Channel c : channels) if (!c.music) return c;   // all busy: replace a sound effect
        return channels[channels.length - 1];
    }

    private void stopChannel(Channel c) {
        c.sfx = -1;
        c.music = false;
        c.note = -1;
    }

    /** Starts the song at pattern n (-1 stops the music). */
    synchronized void music(int n) {
        for (Channel c : channels) if (c.music) stopChannel(c);
        pattern = -1;
        if (n < 0 || n >= Sound.PATTERNS) return;
        startPattern(n);
    }

    synchronized void stopAll() {
        for (Channel c : channels) stopChannel(c);
        pattern = -1;
    }

    /** The pattern the song is on (-1: no music). */
    synchronized int playingPattern() {
        return pattern;
    }

    /** Which note of sfx n is playing on any channel (-1 if it isn't), for the editor. */
    synchronized int playingNote(int n) {
        for (Channel c : channels) if (c.sfx == n) return c.note;
        return -1;
    }

    private void startPattern(int n) {
        Sound.Pattern p = cart.patterns[n];
        if (p.empty()) {
            pattern = -1;
            return;
        }
        pattern = n;
        patternSample = 0;
        patternLength = 0;
        long longestLoop = 0;
        for (int ch = 0; ch < Sound.CHANNELS; ch++) {
            int s = p.channels[ch];
            Channel c = channels[ch];
            if (s < 0) {
                if (c.music) stopChannel(c);
                continue;
            }
            c.sfx = s;
            c.sample = 0;
            c.music = true;
            c.note = 0;
            Sound.Sfx sound = cart.sfx[s];
            long length = (long) Sound.NOTES * sound.speed * TICK;
            // the pattern lasts as long as its first channel that doesn't loop (like a bar of music)
            if (!sound.loops() && patternLength == 0) patternLength = length;
            longestLoop = Math.max(longestLoop, length);
        }
        if (patternLength == 0) patternLength = longestLoop;
    }

    private void nextPattern() {
        Sound.Pattern p = cart.patterns[pattern];
        if (p.stop) {
            music(-1);
            return;
        }
        int next = pattern + 1;
        if (p.loopEnd) {
            next = 0;
            for (int i = pattern; i >= 0; i--) {
                if (cart.patterns[i].loopStart) {
                    next = i;
                    break;
                }
            }
        }
        if (next >= Sound.PATTERNS || cart.patterns[next].empty()) {
            music(-1);
            return;
        }
        startPattern(next);
    }

    // ---------------------------------------------------------------- mixing

    synchronized void mix(short[] out) {
        Cart c = cart;
        for (int i = 0; i < out.length; i++) {
            if (pattern >= 0 && ++patternSample >= patternLength) nextPattern();
            double mixed = 0;
            for (Channel ch : channels) mixed += next(ch, c);
            mixed = Math.tanh(mixed * 1.2) * 0.9; // a soft limit: loud chords squash smoothly
            out[i] = (short) Math.round(mixed * 32767);
        }
    }

    /** The next sample of a channel. */
    private double next(Channel ch, Cart c) {
        if (ch.sfx < 0) {
            ch.amplitude *= 0.99;
            return 0;
        }
        Sound.Sfx s = c.sfx[ch.sfx];
        long noteLength = (long) Math.max(1, s.speed) * TICK;
        int note = (int) (ch.sample / noteLength);
        if (s.loops() && note >= s.loopEnd) {
            ch.sample -= (long) (s.loopEnd - s.loopStart) * noteLength;
            note = (int) (ch.sample / noteLength);
        }
        if (note >= Sound.NOTES) {
            if (ch.music && pattern >= 0) {
                ch.note = -1; // done before the pattern: quiet until the next one
                ch.amplitude *= 0.99;
                return 0;
            }
            stopChannel(ch);
            return 0;
        }
        ch.note = note;
        double t = (double) (ch.sample % noteLength) / noteLength; // how far through the note, 0..1
        ch.sample++;
        int volume = s.volume[note];
        double frequency = BASE * Math.pow(2, s.pitch[note] / 12.0);
        double level = volume / 7.0;
        switch (s.effect[note]) {
            case 1 -> { // slide from the note before
                int before = note > 0 ? s.pitch[note - 1] : s.pitch[note];
                frequency = BASE * Math.pow(2, (before + (s.pitch[note] - before) * t) / 12.0);
            }
            case 2 -> frequency *= 1 + 0.025 * Math.sin(ch.sample * 2 * Math.PI * 7 / RATE); // vibrato
            case 3 -> frequency *= Math.max(0.02, 1 - t);                                      // drop
            case 4 -> level *= t;                                                               // fade in
            case 5 -> level *= 1 - t;                                                           // fade out
            default -> { }
        }
        double previous = ch.phase;
        ch.phase = (ch.phase + frequency / RATE) % 1;
        double p = ch.phase;
        double wave = switch (s.wave[note]) {
            case 0 -> 1 - 4 * Math.abs(p - 0.5);                                   // triangle
            case 1 -> (p < 0.5 ? 1 : -1) * 0.55;                                   // square
            case 2 -> (p < 0.25 ? 1 : -1) * 0.5;                                   // pulse
            case 3 -> (2 * p - 1) * 0.6;                                           // saw
            case 4 -> (1 - 4 * Math.abs(p - 0.5)) * 0.65 + (1 - 4 * Math.abs((p * 2) % 1 - 0.5)) * 0.35; // organ
            default -> {                                                           // noise
                if (ch.phase < previous || random.nextInt(4) == 0) ch.noise = random.nextDouble() * 2 - 1;
                yield ch.noise * 0.6;
            }
        };
        ch.amplitude += (level - ch.amplitude) * 0.02; // glide: no clicks between notes
        return wave * ch.amplitude * 0.3;
    }
}
