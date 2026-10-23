package newhorizon.expand.block.env;

import arc.Events;
import arc.math.Mathf;
import arc.func.Cons;
import arc.struct.IntFloatMap;
import arc.struct.IntSeq;
import arc.struct.IntSet;
import arc.util.Time;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.entities.Units;
import mindustry.game.EventType;
import mindustry.gen.Groups;
import mindustry.gen.Legsc;
import mindustry.gen.Minerc;
import mindustry.type.Item;
import mindustry.world.Tile;
import mindustry.world.Block;
import mindustry.world.meta.BuildVisibility;
import newhorizon.content.nuke.NukeItems;
import newhorizon.content.nuke.OmniBlocks;
import newhorizon.expand.block.omni.OmniStabilizer;

import static mindustry.Vars.tilesize;

/**
 * A deposit that runs out, built like the wall ores of Erekir: a solid block that sits on top of whatever the ground has (floor, ore
 * overlay) and is mined by units. It has no structure of its own, so nothing is lost underneath: when a tile is used up the block is
 * taken away and the ground below, ore and all, is just as it was. How much a tile has left is kept in the tile's data byte (which the
 * game saves with the map), up to {@link #cap}.
 *
 * <p>A deposit can be laid over water. It is never laid where a building stands or a ground unit is (a solid block would trap it):
 * a siphon orb has been striking everything in its reach the whole time it lived, so little is left standing there by the time it ends.
 *
 * <p>The game does not say when a unit has mined an item, so {@link #track} watches every mining unit: each time the mining timer falls
 * back to zero on a deposit tile an item has been mined and one is taken from the tile. Only units can mine it (the ground under it is
 * out of reach of a drill, and nothing can be built on it). Only the server counts.
 *
 * <p>A deposit wears away by itself: every tile loses one every {@link #decaySeconds} seconds, each at a different second of the minute
 * (by where it is), until it is used up like a mined-out tile. A powered {@link OmniStabilizer} in reach stops that. To find the tiles
 * without searching the map, the positions of the deposits are kept in a set that is filled when a deposit is laid and when a map is loaded.
 */
public class OmniDeposit extends Block{
	/** The most one tile can hold. */
	public static final int cap = 255;

	/** Seconds one tile takes to lose one item. */
	public static int decaySeconds = 60;

	/** Where the deposits are (tile positions). */
	private static final IntSet positions = new IntSet();
	private static final IntSeq gone = new IntSeq(), due = new IntSeq();
	private static float clock;
	private static int lastSecond;

	/** The mining timer of every unit that was working a deposit a moment ago, by unit id. */
	private static final IntFloatMap timers = new IntFloatMap();
	private static boolean hooked;

	public OmniDeposit(String name, Item item){
		super(name);
		itemDrop = item;
		variants = 3;
		solid = true;
		//A solid block that fills its tile is treated by the game as a wall of the terrain, whose tile data is how dark it is drawn and how
		//deep legged units are kept out: the amount left is kept in that same byte, so it must not be taken for one.
		fillsTile = false;
		destructible = false;
		update = false;
		breakable = false;
		alwaysReplace = false;
		targetable = false;
		buildVisibility = BuildVisibility.hidden;
		mapColor.set(item.color);
	}

	public static boolean isDeposit(Tile tile){
		return tile != null && tile.block() instanceof OmniDeposit;
	}

	/** How many items the tile has left. */
	public static int remaining(Tile tile){
		return isDeposit(tile) ? tile.data & 0xff : 0;
	}

	/** Takes one item out of the tile; when it is empty the deposit goes and the ground under it is left as it was. Server side. */
	public static void mined(Tile tile){
		if(!isDeposit(tile)) return;
		int left = remaining(tile) - 1;
		if(left <= 0){
			positions.remove(tile.pos());
			tile.setNet(Blocks.air);
			tile.data = 0;
		}else{
			tile.data = (byte)left;
		}
	}

	/** Whether a solid block would trap this unit: it is on the ground and not a walker with legs, which step over blocks. */
	private static boolean crushable(mindustry.gen.Unit unit){
		return !unit.dead && unit.isGrounded() && !(unit instanceof Legsc);
	}

	/** Runs for every unit a solid block on this tile would trap. */
	private static void crushables(Tile tile, Cons<mindustry.gen.Unit> cons){
		Units.nearby(tile.worldx() - tilesize / 2f, tile.worldy() - tilesize / 2f, tilesize, tilesize, unit -> {
			if(crushable(unit)) cons.get(unit);
		});
	}

	/** Whether a deposit can be laid on the tile: bare ground, water included, not a place enemies come from, nobody it would trap. */
	public static boolean free(Tile tile){
		if(tile == null || tile.block() != Blocks.air || tile.overlay() == Blocks.spawn) return false;
		boolean[] trapped = {false};
		crushables(tile, unit -> trapped[0] = true);
		return !trapped[0];
	}

	/**
	 * Adds to a tile: onto a deposit it adds to what is there, onto free ground it starts one. A tile holds at most {@link #cap}; what does
	 * not fit goes to the tiles around it, as far as they have room. Returns how much was not placed.
	 */
	private static int add(Tile tile, int amount, boolean spill){
		if(tile == null || amount <= 0) return 0;
		int have;
		if(isDeposit(tile)) have = remaining(tile);
		else if(free(tile)){
			tile.setNet(NukeItems.omniDeposit);
			positions.add(tile.pos());
			have = 0;
		}else return amount;

		int put = Math.min(amount, cap - have);
		tile.data = (byte)(have + put);
		int rest = amount - put;
		if(rest > 0 && spill){
			for(int[] d : new int[][]{{1, 0}, {0, 1}, {-1, 0}, {0, -1}}){
				rest = add(Vars.world.tile(tile.x + d[0], tile.y + d[1]), rest, false);
				if(rest <= 0) break;
			}
		}
		return rest;
	}

	/**
	 * What a siphon orb leaves where it ended, after it had lived {@code age} ticks (and pulled, which it does from thirty seconds on).
	 * The longer it lived the wider the patch and the more each tile holds. Over an existing patch it adds to the tiles that are
	 * there, and the patch spreads: free ground next to a deposit within a tile of the disc becomes deposit too. It goes over other ore, which
	 * comes back when the deposit is used up, and over water, but not where something stands. Server side.
	 */
	public static void leave(float x, float y, float age){
		if(Vars.net.client()) return;
		float t = Mathf.clamp((age - 60f * 30f) / (60f * 270f));
		float radius = 1.5f + 5.5f * t;
		float centre = 10f + 90f * t;

		int cx = Vars.world.toTile(x), cy = Vars.world.toTile(y);
		int reach = Mathf.ceil(radius) + 1;
		//The deposits that were there before, so that the patch spreads from them and not from what this orb lays itself.
		IntSet before = new IntSet();
		for(int dx = -reach - 1; dx <= reach + 1; dx++){
			for(int dy = -reach - 1; dy <= reach + 1; dy++){
				Tile tile = Vars.world.tile(cx + dx, cy + dy);
				if(isDeposit(tile)) before.add(tile.pos());
			}
		}

		for(int dx = -reach; dx <= reach; dx++){
			for(int dy = -reach; dy <= reach; dy++){
				Tile tile = Vars.world.tile(cx + dx, cy + dy);
				if(tile == null) continue;
				float d = Mathf.dst(dx, dy);
				float noise = Mathf.random(0.7f, 1.3f);
				if(d <= radius){
					add(tile, Math.max(1, Math.round(centre * Mathf.lerp(1f, 0.33f, d / radius) * noise)), true);
				}else if(d <= radius + 1f && free(tile) && nextToDeposit(tile, before)){
					add(tile, Math.max(1, Math.round(centre * 0.25f * noise)), false);
				}
			}
		}
	}

	private static boolean nextToDeposit(Tile tile, IntSet before){
		for(int[] d : new int[][]{{1, 0}, {0, 1}, {-1, 0}, {0, -1}}){
			Tile other = Vars.world.tile(tile.x + d[0], tile.y + d[1]);
			if(other != null && before.contains(other.pos())) return true;
		}
		return false;
	}

	/** Starts watching the miners. */
	public static void hook(){
		if(hooked) return;
		hooked = true;
		Events.run(EventType.Trigger.update, OmniDeposit::track);
		Events.run(EventType.Trigger.update, OmniDeposit::decay);
		Events.on(EventType.ResetEvent.class, e -> {
			timers.clear();
			positions.clear();
		});
		Events.on(EventType.WorldLoadEvent.class, e -> {
			timers.clear();
			scan();
		});
	}

	/** Finds every deposit on the map again, after it was loaded. */
	public static void scan(){
		positions.clear();
		clock = 0f;
		lastSecond = 0;
		if(Vars.world == null || Vars.world.tiles == null) return;
		for(Tile tile : Vars.world.tiles){
			if(isDeposit(tile)) positions.add(tile.pos());
		}
	}

	/** Notes a deposit that was put on the map in some other way than by {@link #leave}. */
	public static void register(Tile tile){
		if(isDeposit(tile)) positions.add(tile.pos());
	}

	/** How many deposit tiles are known. */
	public static int known(){
		return positions.size;
	}

	/** Once a second, the tiles whose turn it is lose one, unless a stabilizer that has power reaches them. Server side. */
	private static void decay(){
		if(Vars.net.client() || positions.size == 0) return;
		clock += Time.delta;
		int second = (int)(clock / 60f);
		if(second == lastSecond) return;
		lastSecond = second;
		decayStep(second);
	}

	/** The tiles whose second it is when the clock reads this second. */
	public static void decayStep(int second){
		arc.struct.Seq<mindustry.gen.Building> stabilizers = null;
		gone.clear();
		due.clear();
		for(IntSet.IntSetIterator it = positions.iterator(); it.hasNext;){
			int pos = it.next();
			Tile tile = Vars.world.tile(pos);
			if(!isDeposit(tile)){
				gone.add(pos);
				continue;
			}
			if(Math.floorMod(second + pos, Math.max(1, decaySeconds)) != 0) continue;
			if(stabilizers == null) stabilizers = OmniStabilizer.all(OmniBlocks.omniStabilizer);
			if(OmniStabilizer.covers(stabilizers, tile)) continue;
			due.add(pos);
		}
		for(int i = 0; i < gone.size; i++) positions.remove(gone.get(i));
		//Taking one off may use a tile up and drop it from the set, which is not done while the set is being walked.
		for(int i = 0; i < due.size; i++) mined(Vars.world.tile(due.get(i)));
	}

	private static void track(){
		if(Vars.net.client()) return;
		boolean any = false;
		for(int i = 0; i < Groups.unit.size(); i++){
			mindustry.gen.Unit unit = Groups.unit.index(i);
			if(!(unit instanceof Minerc)) continue;
			Minerc miner = (Minerc)unit;
			Tile tile = miner.mineTile();
			if(!isDeposit(tile)){
				if(timers.size > 0) timers.remove(unit.id, 0f);
				continue;
			}
			any = true;
			float now = miner.mineTimer(), prev = timers.get(unit.id, 0f);
			timers.put(unit.id, now);
			//The timer climbs to a limit and goes back to zero each time an item is mined; a unit that only switched tiles has not got far.
			Item item = tile.wallDrop();
			float limit = 50f + (unit.type.mineHardnessScaling && item != null ? item.hardness * 15f : 15f);
			if(now < prev && prev > limit * 0.5f) mined(tile);
		}
		if(!any && timers.size > 0) timers.clear();
	}
}
