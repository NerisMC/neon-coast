// Pip's Party Palace for Minecraft Bedrock (phone, tablet, console, PC)
// Night shifts, security cameras, power, doors, jumpscares, possession, the Register, the Franchise Catalog
// and the Pizzeria Kit. Uses only stable script APIs (@minecraft/server 2.0.0, @minecraft/server-ui 2.0.0).
import { world, system, BlockPermutation, BlockVolume, ItemStack } from "@minecraft/server";
import { ActionFormData } from "@minecraft/server-ui";

const NS = "pipsparty";
const BAND = ["pip", "rusty", "marla", "gnash"];
const NAMES = { pip: "Pip the Raccoon", rusty: "Rusty the Robot Dog", marla: "Marla the Mole", gnash: "Gnash the Gator" };
const DOOR = { pip: -1, rusty: 0, marla: 1, gnash: 0 };
const AI = { pip: [0, 1, 3, 6, 10], rusty: [2, 4, 7, 10, 14], marla: [1, 4, 7, 11, 15], gnash: [0, 3, 6, 9, 13] };
const TICKS_PER_HOUR = 900;          // 45 seconds an in-game hour
const DRAIN = 0.0052;                 // power per tick per usage bar
const MODE = { STAGE: 0, ROAM: 1, HUNT: 2, SCARE: 3, SHIFT: 4 };

// ------------------------------------------------------------------ helpers
const isBand = (e) => e && e.isValid && e.typeId.startsWith(NS + ":") && BAND.includes(e.typeId.slice(NS.length + 1));
const who = (e) => e.typeId.slice(NS.length + 1);
const creative = (p) => String(p.getGameMode()).toLowerCase() === "creative";
const spectator = (p) => String(p.getGameMode()).toLowerCase() === "spectator";
const key = (dim, l) => `${dim.id}|${Math.floor(l.x)},${Math.floor(l.y)},${Math.floor(l.z)}`;
const dist2 = (a, b) => (a.x - b.x) ** 2 + (a.y - b.y) ** 2 + (a.z - b.z) ** 2;
const center = (b) => ({ x: b.x + 0.5, y: b.y, z: b.z + 0.5 });
const isNight = () => { const t = world.getTimeOfDay(); return t >= 12800 && t < 23200; };
const yawDir = (yaw) => ({ x: -Math.sin(yaw * Math.PI / 180), z: Math.cos(yaw * Math.PI / 180) });

function say(p, msg) { try { p.sendMessage(msg); } catch { } }
function title(p, t, sub) {
    try { p.onScreenDisplay.setTitle(t, { subtitle: sub ?? "", fadeInDuration: 5, stayDuration: 50, fadeOutDuration: 15 }); } catch { }
}
function sound(dim, id, loc, volume = 1, pitch = 1) { try { dim.playSound(`${NS}.${id}`, loc, { volume, pitch }); } catch { } }
function block(dim, l) { try { return dim.getBlock(l); } catch { return undefined; } }
function setMode(e, m) { try { if (e.getProperty(`${NS}:mode`) !== m) e.setProperty(`${NS}:mode`, m); } catch { } }
function fire(e, ev) { try { e.triggerEvent(`${NS}:${ev}`); } catch { } }

function inventory(p) { return p.getComponent("minecraft:inventory")?.container; }
function countItem(p, id) {
    const inv = inventory(p); let n = 0;
    if (!inv) return 0;
    for (let i = 0; i < inv.size; i++) { const s = inv.getItem(i); if (s && s.typeId === id) n += s.amount; }
    return n;
}
function takeItem(p, id, n) {
    const inv = inventory(p);
    for (let i = 0; i < inv.size && n > 0; i++) {
        const s = inv.getItem(i);
        if (!s || s.typeId !== id) continue;
        const take = Math.min(n, s.amount);
        n -= take;
        if (take === s.amount) inv.setItem(i, undefined); else { s.amount -= take; inv.setItem(i, s); }
    }
}
function give(p, id, n) {
    const inv = inventory(p);
    while (n > 0) {
        const stack = new ItemStack(id, 1);
        const amt = Math.min(n, stack.maxAmount);
        stack.amount = amt;
        n -= amt;
        const left = inv ? inv.addItem(stack) : stack;
        if (left) p.dimension.spawnItem(left, p.location);
    }
}
function useUp(p, id) { if (!creative(p)) takeItem(p, id, 1); }

// ------------------------------------------------------------------ security cameras (tablet)
const view = new Map();   // player id -> { idx, cam, debounce }

function camerasNear(p) {
    const cams = p.dimension.getEntities({ type: `${NS}:security_camera`, location: p.location, maxDistance: 64 });
    cams.sort((a, b) => (a.nameTag || "").localeCompare(b.nameTag || "", undefined, { numeric: true }));
    return cams;
}
function watching(p) { const v = view.get(p.id); return !!(v && v.cam); }

function nextCamera(p, step = 1) {
    const v = view.get(p.id) ?? { idx: -1, cam: undefined, debounce: 0 };
    if (system.currentTick < v.debounce) return;
    v.debounce = system.currentTick + 6;
    view.set(p.id, v);
    if (p.isSneaking) { exitCameras(p); return; }
    const cams = camerasNear(p);
    if (!cams.length) { p.onScreenDisplay.setActionBar("§cNO SIGNAL §7- place Security Cameras nearby"); return; }
    v.idx = (v.idx + step + cams.length) % cams.length;
    const cam = cams[v.idx];
    v.cam = cam.id;
    const r = cam.getRotation();
    try {
        p.camera.setCamera("minecraft:free", { location: { x: cam.location.x, y: cam.location.y + 0.1, z: cam.location.z }, rotation: { x: r.x, y: r.y } });
        p.camera.fade({ fadeColor: { red: 0.75, green: 0.8, blue: 0.75 }, fadeTime: { fadeInTime: 0.05, holdTime: 0.05, fadeOutTime: 0.3 } });
    } catch { }
    try { p.playSound(`${NS}.camera_flip`, { volume: 0.7 }); } catch { }
    showCamLabel(p, cam, v.idx, cams.length);
}
function showCamLabel(p, cam, i, n) {
    p.onScreenDisplay.setActionBar(`§c● REC §f${cam.nameTag || "CAM"}  §7(${i + 1}/${n})  tap tablet: next · sneak + tap: exit`);
}
function exitCameras(p) {
    const v = view.get(p.id);
    if (v) v.cam = undefined;
    try { p.camera.clear(); } catch { }
}

// ------------------------------------------------------------------ jumpscares
const scareCooldown = new Map();   // animatronic id -> tick
const scaring = new Set();         // player ids mid-scare

function jumpscare(bot, p, kills) {
    if (!bot.isValid || !p.isValid || creative(p) || spectator(p) || scaring.has(p.id)) return;
    if ((scareCooldown.get(bot.id) ?? 0) > system.currentTick) return;
    scareCooldown.set(bot.id, system.currentTick + 140);
    scaring.add(p.id);
    exitCameras(p);
    setMode(bot, MODE.SCARE);
    const d = yawDir(p.getRotation().y);
    const spot = { x: p.location.x + d.x * 1.4, y: p.location.y, z: p.location.z + d.z * 1.4 };
    try { bot.teleport(spot, { facingLocation: p.location }); } catch { }
    try {
        p.camera.setCamera("minecraft:free", {
            location: { x: p.location.x, y: p.location.y + 1.62, z: p.location.z },
            facingLocation: { x: spot.x, y: p.location.y + 1.95, z: spot.z },
        });
    } catch { }
    try { p.addEffect("slowness", 40, { amplifier: 9, showParticles: false }); } catch { }
    sound(p.dimension, "jumpscare", p.location, 2.0, 0.9 + Math.random() * 0.2);
    system.runTimeout(() => {
        try { p.camera.fade({ fadeColor: { red: 0.5, green: 0, blue: 0 }, fadeTime: { fadeInTime: 0.1, holdTime: 0.4, fadeOutTime: 0.8 } }); } catch { }
    }, 18);
    system.runTimeout(() => {
        scaring.delete(p.id);
        try { p.camera.clear(); } catch { }
        if (!p.isValid) return;
        try { p.addEffect("darkness", 60, { showParticles: false }); } catch { }
        try { p.applyDamage(kills ? 1000 : 12, { cause: "entityAttack", damagingEntity: bot.isValid ? bot : undefined }); } catch { }
        if (bot.isValid) setMode(bot, bot.getDynamicProperty("shift") ? MODE.SHIFT : isNight() ? MODE.HUNT : MODE.STAGE);
        if (kills && bot.isValid) onGuardCaught(p, bot);
    }, 28);
}

world.afterEvents.entityHurt.subscribe((e) => {
    const src = e.damageSource?.damagingEntity;
    if (isBand(src) && e.hurtEntity.typeId === "minecraft:player") jumpscare(src, e.hurtEntity, isGuardOnShift(e.hurtEntity));
});

// ------------------------------------------------------------------ animatronic setup and free roam
function home(bot) {
    const h = bot.getDynamicProperty("home");
    if (typeof h !== "string") return undefined;
    const [x, y, z, yaw] = h.split(",").map(Number);
    return { x, y, z, yaw };
}
function setHome(bot, loc, yaw) {
    bot.setDynamicProperty("home", `${Math.floor(loc.x) + 0.5},${Math.floor(loc.y)},${Math.floor(loc.z) + 0.5},${yaw}`);
}
function returnHome(bot) {
    const h = home(bot);
    if (h) try { bot.teleport({ x: h.x, y: h.y, z: h.z }, { rotation: { x: 0, y: h.yaw } }); } catch { }
}

world.afterEvents.entitySpawn.subscribe(({ entity }) => {
    if (!entity.isValid) return;
    if (isBand(entity)) {
        if (!home(entity)) setHome(entity, entity.location, entity.getRotation().y);
        if (isNight()) { fire(entity, "night"); setMode(entity, MODE.HUNT); } else setMode(entity, MODE.STAGE);
    } else if (entity.typeId === `${NS}:security_camera`) {
        if (!entity.nameTag) {
            const n = entity.dimension.getEntities({ type: `${NS}:security_camera`, location: entity.location, maxDistance: 64 }).length;
            entity.nameTag = `CAM ${n}`;
        }
        const p = entity.dimension.getPlayers({ location: entity.location, maxDistance: 8, closest: 1 })[0];
        if (p) entity.setRotation({ x: 25, y: p.getRotation().y });
    }
});

const allBand = () => {
    const out = [];
    for (const id of ["minecraft:overworld", "minecraft:nether", "minecraft:the_end"]) {
        try { out.push(...world.getDimension(id).getEntities({ families: ["animatronic"] })); } catch { }
    }
    return out;
};

let wasNight;
system.runInterval(() => {
    const night = isNight();
    const flip = night !== wasNight;
    wasNight = night;
    for (const bot of allBand()) {
        if (bot.getDynamicProperty("shift") || possessed.has(bot.id)) continue;
        if (night) {
            if (flip || bot.getProperty(`${NS}:mode`) === MODE.STAGE) { fire(bot, "night"); setMode(bot, MODE.HUNT); }
        } else if (flip || bot.getProperty(`${NS}:mode`) !== MODE.STAGE) {
            fire(bot, "day");
            returnHome(bot);
            setMode(bot, MODE.STAGE);
        }
        // the show must go on: play the band tune when someone's watching the stage in the daytime
        if (!night && who(bot) === "pip" && system.currentTick % 200 === 0 &&
            bot.dimension.getPlayers({ location: bot.location, maxDistance: 20 }).length) {
            sound(bot.dimension, "stage_band", bot.location, 0.8);
        }
    }
}, 20);

// Pip only moves while nobody's looking; everyone else lunges when close
system.runInterval(() => {
    for (const bot of allBand()) {
        const mode = bot.getProperty(`${NS}:mode`);
        if (mode !== MODE.HUNT || bot.getDynamicProperty("shift") || possessed.has(bot.id)) continue;
        const players = bot.dimension.getPlayers({ location: bot.location, maxDistance: 24 });
        if (who(bot) === "pip") {
            let seen = false;
            for (const p of players) {
                try { if (p.getEntitiesFromViewDirection({ maxDistance: 24 }).some((h) => h.entity.id === bot.id)) seen = true; } catch { }
            }
            const frozen = bot.getDynamicProperty("frozen") === true;
            if (seen !== frozen) { fire(bot, seen ? "freeze" : "unfreeze"); bot.setDynamicProperty("frozen", seen); }
            if (seen) continue;
        }
        for (const p of players) {
            if (dist2(p.location, bot.location) < 2.4) jumpscare(bot, p, isGuardOnShift(p));
        }
    }
}, 5);

// ------------------------------------------------------------------ possession (Override Chip)
const possessed = new Map();   // animatronic id -> player id

world.afterEvents.playerInteractWithEntity.subscribe(({ player, target, itemStack }) => {
    if (!isBand(target) || itemStack?.typeId !== `${NS}:override_chip`) return;
    if (target.getDynamicProperty("shift")) { say(player, "§cThat animatronic is under shift control right now."); return; }
    possessed.set(target.id, player.id);
    fire(target, "possess");
    setMode(target, MODE.HUNT);
    system.runTimeout(() => {
        try { target.getComponent("minecraft:rideable").addRider(player); } catch { }
        try { player.addEffect("invisibility", 20 * 60 * 20, { showParticles: false }); } catch { }
        title(player, `§c${NAMES[who(target)]}`, "Hunt the night guard. Sneak to bail out.");
    }, 2);
});

system.runInterval(() => {
    for (const [botId, playerId] of [...possessed]) {
        const bot = world.getEntity(botId);
        const p = world.getEntity(playerId);
        const riders = bot?.isValid ? (bot.getComponent("minecraft:rideable")?.getRiders() ?? []) : [];
        if (!bot?.isValid || !p?.isValid || !riders.some((r) => r.id === playerId)) {
            possessed.delete(botId);
            if (bot?.isValid) { fire(bot, "release"); if (isNight()) fire(bot, "night"); }
            try { p?.removeEffect("invisibility"); } catch { }
            continue;
        }
        p.onScreenDisplay.setActionBar(`§cYou are ${NAMES[who(bot)]} §7- touch someone to scare them`);
        for (const victim of bot.dimension.getPlayers({ location: bot.location, maxDistance: 1.8 })) {
            if (victim.id !== playerId) jumpscare(bot, victim, isGuardOnShift(victim));
        }
    }
}, 4);

// ------------------------------------------------------------------ night shifts
const shifts = new Map();   // clock key -> shift

function nightOf(p) { return (p.getDynamicProperty("night") ?? 1); }
function isGuardOnShift(p) { for (const s of shifts.values()) if (s.guard === p.id) return true; return false; }
function powerOutNear(dim, loc) {
    for (const s of shifts.values()) if (s.dim.id === dim.id && s.powerOut && dist2(s.clock, loc) < 14 * 14) return true;
    return false;
}

function roomSpot(cam) {
    const d = yawDir(cam.getRotation().y);
    for (let dist = 4; dist >= 1; dist--) {
        const x = Math.floor(cam.location.x + d.x * dist), z = Math.floor(cam.location.z + d.z * dist);
        for (let down = 0; down < 8; down++) {
            const y = Math.floor(cam.location.y) - down;
            const feet = block(cam.dimension, { x, y, z }), head = block(cam.dimension, { x, y: y + 1, z }), floor = block(cam.dimension, { x, y: y - 1, z });
            if (feet?.isAir && head?.isAir && floor && !floor.isAir && !floor.isLiquid) return { x: x + 0.5, y, z: z + 0.5 };
        }
    }
    return { x: cam.location.x, y: cam.location.y - 4, z: cam.location.z };
}

function clockIn(p, clockBlock) {
    const dim = clockBlock.dimension;
    const k = key(dim, clockBlock.location);
    const ex = shifts.get(k);
    if (ex) { say(p, ex.guard === p.id ? "§eYou're on the clock. Survive until 6 AM." : "§cSomeone else is working this shift. Grab an Override Chip and hunt them..."); return; }
    if (isGuardOnShift(p)) { say(p, "You're already on a shift somewhere else."); return; }
    const c = clockBlock.location;
    const s = {
        dim, clock: { x: c.x, y: c.y, z: c.z }, guard: p.id, night: nightOf(p), ticks: 0, power: 100, usage: 1,
        bots: [], rooms: [], doorSpots: [], doors: [], lights: [], bonus: 0, powerOut: false, powerOutTicks: 0, pipStrike: -1,
        gnashMeter: 0, awayTicks: 0, key: k,
    };
    // doors and lights around the office
    const doorBlocks = [];
    for (let x = -10; x <= 10; x++) for (let y = -3; y <= 6; y++) for (let z = -10; z <= 10; z++) {
        const b = block(dim, { x: c.x + x, y: c.y + y, z: c.z + z });
        if (!b) continue;
        if (b.typeId === `${NS}:security_door`) doorBlocks.push(b.location);
        else if (b.typeId === `${NS}:hall_light` && Math.abs(x) <= 12) s.lights.push(b.location);
    }
    for (const d of doorBlocks) {
        let g = s.doors.find((grp) => dist2(grp[0], d) < 2.5 * 2.5);
        if (!g) { g = []; s.doors.push(g); }
        g.push(d);
    }
    s.doors.sort((a, b) => (a[0].x + a[0].z * 3) - (b[0].x + b[0].z * 3));
    for (const g of s.doors) {
        const bottom = g.reduce((m, q) => (q.y < m.y ? q : m), g[0]);
        const dx = bottom.x - c.x, dz = bottom.z - c.z;
        const out = Math.abs(dx) >= Math.abs(dz) ? { x: Math.sign(dx) || 1, z: 0 } : { x: 0, z: Math.sign(dz) || 1 };
        s.doorSpots.push({ x: bottom.x + out.x + 0.5, y: bottom.y, z: bottom.z + out.z + 0.5 });
    }
    if (!s.doorSpots.length) say(p, "§cThis office has no Security Doors within 10 blocks of the Time Clock. Good luck...");
    const cams = dim.getEntities({ type: `${NS}:security_camera`, location: c, maxDistance: 48 });
    s.rooms = cams.map(roomSpot).sort((a, b) => dist2(b, c) - dist2(a, c));
    for (const bot of dim.getEntities({ families: ["animatronic"], location: c, maxDistance: 48 })) {
        if (possessed.has(bot.id)) continue;
        bot.setDynamicProperty("shift", true);
        fire(bot, "day");
        returnHome(bot);
        setMode(bot, MODE.SHIFT);
        const w = who(bot);
        s.bots.push({ id: bot.id, who: w, progress: 0, door: DOOR[w] >= 0 ? DOOR[w] : Math.floor(Math.random() * 2), runTicks: 0 });
    }
    s.bonus = Math.min(4, Math.floor(funRating(dim, c) / 25));
    shifts.set(k, s);
    title(p, `§lNight ${s.night}`, "12 AM");
    say(p, `§dNight ${s.night} at Pip's Party Palace. ${s.bots.length} animatronics, ${cams.length} cameras, ${s.doors.length} doors. Watch your power.`);
    if (s.night === 1) say(p, "§7Tip: the red buttons slam the doors, the white ones flick the hall lights. Tap with the Camera Tablet to flip through cameras (sneak + tap to put it down). Everything uses power.");
}

function doorClosed(s, i) {
    if (i >= s.doors.length) return false;
    const b = block(s.dim, s.doors[i][0]);
    return b?.typeId === `${NS}:security_door` && b.permutation.getState(`${NS}:closed`) === true;
}
function botOf(b) { const e = world.getEntity(b.id); return e?.isValid ? e : undefined; }
function moveBot(bot, to) {
    if (!to) return;
    try { bot.teleport(to, { rotation: { x: 0, y: Math.random() * 360 } }); } catch { }
    sound(bot.dimension, "servo_step", to, 0.6, 0.7);
}

function shiftTick(s) {
    const guard = world.getEntity(s.guard);
    if (!guard?.isValid || guard.dimension.id !== s.dim.id) { endShift(s, false, "The guard is gone. Shift over."); return; }
    if (scaring.has(guard.id)) return;
    if (dist2(guard.location, s.clock) > 16 * 16) {
        if (++s.awayTicks === 1) say(guard, "§cGet back to the office!");
        if (s.awayTicks > 200) { endShift(s, false, "You walked off the job. Shift failed."); return; }
    } else s.awayTicks = 0;

    s.ticks++;
    const hour = Math.floor(s.ticks / TICKS_PER_HOUR);
    if (hour >= 6) { win(s, guard); return; }

    if (s.ticks % 20 === 0) {
        let usage = 1;
        for (let i = 0; i < s.doors.length; i++) if (doorClosed(s, i)) usage++;
        for (const l of s.lights) { const b = block(s.dim, l); if (b?.typeId === `${NS}:hall_light` && b.permutation.getState(`${NS}:lit`)) usage++; }
        if (watching(guard)) usage++;
        s.usage = usage;
    }
    if (!s.powerOut) {
        s.power -= s.usage * DRAIN * (watching(guard) ? 1.25 : 1);
        if (s.power <= 0) powerOut(s);
    } else powerOutTick(s, guard);

    if (!s.powerOut && s.ticks % 100 === 0) moveOpportunity(s, guard, hour);
    gnashRun(s, guard);

    if (s.ticks % 10 === 0 && !watching(guard)) {
        const time = hour === 0 ? "12 AM" : `${hour} AM`;
        const pct = Math.max(0, Math.ceil(s.power));
        const col = s.powerOut || pct < 20 ? "§c" : pct < 50 ? "§e" : "§a";
        const bars = "▮".repeat(Math.min(5, s.usage)) + "▯".repeat(Math.max(0, 5 - s.usage));
        guard.onScreenDisplay.setActionBar(`§fNight ${s.night}   §l${time}§r   ${col}Power ${pct}%   §7Usage ${col}${bars}`);
    }
    if (s.ticks % TICKS_PER_HOUR === 0 && hour > 0) title(guard, "", `§f${hour} AM`);
    if (watching(guard) && s.ticks % 20 === 0) {
        const v = view.get(guard.id);
        const cam = world.getEntity(v.cam);
        if (cam?.isValid) showCamLabel(guard, cam, v.idx, camerasNear(guard).length);
    }
}

function moveOpportunity(s, guard, hour) {
    const camsOn = watching(guard);
    for (const b of s.bots) {
        const bot = botOf(b);
        if (!bot || b.runTicks > 0 || possessed.has(b.id)) continue;
        const n = Math.max(1, Math.min(5, s.night)) - 1;
        const level = Math.min(20, AI[b.who][n] + (hour >= 3 ? 2 : 0) + (s.night > 5 ? s.night - 5 : 0) + s.bonus);
        if (level <= 0 || Math.floor(Math.random() * 20) + 1 > level) continue;

        if (b.who === "gnash") {
            if (camsOn) s.gnashMeter = Math.max(0, s.gnashMeter - 1);
            else if (++s.gnashMeter >= 4 && s.doorSpots.length) {
                b.runTicks = 40;
                sound(s.dim, "gator_run", bot.location, 2.0);
                // he sprints the hall: hop him room by room toward the door
                const path = [...s.rooms.slice(-2), s.doorSpots[0]];
                path.forEach((spot, i) => system.runTimeout(() => { const e = botOf(b); if (e) moveBot(e, spot); }, 8 + i * 10));
            }
            continue;
        }
        if (b.who === "pip" && (camsOn || (s.night < 3 && hour < 3))) continue;

        const atDoor = s.rooms.length + 1;
        if (b.progress >= atDoor && s.doorSpots.length) {
            const door = Math.min(b.door, s.doorSpots.length - 1);
            if (doorClosed(s, door)) {
                sound(s.dim, "door_bang", s.doorSpots[door], 1.4, 0.9);
                b.progress = Math.max(1, Math.floor(Math.random() * Math.max(1, s.rooms.length)) + 1);
                moveBot(bot, s.rooms.length ? s.rooms[b.progress - 1] : home(bot));
                if (DOOR[b.who] < 0) b.door = Math.floor(Math.random() * 2);
            } else {
                jumpscare(bot, guard, true);
            }
        } else {
            b.progress++;
            const to = b.progress <= s.rooms.length ? s.rooms[b.progress - 1]
                : s.doorSpots.length ? s.doorSpots[Math.min(b.door, s.doorSpots.length - 1)] : undefined;
            moveBot(bot, to);
            if (b.progress >= atDoor && Math.random() < 0.33) sound(s.dim, "giggle", bot.location, 0.8, 0.8 + Math.random() * 0.3);
        }
    }
}

function gnashRun(s, guard) {
    for (const b of s.bots) {
        if (b.runTicks <= 0) continue;
        if (--b.runTicks > 0) continue;
        const bot = botOf(b);
        s.gnashMeter = 0;
        if (!bot) continue;
        if (doorClosed(s, 0)) {
            for (let i = 0; i < 3; i++) system.runTimeout(() => sound(s.dim, "door_bang", s.doorSpots[0], 1.6, 0.8 + i * 0.1), i * 6);
            s.power -= 6;
            returnHome(bot);
        } else jumpscare(bot, guard, true);
    }
}

function setDoors(s, closed) {
    for (const g of s.doors) for (const l of g) {
        const b = block(s.dim, l);
        if (b?.typeId === `${NS}:security_door`) b.setPermutation(b.permutation.withState(`${NS}:closed`, closed));
    }
}

function powerOut(s) {
    s.powerOut = true; s.power = 0; s.powerOutTicks = 0;
    setDoors(s, false);
    for (const l of s.lights) {
        const b = block(s.dim, l);
        if (b?.typeId === `${NS}:hall_light`) b.setPermutation(b.permutation.withState(`${NS}:lit`, false));
    }
    sound(s.dim, "power_down", s.clock, 2.0);
    s.pipStrike = 220 + Math.floor(Math.random() * 260);
}

function powerOutTick(s, guard) {
    s.powerOutTicks++;
    let pip;
    for (const b of s.bots) if (b.who === "pip") pip = botOf(b);
    if (!pip && s.bots.length) pip = botOf(s.bots[0]);
    if (!pip) return;
    if (s.powerOutTicks === 60 && s.doorSpots.length) {
        moveBot(pip, s.doorSpots[0]);
        sound(s.dim, "music_box", s.doorSpots[0], 1.5);
    }
    if (s.powerOutTicks === s.pipStrike) jumpscare(pip, guard, true);
}

function win(s, guard) {
    sound(s.dim, "chime_6am", guard.location, 1.5);
    title(guard, "§l§a6 AM", `Night ${s.night} complete!`);
    const pay = 10 + s.night * 15;
    give(guard, `${NS}:pip_coin`, pay);
    guard.setDynamicProperty("night", s.night + 1);
    say(guard, `§6Paycheck: ${pay} Pip Coins. ${s.night >= 5 ? "You survived the week! Nights keep getting harder from here..." : `Night ${s.night + 1} is next.`}`);
    endShift(s, true);
}

function onGuardCaught(p, bot) {
    for (const s of [...shifts.values()]) {
        if (s.guard === p.id) { title(p, "§4§lGAME OVER", `Caught by ${NAMES[who(bot)]} on Night ${s.night}`); endShift(s, false); }
    }
}

function endShift(s, won, msg) {
    for (const b of s.bots) {
        const bot = botOf(b);
        if (!bot) continue;
        bot.setDynamicProperty("shift", false);
        returnHome(bot);
        if (isNight()) { fire(bot, "night"); setMode(bot, MODE.HUNT); } else setMode(bot, MODE.STAGE);
    }
    setDoors(s, false);
    const guard = world.getEntity(s.guard);
    if (guard?.isValid) { exitCameras(guard); if (msg) say(guard, "§c" + msg); }
    shifts.delete(s.key);
}

system.runInterval(() => { for (const s of [...shifts.values()]) { try { shiftTick(s); } catch (err) { console.warn(`[pipsparty] shift: ${err}`); } } }, 1);

world.afterEvents.playerLeave?.subscribe?.(({ playerId }) => {
    for (const s of [...shifts.values()]) if (s.guard === playerId) shifts.delete(s.key);
    view.delete(playerId);
});

// ------------------------------------------------------------------ the business
const FUN = {
    arcade_cabinet: 4, prize_counter: 3, pizza_oven: 3, party_table: 2, ticket_machine: 2, party_balloons: 1, gift_box: 1,
    poster_pip: 1, poster_rusty: 1, poster_marla: 1, poster_gnash: 1, stage_floor: 0.2,
};
function funRating(dim, c) {
    let score = 0, chairs = 0, balls = 0;
    for (let x = -14; x <= 14; x++) for (let y = -2; y <= 5; y++) for (let z = -14; z <= 14; z++) {
        const b = block(dim, { x: c.x + x, y: c.y + y, z: c.z + z });
        if (!b || b.isAir || !b.typeId.startsWith(NS)) continue;
        const id = b.typeId.slice(NS.length + 1);
        if (id === "party_chair") { if (chairs++ < 40) score += 0.5; }
        else if (id === "ball_pit") { if (balls++ < 30) score += 0.5; }
        else score += FUN[id] ?? 0;
    }
    const bots = dim.getEntities({ families: ["animatronic"], location: c, maxDistance: 24 }).length;
    return Math.round(score + bots * 8);
}

function collect(p, reg) {
    const k = "reg|" + key(reg.dimension, reg.location);
    const today = world.getDay();
    const last = world.getDynamicProperty(k);
    const rating = funRating(reg.dimension, reg.location);
    if (typeof last !== "number") {
        world.setDynamicProperty(k, today);
        say(p, `§6The Register is open for business! Come back tomorrow to collect. §7(Fun rating: ${rating})`);
        return;
    }
    const days = Math.min(7, today - last);
    if (days <= 0) { say(p, `§7Register's empty. Come back tomorrow. (Fun rating: ${rating})`); return; }
    const coins = Math.floor(days * (4 + rating / 3));
    world.setDynamicProperty(k, today);
    give(p, `${NS}:pip_coin`, coins);
    try { p.playSound("random.orb", { pitch: 1.3 }); } catch { }
    say(p, `§6${days} day${days > 1 ? "s" : ""} of business: ${Math.floor(days * (10 + rating * 2))} guests, fun rating ${rating}, +${coins} Pip Coins`);
}

const SHOP = [
    ["Camera Tablet", 4, "camera_tablet", 1], ["Security Camera", 3, "security_camera", 1], ["Security Door x2", 6, "security_door", 2],
    ["Door Button", 2, "door_button", 1], ["Light Button", 2, "light_button", 1], ["Hall Light", 2, "hall_light", 1], ["Time Clock", 5, "time_clock", 1],
    ["Arcade Cabinet", 12, "arcade_cabinet", 1], ["Prize Counter", 9, "prize_counter", 1], ["Ticket Machine", 6, "ticket_machine", 1],
    ["Party Table x2", 4, "party_table", 2], ["Party Chair x4", 2, "party_chair", 4], ["Ball Pit x8", 3, "ball_pit", 8],
    ["Pizza Oven", 8, "pizza_oven", 1], ["Kitchen Counter x2", 3, "kitchen_counter", 2], ["Party Balloons x2", 2, "party_balloons", 2],
    ["Gift Box x2", 2, "gift_box", 2], ["Checker Floor x16", 1, "checker_floor", 16], ["Red Party Wall x16", 1, "party_wall_red", 16],
    ["Blue Party Wall x16", 1, "party_wall_blue", 16], ["Purple Party Wall x16", 1, "party_wall_purple", 16], ["Party Trim x16", 1, "party_wall_trim", 16],
    ["Confetti Wall x16", 1, "confetti_wall", 16], ["Ceiling Tile x16", 1, "ceiling_tile", 16], ["Office Wall x16", 1, "office_wall", 16],
    ["Stage Floor x8", 1, "stage_floor", 8], ["Stage Curtain x8", 2, "stage_curtain", 8], ["Star Curtain x8", 2, "star_curtain", 8],
    ["Security Desk", 3, "security_desk", 1], ["Desk Fan", 2, "desk_fan", 1], ["Office Monitor", 3, "office_monitor", 1],
    ["Posters (Pip) x2", 2, "poster_pip", 2], ["Posters (Rusty) x2", 2, "poster_rusty", 2], ["Posters (Marla) x2", 2, "poster_marla", 2],
    ["Posters (Gnash) x2", 2, "poster_gnash", 2], ["Rules Poster", 1, "poster_rules", 1], ["Employees Only Sign", 1, "employees_only_sign", 1],
    ["Exit Sign", 1, "exit_sign", 1], ["Pizza Slice x4", 1, "pizza_slice", 4], ["Party Soda x3", 1, "party_soda", 3],
    ["Override Chip", 20, "override_chip", 1], ["Rusty the Robot Dog", 50, "rusty_spawn_egg", 1], ["Marla the Mole", 60, "marla_spawn_egg", 1],
    ["Gnash the Gator", 70, "gnash_spawn_egg", 1], ["Pip the Raccoon", 40, "pip_spawn_egg", 1], ["Pizzeria Kit", 64, "pizzeria_kit", 1],
];

function openCatalog(p) {
    const coins = countItem(p, `${NS}:pip_coin`);
    const form = new ActionFormData().title("Franchise Catalog").body(`§6You have ${coins} Pip Coins.§r\nRun shifts and collect from the Register to earn more.`);
    for (const [name, price] of SHOP) form.button(`${name}\n${coins >= price ? "§2" : "§4"}${price} Pip Coin${price > 1 ? "s" : ""}`);
    form.show(p).then((r) => {
        if (r.canceled || r.selection === undefined) return;
        const [name, price, id, count] = SHOP[r.selection];
        if (countItem(p, `${NS}:pip_coin`) < price) { say(p, `§cYou need ${price} Pip Coins for ${name}.`); return; }
        takeItem(p, `${NS}:pip_coin`, price);
        give(p, `${NS}:${id}`, count);
        try { p.playSound("random.orb"); } catch { }
        say(p, `§aBought ${name}!`);
    }).catch(() => { });
}

// ------------------------------------------------------------------ block buttons and items
function toggleNear(b, p, id, state, r, sfx) {
    if (powerOutNear(b.dimension, b.location)) { say(p, "§8*click* ...no power."); return; }
    const found = [];
    for (let x = -r; x <= r; x++) for (let y = -r; y <= r; y++) for (let z = -r; z <= r; z++) {
        const t = block(b.dimension, { x: b.location.x + x, y: b.location.y + y, z: b.location.z + z });
        if (t?.typeId === `${NS}:${id}`) found.push(t);
    }
    if (!found.length) { p.onScreenDisplay.setActionBar(`§7No ${id.replace("_", " ")} within ${r} blocks of this button.`); return; }
    const to = !found[0].permutation.getState(`${NS}:${state}`);
    for (const t of found) t.setPermutation(t.permutation.withState(`${NS}:${state}`, to));
    sound(b.dimension, sfx, center(b.location), 1.0, to ? 1.0 : 1.2);
}

system.beforeEvents.startup.subscribe((ev) => {
    const reg = (name, fn) => ev.blockComponentRegistry.registerCustomComponent(`${NS}:${name}`, { onPlayerInteract: (e) => { if (e.player) system.run(() => fn(e.block, e.player)); } });
    reg("door_button", (b, p) => toggleNear(b, p, "security_door", "closed", 3, "door_slam"));
    reg("light_button", (b, p) => toggleNear(b, p, "hall_light", "lit", 4, "light_buzz"));
    reg("time_clock", (b, p) => clockIn(p, b));
    reg("register", (b, p) => collect(p, b));
    reg("catalog", (b, p) => openCatalog(p));
});

function useItem(p, item, onBlock) {
    if (!item) return;
    if (item.typeId === `${NS}:camera_tablet`) nextCamera(p);
    else if (item.typeId === `${NS}:pizzeria_kit`) {
        const v = view.get(p.id) ?? { idx: -1, debounce: 0 };
        if (system.currentTick < v.debounce) return;
        v.debounce = system.currentTick + 40; view.set(p.id, v);
        const base = onBlock ?? { x: Math.floor(p.location.x), y: Math.floor(p.location.y) - 1, z: Math.floor(p.location.z) };
        buildPizzeria(p, base);
    }
}
world.afterEvents.itemUse.subscribe(({ source, itemStack }) => { if (source?.typeId === "minecraft:player") useItem(source, itemStack); });
world.afterEvents.playerInteractWithBlock.subscribe(({ player, itemStack, block: b }) => {
    if (itemStack?.typeId === `${NS}:camera_tablet` || itemStack?.typeId === `${NS}:pizzeria_kit`) useItem(player, itemStack, b.location);
});

// leaving the camera view when hurt, or when the tablet leaves your hand
system.runInterval(() => {
    for (const p of world.getAllPlayers()) {
        if (!watching(p)) continue;
        const held = inventory(p)?.getItem(p.selectedSlotIndex);
        if (held?.typeId !== `${NS}:camera_tablet`) exitCameras(p);
    }
}, 10);

// ------------------------------------------------------------------ Pizzeria Kit: a whole Pip's Party Palace, 40 x 34
const W = 40, DEPTH = 34, H = 6;

function buildPizzeria(p, ground) {
    const dim = p.dimension;
    // the front door faces the player: build north of where they stand
    const x0 = Math.floor(ground.x) - 19, y = Math.floor(ground.y), z0 = Math.floor(ground.z) - DEPTH - 1;
    const at = (lx, dy, lz) => ({ x: x0 + lx, y: y + dy, z: z0 + lz });
    const P = (id, states) => BlockPermutation.resolve(id.includes(":") ? id : `${NS}:${id}`, states ?? {});
    const face = (id, dir) => P(id, { "minecraft:cardinal_direction": dir });
    const set = (lx, dy, lz, perm) => { try { dim.setBlockPermutation(at(lx, dy, lz), perm); } catch { } };
    const fill = (lx1, lz1, lx2, lz2, dy1, dy2, perm) => {
        try { dim.fillBlocks(new BlockVolume(at(Math.min(lx1, lx2), dy1, Math.min(lz1, lz2)), at(Math.max(lx1, lx2), dy2, Math.max(lz1, lz2))), perm); } catch (e) { console.warn(`[pipsparty] fill: ${e}`); }
    };
    const air = P("minecraft:air");
    say(p, "§dBuilding Pip's Party Palace...");
    useUp(p, `${NS}:pizzeria_kit`);

    // site
    fill(-1, -1, W, DEPTH, 1, H + 4, air);
    try {
        dim.fillBlocks(new BlockVolume(at(-1, -4, -1), at(W, -1, DEPTH)), P("minecraft:stone"),
            { blockFilter: { includeTypes: ["minecraft:air", "minecraft:water", "minecraft:flowing_water", "minecraft:short_grass", "minecraft:tall_grass", "minecraft:snow_layer"] } });
    } catch { }
    fill(-1, -1, W, DEPTH, 0, 0, P("minecraft:grass_block"));
    fill(0, 0, W - 1, DEPTH - 1, 0, 0, P("checker_floor"));
    fill(0, 0, W - 1, DEPTH - 1, H, H, P("ceiling_tile"));
    // outer walls
    for (const [a, b, c, d] of [[0, 0, W - 1, 0], [0, DEPTH - 1, W - 1, DEPTH - 1], [0, 0, 0, DEPTH - 1], [W - 1, 0, W - 1, DEPTH - 1]]) {
        fill(a, b, c, d, 1, 2, P("party_wall_red"));
        fill(a, b, c, d, 3, 3, P("party_wall_trim"));
        fill(a, b, c, d, 4, H, P("party_wall_purple"));
    }
    fill(18, DEPTH - 1, 20, DEPTH - 1, 1, 3, air);
    set(19, 4, DEPTH - 2, face("exit_sign", "north"));

    const wall = P("party_wall_blue"), office = P("office_wall");
    // office (north-east) with two security doors
    fill(33, 1, 33, 8, 1, H - 1, office);
    fill(33, 8, 38, 8, 1, H - 1, office);
    fill(34, 1, 38, 7, 0, 0, P("minecraft:polished_andesite"));
    fill(33, 5, 33, 5, 1, 2, P("security_door", { [`${NS}:closed`]: false }));
    fill(36, 8, 36, 8, 1, 2, P("security_door", { [`${NS}:closed`]: false }));
    set(33, 2, 4, P("door_button")); set(33, 2, 6, P("light_button"));
    set(37, 2, 8, P("door_button")); set(38, 2, 8, P("light_button"));
    for (const lx of [35, 36, 37]) set(lx, 1, 2, face("security_desk", "south"));
    set(35, 2, 2, face("office_monitor", "south")); set(36, 2, 2, face("office_monitor", "south")); set(37, 2, 2, face("desk_fan", "south"));
    set(38, 1, 6, face("time_clock", "west"));
    set(38, 3, 3, face("poster_rules", "west")); set(34, 3, 1, face("poster_pip", "south"));
    set(36, H, 5, P("minecraft:sea_lantern"));
    set(34, 1, 2, P("minecraft:chest"));
    system.runTimeout(() => {
        try {
            const inv = block(dim, at(34, 1, 2))?.getComponent("minecraft:inventory")?.container;
            if (inv) {
                inv.setItem(0, new ItemStack(`${NS}:camera_tablet`, 1));
                inv.setItem(1, new ItemStack(`${NS}:override_chip`, 1));
                inv.setItem(2, new ItemStack(`${NS}:pizza_slice`, 4));
                inv.setItem(3, new ItemStack(`${NS}:party_soda`, 2));
            }
        } catch { }
    }, 2);

    // west hall, closet
    fill(28, 2, 32, 2, 1, H - 1, wall); fill(28, 3, 32, 3, 1, H - 1, wall); fill(28, 7, 32, 7, 1, H - 1, wall);
    set(31, H, 5, P("hall_light", { [`${NS}:lit`]: false }));
    set(29, 3, 4, face("poster_gnash", "south"));
    // backstage
    fill(22, 1, 22, 8, 1, H - 1, wall); fill(27, 1, 27, 8, 1, H - 1, wall); fill(22, 9, 27, 9, 1, H - 1, wall);
    fill(22, 4, 22, 4, 1, 2, air); fill(27, 4, 27, 6, 1, 3, air);
    for (let lz = 1; lz <= 8; lz += 2) set(26, 1, lz, P("minecraft:barrel"));
    set(23, 1, 1, P("minecraft:barrel")); set(23, 2, 1, P("minecraft:barrel"));
    set(24, 3, 1, face("employees_only_sign", "south"));
    // east hall
    fill(34, 9, 34, 19, 1, H - 1, wall); fill(38, 9, 38, 19, 1, H - 1, wall);
    set(36, H, 12, P("hall_light", { [`${NS}:lit`]: false }));
    // kitchen
    fill(28, 19, 38, 19, 1, H - 1, wall); fill(27, 19, 27, DEPTH - 1, 1, H - 1, wall);
    fill(35, 19, 37, 19, 1, 3, air); fill(27, 26, 27, 27, 1, 2, air);
    fill(28, 20, 38, 32, 0, 0, P("minecraft:smooth_stone"));
    for (let lz = 21; lz <= 31; lz++) set(38, 1, lz, face("kitchen_counter", "west"));
    for (let lx = 30; lx <= 33; lx++) set(lx, 1, 32, face("pizza_oven", "north"));
    for (let lx = 31; lx <= 34; lx++) set(lx, 1, 26, face("kitchen_counter", "south"));
    // party room
    for (const lx of [29, 32]) {
        set(lx, 1, 12, face("party_table", "south")); set(lx, 1, 15, face("party_table", "south"));
        set(lx, 1, 13, face("party_chair", "north")); set(lx, 1, 11, face("party_chair", "south"));
    }
    set(33, 1, 10, face("gift_box", "south")); set(33, 1, 17, P("party_balloons")); set(33, 2, 17, P("party_balloons"));
    fill(28, 8, 33, 8, 4, 5, P("confetti_wall"));
    // stage
    fill(6, 1, 20, 4, 1, 1, P("stage_floor"));
    fill(6, 1, 20, 1, 2, H - 1, P("stage_curtain"));
    fill(6, 1, 6, 4, 2, H - 1, P("stage_curtain"));
    fill(20, 1, 20, 4, 2, H - 1, P("stage_curtain"));
    for (const lx of [8, 13, 18]) set(lx, H, 5, P("minecraft:sea_lantern"));
    fill(1, 5, 5, 10, 1, H - 1, wall);   // closet wall west of the stage (lz 10 row)
    fill(1, 1, 5, 9, 1, H - 1, air);
    fill(1, 10, 5, 10, 1, H - 1, wall);
    // Gator Grotto
    fill(1, 11, 5, 11, 1, H - 1, wall); fill(1, 19, 5, 19, 1, H - 1, wall);
    fill(5, 12, 5, 18, 1, H - 2, P("star_curtain")); fill(5, 12, 5, 18, H - 1, H - 1, wall);
    fill(1, 12, 4, 18, 0, 0, P("stage_floor"));
    set(6, 4, 15, face("poster_gnash", "east"));
    // arcade
    for (let lz = 25; lz <= 31; lz += 2) set(1, 1, lz, face("arcade_cabinet", "east"));
    fill(3, 28, 6, 31, 1, 1, P("ball_pit"));
    for (let lx = 8; lx <= 10; lx++) set(lx, 1, 32, face("prize_counter", "north"));
    set(11, 1, 32, face("ticket_machine", "north"));
    set(1, 3, 22, face("poster_rusty", "east")); set(1, 3, 24, face("poster_marla", "east"));
    // dining hall
    for (let lx = 11; lx <= 23; lx += 4) for (let lz = 12; lz <= 28; lz += 5) {
        if (lx <= 11 && lz >= 24) continue;
        set(lx, 1, lz, face("party_table", "south")); set(lx + 1, 1, lz, face("party_table", "south"));
        set(lx, 1, lz - 1, face("party_chair", "south")); set(lx + 1, 1, lz - 1, face("party_chair", "south"));
        set(lx, 1, lz + 1, face("party_chair", "north")); set(lx + 1, 1, lz + 1, face("party_chair", "north"));
        if (Math.random() < 0.33) set(lx, 2, lz, face("gift_box", "south"));
    }
    for (let lx = 4; lx <= 24; lx += 6) for (let lz = 12; lz <= 30; lz += 6) set(lx, H, lz, P("minecraft:sea_lantern"));
    set(25, 1, 10, P("party_balloons")); set(25, 2, 10, P("party_balloons"));
    set(22, 1, 31, face("pizzeria_register", "south"));
    set(24, 1, 31, face("franchise_catalog", "south"));
    set(26, 3, 30, face("poster_pip", "west"));
    for (let i = 0; i < 10; i++) {
        const lx = 1 + Math.floor(Math.random() * (W - 2)), lz = 1 + Math.floor(Math.random() * (DEPTH - 2));
        if (block(dim, at(lx, H - 1, lz))?.isAir) set(lx, H - 1, lz, P("minecraft:web"));
    }

    // the band and the cameras (next tick, once the blocks are in)
    system.runTimeout(() => {
        const band = [["pip", 13, 2, 3, 0], ["rusty", 9, 2, 3, 0], ["marla", 17, 2, 3, 0], ["gnash", 2, 1, 15, -90]];
        for (const [w, lx, dy, lz, yaw] of band) {
            try {
                const l = at(lx, dy, lz);
                const e = dim.spawnEntity(`${NS}:${w}`, { x: l.x + 0.5, y: l.y, z: l.z + 0.5 });
                e.setRotation({ x: 0, y: yaw });
                setHome(e, e.location, yaw);
            } catch (err) { console.warn(`[pipsparty] spawn ${w}: ${err}`); }
        }
        const cams = [["CAM 1A Show Stage", 13, 14, 180], ["CAM 1B Dining Area", 25, 31, 135], ["CAM 1C Gator Grotto", 7, 15, 90],
        ["CAM 2A West Hall", 28, 5, -90], ["CAM 2B East Hall", 36, 18, 180], ["CAM 3 Backstage", 25, 7, 135],
        ["CAM 4 Kitchen", 37, 31, 135], ["CAM 5 Arcade", 9, 31, 135], ["CAM 6 Party Room", 32, 17, 135]];
        for (const [name, lx, lz, yaw] of cams) {
            try {
                const l = at(lx, 0, lz);
                const c = dim.spawnEntity(`${NS}:security_camera`, { x: l.x + 0.5, y: y + H - 0.75, z: l.z + 0.5 });
                c.nameTag = name;
                c.setRotation({ x: 25, y: yaw });
            } catch (err) { console.warn(`[pipsparty] camera: ${err}`); }
        }
        say(p, "§dPip's Party Palace is open! §7The office is in the back-right corner: punch the Time Clock to start a night shift.");
    }, 5);
}
