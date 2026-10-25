package newhorizon.expand.units.serpent;

import mindustry.ai.types.FlyingAI;

/** An enemy Serpent head: it goes round its target, see {@link SerpentOrbit}. */
public class SerpentFlyingAI extends FlyingAI{
	@Override
	public void circleAttack(float circleLength){
		if(target == null) return;
		SerpentOrbit.orbit(unit, target, unit.type.circleTargetRadius, 1f);
	}
}
