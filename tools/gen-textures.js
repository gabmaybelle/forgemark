// Placeholder textures for the hearth, forging anvil and smithing hammer, modelled on the
// vanilla textures they sit beside: bricks and stone for the hearth, the anvil for the forging
// anvil, the iron ingot and stick for the hammer. Writes 16x16 PNGs with Node's zlib only.
//   node tools/gen-textures.js
// Gabby's textures in art/ always win: a file with the same name there is copied instead.
// The vanilla textures come from the game jar in Loom's cache (never from the repo).
'use strict';
const fs = require('fs');
const os = require('os');
const path = require('path');
const zlib = require('zlib');
const { spawnSync } = require('child_process');

const out = path.join(__dirname, '..', 'common', 'src', 'main', 'resources', 'assets', 'forgemark', 'textures');
const art = path.join(__dirname, '..', 'art');
const minecraftVersion = /minecraft_version=(\S+)/.exec(fs.readFileSync(path.join(__dirname, '..', 'gradle.properties'), 'utf8'))[1];

// --- PNG reading (8-bit grayscale, RGB, RGBA, grey+alpha, and 1/2/4/8-bit palettes) ---
function readPng(file) {
	const buf = fs.readFileSync(file);
	let pos = 8;
	let width, height, depth, type, palette = null, trns = null;
	const idat = [];
	while (pos < buf.length) {
		const len = buf.readUInt32BE(pos);
		const kind = buf.toString('ascii', pos + 4, pos + 8);
		const data = buf.subarray(pos + 8, pos + 8 + len);
		if (kind === 'IHDR') {
			width = data.readUInt32BE(0);
			height = data.readUInt32BE(4);
			depth = data[8];
			type = data[9];
			if (data[12] !== 0) throw new Error(file + ': interlaced PNGs are not handled');
		} else if (kind === 'PLTE') palette = data;
		else if (kind === 'tRNS') trns = data;
		else if (kind === 'IDAT') idat.push(data);
		pos += 12 + len;
	}
	const raw = zlib.inflateSync(Buffer.concat(idat));
	const channels = { 0: 1, 2: 3, 3: 1, 4: 2, 6: 4 }[type];
	const bitsPerPixel = channels * depth;
	const stride = Math.ceil((width * bitsPerPixel) / 8);
	const bpp = Math.max(1, Math.ceil(bitsPerPixel / 8));
	const rows = [];
	let prev = Buffer.alloc(stride);
	let at = 0;
	for (let y = 0; y < height; y++) {
		const filter = raw[at++];
		const row = Buffer.from(raw.subarray(at, at + stride));
		at += stride;
		for (let i = 0; i < stride; i++) {
			const a = i >= bpp ? row[i - bpp] : 0;
			const b = prev[i];
			const c = i >= bpp ? prev[i - bpp] : 0;
			let add = 0;
			if (filter === 1) add = a;
			else if (filter === 2) add = b;
			else if (filter === 3) add = (a + b) >> 1;
			else if (filter === 4) {
				const p = a + b - c;
				const pa = Math.abs(p - a), pb = Math.abs(p - b), pc = Math.abs(p - c);
				add = pa <= pb && pa <= pc ? a : pb <= pc ? b : c;
			}
			row[i] = (row[i] + add) & 0xff;
		}
		rows.push(row);
		prev = row;
	}
	const pixels = [];
	for (let y = 0; y < height; y++) {
		const row = rows[y];
		const line = [];
		for (let x = 0; x < width; x++) {
			let px;
			if (type === 3) {
				const bitPos = x * depth;
				const byte = row[bitPos >> 3];
				const index = depth === 8 ? byte : (byte >> (8 - depth - (bitPos & 7))) & ((1 << depth) - 1);
				const alpha = trns && index < trns.length ? trns[index] : 255;
				px = [palette[index * 3], palette[index * 3 + 1], palette[index * 3 + 2], alpha];
			} else if (type === 0) {
				// A tRNS chunk names one grey value as transparent (vanilla's grayscale items use 0).
				const transparent = trns && trns.length >= 2 && row[x] === trns.readUInt16BE(0);
				px = [row[x], row[x], row[x], transparent ? 0 : 255];
			} else if (type === 4) px = [row[x * 2], row[x * 2], row[x * 2], row[x * 2 + 1]];
			else if (type === 2) {
				const transparent = trns && trns.length >= 6 && row[x * 3] === trns.readUInt16BE(0) && row[x * 3 + 1] === trns.readUInt16BE(2) && row[x * 3 + 2] === trns.readUInt16BE(4);
				px = [row[x * 3], row[x * 3 + 1], row[x * 3 + 2], transparent ? 0 : 255];
			} else px = [row[x * 4], row[x * 4 + 1], row[x * 4 + 2], row[x * 4 + 3]];
			line.push(px);
		}
		pixels.push(line);
	}
	return pixels;
}

// --- PNG writing (RGBA) ---
function crc32(buf) {
	let c, crc = 0xffffffff;
	for (let n = 0; n < buf.length; n++) {
		c = (crc ^ buf[n]) & 0xff;
		for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
		crc = (crc >>> 8) ^ c;
	}
	return (crc ^ 0xffffffff) >>> 0;
}
function chunk(kind, data) {
	const len = Buffer.alloc(4);
	len.writeUInt32BE(data.length);
	const td = Buffer.concat([Buffer.from(kind, 'ascii'), data]);
	const crc = Buffer.alloc(4);
	crc.writeUInt32BE(crc32(td));
	return Buffer.concat([len, td, crc]);
}
function writePng(file, pixels) {
	const height = pixels.length, width = pixels[0].length;
	const raw = [];
	for (let y = 0; y < height; y++) {
		raw.push(0);
		for (let x = 0; x < width; x++) raw.push(...pixels[y][x]);
	}
	const ihdr = Buffer.alloc(13);
	ihdr.writeUInt32BE(width, 0);
	ihdr.writeUInt32BE(height, 4);
	ihdr[8] = 8;
	ihdr[9] = 6;
	fs.mkdirSync(path.dirname(file), { recursive: true });
	fs.writeFileSync(file, Buffer.concat([
		Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
		chunk('IHDR', ihdr), chunk('IDAT', zlib.deflateSync(Buffer.from(raw))), chunk('IEND', Buffer.alloc(0)),
	]));
}

// --- the vanilla textures, pulled from the game jar in Loom's cache ---
function vanillaTextures(names) {
	const jar = path.join(os.homedir(), '.gradle', 'caches', 'fabric-loom', minecraftVersion, 'minecraft-merged.jar');
	if (!fs.existsSync(jar)) throw new Error('No ' + jar + ': run ./gradlew build once so Loom fetches the game');
	const tmp = fs.mkdtempSync(path.join(os.tmpdir(), 'forgemark-tex-'));
	const entries = names.map((n) => 'assets/minecraft/textures/' + n + '.png');
	let r = spawnSync('unzip', ['-o', '-q', jar, ...entries, '-d', tmp], { stdio: 'ignore' });
	if (r.error || r.status !== 0) {
		r = spawnSync(process.platform === 'win32' ? 'C:\\Windows\\System32\\tar.exe' : 'tar', ['-xf', jar, '-C', tmp, ...entries], { stdio: 'ignore' });
	}
	const textures = {};
	for (const name of names) textures[name] = readPng(path.join(tmp, 'assets', 'minecraft', 'textures', name + '.png'));
	fs.rmSync(tmp, { recursive: true, force: true });
	return textures;
}

// --- pixel helpers ---
const clamp = (v) => Math.max(0, Math.min(255, Math.round(v)));
const tint = ([r, g, b, a], [fr, fg, fb]) => [clamp(r * fr), clamp(g * fg), clamp(b * fb), a];
const blend = ([r, g, b, a], [r2, g2, b2], f) => [clamp(r + (r2 - r) * f), clamp(g + (g2 - g) * f), clamp(b + (b2 - b) * f), a];
const grid = (fn) => Array.from({ length: 16 }, (_, y) => Array.from({ length: 16 }, (_, x) => fn(x, y)));
/** The lightest, middle and darkest opaque colours of a texture, for drawing new shapes in its palette. */
function shades(pixels) {
	const opaque = pixels.flat().filter((p) => p[3] > 0).sort((p, q) => (p[0] + p[1] + p[2]) - (q[0] + q[1] + q[2]));
	// The lowest quarter is mostly outline; the body's shades sit above it.
	return { dark: opaque[Math.floor(opaque.length * 0.3)], mid: opaque[Math.floor(opaque.length * 0.65)], light: opaque[Math.floor(opaque.length * 0.92)] };
}

const v = vanillaTextures(['block/bricks', 'block/stone', 'block/coal_block', 'block/anvil', 'block/anvil_top', 'block/copper_block', 'item/iron_ingot', 'item/stick']);
const copper = shades(v['block/copper_block']).mid;
const steel = [0.82, 0.85, 0.95]; // the forging anvil is bluer and darker than the vanilla one
const ingot = shades(v['item/iron_ingot']);

const textures = {
	// Brick walls on a stone footing, sooted at the top.
	'block/hearth_side': grid((x, y) => {
		if (y >= 12) return v['block/stone'][y][x];
		const brick = v['block/bricks'][y][x];
		return y < 3 ? tint(brick, [0.7, 0.68, 0.66]) : brick;
	}),
	// A stone rim around a bed of coals.
	'block/hearth_top': grid((x, y) => {
		const inside = x >= 3 && x <= 12 && y >= 3 && y <= 12;
		if (!inside) return v['block/stone'][y][x];
		const edge = x === 3 || x === 12 || y === 3 || y === 12;
		const coal = tint(v['block/coal_block'][y][x], edge ? [0.7, 0.62, 0.58] : [1.15, 1.0, 0.92]);
		// A few embers glowing through the coals.
		const ember = !edge && ((x * 13 + y * 7 + (x * y) % 5) % 11) < 2;
		return ember ? blend(coal, [214, 92, 28], 0.6) : coal;
	}),
	'block/hearth_bottom': grid((x, y) => v['block/stone'][y][x]),
	// The vanilla anvil in darker steel.
	'block/forging_anvil': grid((x, y) => tint(v['block/anvil'][y][x], steel)),
	// Its top too, with a brass band across the working face so it reads as its own block.
	'block/forging_anvil_top': grid((x, y) => {
		const px = tint(v['block/anvil_top'][y][x], steel);
		const band = x >= 3 && x <= 12 && y >= 6 && y <= 9;
		return band ? blend(px, copper, 0.35) : px;
	}),
	// A sledge drawn the way vanilla draws tools, on the diagonal: the handle runs from the
	// bottom left up to the top right, and the head is a block set across it at the top end,
	// with a lit top face and a shaded front. Iron ingot shades for the head, stick browns for
	// the handle, and an outline worked out around the shape.
	'item/smithing_hammer': (() => {
		const stick = shades(v['item/stick']);
		const outline = [...ingot.dark.slice(0, 3).map((c) => clamp(c * 0.55)), 255];
		// u is constant along the handle's direction (up and right), w across it.
		const u = (x, y) => x + y;
		const w = (x, y) => x - y;
		const head = (x, y) => Math.abs(w(x, y) - 4) <= 2 && u(x, y) >= 10 && u(x, y) <= 21;
		const handle = (x, y) => (u(x, y) === 15 || u(x, y) === 16) && w(x, y) >= -12 && w(x, y) < 2;
		const solid = (x, y) => x >= 0 && y >= 0 && x < 16 && y < 16 && (head(x, y) || handle(x, y));
		return grid((x, y) => {
			if (head(x, y)) {
				const across = w(x, y) - 4; // -2 top face, +2 front face
				const end = u(x, y) <= 11 || u(x, y) >= 20; // the striking faces at both ends
				if (end) return across < 0 ? ingot.mid : ingot.dark;
				if (across <= -1) return across === -2 ? ingot.light : ingot.mid;
				if (across === 0) return blend(ingot.mid, ingot.dark, 0.35);
				return across === 2 ? ingot.dark : blend(ingot.mid, ingot.dark, 0.7);
			}
			if (handle(x, y)) {
				if (w(x, y) <= -11) return stick.dark; // the butt
				return u(x, y) === 15 ? stick.light : stick.mid;
			}
			// One pixel of outline around everything solid.
			for (const [dx, dy] of [[1, 0], [-1, 0], [0, 1], [0, -1]]) {
				if (solid(x + dx, y + dy)) return outline;
			}
			return [0, 0, 0, 0];
		});
	})(),
};

for (const [name, pixels] of Object.entries(textures)) {
	const own = path.join(art, name.replace('/', path.sep) + '.png');
	const target = path.join(out, name + '.png');
	if (fs.existsSync(own)) {
		fs.mkdirSync(path.dirname(target), { recursive: true });
		fs.copyFileSync(own, target);
		console.log('art/        ' + name);
		continue;
	}
	writePng(target, pixels);
	console.log('placeholder ' + name);
}

// The mod icon (shown in mod lists), the same design as the store pages' icon: the hammer with
// hard edges over a dithered ember glow, with sparks off its head. Laid out on a 512 canvas and
// drawn at any size that divides it: 128x128 here (`--store-icon <file>` writes the 512 one).
// art/icon.png replaces the in-game icon, like the textures.
function drawIcon(size) {
	const k = size / 512, scale = 24 * k, cell = 16 * k, offset = (size - 16 * scale) / 2;
	const hammer = textures["item/smithing_hammer"];
	const hammerAt = (x, y, d = 0) => {
		const hx = Math.floor((x - offset - d) / scale), hy = Math.floor((y - offset - d) / scale);
		return hx >= 0 && hy >= 0 && hx < 16 && hy < 16 && hammer[hy][hx][3] > 0 ? hammer[hy][hx] : null;
	};
	// Ember glow: five soft bands, brightest just below the head, with a 4x4 ordered dither.
	const palette = [[24, 13, 10], [50, 23, 14], [88, 35, 16], [134, 52, 19], [180, 78, 27]];
	const bayer = [[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]];
	const cells = 512 / 16;
	const glow = (cx, cy) => {
		const dx = (cx + 0.5) / cells - 0.62, dy = (cy + 0.5) / cells - 0.52;
		const t = Math.max(0, 1 - Math.sqrt(dx * dx + dy * dy) / 0.78) ** 1.6 * (palette.length - 1);
		const band = Math.floor(t);
		const pick = t - band > (bayer[cy % 4][cx % 4] + 0.5) / 16 ? band + 1 : band;
		return palette[Math.min(pick, palette.length - 1)];
	};
	// Sparks on the 512 canvas: [x, y, brightness 0 bright .. 2 dim], 22 pixels square.
	const sparkColours = [[255, 244, 190], [255, 204, 90], [240, 130, 40]];
	const sparks = [[392, 252, 0], [430, 226, 1], [412, 290, 0], [458, 270, 2], [372, 312, 1], [448, 318, 1], [474, 228, 2], [418, 350, 2]];
	const sparkSize = Math.max(1, Math.round(22 * k));
	return Array.from({ length: size }, (_, y) => Array.from({ length: size }, (_, x) => {
		const h = hammerAt(x, y);
		// The metal warmed a touch, as if lit by the forge.
		if (h) return [Math.min(255, Math.round(h[0] * 1.06 + 10)), Math.round(h[1] * 0.98 + 2), Math.round(h[2] * 0.9), 255];
		for (const [sx, sy, c] of sparks) {
			const px = Math.round(sx * k), py = Math.round(sy * k);
			if (x >= px && x < px + sparkSize && y >= py && y < py + sparkSize) return [...sparkColours[c], 255];
		}
		let [r, g, b] = glow(Math.floor(x / cell), Math.floor(y / cell));
		if (hammerAt(x, y, 12 * k)) { r *= 0.35; g *= 0.35; b *= 0.35; }   // drop shadow
		return [Math.round(r), Math.round(g), Math.round(b), 255];
	}));
}
{
	const own = path.join(art, "icon.png");
	const target = path.join(out, "..", "icon.png");
	if (fs.existsSync(own)) {
		fs.copyFileSync(own, target);
		console.log("art/        icon");
	} else {
		writePng(target, drawIcon(128));
		console.log("placeholder icon");
	}
	if (process.argv.includes('--store-icon')) {
		const store = process.argv[process.argv.indexOf('--store-icon') + 1];
		writePng(store, drawIcon(512));
		console.log('store icon  ' + store);
	}
}

// A preview sheet for checking by eye: every texture at 8x, side by side (not shipped).
if (process.argv.includes('--preview')) {
	const names = Object.keys(textures);
	const scale = 8, gap = 8;
	const width = names.length * (16 * scale + gap) + gap, height = 16 * scale + 2 * gap;
	const sheet = Array.from({ length: height }, () => Array.from({ length: width }, () => [60, 60, 60, 255]));
	names.forEach((name, i) => {
		const ox = gap + i * (16 * scale + gap);
		for (let y = 0; y < 16 * scale; y++) for (let x = 0; x < 16 * scale; x++) {
			const px = textures[name][Math.floor(y / scale)][Math.floor(x / scale)];
			if (px[3] > 0) sheet[gap + y][ox + x] = [px[0], px[1], px[2], 255];
		}
	});
	const preview = process.argv[process.argv.indexOf('--preview') + 1];
	writePng(preview, sheet);
	console.log('preview     ' + preview);
}
