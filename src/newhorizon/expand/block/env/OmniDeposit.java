package newhorizon.expand.block.env;

import arc.Events;
import arc.math.Mathf;
import arc.func.Cons;
import arc.graphics.Color;
import arc.struct.IntFloatMap;
import arc.struct.IntIntMap;
import arc.struct.IntSeq;
import arc.struct.IntSet;
import arc.util.Time;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.entities.Units;
import mindustry.game.EventType;
import mindustry.gen.Building;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Legsc;
import mindustry.gen.Minerc;
import mindustry.type.Item;
import mindustry.world.Tile;
import mindustry.world.Block;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.meta.BuildVisibility;
import newhorizon.content.nuke.NukeItems;

import static mindustry.Vars.tilesize;

/**
 * A deposit that runs out, built like the wall ores of Erekir: a solid block that sits on top of whatever the ground has (floor, ore
 * overlay) and is mined by units. It has no structure of its own, so nothing is lost underneath: when a tile is used up the block is
 * taken away and the ground below, ore and all, is just as it was. How much a tile has left is kept in the tile's data byte (which the
 * game saves with the map), up to {@link #cap}.
 *
 * <p>A deposit can be laid over water, and over buildings and ground units: those tiles are claimed, and while they are claimed whatever
 * stands on them is slowly destroyed (a share of its health every second), so that nothing ends up stuck inside a solid block. When the
 * tile is clear the deposit forms. Cores are never claimed. The claims are only in memory, so they are lost when the game is saved and
 * loaded.
 *
 * <p>The game does not say when a unit has mined an item, so {@link #track} watches every mining unit: each time the mining timer falls
 * back to zero on a deposit tile an item has been mined and one is taken from the tile. Only units can mine it (the ground under it is
 * out of reach of a drill, and nothing can be built on it). Only the server counts.
 */
public class OmniDeposit extends Block{
	/** The most one tile can hold. */
	public static final int cap = 255;

	/** The mining timer of every unit that was working a deposit a moment ago, by unit id. */
	private static final IntFloatMap timers = new IntFloatMap();
	private static boolean hooked;

	/** Tiles claimed for a deposit that cannot form yet because something is in the way, by position: what each will hold, and when it was claimed. */
	private static final IntIntMap claims = new IntIntMap();
	private static final IntFloatMap claimed = new IntFloatMap();
	/** How much of its health a building or a unit on a claimed tile loses each second, and the least that is, so that small things die too. */
	public static float buildingRate = 0.04f, unitRate = 0.05f, minimumDamage = 15f;
	/** Ticks between two rounds of damage, and how long a claim is kept if the tile never clears. */
	public static float claimInterval = 10f, claimLife = 60f * 60f * 5f;
	private static float claimTimer;

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

	/** Whether a deposit can form on the tile this moment: bare ground, water included, not a place enemies come from, nobody it would trap. */
	public static boolean free(Tile tile){
		if(tile == null || tile.block() != Blocks.air || tile.overlay() == Blocks.spawn) return false;
		boolean[] trapped = {false};
		crushables(tile, unit -> trapped[0] = true);
		return !trapped[0];
	}

	/**
	 * Whether a deposit can be laid on the tile, now or once what is in the way has been destroyed: a building (other than a core) or a
	 * ground unit on it. A wall of the terrain, a spawn and a core stay as they are.
	 */
	public static boolean claimable(Tile tile){
		if(tile == null || tile.overlay() == Blocks.spawn) return false;
		if(tile.build != null) return !(tile.build.block instanceof CoreBlock) && !isDeposit(tile);
		return tile.block() == Blocks.air;
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
			have = 0;
		}else if(claimable(tile)){
			//Something is in the way: the tile is claimed, what is on it is destroyed slowly, and the deposit forms when it is clear.
			int pos = tile.pos();
			if(!claims.containsKey(pos)) claimed.put(pos, Time.time);
			claims.put(pos, Math.min(cap, claims.get(pos, 0) + amount));
			return 0;
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
	 * comes back when the deposit is used up, and over buildings and ground units, which are destroyed slowly. Server side.
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
				}else if(d <= radius + 1f && claimable(tile) && nextToDeposit(tile, before)){
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
		Events.run(EventType.Trigger.update, () -> {
			track();
			clear();
		});
		Events.on(EventType.ResetEvent.class, e -> forget());
		Events.on(EventType.WorldLoadEvent.class, e -> forget());
	}

	private static void forget(){
		timers.clear();
		claims.clear();
		claimed.clear();
	}

	/** How many tiles are claimed and waiting for what is on them to be destroyed. */
	public static int claimedTiles(){
		return claims.size;
	}

	/** Every round: damages what stands on the claimed tiles, and lays the deposit on those that are clear. Server side. */
	private static void clear(){
		if(Vars.net.client() || claims.size == 0) return;
		claimTimer += Time.delta;
		if(claimTimer < claimInterval) return;
		float seconds = claimTimer / 60f;
		claimTimer = 0f;

		IntSet hitBuildings = new IntSet(), hitUnits = new IntSet();
		IntSeq done = new IntSeq();
		IntSeq keys = new IntSeq();
		claims.keys().toArray().each(keys::add);
		for(int i = 0; i < keys.size; i++){
			int pos = keys.get(i);
			Tile tile = Vars.world.tile(pos);
			if(tile == null || Time.time - claimed.get(pos, 0f) > claimLife){
				done.add(pos);
				continue;
			}

			if(tile.build != null){
				Building build = tile.build;
				if(build.block instanceof CoreBlock || isDeposit(tile)){
					done.add(pos);
					continue;
				}
				if(hitBuildings.add(build.id)) build.damagePierce(Math.max(minimumDamage, build.maxHealth * buildingRate) * seconds);
			}else if(tile.block() == Blocks.air){
				boolean[] trapped = {false};
				crushables(tile, unit -> {
					trapped[0] = true;
					if(hitUnits.add(unit.id)) unit.damagePierce(Math.max(minimumDamage, unit.maxHealth * unitRate) * seconds);
				});
				if(!trapped[0]){
					int amount = claims.get(pos, 1);
					tile.setNet(NukeItems.omniDeposit);
					tile.data = (byte)Math.min(cap, amount);
					done.add(pos);
					continue;
				}
			}else{
				//Something else has taken the place (a terrain wall, say).
				done.add(pos);
				continue;
			}
			if(Mathf.chance(0.35)) Call.effect(Fx.hitLaserBlast, tile.worldx(), tile.worldy(), 0f, Color.valueOf("c885ff"));
		}
		for(int i = 0; i < done.size; i++){
			claims.remove(done.get(i), 0);
			claimed.remove(done.get(i), 0f);
		}
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
