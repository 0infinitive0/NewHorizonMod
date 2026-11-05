package newhorizon.expand.units.spore;

import mindustry.entities.Units;
import mindustry.gen.Building;
import mindustry.gen.Teamc;
import newhorizon.content.spore.SporeContent;
import newhorizon.expand.units.ai.SporelingAI;

/**
 * The Spore Mote's brain: the Sporeling's (hunts enemy units, air and ground, never buildings, can be guided by the player's clicks), but
 * flying, and it goes for enemy units that do not carry Blight yet first: only when every enemy unit in reach is infected already does it
 * go for the closest of those. With nothing to hunt and no order it flies straight at the nearest enemy core instead of following the ground flow field.
 */
public class MoteAI extends SporelingAI{
	/** Units that are not Blighted come first; among them (or among the Blighted ones, if there is nothing else) the closest. */
	@Override
	public Teamc findTarget(float x, float y, float range, boolean air, boolean ground){
		Teamc fresh = Units.closestEnemy(unit.team, x, y, range, u -> u.checkTarget(air, ground) && !SporeContent.carriesBlight(u));
		return fresh != null ? fresh : super.findTarget(x, y, range, air, ground);
	}

	@Override
	protected void advance(){
		Building core = unit.closestEnemyCore();
		if(core != null) moveTo(core, unit.type.range * 0.8f);
	}
}
