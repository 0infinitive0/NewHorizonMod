package newhorizon.expand.bullets.ricochet;

import arc.Events;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Time;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.gen.Building;
import mindustry.gen.Healthc;
import mindustry.gen.Unit;
import mindustry.type.StatusEffect;

/**
 * Stops a unit or a building from regaining health for a while.
 *
 * <p>Mindustry has no status that blocks healing (and buildings have no statuses at all), so each locked target is recorded here with the
 * health it had when the lock was set. Every tick a target that has more health than its lock is put back to the lock, which undoes a heal
 * from any source (menders, repair fields, repair drones, regeneration), and a target that has less health than its lock lowers the lock,
 * so damage still counts. A unit also gets the visible {@link #status} while it is locked.
 */
public class AntiHeal{
	/** The visible marker for a locked unit (set by the content loader; null in a test that does not need it). */
	public static StatusEffect status;

	private static final ObjectMap<Healthc, Lock> locks = new ObjectMap<>();
	private static final Seq<Healthc> finished = new Seq<>();
	private static boolean hooked;

	private static class Lock{
		float health;
		float left;
	}

	public static void load(){
		if(hooked) return;
		hooked = true;
		Events.run(EventType.Trigger.update, AntiHeal::update);
		Events.on(EventType.ResetEvent.class, e -> locks.clear());
	}

	/** Locks {@code target}'s health for {@code ticks}; a second call refreshes the timer and keeps the lower lock. */
	public static void apply(Healthc target, float ticks){
		if(target == null || Vars.net.client() || ticks <= 0f || !alive(target)) return;
		Lock lock = locks.get(target);
		if(lock == null){
			lock = new Lock();
			lock.health = target.health();
			locks.put(target, lock);
		}else{
			lock.health = Math.min(lock.health, target.health());
		}
		lock.left = Math.max(lock.left, ticks);
		if(status != null && target instanceof Unit) ((Unit)target).apply(status, ticks);
	}

	public static boolean isLocked(Healthc target){
		return locks.containsKey(target);
	}

	/** The health a locked target cannot rise above, or -1. */
	public static float lockedHealth(Healthc target){
		Lock lock = locks.get(target);
		return lock == null ? -1f : lock.health;
	}

	public static int size(){
		return locks.size;
	}

	public static void clear(){
		locks.clear();
	}

	/** True while the target is still in the world and alive. */
	public static boolean alive(Healthc target){
		if(target instanceof Building) return ((Building)target).isValid();
		if(target instanceof Unit) return ((Unit)target).isAdded() && !((Unit)target).dead();
		return false;
	}

	/** One tick (public so a test can drive it). */
	public static void update(){
		if(locks.isEmpty() || Vars.net.client()) return;

		finished.clear();
		for(ObjectMap.Entry<Healthc, Lock> e : locks){
			Healthc target = e.key;
			Lock lock = e.value;
			lock.left -= Time.delta;
			if(lock.left <= 0f || !alive(target)){
				finished.add(target);
				continue;
			}
			if(target.health() > lock.health){
				target.health(lock.health);
			}else{
				lock.health = target.health();
			}
		}

		for(int i = 0; i < finished.size; i++) locks.remove(finished.get(i));
		finished.clear();
	}
}
