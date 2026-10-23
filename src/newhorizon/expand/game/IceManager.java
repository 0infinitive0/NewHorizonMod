package newhorizon.expand.game;

import arc.Events;
import arc.math.Mathf;
import arc.struct.IntSeq;
import arc.util.Time;
import mindustry.Vars;
import mindustry.entities.Units;
import mindustry.game.EventType;
import mindustry.gen.WaterMovec;
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
	public static float freezeTicks = 45f * 60f, meltTicks = 90f * 60f;
	/** Tiles changed per second even if only a few candidates are left, so the tail does not take forever. */
	public static float minPerTick = 0.15f;
	/** Limits the amount of floor packets per tick. */
	public static int maxPerTick = 48;
	/** Time between two scans of the whole map. */
	public static float rescanTime = 10f * 60f;

	protected static final IntSeq candidates = new IntSeq();
	protected static boolean wasCold = false;
	protected static float rescanTimer = 0f;
	protected static float budget = 0f;

	protected static boolean navalFound = false;

	public static void load(){
		Events.on(EventType.WorldLoadEvent.class, e -> {
			candidates.clear();
			wasCold = false;
			rescanTimer = 0f;
			budget = 0f;
		});

		Events.run(EventType.Trigger.update, IceManager::update);
	}

	public static void update(){
		if(Vars.net.client() || !Vars.state.isGame() || Vars.world.isGenerating() || IceFloor.iceOf.isEmpty())return;

		float strength = ColdWave.strength();
		boolean cold = strength > 0.01f;

		rescanTimer -= Time.delta;
		if(cold != wasCold || rescanTimer <= 0f){
			wasCold = cold;
			rescanTimer = rescanTime;
			scan(cold);
		}

		if(candidates.isEmpty())return;

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

	/** Naval units die on solid ground, so do not freeze the water below them. */
	protected static boolean navalOn(Tile tile){
		navalFound = false;
		Units.nearby(tile.worldx() - Vars.tilesize, tile.worldy() - Vars.tilesize, Vars.tilesize * 2f, Vars.tilesize * 2f, u -> {
			if(u instanceof WaterMovec)navalFound = true;
		});
		return navalFound;
	}
}
