package newhorizon.expand.map.planet;

import arc.math.Mathf;
import arc.math.Rand;
import arc.struct.IntIntMap;
import arc.struct.IntSeq;
import arc.struct.Seq;
import arc.util.Log;
import mindustry.content.Blocks;
import mindustry.game.Team;
import mindustry.type.Sector;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.Tiles;
import mindustry.world.blocks.units.UnitFactory;
import newhorizon.content.hiemara.HiemaraDefense;

/**
 * The prebuilt enemy bases of Hiemara: a few sectors have an island with an enemy core, turrets, walls, naval mines and a
 * shipyard that makes ships on its own, and are won by destroying the core instead of by surviving waves.
 * <p>
 * Which sectors, and how strong, depends on the sector number and its distance from the start sector only, so a sector
 * always has the same base. The sectors next to the start sector never have one, so the first captures are by waves.
 * <ul>
 * <li>Tier 1, outpost: shard core, arc coils, copper walls, a few mines, a yard making rissos.</li>
 * <li>Tier 2, fortress: foundation core, arc coils, pulse lances, a vortex generator, titanium walls, a yard with an
 * additive reconstructor (minke), more mines.</li>
 * <li>Tier 3, citadel: nucleus core, many turrets, two vortex generators, thorium walls in two rings, a yard with two
 * reconstructors (bryde), a lot of mines.</li>
 * </ul>
 */
public class HiemaraBase{
	/** Share of the eligible sectors that get a base. */
	public static float share = 0.12f;
	/** Sectors with at least this many steps to the start sector can have a base. */
	public static int minDistance = 2;
	
	protected static final IntIntMap distances = new IntIntMap();
	protected static int cachedFor = -1, maxDistance = 1;
	
	/** @return 0 if the sector has no enemy base, else the tier 1-3. */
	public static int tier(Sector sector){
		distances(sector);
		int distance = distances.get(sector.id, -1);
		if(sector.id == sector.planet.startSector || distance < minDistance)return 0;
		if(!new Rand(sector.id * 7919L + 13L).chance(share))return 0;
		//thirds of the way to the furthest sector
		return distance * 3 <= maxDistance ? 1 : distance * 3 <= maxDistance * 2 ? 2 : 3;
	}
	
	/** Number of sector steps from the start sector. */
	protected static void distances(Sector sector){
		if(cachedFor == sector.planet.id && distances.size > 0)return;
		cachedFor = sector.planet.id;
		distances.clear();
		maxDistance = 1;
		
		IntSeq queue = new IntSeq();
		int start = sector.planet.startSector;
		distances.put(start, 0);
		queue.add(start);
		for(int i = 0; i < queue.size; i++){
			Sector current = sector.planet.sectors.get(queue.get(i));
			for(Sector next : current.near()){
				if(distances.containsKey(next.id))continue;
				int distance = distances.get(current.id, 0) + 1;
				distances.put(next.id, distance);
				maxDistance = Math.max(maxDistance, distance);
				queue.add(next.id);
			}
		}
	}
	
	/** Prints which sectors have a base, once all of them are decided. */
	public static void log(Sector last){
		if(last.id != last.planet.sectors.size - 1)return;
		int[] count = new int[4];
		StringBuilder ids = new StringBuilder();
		for(Sector sector : last.planet.sectors){
			int tier = tier(sector);
			count[tier]++;
			if(tier > 0)ids.append(sector.id).append("(T").append(tier).append(") ");
		}
		Log.info("Hiemara enemy bases: @ outposts, @ fortresses, @ citadels: @", count[1], count[2], count[3], ids.toString().trim());
	}
	
	/** Builds the base of a tier around the centre. The centre is moved away from the edges of the map. */
	public static void build(Tiles tiles, int tier, int centreX, int centreY, Rand rand){
		int radius = 8 + 3 * (tier - 1);
		int margin = radius + 16;
		int cx = Mathf.clamp(centreX, margin, tiles.width - margin), cy = Mathf.clamp(centreY, margin, tiles.height - margin);
		Team team = Team.malis;
		
		//the island
		for(int x = cx - radius - 1; x <= cx + radius + 1; x++){
			for(int y = cy - radius - 1; y <= cy + radius + 1; y++){
				Tile tile = tiles.get(x, y);
				if(tile != null && Mathf.within(x, y, cx, cy, radius + 1f)){
					tile.setFloor(Blocks.water.asFloor());
					tile.setOverlay(Blocks.air);
					tile.setBlock(Blocks.air);
				}
			}
		}
		
		//the core
		put(tiles, tier == 1 ? Blocks.coreShard : tier == 2 ? Blocks.coreFoundation : Blocks.coreNucleus, cx, cy, 0, team);
		
		//the shipyard faces east, into the water: yard, then the reconstructors that make better ships of what it builds
		int fx = cx + radius - 2;
		Block yard = put(tiles, Blocks.navalFactory, fx, cy, 0, team);
		if(yard != null && tiles.getn(fx, cy).build instanceof UnitFactory.UnitFactoryBuild){
			((UnitFactory.UnitFactoryBuild)tiles.getn(fx, cy).build).currentPlan = 0;
		}
		if(tier >= 2)put(tiles, Blocks.additiveReconstructor, fx + 3, cy, 0, team);
		if(tier >= 3)put(tiles, Blocks.multiplicativeReconstructor, fx + 7, cy, 0, team);
		
		//turrets on two rings
		Block[] inner = tier == 1 ? new Block[]{HiemaraDefense.arcCoil} : new Block[]{HiemaraDefense.arcCoil, HiemaraDefense.pulseLance};
		ring(tiles, cx, cy, inner, tier == 1 ? 3 : tier == 2 ? 5 : 6, radius * 0.55f, rand.random(360f), team);
		if(tier >= 2){
			put(tiles, HiemaraDefense.vortexGenerator, cx - radius / 2 - 1, cy + radius / 2 + 1, 0, team);
			if(tier >= 3){
				put(tiles, HiemaraDefense.vortexGenerator, cx - radius / 2 - 1, cy - radius / 2 - 2, 0, team);
				ring(tiles, cx, cy, new Block[]{HiemaraDefense.pulseLance, HiemaraDefense.arcCoil}, 6, radius * 0.8f, rand.random(360f), team);
			}
		}
		
		//walls
		Block wall = tier == 1 ? Blocks.copperWallLarge : tier == 2 ? Blocks.titaniumWallLarge : Blocks.thoriumWallLarge;
		ring(tiles, cx, cy, new Block[]{wall}, (int)(Mathf.PI2 * (radius - 0.5f) / 2.1f), radius - 0.5f, 0f, team);
		if(tier >= 3)ring(tiles, cx, cy, new Block[]{wall}, (int)(Mathf.PI2 * (radius + 1.5f) / 2.1f), radius + 1.5f, 0f, team);
		
		//mines in the water around
		int mines = tier == 1 ? 5 : tier == 2 ? 10 : 18;
		for(int i = 0, tries = 0; i < mines && tries < mines * 30; tries++){
			float angle = rand.random(360f), dst = rand.random(radius + 4f, radius + 12f);
			int x = cx + (int)(Mathf.cosDeg(angle) * dst), y = cy + (int)(Mathf.sinDeg(angle) * dst);
			if(y > cy - 5 && y < cy + 5 && x > cx)continue;
			Tile tile = tiles.get(x, y);
			if(tile != null && tile.floor() == Blocks.deepwater && put(tiles, HiemaraDefense.navalMine, x, y, 0, team) != null)i++;
		}
	}
	
	/** Places the blocks one after the other round a ring, where there is room. */
	protected static void ring(Tiles tiles, int cx, int cy, Block[] blocks, int count, float radius, float start, Team team){
		for(int i = 0; i < count; i++){
			float angle = start + 360f * i / count;
			int x = cx + Math.round(Mathf.cosDeg(angle) * radius), y = cy + Math.round(Mathf.sinDeg(angle) * radius);
			//the way out of the shipyard stays open
			if(x > cx && Math.abs(y - cy) <= 3)continue;
			put(tiles, blocks[i % blocks.length], x, y, 0, team);
		}
	}
	
	/** @return the block if it was placed, null if there was something in the way. */
	protected static Block put(Tiles tiles, Block block, int x, int y, int rotation, Team team){
		int offset = (block.size - 1) / 2;
		for(int dx = 0; dx < block.size; dx++){
			for(int dy = 0; dy < block.size; dy++){
				Tile tile = tiles.get(x - offset + dx, y - offset + dy);
				if(tile == null || tile.build != null || tile.block() != Blocks.air)return null;
			}
		}
		tiles.getn(x, y).setBlock(block, team, rotation);
		return block;
	}
}
