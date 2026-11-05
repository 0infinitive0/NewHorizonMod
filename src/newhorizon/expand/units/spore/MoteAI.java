package newhorizon.expand.units.spore;

import mindustry.gen.Building;
import newhorizon.expand.units.ai.SporelingAI;

/**
 * The Spore Mote's brain: the Sporeling's (hunts enemy units, air and ground, never buildings, can be guided by the player's clicks), but
 * flying: with nothing to hunt and no order it flies straight at the nearest enemy core instead of following the ground flow field.
 */
public class MoteAI extends SporelingAI{
	@Override
	protected void advance(){
		Building core = unit.closestEnemyCore();
		if(core != null) moveTo(core, unit.type.range * 0.8f);
	}
}
