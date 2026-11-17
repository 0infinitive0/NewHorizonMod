import arc.files.*;
import arc.math.*;
import arc.struct.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.io.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.units.*;

/**
 * Writes the three attack maps that show off the nukes and what came with them into a folder (normally assets/maps):
 *
 * <ul>
 * <li><b>Nuclear Standoff</b>: two bases either side of a ridge. The enemy has every kind of launcher, with silos that keep them loaded,
 * and the whole set of nuke defences.</li>
 * <li><b>Leviathan Strait</b>: islands. The enemy island has a Serpent Nest and a Crab Dock at work, and an Orbital Launcher.</li>
 * <li><b>Event Horizon</b>: a fortress of omni walls and omni shields with a Gravity Dock and a Modular Foundry, and omni-matter
 * deposits for the player.</li>
 * </ul>
 *
 * The enemy team cheats (no items, no power needed), so its factories, docks and launchers run by themselves; how fast is set by its
 * unit build speed. Nothing is random between runs: the same code writes the same maps.
 *
 * <p>Usage: {@code java -cp <server-release.jar>:<classes> AttackMaps <data dir with mods/NewHorizonMod.jar> <output dir>}
 */
public class AttackMaps{
	static final Team player = Team.sharded, enemy = Team.crux;
	static int w, h, seed;
	static Block ground;

	/** The mod's content, by name: its classes are loaded by the game's own mod loader, so they are not used here directly. */
	static Block B(String name){
		Block block = Vars.content.block("new-horizon-" + name);
		if(block == null) throw new IllegalArgumentException("no block " + name);
		return block;
	}

	static UnitType U(String name){
		UnitType type = Vars.content.unit("new-horizon-" + name);
		if(type == null) throw new IllegalArgumentException("no unit " + name);
		return type;
	}

	static Item omni(){
		return Vars.content.item("new-horizon-omni-matter");
	}

	public static void main(String[] args){
		Harness.boot(args[0], () -> {
			Fi out = new Fi(args[1]);
			out.mkdirs();
			standoff(out);
			strait(out);
			horizon(out);
		});
	}

	// ------------------------------------------------------------------------------------------------------------------------
	// Nuclear Standoff
	// ------------------------------------------------------------------------------------------------------------------------

	static void standoff(Fi out) throws Exception{
		begin(240, 150, 11, Blocks.darksand);
		int px = 38, py = 75, ex = 204, ey = 75;
		for(int x = 0; x < w; x++){
			for(int y = 0; y < h; y++){
				float n = fbm(x, y, 22f, 0);
				Block floor = n > 0.56f ? Blocks.stone : n < 0.36f ? Blocks.sand : Blocks.darksand;
				Block wall = Blocks.air;
				//Rocks, thicker towards the edges; a ridge down the middle with two passes through it.
				float edge = Math.min(Math.min(x, w - 1 - x), Math.min(y, h - 1 - y));
				if(fbm(x, y, 13f, 1) + Mathf.clamp(1f - edge / 9f) * 0.5f > 0.69f) wall = Blocks.duneWall;
				float ridge = Math.abs(x - (120f + (fbm(x, y, 30f, 2) - 0.5f) * 26f));
				boolean pass = Math.abs(y - 38) < 7 || Math.abs(y - 112) < 7;
				if(ridge < 5f && !pass) wall = Blocks.stoneWall;
				if(ridge < 9f) floor = Blocks.stone;
				set(x, y, floor, wall);
			}
		}
		clear(px, py, 26);
		clear(ex, ey, 40);
		clear(120, 38, 9);
		clear(120, 112, 9);
		ore(px - 12, py - 14, 6, Blocks.oreCopper);
		ore(px - 12, py + 14, 6, Blocks.oreLead);
		ore(px + 14, py - 17, 6, Blocks.oreTitanium);
		ore(px + 15, py + 17, 5, Blocks.oreThorium);
		ore(px - 21, py, 5, Blocks.oreCoal);
		ore(px + 2, py + 24, 5, Blocks.oreScrap);
		ore(96, 40, 6, Blocks.oreThorium);
		ore(96, 110, 6, Blocks.oreTitanium);
		ore(140, 75, 6, Blocks.oreThorium);
		Vars.world.endMapLoad();

		put(Blocks.coreNucleus, px, py, 0, player);
		defence(px + 12, py - 7);

		//The enemy: its core at the back, a wall across the front, and the silos between them.
		put(Blocks.coreNucleus, ex + 8, ey, 0, enemy);
		silo(ex - 16, ey + 20, B("nuke-launcher"), U("nuke-explosive"));
		silo(ex - 16, ey - 20, B("nuke-launcher"), U("nuke-incendiary"));
		silo(ex - 18, ey, B("nuke-launcher"), U("nuke-quake"));
		silo(ex + 2, ey + 28, B("multi-nuke-launcher"), U("nuke-swarm"));
		silo(ex + 4, ey - 28, B("orbital-launcher"), U("nuke-radiation"));
		//Everything a base can do about nukes, so that the player's own have to get past it.
		put(B("cloak-projector"), ex + 12, ey + 8, 0, enemy);
		put(B("nuke-jammer"), ex - 6, ey + 10, 0, enemy);
		put(B("nuke-interceptor"), ex - 22, ey + 10, 0, enemy);
		put(B("nuke-interceptor"), ex - 22, ey - 10, 0, enemy);
		put(B("long-range-radar"), ex + 14, ey - 9, 0, enemy);
		put(B("decoy-beacon"), ex + 28, ey, 0, enemy);
		wallLine(ex - 30, ey - 32, ex - 30, ey + 32, Blocks.thoriumWallLarge, ey - 3, ey + 3);
		for(int i = -3; i <= 3; i++){
			if(i == 0) continue;
			put(i % 2 == 0 ? Blocks.cyclone : Blocks.ripple, ex - 26, ey + i * 9, 0, enemy);
		}
		put(Blocks.spectre, ex - 8, ey + 4, 0, enemy);
		put(Blocks.spectre, ex - 8, ey - 5, 0, enemy);
		put(Blocks.cyclone, ex + 16, ey + 18, 0, enemy);
		put(Blocks.cyclone, ex + 16, ey - 18, 0, enemy);
		factory(Blocks.groundFactory, ex - 4, ey + 18, UnitTypes.dagger);
		factory(Blocks.groundFactory, ex - 4, ey - 18, UnitTypes.dagger);
		factory(Blocks.airFactory, ex + 22, ey + 10, UnitTypes.flare);

		Rules rules = rules(0.06f, 60f * 150f, 60f * 240f);
		rules.loadout = ItemStack.list(Items.copper, 4000, Items.lead, 4000, Items.graphite, 3000, Items.silicon, 4000, Items.titanium, 3000,
			Items.thorium, 2500, Items.metaglass, 1000, Items.plastanium, 800, Items.phaseFabric, 600, Items.surgeAlloy, 800);
		rules.spawns = Seq.with(
			group(UnitTypes.dagger, 1, 1, 2, 2f, 12),
			group(UnitTypes.mace, 4, 2, 1, 3f, 8),
			group(UnitTypes.horizon, 3, 2, 2, 2f, 10),
			group(UnitTypes.fortress, 8, 3, 1, 4f, 6),
			group(UnitTypes.zenith, 10, 3, 1, 4f, 6),
			group(UnitTypes.scepter, 18, 6, 1, 0f, 1)
		);
		write(out, "nuclear-standoff", "Nuclear Standoff",
			"[accent]Attack.[] The enemy across the ridge has a Nuke Launcher at every silo, a Multi Nuke Launcher and an Orbital Launcher, and they keep "
				+ "loading themselves. You start with a radar and two Nuke Interceptors, which is not enough for long: add a Decoy Beacon or a Nuke Jammer, "
				+ "then build your own Nuke Builder and answer. Its core is under a Cloak Projector: take that out first, or fly the nuke in yourself.");
	}

	// ------------------------------------------------------------------------------------------------------------------------
	// Leviathan Strait
	// ------------------------------------------------------------------------------------------------------------------------

	static void strait(Fi out) throws Exception{
		begin(220, 220, 23, Blocks.sand);
		int px = 52, py = 52, ex = 166, ey = 166;
		//Land is where this is above zero: the two big islands and three small ones with the rare ores.
		int[][] isles = {{px, py, 36}, {ex, ey, 40}, {110, 110, 13}, {56, 158, 13}, {160, 58, 13}};
		for(int x = 0; x < w; x++){
			for(int y = 0; y < h; y++){
				float land = -1f;
				for(int[] isle : isles){
					land = Math.max(land, 1f - Mathf.dst(x, y, isle[0], isle[1]) / isle[2]);
				}
				land += (fbm(x, y, 16f, 0) - 0.5f) * 0.3f;
				Block floor, wall = Blocks.air;
				if(land > 0.08f){
					floor = fbm(x, y, 20f, 1) > 0.55f ? Blocks.grass : Blocks.sand;
					if(land < 0.5f && fbm(x, y, 9f, 2) > 0.7f) wall = Blocks.sandWall;
				}else if(land > 0f){
					floor = Blocks.sandWater;
				}else if(land > -0.22f){
					floor = Blocks.water;
				}else{
					floor = Blocks.deepwater;
				}
				set(x, y, floor, wall);
			}
		}
		ground = Blocks.sand;
		clear(px, py, 20);
		clear(ex, ey, 30);
		ore(px - 12, py - 10, 6, Blocks.oreCopper);
		ore(px - 12, py + 10, 6, Blocks.oreLead);
		ore(px + 12, py - 14, 5, Blocks.oreCoal);
		ore(px + 14, py + 17, 5, Blocks.oreTitanium);
		ore(px, py + 20, 5, Blocks.oreScrap);
		ore(110, 110, 7, Blocks.oreThorium);
		ore(56, 158, 7, Blocks.oreTitanium);
		ore(160, 58, 7, Blocks.oreThorium);
		Vars.world.endMapLoad();

		put(Blocks.coreFoundation, px, py, 0, player);
		defence(px + 4, py - 7);

		put(Blocks.coreNucleus, ex + 6, ey + 6, 0, enemy);
		//A nuke hurts its own side too, so the kinds here are ones that leave the Serpent and the King Crab alone (an EMP kills what flies).
		//The two that have to be stopped before they finish: the Serpent hatches first, the King Crab long after.
		factory(B("serpent-nest"), ex - 12, ey + 12, U("serpent"));
		factory(B("crab-dock"), ex + 12, ey - 14, U("king-crab"));
		silo(ex - 16, ey - 6, B("orbital-launcher"), U("nuke-quake"));
		silo(ex - 6, ey - 20, B("nuke-launcher"), U("nuke-swarm"));
		silo(ex - 10, ey + 24, B("nuke-launcher"), U("nuke-incendiary"));
		put(B("nuke-jammer"), ex + 2, ey - 4, 0, enemy);
		put(B("tracking-radar"), ex + 14, ey + 14, 0, enemy);
		put(B("nuke-interceptor"), ex - 2, ey + 10, 0, enemy);
		//The shore that faces the player.
		for(int i = 0; i < 7; i++){
			float a = 180f + i * 15f;
			int tx = ex + Math.round(Mathf.cosDeg(a) * 26f), ty = ey + Math.round(Mathf.sinDeg(a) * 26f);
			put(i % 3 == 1 ? Blocks.cyclone : Blocks.ripple, tx, ty, 0, enemy);
		}
		put(Blocks.spectre, ex - 4, ey - 4, 0, enemy);
		put(Blocks.cyclone, ex + 18, ey + 2, 0, enemy);
		put(Blocks.cyclone, ex + 2, ey + 20, 0, enemy);
		factory(Blocks.airFactory, ex + 18, ey - 2, UnitTypes.flare);
		factory(Blocks.airFactory, ex + 20, ey + 10, UnitTypes.flare);

		Rules rules = rules(0.12f, 60f * 180f, 60f * 300f);
		rules.loadout = ItemStack.list(Items.copper, 4000, Items.lead, 4000, Items.graphite, 2500, Items.silicon, 3500, Items.titanium, 2500,
			Items.thorium, 1500, Items.metaglass, 1500, Items.plastanium, 500, Items.phaseFabric, 300, Items.surgeAlloy, 500);
		//Only what flies: nothing else gets off the island.
		rules.spawns = Seq.with(
			group(UnitTypes.flare, 1, 1, 3, 1f, 16),
			group(UnitTypes.horizon, 2, 2, 2, 2f, 12),
			group(UnitTypes.zenith, 6, 3, 1, 3f, 8),
			group(UnitTypes.antumbra, 14, 5, 1, 6f, 3)
		);
		write(out, "leviathan-strait", "Leviathan Strait",
			"[accent]Attack.[] Islands and deep water. The enemy island has a Serpent Nest and a Crab Dock at work: the Serpent hatches after about "
				+ "twenty-five minutes, the King Crab after about forty, and its Orbital Launcher throws Quake nukes that only a radar "
				+ "brings back into reach. The Naval Factory builds Nuke Carriers; the small islands have the thorium and titanium.");
	}

	// ------------------------------------------------------------------------------------------------------------------------
	// Event Horizon
	// ------------------------------------------------------------------------------------------------------------------------

	static void horizon(Fi out) throws Exception{
		begin(200, 200, 37, Blocks.basalt);
		int px = 100, py = 34, ex = 100, ey = 162;
		for(int x = 0; x < w; x++){
			for(int y = 0; y < h; y++){
				float n = fbm(x, y, 24f, 0);
				Block floor = n > 0.6f ? Blocks.darksand : n < 0.34f ? Blocks.hotrock : Blocks.basalt;
				Block wall = Blocks.air;
				float edge = Math.min(Math.min(x, w - 1 - x), Math.min(y, h - 1 - y));
				if(fbm(x, y, 12f, 1) + Mathf.clamp(1f - edge / 8f) * 0.5f > 0.7f) wall = Blocks.duneWall;
				//Two long rock arms that leave a way round each end and one through the middle.
				boolean arm = Math.abs(y - (98f + (fbm(x, y, 30f, 2) - 0.5f) * 18f)) < 4f && Math.abs(x - 100) > 9 && x > 24 && x < w - 24;
				if(arm) wall = Blocks.duneWall;
				set(x, y, floor, wall);
			}
		}
		clear(px, py, 26);
		clear(ex, ey, 34);
		clear(100, 98, 10);
		//The fortress stands on metal.
		for(int x = ex - 30; x <= ex + 30; x++){
			for(int y = ey - 26; y <= ey + 26; y++){
				Tile tile = Vars.world.tile(x, y);
				if(tile != null) tile.setFloor((Floor)(((x + y) & 3) == 0 ? Blocks.darkPanel3 : Blocks.metalFloor));
			}
		}
		ore(px - 16, py - 10, 6, Blocks.oreCopper);
		ore(px + 16, py - 10, 6, Blocks.oreLead);
		ore(px - 20, py + 10, 6, Blocks.oreTitanium);
		ore(px + 20, py + 10, 6, Blocks.oreThorium);
		ore(px, py - 22, 5, Blocks.oreCoal);
		ore(40, 100, 6, Blocks.oreThorium);
		ore(160, 100, 6, Blocks.oreTitanium);
		Vars.world.endMapLoad();

		put(Blocks.coreNucleus, px, py, 0, player);
		defence(px - 9, py + 8);
		//Omni-matter to mine, either side of the base and in the middle of the map.
		deposit(px - 30, py + 4, 4);
		deposit(px + 30, py + 4, 4);
		deposit(100, 84, 5);

		put(Blocks.coreNucleus, ex, ey + 12, 0, enemy);
		factory(B("gravity-dock"), ex - 16, ey + 8, U("gravity-base"));
		factory(B("modular-foundry"), ex + 16, ey + 8, U("chimera"));
		silo(ex - 22, ey - 8, B("nuke-launcher"), U("nuke-scatter"));
		silo(ex + 14, ey - 8, B("nuke-launcher"), U("nuke-emp"));
		silo(ex - 6, ey - 2, B("multi-nuke-launcher"), U("nuke-cluster"));
		put(B("cloak-projector"), ex - 6, ey + 14, 0, enemy);
		put(B("nuke-interceptor"), ex + 8, ey + 16, 0, enemy);
		put(B("decoy-beacon"), ex, ey + 24, 0, enemy);
		put(B("tracking-radar"), ex + 8, ey + 4, 0, enemy);
		//Omni walls right round it, open at the front, and two shields that hold as long as their omni-matter lasts.
		wallLine(ex - 30, ey - 22, ex + 30, ey - 22, B("omni-wall"), ex - 4, ex + 4);
		wallLine(ex - 30, ey - 21, ex - 30, ey + 26, B("omni-wall"), -1, -1);
		wallLine(ex + 30, ey - 21, ex + 30, ey + 26, B("omni-wall"), -1, -1);
		wallLine(ex - 29, ey + 26, ex + 29, ey + 26, B("omni-wall"), -1, -1);
		for(int side = -1; side <= 1; side += 2){
			Building shield = put(B("omni-shield"), ex + side * 14, ey - 16, 0, enemy);
			if(shield != null && shield.items != null) shield.items.add(omni(), Math.min(30, shield.block.itemCapacity));
			put(Blocks.spectre, ex + side * 24, ey - 17, 0, enemy);
			put(Blocks.ripple, ex + side * 9, ey - 18, 0, enemy);
			put(Blocks.cyclone, ex + side * 25, ey - 2, 0, enemy);
			put(Blocks.cyclone, ex + side * 25, ey + 18, 0, enemy);
			put(Blocks.meltdown, ex + side * 5, ey - 12, 0, enemy);
		}

		Rules rules = rules(0.1f, 60f * 150f, 60f * 300f);
		rules.loadout = ItemStack.list(Items.copper, 5000, Items.lead, 5000, Items.graphite, 3000, Items.silicon, 5000, Items.titanium, 4000,
			Items.thorium, 3000, Items.metaglass, 1500, Items.plastanium, 1500, Items.phaseFabric, 1000, Items.surgeAlloy, 1500,
			omni(), 400);
		//Chimera frames of the enemy team fit themselves out with two arms, a core and a hull.
		rules.spawns = Seq.with(
			group(UnitTypes.dagger, 1, 1, 2, 2f, 12),
			group(UnitTypes.fortress, 3, 2, 1, 3f, 8),
			group(UnitTypes.zenith, 5, 3, 1, 3f, 8),
			group(U("chimera"), 6, 4, 1, 12f, 3),
			group(UnitTypes.scepter, 16, 6, 1, 0f, 1)
		);
		write(out, "event-horizon", "Event Horizon",
			"[accent]Attack.[] A fortress behind omni walls and two omni shields. Its Gravity Dock is building a Gravity Base (about three quarters of an hour), its "
				+ "Modular Foundry turns out Chimeras, and its launchers throw Scatter, EMP and Cluster nukes. "
				+ "There are omni-matter deposits by your base and in the middle of the map: enough for omni walls, an Omni Shield, a Chimera of "
				+ "your own or the Anti-Matter Cannon.");
	}

	// ------------------------------------------------------------------------------------------------------------------------
	// Terrain
	// ------------------------------------------------------------------------------------------------------------------------

	static void begin(int width, int height, int mapSeed, Block base){
		//What Logic.reset does, without its event: the mod listens for it with an input handler that needs a player.
		Vars.state.rules = new Rules();
		Vars.state.teams = new Teams();
		Vars.state.wave = 1;
		Vars.state.wavetime = 0f;
		Vars.state.tick = 0;
		Groups.clear();
		w = width;
		h = height;
		seed = mapSeed;
		ground = base;
		Vars.world.beginMapLoad();
		Vars.world.resize(w, h);
	}

	static void set(int x, int y, Block floor, Block wall){
		Vars.world.tiles.set(x, y, new Tile(x, y, floor, Blocks.air, wall));
	}

	/** Open, dry ground in a rough circle. */
	static void clear(int cx, int cy, int radius){
		for(int x = cx - radius; x <= cx + radius; x++){
			for(int y = cy - radius; y <= cy + radius; y++){
				Tile tile = Vars.world.tile(x, y);
				if(tile == null || Mathf.dst(x, y, cx, cy) > radius - fbm(x, y, 6f, 9) * 3f) continue;
				if(tile.block() != Blocks.air) tile.setBlock(Blocks.air);
				if(tile.floor().isLiquid) tile.setFloor((Floor)ground);
			}
		}
	}

	/** A patch of ore on whatever dry, open ground is there. */
	static void ore(int cx, int cy, int radius, Block ore){
		for(int x = cx - radius; x <= cx + radius; x++){
			for(int y = cy - radius; y <= cy + radius; y++){
				Tile tile = Vars.world.tile(x, y);
				if(tile == null || tile.floor().isLiquid || tile.block() != Blocks.air) continue;
				if(Mathf.dst(x, y, cx, cy) <= radius * (0.6f + fbm(x, y, 5f, 7 + cx) * 0.6f)) tile.setOverlay(ore);
			}
		}
	}

	/** A patch of full omni-matter deposits; how much a tile holds is in its data byte. */
	static void deposit(int cx, int cy, int radius){
		for(int x = cx - radius; x <= cx + radius; x++){
			for(int y = cy - radius; y <= cy + radius; y++){
				Tile tile = Vars.world.tile(x, y);
				if(tile == null || tile.build != null || Mathf.dst(x, y, cx, cy) > radius * (0.55f + fbm(x, y, 4f, 5) * 0.6f)) continue;
				tile.setBlock(B("omni-matter-deposit"));
				tile.data = (byte)220;
			}
		}
	}

	static float hash(int x, int y, int s){
		int n = x * 374761393 + y * 668265263 + (s + seed * 31) * 1442695041;
		n = (n ^ (n >>> 13)) * 1274126177;
		return ((n ^ (n >>> 16)) & 0xffff) / 65535f;
	}

	static float smooth(float x, float y, int s){
		int ix = Mathf.floor(x), iy = Mathf.floor(y);
		float fx = x - ix, fy = y - iy;
		fx = fx * fx * (3f - 2f * fx);
		fy = fy * fy * (3f - 2f * fy);
		return Mathf.lerp(Mathf.lerp(hash(ix, iy, s), hash(ix + 1, iy, s), fx), Mathf.lerp(hash(ix, iy + 1, s), hash(ix + 1, iy + 1, s), fx), fy);
	}

	/** Smooth noise between 0 and 1, three octaves. */
	static float fbm(int x, int y, float scale, int s){
		return smooth(x / scale, y / scale, s) * 0.57f + smooth(x * 2f / scale, y * 2f / scale, s + 100) * 0.29f + smooth(x * 4f / scale, y * 4f / scale, s + 200) * 0.14f;
	}

	// ------------------------------------------------------------------------------------------------------------------------
	// Structures
	// ------------------------------------------------------------------------------------------------------------------------

	/** Places a block centred on the tile, on cleared dry ground, with ammo if it is a turret that takes items. Null if it does not fit. */
	static Building put(Block block, int x, int y, int rotation, Team team){
		int low = -(block.size - 1) / 2, high = block.size / 2;
		for(int dx = low; dx <= high; dx++){
			for(int dy = low; dy <= high; dy++){
				Tile tile = Vars.world.tile(x + dx, y + dy);
				if(tile == null || tile.build != null){
					System.out.println("! no room for " + block.name + " at " + x + "," + y);
					return null;
				}
			}
		}
		for(int dx = low; dx <= high; dx++){
			for(int dy = low; dy <= high; dy++){
				Tile tile = Vars.world.tile(x + dx, y + dy);
				if(tile.block() != Blocks.air) tile.setBlock(Blocks.air);
				if(tile.floor().isLiquid) tile.setFloor((Floor)ground);
				tile.setOverlay(Blocks.air);
			}
		}
		Tile tile = Vars.world.tile(x, y);
		tile.setBlock(block, team, rotation);
		Building build = tile.build;
		if(build != null && block instanceof ItemTurret){
			Item ammo = ((ItemTurret)block).ammoTypes.keys().toSeq().first();
			for(int i = 0; i < 40 && build.acceptItem(build, ammo); i++) build.handleItem(build, ammo);
		}
		return build;
	}

	/**
	 * What the player starts with against nukes, since one that gets through takes the core with it: a Tracking Radar and two Nuke
	 * Interceptors on a field of solar panels. Structures that touch share power, so it needs no nodes. The tile is its lower left corner;
	 * it is 18 tiles wide and 15 high.
	 */
	static void defence(int ox, int oy){
		for(int i = 0; i < 6; i++){
			for(int j = 0; j < 4; j++){
				put(Blocks.largeSolarPanel, ox + 1 + i * 3, oy + 1 + j * 3, 0, player);
			}
		}
		put(B("nuke-interceptor"), ox + 1, oy + 13, 0, player);
		put(Blocks.batteryLarge, ox + 4, oy + 13, 0, player);
		put(B("tracking-radar"), ox + 7, oy + 13, 0, player);
		put(B("tracking-radar"), ox + 10, oy + 13, 0, player);
		put(Blocks.batteryLarge, ox + 13, oy + 13, 0, player);
		put(B("nuke-interceptor"), ox + 16, oy + 13, 0, player);
	}

	/** A unit factory of the enemy's, set to make this unit. */
	static Building factory(Block block, int x, int y, UnitType type){
		Building build = put(block, x, y, 0, enemy);
		if(build instanceof UnitFactory.UnitFactoryBuild){
			int plan = ((UnitFactory)block).plans.indexOf(p -> p.unit == type);
			if(plan < 0) throw new IllegalArgumentException(block.name + " does not make " + type.name);
			((UnitFactory.UnitFactoryBuild)build).currentPlan = plan;
		}
		return build;
	}

	/** A Nuke Builder with a launcher in front of it (to its right), so that what it makes rolls straight in. The tile is the builder's. */
	static void silo(int x, int y, Block launcher, UnitType nuke){
		factory(B("nuke-builder"), x, y, nuke);
		//The builder's right edge is two tiles from its centre tile; the launcher's left edge is (size - 1) / 2 from its own.
		put(launcher, x + 3 + (launcher.size - 1) / 2, y, 0, enemy);
	}

	/** A straight line of walls of the enemy's, with a gap between two coordinates along it (both -1 for none). */
	static void wallLine(int x1, int y1, int x2, int y2, Block wall, int gapFrom, int gapTo){
		boolean vertical = x1 == x2;
		int length = vertical ? y2 - y1 : x2 - x1;
		for(int i = 0; i <= length; i += wall.size){
			int x = vertical ? x1 : x1 + i, y = vertical ? y1 + i : y1;
			int along = vertical ? y : x;
			if(along + wall.size > gapFrom && along - wall.size < gapTo) continue;
			put(wall, x, y, 0, enemy);
		}
	}

	// ------------------------------------------------------------------------------------------------------------------------
	// Rules and writing
	// ------------------------------------------------------------------------------------------------------------------------

	/**
	 * Attack rules: the game ends when the enemy's cores are gone, waves come out of them, and the enemy needs neither items nor power.
	 * Its build speed is what paces its nukes, the Serpent, the King Crab and the Gravity Base.
	 */
	static Rules rules(float enemyBuildSpeed, float waveSpacing, float firstWave){
		Rules rules = Vars.state.rules;
		rules.attackMode = true;
		rules.waves = true;
		rules.waveTimer = true;
		rules.wavesSpawnAtCores = true;
		rules.waveSpacing = waveSpacing;
		rules.initialWaveSpacing = firstWave;
		rules.waveTeam = enemy;
		rules.defaultTeam = player;
		rules.enemyCoreBuildRadius = 55f * 8f;
		Rules.TeamRule rule = rules.teams.get(enemy);
		rule.cheat = true;
		rule.infiniteResources = true;
		rule.unitBuildSpeedMultiplier = enemyBuildSpeed;
		return rules;
	}

	/** A wave group: from this wave on, every so many waves, this many, one more every {@code scaling} waves (0 for never), up to a most. */
	static SpawnGroup group(UnitType type, int begin, int spacing, int amount, float scaling, int max){
		SpawnGroup group = new SpawnGroup(type);
		group.begin = begin;
		group.spacing = spacing;
		group.unitAmount = amount;
		group.unitScaling = scaling <= 0f ? SpawnGroup.never : scaling;
		group.max = max;
		return group;
	}

	static void write(Fi out, String file, String name, String description) throws Exception{
		int enemyBlocks = Vars.state.teams.get(enemy).buildings.size, cores = Vars.state.teams.get(enemy).cores.size;
		if(Vars.state.teams.get(player).cores.size != 1 || cores < 1) throw new IllegalStateException(name + ": cores missing");
		Fi target = out.child(file + ".msav");
		StringMap tags = StringMap.of("name", name, "author", "New Horizon", "description", description);
		MapIO.writeMap(target, new mindustry.maps.Map(target, w, h, tags, true));
		System.out.println(name + ": " + w + "x" + h + ", " + enemyBlocks + " enemy structures, " + target.length() + " bytes -> " + target.path());
	}
}
