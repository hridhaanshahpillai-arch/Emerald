# EMERALD 💚

A cozy fantasy console. Make tiny games on a 128 x 128 screen with 16 colours: code, sprites,
maps, sounds and music, all in one little window, written in **Seven** (the language from
[Sev7n](https://github.com/hridhaanshahpillai-arch/Sev7n)).

Like PICO-8, but roomier, and free:

| | PICO-8 | Emerald |
|-|--------|---------|
| Code | 8,192 tokens | **65,536 tokens** |
| Map | 128 x 32 tiles | **128 x 64 tiles** |
| Keys | 6 buttons | **every key + the mouse + controllers** |
| Price | $15 | **free** |

## Run it

Double-click **build.bat** once (it makes `build\emerald.jar`; needs Java 21), then **emerald.bat**.
Emerald opens fullscreen: **F11** switches to a window and back. The screen stays sharp, never stretched.

Emerald starts at its **command line**. Type `HELP` for the commands:

| | |
|-|-|
| `LOAD DEMO` | open **SHARD**, the game that comes with Emerald (`RUN` to play, Esc to see how it's made) |
| `LOAD MYGAME` / `SAVE MYGAME` | open / save a cart (in the `carts` folder) |
| `SAVE MYGAME.PNG` | save the cart as a **picture** of a cartridge, with your game on the label |
| `RUN` | play it (Ctrl+R works anywhere) |
| `NEW` | an empty cart |
| `EXPORT` | turn the cart into an app (`exports\NAME\NAME\NAME.exe`, Java inside) to share |
| `DIR`, `CD NAME` | look around the carts folder |
| `FOLDER` | open the folder on your PC |
| `EXIT` | turn Emerald off |

`emerald.bat carts\shard.emerald` plays a cart straight away instead.

**Esc** moves around: game → command line → editor → command line. **F6** saves a screenshot and
**F8** starts / stops a GIF (in the `screenshots` folder), wherever you are.

## Finding bugs

While a game runs (not in exported games): **F5** pauses, **F10** goes one frame forward, holding **F4**
rewinds up to 10 seconds, and **F7** shows your variables live.

## The editor

- **CODE** `{ }`: type your game. Ctrl+S saves, Ctrl+R (or RUN) runs it. Ctrl+C/X/V, Ctrl+Z/Y,
  Shift+arrows to select, the mouse to click and scroll. Real capitals have a line under them.
- **SPRITES** (ghost): draw 8x8 (or 16x16) pixel art. Left mouse paints, right mouse picks a colour,
  PEN or FILL, and the sheet at the bottom (tabs 1-4) picks which sprite to draw. The 8 dots under
  the palette are the sprite's **flags**: mark tiles as solid, deadly, a ladder... and check them with `fget`.
  **ANIM** plays this sprite and the next 1-3 as an animation, so you can see a walk cycle as you draw it.
- **MAP** (grid): paint levels with your sprites as tiles, every 16 x 16 tiles one screen. Left
  mouse paints, right mouse picks up a tile, tile 0 erases. Arrows, the wheel or Space + drag move
  around; ZOOM / 8PX switches between the overview and full size. **AUTO** (or T) auto-tiles: draw a set
  of 16 tiles in one sheet row, ordered by which sides are open (+1 above, +2 below, +4 left, +8 right),
  paint with any of them, and Emerald picks the right edge and corner pieces for you.
- **SFX** (wave): make sounds. Paint notes with the mouse (higher = higher note), right mouse erases,
  the strip under them is each note's volume. Pick an instrument (TR triangle, SQ square, PU pulse,
  SW saw, OR organ, NS noise) and an effect (slide, vibrato, drop, fade in, fade out) before you
  paint. SPD is the speed, LOOP repeats part of the sound. Space plays it.
- **MUSIC** (notes): make songs. Each pattern plays up to 4 sounds at once, and the song plays
  patterns in order. LOOP START / LOOP END make it repeat, STOP ends it. Space plays from here.

Everything is saved together in the one `.emerald` file (or `.png` cart picture). You can still
edit a `.emerald` in any text editor too: Emerald reloads it when you save.

Look at `carts/shard.emerald` (`LOAD DEMO`): a whole game with 9 levels, a boss, a story, menus and music.

**SHARD for MakeCode Arcade** (and the Meowbit) is in the `makecode` folder: `shard-full.ts` is the whole game,
`shard-mini.ts` a small one-screen-rooms version. Paste either into the JavaScript tab at arcade.makecode.com.

## A cart

```seven
var x = 60

func init() { }             // once at the start

func update() {             // every frame
    if btn(0) { x -= 1 }    // left
    if btn(1) { x += 1 }    // right
}

func draw() {               // every frame
    cls(1)
    print("HELLO!", 2, 2, 11)
    circfill(x, 64, 6, 7)
}
```

## Commands

| Drawing | |
|---------|-|
| `cls([c])` | clear the screen |
| `pset(x, y, [c])`, `pget(x, y)` | one pixel |
| `line(x0, y0, x1, y1, [c])` | |
| `rect(...)`, `rectfill(...)` | a box, outlined or filled |
| `circ(x, y, r, [c])`, `circfill(...)` | a circle |
| `print(text, [x, y], [c])` | text (always in capitals) |
| `color(c)` | the colour used when you leave one out |
| `spr(n, x, y, [w, h], [flipX], [flipY])` | sprite n (w x h sprites big) |
| `sspr(sx, sy, sw, sh, x, y, [w, h], [flipX], [flipY])` | any part of the sprite sheet, stretched to w x h |
| `sget(x, y)`, `sset(x, y, c)` | a pixel of the sprite sheet |
| `map([tx, ty], [x, y], [w, h], [flags])` | draw part of the map (one screen from 0, 0 if left out; only tiles with those flags if given) |
| `mget(x, y)`, `mset(x, y, n)` | read / change a map tile (0 = empty) |
| `fget(n, [f])`, `fset(n, [f], on)` | sprite n's flags: one (0-7), or all 8 as a number |
| `camera(x, y)` | shift everything drawn after it (the camera follows the player); `camera()` resets |
| `pal(c0, c1, [1])` | draw colour c0 as c1 (with 1: show it as c1 on the whole screen, for flashes and fades); `pal()` resets |
| `palt(c, on)` | make colour c see-through in sprites and the map (0 is, at first); `palt()` resets |

| Input | |
|-------|-|
| `btn(b, [player])`, `btnp(b, [player])` | held / just pressed: 0 left, 1 right, 2 up, 3 down (arrows or WASD), 4 = Z, 5 = X. Player 0 is the keyboard and the first controller, 1-3 more controllers |
| `controllers()` | how many controllers are plugged in |
| `key("a")`, `keyp("space")` | any key by name: letters, numbers, `space`, `enter`, `shift`, `ctrl`, `tab`, `escape`, `up`... `f1`-`f12` |
| `mousex()`, `mousey()` | where the mouse is on the screen (-1 outside) |
| `mousedown()`, `click()` | button held / just clicked |
| `cursor("hand")` | `"arrow"`, `"hand"` or `"none"` |

| Sound | |
|-------|-|
| `sfx(n, [channel])` | play sound n (on a free channel, or channel 0-3); `sfx(-1)` stops sounds |
| `music(n)` | play the song from pattern n; `music(-1)` stops it |

| Saving | |
|--------|-|
| `cartdata("mygame")` | once: where this game's saves go |
| `dset(slot, n)`, `dget(slot)` | 64 numbers (slots 0-63) kept after the game closes: high scores, levels, settings |

| Other | |
|-------|-|
| `widescreen(true)` | make the screen 228 x 128 (16:9) so it fills wide monitors; `screenwidth()` says how wide it is |
| `rnd([n])` | a random number from 0 up to n (1 if left out) |
| `flr(x)` | round down |
| `t()` | seconds since the cart started |
| `log(...)` | print to the terminal (for debugging) |

| Helpers | |
|---------|-|
| `burst(x, y, [n], [c], [speed])` | a puff of particles (moved and drawn for you) |
| `shake([amount])` | shake the screen; it settles by itself |
| `say(text)`, `saying()` | a dialogue box, typed out letter by letter (Z, X or click to go on) |
| `after(seconds, func)`, `every(seconds, func)`, `stoptimers()` | call a function later, or again and again |

Don't name your own functions the same as these (or any command here): yours would replace Emerald's.

Plus everything Seven has: lists, maps, `sin`, `cos`, `abs`, `min`, `max`, `clamp`, f-strings and more.

Controllers: Xbox, PlayStation, Switch Pro and most others (the d-pad or left stick moves, A / Cross
is 4, B / Circle is 5).

## Colours

| | | | |
|-|-|-|-|
| 0 ink | 1 night | 2 plum | 3 deep emerald |
| 4 cocoa | 5 stone | 6 mist | 7 cream |
| 8 strawberry | 9 peach | 10 lemon | 11 emerald |
| 12 sky | 13 lavender | 14 pink | 15 sakura |

The PixelPico font, the cursors and the icon were all made for Emerald by its creator.

## License

MIT: use it, change it, share it, make and sell games with it. See [LICENSE](LICENSE).
