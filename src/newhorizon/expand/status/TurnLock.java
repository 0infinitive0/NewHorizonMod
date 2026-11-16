package newhorizon.expand.status;

import arc.Events;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Time;
import mindustry.entities.units.WeaponMount;
import mindustry.game.EventType;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.type.StatusEffect;
import mindustry.world.blocks.defense.turrets.Turret;

/**
 * Keeps units and turrets from turning. The game has no status field for it: a unit or a turret turns by setting its own rotation every tick,
 * so this puts the rotation back at the <b>end</b> of every game tick ({@link EventType.Trigger#afterGameUpdate}, after everything has moved),
 * which means that what is drawn, and what is aimed with on the next tick, is the locked value.
 *
 * <p>A unit is locked while it has the given status effect: its body and every rotating weapon stay as they were when it was locked, so it
 * can still move but cannot face anything new, and its rotating weapons cannot aim. A turret is locked for a time: it keeps its ammo and can
 * still shoot, but only at what is already in front of it.
 */
public class TurnLock{
	/** What a locked unit looked like when it was locked: its body rotation, then the rotation of each mount (NaN for one that does not rotate). */
	private static final ObjectMap<Unit, float[]> units = new ObjectMap<>();
	/** The status effect that keeps units locked. */
	private static StatusEffect status;

	private static final class TurretLock{
		float until, rotation;
	}

	private static final ObjectMap<Building, TurretLock> turrets = new ObjectMap<>();
	private static final Seq<Object> done = new Seq<>();
	private static boolean hooked;

	public static void hook(StatusEffect lockedStatus){
		status = lockedStatus;
		if(hooked) return;
		hooked = true;
		Events.run(EventType.Trigger.afterGameUpdate, TurnLock::update);
		Events.on(EventType.WorldLoadEvent.class, e -> clear());
		Events.on(EventType.ResetEvent.class, e -> clear());
	}

	private static void clear(){
		units.clear();
		turrets.clear();
	}

	/** Locks a unit for {@code time} ticks: gives it the status effect and remembers how it faces. Server side. */
	public static void lock(Unit unit, float time){
		if(unit == null || unit.dead() || status == null) return;
		unit.apply(status, time);
		if(!units.containsKey(unit)){
			float[] saved = new float[1 + unit.mounts.length];
			saved[0] = unit.rotation;
			for(int i = 0; i < unit.mounts.length; i++){
				WeaponMount m = unit.mounts[i];
				saved[1 + i] = m.weapon.rotate ? m.rotation : Float.NaN;
			}
			units.put(unit, saved);
		}
	}

	/** Locks a turret for {@code time} ticks. Anything that is not a turret is ignored. Server side. */
	public static void lock(Building build, float time){
		if(build == null || !build.isValid() || !(build.block instanceof Turret) || !(build instanceof Turret.TurretBuild t)) return;
		TurretLock lock = turrets.get(build);
		if(lock == null){
			lock = new TurretLock();
			lock.rotation = t.rotation;
			turrets.put(build, lock);
		}
		lock.until = Math.max(lock.until, Time.time + time);
	}

	public static boolean locked(Unit unit){
		return units.containsKey(unit);
	}

	public static boolean locked(Building build){
		return turrets.containsKey(build);
	}

	private static void update(){
		if(units.isEmpty() && turrets.isEmpty()) return;

		done.clear();
		for(ObjectMap.Entry<Unit, float[]> e : units.entries()){
			Unit u = e.key;
			if(u.dead() || !u.isAdded() || status == null || !u.hasEffect(status)){
				done.add(u);
				continue;
			}
			float[] saved = e.value;
			u.rotation = saved[0];
			for(int i = 0; i < u.mounts.length && 1 + i < saved.length; i++){
				if(!Float.isNaN(saved[1 + i])) u.mounts[i].rotation = saved[1 + i];
			}
		}
		for(Object o : done) units.remove((Unit)o);

		done.clear();
		for(ObjectMap.Entry<Building, TurretLock> e : turrets.entries()){
			Building b = e.key;
			if(!b.isValid() || Time.time >= e.value.until || !(b instanceof Turret.TurretBuild t)){
				done.add(b);
				continue;
			}
			t.rotation = e.value.rotation;
		}
		for(Object o : done) turrets.remove((Building)o);
	}
}
