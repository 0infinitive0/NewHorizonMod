package newhorizon.expand.block.chronos;

import arc.Events;
import arc.struct.ObjectFloatMap;
import arc.struct.Seq;
import arc.util.Time;
import mindustry.game.EventType;
import mindustry.gen.Building;
import mindustry.world.blocks.defense.ForceProjector;
import mindustry.world.blocks.defense.turrets.Turret;
import mindustry.world.blocks.power.PowerGenerator;
import newhorizon.content.chronos.ChronosFx;

/**
 * Switches enemy structures off for a while: a disabled turret stops shooting and a disabled generator stops producing, until the
 * time runs out. The game itself never switches {@code enabled} back on (only processors do), so this keeps the books.
 */
public class TemporalDisable{
	private static final ObjectFloatMap<Building> until = new ObjectFloatMap<>();
	private static final Seq<Building> done = new Seq<>();
	private static boolean hooked;

	public static void init(){
		if(hooked) return;
		hooked = true;
		Events.run(EventType.Trigger.update, TemporalDisable::update);
		Events.on(EventType.WorldLoadEvent.class, e -> until.clear());
		Events.on(EventType.ResetEvent.class, e -> until.clear());
	}

	/** What can be switched off: turrets, generators and shields. Conveyors and the like are left alone. */
	public static boolean canDisable(Building build){
		return build != null && build.isValid() && (build.block instanceof Turret || build.block instanceof PowerGenerator || build.block instanceof ForceProjector);
	}

	/** Switches the building off for {@code ticks}. A longer remaining time is kept. */
	public static boolean disable(Building build, float ticks){
		if(!canDisable(build)) return false;

		float end = Time.time + ticks;
		if(!until.containsKey(build) || until.get(build, 0f) < end) until.put(build, end);
		build.enabled = false;
		if(build instanceof Turret.TurretBuild){
			//A turret that was already loaded would still get one more shot off.
			Turret.TurretBuild turret = (Turret.TurretBuild)build;
			turret.reloadCounter = 0f;
			turret.target = null;
		}
		ChronosFx.disabled.at(build.x, build.y, build.block.size * 4f);
		return true;
	}

	public static boolean isDisabled(Building build){
		return until.containsKey(build);
	}

	private static void update(){
		if(until.isEmpty()) return;

		done.clear();
		for(ObjectFloatMap.Entry<Building> e : until){
			Building build = e.key;
			if(!build.isValid()){
				done.add(build);
			}else if(Time.time >= e.value){
				build.enabled = true;
				done.add(build);
			}else{
				build.enabled = false;
				if(Time.time % 30f < Time.delta) ChronosFx.disabled.at(build.x, build.y, build.block.size * 4f);
			}
		}
		for(Building build : done) until.remove(build, 0f);
	}

	public static void clear(){
		until.clear();
	}
}
