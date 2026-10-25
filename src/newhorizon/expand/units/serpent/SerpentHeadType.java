package newhorizon.expand.units.serpent;

import mindustry.game.Team;
import mindustry.gen.Unit;
import newhorizon.expand.units.unitType.NHUnitType;

/**
 * The head of a Serpent. While any section of the body is alive nothing can hurt it, and so nothing is allowed to aim at it either:
 * enemy units, turrets and homing bullets all ask {@code targetable} before they lock on.
 */
public class SerpentHeadType extends NHUnitType{
	public SerpentHeadType(String name){
		super(name);
	}

	@Override
	public boolean targetable(Unit unit, Team targeter){
		return !SerpentChain.guarded(unit) && super.targetable(unit, targeter);
	}

	@Override
	public boolean killable(Unit unit){
		return !SerpentChain.guarded(unit) && super.killable(unit);
	}
}
