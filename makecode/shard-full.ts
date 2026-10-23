// SHARD - Pip and the Emerald Heart. The full game (9 levels, the story, the Yeti), for MakeCode Arcade.
// Paste all of this into the JavaScript tab at arcade.makecode.com
//
// Controls: left / right run, A jump (also off walls), B dash (aim with the d-pad).
// Grab the shard at the end of each level. Embers give your dash back in the air.

// everything lives in its own namespace, so it can't clash with other code in the project
namespace shard {
    const T = 8

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

    const LEVELS: string[][] = [
        [
            "................................................",
            "................................................",
            "................................................",
            "................................................",
            "................................................",
            "................................................",
            "................................................",
            ".............................b..................",
            "............................===.................",
            "...............b................................",
            "...........b..====.....#####.................G..",
            ".......................#####...........#########",
            "...................#########......^^...#########",
            "##########...###############...#################",
            "##########...###############...#################",
            "##########...###############...#################"
        ],
        [
            "................................................",
            "................................................",
            ".....................................b..........",
            "..............................................G.",
            ".........................................D..####",
            "............................................####",
            "...................................####.....####",
            "...................................####.....####",
            "...............................CCC.####.....####",
            "...........b.......................####.....####",
            ".....................#########.....####.....####",
            ".....................#########.....####.....####",
            ".................^^..#########.....####.....####",
            "########.......###############.....####.....####",
            "########.......###############.....####.....####",
            "########.......###############.....####.....####"
        ],
        [
            "................................................",
            ".........................b......................",
            "................................................",
            "................................................",
            "............................##..................",
            ".........====.###...........##..................",
            "..............###...........##..................",
            "..............###.CC.....S..##..................",
            "..............###....######.##..................",
            "..............###....######.##..........b.......",
            "..............###....######.##........====......",
            "..............###....######.##..................",
            ".......S......###....######.##....^^^........G..",
            "########......###....######.####################",
            "########^^^^^^###....######.####################",
            "#################....######.####################"
        ],
        [
            "################################################",
            "################################################",
            "..............vvvvvv........vvvvvv..............",
            "................................................",
            "..........................................b.....",
            ".................b..............................",
            "..................................D.............",
            ".............................................G..",
            "...............................D.....###########",
            "................##.......CCC.........###########",
            "................##...................###########",
            "...........##...##...##..............###########",
            "#######....##...##...##..............###########",
            "#######....##...##...##..............###########",
            "#######^^^^##^^^##^^^##^^^^^^^^^^^^^^###########",
            "################################################"
        ],
        [
            "################################################",
            "################################################",
            "......I..I.......I.I.I.......I.I......I....I....",
            "................................................",
            "................................................",
            "................................................",
            "........................................b.......",
            "................................................",
            ".........................b......................",
            ".............b....................CC.###........",
            ".....................................###.....G..",
            "...........................######....###########",
            "...............########....######....###########",
            "############...########....######....###########",
            "############...########^^^^######....###########",
            "############...##################....###########"
        ],
        [
            "################################################",
            "################################################",
            "........vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv.......",
            "................................................",
            "................................................",
            ".............................b..................",
            "................................................",
            "................................................",
            "...........D...........D..........D...D.........",
            ".................b...........................G..",
            "..........................................######",
            "######...........##.........##............######",
            "######...........##.........##............######",
            "######...........##.........##............######",
            "######^^^^^^^^^^^##^^^^^^^^^##^^^^^^^^^^^^######",
            "################################################"
        ],
        [
            "................................................",
            "................................................",
            ".............................b..................",
            "...................................b............",
            "...........................====.................",
            "................................................",
            ".................................CC.............",
            "....................................CC..........",
            ".......................................C........",
            "............b................................G..",
            "........................S................#######",
            "....................#####................#######",
            ".................^..#####................#######",
            "##################..#####................#######",
            "##################..#####................#######",
            "##################..#####................#######"
        ],
        [
            "...................................b............",
            "................................................",
            ".................................####...........",
            "........b.......D...............................",
            "...........##.......CCC...............CC........",
            "...........##.............................CC..G.",
            "......====.##...........###..................###",
            "...........##...........###..................###",
            "...........##...........###...S..............###",
            "...........##...........###..###.............###",
            "...........##...........###..###.............###",
            "...........##...........###..###.............###",
            "....S......##...........###..###.............###",
            "#####......##...........###..###.............###",
            "#####^^^^^^##^^^^^^^^^^^###^^###^^^^^^^^^^^^^###",
            "################################################"
        ],
        [
            "################################",
            "................................",
            "................................",
            "................................",
            "................................",
            "................................",
            "................................",
            "................................",
            "................................",
            "...====..................====...",
            "................................",
            "................................",
            "................................",
            "################################",
            "################################",
            "################################"
        ]
    ]

    const SPIKES_DOWN = SPIKES.clone()
    SPIKES_DOWN.flipY()
    const ICICLE = img`
        1 1 1 1 1 1 1 .
        . 1 9 9 9 1 . .
        . 1 9 9 1 . . .
        . . 1 9 1 . . .
        . . 1 9 . . . .
        . . . 1 . . . .
        . . . 1 . . . .
        . . . . . . . .
    `
    const LEDGE = img`
        e e e e e e e e
        4 4 4 4 4 4 4 4
        e e e e e e e e
        . f . . . . f .
        . . . . . . . .
        . . . . . . . .
        . . . . . . . .
        . . . . . . . .
    `

    // ---------------------------------------------------------------- levels
    // # ground  ^ v spikes  I icicle  S spring  C crumbling block  = ledge (jump up through it)
    // D ember  b coin  G the shard (end of the level)

    const NAMES = ["1. THE MEADOW", "2. WINDY RIDGE", "3. SPRING WOODS", "4. FROZEN CAVES", "5. ICE FALLS", "6. CRYSTAL HOLLOW", "7. THE GLACIER", "8. THE SUMMIT", "9. THE YETI"]
    const STARTS = [[2, 13], [2, 13], [2, 13], [2, 12], [2, 13], [2, 11], [2, 13], [1, 13], [3, 13]]
    const THEMES = [0, 0, 0, 4, 2, 4, 2, 3, 3]
    const SKIES = [9, 9, 4, 15, 15, 12, 15, 11, 15]
    const BOSS = 8

    const STORY: string[][] = [
        ["LONG AGO THE EMERALD HEART KEPT THE MOUNTAIN GREEN AND WARM.",
            "THIS YEAR SPRING NEVER CAME. THE RIVERS FROZE. THE FIELDS WENT GREY.",
            "BRAM: PIP! THE HEART HAS CRACKED. I SAW NINE GREEN LIGHTS FLY UP THE MOUNTAIN.",
            "BRAM: MY KNEES ARE TOO OLD FOR CLIMBING. TAKE MY RED SCARF. NOW GO!",
            "LEFT / RIGHT TO RUN. A TO JUMP - HOLD IT TO JUMP HIGHER."],
        ["THE FIRST SHARD IS WARM IN PIP'S HANDS. EIGHT MORE TO GO.",
            "THE GAPS UP HERE ARE TOO WIDE. PRESS B TO DASH - AIM WITH THE D-PAD.",
            "ONE DASH IN THE AIR. LAND, OR TOUCH AN EMBER, TO GET IT BACK."],
        ["THE WOODS ARE QUIET. TOO QUIET.",
            "RED SPRINGS THROW YOU HIGH. JUMP AT A WALL IN THE AIR TO KICK OFF IT.",
            "SANDY BLOCKS CRUMBLE WHEN YOU STAND ON THEM. DON'T WAIT AROUND."],
        ["THE PATH DIVES UNDER THE ICE.",
            "A SNOWY OWL DROPS DOWN. WREN: A CHILD? UP HERE? YOU MUST BE AFTER THE SHARDS.",
            "WREN: MIND THE CEILING. THE ICE HAS TEETH."],
        ["WREN: THE ICE FALLS. THE FLOOR IS AS SLICK AS GLASS.",
            "WREN: YOU WILL SLIDE. START RUNNING EARLY. STOP EARLY."],
        ["WREN: THE CRYSTAL HOLLOW. NO FLOOR HERE. ONLY SPIKES.",
            "WREN: FLY FROM EMBER TO EMBER. EVERY EMBER GIVES YOUR DASH BACK.",
            "WREN: DO NOT LOOK DOWN."],
        ["WREN: PIP. THE HEART DID NOT CRACK BY ITSELF.",
            "WREN: THE MOUNTAIN WAS LONELY. NOBODY EVER CLIMBED UP TO SAY THANK YOU.",
            "PIP: THEN I WILL GO ALL THE WAY UP. AND I WILL SAY IT."],
        ["THE SUMMIT. THE WIND IS LOUD AND THE SNOW IS DEEP.",
            "WREN: I CANNOT FLY IN THIS WIND. FROM HERE YOU GO ALONE."],
        ["THE FOG MOVES. TWO RED EYES OPEN IN THE WHITE.",
            "A YETI - AND IN ITS FOREHEAD, THE LAST SHARD!",
            "WHEN IT SLAMS DOWN IT GETS DIZZY. DASH INTO IT OR STOMP ON IT THEN. JUMP THE SHOCKWAVES!"]
    ]

    // ---------------------------------------------------------------- state

    let level = 0
    let grid: string[] = []
    let lw = 48
    let world: Image = null
    let worldSprite: Sprite = null
    let sky = 9
    let px = 0
    let py = 0
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
    let won = 0
    let steps = 0
    let deaths = 0
    let shards = 0
    let playing = false
    let pip: Sprite = null
    let spark: Sprite = null
    let scarf: Sprite[] = []
    let things: Sprite[] = []
    let icicles: Sprite[] = []
    let crumbles: number[][] = []
    let taken: string[] = []
    let last = 0
    let acc = 0

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
        if (cx < 0 || cx >= lw) return "#"
        if (cy < 0 || cy >= 16) return "."
        return grid[cy].charAt(cx)
    }

    function solidAt(x: number, y: number): boolean {
        const c = cellAt(x, y)
        return c == "#" || c == "C"
    }

    // Pip's body: 6 wide, 12 tall, from (x, y)
    function boxSolid(x: number, y: number): boolean {
        return solidAt(x, y) || solidAt(x + 5, y) || solidAt(x, y + 6) || solidAt(x + 5, y + 6) || solidAt(x, y + 11) || solidAt(x + 5, y + 11)
    }

    function setCell(cx: number, cy: number, ch: string) {
        grid[cy] = grid[cy].substr(0, cx) + ch + grid[cy].substr(cx + 1)
    }

    function tileFor(cx: number, cy: number): Image {
        const theme = THEMES[level]
        const isTop = cy == 0 || grid[cy - 1].charAt(cx) != "#"
        if (theme == 0) return isTop ? GRASS : DIRT
        if (theme == 1) return isTop ? MOSS : STONE
        if (theme == 2 || theme == 4) return isTop ? ICE_TOP : ICE   // 4: icy caves (looks like ice, not slippery)
        return isTop ? SNOW : STONE
    }

    function drawWorld() {
        world = image.create(lw * T, 128)
        const theme = THEMES[level]
        // far hills (or stars in the dark levels)
        for (let i = 0; i < lw / 3; i++) {
            if (sky == 15 || sky == 12) world.setPixel(randint(0, lw * T), randint(0, 90), 13)
            else world.fillCircle(i * 26 + 8, 134, 24, theme == 2 ? 9 : (theme == 3 ? 1 : 6))
        }
        for (let cy = 0; cy < 16; cy++) {
            for (let cx = 0; cx < lw; cx++) {
                const c = grid[cy].charAt(cx)
                const x = cx * T
                const y = cy * T
                if (c == "#") {
                    world.drawImage(tileFor(cx, cy), x, y)
                    if (cy > 0 && grid[cy - 1].charAt(cx) == "." && Math.percentChance(25)) {
                        const deco = theme == 0 ? (Math.percentChance(50) ? FLOWER : TUFT) : CRYSTAL
                        world.drawTransparentImage(deco, x, y - T)
                    }
                }
                if (c == "^") world.drawTransparentImage(SPIKES, x, y)
                if (c == "v") world.drawTransparentImage(SPIKES_DOWN, x, y)
                if (c == "S") world.drawTransparentImage(SPRING, x, y)
                if (c == "C") world.drawTransparentImage(CRUMBLE, x, y)
                if (c == "=") world.drawTransparentImage(LEDGE, x, y)
            }
        }
        if (worldSprite) worldSprite.destroy()
        worldSprite = sprites.create(world, SpriteKind.Food)
        worldSprite.setFlag(SpriteFlag.Ghost, true)
        worldSprite.left = 0
        worldSprite.top = 0
        worldSprite.z = -10
    }

    // ---------------------------------------------------------------- levels

    function clearThings() {
        for (const s of things) s.destroy()
        things = []
        for (const b of balls) b.destroy()
        balls = []
        for (const oldIcicle of icicles) oldIcicle.destroy()
        icicles = []
        for (const w of waves) w.destroy()
        waves = []
        if (yeti) yeti.destroy()
        yeti = null
        if (shardDrop) shardDrop.destroy()
        shardDrop = null
    }

    function startLevel() {
        clearThings()
        crumbles = []
        grid = []
        for (const row of LEVELS[level]) grid.push(row)
        lw = grid[0].length
        sky = SKIES[level]
        scene.setBackgroundColor(sky)
        for (let cy = 0; cy < 16; cy++) {
            for (let cx = 0; cx < lw; cx++) {
                const c = grid[cy].charAt(cx)
                if (c == "I") {
                    // an icicle that falls when Pip walks under it
                    setCell(cx, cy, ".")
                    const ic = sprites.create(ICICLE, SpriteKind.Projectile)
                    ic.left = cx * T
                    ic.top = cy * T
                    ic.data["state"] = 0
                    ic.data["t"] = 0
                    ic.data["home"] = ic.x
                    icicles.push(ic)
                } else if (c == "D" || c == "b" || c == "G") {
                    const key = level + ":" + cx + ":" + cy
                    setCell(cx, cy, ".")
                    if (c == "b" && taken.indexOf(key) >= 0) continue
                    const s = sprites.create(c == "D" ? EMBER : (c == "b" ? COIN : SHARD), SpriteKind.Food)
                    s.left = cx * T
                    s.top = cy * T
                    s.data["kind"] = c
                    s.data["wait"] = 0
                    s.data["key"] = key
                    if (c == "G") s.startEffect(effects.halo)
                    things.push(s)
                }
            }
        }
        drawWorld()
        px = STARTS[level][0] * T + 1
        py = STARTS[level][1] * T - 12
        vx = 0
        vy = 0
        dashTime = 0
        canDash = true
        dead = 0
        won = 0
        pip.setFlag(SpriteFlag.Invisible, false)
        for (const s of scarf) {
            s.x = px
            s.y = py + 6
        }
        if (level == BOSS && yHits < 3) startYeti()
    }

    function showStory() {
        for (const line of STORY[level]) game.showLongText(line, DialogLayout.Bottom)
    }

    function nextLevel() {
        level++
        startLevel()
        game.splash(NAMES[level])
        showStory()
        last = game.runtime()
    }

    function die() {
        if (dead > 0 || won > 0) return
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

    function moveX(amount: number) {
        const nx = px + amount
        if (!boxSolid(nx, py)) {
            px = nx
            return
        }
        const dir = amount > 0 ? 1 : -1
        let guard = 0
        while (!boxSolid(px + dir, py) && guard < 8) {
            px += dir
            guard++
        }
        vx = 0
    }

    function moveY(amount: number) {
        // ledges hold Pip only when coming down onto them (hold down to drop through)
        if (amount > 0 && !controller.down.isPressed()) {
            const feet = py + 12
            const after = feet + amount
            const ledgeTop = Math.floor(after / 8) * 8
            if (feet <= ledgeTop && after >= ledgeTop && (cellAt(px, after) == "=" || cellAt(px + 5, after) == "=")) {
                py = ledgeTop - 12
                vy = 0
                return
            }
        }
        const ny = py + amount
        if (!boxSolid(px, ny)) {
            py = ny
            return
        }
        const dir = amount > 0 ? 1 : -1
        let guard = 0
        while (!boxSolid(px, py + dir) && guard < 8) {
            py += dir
            guard++
        }
        if (dir > 0) py = Math.floor((py + 13) / 8) * 8 - 12
        vy = 0
    }

    function onLedge(): boolean {
        const feet = py + 12
        if (feet - Math.floor(feet / 8) * 8 > 1) return false
        return cellAt(px, feet) == "=" || cellAt(px + 5, feet) == "="
    }

    function crumbleUnder() {
        for (const x of [px + 1, px + 4]) {
            if (cellAt(x, py + 12) == "C") {
                const cx = Math.floor(x / T)
                const cy = Math.floor((py + 12) / T)
                let known = false
                for (const cr of crumbles) if (cr[0] == cx && cr[1] == cy && cr[2] > 0) known = true
                if (!known) crumbles.push([cx, cy, 18, 0])
            }
        }
    }

    function updateCrumbles() {
        for (const cr of crumbles) {
            if (cr[2] > 0) {
                cr[2]--
                if (cr[2] == 0) {
                    setCell(cr[0], cr[1], ".")
                    world.fillRect(cr[0] * T, cr[1] * T, T, T, 0)
                    cr[3] = 140
                }
            } else if (cr[3] > 0) {
                cr[3]--
                if (cr[3] == 0) {
                    if (Math.abs(cr[0] * T + 4 - (px + 3)) < 8 && Math.abs(cr[1] * T + 4 - (py + 6)) < 11) cr[3] = 10
                    else {
                        setCell(cr[0], cr[1], "C")
                        world.drawTransparentImage(CRUMBLE, cr[0] * T, cr[1] * T)
                    }
                }
            }
        }
    }

    function touches(ch: string): boolean {
        return cellAt(px + 1, py + 2) == ch || cellAt(px + 4, py + 2) == ch || cellAt(px + 1, py + 10) == ch || cellAt(px + 4, py + 10) == ch || cellAt(px + 2, py + 6) == ch
    }

    function near(s: Sprite, r: number): boolean {
        return Math.abs(s.x - (px + 3)) < r && Math.abs(s.y - (py + 6)) < r + 3
    }

    function step() {
        updateCrumbles()
        if (won > 0) {
            won--
            if (won == 0) nextLevel()
            return
        }
        if (dead > 0) {
            dead--
            if (dead == 0) startLevel()
            return
        }
        steps++
        let dx = 0
        if (controller.left.isPressed()) dx = -1
        if (controller.right.isPressed()) dx = 1
        const jumpHeld = controller.A.isPressed()

        grounded = vy >= 0 && (solidAt(px, py + 12) || solidAt(px + 5, py + 12) || onLedge())
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

        if (jumpBuffer > 0 && dashTime == 0) {
            if (coyote > 0) {
                vy = -3.2
                jumpBuffer = 0
                coyote = 0
                music.thump.play()
            } else if (wall != 0) {
                vy = -3.0
                vx = -wall * 2.2
                facing = -wall
                wallLock = 10
                jumpBuffer = 0
                music.thump.play()
            }
        }
        if (jumpBuffer > 0) jumpBuffer--

        if (dashAsked) {
            dashAsked = false
            if (canDash) {
                let ddx = dx
                let ddy = 0
                if (controller.up.isPressed()) ddy = -1
                if (controller.down.isPressed()) ddy = 1
                if (ddx == 0 && ddy == 0) ddx = facing
                const speed = ddx != 0 && ddy != 0 ? 2.6 : 3.4
                vx = ddx * speed
                vy = ddy * speed
                if (ddx != 0) facing = ddx
                dashTime = 9
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
                const under = cellAt(px + 3, py + 12)
                if (grounded && THEMES[level] == 2 && under == "#") grip = 0.035   // ice: slow to start, slow to stop
                vx += (dx * 1.4 - vx) * grip
            }
            let g = 0.2
            if (Math.abs(vy) < 0.6 && jumpHeld) g = 0.1
            if (vy < 0 && !jumpHeld) g = 0.4
            vy += g
            if (wall != 0 && dx == wall && vy > 0.8) vy = 0.8
            if (vy > 3.6) vy = 3.6
        }

        moveX(vx)
        moveY(vy)

        updateIcicles()
        if (dead > 0) return
        if (touches("^") || touches("v") || py > 130) {
            die()
            return
        }
        if (vy >= 0 && touches("S")) {
            vy = -5.4
            dashTime = 0
            canDash = true
            coyote = 0
            music.jumpUp.play()
        }
        for (const s of things) {
            const wait = s.data["wait"] as number
            if (wait > 0) {
                s.data["wait"] = wait - 1
                if (wait == 1) s.setFlag(SpriteFlag.Invisible, false)
                continue
            }
            if (s.flags & SpriteFlag.Invisible) continue
            if (!near(s, 6)) continue
            const kind = s.data["kind"] as string
            if (kind == "b") {
                info.changeScoreBy(1)
                taken.push(s.data["key"] as string)
                music.baDing.play()
                s.setFlag(SpriteFlag.Invisible, true)
                s.data["wait"] = -1
            } else if (kind == "D" && !canDash) {
                canDash = true
                music.magicWand.play()
                s.setFlag(SpriteFlag.Invisible, true)
                s.data["wait"] = 150
            } else if (kind == "G") {
                shards++
                music.powerUp.play()
                scene.cameraShake(3, 300)
                s.setFlag(SpriteFlag.Invisible, true)
                s.data["wait"] = -1
                won = 60
            }
        }
        if (level == BOSS) updateYeti()
    }

    // icicles shake when Pip passes under, then drop; they shatter on the ground
    function updateIcicles() {
        for (const ic of icicles) {
            const state = ic.data["state"] as number
            if (state == 0) {
                if (px + 6 > ic.left - 6 && px < ic.right + 6 && py > ic.top) {
                    ic.data["state"] = 1
                    ic.data["t"] = 14
                }
            } else if (state == 1) {
                ic.x = (ic.data["home"] as number) + randint(-1, 1)
                ic.data["t"] = (ic.data["t"] as number) - 1
                if ((ic.data["t"] as number) <= 0) {
                    ic.x = ic.data["home"] as number
                    ic.ay = 400
                    ic.data["state"] = 2
                }
            } else if (state == 2) {
                if (Math.abs(ic.x - (px + 3)) < 6 && ic.bottom > py && ic.top < py + 12) die()
                if (solidAt(ic.x, ic.bottom) || ic.top > 128) {
                    ic.data["state"] = 3
                    ic.ay = 0
                    ic.vy = 0
                    ic.startEffect(effects.disintegrate, 200)
                    ic.lifespan = 200
                    music.smallCrash.play()
                }
            }
        }
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
        pip.bottom = py + 12
        let hx = px + 3 - facing * 4
        let hy = py + 6
        for (const s of scarf) {
            s.x += (hx - s.x) * 0.5
            s.y += (hy - s.y) * 0.5
            hx = s.x - facing * 1.5
            hy = s.y + 0.4
            s.setFlag(SpriteFlag.Invisible, dead > 0)
        }
        const tip = scarf[scarf.length - 1]
        spark.x = tip.x + randint(-1, 1)
        spark.y = tip.y - 2
        spark.image.setPixel(0, 0, Math.percentChance(50) ? 4 : 5)
        spark.setFlag(SpriteFlag.Invisible, !canDash || dashTime > 0 || dead > 0)
        // the camera follows Pip
        scene.centerCameraAt(Math.constrain(px + 3, 80, lw * T - 80), Math.constrain(py + 6, 60, 68))
    }

    // ---------------------------------------------------------------- the yeti

    function floorTop(): number {
        return 13 * T - 16
    }

    function startYeti() {
        yeti = sprites.create(YETI, SpriteKind.Enemy)
        yeti.left = 180
        yeti.top = floorTop()
        yState = "walk"
        yTimer = 80
        yCount = 0
    }

    function overlapsYeti(): boolean {
        return px + 6 > yeti.left + 2 && px < yeti.right - 2 && py + 12 > yeti.top + 2 && py < yeti.bottom
    }

    function updateYeti() {
        for (const b of balls) {
            if (near(b, 5)) die()
            if (b.bottom > 13 * T) b.destroy()
        }
        balls = balls.filter(b => !(b.flags & sprites.Flag.Destroyed))
        for (const w of waves) {
            if (near(w, 5)) die()
            if (w.left < 8 || w.right > lw * T - 8) w.destroy()
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
            yeti.x += (target < yeti.x ? -1 : 1) * 0.4
            if (yTimer <= 0) {
                yCount++
                yState = yCount % 3 == 0 ? "throw" : "crouch"
                yTimer = 30
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
            if (yeti.top >= floorTop()) {
                yeti.top = floorTop()
                land()
            }
        } else if (yState == "throw") {
            if (yTimer <= 0) {
                const b = sprites.create(SNOWBALL, SpriteKind.Projectile)
                b.x = yeti.x
                b.y = yeti.top
                b.vx = (target - yeti.x) / 0.9
                b.vy = -90
                b.ay = 200
                balls.push(b)
                music.pewPew.play()
                yState = "walk"
                yTimer = 70
            }
        } else if (yState == "stun") {
            if (yTimer % 10 == 0) yeti.startEffect(effects.halo, 200)
            if (overlapsYeti() && (dashTime > 0 || vy > 0)) {
                yHits++
                music.smallCrash.play()
                scene.cameraShake(6, 300)
                vy = -3.5
                vx = px + 3 < yeti.x ? -2 : 2
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
        yeti.left = Math.constrain(yeti.left, 8, lw * T - 24)
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
        shards++
        music.powerUp.play()
        game.showLongText("THE YETI BLINKS. ITS EYES ARE GENTLE NOW. IT LOOKS CONFUSED, AND VERY FLUFFY.", DialogLayout.Bottom)
        game.showLongText("THE NINE SHARDS CLICK TOGETHER IN PIP'S HANDS. THE HEART BEATS ONCE.", DialogLayout.Bottom)
        game.showLongText("PIP: THANK YOU, MOUNTAIN. FOR EVERY SPRING.", DialogLayout.Bottom)
        game.showLongText("WARMTH ROLLS DOWN THE SLOPES. THE RIVERS START TO RUN. FAR BELOW, BRAM LAUGHS.", DialogLayout.Bottom)
        game.showLongText("AND LATER... PIP AND HIS FRIEND BRAE SPENT THE WARM DAYS BY THE WATERFALL.", DialogLayout.Center)
        
        game.showLongText("THE END.    COINS: " + info.score() + " / 19    FALLS: " + deaths, DialogLayout.Center)
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
        const now = game.runtime()
        acc += Math.min(now - last, 100)
        last = now
        while (acc >= 16) {
            step()
            acc -= 16
        }
        drawPip()
    })

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

    game.splash("SHARD", "PIP AND THE EMERALD HEART")
    startLevel()
    game.splash(NAMES[0])
    showStory()
    last = game.runtime()
    playing = true
}
