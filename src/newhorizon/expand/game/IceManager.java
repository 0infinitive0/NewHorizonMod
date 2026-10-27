package newhorizon.expand.game;

import arc.Events;
import arc.math.Mathf;
import arc.struct.IntSeq;
import arc.struct.ObjectFloatMap;
import arc.struct.Seq;
import arc.util.Time;
import mindustry.Vars;
import mindustry.entities.Units;
import mindustry.game.EventType;
import mindustry.gen.Building;
import mindustry.gen.WaterMovec;
import mindustry.world.Block;
import mindustry.world.Tile;
import newhorizon.expand.block.env.IceFloor;
import newhorizon.expand.weather.ColdWave;

/**
 * Server side logic of freezing and melting water.
 * While a {@link ColdWave} is active water tiles freeze one by one, otherwise the ice slowly melts back to the
 * original water. Ice that has a building on top never melts.
 */
public class IceManager{
	/** Time scale (ticks) in which most of the water freezes / melts. */
	public static float freezeTicks = 45f * 60f, meltTicks = 8f * 60f * 60f;
	/** Ice stays untouched this long after the last cold wave before it starts to thaw. */
	public static float thawDelay = 2.5f * 60f * 60f;
	/** Tiles changed per tick even if only a few candidates are left, so the tail does not take forever. */
	public static float minPerTick = 0.02f;
	/** Limits the amount of floor packets per tick. */
	public static int maxPerTick = 48;
	/** Time between two scans of the whole map. */
	public static float rescanTime = 10f * 60f;

	protected static final IntSeq candidates = new IntSeq();
	protected static boolean wasCold = false, wasActive = false;
	protected static float rescanTimer = 0f;
	protected static float budget = 0f;

	protected static boolean navalFound = false;
	/** Ticks since the last cold wave. */
	protected static float sinceCold = 0f;
	/** Powered heaters, they keep water from freezing and thaw the ice around them. */
	public static final Seq<Building> heaters = new Seq<>();
	/** Radius in world units of a heater, set by the heater block that registered. */
	public static final ObjectFloatMap<Block> heaterRadius = new ObjectFloatMap<>();

	public static void load(){
		Events.on(EventType.WorldLoadEvent.class, e -> {
			candidates.clear();
			wasCold = false;
			rescanTimer = 0f;
			budget = 0f;
			sinceCold = 0f;
			heaters.clear();
		});

		Events.run(EventType.Trigger.update, IceManager::update);
	}

	public static void update(){
		if(Vars.net.client() || !Vars.state.isGame() || Vars.world.isGenerating() || IceFloor.iceOf.isEmpty())return;

		float strength = ColdWave.strength();
		boolean cold = strength > 0.01f;

		if(cold){
			sinceCold = 0f;
		}else{
			sinceCold += Time.delta;
		}
		
		//Nothing thaws before the delay is over, and a new cold wave stops it again.
		boolean thawing = !cold && sinceCold >= thawDelay;
		boolean active = cold || thawing;
		
		rescanTimer -= Time.delta;
		if(active != wasActive || cold != wasCold || rescanTimer <= 0f){
			wasCold = cold;
			wasActive = active;
			rescanTimer = rescanTime;
			if(active)scan(cold);else candidates.clear();
		}

		if(!active || candidates.isEmpty())return;

		budget += (candidates.size / (cold ? freezeTicks : meltTicks) + minPerTick) * (cold ? strength : 1f) * Time.delta;
		int amount = Math.min((int)budget, maxPerTick);
		budget -= (int)budget;

		while(amount-- > 0 && candidates.size > 0){
			int index = Mathf.random(candidates.size - 1);
			int pos = candidates.get(index);
			candidates.set(index, candidates.peek());
			candidates.pop();

			Tile tile = Vars.world.tile(pos);
			if(tile == null)continue;
			if(cold && heated(tile))continue;

			if(cold){
				freeze(tile);
			}else{
				melt(tile);
			}
		}
	}

	protected static void scan(boolean cold){
		candidates.clear();

		for(Tile tile : Vars.world.tiles){
			if(cold){
				if(IceFloor.of(tile.floor()) != null)candidates.add(tile.pos());
			}else if(tile.floor() instanceof IceFloor && tile.build == null){
				candidates.add(tile.pos());
			}
		}
	}

	protected static void freeze(Tile tile){
		IceFloor ice = IceFloor.of(tile.floor());
		if(ice == null || navalOn(tile))return;

		IceFloor.setFloor(tile, ice);
	}

	protected static void melt(Tile tile){
		if(!(tile.floor() instanceof IceFloor) || tile.build != null)return;

		((IceFloor)tile.floor()).melt(tile);
	}

	/** @return whether a powered heater covers the tile. */
	public static boolean heated(Tile tile){
		for(int i = 0; i < heaters.size; i++){
			Building heater = heaters.get(i);
			if(heater.isValid() && heater.efficiency > 0f && heater.within(tile.worldx(), tile.worldy(), heaterRadius.get(heater.block, 0f))){
				return true;
			}
		}
		return false;
	}
	
	/** Naval units die on solid ground, so do not freeze the water below them. */
	protected static boolean navalOn(Tile tile){
		navalFound = false;
		Units.nearby(tile.worldx() - Vars.tilesize, tile.worldy() - Vars.tilesize, Vars.tilesize * 2f, Vars.tilesize * 2f, u -> {
			if(u instanceof WaterMovec)navalFound = true;
		});
		return navalFound;
	}
}
