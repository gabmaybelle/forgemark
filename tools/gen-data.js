// Data files that follow a pattern: the per-metal tags (heat time and strikes per heat), the
// heatable-materials tag, the block states, models, item definitions, loot tables and recipes
// for the hearth, the forging anvil and the hammer. Edit this script, not its output.
//   node tools/gen-data.js
'use strict';
const fs = require('fs');
const path = require('path');
const res = path.join(__dirname, '..', 'common', 'src', 'main', 'resources');

function write(rel, json) {
	const file = path.join(res, rel);
	fs.mkdirSync(path.dirname(file), { recursive: true });
	fs.writeFileSync(file, JSON.stringify(json, null, '\t') + '\n');
	console.log(rel);
}
const optional = (id) => ({ id, required: false });

// Metals. Anything heatable that isn't in a tag below uses the defaults in Metal.java (heats
// in 10 seconds, 3 strikes per heat). The c: tags are Fabric's and NeoForge's shared
// conventions, so modded metals that use them are covered without per-mod code.
write('data/forgemark/tags/item/heatable.json', {
	values: ['minecraft:iron_ingot', 'minecraft:gold_ingot', 'minecraft:copper_ingot', 'minecraft:netherite_ingot', 'minecraft:diamond',
		optional('#c:ingots'), optional('#c:gems')],
});
write('data/forgemark/tags/item/heats_quickly.json', {
	values: ['minecraft:gold_ingot', 'minecraft:copper_ingot', optional('#c:ingots/gold'), optional('#c:ingots/copper'), optional('#c:ingots/tin'), optional('#c:ingots/silver')],
});
write('data/forgemark/tags/item/heats_slowly.json', {
	values: ['minecraft:netherite_ingot', 'minecraft:diamond', optional('#c:ingots/netherite'), optional('#c:gems/diamond'), optional('#c:ingots/steel'), optional('#c:ingots/adamantite')],
});
write('data/forgemark/tags/item/soft_metal.json', { values: ['minecraft:gold_ingot', optional('#c:ingots/gold')] });
write('data/forgemark/tags/item/hard_metal.json', {
	values: ['minecraft:netherite_ingot', 'minecraft:diamond', optional('#c:ingots/netherite'), optional('#c:gems/diamond'), optional('#c:ingots/steel'), optional('#c:ingots/adamantite')],
});

// Blocks and items: states, models, item definitions, loot, recipes.
const hearthModel = { parent: 'minecraft:block/cube_bottom_top', textures: { top: 'forgemark:block/hearth_top', side: 'forgemark:block/hearth_side', bottom: 'forgemark:block/hearth_bottom' } };
write('assets/forgemark/blockstates/hearth.json', { variants: { 'lit=false': { model: 'forgemark:block/hearth' }, 'lit=true': { model: 'forgemark:block/hearth' } } });
write('assets/forgemark/models/block/hearth.json', hearthModel);
write('assets/forgemark/items/hearth.json', { model: { type: 'minecraft:model', model: 'forgemark:block/hearth' } });

const anvilVariants = {};
for (const [facing, y] of [['north', 180], ['south', 0], ['west', 90], ['east', 270]]) {
	anvilVariants['facing=' + facing] = y ? { model: 'forgemark:block/forging_anvil', y } : { model: 'forgemark:block/forging_anvil' };
}
write('assets/forgemark/blockstates/forging_anvil.json', { variants: anvilVariants });
write('assets/forgemark/models/block/forging_anvil.json', {
	parent: 'minecraft:block/template_anvil',
	textures: { particle: 'forgemark:block/forging_anvil', body: 'forgemark:block/forging_anvil', top: 'forgemark:block/forging_anvil_top' },
});
write('assets/forgemark/items/forging_anvil.json', { model: { type: 'minecraft:model', model: 'forgemark:block/forging_anvil' } });

write('assets/forgemark/models/item/smithing_hammer.json', { parent: 'minecraft:item/handheld', textures: { layer0: 'forgemark:item/smithing_hammer' } });
write('assets/forgemark/items/smithing_hammer.json', { model: { type: 'minecraft:model', model: 'forgemark:item/smithing_hammer' } });

write('data/minecraft/tags/block/mineable/pickaxe.json', { values: ['forgemark:hearth', 'forgemark:forging_anvil'] });
write('data/forgemark/tags/item/hammers.json', { values: ['forgemark:smithing_hammer'] });
for (const block of ['hearth', 'forging_anvil']) {
	write(`data/forgemark/loot_table/blocks/${block}.json`, {
		type: 'minecraft:block',
		pools: [{ rolls: 1, entries: [{ type: 'minecraft:item', name: 'forgemark:' + block }], condition: { type: 'minecraft:survives_explosion' } }],
		random_sequence: 'forgemark:blocks/' + block,
	});
}
write('data/forgemark/recipe/hearth.json', {
	type: 'minecraft:crafting_shaped', pattern: ['BBB', 'B B', 'SSS'], key: { B: 'minecraft:bricks', S: 'minecraft:stone' }, result: { id: 'forgemark:hearth' },
});
write('data/forgemark/recipe/forging_anvil.json', {
	type: 'minecraft:crafting_shaped', pattern: ['III', ' B ', 'III'], key: { I: 'minecraft:iron_ingot', B: 'minecraft:iron_block' }, result: { id: 'forgemark:forging_anvil' },
});
write('data/forgemark/recipe/smithing_hammer.json', {
	type: 'minecraft:crafting_shaped', pattern: ['II ', 'IS ', '  S'], key: { I: 'minecraft:iron_ingot', S: 'minecraft:stick' }, result: { id: 'forgemark:smithing_hammer' },
});
