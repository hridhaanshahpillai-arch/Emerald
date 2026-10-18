package emerald;

import java.awt.event.KeyEvent;

/**
 * The MUSIC editor: a song is a row of patterns, and each pattern plays up to 4 sfx at once (one
 * per channel), like the instruments in a band. Pick a pattern, give its channels sfx numbers,
 * and mark where the song loops back or stops.
 *
 *   each channel   click the box to turn it on or off, < > (or the wheel) to pick its sfx
 *   LOOP START / LOOP END / STOP   what happens after this pattern
 *   the strip at the bottom: every pattern (click one); PLAY (or Space) plays the song from here
 */
final class MusicEditor {

    private static final int[] ROW_Y = {22, 39, 56, 73};

    private final Audio audio;
    private Cart cart;
    int current;                         // which pattern
    private final int[] lastSfx = {0, 1, 2, 3}; // what a channel gets when it's turned back on
    private boolean dirty;
    String hover = "";
    boolean overButton;

    MusicEditor(Audio audio) {
        this.audio = audio;
    }

    void setCart(Cart cart) {
        this.cart = cart;
        dirty = false;
    }

    boolean dirty() {
        return dirty;
    }

    void saved() {
        dirty = false;
    }

    void update(Input input) {
        hover = "";
        overButton = false;
        int mx = input.mouseX, my = input.mouseY;
        Sound.Pattern p = cart.patterns[current];
        for (Input.KeyPress k : input.presses()) if (!k.ctrl() && k.code() == KeyEvent.VK_SPACE) togglePlay();

        button(input, 2, 10, "<", () -> current = (current + Sound.PATTERNS - 1) % Sound.PATTERNS);
        button(input, 54, 10, ">", () -> current = (current + 1) % Sound.PATTERNS);
        button(input, 104, 10, audio.playingPattern() >= 0 ? "STOP" : "PLAY", this::togglePlay);
        for (int ch = 0; ch < Sound.CHANNELS; ch++) {
            final int c = ch;
            int y = ROW_Y[ch];
            if (button(input, 2, y + 4, p.channels[c] >= 0 ? "ON" : "--", () -> {
                if (p.channels[c] >= 0) {
                    lastSfx[c] = p.channels[c];
                    p.channels[c] = -1;
                } else {
                    p.channels[c] = lastSfx[c];
                }
                dirty = true;
            })) hover = "CHANNEL " + (c + 1) + ": ON OR OFF";
            button(input, 16, y + 4, "<", () -> setSfx(p, c, -1));
            button(input, 34, y + 4, ">", () -> setSfx(p, c, 1));
            boolean overRow = mx >= 0 && my >= y && my < y + 15;
            if (overRow && input.wheel() != 0 && p.channels[c] >= 0) setSfx(p, c, input.wheel());
            if (overRow && hover.isEmpty() && p.channels[c] >= 0) hover = "CHANNEL " + (c + 1) + " PLAYS SFX " + p.channels[c];
        }
        if (button(input, 2, 93, "LOOP START", () -> {
            p.loopStart = !p.loopStart;
            dirty = true;
        })) hover = "LOOP END GOES BACK TO HERE";
        if (button(input, 46, 93, "LOOP END", () -> {
            p.loopEnd = !p.loopEnd;
            dirty = true;
        })) hover = "AFTER THIS: BACK TO LOOP START";
        if (button(input, 82, 93, "STOP", () -> {
            p.stop = !p.stop;
            dirty = true;
        })) hover = "THE SONG ENDS AFTER THIS";
        // the pattern strip
        if (mx >= 0 && mx < 128 && my >= 103 && my < 119) {
            int n = (my - 103) / 4 * 16 + mx / 8;
            hover = "PATTERN " + n;
            overButton = true;
            if (input.mouseClicked()) current = n;
        }
    }

    private void setSfx(Sound.Pattern p, int channel, int by) {
        int now = p.channels[channel] >= 0 ? p.channels[channel] : lastSfx[channel];
        p.channels[channel] = Math.floorMod(now + by, Sound.SFX_COUNT);
        lastSfx[channel] = p.channels[channel];
        dirty = true;
    }

    private void togglePlay() {
        if (audio.playingPattern() >= 0) audio.music(-1);
        else audio.music(current);
    }

    private boolean button(Input input, int x, int y, String label, Runnable action) {
        int w = label.length() * PixelFont.ADVANCE + 3;
        boolean over = input.mouseX >= x && input.mouseY >= y && input.mouseX < x + w && input.mouseY < y + 7;
        if (over) {
            overButton = true;
            if (input.mouseClicked()) action.run();
        }
        return over;
    }

    void draw(Screen s, PixelFont font) {
        Sound.Pattern p = cart.patterns[current];
        s.rectfill(0, 8, 127, 120, 1);
        label(s, font, 2, 10, "<", false);
        font.draw(s, String.format("PATTERN %02d", current), 11, 11, 7);
        label(s, font, 54, 10, ">", false);
        boolean playing = audio.playingPattern() >= 0;
        label(s, font, 104, 10, playing ? "STOP" : "PLAY", playing);
        for (int ch = 0; ch < Sound.CHANNELS; ch++) {
            int y = ROW_Y[ch], sfx = p.channels[ch];
            s.rectfill(0, y, 127, y + 14, 0);
            label(s, font, 2, y + 4, sfx >= 0 ? "ON" : "--", sfx >= 0);
            label(s, font, 16, y + 4, "<", false);
            font.draw(s, sfx >= 0 ? String.format("%02d", sfx) : "--", 25, y + 5, sfx >= 0 ? 7 : 5);
            label(s, font, 34, y + 4, ">", false);
            if (sfx < 0) continue;
            // a little picture of the sfx: its notes as a line of dots
            Sound.Sfx sound = cart.sfx[sfx];
            int playingNote = playing ? audio.playingNote(sfx) : -1;
            for (int i = 0; i < Sound.NOTES; i++) {
                int x = 46 + i * 2;
                if (i == playingNote) s.rectfill(x, y + 1, x + 1, y + 13, 5);
                if (sound.volume[i] == 0) continue;
                int dotY = y + 13 - sound.pitch[i] * 12 / 63;
                s.rectfill(x, dotY, x + 1, dotY + 1, Sound.WAVE_COLOURS[sound.wave[i]]);
            }
        }
        label(s, font, 2, 93, "LOOP START", p.loopStart);
        label(s, font, 46, 93, "LOOP END", p.loopEnd);
        label(s, font, 82, 93, "STOP", p.stop);
        // every pattern: filled if it has something, outlined if it's this one, bright while it plays
        int playingPattern = audio.playingPattern();
        for (int n = 0; n < Sound.PATTERNS; n++) {
            int x = n % 16 * 8, y = 103 + n / 16 * 4;
            int colour = n == playingPattern ? 11 : cart.patterns[n].empty() ? 0 : 3;
            s.rectfill(x, y, x + 6, y + 2, colour);
            if (n == current) s.rect(x - 1, y - 1, x + 7, y + 3, 7);
        }
    }

    private static void label(Screen s, PixelFont font, int x, int y, String text, boolean on) {
        int w = text.length() * PixelFont.ADVANCE + 3;
        s.rectfill(x, y, x + w - 1, y + 6, on ? 11 : 5);
        font.draw(s, text, x + 2, y + 1, on ? 1 : 7);
    }
}
