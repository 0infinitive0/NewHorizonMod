package newhorizon.expand.units.ai;

import arc.math.geom.Vec2;
import arc.util.Time;
import mindustry.ai.Pathfinder;
import mindustry.entities.Units;
import mindustry.entities.units.AIController;
import mindustry.gen.Teamc;
import mindustry.gen.Unit;

/**
 * Hunts enemy units and ignores structures entirely: it never targets a building and never detonates on one that
 * happens to be in its way.
 *
 * <p>An enemy unit within {@link #huntRange} always comes first. With none in sight it is guided by the player when it has been
 * given an order (a position to hold, or an enemy unit to attack, both through the usual command clicks) and otherwise marches
 * along the flow field towards the enemy core. Picking the Hunt command again forgets the orders.
 *
 * <p>The search runs on its own timer: the base class uses {@link #retarget()} for its own targeting in the same frame, which
 * would leave this one never getting a turn.
 */
public class SporelingAI extends AIController{
	/** How far away the Sporeling notices an enemy unit and breaks formation to charge it. */
	public float huntRange = 220f;
	/** Ticks between two searches for prey. */
	public float searchInterval = 20f;

	private final Vec2 charge = new Vec2();
	private float searchTimer = 0f;
	private Teamc prey;
	/** Position the player told it to hold, if any. */
	private Vec2 rally;
	/** Enemy unit the player pointed it at, if any. */
	private Unit ordered;

	@Override
	public void updateMovement(){
		searchTimer -= Time.delta;
		if(searchTimer <= 0f){
			searchTimer = searchInterval;
			prey = findPrey();
		}

		if(Units.invalidateTarget(prey, unit.team, unit.x, unit.y, huntRange * 1.5f)){
			prey = null;
		}

		if(prey != null){
			unit.lookAt(prey);
			unit.movePref(charge.set(prey).sub(unit).limit(unit.speed()));
		}else if(rally != null){
			//Idle and guided: go where the player said and wait there for something to hunt.
			if(!unit.within(rally, 12f)){
				moveTo(rally, 8f);
			}
		}else{
			pathfind(Pathfinder.fieldCore);
		}
	}

	protected Teamc findPrey(){
		if(ordered != null){
			if(ordered.isValid() && ordered.team != unit.team) return ordered;
			ordered = null;
		}
		return findMainTarget(unit.x, unit.y, huntRange, unit.type.targetAir, unit.type.targetGround);
	}

	/** A click on the ground: hold that spot while idle. */
	@Override
	public void commandPosition(Vec2 pos){
		rally = pos == null ? null : pos.cpy();
		ordered = null;
		prey = null;
		searchTimer = 0f;
	}

	/** A click on an enemy: it goes for that unit first. Buildings are ignored, it cannot hurt them. */
	@Override
	public void commandTarget(Teamc target){
		if(target instanceof Unit){
			ordered = (Unit)target;
			rally = null;
			prey = null;
			searchTimer = 0f;
		}
	}

	/** Units only, no buildings: this also feeds the weapon's own targeting. */
	@Override
	public Teamc findMainTarget(float x, float y, float range, boolean air, boolean ground){
		return findTarget(x, y, range, air, ground);
	}

	@Override
	public Teamc findTarget(float x, float y, float range, boolean air, boolean ground){
		//closestTarget(team, x, y, range, unitPred) would also fall back to buildings, closestEnemy is units only.
		return Units.closestEnemy(unit.team, x, y, range, u -> u.checkTarget(air, ground));
	}
}
