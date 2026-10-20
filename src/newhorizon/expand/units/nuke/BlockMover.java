package newhorizon.expand.units.nuke;

import arc.Events;
import arc.math.Mathf;
import arc.struct.Queue;
import arc.struct.Seq;
import arc.util.io.Reads;
import arc.util.io.ReusableByteOutStream;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.entities.Units;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.ConstructBlock;
import mindustry.world.blocks.ControlBlock;
import mindustry.world.blocks.defense.turrets.Turret;
import mindustry.world.blocks.liquid.LiquidRouter;
import mindustry.world.blocks.payloads.PayloadBlock;
import mindustry.world.blocks.power.Battery;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.blocks.storage.StorageBlock;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import static mindustry.Vars.tilesize;

/**
 * Moves structures about for the nukes that rearrange a base (scatter, magnet) or rebuild part of it (scramble). Moving a structure is one
 * network message to take it away and one to put it down, and every tile change makes the game recalculate paths and neighbours, so the
 * moves are queued and done a few dozen a tick instead of all in the same moment.
 *
 * <p>Most structures (walls, conveyors, ...) come out as fresh ones of the same kind. Those that hold something worth keeping (turrets with
 * their ammo, containers and vaults with their items, tanks with their liquid, batteries with their charge, payload blocks with what they
 * carry) are saved the way the game saves them and loaded back into the new structure, so they keep it all. What the other players see
 * of such a structure's contents is whatever the game sends for it; the server has all of it.
 */
public class BlockMover{
	/** How many queued jobs are done in a tick. */
	public static int perTick = 24;

	private static final Queue<Runnable> queue = new Queue<>();
	private static boolean hooked;
	private static final Seq<Tile> tmpTiles = new Seq<>();

	/** Queues something to be done on a later tick. Server side. */
	public static void enqueue(Runnable job){
		hook();
		queue.addLast(job);
	}

	public static int pending(){
		return queue.size;
	}

	/** Does every queued job at once. For tests, and for anything that cannot wait. */
	public static void flush(){
		while(queue.size > 0) run(queue.removeFirst());
	}

	private static void run(Runnable job){
		try{
			job.run();
		}catch(Throwable t){
			//One structure that cannot be moved must not stop the rest.
			arc.util.Log.err("A block mover job failed", t);
		}
	}

	private static void hook(){
		if(hooked) return;
		hooked = true;
		Events.run(EventType.Trigger.update, () -> {
			if(Vars.net.client()) return;
			for(int i = 0; i < perTick && queue.size > 0; i++) run(queue.removeFirst());
		});
		Events.on(EventType.ResetEvent.class, e -> queue.clear());
		Events.on(EventType.WorldLoadEvent.class, e -> queue.clear());
	}

	/** Whether this structure can be picked up and put down somewhere else: not cores, not unfinished ones, not ones a player is flying. */
	public static boolean movable(Building build){
		if(build == null || !build.isValid() || build.tile == null || build.team == Team.derelict) return false;
		if(build.block instanceof CoreBlock || build.block instanceof ConstructBlock) return false;
		return !(build instanceof ControlBlock && ((ControlBlock)build).isControlled());
	}

	/** Whether the structure holds something that is carried over, instead of coming out fresh. */
	public static boolean keepsState(Building build){
		Block block = build.block;
		return block instanceof Turret || block instanceof StorageBlock || block instanceof LiquidRouter || block instanceof Battery || block instanceof PayloadBlock;
	}

	/**
	 * Whether {@code block} fits with its origin on the tile: every tile it would cover is free (or covered by {@code ignore}, the structure
	 * that is about to move), buildable ground, and nothing is standing where a solid block would go.
	 */
	public static boolean fits(Block block, Tile origin, Building ignore){
		if(origin == null) return false;
		tmpTiles.clear();
		origin.getLinkedTilesAs(block, tmpTiles);
		if(tmpTiles.isEmpty()) return false;
		for(Tile t : tmpTiles){
			if(t == null) return false;
			if(t.build != null && t.build != ignore) return false;
			if(t.build == null && t.block() != Blocks.air) return false;
			if(!t.floor().placeableOn) return false;
			if(t.floor().isDeep() && !block.floating && !block.placeableLiquid && !block.requiresWater) return false;
		}
		if(block.solid){
			float size = block.size * tilesize;
			float cx = origin.worldx() + block.offset, cy = origin.worldy() + block.offset;
			if(Units.anyEntities(cx - size / 2f, cy - size / 2f, size, size, true)) return false;
		}
		return true;
	}

	/**
	 * Takes the structure off its place and puts it down with its origin on {@code to}. A structure that keeps state arrives with all of
	 * it. Returns the new structure, or null if nothing was moved. Server side.
	 */
	public static Building move(Building build, Tile to){
		if(!movable(build) || to == null || to == build.tile) return null;
		if(!fits(build.block, to, build)) return null;

		Block block = build.block;
		Team team = build.team;
		int rotation = build.rotation;
		boolean claimed = NukeSpawned.claimed(build);
		byte[] state = keepsState(build) ? save(build) : null;

		build.tile.setNet(Blocks.air);
		to.setNet(block, team, rotation);
		Building now = to.build;
		if(now == null) return null;

		if(state != null) load(now, state);
		if(claimed) NukeSpawned.claim(now);
		return now;
	}

	/** Puts a fresh structure of the same kind, turned the given way, where this one stands. Server side. */
	public static Building rebuild(Building build, int rotation){
		if(!movable(build)) return null;
		Block block = build.block;
		Team team = build.team;
		Tile tile = build.tile;
		boolean claimed = NukeSpawned.claimed(build);
		byte[] state = keepsState(build) ? save(build) : null;

		tile.setNet(block, team, rotation);
		Building now = tile.build;
		if(now == null) return null;
		if(state != null) load(now, state);
		now.rotation = rotation;
		if(claimed) NukeSpawned.claim(now);
		return now;
	}

	/** The structure's state as the game would save it. */
	public static byte[] save(Building build){
		ReusableByteOutStream bytes = new ReusableByteOutStream();
		build.writeAll(new Writes(new DataOutputStream(bytes)));
		return bytes.toByteArray();
	}

	/** Loads a saved state into a new structure of the same kind. */
	public static void load(Building build, byte[] state){
		build.readAll(new Reads(new DataInputStream(new ByteArrayInputStream(state))), build.version());
		//Links to other structures are by position: they do not mean the same thing in a new place.
		if(build.power != null) build.power.links.clear();
	}

	// ------------------------------------------------------------------ the two rearranging nukes

	/** Every movable structure within range of the point, of every team. */
	public static Seq<Building> within(float x, float y, float radius){
		Seq<Building> found = new Seq<>();
		Units.nearbyBuildings(x, y, radius, b -> {
			if(b.dst(x, y) <= radius && movable(b)) found.add(b);
		});
		return found;
	}

	/** Scatter: every structure in range is thrown to a random free place within {@code reach} of the point. Queued. Returns how many were queued. */
	public static int scatter(float x, float y, float radius, float reach){
		Seq<Building> found = within(x, y, radius);
		found.shuffle();
		for(Building b : found){
			enqueue(() -> {
				if(!movable(b)) return;
				for(int tries = 0; tries < 24; tries++){
					float angle = Mathf.random(360f), dst = Mathf.sqrt(Mathf.random()) * reach;
					Tile to = Vars.world.tileWorld(x + Mathf.cosDeg(angle) * dst, y + Mathf.sinDeg(angle) * dst);
					if(to != null && move(b, to) != null) return;
				}
			});
		}
		return found.size;
	}

	/**
	 * Magnet: every structure in range is drawn in towards the point, the nearest first, to the free place nearest the middle that is
	 * closer than where it stands, so that they pack together. Queued. Returns how many were queued.
	 */
	public static int magnet(float x, float y, float radius){
		Seq<Building> found = within(x, y, radius);
		found.sort(b -> b.dst2(x, y));
		//Places in order of distance from the middle, worked out once.
		int r = (int)(radius / tilesize) + 1;
		Seq<int[]> spots = new Seq<>();
		int cx = Vars.world.toTile(x), cy = Vars.world.toTile(y);
		for(int dx = -r; dx <= r; dx++) for(int dy = -r; dy <= r; dy++) if(dx * dx + dy * dy <= r * r) spots.add(new int[]{dx, dy});
		spots.sort(s -> s[0] * s[0] + s[1] * s[1]);

		for(Building b : found){
			enqueue(() -> {
				if(!movable(b)) return;
				float own = b.dst(x, y);
				for(int[] s : spots){
					Tile to = Vars.world.tile(cx + s[0], cy + s[1]);
					if(to == null) continue;
					float d = Mathf.dst(to.worldx() + b.block.offset, to.worldy() + b.block.offset, x, y);
					//Nothing closer is free: it stays.
					if(d >= own - tilesize * 0.5f) return;
					if(move(b, to) != null) return;
				}
			});
		}
		return found.size;
	}
}
