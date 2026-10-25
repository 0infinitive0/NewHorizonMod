package newhorizon.expand.units.serpent;

import mindustry.ai.types.CommandAI;
import mindustry.gen.Teamc;

/**
 * A player team's Serpent head: the usual commands, but when it attacks it goes round its target (the base game's circling attack
 * flies through it and back), see {@link SerpentOrbit}.
 */
public class SerpentCommandAI extends CommandAI{
	@Override
	public void circleAttack(float circleLength){
		Teamc aim = attackTarget != null ? attackTarget : target;
		if(aim == null) return;
		SerpentOrbit.orbit(unit, aim, unit.type.circleTargetRadius, 1f);
	}
}
