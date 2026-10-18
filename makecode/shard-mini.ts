// SHARD MINI - Pip and the Emerald Heart, small enough for a Meowbit.
// Paste all of this into the JavaScript tab at arcade.makecode.com
//
// Controls: left / right run, A jump (also off walls), B dash (aim with the d-pad).
// Climb out of the top of each room. Embers give your dash back in the air.

const T = 8
const W = 20
const H = 15

// ---------------------------------------------------------------- art

const PIP_IDLE = img`
    . . . . . . . . . . . . . . . .
    . . . . . . . . . . . . . . . .
    . . . . . f f f f f f . . . . .
    . . . . f 7 7 7 7 7 7 f . . . .
    . . . f 7 6 7 7 7 7 7 7 f . . .
    . . . f 7 7 7 7 7 7 7 7 7 f f .
    . . . f e d d d d d d f f f . .
    . . . f e e d d f d f d f . . .
    . . . f e d d d d d d d f . . .
    . . . . . f d d d d f . . . . .
    . . . f f 6 7 7 7 7 7 6 f f . .
    . . f d f 6 7 7 7 7 7 6 f d f .
    . . . f f e e 5 e e e f . . . .
    . . . . f 6 7 6 6 7 6 f . . . .
    . . . . f f f . . f f f . . . .
    . . . f e e f . . f e e f . . .
`
const PIP_RUN1 = img`
    . . . . . . . . . . . . . . . .
    . . . . . . . . . . . . . . . .
    . . . . . f f f f f f . . . . .
    . . . . f 7 7 7 7 7 7 f . . . .
    . . . f 7 6 7 7 7 7 7 7 f . . .
    . . . f 7 7 7 7 7 7 7 7 7 f f .
    . . . f e d d d d d d f f f . .
    . . . f e e d d f d f d f . . .
    . . . f e d d d d d d d f . . .
    . . . . . f d d d d f . . . . .
    . . . f f 6 7 7 7 7 7 6 f f . .
    . . f d f 6 7 7 7 7 7 6 f d f .
    . . . f f e e 5 e e e f . . . .
    . . . . f 6 7 6 6 7 6 f . . . .
    . . . f f f . . . . f f f . . .
    . . f e e f . . . . . f e e f .
`
const PIP_RUN2 = img`
    . . . . . . . . . . . . . . . .
    . . . . . f f f f f f . . . . .
    . . . . f 7 7 7 7 7 7 f . . . .
    . . . f 7 6 7 7 7 7 7 7 f . . .
    . . . f 7 7 7 7 7 7 7 7 7 f f .
    . . . f e d d d d d d f f f . .
    . . . f e e d d f d f d f . . .
    . . . f e d d d d d d d f . . .
    . . . . . f d d d d f . . . . .
    . . . f f 6 7 7 7 7 7 6 f f . .
    . . f d f 6 7 7 7 7 7 6 f d f .
    . . . f f e e 5 e e e f . . . .
    . . . . f 6 7 6 6 7 6 f . . . .
    . . . . f 6 7 6 6 7 6 f . . . .
    . . . . . f f f f . . . . . . .
    . . . . . f e e e f . . . . . .
`
const PIP_JUMP = img`
    . . . . . . . . . . . . . . . .
    . . . . . . . . . . . . . . . .
    . . . . . f f f f f f . . . . .
    . . . . f 7 7 7 7 7 7 f . . . .
    . . . f 7 6 7 7 7 7 7 7 f . . .
    . . . f 7 7 7 7 7 7 7 7 7 f f .
    . . . f e d d d d d d f f f . .
    . . . f e e d d f d f d f . . .
    . . . f e d d d d d d d f . . .
    . . . . . f d d d d f . . . . .
    . . d f f 6 7 7 7 7 7 6 f f d .
    . . . f 6 7 7 7 7 7 7 7 f . . .
    . . . f f e e 5 e e e f . . . .
    . . . . f 6 7 6 6 7 6 f . . . .
    . . . . f e e f . f e e f . . .
    . . . . . . . . . . . . . . . .
`
const PIP_DASH = img`
    . . . . . . . . . . . . . . . .
    . . . . . . . . . . . . . . . .
    . . . . . . f f f f f f . . . .
    . . . . . f 7 7 7 7 7 7 f . . .
    . . . . f 7 6 7 7 7 7 7 7 f . .
    . . . . f 7 7 7 7 7 7 7 7 7 f f
    . . . . f e d d d d d d f f f .
    . . . . f e e d d f d f d f . .
    . . . . f e d d d d d d d f . .
    . . . . . . f d d d d f . . . .
    . . . . f f 6 7 7 7 7 7 6 f f .
    . . . f d f 6 7 7 7 7 7 6 f d f
    . . . f f e e 5 e e e f . . . .
    . . . . f 6 7 6 6 7 6 f . . . .
    . . f f f f f . . . . . . . . .
    . f e e f f e e f . . . . . . .
`

// tiles: grass and dirt (meadow), stone with moss (caves), ice, snow on stone (summit)
const GRASS = img`
    7 7 7 7 7 7 7 7
    7 6 7 7 6 7 7 6
    e 7 e e e 6 e e
    e e e e e e e e
    e e e d e e e e
    e e e e e e d e
    e d e e e e e e
    e e e e e e e e
`
const DIRT = img`
    e e e e e e e e
    e e e d e e e e
    e e e e e e d e
    e d e e e e e e
    e e e e e e e e
    e e e e d e e e
    e e e e e e e d
    e e d e e e e e
`
const MOSS = img`
    7 6 7 7 6 7 6 7
    6 c 6 c c 6 c c
    c c c c c c c c
    c b c c c c b c
    c c c c c c c c
    c c c b c c c c
    c c c c c c c b
    c b c c c c c c
`
const STONE = img`
    c c c c c c c c
    c b c c c c b c
    c c c c c c c c
    f f f c f f f f
    c c c c c c c c
    c c b c c c b c
    c c c c c c c c
    f c f f f f c f
`
const ICE_TOP = img`
    1 1 1 1 1 1 1 1
    9 1 9 9 9 9 1 9
    9 9 1 9 9 9 9 9
    9 9 9 1 9 9 9 1
    9 9 9 9 1 9 9 9
    9 1 9 9 9 1 9 9
    9 9 9 9 9 9 1 9
    8 9 9 9 9 9 9 8
`
const ICE = img`
    9 9 9 1 9 9 9 9
    9 9 9 9 1 9 9 9
    1 9 9 9 9 1 9 9
    9 1 9 9 9 9 1 9
    9 9 1 9 9 9 9 1
    9 9 9 1 9 9 9 9
    9 9 9 9 1 9 9 9
    8 9 9 9 9 1 9 8
`
const SNOW = img`
    1 1 1 1 1 1 1 1
    1 1 1 9 1 1 1 1
    9 1 c 9 9 1 c 9
    c c c c c c c c
    c b c c c c b c
    c c c c c c c c
    f f c f f f f c
    c c c c c c c c
`
const SPIKES = img`
    . . . . . . . .
    . . . . . . . .
    . 1 . . . 1 . .
    . 1 . . . 1 . .
    b 1 b . b 1 b .
    b b b . b b b .
    b b b b b b b b
    c c c c c c c c
`
const SPRING = img`
    . . . . . . . .
    . . . . . . . .
    . . . . . . . .
    . 2 2 2 2 2 2 .
    . 2 3 3 3 3 2 .
    . . b . . b . .
    . . . b b . . .
    . f f c c f f .
`
const CRUMBLE = img`
    4 4 4 4 4 4 4 e
    4 5 4 4 4 e 4 e
    4 4 e e 4 4 4 e
    4 e 4 4 4 4 e e
    4 4 4 e 4 4 4 e
    4 5 e 4 4 4 4 e
    4 4 4 4 4 e 4 e
    e e e e e e e e
`
const EMBER = img`
    . . . 4 . . . .
    . . 4 5 4 . . .
    . 4 5 5 1 4 . .
    . 4 5 1 1 5 4 .
    4 5 5 1 1 5 5 4
    4 5 1 1 1 1 5 4
    . 4 5 5 5 5 4 .
    . . 4 4 4 4 . .
`
const COIN = img`
    . . f f f f . .
    . f 5 5 5 5 f .
    f 5 1 5 5 4 5 f
    f 5 1 5 4 5 5 f
    f 5 5 1 4 5 5 f
    f 5 5 4 5 5 4 f
    . f 4 4 4 4 f .
    . . f f f f . .
`
const SHARD = img`
    . . 6 7 7 6 . .
    . 6 7 1 1 7 6 .
    6 7 7 1 7 7 7 6
    6 7 7 7 7 7 7 6
    . 6 7 7 7 7 6 .
    . . 6 7 7 6 . .
    . . . 6 6 . . .
    . . . . . . . .
`
const FLOWER = img`
    . . . . . . . .
    . . . . . . . .
    . . . 2 . . . .
    . . 2 5 2 . . .
    . . . 2 . . . .
    . . . 6 . . . .
    . . 7 6 . . . .
    . . . 6 . . . .
`
const TUFT = img`
    . . . . . . . .
    . . . . . . . .
    . . . . . . . .
    . . . . . . . .
    . . . . . . . .
    . . . 7 . . . .
    . 7 . 7 . 7 . .
    . . 7 7 7 . . .
`
const CRYSTAL = img`
    . . . . . . . .
    . . . . . . . .
    . . . . 9 . . .
    . . . 9 1 . . .
    . 9 . 9 1 . 9 .
    . 9 1 9 1 1 9 .
    9 9 1 9 1 1 9 9
    . . . . . . . .
`
const YETI = img`
    . . . . . 6 7 7 6 . . . . . . .
    . . . . 6 7 1 7 7 6 . . . . . .
    . . . . f 1 1 1 1 f f . . . . .
    . . . f 1 1 1 1 1 1 1 f . . . .
    . . f 1 1 b b b b b 1 1 f . . .
    . . f 1 b 2 b b 2 b b 1 f . . .
    . . f 1 b b b b b b b 1 f . . .
    . . f 1 b 1 f f 1 b b 1 f . . .
    . f 1 1 1 b b b b b 1 1 1 f . .
    f 1 1 1 1 1 1 1 1 1 1 1 1 1 f .
    f 1 d 1 1 1 1 1 1 1 1 1 d 1 f .
    f 1 d 1 1 1 1 1 1 1 1 1 d 1 f .
    . f 1 1 1 1 1 1 1 1 1 1 1 f . .
    . f 1 1 1 f . . . f 1 1 1 f . .
    . f 1 1 f . . . . . f 1 1 f . .
    . f f f f . . . . . f f f f . .
`
const SNOWBALL = img`
    . f f f f .
    f 1 1 1 1 f
    f 1 1 1 9 f
    f 1 1 9 9 f
    f 1 9 9 9 f
    . f f f f .
`
const WAVE = img`
    . 1 1 .
    1 9 9 1
    1 1 1 1
    1 9 1 1
    1 1 9 1
    1 1 1 1
`

// ---------------------------------------------------------------- rooms
// # ground   ^ spikes   s spring   c crumbling block   e ember   o coin   p start
// themes: 0 meadow, 1 caves, 2 ice (slippery), 3 summit

const THEMES = [0, 0, 0, 4, 4, 2, 2, 3, 3]
const SKIES = [9, 12, 8, 11, 12]
const ROOMS: string[][] = [
    [   // 1: climb the steps
        "################...#",
        "#..................#",
        "#..................#",
        "#..............#####",
        "#..................#",
        "#...........###....#",
        "#..................#",
        "#..o....###........#",
        "#..................#",
        "#.###..............#",
        "#..................#",
        "#.....###..........#",
        "#.p................#",
        "##########...#######",
        "##########^^^#######"
    ],
    [   // 2: a spring and a crumbling block
        "#...################",
        "#..................#",
        "#..................#",
        "####...............#",
        "#..................#",
        "#.....cc...........#",
        "#..................#",
        "#.........###......#",
        "#..................#",
        "#..................#",
        "#..................#",
        "#..................#",
        "#.........o...s..p.#",
        "####################",
        "####################"
    ],
    [   // 3: too far to jump - dash! (B)
        "################..##",
        "#..................#",
        "#..................#",
        "#...............##.#",
        "#..................#",
        "#............###...#",
        "#..................#",
        "#..............###.#",
        "#..................#",
        "#...........###....#",
        "#........e.........#",
        "#.........o....###.#",
        "#.p................#",
        "#######......#######",
        "#######^^^^^^#######"
    ],
    [   // 4: the shaft - jump from wall to wall
        "#########..#########",
        "#.......#..#.......#",
        "#.......#..#.......#",
        "#.......#..#.......#",
        "#.......#..#.......#",
        "#.......#o.#.......#",
        "#.......#..#.......#",
        "#.......#..#.......#",
        "#.......#..#.......#",
        "#.......#..#.......#",
        "#.......#..#.......#",
        "#..................#",
        "#.p................#",
        "####################",
        "####################"
    ],
    [   // 5: crumbling bridge over spikes
        "#################..#",
        "#..................#",
        "#..................#",
        "#................###",
        "#..................#",
        "#............###...#",
        "#..................#",
        "#................###",
        "#.........o........#",
        "#.............###..#",
        "#..................#",
        "#....cc..cc..cc..###",
        "#.p................#",
        "####............####",
        "####^^^^^^^^^^^^####"
    ],
    [   // 6: slippery ice
        "#..#################",
        "#..................#",
        "#..................#",
        "###................#",
        "#..................#",
        "#....###...........#",
        "#..................#",
        "####...............#",
        "#..................#",
        "#....###...........#",
        "#..............o...#",
        "####...............#",
        "#.......^....^....p#",
        "####################",
        "####################"
    ],
    [   // 7: spring, then an ember in the air
        "#########..#########",
        "#..................#",
        "#..................#",
        "#.......####.......#",
        "#..................#",
        "#...###............#",
        "#..................#",
        "#...............o..#",
        "#..................#",
        "#..................#",
        "#..................#",
        "#..................#",
        "#ps................#",
        "###..............###",
        "###^^^^^^^^^^^^^^###"
    ],
    [   // 8: the summit
        "################...#",
        "#..................#",
        "#..................#",
        "#...............####",
        "#..................#",
        "#.............###..#",
        "#..................#",
        "#..................#",
        "#........e.........#",
        "#.......o..........#",
        "#..................#",
        "#.....cc...........#",
        "#p..........s......#",
        "#####......#########",
        "#####^^^^^^#########"
    ],
    [   // 9: the yeti
        "####################",
        "#..................#",
        "#..................#",
        "#..................#",
        "#..................#",
        "#..................#",
        "#..................#",
        "#..................#",
        "#..................#",
        "#.###..........###.#",
        "#..................#",
        "#..................#",
        "#.p................#",
        "####################",
        "####################"
    ]
]
const BOSS_ROOM = 8

// ---------------------------------------------------------------- state

let room = 0
let grid: string[] = []
let sky = 9
let bg: Image = null
let px = 0
let py = 0
let startX = 0
let startY = 0
let vx = 0
let vy = 0
let facing = 1
let grounded = false
let canDash = true
let dashTime = 0
let coyote = 0
let jumpBuffer = 0
let dashAsked = false
let wallLock = 0
let dead = 0
let steps = 0
let deaths = 0
let playing = false
let pip: Sprite = null
let spark: Sprite = null
let scarf: Sprite[] = []
let things: Sprite[] = []
let crumbles: number[][] = []   // [cx, cy, timer, back]
let taken: string[] = []        // coins already picked up (room:x:y), so they don't come back
let last = 0
let acc = 0

// the yeti
let yeti: Sprite = null
let yState = ""
let yTimer = 0
let yvx = 0
let yvy = 0
let yHits = 0
let yCount = 0
let balls: Sprite[] = []
let waves: Sprite[] = []
let shardDrop: Sprite = null

// ---------------------------------------------------------------- the map

function cellAt(x: number, y: number): string {
    const cx = Math.floor(x / T)
    const cy = Math.floor(y / T)
    if (cx < 0 || cx >= W) return "#"
    if (cy < 0 || cy >= H) return "."
    return grid[cy].charAt(cx)
}

function solidAt(x: number, y: number): boolean {
    const c = cellAt(x, y)
    return c == "#" || c == "c"
}

// Pip's body: 6 wide, 7 tall, from (x, y)
function boxSolid(x: number, y: number): boolean {
    return solidAt(x, y) || solidAt(x + 5, y) || solidAt(x, y + 6) || solidAt(x + 5, y + 6)
}

function setCell(cx: number, cy: number, ch: string) {
    grid[cy] = grid[cy].substr(0, cx) + ch + grid[cy].substr(cx + 1)
}

function tileFor(cx: number, cy: number): Image {
    const theme = THEMES[room]
    const top = cy == 0 || grid[cy - 1].charAt(cx) != "#"
    if (theme == 0) return top ? GRASS : DIRT
    if (theme == 1) return top ? MOSS : STONE
    if (theme == 2 || theme == 4) return top ? ICE_TOP : ICE   // 4: icy caves (looks like ice, not slippery)
    return top ? SNOW : STONE
}

function drawRoom() {
    bg = image.create(160, 120)
    bg.fill(sky)
    // a few far-away hills or stars
    for (let i = 0; i < 6; i++) {
        if (THEMES[room] == 4 || room == BOSS_ROOM) bg.setPixel(randint(0, 159), randint(0, 100), 13)
        else bg.fillCircle(i * 32 + 10, 124, 22, THEMES[room] == 2 ? 9 : 6)
    }
    for (let cy = 0; cy < H; cy++) {
        for (let cx = 0; cx < W; cx++) {
            const c = grid[cy].charAt(cx)
            const x = cx * T
            const y = cy * T
            if (c == "#") {
                bg.drawImage(tileFor(cx, cy), x, y)
                // decorations on top
                if (cy > 0 && grid[cy - 1].charAt(cx) == "." && Math.percentChance(30)) {
                    const theme = THEMES[room]
                    const deco = theme == 0 ? (Math.percentChance(50) ? FLOWER : TUFT) : CRYSTAL
                    bg.drawTransparentImage(deco, x, y - T)
                }
            }
            if (c == "^") bg.drawTransparentImage(SPIKES, x, y)
            if (c == "s") bg.drawTransparentImage(SPRING, x, y)
            if (c == "c") bg.drawTransparentImage(CRUMBLE, x, y)
        }
    }
    scene.setBackgroundImage(bg)
}

// ---------------------------------------------------------------- rooms

function clearThings() {
    for (const s of things) s.destroy()
    things = []
    for (const b of balls) b.destroy()
    balls = []
    for (const w of waves) w.destroy()
    waves = []
    if (yeti) yeti.destroy()
    yeti = null
    if (shardDrop) shardDrop.destroy()
    shardDrop = null
}

function startRoom() {
    clearThings()
    crumbles = []
    grid = []
    for (const row of ROOMS[room]) grid.push(row)
    sky = SKIES[THEMES[room]]
    if (room == BOSS_ROOM) sky = 12
    for (let cy = 0; cy < H; cy++) {
        for (let cx = 0; cx < W; cx++) {
            const c = grid[cy].charAt(cx)
            if (c == "p") {
                startX = cx * T + 1
                startY = cy * T + 1
                setCell(cx, cy, ".")
            } else if (c == "e" || c == "o") {
                if (c == "o" && taken.indexOf(room + ":" + cx + ":" + cy) >= 0) {
                    setCell(cx, cy, ".")
                    continue
                }
                const s = sprites.create(c == "e" ? EMBER : COIN, SpriteKind.Food)
                s.left = cx * T
                s.top = cy * T
                s.data["kind"] = (c)
                s.data["wait"] = (0)
                s.data["key"] = (room + ":" + cx + ":" + cy)
                things.push(s)
                setCell(cx, cy, ".")
            }
        }
    }
    drawRoom()
    px = startX
    py = startY
    vx = 0
    vy = 0
    dashTime = 0
    canDash = true
    dead = 0
    pip.setFlag(SpriteFlag.Invisible, false)
    for (const s of scarf) {
        s.x = px
        s.y = py + 4
    }
    if (room == BOSS_ROOM && yHits < 3) startYeti()
}

function nextRoom() {
    room++
    music.powerUp.play()
    startRoom()
}

function die() {
    if (dead > 0) return
    dead = 40
    deaths++
    music.bigCrash.play()
    scene.cameraShake(4, 300)
    const puff = sprites.create(PIP_IDLE, SpriteKind.Player)
    puff.x = pip.x
    puff.y = pip.y
    puff.startEffect(effects.disintegrate, 400)
    puff.lifespan = 400
    pip.setFlag(SpriteFlag.Invisible, true)
}

// ---------------------------------------------------------------- Pip

function moveX(a: number) {
    const nx = px + a
    if (!boxSolid(nx, py)) {
        px = nx
        return
    }
    const s = a > 0 ? 1 : -1
    let guard = 0
    while (!boxSolid(px + s, py) && guard < 8) {
        px += s
        guard++
    }
    vx = 0
}

function moveY(a: number) {
    const ny = py + a
    if (!boxSolid(px, ny)) {
        py = ny
        return
    }
    const s = a > 0 ? 1 : -1
    let guard = 0
    while (!boxSolid(px, py + s) && guard < 8) {
        py += s
        guard++
    }
    if (s > 0) py = Math.floor((py + 8) / 8) * 8 - 7
    vy = 0
}

function crumbleUnder() {
    for (const x of [px + 1, px + 4]) {
        if (cellAt(x, py + 7) == "c") {
            const cx = Math.floor(x / T)
            const cy = Math.floor((py + 7) / T)
            let known = false
            for (const c of crumbles) if (c[0] == cx && c[1] == cy && c[2] > 0) known = true
            if (!known) crumbles.push([cx, cy, 18, 0])
        }
    }
}

function updateCrumbles() {
    for (const c of crumbles) {
        if (c[2] > 0) {
            c[2]--
            if (c[2] == 0) {
                setCell(c[0], c[1], ".")
                bg.fillRect(c[0] * T, c[1] * T, T, T, sky)
                c[3] = 140
            }
        } else if (c[3] > 0) {
            c[3]--
            if (c[3] == 0) {
                // grow back, unless Pip is in the way
                if (Math.abs(c[0] * T + 4 - (px + 3)) < 8 && Math.abs(c[1] * T + 4 - (py + 3)) < 8) c[3] = 10
                else {
                    setCell(c[0], c[1], "c")
                    bg.drawTransparentImage(CRUMBLE, c[0] * T, c[1] * T)
                }
            }
        }
    }
}

function touches(ch: string): boolean {
    return cellAt(px + 1, py + 1) == ch || cellAt(px + 4, py + 1) == ch || cellAt(px + 1, py + 5) == ch || cellAt(px + 4, py + 5) == ch
}

function near(s: Sprite, r: number): boolean {
    return Math.abs(s.x - (px + 3)) < r && Math.abs(s.y - (py + 3)) < r
}

function step() {
    updateCrumbles()
    if (dead > 0) {
        dead--
        if (dead == 0) startRoom()
        return
    }
    steps++
    let dx = 0
    if (controller.left.isPressed()) dx = -1
    if (controller.right.isPressed()) dx = 1
    const jumpHeld = controller.A.isPressed()

    grounded = vy >= 0 && (solidAt(px, py + 7) || solidAt(px + 5, py + 7))
    if (grounded) {
        coyote = 6
        if (dashTime == 0) canDash = true
        crumbleUnder()
    } else if (coyote > 0) coyote--
    let wall = 0
    if (!grounded) {
        if (boxSolid(px + 1, py)) wall = 1
        else if (boxSolid(px - 1, py)) wall = -1
    }

    // jump: off the ground (a moment after leaving it still counts) or off a wall
    if (jumpBuffer > 0 && dashTime == 0) {
        if (coyote > 0) {
            vy = -3.4
            jumpBuffer = 0
            coyote = 0
            music.thump.play()
        } else if (wall != 0) {
            vy = -3.2
            vx = -wall * 2.2
            facing = -wall
            wallLock = 9
            jumpBuffer = 0
            music.thump.play()
        }
    }
    if (jumpBuffer > 0) jumpBuffer--

    // dash
    if (dashAsked) {
        dashAsked = false
        if (canDash) {
            let ddx = dx
            let ddy = 0
            if (controller.up.isPressed()) ddy = -1
            if (controller.down.isPressed()) ddy = 1
            if (ddx == 0 && ddy == 0) ddx = facing
            const speed = ddx != 0 && ddy != 0 ? 2.6 : 3.3
            vx = ddx * speed
            vy = ddy * speed
            if (ddx != 0) facing = ddx
            dashTime = 8
            canDash = false
            wallLock = 0
            scene.cameraShake(2, 100)
            music.pewPew.play()
        }
    }

    if (dashTime > 0) {
        dashTime--
        if (dashTime == 0) {
            vx *= 0.5
            vy *= 0.5
        }
    } else {
        if (dx != 0 && wallLock == 0) facing = dx
        if (wallLock > 0) wallLock--
        else {
            let grip = grounded ? 0.35 : 0.18
            if (grounded && THEMES[room] == 2) grip = 0.04   // ice: slow to start, slow to stop
            vx += (dx * 1.3 - vx) * grip
        }
        let g = 0.25
        if (Math.abs(vy) < 0.5 && jumpHeld) g = 0.12
        if (vy < 0 && !jumpHeld) g = 0.5
        vy += g
        if (wall != 0 && dx == wall && vy > 0.8) vy = 0.8
        if (vy > 3.5) vy = 3.5
    }

    moveX(vx)
    moveY(vy)

    // what Pip touches
    if (touches("^") || py > 122) {
        die()
        return
    }
    if (vy >= 0 && touches("s")) {
        vy = -5.5
        dashTime = 0
        canDash = true
        music.jumpUp.play()
    }
    for (const s of things) {
        const wait = (s.data["wait"] as number)
        if (wait > 0) {
            s.data["wait"] = (wait - 1)
            if (wait == 1) s.setFlag(SpriteFlag.Invisible, false)
            continue
        }
        if (s.flags & SpriteFlag.Invisible) continue
        if (!near(s, 6)) continue
        const kind = (s.data["kind"] as string)
        if (kind == "o") {
            info.changeScoreBy(1)
            taken.push((s.data["key"] as string))
            music.baDing.play()
            s.setFlag(SpriteFlag.Invisible, true)
            s.data["wait"] = (-1)
        } else if (kind == "e" && !canDash) {
            canDash = true
            music.magicWand.play()
            s.setFlag(SpriteFlag.Invisible, true)
            s.data["wait"] = (150)   // it comes back
        }
    }
    if (py < -6 && room < BOSS_ROOM) {
        nextRoom()
        return
    }
    if (room == BOSS_ROOM) updateYeti()
}

function drawPip() {
    let frame = PIP_IDLE
    if (dashTime > 0) frame = PIP_DASH
    else if (!grounded) frame = PIP_JUMP
    else if (Math.abs(vx) > 0.3) frame = Math.floor(steps / 6) % 2 == 0 ? PIP_RUN1 : PIP_RUN2
    const shown = frame.clone()
    if (facing < 0) shown.flipX()
    pip.setImage(shown)
    pip.x = px + 3
    pip.bottom = py + 7
    // the scarf: each bit follows the one before, blowing back
    let hx = px + 3 - facing * 4
    let hy = py + 1
    for (const s of scarf) {
        s.x += (hx - s.x) * 0.5
        s.y += (hy - s.y) * 0.5
        hx = s.x - facing * 1.5
        hy = s.y + 0.4
        s.setFlag(SpriteFlag.Invisible, dead > 0)
    }
    // the ember spark on the scarf's tip while the dash is ready
    const tip = scarf[scarf.length - 1]
    spark.x = tip.x + randint(-1, 1)
    spark.y = tip.y - 2
    spark.image.setPixel(0, 0, Math.percentChance(50) ? 4 : 5)
    spark.setFlag(SpriteFlag.Invisible, !canDash || dashTime > 0 || dead > 0)
}

// ---------------------------------------------------------------- the yeti

function startYeti() {
    yeti = sprites.create(YETI, SpriteKind.Enemy)
    yeti.left = 120
    yeti.top = 13 * T - 16
    yState = "walk"
    yTimer = 80
    yCount = 0
}

function yetiFloor(): number {
    return 13 * T - 16
}

function overlapsYeti(): boolean {
    return px + 6 > yeti.left + 2 && px < yeti.right - 2 && py + 7 > yeti.top + 2 && py < yeti.bottom
}

function updateYeti() {
    // snowballs and shockwaves
    for (const b of balls) {
        if (near(b, 5)) die()
        if (b.bottom > 13 * T) b.destroy()
    }
    balls = balls.filter(b => !(b.flags & sprites.Flag.Destroyed))
    for (const w of waves) {
        if (near(w, 5)) die()
        if (w.left < 8 || w.right > 152) w.destroy()
    }
    waves = waves.filter(w => !(w.flags & sprites.Flag.Destroyed))
    if (shardDrop) {
        if (shardDrop.bottom >= 13 * T) {
            shardDrop.vy = 0
            shardDrop.bottom = 13 * T
        }
        if (near(shardDrop, 7)) win()
        return
    }
    if (!yeti) return

    yTimer--
    const target = px + 3
    if (yState == "walk") {
        const dir = target < yeti.x ? -1 : 1
        yeti.x += dir * 0.4
        if (yTimer <= 0) {
            yCount++
            if (yCount % 3 == 0) {
                yState = "throw"
                yTimer = 30
            } else {
                yState = "crouch"
                yTimer = 30
            }
        }
    } else if (yState == "crouch") {
        yeti.x += randint(-1, 1)
        if (yTimer <= 0) {
            yState = "air"
            yvy = -5
            yvx = Math.constrain((target - yeti.x) / 40, -2, 2)
            music.jumpUp.play()
        }
    } else if (yState == "air") {
        yeti.x += yvx
        yvy += 0.25
        yeti.y += yvy
        if (yeti.top >= yetiFloor()) {
            yeti.top = yetiFloor()
            land()
        }
    } else if (yState == "throw") {
        if (yTimer <= 0) {
            const b = sprites.create(SNOWBALL, SpriteKind.Projectile)
            b.x = yeti.x
            b.y = yeti.top
            b.vx = (target - yeti.x) / 0.9   // lands near Pip about 0.9 seconds later
            b.vy = -90
            b.ay = 200
            balls.push(b)
            music.pewPew.play()
            yState = "walk"
            yTimer = 70
        }
    } else if (yState == "stun") {
        if (yTimer % 10 == 0) yeti.startEffect(effects.halo, 200)
        // a dash or a stomp on him while he's dizzy is a hit
        if (overlapsYeti() && (dashTime > 0 || vy > 0)) {
            yHits++
            music.smallCrash.play()
            scene.cameraShake(6, 300)
            vy = -3.5
            vx = (px + 3 < yeti.x ? -2 : 2)
            canDash = true
            if (yHits >= 3) {
                defeatYeti()
                return
            }
            yState = "walk"
            yTimer = 60
        }
        if (yTimer <= 0) {
            yState = "walk"
            yTimer = 70
        }
    }
    yeti.left = Math.constrain(yeti.left, 8, 136)
    // touching him hurts, unless he is dizzy
    if (yState != "stun" && overlapsYeti()) die()
}

function land() {
    scene.cameraShake(6, 400)
    music.bigCrash.play()
    for (const d of [-1, 1]) {
        const w = sprites.create(WAVE, SpriteKind.Projectile)
        w.bottom = 13 * T
        w.x = yeti.x + d * 10
        w.vx = d * 70
        waves.push(w)
    }
    yState = "stun"
    yTimer = 120
}

function defeatYeti() {
    yeti.startEffect(effects.disintegrate, 800)
    yeti.lifespan = 800
    const at = yeti.x
    yeti = null
    shardDrop = sprites.create(SHARD, SpriteKind.Food)
    shardDrop.x = at
    shardDrop.y = 40
    shardDrop.vy = 40
    shardDrop.startEffect(effects.halo)
}

function win() {
    playing = false
    shardDrop.destroy()
    shardDrop = null
    music.powerUp.play()
    game.showLongText("THE LAST SHARD! THE EMERALD HEART BEATS AGAIN.", DialogLayout.Bottom)
    game.showLongText("WARMTH ROLLS DOWN THE MOUNTAIN. THE SNOW MELTS INTO RIVERS.", DialogLayout.Bottom)
    game.showLongText("THE YETI BLINKS, CALM AND VERY FLUFFY. PIP HEADS HOME.", DialogLayout.Bottom)
    game.showLongText("COINS: " + info.score() + " / 8    FALLS: " + deaths, DialogLayout.Center)
    game.over(true)
}

// ---------------------------------------------------------------- start

controller.A.onEvent(ControllerButtonEvent.Pressed, function () {
    jumpBuffer = 6
})
controller.B.onEvent(ControllerButtonEvent.Pressed, function () {
    dashAsked = true
})

game.onUpdate(function () {
    if (!playing) return
    // the physics runs at a steady 60 steps a second, whatever the device's speed
    const now = game.runtime()
    acc += Math.min(now - last, 100)
    last = now
    while (acc >= 16) {
        step()
        acc -= 16
    }
    drawPip()
})

// the tune: a gentle loop in the background
control.runInBackground(function () {
    while (true) {
        music.playMelody("A C5 E5 C5 A B C5 - ", 140)
        music.playMelody("F A C5 A F G A - ", 140)
        music.playMelody("E G C5 G E G A B ", 140)
        music.playMelody("G B D5 B G A B - ", 140)
    }
})

pip = sprites.create(PIP_IDLE, SpriteKind.Player)
pip.z = 10
for (let i = 0; i < 3; i++) {
    const s = sprites.create(img`
        2 2
        2 2
    `, SpriteKind.Player)
    s.z = 9
    scarf.push(s)
}
spark = sprites.create(img`
    4
`, SpriteKind.Player)
spark.z = 11
info.setScore(0)

game.splash("SHARD MINI", "PIP AND THE EMERALD HEART")
game.showLongText("THE EMERALD HEART KEPT THE MOUNTAIN WARM. ONE NIGHT IT CRACKED, AND THE COLD CREPT IN.", DialogLayout.Bottom)
game.showLongText("PIP, A BOY FROM THE VALLEY, CLIMBS TO FIND THE LAST SHARD.", DialogLayout.Bottom)
game.showLongText("A: JUMP (ALSO OFF WALLS)   B: DASH, AIM WITH THE D-PAD. CLIMB OUT OF THE TOP OF EACH ROOM.", DialogLayout.Bottom)
startRoom()
last = game.runtime()
playing = true
