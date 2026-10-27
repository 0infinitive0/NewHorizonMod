package newhorizon.expand.units.hiemara;

import arc.graphics.g2d.Draw;
import mindustry.game.Team;
import mindustry.gen.Unit;
import mindustry.type.UnitType;
import mindustry.entities.units.WeaponMount;

/**
 * A ship that dives. It can be neither targeted nor hit while none of its weapons has fired in the last moments, so it
 * can only be shot at right after it shoots.
 */
public class SubmarineUnitType extends UnitType{
	/** Ticks after a shot during which it stays on the surface. */
	public float surfaceTime = 45f;
	
	public SubmarineUnitType(String name){
		super(name);
	}
	
	public boolean submerged(Unit unit){
		for(WeaponMount mount : unit.mounts){
			if(mount.reload > mount.weapon.reload - surfaceTime)return false;
		}
		return true;
	}
	
	@Override
	public boolean targetable(Unit unit, Team targeter){
		return !submerged(unit);
	}
	
	@Override
	public boolean hittable(Unit unit){
		return !submerged(unit);
	}
	
	@Override
	public void applyColor(Unit unit){
		super.applyColor(unit);
		if(submerged(unit))Draw.alpha(0.4f);
	}
}
