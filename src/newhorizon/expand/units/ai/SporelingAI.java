package newhorizon.expand.units.ai;

import arc.math.geom.Vec2;
import mindustry.ai.Pathfinder;
import mindustry.entities.Units;
import mindustry.entities.units.AIController;
import mindustry.gen.Teamc;

/**
 * Hunts enemy units and ignores structures entirely: it never targets a building and never detonates on one that
 * happens to be in its way. With no unit in sight it marches along the flow field towards the enemy core.
 */
public class SporelingAI extends AIController{
	/** How far away the Sporeling notices an enemy unit and breaks formation to charge it. */
	public float huntRange = 220f;

	private final Vec2 charge = new Vec2();
	private Teamc prey;

	@Override
	public void updateMovement(){
		if(retarget()){
			prey = findPrey();
		}

		if(Units.invalidateTarget(prey, unit.team, unit.x, unit.y, huntRange * 1.5f)){
			prey = null;
		}

		if(prey != null){
			unit.lookAt(prey);
			unit.movePref(charge.set(prey).sub(unit).limit(unit.speed()));
		}else{
			pathfind(Pathfinder.fieldCore);
		}
	}

	protected Teamc findPrey(){
		return findMainTarget(unit.x, unit.y, huntRange, unit.type.targetAir, unit.type.targetGround);
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
