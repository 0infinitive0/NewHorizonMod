package newhorizon.expand.units.ai;

import arc.math.Angles;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.ai.types.CommandAI;
import mindustry.entities.Units;
import mindustry.entities.units.AIController;
import mindustry.gen.Teamc;
import mindustry.gen.Unit;
import newhorizon.expand.units.ablility.RammerBayAbility;

/**
 * The Rammer's brain. It flies the Rammer in <b>attack runs</b> at whatever its mothership is attacking, and goes back to the mothership to be
 * healed when it is low.
 *
 * <p>Flying is done here and not by the game's {@code moveAt}, which ties turning to acceleration: the speed rises slowly ({@link #accelRate}, so
 * a Rammer at rest takes a couple of seconds to get up to speed) but the heading turns fast ({@link #turnRate} degrees a tick) and
 * <b>keeps the speed</b> through the turn, so it can turn sharply. A run is: fly at the target, straight through it, keep going to
 * {@link #runOut} px past it (so that the turn back has room), turn round and go again.
 *
 * <p>What it attacks is what the mothership targets (see {@link #targetOf}); with nothing to attack it orbits the mothership in its own slot.
 * Below {@link #returnBelow} of its health it flies to its slot, hovers there where the mothership's repair beams reach it, and goes back to the
 * attack at {@link #resumeAt}. With no mothership it looks for another with a free slot, and failing that fights alone while it slowly dies
 * ({@link #unboundDrain}).
 */
public class RammerAI extends AIController{
	/** Degrees a tick the heading turns, and px a tick a tick the speed rises and falls. */
	public float turnRate = 8f, accelRate = 0.05f, brakeRate = 0.15f;
	/** Health fractions at which it breaks off to be healed, and goes back to the attack. */
	public float returnBelow = 0.35f, resumeAt = 0.9f;
	/** How far past the target a run goes before the turn, and how far from itself it looks for something to fight with no mothership. */
	public float runOut = 150f, soloRange = 700f;
	/** How far from the mothership's centre its slot is (beyond the mothership's body), and how near the slot counts as there. */
	public float slotGap = 40f, arrive = 14f;
	/** How far from the mothership it guards, and how far it looks for a new one. */
	public float guardRange = 400f, adoptRange = 2000f;
	/** The share of its maximum health an unbound Rammer loses every second (it dies of it, in about a hundred seconds). */
	public float unboundDrain = 0.01f;

	public Unit owner;
	public RammerBayAbility bay;
	public boolean returning, running;

	protected float searchTimer;
	protected Teamc solo;

	@Override
	public void updateTargeting(){
		//the Rammer has no weapon: what it attacks comes from its mothership
	}

	@Override
	public void updateMovement(){
		boolean hasOwner = owner != null && owner.isValid() && bay != null;
		if(!hasOwner){
			owner = null;
			searchTimer -= Time.delta;
			if(searchTimer <= 0f){
				searchTimer = 30f;
				hasOwner = RammerBayAbility.adopt(unit, this, adoptRange);
			}
		}

		if(!hasOwner) drain();

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
			attack(t);
		}else if(hasOwner){
			dock();
		}else{
			fly(unit.x + unit.vel.x, unit.y + unit.vel.y, 0f);
		}
	}

	/**
	 * A Rammer with no mothership slowly wastes away. The loss comes straight off its health (armour and Bulwark cannot soften it), is not held
	 * back at one point, and kills it when it runs out. A mothership with a free slot adopting it ends it.
	 */
	protected void drain(){
		if(Vars.net.client() || !unit.isValid()) return;
		unit.health -= unit.maxHealth * unboundDrain * Time.delta / 60f;
		if(Mathf.chance(0.04 * Time.delta)) Fx.smoke.at(unit.x + Mathf.range(unit.hitSize / 3f), unit.y + Mathf.range(unit.hitSize / 3f));
		if(unit.health <= 0f) unit.kill();
	}

	/** One tick of an attack run at a target. */
	protected void attack(Teamc t){
		float dx = t.getX() - unit.x, dy = t.getY() - unit.y;
		float dist = Mathf.len(dx, dy);
		float speed = unit.vel.len();
		boolean behind = speed > 0.5f && (dx * unit.vel.x + dy * unit.vel.y) < 0f;

		//through it: keep flying straight until there is room to turn round in
		if(!running && behind && dist < runOut) running = true;
		if(running && dist >= runOut) running = false;

		if(running){
			float h = speed > 0.1f ? unit.vel.angle() : unit.rotation;
			fly(unit.x + Angles.trnsx(h, 100f), unit.y + Angles.trnsy(h, 100f), unit.speed());
		}else{
			fly(t.getX(), t.getY(), unit.speed());
		}
	}

	/** Flies to the Rammer's slot beside the mothership and hovers there. */
	protected void dock(){
		running = false;
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
	 * those the nearest enemy within {@code guard} of it (so that the Rammers defend it). Null when there is nothing.
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
