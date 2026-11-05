package newhorizon.expand.zmodular;

import mindustry.gen.LegsUnit;
import newhorizon.expand.entities.EntityRegister;
import newhorizon.expand.units.modular.HitCapAbility;
import newhorizon.expand.units.modular.LaserBlockAbility;

/**
 * The entity of the Chimera frame: a legged unit that takes its two kinds of protection where the damage lands. A Hit Cap core cuts every
 * hit down to the cap; a Laser Block core ignores damage while an enemy laser passes through the frame.
 *
 * <p>This class is in its own package, named so that it sorts last: {@link EntityRegister} gives ids in the order of the classes' names,
 * and a class that sorted earlier would move the ids of the others, and with them every saved entity of theirs.
 */
public class ChimeraEntity extends LegsUnit{
	@Override
	public int classId(){
		return EntityRegister.getID(ChimeraEntity.class);
	}

	@Override
	public void rawDamage(float amount){
		if(LaserBlockAbility.blocks(this)) return;
		float cap = HitCapAbility.capOf(this);
		if(cap > 0f) amount = Math.min(amount, cap);
		super.rawDamage(amount);
	}
}
