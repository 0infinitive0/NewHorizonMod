package newhorizon.expand.units.serpent;

import mindustry.ai.types.FlyingAI;
import mindustry.entities.Units;

/** An enemy Serpent head: it goes round its target, see {@link SerpentOrbit}. */
public class SerpentFlyingAI extends FlyingAI{
	@Override
	public void circleAttack(float circleLength){
		if(target == null) return;
		SerpentOrbit.orbit(unit, target, unit.type.circleTargetRadius, 1f);
	}

	/**
	 * Keeps the head turned to its target. The usual per-tick routine for a flying unit turns it to the way it is moving, and the orbit
	 * turns it back to the target, so the two cancelled out and the head sat turned along its ring instead of facing the target, which
	 * left its cannon and flames, which cannot turn, pointing sideways.
	 */
	@Override
	public void updateVisuals(){
		if(target != null && !Units.invalidateTarget(target, unit, unit.range())){
			if(unit.isFlying() && unit.type.wobble) unit.wobble();
			return;
		}
		super.updateVisuals();
	}

	/**
	 * Faces the target itself. The usual way leads the target by the speed of the unit's first weapon's bullet, and the head's first
	 * weapon is the lightning cannon, which has no bullet speed, so the head would turn to a point far from the target and neither its
	 * cannon nor its flames, which cannot turn, would point at it.
	 */
	@Override
	public void faceTarget(){
		if(target != null && !Units.invalidateTarget(target, unit, unit.range())) unit.lookAt(target);
		else if(unit.moving()) unit.lookAt(unit.vel().angle());
	}
}
