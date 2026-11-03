package newhorizon.expand.units.unitType;

import mindustry.game.Team;
import mindustry.gen.Unit;
import newhorizon.expand.units.modular.CloakAbility;

/** The type of the Chimera frame and of its pods: a cloaked unit cannot be targeted by the enemy (see {@link CloakAbility}). */
public class ModularUnitType extends NHUnitType{
	public ModularUnitType(String name){
		super(name);
	}

	@Override
	public boolean targetable(Unit unit, Team targeter){
		return !(targeter != unit.team && CloakAbility.hidden(unit)) && super.targetable(unit, targeter);
	}
}
