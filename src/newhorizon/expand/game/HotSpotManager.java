package newhorizon.expand.game;

import arc.Events;
import arc.math.Mathf;
import arc.struct.IntSeq;
import arc.struct.IntSet;
import arc.struct.Seq;
import arc.util.Time;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.content.StatusEffects;
import mindustry.entities.Units;
import mindustry.game.EventType;
import mindustry.gen.Building;
import mindustry.gen.Call;
import mindustry.gen.Iconc;
import mindustry.world.Tile;
import newhorizon.expand.block.env.HotSpotFloor;
import newhorizon.expand.block.env.IceFloor;

import static mindustry.Vars.tilesize;

/**
 * Hot spots and the volcano that erupts from them. A map has up to three hot spots (patches of molten rock, {@link HotSpotFloor}).
 * From time to time one erupts, unless a powered seismic stabilizer stands within its reach: a wave of slag spreads from it, which
 * destroys every block in its way and burns what swims in it, melts the ice around it and keeps the water from freezing for a while.
 * The slag then cools, from the edge to the middle, into shallow water. Nothing here is saved: the vents and the slag are tiles of
 * the map, and when a map is loaded the vents are found again and the slag starts to cool.
 */
public class HotSpotManager{
	/** Most hot spots on one map. */
	public static final int maxSpots = 3;
	/** Reach of a hot spot in tiles: a stabilizer inside it holds the volcano back. */
	public static float influence = 14f;
	/** Radius in tiles of the slag of an eruption. */
	public static float eruptRadius = 10f;
	/** The ice melts, and does not form, up to this many times the radius of the slag. */
	public static float meltFactor = 2.2f;
	/** Ticks from the start of the eruption to the slag reaching the full radius. */
	public static float spreadTime = 6f * 60f;
	/** Ticks the water stays warm after an eruption. */
	public static float warmTime = 3f * 60f * 60f;
	/** No eruption this long after a map is loaded, and then every so long. */
	public static float firstDelay = 12f * 60f * 60f, minInterval = 9f * 60f * 60f, maxInterval = 15f * 60f * 60f;
	/** The warning comes this long before an eruption. */
	public static float warnTime = 40f * 60f;
	/** The slag takes between this many seconds (the outer edge) and this many more per tile of depth (the middle) to cool. */
	public static float coolBase = 70f, coolPerTile = 8f;
	/** Time scale of the cooling, 1 normally. */
	public static float coolScale = 1f;
	
	public static class Spot{
		public int x, y;
		public float countdown;
		public boolean warned;
		
		public float wx(){
			return x * tilesize;
		}
		
		public float wy(){
			return y * tilesize;
		}
	}
	
	/** A wave of slag that is spreading. */
	public static class Front{
		public int cx, cy;
		public float radius, max, previous;
		public final IntSeq tiles = new IntSeq();
	}
	
	/** A patch where the ice melts, and does not form, for a while. */
	public static class Zone{
		public float x, y, radius, until, nextMelt;
	}
	
	public static final Seq<Spot> spots = new Seq<>();
	public static final Seq<Front> fronts = new Seq<>();
	public static final Seq<Zone> zones = new Seq<>();
	/** Slag tiles waiting to cool: tile position and time (game ticks). */
	protected static final IntSeq coolTiles = new IntSeq();
	protected static final Seq<Float> coolTimes = new Seq<>();
	protected static float coolTimer;
	
	public static void load(){
		Events.on(EventType.WorldLoadEvent.class, e -> reset());
		Events.run(EventType.Trigger.update, HotSpotManager::update);
	}
	
	/** Finds the hot spots of the map and the slag that has to cool. */
	public static void reset(){
		spots.clear();
		fronts.clear();
		zones.clear();
		coolTiles.clear();
		coolTimes.clear();
		
		IntSet seen = new IntSet();
		var tiles = Vars.world.tiles;
		for(Tile tile : tiles){
			if(tile.floor() == Blocks.slag){
				//slag from before the map was saved: it goes on cooling
				coolTiles.add(tile.pos());
				coolTimes.add(Time.time + (coolBase + Mathf.random(0f, 120f)) * 60f / coolScale);
			}
			
			if(!(tile.floor() instanceof HotSpotFloor) || seen.contains(tile.pos()))continue;
			
			//one hot spot is all the vent tiles that touch
			IntSeq stack = new IntSeq();
			stack.add(tile.pos());
			seen.add(tile.pos());
			float sx = 0f, sy = 0f;
			int n = 0;
			while(stack.size > 0){
				Tile current = Vars.world.tile(stack.pop());
				sx += current.x;
				sy += current.y;
				n++;
				for(int dx = -1; dx <= 1; dx++){
					for(int dy = -1; dy <= 1; dy++){
						Tile other = Vars.world.tile(current.x + dx, current.y + dy);
						if(other != null && other.floor() instanceof HotSpotFloor && seen.add(other.pos()))stack.add(other.pos());
					}
				}
			}
			
			if(spots.size < maxSpots){
				Spot spot = new Spot();
				spot.x = Math.round(sx / n);
				spot.y = Math.round(sy / n);
				spot.countdown = firstDelay + Mathf.random(0f, minInterval * 0.5f);
				spots.add(spot);
			}
		}
	}
	
	/** @return the hot spot whose reach covers a point in world units, or null. */
	public static Spot spotAt(float wx, float wy){
		for(Spot spot : spots){
			if(Mathf.within(wx, wy, spot.wx(), spot.wy(), influence * tilesize))return spot;
		}
		return null;
	}
	
	/** @return whether a powered seismic stabilizer holds the hot spot back. */
	public static boolean stabilized(Spot spot){
		for(Building build : newhorizon.expand.block.hiemara.SeismicStabilizer.active){
			if(build.isValid() && build.efficiency > 0f && build.within(spot.wx(), spot.wy(), influence * tilesize))return true;
		}
		return false;
	}
	
	/** @return whether the water here is kept warm by an eruption, so it does not freeze. */
	public static boolean warm(Tile tile){
		for(int i = 0; i < zones.size; i++){
			Zone zone = zones.get(i);
			if(zone.until > Time.time && tile.within(zone.x, zone.y, zone.radius))return true;
		}
		return false;
	}
	
	protected static float interval(){
		return Mathf.random(minInterval, maxInterval);
	}
	
	public static void update(){
		if(Vars.net.client() || !Vars.state.isGame() || Vars.world.isGenerating())return;
		
		for(Spot spot : spots){
			spot.countdown -= Time.delta;
			if(!spot.warned && spot.countdown < warnTime && spot.countdown > 0f){
				spot.warned = true;
				if(!stabilized(spot))Call.warningToast(Iconc.warning, "@hotspot.warning");
			}
			if(spot.countdown <= 0f){
				if(stabilized(spot)){
					spot.countdown = interval();
					spot.warned = false;
				}else{
					erupt(spot.x, spot.y, eruptRadius);
				}
			}
		}
		
		for(int i = fronts.size - 1; i >= 0; i--){
			if(advance(fronts.get(i)))fronts.remove(i);
		}
		
		for(int i = zones.size - 1; i >= 0; i--){
			Zone zone = zones.get(i);
			if(zone.until <= Time.time){
				zones.remove(i);
			}else if(Time.time >= zone.nextMelt){
				zone.nextMelt = Time.time + 300f;
				meltIce(zone.x, zone.y, zone.radius);
			}
		}
		
		coolTimer += Time.delta;
		if(coolTimer >= 15f){
			coolTimer = 0f;
			cool();
		}
	}
	
	/** Moves a hot spot: its vent is closed (it becomes shallow water) and a new one opens at another tile. */
	public static void moveSpot(Spot spot, int x, int y){
		arc.math.geom.Geometry.circle(spot.x, spot.y, 2, (px, py) -> {
			Tile tile = Vars.world.tile(px, py);
			if(tile != null && tile.floor() instanceof HotSpotFloor)IceFloor.setFloor(tile, Blocks.water.asFloor());
		});
		spot.x = x;
		spot.y = y;
		arc.math.geom.Geometry.circle(x, y, 2, (px, py) -> {
			Tile tile = Vars.world.tile(px, py);
			if(tile != null)IceFloor.setFloor(tile, newhorizon.content.hiemara.HiemaraFloors.hotSpot);
		});
		spot.countdown = interval();
		spot.warned = false;
	}
	
	/** Starts an eruption at a tile, from a hot spot or made by a destabilizer. The hot spot in reach of it starts to wait again. */
	public static void erupt(int x, int y, float radius){
		Front front = new Front();
		front.cx = x;
		front.cy = y;
		front.max = radius;
		fronts.add(front);
		
		Spot spot = spotAt(x * tilesize, y * tilesize);
		if(spot != null){
			spot.countdown = interval();
			spot.warned = false;
		}
		
		Zone zone = new Zone();
		zone.x = x * tilesize;
		zone.y = y * tilesize;
		zone.radius = radius * meltFactor * tilesize;
		zone.until = Time.time + warmTime;
		zones.add(zone);
		meltIce(zone.x, zone.y, zone.radius);
		
		Call.warningToast(Iconc.warning, "@hotspot.erupt");
		Call.effect(Fx.massiveExplosion, x * tilesize, y * tilesize, 0f, arc.graphics.Color.orange);
	}
	
	/** Melts all the ice in a circle, in world units. */
	protected static void meltIce(float wx, float wy, float radius){
		int r = (int)(radius / tilesize) + 1;
		int cx = Math.round(wx / tilesize), cy = Math.round(wy / tilesize);
		int budget = 400;
		for(int dx = -r; dx <= r && budget > 0; dx++){
			for(int dy = -r; dy <= r && budget > 0; dy++){
				Tile tile = Vars.world.tile(cx + dx, cy + dy);
				if(tile == null || !(tile.floor() instanceof IceFloor) || tile.build != null)continue;
				if(!Mathf.within(tile.worldx(), tile.worldy(), wx, wy, radius))continue;
				((IceFloor)tile.floor()).melt(tile);
				budget--;
			}
		}
	}
	
	/** Spreads a wave of slag a step further. @return whether it is done. */
	protected static boolean advance(Front front){
		front.previous = front.radius;
		front.radius = Math.min(front.max, front.radius + front.max / spreadTime * Time.delta);
		
		int r = (int)front.radius + 1;
		for(int dx = -r; dx <= r; dx++){
			for(int dy = -r; dy <= r; dy++){
				float dst = Mathf.dst(dx, dy);
				if(dst > front.radius || dst <= front.previous - 1f && front.previous > 0f)continue;
				Tile tile = Vars.world.tile(front.cx + dx, front.cy + dy);
				if(tile == null || tile.floor() instanceof HotSpotFloor)continue;
				
				//everything standing in the way is destroyed
				Building build = tile.build;
				if(build != null && build.isValid()){
					Call.effect(Fx.explosion, build.x, build.y, 0f, arc.graphics.Color.orange);
					build.kill();
				}
				
				if(tile.floor() == Blocks.slag)continue;
				if(tile.floor() instanceof IceFloor || tile.floor().isLiquid){
					IceFloor.setFloor(tile, Blocks.slag.asFloor());
					front.tiles.add(tile.pos());
					if(Mathf.chance(0.08))Call.effect(Fx.fire, tile.worldx(), tile.worldy(), 0f, arc.graphics.Color.white);
				}
			}
		}
		
		//what swims in the slag burns
		float wr = front.radius * tilesize;
		Units.nearby(front.cx * tilesize - wr, front.cy * tilesize - wr, wr * 2f, wr * 2f, unit -> {
			if(!unit.isFlying() && unit.within(front.cx * tilesize, front.cy * tilesize, wr)){
				unit.damage(8f * Time.delta);
				unit.apply(StatusEffects.melting, 240f);
			}
		});
		
		if(front.radius < front.max)return false;
		
		//the outer edge cools first and the middle last
		for(int i = 0; i < front.tiles.size; i++){
			int pos = front.tiles.get(i);
			Tile tile = Vars.world.tile(pos);
			if(tile == null)continue;
			float depth = front.max - Mathf.dst(tile.x, tile.y, front.cx, front.cy);
			coolTiles.add(pos);
			coolTimes.add(Time.time + (coolBase + depth * coolPerTile + Mathf.random(0f, 60f)) * 60f / coolScale);
		}
		return true;
	}
	
	/** Turns the slag that is cool by now into shallow water. */
	protected static void cool(){
		int budget = 60;
		for(int i = coolTiles.size - 1; i >= 0 && budget > 0; i--){
			if(coolTimes.get(i) > Time.time)continue;
			Tile tile = Vars.world.tile(coolTiles.get(i));
			coolTiles.removeIndex(i);
			coolTimes.remove(i);
			if(tile != null && tile.floor() == Blocks.slag){
				IceFloor.setFloor(tile, Blocks.water.asFloor());
				budget--;
			}
		}
	}
}
