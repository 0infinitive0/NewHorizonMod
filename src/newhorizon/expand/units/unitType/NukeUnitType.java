package newhorizon.expand.units.unitType;

import mindustry.game.Team;
import mindustry.gen.Unit;
import newhorizon.content.nuke.NukeStatus;

/**
 * A nuke. One that has been launched from orbit is flying too high to be shot at: while it has the high altitude status, nothing
 * targets it and bullets pass it by.
 */
public class NukeUnitType extends NHUnitType{
	public NukeUnitType(String name){
		super(name);
	}

	protected boolean high(Unit unit){
		return NukeStatus.highAltitude != null && unit.hasEffect(NukeStatus.highAltitude);
	}

	@Override
	public boolean targetable(Unit unit, Team targeter){
		return !high(unit) && super.targetable(unit, targeter);
	}

	@Override
	public boolean hittable(Unit unit){
		return !high(unit) && super.hittable(unit);
	}
}
