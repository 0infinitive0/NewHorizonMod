package newhorizon.expand.status;

import arc.Events;
import arc.struct.IntFloatMap;
import arc.struct.IntMap;
import arc.struct.IntSeq;
import arc.util.Time;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.gen.Building;
import mindustry.gen.Unit;
import newhorizon.content.nuke.NukeStatus;

/**
 * Stops things getting better for a while. The game has no one place that healing goes through (menders, repair beams, abilities and
 * status effects each add to health on their own), so this works on the result: while something is held, whatever health it gained since
 * the last tick is taken away again, and damage is untouched. A unit is held for as long as it has the unhealable status; a building is held
 * until the time it was marked for, which is pushed on by every mark. Only the server does it.
 */
public class NoHeal{
	/** What a unit's health was last tick and when, by unit id. */
	private static final IntMap<float[]> units = new IntMap<>();
	/** Until when a building (by tile position) is held, and what its health was last tick. */
	private static final IntFloatMap until = new IntFloatMap(), builds = new IntFloatMap();
	private static final IntSeq done = new IntSeq();
	private static boolean hooked;

	public static void hook(){
		if(hooked) return;
		hooked = true;
		Events.run(EventType.Trigger.update, NoHeal::updateBuildings);
		Events.on(EventType.ResetEvent.class, e -> clear());
		Events.on(EventType.WorldLoadEvent.class, e -> clear());
	}

	public static void clear(){
		units.clear();
		until.clear();
		builds.clear();
	}

	/** Whether the unit cannot heal now. */
	public static boolean blocked(Unit unit){
		return NukeStatus.unhealable != null && unit.hasEffect(NukeStatus.unhealable);
	}

	/** Whether the building cannot heal now. */
	public static boolean blocked(Building build){
		return until.get(build.pos(), 0f) > Time.time;
	}

	/** Runs every tick for a unit that is held: takes back what it healed. */
	public static void hold(Unit unit){
		if(Vars.net.client() || unit.dead) return;
		float[] last = units.get(unit.id);
		if(last == null) units.put(unit.id, last = new float[]{unit.health, Time.time});
		//An entry from before the hold ran out says nothing about now.
		if(Time.time - last[1] <= 3f && unit.health > last[0]) unit.health = last[0];
		last[0] = unit.health;
		last[1] = Time.time;
		if(units.size > 512) prune();
	}

	private static void prune(){
		done.clear();
		for(IntMap.Entry<float[]> e : units) if(Time.time - e.value[1] > 5f) done.add(e.key);
		for(int i = 0; i < done.size; i++) units.remove(done.get(i));
	}

	/** Holds a building for this many ticks from now. Server side. */
	public static void mark(Building build, float ticks){
		if(Vars.net.client() || build == null || !build.isValid()) return;
		int pos = build.pos();
		if(until.get(pos, 0f) <= Time.time) builds.put(pos, build.health);
		until.put(pos, Math.max(until.get(pos, 0f), Time.time + ticks));
	}

	private static void updateBuildings(){
		if(Vars.net.client() || until.size == 0) return;
		done.clear();
		for(IntFloatMap.Entry e : until){
			Building build = Vars.world.build(e.key);
			if(build == null || !build.isValid() || e.value <= Time.time){
				done.add(e.key);
				continue;
			}
			float last = builds.get(e.key, build.health);
			if(build.health > last) build.health = last;
			builds.put(e.key, build.health);
		}
		for(int i = 0; i < done.size; i++){
			until.remove(done.get(i), 0f);
			builds.remove(done.get(i), 0f);
		}
	}
}
