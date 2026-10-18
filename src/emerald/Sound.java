package emerald;

/**
 * What a cart's sound is made of:
 *
 *   Sfx      a sound effect or a bar of music: 32 notes, each with a pitch, an instrument, a volume
 *            and an effect, played at a speed (and optionally looping part of it). A cart has 64.
 *   Pattern  one step of a song: up to 4 sfx played together, one per channel. A cart has 64, and
 *            a song plays them in order (flags make it loop back, or stop).
 */
final class Sound {

    private Sound() { }

    static final int SFX_COUNT = 64, PATTERNS = 64, NOTES = 32, CHANNELS = 4;
    /** The instruments. */
    static final String[] WAVES = {"TRIANGLE", "SQUARE", "PULSE", "SAW", "ORGAN", "NOISE"};
    static final String[] WAVE_SHORT = {"TR", "SQ", "PU", "SW", "OR", "NS"};
    /** The effects a note can have. */
    static final String[] EFFECTS = {"NONE", "SLIDE", "VIBRATO", "DROP", "FADE IN", "FADE OUT"};
    /** A colour for each instrument (notes are drawn in their instrument's colour). */
    static final int[] WAVE_COLOURS = {11, 12, 13, 9, 10, 6};
    /** "C-", "C#"... for showing note names. */
    static final String[] NOTE_NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};

    static final class Sfx {
        /** 1 = fastest; each note lasts speed / 120 of a second. */
        int speed = 16;
        /** Notes loopStart up to (not including) loopEnd repeat while it plays; no loop when loopEnd <= loopStart. */
        int loopStart, loopEnd;
        final int[] pitch = new int[NOTES];   // 0..63: C2 upwards, 12 to an octave
        final int[] wave = new int[NOTES];    // 0..5 (WAVES)
        final int[] volume = new int[NOTES];  // 0 (silent) .. 7
        final int[] effect = new int[NOTES];  // 0..5 (EFFECTS)

        boolean empty() {
            for (int v : volume) if (v > 0) return false;
            return true;
        }

        boolean loops() {
            return loopEnd > loopStart;
        }

        /** Saved as one line: speed, loop start, loop end, then 5 hex digits a note (pitch 2, wave, volume, effect). */
        String encode() {
            StringBuilder out = new StringBuilder(String.format("%02x%02x%02x", speed, loopStart, loopEnd));
            for (int i = 0; i < NOTES; i++) out.append(String.format("%02x%x%x%x", pitch[i], wave[i], volume[i], effect[i]));
            return out.toString();
        }

        static Sfx decode(String line) {
            Sfx s = new Sfx();
            try {
                s.speed = Math.max(1, Integer.parseInt(line.substring(0, 2), 16));
                s.loopStart = Math.min(NOTES, Integer.parseInt(line.substring(2, 4), 16));
                s.loopEnd = Math.min(NOTES, Integer.parseInt(line.substring(4, 6), 16));
                for (int i = 0; i < NOTES; i++) {
                    int at = 6 + i * 5;
                    s.pitch[i] = Math.min(63, Integer.parseInt(line.substring(at, at + 2), 16));
                    s.wave[i] = Math.min(WAVES.length - 1, Character.digit(line.charAt(at + 2), 16));
                    s.volume[i] = Math.min(7, Character.digit(line.charAt(at + 3), 16));
                    s.effect[i] = Math.min(EFFECTS.length - 1, Character.digit(line.charAt(at + 4), 16));
                }
            } catch (RuntimeException ignored) {
                // a damaged line: what was read so far stays
            }
            return s;
        }
    }

    static final class Pattern {
        /** The sfx each channel plays (-1: nothing). */
        final int[] channels = {-1, -1, -1, -1};
        boolean loopStart, loopEnd, stop;

        boolean empty() {
            for (int c : channels) if (c >= 0) return false;
            return true;
        }

        /** Saved as: flags (1 loop start, 2 loop end, 4 stop), then 2 hex digits a channel (ff = nothing). */
        String encode() {
            int flags = (loopStart ? 1 : 0) | (loopEnd ? 2 : 0) | (stop ? 4 : 0);
            StringBuilder out = new StringBuilder(String.format("%x", flags));
            for (int c : channels) out.append(c < 0 ? "ff" : String.format("%02x", c));
            return out.toString();
        }

        static Pattern decode(String line) {
            Pattern p = new Pattern();
            try {
                int flags = Character.digit(line.charAt(0), 16);
                p.loopStart = (flags & 1) != 0;
                p.loopEnd = (flags & 2) != 0;
                p.stop = (flags & 4) != 0;
                for (int c = 0; c < CHANNELS; c++) {
                    int v = Integer.parseInt(line.substring(1 + c * 2, 3 + c * 2), 16);
                    p.channels[c] = v >= SFX_COUNT ? -1 : v;
                }
            } catch (RuntimeException ignored) {
                // a damaged line
            }
            return p;
        }
    }
}
