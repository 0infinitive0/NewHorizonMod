package newhorizon.expand.units.hiemara;

import mindustry.type.UnitType;

/**
 * A unit that hauls items between a port and the depots. Most carry one kind of item; the big ships carry
 * {@link #holdTypes} kinds at once, {@link #itemCapacity} of each.
 */
public class CargoUnitType extends UnitType{
	/** How many different items the unit carries at the same time. */
	public int holdTypes = 1;
	/** The tier in its line, for display. */
	public int cargoTier = 1;
	
	public CargoUnitType(String name){
		super(name);
	}
	
	public boolean multi(){
		return holdTypes > 1;
	}
}
