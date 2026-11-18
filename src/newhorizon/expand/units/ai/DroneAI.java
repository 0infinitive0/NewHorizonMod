package newhorizon.expand.units.ai;

import arc.math.Angles;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.Vars;
import mindustry.ai.types.CommandAI;
import mindustry.content.Fx;
import mindustry.entities.Units;
import mindustry.entities.units.AIController;
import mindustry.gen.Teamc;
import mindustry.gen.Unit;
import newhorizon.expand.units.ablility.DroneBayAbility;

/**
 * What every Mothership drone does, whatever it fights with: it is bound to a Mothership slot, attacks what the Mothership attacks (see
 * {@link #targetOf}), goes back to the Mothership to be healed when low, can be <b>recalled</b> (it flies to the Mothership and is absorbed),
 * and slowly wastes away with no Mothership. How it attacks is up to the subclass ({@link #engage}).
 *
 * <p>Flying is done here and not by the game's {@code moveAt}, which ties turning to acceleration: the speed rises slowly ({@link #accelRate})
 * but the heading turns fast ({@link #turnRate} degrees a tick) and <b>keeps the speed</b> through the turn, so a drone can turn sharply.
 *
 * <p>With a weapon, the drone's mounts all aim at the one target it chose (the game's own targeting is replaced, see {@link #findMainTarget}).
 * Below {@link #returnBelow} of its health it flies to its slot, hovers there where the Mothership's repair beams reach it, and goes back to the
 * attack at {@link #resumeAt}. With no Mothership it looks for another with a free slot of its type, and failing that fights alone while it
 * slowly dies ({@link #unboundDrain}).
 */
public abstract class DroneAI extends AIController{
	/** Degrees a tick the heading turns, and px a tick a tick the speed rises and falls. */
	public float turnRate = 8f, accelRate = 0.05f, brakeRate = 0.15f;
	/** Health fractions at which it breaks off to be healed, and goes back to the attack. */
	public float returnBelow = 0.35f, resumeAt = 0.9f;
	/** How far from itself it looks for something to fight with no mothership. */
	public float soloRange = 700f;
	/** How far from the mothership's centre its slot is (beyond the mothership's body), and how near the slot counts as there. */
	public float slotGap = 40f, arrive = 14f;
	/** How far from the mothership it guards (the nearest enemy within this of it is attacked when the mothership has no target), and how far it looks for a new one. */
	public float guardRange = 800f, adoptRange = 2000f;
	/** The share of its maximum health an unbound drone loses every second (it dies of it, in about a hundred seconds). */
	public float unboundDrain = 0.01f;
	/** How far outside touching a recalled drone is taken in (px). */
	public float absorbReach = 10f;

	public Unit owner;
	public DroneBayAbility bay;
	/** Breaking off to be healed, and ordered back to the mothership to be absorbed (set by the bay). */
	public boolean returning, recalling;

	protected float searchTimer;
	protected Teamc solo;
	/** The target all its weapons aim at this tick. */
	protected Teamc chosen;

	/** The target all its weapons aim at this tick (null when it has none). */
	public Teamc chosenTarget(){
		return chosen;
	}

	/** Flies the drone at a target, one tick. */
	protected abstract void engage(Teamc target);

	@Override
	public Teamc findMainTarget(float x, float y, float range, boolean air, boolean ground){
		return chosen;
	}

	@Override
	public void updateMovement(){
		boolean hasOwner = owner != null && owner.isValid() && bay != null;
		if(!hasOwner){
			owner = null;
			searchTimer -= Time.delta;
			if(searchTimer <= 0f){
				searchTimer = 30f;
				hasOwner = DroneBayAbility.adopt(unit, this, adoptRange);
			}
		}

		chosen = null;
		if(!hasOwner){
			recalling = false;
			drain();
		}

		//ordered home: fly to the middle of the mothership and be absorbed there
		if(recalling){
			recall();
			return;
		}

		float hp = unit.healthf();
		if(hasOwner && !returning && hp < returnBelow) returning = true;
		else if(returning && (hp >= resumeAt || !hasOwner)) returning = false;

		if(returning){
			dock();
			return;
		}

		Teamc t = null;
		if(hasOwner){
			t = targetOf(owner, guardRange);
		}else{
			if(solo == null || !valid(solo, unit) || timer.get(timerTarget, 40f)){
				solo = Units.closestTarget(unit.team, unit.x, unit.y, soloRange, u -> u.checkTarget(true, true), b -> true);
			}
			t = solo;
		}

		if(t != null){
			chosen = t;
			engage(t);
		}else if(hasOwner){
			dock();
		}else{
			fly(unit.x + unit.vel.x, unit.y + unit.vel.y, 0f);
		}
	}

	/**
	 * A drone with no mothership slowly wastes away. The loss comes straight off its health (armour and Bulwark cannot soften it), is not held
	 * back at one point, and kills it when it runs out. A mothership with a free slot adopting it ends it.
	 */
	protected void drain(){
		if(Vars.net.client() || !unit.isValid()) return;
		unit.health -= unit.maxHealth * unboundDrain * Time.delta / 60f;
		if(Mathf.chance(0.04 * Time.delta)) Fx.smoke.at(unit.x + Mathf.range(unit.hitSize / 3f), unit.y + Mathf.range(unit.hitSize / 3f));
		if(unit.health <= 0f) unit.kill();
	}

	/** One tick of going home to be absorbed. */
	protected void recall(){
		if(owner == null || !owner.isValid()) return;
		float dist = unit.dst(owner);
		//Taken in on touching it. Flying units push each other apart by their sizes, so a drone can never get closer to the middle of a big
		//mothership than half of both sizes added; a smaller distance than that would never be reached.
		if(dist < (owner.hitSize + unit.hitSize) / 2f + absorbReach){
			if(bay != null) bay.absorb(unit);
			return;
		}
		float cap = Math.min(unit.speed(), (float)Math.sqrt(2f * brakeRate * 0.8f * dist) + owner.vel.len() + 1.5f);
		fly(owner.x, owner.y, cap);
	}

	/** Flies to the drone's slot beside the mothership and hovers there. */
	protected void dock(){
		if(owner == null){
			fly(unit.x, unit.y, 0f);
			return;
		}
		int count = Math.max(1, bay == null ? 4 : bay.count);
		int idx = bay == null ? 0 : Math.max(0, bay.bound.indexOf(unit));
		float ang = owner.rotation + 45f + idx * 360f / count;
		float r = owner.hitSize / 2f + slotGap;
		float sx = owner.x + Angles.trnsx(ang, r), sy = owner.y + Angles.trnsy(ang, r);
		float dist = unit.dst(sx, sy);
		if(dist < arrive){
			unit.vel.lerp(owner.vel, Mathf.clamp(0.25f * Time.delta));
			unit.rotation = Angles.moveToward(unit.rotation, owner.rotation, unit.type.rotateSpeed * Time.delta);
			return;
		}
		//slow down in time to stop at the slot (what the brakes can do over the distance), plus the mothership's own speed
		float cap = Math.min(unit.speed(), (float)Math.sqrt(2f * brakeRate * 0.8f * dist) + owner.vel.len());
		fly(sx, sy, cap);
	}

	/** Steers toward a point: the speed moves toward {@code speedCap} by the acceleration, the heading toward the point by the turn rate. */
	protected void fly(float tx, float ty, float speedCap){
		float speed = unit.vel.len();
		float heading = speed > 0.1f ? unit.vel.angle() : unit.rotation;
		float want = Angles.angle(unit.x, unit.y, tx, ty);
		heading = Angles.moveToward(heading, want, turnRate * Time.delta);
		if(speed < speedCap) speed = Math.min(speedCap, speed + accelRate * Time.delta);
		else speed = Math.max(speedCap, speed - brakeRate * Time.delta);
		unit.vel.trns(heading, speed);
		unit.rotation = Angles.moveToward(unit.rotation, heading, Math.max(unit.type.rotateSpeed, turnRate) * Time.delta);
	}

	private static java.lang.reflect.Field targetField;

	/** An AI's target (the field is protected, and this is not a subclass of that AI). */
	protected static Teamc targetField(AIController ai){
		try{
			if(targetField == null){
				targetField = AIController.class.getDeclaredField("target");
				targetField.setAccessible(true);
			}
			return (Teamc)targetField.get(ai);
		}catch(Throwable e){
			return null;
		}
	}

	protected static boolean valid(Teamc t, Unit from){
		return t != null && !Units.invalidateTarget(t, from.team, from.x, from.y);
	}

	/**
	 * What a mothership is attacking: the Command AI's attack target, an AI's own target, what a player aims at while shooting, and with none of
	 * those the nearest enemy within {@code guard} of it (so that the drones defend it). Null when there is nothing.
	 */
	public static Teamc targetOf(Unit m, float guard){
		var c = m.controller();
		Teamc t = null;
		if(c instanceof CommandAI ai) t = ai.attackTarget;
		if(!valid(t, m) && c instanceof AIController ai) t = targetField(ai);
		if(!valid(t, m) && m.isPlayer() && m.isShooting()){
			t = Units.closestTarget(m.team, m.aimX, m.aimY, 160f, u -> u.checkTarget(true, true), b -> true);
		}
		if(!valid(t, m)) t = Units.closestTarget(m.team, m.x, m.y, guard, u -> u.checkTarget(true, true), b -> true);
		return valid(t, m) ? t : null;
	}
}
