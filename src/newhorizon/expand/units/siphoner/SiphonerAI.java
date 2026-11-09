package newhorizon.expand.units.siphoner;

import mindustry.Vars;
import mindustry.ai.types.FlyingAI;
import mindustry.gen.Building;
import mindustry.gen.Teamc;
import mindustry.world.blocks.storage.CoreBlock;

/**
 * The E-Siphoner's own behaviour (its Drain command, and what the enemy's waves use). It flies at the enemy's working generators and batteries
 * first ({@code targetFlags} of the unit), then at any enemy block that is on a power network, wherever it is (a network that makes power
 * before one that does not), and only when there is none does it go for what a flyer usually goes for. It holds at a distance from its
 * target where both the Tesla and the power drain reach, without circling.
 */
public class SiphonerAI extends FlyingAI{
	@Override
	public void updateMovement(){
		unloadPayloads();
		if(target != null && unit.hasWeapons()){
			moveTo(target, unit.type.range * 0.75f);
			unit.lookAt(target);
		}else{
			super.updateMovement();
		}
	}

	@Override
	public Teamc findMainTarget(float x, float y, float range, boolean air, boolean ground){
		Teamc found = super.findMainTarget(x, y, range, air, ground);
		//A generator or battery that works, or something in reach: that is what it was made for.
		if(found != null && !(found instanceof CoreBlock.CoreBuild)) return found;
		if(!ground) return found;
		//Anywhere on the map (a finite range: the search is a rectangle around the unit).
		float far = (Vars.world.width() + Vars.world.height()) * Vars.tilesize;
		Building powered = Vars.indexer.findEnemyTile(unit.team, x, y, far, b -> b.isValid() && b.power != null && b.power.graph != null && b.power.graph.getPowerProduced() > 0.0001f);
		if(powered == null) powered = Vars.indexer.findEnemyTile(unit.team, x, y, far, b -> b.isValid() && b.power != null && b.power.graph != null);
		return powered != null ? powered : found;
	}
}
